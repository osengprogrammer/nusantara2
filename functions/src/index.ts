import {onDocumentUpdated, onDocumentCreated} from "firebase-functions/v2/firestore";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

if (admin.apps.length === 0) {
    admin.initializeApp();
}

const SERVER_SECRET = "AZURA_ULTIMATE_PROTECTION_GUARD_2026_PROD";

/**
 * 👑 ON REGISTRATION APPROVED
 * Triggered when a membership status is set to ACTIVE.
 * Sets up whitelisted_accounts, main account, and school data.
 */
export const onregistrationapproved = onDocumentUpdated(
    "memberships/{uid}", 
    async (event) => {
        const before = event.data?.before.data();
        const after = event.data?.after.data();
        const uid = event.params.uid;

        if (!before || !after || !event.data) return;
        if (before.status === "ACTIVE" || after.status !== "ACTIVE") return;

        const email = (after.email || "").toLowerCase().trim();
        if (!email) return;

        const db = admin.firestore();
        console.log(`🚀 Activating user: ${email} with UID: ${uid}`);

        const hardwareId = after.hardwareId;
        if (!hardwareId || typeof hardwareId !== "string" || hardwareId.length < 5) return;

        const schoolId = after.schoolId || `SCH-${crypto.createHash("sha1").update(email).digest("hex").substring(0, 8).toUpperCase()}`;
        const schoolName = after.schoolName || "Sekolah Azura";
        const role = after.role || "ADMIN";

        const dbSeed = crypto.createHash("sha256").update(uid + SERVER_SECRET).digest("hex");
        const secureIsoKey = crypto.createHmac("sha256", SERVER_SECRET).update(`${hardwareId}-${dbSeed}-${uid}`).digest("hex");
        const expireDate = Date.now() + 365 * 24 * 60 * 60 * 1000;

        const batch = db.batch();
        // 🔥 Unified Collection Name: whitelisted_accounts
        const whitelistRef = db.collection("whitelisted_accounts").doc(uid);
        const accountRef = db.collection("accounts").doc(uid);

        batch.set(whitelistRef, {
            userId: uid, email, name: after.name || after.adminName || "User Azura",
            status: "ACTIVE", hardwareId, secureIsoKey, expireDate,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            activeSchoolId: schoolId,
            memberships: { [schoolId]: { schoolName: schoolName, role: role } },
            schoolId: schoolId, schoolName: schoolName, role: role,
            followingIds: [], followerIds: []
        });

        // 🔥 CRITICAL: Update the main accounts collection so the app can sync the status
        batch.set(accountRef, {
            status: "ACTIVE",
            role: role,
            activeSchoolId: schoolId,
            memberships: { [schoolId]: { schoolName: schoolName, role: role, status: "ACTIVE" } },
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        const orgRef = db.collection("schools").doc(schoolId);
        batch.set(orgRef, {
            schoolId: schoolId, schoolName: schoolName, accountId: uid,           
            ownerEmail: email, db_seed: dbSeed,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        batch.delete(event.data.after.ref);

        try {
            await batch.commit();
        } catch (err) {
            console.error("❌ Batch commit failed:", err);
        }
    }
);

/**
 * 🔔 SEND PARENT NOTIFICATION
 * Triggered when a new attendance record is pushed.
 * Uses studentId (Unified Identity).
 */
export const sendparentnotification = onDocumentCreated(
    "attendance_logs/{logId}",
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();
        const schoolId = data.schoolId;
        // 🔥 Unified Identity: Handle studentId (new) and faceId (legacy)
        const studentId = data.studentId || data.faceId;
        const studentName = data.name;
        const status = data.status; 

        if (!schoolId || !studentId) {
            console.log("⚠️ Missing schoolId or studentId, skipping notification.");
            return;
        }

        try {
            // 🔥 Query parent_links using studentId
            const linksSnapshot = await admin.firestore().collection('schools').doc(schoolId)
                .collection('parent_links')
                .where('studentId', '==', studentId)
                .where('status', '==', 'APPROVED')
                .get();

            if (linksSnapshot.empty) {
                // Fallback to legacy faceId if no links found with studentId
                const legacySnapshot = await admin.firestore().collection('schools').doc(schoolId)
                    .collection('parent_links')
                    .where('faceId', '==', studentId)
                    .where('status', '==', 'APPROVED')
                    .get();
                
                if (legacySnapshot.empty) return;
                
                await sendToFCM(legacySnapshot, studentName, status);
            } else {
                await sendToFCM(linksSnapshot, studentName, status);
            }
        } catch (error) {
            console.error("❌ Notification error:", error);
        }
    }
);

/**
 * 🛡️ GET SECURITY ISO KEY
 * Https Callable function for the app to refresh its security key.
 */
export const getsecurityisokey = onCall(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new HttpsError("unauthenticated", "User must be logged in.");
    }

    const hardwareId = request.data.hardwareId;
    if (!hardwareId) {
        throw new HttpsError("invalid-argument", "Hardware ID is required.");
    }

    const db = admin.firestore();
    const doc = await db.collection("whitelisted_accounts").doc(uid).get();

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
});

async function sendToFCM(snapshot: admin.firestore.QuerySnapshot, studentName: string, status: string) {
    const promises: Promise<any>[] = [];
    snapshot.forEach(doc => {
        const parentEmail = doc.data().parentEmail;
        const p = admin.firestore().collection('parent_users').doc(parentEmail).get().then(parentDoc => {
            if (parentDoc.exists) {
                const fcmToken = parentDoc.data()?.fcmToken;
                if (fcmToken) {
                    let statusText = "Hadir";
                    const s = (status || "").toUpperCase();
                    if (s === "A" || s === "ALPA") statusText = "Alpa (Tanpa Keterangan)";
                    else if (s === "S" || s === "SAKIT") statusText = "Sakit";
                    else if (s === "I" || s === "IZIN") statusText = "Izin";
                    
                    return admin.messaging().send({ 
                        notification: { 
                            title: 'Azura Time: Info Kehadiran', 
                            body: `${studentName} telah melakukan presensi (${statusText}).` 
                        }, 
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
