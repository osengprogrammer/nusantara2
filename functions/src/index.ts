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

interface AccountDoc {
    email: string;
    name: string;
    status: string;
    role: string;
    activeSchoolId?: string;
    activeClassId?: string;
    fcmToken?: string;
    memberships: Record<string, any>;
    lastUpdated: admin.firestore.FieldValue;
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

export const sendparentnotification = onDocumentCreated(
    { document: `${COLLECTIONS.SCHOOLS}/{schoolId}/${COLLECTIONS.CHECKIN_RECORDS}/{recordId}`, region: "asia-southeast2" },
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();
        const schoolId = event.params.schoolId;
        const studentId = data.studentId || data.faceId;
        const studentName = data.name;
        const status = data.status;

        if (!schoolId || !studentId) return;

        try {
            const linksSnapshot = await admin.firestore().collection(COLLECTIONS.SCHOOLS).doc(schoolId)
                .collection(COLLECTIONS.PARENT_LINKS)
                .where("studentId", "==", studentId)
                .where("status", "==", "APPROVED")
                .get();

            if (linksSnapshot.empty) {
                const legacySnapshot = await admin.firestore().collection(COLLECTIONS.SCHOOLS).doc(schoolId)
                    .collection(COLLECTIONS.PARENT_LINKS)
                    .where("faceId", "==", studentId)
                    .where("status", "==", "APPROVED")
                    .get();
                if (!legacySnapshot.empty) await sendToFCM(legacySnapshot, studentName, status);
            } else {
                await sendToFCM(linksSnapshot, studentName, status);
            }
        } catch (error) {
            console.error("❌ AI-NATIVE: Notification error:", error);
        }
    }
);

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

async function sendToFCM(snapshot: admin.firestore.QuerySnapshot, studentName: string, status: string) {
    const promises: Promise<any>[] = [];
    snapshot.forEach(doc => {
        const parentEmail = doc.data().parentEmail;
        const p = admin.firestore().collection(COLLECTIONS.PARENT_USERS).doc(parentEmail).get().then(parentDoc => {
            if (parentDoc.exists) {
                const fcmToken = parentDoc.data()?.fcmToken;
                if (fcmToken) {
                    let statusText = "Present";
                    const s = (status || "").toUpperCase();
                    if (s === "A" || s === "ABSENT") statusText = "Absent (No Notice)";
                    else if (s === "S" || s === "SICK") statusText = "Sick";
                    else if (s === "P" || s === "PERMISSION") statusText = "Permission";

                    return admin.messaging().send({
                        notification: { title: "Azura Time: Attendance Info", body: `${studentName} has recorded attendance (${statusText}).` },
                        token: fcmToken
                    });
                }
            }
            return null;
        });
        promises.push(p);
    });
    await Promise.all(promises);
}
