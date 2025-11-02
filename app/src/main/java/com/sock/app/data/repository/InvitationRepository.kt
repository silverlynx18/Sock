package com.sock.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sock.app.data.model.Invitation
import com.sock.app.data.model.InvitationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class InvitationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val invitationsCollection = db.collection("invitations")

    suspend fun getPendingInvitations(userId: String): Result<List<Invitation>> {
        return try {
            val query = invitationsCollection
                .whereEqualTo("invitedUserID", userId)
                .whereEqualTo("status", "pending_acceptance")
                .get()
                .await()
            
            val invitations = query.documents.mapNotNull { doc ->
                Invitation.fromDocument(doc)
            }
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePendingInvitations(userId: String): Flow<List<Invitation>> = flow {
        invitationsCollection
            .whereEqualTo("invitedUserID", userId)
            .whereEqualTo("status", "pending_acceptance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    emit(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val invitations = it.documents.mapNotNull { doc ->
                        Invitation.fromDocument(doc)
                    }
                    emit(invitations)
                }
            }
    }

    suspend fun getInvitation(invitationId: String): Result<Invitation?> {
        return try {
            val document = invitationsCollection.document(invitationId).get().await()
            val invitation = Invitation.fromDocument(document)
            Result.success(invitation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
