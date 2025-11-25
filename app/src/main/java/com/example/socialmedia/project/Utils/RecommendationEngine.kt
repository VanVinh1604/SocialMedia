package com.example.socialmedia.project.Utils

import android.util.Log
import com.example.socialmedia.project.Domain.Model.ReelInteractionModel
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecommendationEngine(private val database: FirebaseDatabase) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val TAG = "RecommendationEngine"

    fun getRecommendedReels(callback: (List<ReelModel>) -> Unit) {
        if (currentUserId.isEmpty()) {
            callback(emptyList())
            return
        }

        getUserInteractions { interactions ->
            val hashtagScores = extractHashtagScores(interactions)
            val viewedReelIds = interactions.filter { it.interactionType == "VIEW" }.map { it.reelId }

            fetchAndScoreReels(viewedReelIds, hashtagScores, callback)
        }
    }

    private fun getUserInteractions(callback: (List<ReelInteractionModel>) -> Unit) {
        database.reference.child("ReelInteractions")
            .orderByChild("userId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ReelInteractionModel>()
                    for (child in snapshot.children) {
                        child.getValue(ReelInteractionModel::class.java)?.let { list.add(it) }
                    }
                    list.sortByDescending { it.timestamp }
                    callback(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Load interactions failed", error.toException())
                    callback(emptyList())
                }
            })
    }

    private fun extractHashtagScores(interactions: List<ReelInteractionModel>): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        interactions.forEach { interaction ->
            val weight = when (interaction.interactionType) {
                "LIKE" -> 10.0
                "COMMENT" -> 8.0
                "SHARE" -> 6.0
                "VIEW" -> 2.0
                else -> 0.0
            }
            interaction.hashtags.forEach { tag ->
                scores[tag] = scores.getOrDefault(tag, 0.0) + weight
            }
        }
        return scores
    }

    private fun fetchAndScoreReels(
        viewedIds: List<String>,
        hashtagScores: Map<String, Double>,
        callback: (List<ReelModel>) -> Unit
    ) {
        database.reference.child("Reels")
            .orderByChild("createdAt")
            .limitToLast(200)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val result = mutableListOf<ReelModel>()
                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java) ?: continue
                        if (reel.userId == currentUserId || viewedIds.contains(reel.reelId)) continue

                        val score = reel.hashtags.sumOf { hashtagScores.getOrDefault(it, 0.0) }
                            .plus(reel.likeCount * 0.1)
                            .plus(if ((System.currentTimeMillis() - reel.createdAt) < 24 * 60 * 60 * 1000) 50 else 0)

                        if (score > 5) result.add(reel)
                    }
                    result.shuffle()
                    callback(result.take(50))
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    /**
     * ✅ GHI NHẬN TƯƠNG TÁC - TỰ ĐỘNG FETCH HASHTAGS TỪ FIREBASE
     */
    fun recordInteraction(
        reelId: String,
        interactionType: String,
        duration: Long = 0
    ) {
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ User not logged in, skipping interaction")
            return
        }

        Log.d(TAG, "🔄 Recording $interactionType for reel: ${reelId.take(8)}...")

        // ✅ LUÔN FETCH HASHTAGS TỪ FIREBASE TRƯỚC KHI LƯU
        // ✅ FETCH TOÀN BỘ REEL ĐỂ KIỂM TRA
        database.reference.child("Reels").child(reelId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hashtags = mutableListOf<String>()

                    // Đọc hashtags từ Firebase
                    val hashtagsSnapshot = snapshot.child("hashtags")
                    for (child in hashtagsSnapshot.children) {
                        child.getValue(String::class.java)?.let { hashtags.add(it) }
                    }

                    // ✅ NẾU KHÔNG CÓ HASHTAGS → THỬ EXTRACT TỪ CAPTION
                    if (hashtags.isEmpty()) {
                        val caption = snapshot.child("caption").getValue(String::class.java)
                        Log.d(TAG, "⚠️ No hashtags found, caption: $caption")

                        if (!caption.isNullOrBlank()) {
                            val extractedHashtags = HashtagUtils.extractHashtags(caption)
                            if (extractedHashtags.isNotEmpty()) {
                                Log.d(TAG, "🔧 Extracted from caption: $extractedHashtags")
                                hashtags.addAll(extractedHashtags)

                                // Lưu hashtags vào Firebase luôn
                                snapshot.ref.child("hashtags").setValue(extractedHashtags)
                            }
                        }
                    }

                    Log.d(TAG, "📌 Final hashtags: $hashtags")
                    saveInteraction(reelId, interactionType, hashtags, duration)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Failed to fetch reel: ${error.message}")
                    saveInteraction(reelId, interactionType, emptyList(), duration)
                }
            })
    }

    /**
     * ✅ LƯU INTERACTION VÀO FIREBASE
     */
    private fun saveInteraction(
        reelId: String,
        interactionType: String,
        hashtags: List<String>,
        duration: Long
    ) {
        val interactionId = database.reference.child("ReelInteractions").push().key

        if (interactionId == null) {
            Log.e(TAG, "❌ Failed to generate interaction ID")
            return
        }

        // Sử dụng HashMap để đảm bảo hashtags được lưu đúng
        val interactionData = hashMapOf<String, Any>(
            "interactionId" to interactionId,
            "userId" to currentUserId,
            "reelId" to reelId,
            "interactionType" to interactionType,
            "hashtags" to hashtags,  // ✅ Lưu hashtags đã fetch
            "timestamp" to ServerValue.TIMESTAMP,
            "duration" to duration
        )

        database.reference
            .child("ReelInteractions")
            .child(interactionId)
            .setValue(interactionData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ SAVED: $interactionType")
                Log.d(TAG, "   ReelID: ${reelId.take(8)}...")
                Log.d(TAG, "   Hashtags: $hashtags")
                Log.d(TAG, "   InteractionID: ${interactionId.take(8)}...")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to save interaction", error)
            }
    }
    /**
     * 🔍 DEBUG: Xem điểm hashtag của user hiện tại
     */
    fun getHashtagScoresDebug(callback: (Map<String, Double>) -> Unit) {
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ User not logged in")
            callback(emptyMap())
            return
        }

        Log.d(TAG, "🔍 Fetching hashtag scores for user: ${currentUserId.take(8)}...")

        getUserInteractions { interactions ->
            val scores = extractHashtagScores(interactions)

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 HASHTAG SCORES BREAKDOWN:")
            Log.d(TAG, "   Total interactions: ${interactions.size}")
            Log.d(TAG, "   Unique hashtags: ${scores.size}")
            Log.d(TAG, "")

            // Sắp xếp theo điểm giảm dần
            scores.entries
                .sortedByDescending { it.value }
                .forEachIndexed { index, (tag, score) ->
                    val interactionCount = interactions.count { interaction ->
                        tag in interaction.hashtags
                    }
                    Log.d(TAG, "   ${index + 1}. #$tag")
                    Log.d(TAG, "      Score: $score")
                    Log.d(TAG, "      Interactions: $interactionCount times")
                    Log.d(TAG, "")
                }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

            callback(scores)
        }
    }
}