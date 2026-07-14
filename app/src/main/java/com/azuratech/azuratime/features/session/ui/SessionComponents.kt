package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuratime.core.ui.components.TierBadge

fun getDayName(day: Int): String = when (day) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Unknown"
}
