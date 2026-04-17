import {onDocumentUpdated, onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

if (admin.apps.length === 0) {
    admin.initializeApp();
}

const SERVER_SECRET = "AZURA_ULTIMATE_PROTECTION_GUARD_2026_PROD";

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
        const whitelistRef = db.collection("whitelisted_users").doc(uid);

        batch.set(whitelistRef, {
            userId: uid, email, name: after.name || after.adminName || "User Azura",
            status: "ACTIVE", hardwareId, secureIsoKey, expireDate,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            activeSchoolId: schoolId,
            memberships: { [schoolId]: { schoolName: schoolName, role: role } },
            schoolId: schoolId, schoolName: schoolName, role: role,
            followingIds: [], followerIds: []
        });

        const orgRef = db.collection("schools").doc(schoolId);
        batch.set(orgRef, {
            schoolId: schoolId, schoolName: schoolName, ownerId: uid,           
            ownerEmail: email, db_seed: dbSeed,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        batch.delete(event.data.after.ref);

        try {
            await batch.commit();
        } catch (err) {}
    }
);

export const sendparentnotification = onDocumentCreated(
    "attendance_logs/{logId}",
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const data = snap.data();
        const schoolId = data.schoolId;
        const faceId = data.faceId;
        const studentName = data.name;
        const status = data.status; 

        if (!schoolId || !faceId) return;

        try {
            const linksSnapshot = await admin.firestore().collection('schools').doc(schoolId).collection('parent_links').where('faceId', '==', faceId).where('status', '==', 'APPROVED').get();
            if (linksSnapshot.empty) return;

            const promises: Promise<any>[] = [];
            linksSnapshot.forEach(doc => {
                const parentEmail = doc.data().parentEmail;
                const p = admin.firestore().collection('parent_users').doc(parentEmail).get().then(parentDoc => {
                    if (parentDoc.exists) {
                        const fcmToken = parentDoc.data()?.fcmToken;
                        if (fcmToken) {
                            let statusText = "Hadir";
                            if (status === "A" || status === "ALPA") statusText = "Alpa (Tanpa Keterangan)";
                            else if (status === "S" || status === "SAKIT") statusText = "Sakit";
                            else if (status === "I" || status === "IZIN") statusText = "Izin";
                            return admin.messaging().send({ notification: { title: 'Azura Time: Info Kehadiran', body: `${studentName} telah melakukan presensi (${statusText}).` }, token: fcmToken });
                        }
                    }
                    return null;
                });
                promises.push(p);
            });
            await Promise.all(promises);
        } catch (error) {}
    }
);
