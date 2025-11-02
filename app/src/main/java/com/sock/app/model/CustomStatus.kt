package com.sock.app.model

enum class CustomStatusTone(val label: String, val colorHex: Long) {
    RELAXED("Relaxed", 0xFF4CAF50),
    SOCIAL("Social", 0xFF039BE5),
    FOCUSED("Focused", 0xFFFFA000),
    BOUNDARY("Boundary", 0xFFD32F2F);
}

data class CustomStatus(
    val text: String,
    val tone: CustomStatusTone,
    val note: String? = null
)
