package com.example.sockapp.data.models.group

enum class GroupRole(val level: Int, val displayName: String) {
    OWNER(3, "Owner"),
    ADMIN(2, "Admin"),
    MODERATOR(1, "Moderator"),
    MEMBER(0, "Member");

    fun isAtLeast(role: GroupRole): Boolean {
        return this.level >= role.level
    }

    fun canManageSettings(): Boolean = this.isAtLeast(ADMIN)
    fun canModerateContent(): Boolean = this.isAtLeast(MODERATOR)

    fun canRemoveMember(memberToRemoveRole: GroupRole, currentUserIsOwner: Boolean): Boolean {
        return when (this) {
            OWNER -> memberToRemoveRole != OWNER // Owner can remove anyone but another owner (requires explicit ownership transfer)
            ADMIN -> memberToRemoveRole.level < ADMIN.level // Admin can remove moderators and members
            MODERATOR -> memberToRemoveRole.level < MODERATOR.level // Moderator can remove members (if feature enabled)
            MEMBER -> false
        }
    }

    fun canPromoteTo(targetRole: GroupRole): Boolean {
        if (this == OWNER && targetRole != OWNER) return true // Owner can promote to any role below Owner
        if (this == ADMIN && targetRole.level < ADMIN.level && targetRole != OWNER) return true // Admin can promote to Moderator or Member
        return false
    }

    fun canDemoteFrom(currentRoleBeingDemoted: GroupRole): Boolean {
        if (currentRoleBeingDemoted == OWNER) return false // No one can demote an owner except through ownership transfer
        if (this == OWNER) return true // Owner can demote Admin, Moderator, Member
        if (this == ADMIN && currentRoleBeingDemoted.level < ADMIN.level) return true // Admin can demote Moderator, Member
        return false
    }

    companion object {
        fun fromString(roleString: String?): GroupRole {
            return entries.find { it.name.equals(roleString, ignoreCase = true) } ?: MEMBER
        }
    }
}
