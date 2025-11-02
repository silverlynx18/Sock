package com.sock.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sock.app.data.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class GroupRepository {
    private val db = FirebaseFirestore.getInstance()
    private val groupsCollection = db.collection("groups")

    suspend fun getGroup(groupId: String): Result<Group?> {
        return try {
            val document = groupsCollection.document(groupId).get().await()
            val group = Group.fromDocument(document)
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGroup(groupId: String): Flow<Group?> = flow {
        groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                emit(null)
                return@addSnapshotListener
            }
            snapshot?.let {
                val group = Group.fromDocument(it)
                emit(group)
            }
        }
    }

    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val groupIds = userDoc.get("groups") as? List<*> ?: emptyList<Any>()
            
            val groups = groupIds.mapNotNull { groupId ->
                val groupDoc = groupsCollection.document(groupId.toString()).get().await()
                Group.fromDocument(groupDoc)
            }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
