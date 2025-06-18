package com.example.sockapp.viewmodels

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Represents a phone contact with its SockApp status.
 * @param id The contact's ID from the device.
 * @param name The contact's display name.
 * @param phoneNumber The contact's phone number. IMPORTANT: Should be normalized to E.164 for reliable matching.
 * @param sockAppStatus The [SockAppUserStatus] indicating if the contact is a SockApp user.
 * @param sockAppUserId The SockApp User ID if the contact is a user.
 * @param sockAppDisplayName The SockApp display name or username if the contact is a user.
 */
data class PhoneContact(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    var sockAppStatus: SockAppUserStatus = SockAppUserStatus.UNKNOWN,
    var sockAppUserId: String? = null,
    var sockAppDisplayName: String? = null
)

/**
 * Enum representing the status of a phone contact in relation to the SockApp.
 */
enum class SockAppUserStatus {
    UNKNOWN,        // Status not yet checked
    LOADING,        // Currently checking status via Cloud Function
    IS_USER,        // Confirmed SockApp user
    NOT_USER,       // Confirmed not a SockApp user
    CHECK_FAILED    // Attempt to check status failed (e.g., network error, CF error for this specific contact)
}

/**
 * UI State for contact-related screens.
 * @param contacts List of [PhoneContact]s loaded from the device.
 * @param isLoadingContacts True if contacts are currently being loaded from the device.
 * @param error General error message for contact operations.
 * @param permissionGranted True if READ_CONTACTS permission has been granted by the user.
 * @param isLoadingSockStatuses True if SockApp user statuses for contacts are being fetched.
 */
data class ContactsUiState(
    val contacts: List<PhoneContact> = emptyList(),
    val isLoadingContacts: Boolean = false,
    val error: String? = null,
    val permissionGranted: Boolean = false,
    val isLoadingSockStatuses: Boolean = false
)

class ContactsViewModel : ViewModel() {

    private val functions: FirebaseFunctions = Firebase.functions

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    /**
     * Loads contacts from the device's contact book.
     * Requires READ_CONTACTS permission, which must be handled by the UI layer.
     * Normalization of phone numbers to E.164 format is crucial here before use.
     * @param contentResolver The application's [ContentResolver].
     */
    @SuppressLint("Range") // Suppress warning for cursor.getColumnIndex, common in ContentResolver queries
    fun loadPhoneContacts(contentResolver: ContentResolver) {
        if (!uiState.value.permissionGranted) {
            _uiState.update { it.copy(error = "Cannot load contacts: Permission not granted.") }
            return
        }

        _uiState.update { it.copy(isLoadingContacts = true, error = null) }
        viewModelScope.launch {
            try {
                val loadedContacts = mutableListOf<PhoneContact>()
                // Define the projection (columns to retrieve)
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, // Use DISPLAY_NAME_PRIMARY for better name results
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER // Request normalized number if available (API 16+)
                )

                // Query the contacts provider
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null, // No selection filter for now, load all contacts with phone numbers
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC" // Order by name
                )

                cursor?.use { // Ensure cursor is closed automatically
                    while (it.moveToNext()) {
                        val id = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
                        val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY))
                        val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        // Attempt to use normalized number if available, otherwise use the raw number.
                        // IMPORTANT: Further robust E.164 normalization is needed here or before sending to backend.
                        // Libraries like libphonenumber from Google can be used for this.
                        val normalizedNumber = try {
                            it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)) ?: number
                        } catch (e: Exception) { number } // Fallback if NORMALIZED_NUMBER column doesn't exist (older APIs)

                        // TODO: Implement robust E.164 normalization for 'normalizedNumber'
                        // For now, using it as is, but this is a critical point for accuracy.
                        loadedContacts.add(PhoneContact(id = id, name = name, phoneNumber = normalizedNumber))
                    }
                }
                _uiState.update { it.copy(contacts = loadedContacts, isLoadingContacts = false) }

                // After loading contacts, automatically check their SockApp status
                if (loadedContacts.isNotEmpty()) {
                    // Send only the phone numbers for checking
                    checkContactsSockAppStatus(loadedContacts.map { c -> c.phoneNumber })
                }
            } catch (e: SecurityException) {
                 Firebase.functions.logger.error("ContactsViewModel: SecurityException loading contacts. Is permission truly granted?", e)
                _uiState.update { it.copy(error = "Permission error while loading contacts.", isLoadingContacts = false, permissionGranted = false) }
            }
            catch (e: Exception) {
                Firebase.functions.logger.error("ContactsViewModel: Error loading phone contacts", e)
                _uiState.update { it.copy(error = "An unexpected error occurred while loading contacts.", isLoadingContacts = false) }
            }
        }
    }

    /**
     * Updates the ViewModel's state regarding contact permission.
     * Should be called by the UI layer after the user responds to the permission request.
     * @param isGranted True if the permission was granted.
     */
    fun setContactsPermissionGranted(isGranted: Boolean) {
        _uiState.update { it.copy(permissionGranted = isGranted) }
        if (!isGranted) {
            // Clear contacts and show error if permission is denied after being granted, or on initial denial.
            _uiState.update { it.copy(contacts = emptyList(), error = "Read contacts permission is required to use this feature.")}
        } else {
            // If granted, clear any previous permission-related error.
             _uiState.update { it.copy(error = if(it.error == "Read contacts permission is required to use this feature.") null else it.error) }
        }
    }

    /**
     * Calls a Cloud Function to check the SockApp status of the provided phone numbers.
     * Updates the [PhoneContact.sockAppStatus] for each contact in the UI state.
     * @param phoneNumbersToCheck Optional list of phone numbers to check. If null, checks all currently loaded contacts.
     */
    fun checkContactsSockAppStatus(phoneNumbersToCheck: List<String>? = null) {
        val currentContacts = uiState.value.contacts
        val numbersToQuery = phoneNumbersToCheck ?: currentContacts.map { it.phoneNumber }

        if (numbersToQuery.isEmpty()) {
            _uiState.update { it.copy(isLoadingSockStatuses = false) } // Nothing to check
            return
        }

        _uiState.update { it.copy(isLoadingSockStatuses = true) }
        // Mark relevant contacts as LOADING
        _uiState.update { currentState ->
            currentState.copy(contacts = currentState.contacts.map { contact ->
                if (numbersToQuery.contains(contact.phoneNumber)) contact.copy(sockAppStatus = SockAppUserStatus.LOADING) else contact
            })
        }

        viewModelScope.launch {
            try {
                // TODO: Implement pagination for `numbersToQuery` if it can exceed CF payload or processing limits (e.g., >20-30 numbers).
                // The CF 'checkPhoneNumbersSockStatus' also has a limit (e.g. 20).
                val limitedNumbersToQuery = numbersToQuery.take(20) // Adhere to CF limit

                val result = functions.getHttpsCallable("checkPhoneNumbersSockStatus")
                    .call(mapOf("phoneNumbers" to limitedNumbersToQuery))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val statusMap = result.data as? Map<String, Map<String, Any>> ?: emptyMap()

                _uiState.update { currentState ->
                    val updatedContacts = currentState.contacts.map { contact ->
                        val statusResult = statusMap[contact.phoneNumber]
                        if (statusResult != null) {
                            contact.copy(
                                sockAppStatus = if (statusResult["isUser"] == true) SockAppUserStatus.IS_USER else SockAppUserStatus.NOT_USER,
                                sockAppUserId = statusResult["userId"] as? String,
                                sockAppDisplayName = statusResult["displayName"] as? String // Or use username: statusResult["username"]
                            )
                        } else if (contact.sockAppStatus == SockAppUserStatus.LOADING) {
                            // If it was loading and no result, means it wasn't in this batch or failed in CF for this number
                            contact.copy(sockAppStatus = SockAppUserStatus.CHECK_FAILED)
                        } else {
                            contact // No change if not part of current query or already processed
                        }
                    }
                    currentState.copy(contacts = updatedContacts, isLoadingSockStatuses = false)
                }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ContactsViewModel: Error checking SockApp statuses", e)
                val errorMessage = if (e is FirebaseFunctionsException) e.message ?: "Failed to check contact statuses." else "An error occurred while checking contact statuses."
                _uiState.update { currentState ->
                     val errorHandledContacts = currentState.contacts.map { contact ->
                        // Revert status for contacts that were loading in this failed batch
                        if (contact.sockAppStatus == SockAppUserStatus.LOADING && numbersToQuery.contains(contact.phoneNumber)) {
                            contact.copy(sockAppStatus = SockAppUserStatus.CHECK_FAILED)
                        } else {
                            contact
                        }
                    }
                    currentState.copy(
                        error = errorMessage,
                        isLoadingSockStatuses = false,
                        contacts = errorHandledContacts
                    )
                }
            }
        }
    }

    /**
     * Clears any general error messages from the UI state.
     */
     fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
