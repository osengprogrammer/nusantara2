/**
 * One-time cleanup script: Remove duplicate checkin_records created by the infinite loop.
 *
 * Run with: npx ts-node src/cleanup-checkin-duplicates.ts
 */
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
    admin.initializeApp();
}

const db = admin.firestore();

async function cleanup() {
    const schoolsSnap = await db.collection("schools").get();

    for (const schoolDoc of schoolsSnap.docs) {
        const schoolId = schoolDoc.id;
        const checkinRef = db.collection("schools").doc(schoolId).collection("checkin_records");

        // Get all checkin records, ordered by timestamp
        const allRecords = await checkinRef.orderBy("timestamp", "asc").get();

        // Group by studentId + date to find duplicates
        const seen = new Map<string, string[]>(); // key -> [docId, ...]

        for (const doc of allRecords.docs) {
            const data = doc.data();
            const studentId = data.studentId || data.faceId || "";
            const date = data.attendanceDate || "";
            const key = `${studentId}_${date}`;

            if (!seen.has(key)) {
                seen.set(key, []);
            }
            seen.get(key)!.push(doc.id);
        }

        // Delete duplicates (keep the first one)
        let deletedCount = 0;
        for (const [key, docIds] of seen) {
            if (docIds.length > 1) {
                console.log(`🔄 ${schoolId}: ${key} has ${docIds.length} duplicates, removing ${docIds.length - 1}`);
                // Keep first, delete rest
                for (let i = 1; i < docIds.length; i++) {
                    await checkinRef.doc(docIds[i]).delete();
                    deletedCount++;
                }
            }
        }

        console.log(`✅ ${schoolId}: Cleaned up ${deletedCount} duplicate records`);
    }

    console.log("🎉 Cleanup complete!");
    process.exit(0);
}

cleanup().catch((err) => {
    console.error("❌ Cleanup failed:", err);
    process.exit(1);
});
