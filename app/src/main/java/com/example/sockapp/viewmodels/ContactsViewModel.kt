package com.example.sockapp.viewmodels

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PhoneContact(
    val id: String,
    val name: String?,
    val phoneNumber: String, // Normalized E.164 if possible
    var sockAppStatus: SockAppUserStatus = SockAppUserStatus.UNKNOWN, // UNKNOWN, IS_USER, NOT_USER
    var sockAppUserId: String? = null, // If IS_USER
    var sockAppDisplayName: String? = null // If IS_USER
)

enum class SockAppUserStatus {
    UNKNOWN, // Status not yet checked
    LOADING, // Currently checking
    IS_USER, // Is a SockApp user
    NOT_USER, // Not a SockApp user
    CHECK_FAILED // API call failed for this contact
}

data class ContactsUiState(
    val contacts: List<PhoneContact> = emptyList(),
    val isLoadingContacts: Boolean = false,
    val error: String? = null,
    val permissionGranted: Boolean = false, // App needs to manage this via Android permission system
    val isLoadingSockStatuses: Boolean = false
)

class ContactsViewModel : ViewModel() {

    private val functions: FirebaseFunctions = Firebase.functions

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    // IMPORTANT: Contact loading requires READ_CONTACTS permission.
    // This ViewModel assumes permission is granted. UI layer must handle permission request.
    @SuppressLint("Range") // Suppress warning for cursor.getColumnIndex
    fun loadPhoneContacts(contentResolver: ContentResolver) {
        if (!uiState.value.permissionGranted) {
            _uiState.update { it.copy(error = "Read contacts permission not granted.") }
            // UI should observe permissionGranted and guide user to grant it.
            return
        }

        _uiState.update { it.copy(isLoadingContacts = true, error = null) }
        viewModelScope.launch {
            try {
                val loadedContacts = mutableListOf<PhoneContact>()
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
                        val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        // TODO: Normalize phone number to E.164 format before adding to list and sending to CF
                        loadedContacts.add(PhoneContact(id = id, name = name, phoneNumber = number))
                    }
                }
                _uiState.update { it.copy(contacts = loadedContacts, isLoadingContacts = false) }
                if (loadedContacts.isNotEmpty()) {
                    checkContactsSockAppStatus(loadedContacts.map {c -> c.phoneNumber })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load contacts: ${e.message}", isLoadingContacts = false) }
            }
        }
    }

    // To be called by UI after permission is granted by user.
    fun setContactsPermissionGranted(isGranted: Boolean) {
        _uiState.update { it.copy(permissionGranted = isGranted) }
        if (!isGranted) {
            _uiState.update { it.copy(contacts = emptyList(), error = "Permission denied. Cannot load contacts.")}
        }
    }

    fun checkContactsSockAppStatus(phoneNumbersToCheck: List<String>? = null) {
        val numbers = phoneNumbersToCheck ?: uiState.value.contacts.map { it.phoneNumber }
        if (numbers.isEmpty()) return

        _uiState.update { it.copy(isLoadingSockStatuses = true) }
        // Update individual contact statuses to LOADING
        _uiState.update { currentState ->
            currentState.copy(contacts = currentState.contacts.map { contact ->
                if (numbers.contains(contact.phoneNumber)) contact.copy(sockAppStatus = SockAppUserStatus.LOADING) else contact
            })
        }

        viewModelScope.launch {
            try {
                // TODO: Paginate if numbers list is very large (CFs have payload limits)
                val result = functions.getHttpsCallable("checkPhoneNumbersSockStatus")
                    .call(mapOf("phoneNumbers" to numbers))
                    .await()

                val statusMap = result.data as? Map<String, Map<String, Any>> ?: emptyMap()

                _uiState.update { currentState ->
                    val updatedContacts = currentState.contacts.map { contact ->
                        val statusResult = statusMap[contact.phoneNumber] // Assuming CF returns map keyed by phone number
                        if (statusResult != null) {
                            contact.copy(
                                sockAppStatus = if (statusResult["isUser"] == true) SockAppUserStatus.IS_USER else SockAppUserStatus.NOT_USER,
                                sockAppUserId = statusResult["userId"] as? String,
                                sockAppDisplayName = statusResult["displayName"] as? String
                            )
                        } else {
                            // If a number wasn't in the result, or some error for that specific number
                            contact.copy(sockAppStatus = SockAppUserStatus.CHECK_FAILED)
                        }
                    }
                    currentState.copy(contacts = updatedContacts, isLoadingSockStatuses = false)
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                     val errorHandledContacts = currentState.contacts.map { contact ->
                        if (contact.sockAppStatus == SockAppUserStatus.LOADING) contact.copy(sockAppStatus = SockAppUserStatus.CHECK_FAILED) else contact
                    }
                    currentState.copy(
                        error = "Failed to check SockApp statuses: ${e.message}",
                        isLoadingSockStatuses = false,
                        contacts = errorHandledContacts
                    )
                }
            }
        }
    }
     fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
