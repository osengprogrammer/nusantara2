package com.azuratech.azuratime.features.bankforwarder.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.bankforwarder.data.local.BankNotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class BankForwarderNotificationListener : NotificationListenerService() {

    private val TAG = "BankNotifListener"

    // Known Indonesian bank package names
    private val BANK_PACKAGES = setOf(
        // BCA
        "com.bca",                     // BCA Mobile
        "id.bca.klikbca",             // KlikBCA
        "com.bca.jmp",                // BCA Syariah
        // BNI
        "com.bni.bni",                 // BNI Mobile
        "id.co.bni.mobile",           // BNI Mobile Banking
        // BRI
        "com.bri.bri",                 // BRImo
        "co.id.bri.brimo",            // BRImo (alt)
        // Mandiri
        "com.mandiri.mandiri",        // Mandiri Online
        "id.co.mandiri.mobile",      // Livin by Mandiri
        // BSI
        "com.bsm.bsmline",           // BSI Mobile
        "id.co.bankbsi.bsi",         // BSI Mobile (new)
        // Digital wallets
        "com.jenius.maya",            // Jenius
        "id.dana",                    // DANA
        "id.orid.payment",           // OVO
        "com.gopay.gopay",           // GoPay
        "com.shopeepay.shopeepay",   // ShopeePay
        "com.linkaja",               // LinkAja
        // Other banks
        "com.bank.muamalat",         // Bank Muamalat
        "id.co.cimb",                // CIMB Niaga
        "com.bnc",                   // Bank Neo Commerce
        "id.danamon.mobile",         // Danamon
        "id.co.bank.btpn",           // BTPN / Jenius
        "com.via.dki",               // DKI
        // Samsung Pay
        "com.samsung.android.spay",  // Samsung Pay
        "com.krom.samsung",          // Samsung (some regions)
    )

    // Regex patterns for parsing Indonesian bank notifications
    private val AMOUNT_PATTERNS = listOf(
        // 1. Explicit currency prefix: "Rp 50.000", "IDR 5,321.00", "+Rp 100.000"
        Regex("""(?:Rp\.?|IDR)\s?([\d.,]+)""", RegexOption.IGNORE_CASE),
        // 2. Keywords before amount: "sejumlah Rp 50.000", "masuk Rp 50.000"
        Regex("""(?:sejumlah|nominal|jumlah|transfer|top.?up|masuk|diterima|uang)\s*(?:sebesar\s*)?(?:Rp\.?|IDR)?\s?([\d.,]+)""", RegexOption.IGNORE_CASE),
        // 3. Fallback: "Rp 50.000" or "Rp. 50.000" anywhere — handles most e-wallet formats
        Regex("""Rp\.?\s?([\d.,]+)""", RegexOption.IGNORE_CASE),
    )

    // Blacklist: only exclude known OUTGOING transaction keywords.
    // "pembayaran" alone is NOT in the list — it appears in incoming too
    // ("Pembayaran diterima dari John"). Use specific outgoing-only phrases.
    // DANA/OVO incoming notifications use "diterima" but NOT "dana terkirim".
    private val OUTGOING_KEYWORDS = listOf(
        "terkirim", "dikirim", "berhasil dikirim", "transfer keluar",
        "pembayaran berhasil", "pembayaran ke",
        "gagal", "penarikan", "withdraw", "debit",
        "pengeluaran", "biaya admin", "fee",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName ?: return

        // Only process notifications from known bank/payment apps
        if (!isBankNotification(packageName)) {
            return
        }

        Log.d(TAG, "🏦 Bank notification detected from: $packageName")

        // Extract notification content
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val body = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()

        // Use bigText if available (expanded notification), otherwise fall back to body
        val fullText = bigText ?: body

        Log.d(TAG, "📩 Title: $title")
        Log.d(TAG, "📩 Body: $fullText")

        // Check if this is an incoming/credit notification (not outgoing)
        if (!isIncomingTransaction(fullText)) {
            Log.d(TAG, "⏭️ Not an incoming transaction, skipping.")
            return
        }

        // Parse the notification
        val parsed = parseBankNotification(title, fullText, packageName)
        if (parsed == null) {
            Log.w(TAG, "⚠️ Failed to parse bank notification from $packageName")
            return
        }

        Log.d(TAG, "✅ Parsed: bank=${parsed.bankName}, amount=${parsed.amount}, studentId=${parsed.studentId}")

        // Process asynchronously
        processNotification(parsed)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * Check if the notification is from a known bank/payment app
     */
    private fun isBankNotification(packageName: String): Boolean {
        return BANK_PACKAGES.any { pkg ->
            packageName.contains(pkg, ignoreCase = true)
        }
    }

    /**
     * Blacklist logic: return false ONLY for known outgoing/failed transactions.
     * Everything else is assumed to be incoming and will be parsed for an amount.
     */
    private fun isIncomingTransaction(text: String): Boolean {
        val lowerText = text.lowercase()
        val isOutgoing = OUTGOING_KEYWORDS.any { keyword ->
            lowerText.contains(keyword)
        }
        if (isOutgoing) {
            Log.d(TAG, "🚫 Outgoing keyword detected, skipping.")
        }
        return !isOutgoing
    }

    /**
     * Parse bank notification text to extract structured data
     */
    private fun parseBankNotification(title: String, body: String, packageName: String): ParsedBankNotification? {
        val fullText = "$title $body"

        // Extract bank name from package
        val bankName = extractBankName(packageName)

        // Extract amount
        val amount = extractAmount(body)
        if (amount == null || amount <= 0) {
            Log.w(TAG, "⚠️ Could not extract valid amount from: $body")
            return null
        }

        // Extract student ID from last 3 digits of the transferred amount
        val studentId = extractStudentIdFromAmount(amount)

        return ParsedBankNotification(
            bankName = bankName,
            title = title,
            body = body,
            amount = amount,
            studentId = studentId,
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Extract bank name from package name
     */
    private fun extractBankName(packageName: String): String {
        return when {
            packageName.contains("bca", ignoreCase = true) -> "BCA"
            packageName.contains("bni", ignoreCase = true) -> "BNI"
            packageName.contains("bri", ignoreCase = true) || packageName.contains("brimo", ignoreCase = true) -> "BRI"
            packageName.contains("mandiri", ignoreCase = true) || packageName.contains("livin", ignoreCase = true) -> "Mandiri"
            packageName.contains("bsi", ignoreCase = true) || packageName.contains("bsm", ignoreCase = true) -> "BSI"
            packageName.contains("jenius", ignoreCase = true) || packageName.contains("btpn", ignoreCase = true) -> "Jenius"
            packageName.contains("dana", ignoreCase = true) -> "DANA"
            packageName.contains("ovo", ignoreCase = true) -> "OVO"
            packageName.contains("gopay", ignoreCase = true) -> "GoPay"
            packageName.contains("shopeepay", ignoreCase = true) -> "ShopeePay"
            packageName.contains("linkaja", ignoreCase = true) -> "LinkAja"
            packageName.contains("muamalat", ignoreCase = true) -> "Muamalat"
            packageName.contains("cimb", ignoreCase = true) -> "CIMB"
            packageName.contains("bnc", ignoreCase = true) || packageName.contains("neon", ignoreCase = true) -> "BNC"
            packageName.contains("danamon", ignoreCase = true) -> "Danamon"
            packageName.contains("dki", ignoreCase = true) -> "DKI"
            else -> "Unknown Bank"
        }
    }

    /**
     * Extract amount from notification text
     * Handles formats like: "Rp 50.000", "Rp50.000", "Rp 50,000", "IDR 50000"
     */
    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                // Clean the amount string: remove dots (thousands separator), replace comma with dot for decimals
                val cleaned = amountStr
                    .replace(".", "")  // Remove thousands separator
                    .replace(",", ".") // Replace comma with dot for decimals

                val amount = cleaned.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        return null
    }

    /**
     * Extract student ID from the LAST 3 DIGITS of the transferred amount.
     *
     * Business logic: parents are asked to transfer an amount ending with
     * the last 3 digits of their child's student ID.
     *
     * Examples:
     *   Student ID 54321 → last 3 digits: 321
     *   Parent transfers Rp 10.000.321 → last 3 digits of amount: 321 → MATCH ✅
     *   Parent transfers Rp 500.321     → last 3 digits of amount: 321 → MATCH ✅
     *   Parent transfers Rp 5.321       → last 3 digits of amount: 321 → MATCH ✅
     *
     * The Cloud Function will use this 3-digit suffix to look up the matching student.
     */
    private fun extractStudentIdFromAmount(amount: Double): String? {
        val amountLong = amount.toLong()
        if (amountLong < 100) {
            Log.w(TAG, "⚠️ Amount $amount is too small to extract 3-digit student suffix")
            return null
        }

        // Take last 3 digits of the integer amount
        val suffix = (amountLong % 1000).toString().padStart(3, '0')
        Log.d(TAG, "🎯 Extracted student ID suffix from amount $amount: $suffix")
        return suffix
    }

    /**
     * Process the parsed notification: save to Room and upload to Firestore
     */
    private fun processNotification(parsed: ParsedBankNotification) {
        val context = applicationContext
        val db = AppDatabase.getInstance(context)
        val sessionManager = SessionManager.getInstance(context)
        val schoolId = sessionManager.getActiveSchoolId()

        if (schoolId.isNullOrBlank()) {
            Log.w(TAG, "⚠️ No active schoolId found in session. Cannot process notification.")
            return
        }

        val notifId = UUID.randomUUID().toString()
        val entity = BankNotificationEntity(
            id = notifId,
            bankName = parsed.bankName,
            title = parsed.title,
            body = parsed.body,
            amount = parsed.amount,
            studentId = parsed.studentId,
            timestamp = parsed.timestamp,
            isProcessed = false,
            isSynced = false,
        )

        // Save to Room database (local persistence)
        try {
            kotlinx.coroutines.runBlocking {
                db.bankNotificationDao().insertNotification(entity)
            }
            Log.d(TAG, "💾 Saved notification to Room: $notifId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save notification to Room", e)
        }

        // Upload to Firestore for Cloud Function processing
        uploadToFirestore(notifId, parsed, schoolId)
    }

    /**
     * Upload parsed notification to Firestore bank_notifications collection
     */
    private fun uploadToFirestore(notifId: String, parsed: ParsedBankNotification, schoolId: String) {
        val firestore = FirebaseFirestore.getInstance()

        val docData = hashMapOf(
            "title" to parsed.title,
            "body" to parsed.body,
            "schoolId" to schoolId,
            "studentId" to parsed.studentId,
            "amount" to parsed.amount,
            "bankName" to parsed.bankName,
            "packageName" to parsed.packageName,
            "processed" to false,
            "status" to "PENDING",
            "timestamp" to com.google.firebase.Timestamp.now(),
        )

        firestore.collection("bank_notifications")
            .document(notifId)
            .set(docData)
            .addOnSuccessListener {
                Log.d(TAG, "☁️ Uploaded to Firestore: $notifId (schoolId=$schoolId, amount=${parsed.amount})")
                // Mark as synced in Room
                markAsSynced(notifId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to upload to Firestore: $notifId", e)
            }
    }

    /**
     * Mark notification as synced in Room database
     */
    private fun markAsSynced(notifId: String) {
        try {
            kotlinx.coroutines.runBlocking {
                AppDatabase.getInstance(applicationContext)
                    .bankNotificationDao()
                    .markAsSynced(listOf(notifId))
            }
            Log.d(TAG, "✅ Marked as synced in Room: $notifId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark as synced: $notifId", e)
        }
    }

    /**
     * Data class to hold parsed bank notification data
     */
    private data class ParsedBankNotification(
        val bankName: String,
        val title: String,
        val body: String,
        val amount: Double,
        val studentId: String?,
        val packageName: String,
        val timestamp: Long,
    )
}
