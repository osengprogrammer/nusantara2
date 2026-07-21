import { onDocumentUpdated, onDocumentCreated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

if (admin.apps.length === 0) {
    admin.initializeApp();
}

const SERVER_SECRET = "AZURA_ULTIMATE_PROTECTION_GUARD_2026_PROD";

const COLLECTIONS = {
    MEMBERSHIPS: "memberships",
    ACCOUNTS: "accounts",
    WHITELISTED_ACCOUNTS: "whitelisted_accounts",
    SCHOOLS: "schools",
    CONNECTION_REQUESTS: "connection_requests",
    PARENT_LINKS: "parent_links",
    PARENT_USERS: "parent_users",
    CHECKIN_RECORDS: "checkin_records",
};

interface MembershipDoc {
    email: string;
    name?: string;
    adminName?: string;
    status: string;
    hardwareId: string;
    schoolId?: string;
    schoolName?: string;
    role?: string;
}

interface SchoolDoc {
    schoolId: string;
    schoolName: string;
    accountId?: string;
    ownerEmail?: string;
    db_seed: string;
    createdAt?: admin.firestore.FieldValue;
    updatedAt: admin.firestore.FieldValue;
}

// ==========================================
// 1. REGISTRATION APPROVAL
// ==========================================
export const onregistrationapproved = onDocumentUpdated(
    { document: `${COLLECTIONS.MEMBERSHIPS}/{uid}`, region: "asia-southeast2" },
    async (event) => {
        const before = event.data?.before.data() as MembershipDoc | undefined;
        const after = event.data?.after.data() as MembershipDoc | undefined;
        const uid = event.params.uid;

        if (!before || !after || !event.data) return;
        if (before.status === "ACTIVE" || after.status !== "ACTIVE") return;

        const email = (after.email || "").toLowerCase().trim();
        if (!email) return;

        const db = admin.firestore();
        console.log(`🚀 AI-NATIVE: Activating user ${email} (UID: ${uid})`);

        const hardwareId = after.hardwareId;
        if (!hardwareId || typeof hardwareId !== "string" || hardwareId.length < 5) return;

        const schoolId = after.schoolId || `SCH-${crypto.createHash("sha1").update(email).digest("hex").substring(0, 8).toUpperCase()}`;
        const schoolName = after.schoolName || "Azura School";
        const role = after.role || "USER";
        const name = after.name || after.adminName || "Azura User";

        const dbSeed = crypto.createHash("sha256").update(uid + SERVER_SECRET).digest("hex");
        const secureIsoKey = crypto.createHmac("sha256", SERVER_SECRET).update(`${hardwareId}-${dbSeed}-${uid}`).digest("hex");
        const expireDate = Date.now() + 365 * 24 * 60 * 60 * 1000;

        const batch = db.batch();
        const whitelistRef = db.collection(COLLECTIONS.WHITELISTED_ACCOUNTS).doc(uid);
        const accountRef = db.collection(COLLECTIONS.ACCOUNTS).doc(uid);

        batch.set(whitelistRef, {
            userId: uid, email, name, status: "ACTIVE", hardwareId, secureIsoKey, expireDate,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            activeSchoolId: schoolId,
            memberships: { [schoolId]: { schoolName: schoolName, role: role } },
            schoolId: schoolId, schoolName: schoolName, role: role,
            followingIds: [], followerIds: []
        });

        batch.set(accountRef, {
            email, name, status: "ACTIVE", role: role, activeSchoolId: schoolId,
            memberships: { [schoolId]: { schoolName: schoolName, role: role, status: "ACTIVE" } },
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            const orgRef = db.collection(COLLECTIONS.SCHOOLS).doc(schoolId);
            const schoolData: Partial<SchoolDoc> = {
                schoolId: schoolId, schoolName: schoolName, db_seed: dbSeed,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            };
            if (!after.schoolId) {
                schoolData.accountId = uid;
                schoolData.ownerEmail = email;
                schoolData.createdAt = admin.firestore.FieldValue.serverTimestamp();
            }
            batch.set(orgRef, schoolData, { merge: true });
        }

        batch.delete(event.data.after.ref);

        try {
            await batch.commit();
            console.log(`✅ AI-NATIVE: Activation complete for ${uid}`);
        } catch (err) {
            console.error("❌ AI-NATIVE: Activation batch failed:", err);
        }
    }
);

// ==========================================
// 2. CONNECTION REQUEST NOTIFICATION
// ==========================================
export const onconnectionrequestcreated = onDocumentCreated(
    { document: `${COLLECTIONS.CONNECTION_REQUESTS}/{requestId}`, region: "asia-southeast2" },
    async (event) => {
        const data = event.data?.data();
        if (!data) return;
        const senderId = data.senderId;
        const targetId = data.targetId;
        const db = admin.firestore();

        try {
            const senderDoc = await db.collection(COLLECTIONS.WHITELISTED_ACCOUNTS).doc(senderId).get();
            const senderName = senderDoc.data()?.name || "Someone";
            const targetDoc = await db.collection(COLLECTIONS.ACCOUNTS).doc(targetId).get();
            const fcmToken = targetDoc.data()?.fcmToken;

            if (fcmToken) {
                await admin.messaging().send({
                    notification: { title: "Connection Request! 🤝", body: `${senderName} wants to connect with you.` },
                    token: fcmToken
                });
            }
        } catch (error) {
            console.error("❌ AI-NATIVE: Connection notification error:", error);
        }
    }
);

// ==========================================
// 3. ACCOUNT FOLLOWED NOTIFICATION
// ==========================================
export const onaccountfollowed = onDocumentUpdated(
    { document: `${COLLECTIONS.WHITELISTED_ACCOUNTS}/{uid}`, region: "asia-southeast2" },
    async (event) => {
        const before = event.data?.before.data();
        const after = event.data?.after.data();
        const uid = event.params.uid;
        if (!before || !after) return;

        const beforeFollowers = (before.followerIds || []) as string[];
        const afterFollowers = (after.followerIds || []) as string[];

        if (afterFollowers.length > beforeFollowers.length) {
            const newFollowerUid = afterFollowers[afterFollowers.length - 1];
            const db = admin.firestore();

            try {
                const followerDoc = await db.collection(COLLECTIONS.WHITELISTED_ACCOUNTS).doc(newFollowerUid).get();
                const followerName = followerDoc.data()?.name || "Someone";
                const targetDoc = await db.collection(COLLECTIONS.ACCOUNTS).doc(uid).get();
                const fcmToken = targetDoc.data()?.fcmToken;

                if (fcmToken) {
                    await admin.messaging().send({
                        notification: { title: "Connected! 🤝", body: `${followerName} is now connected with you.` },
                        token: fcmToken
                    });
                }
            } catch (error) {
                console.error("❌ AI-NATIVE: Follow notification error:", error);
            }
        }
    }
);

// ==========================================
// 4. ATTENDANCE NOTIFICATION
// Trigger: schools/{schoolId}/checkin_records/{recordId}
// (synced from azura-time via FirestoreManager)
// ==========================================
export const sendparentnotification = onDocumentCreated(
    { document: "schools/{schoolId}/checkin_records/{recordId}", region: "asia-southeast2" },
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();

        const schoolId = event.params.schoolId || data.schoolId;
        const studentId = data.studentId || data.faceId;
        const studentName = data.name || "Siswa";
        const status = data.status || "H";

        if (!schoolId || !studentId) {
            console.log("⚠️ Missing schoolId or studentId in checkin record");
            return;
        }

        const subscriptionKey = `${schoolId}_${studentId}`;

        try {
            const db = admin.firestore();

            // ✅ Loop breaker: skip if this record was created by the Cloud Function itself
            if (data.source === "cloud_function") {
                console.log(`ℹ️ Skipping self-triggered record for ${studentName}`);
                return;
            }

            // Query parent subscriptions
            const parentUsersSnapshot = await db.collection(COLLECTIONS.PARENT_USERS)
                .where(`subscriptions.${subscriptionKey}.status`, "in", ["ACTIVE", "APPROVED"])
                .get();

            if (parentUsersSnapshot.empty) {
                console.log(`ℹ️ No matching parent subscriptions found for key: ${subscriptionKey}`);
                return;
            }

            const promises = parentUsersSnapshot.docs.map(async (parentDoc) => {
                const parentEmail = parentDoc.id;
                const parentData = parentDoc.data();
                const fcmToken = parentData?.fcmToken;

                // ✅ FIX 2: Save notification to Firestore so app notification screen can read it
                let statusText = "Present";
                const s = (status || "").toUpperCase();
                if (s === "A" || s === "ABSENT") statusText = "Absent";
                else if (s === "S" || s === "SICK") statusText = "Sick";
                else if (s === "I" || s === "IZIN") statusText = "Permission";
                else if (s === "T" || s === "LATE") statusText = "Late";

                await db.collection(COLLECTIONS.PARENT_USERS).doc(parentEmail)
                    .collection("notifications")
                    .add({
                        title: "Kehadiran Tercatat",
                        message: `${studentName} telah tercatat (${statusText}).`,
                        studentId,
                        schoolId,
                        type: "ATTENDANCE_CHECKIN",
                        isRead: false,
                        timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    });
                console.log(`✅ Notification saved to Firestore for ${parentEmail}`);

                // Send FCM push notification
                if (fcmToken) {
                    try {
                        await admin.messaging().send({
                            notification: {
                                title: "Kehadiran Tercatat",
                                body: `${studentName} telah tercatat (${statusText}).`
                            },
                            data: {
                                studentId,
                                schoolId,
                                status: status || "",
                                type: "ATTENDANCE_CHECKIN"
                            },
                            token: fcmToken
                        });
                        console.log(`✅ SUCCESS: Notification sent to ${parentEmail}`);
                    } catch (err: any) {
                        if (err.code === 'messaging/registration-token-not-registered') {
                            console.log(`🧹 Cleaning up dead FCM token for ${parentEmail}`);
                            try {
                                await db.collection(COLLECTIONS.PARENT_USERS).doc(parentEmail).update({
                                    fcmToken: admin.firestore.FieldValue.delete()
                                });
                            } catch (cleanupErr) {
                                console.error(`❌ Failed to clean up token for ${parentEmail}:`, cleanupErr);
                            }
                        } else {
                            console.error(`❌ FCM ERROR for ${parentEmail}:`, err);
                        }
                    }
                }
            });

            await Promise.all(promises);
        } catch (error) {
            console.error("❌ AI-NATIVE: Notification error:", error);
        }
    }
);

// ==========================================
// 5. PAYMENT NOTIFICATION (FIXED: saves notif to Firestore)
// ==========================================
export const sendpaymentnotification = onDocumentCreated(
    { document: "schools/{schoolId}/transactions/{recordId}", region: "asia-southeast2" },
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();

        const schoolId = event.params.schoolId || data.schoolId;
        const studentId = data.studentId || data.faceId;
        const amount = data.amount || 0;
        const type = data.type || "UNKNOWN";
        const performedBy = data.performedByAccountName || "Admin";

        if (!schoolId || !studentId) {
            console.log("⚠️ Missing schoolId or studentId in transaction");
            return;
        }

        const subscriptionKey = `${schoolId}_${studentId}`;

        try {
            const db = admin.firestore();

            const parentUsersSnapshot = await db.collection(COLLECTIONS.PARENT_USERS)
                .where(`subscriptions.${subscriptionKey}.status`, "in", ["ACTIVE", "APPROVED"])
                .get();

            if (parentUsersSnapshot.empty) {
                console.log(`ℹ️ No matching parent subscriptions found for key: ${subscriptionKey}`);
                return;
            }

            const promises = parentUsersSnapshot.docs.map(async (parentDoc) => {
                const parentEmail = parentDoc.id;
                const parentData = parentDoc.data();
                const fcmToken = parentData?.fcmToken;

                const formattedAmount = `Rp ${amount.toLocaleString('id-ID')}`;

                let title = "Transaksi Saldo";
                let message = "";

                if (type === "TOP_UP") {
                    title = "💰 Top Up Berhasil";
                    message = `Saldo anak Anda bertambah ${formattedAmount} oleh ${performedBy}.`;
                } else if (type === "DEDUCTION") {
                    title = "💸 Saldo Digunakan";
                    message = `Saldo anak Anda berkurang ${formattedAmount} oleh ${performedBy}.`;
                } else {
                    message = `Transaksi ${formattedAmount} tercatat untuk anak Anda.`;
                }

                // ✅ FIX: Save notification to Firestore
                await db.collection(COLLECTIONS.PARENT_USERS).doc(parentEmail)
                    .collection("notifications")
                    .add({
                        title,
                        message,
                        studentId,
                        schoolId,
                        amount,
                        type,
                        transactionType: "PAYMENT",
                        isRead: false,
                        timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    });
                console.log(`✅ Payment notification saved to Firestore for ${parentEmail}`);

                // Send FCM push notification
                if (fcmToken) {
                    try {
                        await admin.messaging().send({
                            notification: { title: title, body: message },
                            data: {
                                studentId,
                                schoolId,
                                amount: amount.toString(),
                                type,
                                performedBy,
                                transactionType: "PAYMENT"
                            },
                            token: fcmToken
                        });
                        console.log(`✅ SUCCESS: Payment notification sent to ${parentEmail}`);
                    } catch (err) {
                        console.error(`❌ FCM ERROR for ${parentEmail}:`, err);
                    }
                }
            });

            await Promise.all(promises);
        } catch (error) {
            console.error("❌ AI-NATIVE: Payment notification error:", error);
        }
    }
);

// ==========================================
// 6. GET SECURITY ISO KEY
// ==========================================
export const getsecurityisokey = onCall(
    { region: "asia-southeast2" },
    async (request) => {
        const uid = request.auth?.uid;
        if (!uid) {
            throw new HttpsError("unauthenticated", "User must be logged in.");
        }

        const hardwareId = request.data.hardwareId;
        if (!hardwareId) {
            throw new HttpsError("invalid-argument", "Hardware ID is required.");
        }

        const db = admin.firestore();
        const doc = await db.collection(COLLECTIONS.WHITELISTED_ACCOUNTS).doc(uid).get();

        if (!doc.exists) {
            throw new HttpsError("permission-denied", "User is not whitelisted.");
        }

        const data = doc.data();
        if (data?.status !== "ACTIVE") {
            throw new HttpsError("permission-denied", "Account is not active.");
        }

        if (data.hardwareId !== hardwareId) {
            throw new HttpsError("permission-denied", "Hardware ID mismatch.");
        }

        return {
            isoKey: data.secureIsoKey,
            expireDate: data.expireDate
        };
    }
);

// ==========================================
// 7. PROCESS BANK NOTIFICATION (Zero-Cost Auto Top-Up)
// Trigger: bank_notifications/{notifId}
// Parses raw bank notification text and auto-credits student wallet
// ==========================================
export const processbanknotification = onDocumentCreated(
    { document: "bank_notifications/{notifId}", region: "asia-southeast2" },
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();
        const notifId = event.params.notifId;
        const db = admin.firestore();

        // --- Guard Clause: prevent double processing ---
        if (data.processed === true) {
            console.log(`ℹ️ Notification ${notifId} already processed, skipping.`);
            return null;
        }

        console.log(`🏦 Processing bank notification: ${notifId}`);

        const title = (data.title || "").toString();
        const body = (data.body || "").toString();
        const schoolId = data.schoolId || "";
        console.log(`📝 Raw notification body: ${body}`);

        // --- Parse Amount from notification text ---
        // BCA/BNI use "IDR 5,321.00" format (comma = thousands, .00 = cents)
        // Indonesian banks/e-wallets use "Rp 50.000" format (dot = thousands)
        const amountMatch = body.match(/IDR\s?([\d,]+\.\d{2})/i)
            || body.match(/Rp\s?([\d,.]+)/i)
            || title.match(/IDR\s?([\d,]+\.\d{2})/i)
            || title.match(/Rp\s?([\d,.]+)/i);

        if (!amountMatch || !schoolId) {
            console.log(`❌ Failed to parse bank notification ${notifId}: missing amount or schoolId.`);
            console.log(`   Title: ${title}`);
            console.log(`   Body: ${body}`);
            await db.collection("bank_notifications").doc(notifId).update({
                processed: true,
                status: "FAILED_PARSE",
                parsedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            return null;
        }

        // Format-aware amount cleaning:
        //   IDR 5,321.00 → strip decimal → "5,321" → remove commas → "5321"
        //   Rp 50.000    → remove dots → "50000"
        //   Rp 5.321     → remove dots → "5321"
        let cleanAmount: string;
        const rawValue = amountMatch[1];
        const isIDR = amountMatch[0].toUpperCase().startsWith("IDR");

        if (isIDR) {
            // IDR format: strip .XX cents, then remove commas
            cleanAmount = rawValue.replace(/\.\d{2}$/, "").replace(/,/g, "");
        } else {
            // Rp format: remove dots (thousands separator)
            cleanAmount = rawValue.replace(/\./g, "");
        }

        const amount = parseInt(cleanAmount, 10);

        if (isNaN(amount) || amount <= 0) {
            console.log(`❌ Invalid parsed amount for ${notifId}: "${rawValue}" → "${cleanAmount}" → ${amount}`);
            await db.collection("bank_notifications").doc(notifId).update({
                processed: true,
                status: "FAILED_PARSE",
                parsedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            return null;
        }

        console.log(`💰 Parsed amount: ${rawValue} → ${cleanAmount} → ${amount}`);

        // --- Determine student ID ---
        // Priority:
        // 1. If the Android app sent a studentId field directly in the document, use it
        // 2. Otherwise, extract from LAST 3 DIGITS of the transferred amount
        //    (parents transfer an amount ending with last 3 digits of child's student ID)
        const suffix3 = (amount % 1000).toString().padStart(3, '0');
        let studentId: string | null = data.studentId ? (data.studentId as string).trim() : null;

        if (studentId && studentId.length > 0) {
            console.log(`📋 Using studentId from document data: ${studentId} (suffix: ${suffix3})`);
        } else {
            console.log(`🔍 No studentId in document. Looking up wallet by suffix: ${suffix3} (from amount ${amount})`);

            // Query all student wallets for this school to find a matching suffix
            // Firestore lacks ENDS_WITH, so we fetch all wallets and filter in code.
            // For large schools, consider storing a `studentIdSuffix` field on wallet docs.
            const walletsSnapshot = await db.collection("schools").doc(schoolId)
                .collection("student_wallets")
                .get();

            const matchingWallet = walletsSnapshot.docs
                .map(doc => ({ id: doc.id, data: doc.data() }))
                .find(w => (w.data.studentId || "").endsWith(suffix3));

            if (!matchingWallet) {
                console.log(`❌ No student wallet found with ID suffix: ${suffix3} for school ${schoolId}`);
                await db.collection("bank_notifications").doc(notifId).update({
                    processed: true,
                    status: "FAILED_PARSE",
                    parsedAt: admin.firestore.FieldValue.serverTimestamp(),
                    parsedAmount: amount,
                    parsedSuffix: suffix3,
                });
                return null;
            }

            studentId = matchingWallet.data.studentId;
            console.log(`✅ Matched student: ${studentId} (wallet doc: ${matchingWallet.id}) via suffix: ${suffix3}`);
        }

        // Safety guard — should never reach here without a studentId
        if (!studentId) {
            console.log(`❌ Student ID unexpectedly null for ${notifId}`);
            return null;
        }

        // --- Atomic Transaction: create transaction record + update wallet ---
        try {
            await db.runTransaction(async (transaction) => {
                const walletRef = db.collection("schools").doc(schoolId)
                    .collection("student_wallets").doc(studentId);
                const txRef = db.collection("schools").doc(schoolId)
                    .collection("transactions").doc();

                // Create transaction record
                transaction.set(txRef, {
                    studentId,
                    schoolId,
                    amount,
                    type: "TOP_UP",
                    performedBy: "BANK_FORWARDER",
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                });

                // Increment wallet balance (creates doc with initial balance if missing)
                transaction.set(walletRef, {
                    studentId,
                    schoolId,
                    balance: admin.firestore.FieldValue.increment(amount),
                    lastTopUp: admin.firestore.FieldValue.serverTimestamp(),
                }, { merge: true });
            });

            // --- Mark as processed ---
            await db.collection("bank_notifications").doc(notifId).update({
                processed: true,
                status: "SUCCESS",
                parsedAt: admin.firestore.FieldValue.serverTimestamp(),
                parsedAmount: amount,
                parsedSuffix: suffix3,
                parsedStudentId: studentId,
            });

            console.log(`✅ Auto Top-Up successful for student ${studentId}: +Rp ${amount.toLocaleString("id-ID")}`);
        } catch (error) {
            console.error(`❌ Auto Top-Up failed for notification ${notifId}:`, error);
            await db.collection("bank_notifications").doc(notifId).update({
                processed: true,
                status: "ERROR",
                errorMessage: String(error),
                parsedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }

        return null;
    }
);
