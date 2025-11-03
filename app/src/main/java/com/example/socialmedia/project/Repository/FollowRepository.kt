package com.example.socialmedia.project.Repository

import android.util.Log
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

class FollowRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid

    /**
     * Lấy danh sách ID (followers hoặc following)
     */
    private suspend fun getFollowIds(userId: String, type: String): List<String> {
        return try {
            val snapshot = db.child("Follow").child(userId).child(type).get().await()
            snapshot.children.mapNotNull { it.key }
        } catch (e: Exception) {
            Log.e("FollowRepository", "Lỗi lấy ID: ${e.message}")
            emptyList()
        }
    }

    /**
     * [MỚI] Lấy danh sách ID NHỮNG NGƯỜI MÀ TÔI (user hiện tại) ĐANG FOLLOW
     * Dùng Set để kiểm tra ".contains()" nhanh hơn
     */
    suspend fun getMyFollowingIdsSet(): Set<String> = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext emptySet()
        try {
            val snapshot = db.child("Follow").child(uid).child("following").get().await()
            // Trả về một Set<String>
            return@withContext snapshot.children.mapNotNull { it.key }.toSet()
        } catch (e: Exception) {
            Log.e("FollowRepository", "Lỗi lấy MyFollowingIds: ${e.message}")
            return@withContext emptySet()
        }
    }


    /**
     * Lấy chi tiết UserModel từ một ID
     */
    private suspend fun getUserDetails(userId: String): UserModel? {
        return try {
            val snapshot = db.child("InfoUser").child(userId).get().await()
            snapshot.getValue(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("FollowRepository", "Lỗi lấy User: ${e.message}")
            null
        }
    }

    /**
     * LYY Lấy danh sách UserModel đầy đủ (cho Followers/Following)
     */
    suspend fun getFollowList(userId: String, type: String): List<UserModel> = withContext(Dispatchers.IO) {
        try {
            // 1. Lấy danh sách ID
            val userIds = getFollowIds(userId, type)
            if (userIds.isEmpty()) {
                return@withContext emptyList()
            }

            // 2. Lấy chi tiết cho từng ID
            val userList = userIds.mapNotNull { id ->
                getUserDetails(id)
            }
            return@withContext userList

        } catch (e: Exception) {
            Log.e("FollowRepository", "Lỗi getFollowList: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * [MỚI] Hàm để Follow một người
     */
    suspend fun followUser(targetUserId: String) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        if (uid == targetUserId) return@withContext // Không thể tự follow

        // === [SỬA LỖI] THÊM KIỂM TRA TRÙNG ===
        // 1. Kiểm tra xem đã follow người này chưa
        val followingRef = db.child("Follow").child(uid).child("following").child(targetUserId)
        val snapshot = followingRef.get().await()

        if (snapshot.exists() && snapshot.value == true) {
            // Nếu đã tồn tại (snapshot.exists() == true), nghĩa là đã follow
            Log.w("FollowRepository", "Đã follow người này rồi ($targetUserId), không thực hiện lại.")
            return@withContext // Dừng hàm, không làm gì cả
        }
        // === KẾT THÚC SỬA LỖI ===

        // 2. Thêm targetId vào "following" của user hiện tại
        followingRef.setValue(true).await()

        // 3. Thêm user hiện tại vào "followers" của target
        db.child("Follow").child(targetUserId).child("followers").child(uid).setValue(true).await()

        // 4. Cập nhật count (dùng transaction)
        updateFollowCount(uid, "followingCount", 1)
        updateFollowCount(targetUserId, "followerCount", 1)
    }

    /**
     * [MỚI] Hàm để Unfollow một người
     */
    suspend fun unfollowUser(targetUserId: String) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext

        // === [SỬA LỖI] THÊM KIỂM TRA TRÙNG ===
        // 1. Kiểm tra xem có đang follow người này không
        val followingRef = db.child("Follow").child(uid).child("following").child(targetUserId)
        val snapshot = followingRef.get().await()

        if (!snapshot.exists()) {
            // Nếu không tồn tại, nghĩa là chưa follow (hoặc đã unfollow rồi)
            Log.w("FollowRepository", "Chưa follow người này ($targetUserId), không thực hiện unfollow.")
            return@withContext // Dừng hàm
        }
        // === KẾT THÚC SỬA LỖI ===

        // 2. Xóa targetId khỏi "following" của user hiện tại
        followingRef.removeValue().await()

        // 3. Xóa user hiện tại khỏi "followers" của target
        db.child("Follow").child(targetUserId).child("followers").child(uid).removeValue().await()

        // 4. Cập nhật count
        updateFollowCount(uid, "followingCount", -1)
        updateFollowCount(targetUserId, "followerCount", -1)
    }

    /**
     * [MỚI] Hàm Transaction để cập nhật count
     */
    private fun updateFollowCount(userId: String, field: String, delta: Int) {
        val ref = db.child("InfoUser").child(userId).child(field)

        ref.runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val count = currentData.getValue(Int::class.java) ?: 0
                currentData.value = max(0, count + delta) // Đảm bảo không âm
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("FollowRepository", "Lỗi transaction $field: ${error.message}")
                }
            }
        })
    }
}

