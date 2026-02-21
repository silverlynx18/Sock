package com.example.sockapp.ui.components.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldDefaults
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sockapp.data.models.userstatus.AppPresetStatus
import com.example.sockapp.data.models.userstatus.CustomStatusType
import com.example.sockapp.data.models.userstatus.UserGeneratedStatusPreset
import com.example.sockapp.viewmodels.ProfileViewModel
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val MAX_CUSTOM_STATUS_LENGTH = 100

// --- Duration Options Data ---
sealed class StatusDurationOption(val displayName: String) {
    object DontClear : StatusDurationOption("Don't clear")
    object After30Mins : StatusDurationOption("Clear after 30 minutes")
    object After1Hour : StatusDurationOption("Clear after 1 hour")
    object After4Hours : StatusDurationOption("Clear after 4 hours")
    object EndOfDay : StatusDurationOption("Clear at end of day") // e.g. 11:59 PM today
    object Custom : StatusDurationOption("Custom date & time...") // Would trigger date/time picker
    // TODO: Add more options like "Clear tomorrow morning"

    companion object {
        val options = listOf(DontClear, After30Mins, After1Hour, After4Hours, EndOfDay, Custom)
    }
}

fun calculateExpiresAt(option: StatusDurationOption, customTimestamp: Timestamp? = null): Timestamp? {
    val now = System.currentTimeMillis()
    return when (option) {
        StatusDurationOption.DontClear -> null
        StatusDurationOption.After30Mins -> Timestamp(now + TimeUnit.MINUTES.toMillis(30), 0)
        StatusDurationOption.After1Hour -> Timestamp(now + TimeUnit.MINUTES.toMillis(60), 0)
        StatusDurationOption.After4Hours -> Timestamp(now + TimeUnit.HOURS.toMillis(4), 0)
        StatusDurationOption.EndOfDay -> {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            Timestamp(calendar.timeInMillis / 1000, (calendar.timeInMillis % 1000).toInt() * 1_000_000)
        }
        StatusDurationOption.Custom -> customTimestamp // Requires a date/time picker to set
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSelectionBottomSheet(
    profileViewModel: ProfileViewModel,
    // If groupId is null, it's for global status. Otherwise, for group-specific.
    forGroupId: String? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val user = profileState.user
    val userPresets = profileState.userStatusPresets

    // --- State for UI selection ---
    // Determines which section is active / what kind of status is being built
    var currentSelectionType by remember { mutableStateOf(CustomStatusType.APP_PRESET) }

    // Holds ID of selected AppPresetStatus (e.g., "online", "busy")
    var selectedAppPresetId by remember { mutableStateOf(AppPresetStatus.Online.id) }

    // Holds ID of selected UserGeneratedStatusPreset
    var selectedUserPresetId by remember { mutableStateOf<String?>(null) }

    // For ad-hoc custom status
    var adHocCustomText by remember { mutableStateOf("") }
    var adHocCustomIconKey by remember { mutableStateOf("") } // User types icon key for now

    // For duration
    var selectedDurationOption by remember { mutableStateOf<StatusDurationOption>(StatusDurationOption.DontClear) }
    var customDateTimeForExpiry by remember { mutableStateOf<Timestamp?>(null) } // For custom duration picker

    // Initialize state based on current status (global or group-specific)
    LaunchedEffect(user, forGroupId, profileState.groupStatusDetails) {
        val (initialType, initialActiveRefId, initialText, initialIconKey, initialExpiresAt) =
            if (forGroupId != null) {
                val groupStatus = profileState.groupStatusDetails[forGroupId]
                Triple(
                    groupStatus?.type ?: CustomStatusType.APP_PRESET,
                    groupStatus?.activeStatusReferenceId ?: user?.activeStatusId ?: AppPresetStatus.Online.id,
                    groupStatus?.customText ?: "",
                    groupStatus?.customIconKey ?: "",
                ).let { Triple(it.first, it.second, it.third).let { t -> Quadruple(t.first, t.second, t.third, groupStatus?.expiresAt)} }

            } else {
                 Triple(
                    if (!user?.globalCustomStatusText.isNullOrBlank() || !user?.globalCustomStatusIconKey.isNullOrBlank()) CustomStatusType.AD_HOC_CUSTOM else CustomStatusType.APP_PRESET,
                    user?.activeStatusId ?: AppPresetStatus.Online.id,
                    user?.globalCustomStatusText ?: "",
                    user?.globalCustomStatusIconKey ?: "",
                ).let { Triple(it.first, it.second, it.third).let { t -> Quadruple(t.first, t.second, t.third, user?.globalStatusExpiresAt)} }
            }

        currentSelectionType = initialType
        when (initialType) {
            CustomStatusType.APP_PRESET -> selectedAppPresetId = initialActiveRefId ?: AppPresetStatus.Online.id
            CustomStatusType.USER_GENERATED_PRESET -> selectedUserPresetId = initialActiveRefId
            CustomStatusType.AD_HOC_CUSTOM -> { /* Handled by adHoc fields */ }
        }
        adHocCustomText = initialText ?: ""
        adHocCustomIconKey = initialIconKey ?: ""
        // TODO: Initialize selectedDurationOption based on initialExpiresAt
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0.dp) // Consume no insets
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text("Set your status", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(12.dp))

            // --- Main Content Area (Scrollable) ---
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {

                // Section: App Preset Statuses
                item { ListSubheader("Quick select") }
                items(AppPresetStatus.selectableAppPresetStatuses) { preset ->
                    StatusPresetItem(
                        text = preset.displayName,
                        icon = preset.icon,
                        isSelected = currentSelectionType == CustomStatusType.APP_PRESET && selectedAppPresetId == preset.id,
                        onClick = {
                            currentSelectionType = CustomStatusType.APP_PRESET
                            selectedAppPresetId = preset.id
                            selectedUserPresetId = null
                            adHocCustomText = if (preset == AppPresetStatus.ShowingCustomMessage) adHocCustomText else "" // Keep text if switching to custom message type
                            adHocCustomIconKey = ""
                        }
                    )
                }

                // Section: User's Saved Presets
                if (userPresets.isNotEmpty()) {
                    item { ListSubheader("Your saved statuses") }
                    items(userPresets) { preset ->
                        StatusPresetItem(
                            text = preset.presetName, // Or preset.statusText
                            icon = getIconForKey(preset.iconKey), // Assumes getIconForKey helper
                            isSelected = currentSelectionType == CustomStatusType.USER_GENERATED_PRESET && selectedUserPresetId == preset.presetId,
                            onClick = {
                                currentSelectionType = CustomStatusType.USER_GENERATED_PRESET
                                selectedUserPresetId = preset.presetId
                                selectedAppPresetId = null // Clear app preset selection
                                // Populate ad-hoc fields from preset for potential editing or direct use
                                adHocCustomText = preset.statusText
                                adHocCustomIconKey = preset.iconKey
                            }
                        )
                    }
                }

                // Section: Ad-hoc Custom Status Input
                item { ListSubheader("Create new or edit current") }
                item {
                    OutlinedTextField(
                        value = adHocCustomText,
                        onValueChange = {
                            if (it.length <= MAX_CUSTOM_STATUS_LENGTH) adHocCustomText = it
                            currentSelectionType = CustomStatusType.AD_HOC_CUSTOM // Switch to ad-hoc if user types
                            selectedAppPresetId = null; selectedUserPresetId = null; // Clear other selections
                        },
                        label = { Text("Custom status message") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        supportingText = { Text("${adHocCustomText.length}/${MAX_CUSTOM_STATUS_LENGTH}") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = adHocCustomIconKey,
                        onValueChange = {
                            adHocCustomIconKey = it
                            currentSelectionType = CustomStatusType.AD_HOC_CUSTOM
                             selectedAppPresetId = null; selectedUserPresetId = null;
                        },
                        label = { Text("Custom icon key (e.g., 'coffee', 'work')") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        leadingIcon = { Icon(getIconForKey(adHocCustomIconKey), "Selected Icon") }
                    )
                }
                 item {
                    // TODO: Button to "Save current as preset" - opens another small dialog
                    // TextButton(onClick = { /* Open save preset dialog */ }) { Text("Save as preset...") }
                }


                // Section: Duration Selection
                item { ListSubheader("Clear status after...") }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDurationOption.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Duration") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            StatusDurationOption.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = { selectedDurationOption = option; expanded = false;
                                        if (option == StatusDurationOption.Custom) { /* TODO: Show date/time picker */ }
                                    }
                                )
                            }
                        }
                    }
                }
                // TODO: If selectedDurationOption is Custom, show Date/Time picker inputs
            } // End LazyColumn

            // --- Action Buttons ---
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val finalExpiresAt = calculateExpiresAt(selectedDurationOption, customDateTimeForExpiry)
                        var finalType = currentSelectionType
                        var finalActiveRefId: String? = null
                        var finalText: String? = null
                        var finalIconKey: String? = null

                        when (currentSelectionType) {
                            CustomStatusType.APP_PRESET -> {
                                finalActiveRefId = selectedAppPresetId
                                // If AppPreset is ShowingCustomMessage, then adHocText/Icon are used.
                                if (selectedAppPresetId == AppPresetStatus.ShowingCustomMessage.id) {
                                    finalText = adHocCustomText.trim().ifEmpty { null }
                                    finalIconKey = adHocCustomIconKey.trim().ifEmpty { null }
                                    // If text is empty for ShowingCustomMessage, maybe revert to Online or a default
                                    if(finalText == null && finalIconKey == null) finalActiveRefId = AppPresetStatus.Online.id
                                } else {
                                    // For other app presets, their inherent text/icon apply.
                                    // AdHoc fields might be used as overrides if UX allows, but current logic doesn't show inputs for them.
                                }
                            }
                            CustomStatusType.USER_GENERATED_PRESET -> {
                                finalActiveRefId = selectedUserPresetId
                                // Text and icon are typically from the preset, but adHoc fields might hold edits.
                                // For simplicity, assume preset values are used unless explicitly edited (adHoc fields are primary if different).
                                val preset = userPresets.find { it.presetId == selectedUserPresetId }
                                finalText = adHocCustomText.trim().ifEmpty { preset?.statusText }
                                finalIconKey = adHocCustomIconKey.trim().ifEmpty { preset?.iconKey }
                            }
                            CustomStatusType.AD_HOC_CUSTOM -> {
                                finalText = adHocCustomText.trim().ifEmpty { null }
                                finalIconKey = adHocCustomIconKey.trim().ifEmpty { null }
                                // If both are empty, what should it be? Maybe default to "Online" AppPresetStatus
                                if (finalText == null && finalIconKey == null) {
                                    finalType = CustomStatusType.APP_PRESET
                                    finalActiveRefId = AppPresetStatus.Online.id
                                } else {
                                  // If there's text or icon, ensure activeStatusReferenceId reflects it's not an app preset.
                                  // Could use AppPresetStatus.ShowingCustomMessage.id or null for purely ad-hoc.
                                   finalActiveRefId = AppPresetStatus.ShowingCustomMessage.id
                                }
                            }
                        }

                        if (forGroupId != null) {
                            profileViewModel.updateGroupSpecificStatus(
                                groupId = forGroupId,
                                type = finalType,
                                activeStatusReferenceId = finalActiveRefId,
                                customText = finalText,
                                customIconKey = finalIconKey,
                                expiresAt = finalExpiresAt
                            )
                        } else {
                            profileViewModel.updateUserGlobalStatus(
                                activeStatusId = finalActiveRefId, // This becomes User.activeStatusId
                                customText = finalText,            // User.globalCustomStatusText
                                customIconKey = finalIconKey,      // User.globalCustomStatusIconKey
                                expiresAt = finalExpiresAt,        // User.globalStatusExpiresAt
                                overwriteAllGroupStatuses = profileState.user?.overwriteAllGroupStatusesWithGlobal // Not changed here
                            )
                        }
                        onDismiss()
                    },
                    enabled = adHocCustomText.length <= MAX_CUSTOM_STATUS_LENGTH && !profileState.isUpdating
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Save Status")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Status")
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // Extra padding at the bottom
        }
    }
}

@Composable
private fun ListSubheader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
    )
}

@Composable
private fun StatusPresetItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


// Previews would need a mock ProfileViewModel instance.
// For brevity, detailed previews are omitted here but would be essential.
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "StatusSelectionBottomSheet Preview")
@Composable
fun StatusSelectionBottomSheetPreview() {
    // This requires a Mock ProfileViewModel or passing state directly for a good preview.
    // SockAppTheme {
    //    StatusSelectionBottomSheet(profileViewModel = viewModel(), onDismiss = {})
    // }
    Text("StatusSelectionBottomSheet Preview (Needs ViewModel)")
}
