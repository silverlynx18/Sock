package com.example.sockapp.data.models.userstatus

// This enum helps differentiate how a status is defined, especially for group-specific statuses.
// For global status, `User.activeStatusId` pointing to an AppPresetStatus vs. User.globalCustomStatusText being non-null implies this.
// For GroupStatusDetail, this can be more explicit.
enum class CustomStatusType {
    APP_PRESET, // Uses an `activeStatusId` that refers to a system-defined AppPresetStatus (e.g., "Online", "Busy")
    USER_GENERATED_PRESET, // Uses an `activeStatusId` that refers to a UserGeneratedStatusPreset.id
                           // The text/icon are then taken from that preset.
    AD_HOC_CUSTOM; // A unique, one-off custom status with text and icon defined directly in GroupStatusDetail
                   // or globally in User.globalCustomStatusText/IconKey fields.
}
