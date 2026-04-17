package com.azuratech.azuratime.core.util

import android.content.Context
import android.widget.Toast

/**
 * Convenience extension to show a short Toast message from a Context.
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
