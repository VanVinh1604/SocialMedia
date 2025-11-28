package com.example.socialmedia.project.Utils

import android.util.Log
import com.example.socialmedia.project.Domain.Model.ReelInteractionModel
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.pow

class RecommendationEngine(private val database: FirebaseDatabase) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val TAG = "RecommendationEngine"

    // 🎯 HÀM CHÍNH: LẤY DANH SÁCH ĐỀ XUẤT
    fun getRecommendedReels(callback: (List<ReelModel>) -> Unit) {
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ User not logged in")
            callback(emptyList())
            return
        }

        Log.d(TAG, "🔄 Getting recommendations for user: ${currentUserId.take(8)}...")

        getUserInteractions { interactions ->
            if (interactions.isEmpty()) {
                Log.w(TAG, "⚠️ No interactions found, returning empty list")
                callback(emptyList())
                return@getUserInteractions
            }

            val hashtagScores = extractHashtagScores(interactions)
            val viewedReelIds = interactions.filter { it.interactionType == "VIEW" }.map { it.reelId }

            Log.d(TAG, "📊 Hashtag scores calculated: ${hashtagScores.size} tags")
            Log.d(TAG, "👁️ Viewed ${viewedReelIds.size} reels")

            fetchAndScoreReels(viewedReelIds, hashtagScores, callback)
        }
    }

    // 📥 LẤY TƯƠNG TÁC CỦA USER
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
                    Log.d(TAG, "✅ Loaded ${list.size} interactions")
                    callback(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Load interactions failed", error.toException())
                    callback(emptyList())
                }
            })
    }

    // 🧮 TÍNH ĐIỂM CHO TỪNG HASHTAG (CÓ TIME DECAY) - ✅ FIXED
    private fun extractHashtagScores(interactions: List<ReelInteractionModel>): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        val now = System.currentTimeMillis()

        interactions.forEach { interaction ->
            // 🎯 ĐIỂM GỐC THEO LOẠI TƯƠNG TÁC - ✅ TĂNG TRỌNG SỐ
            val baseWeight = when (interaction.interactionType) {
                "LIKE" -> 20.0        // ← Tăng từ 10.0
                "COMMENT" -> 15.0     // ← Tăng từ 8.0
                "SHARE" -> 12.0       // ← Tăng từ 6.0
                "WATCH_TIME" -> {
                    // Xem > 15 giây = video hay
                    if (interaction.duration > 15000) 10.0 else 2.0  // ← Tăng từ 5.0/1.0
                }
                "VIEW" -> 2.0

                // ❌ TÍN HIỆU TIÊU CỰC - ✅ TĂNG PENALTY
                "HIDE" -> -30.0       // ← Tăng từ -15.0
                "SKIP" -> -5.0        // ← Tăng từ -3.0

                else -> 0.0
            }

            // ⏳ TIME DECAY: Tương tác càng cũ càng ít giá trị - ✅ GIẢM TỐC ĐỘ DECAY
            // Mỗi 7 ngày giảm 5% (0.95^(days/7)) thay vì 10%
            val daysSince = (now - interaction.timestamp) / (1000.0 * 60 * 60 * 24)
            val decayFactor = 0.95.pow(daysSince / 7.0)  // ← Đổi từ 0.9

            // 🎯 ĐIỂM CUỐI = ĐIỂM GỐC × HỆ SỐ GIẢM
            val finalWeight = baseWeight * decayFactor

            // 📊 CỘNG ĐIỂM CHO TỪNG HASHTAG
            interaction.hashtags.forEach { tag ->
                scores[tag] = scores.getOrDefault(tag, 0.0) + finalWeight
            }
        }

        // 📋 LOG TOP 10 HASHTAGS
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📊 TOP HASHTAG SCORES:")
        scores.entries
            .sortedByDescending { it.value }
            .take(10)
            .forEachIndexed { index, (tag, score) ->
                Log.d(TAG, "   ${index + 1}. #$tag = %.2f".format(score))
            }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return scores
    }

    // 🎬 FETCH REELS VÀ TÍNH ĐIỂM - ✅ FIXED
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
                    val allReels = mutableListOf<ReelModel>()
                    val recommendedReels = mutableListOf<Pair<ReelModel, Double>>()

                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java) ?: continue
                        allReels.add(reel)

                        // 🚫 BỎ QUA VIDEO CỦA CHÍNH USER HOẶC ĐÃ XEM


                        // 🧮 TÍNH ĐIỂM
                        val score = calculateReelScore(reel, hashtagScores, viewedIds)

                        // ✅ CHỈ THÊM NẾU ĐIỂM > 20 - ✅ TĂNG THRESHOLD
                        if (score > 20.0) {  // ← Tăng từ 5.0
                            recommendedReels.add(reel to score)
                        }
                    }

                    // 🎲 TRỘN VỚI VIDEO NGẪU NHIÊN (90% recommended + 10% random) - ✅ TĂNG TỈ LỆ
                    val finalReels = mixRecommendations(
                        recommendedReels.sortedByDescending { it.second }.map { it.first },
                        allReels
                    )

                    Log.d(TAG, "✅ Final: ${finalReels.size} reels (${recommendedReels.size} recommended + random)")
                    callback(finalReels)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Fetch reels failed", error.toException())
                    callback(emptyList())
                }
            })
    }

    // 📦 LOAD VIDEO THEO BATCH (20 VIDEO MỖI LẦN)
    fun getRecommendedReelsBatch(batchSize: Int, callback: (List<ReelModel>) -> Unit) {
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ User not logged in")
            callback(emptyList())
            return
        }

        Log.d(TAG, "📦 Getting batch of $batchSize recommendations...")

        getUserInteractions { interactions ->
            if (interactions.isEmpty()) {
                Log.w(TAG, "⚠️ No interactions, loading random batch")
                loadRandomReelsBatch(batchSize, callback)
                return@getUserInteractions
            }

            val hashtagScores = extractHashtagScores(interactions)
            val viewedReelIds = interactions.filter { it.interactionType == "VIEW" }.map { it.reelId }

            fetchAndScoreReelsBatch(viewedReelIds, hashtagScores, batchSize, callback)
        }
    }

    // 📦 FETCH BATCH - ✅ FIXED
    private fun fetchAndScoreReelsBatch(
        viewedIds: List<String>,
        hashtagScores: Map<String, Double>,
        batchSize: Int,
        callback: (List<ReelModel>) -> Unit
    ) {
        database.reference.child("Reels")
            .orderByChild("createdAt")
            .limitToLast(200)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allReels = mutableListOf<ReelModel>()
                    val recommendedReels = mutableListOf<Pair<ReelModel, Double>>()

                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java) ?: continue
                        allReels.add(reel)

                        // 🚫 Bỏ qua video của user hoặc đã xem
                        if (reel.userId == currentUserId || viewedIds.contains(reel.reelId)) {
                            continue
                        }

                        // 🧮 Tính điểm
                        val score = calculateReelScore(reel, hashtagScores, viewedIds)
                        if (score > 20.0) {  // ← Tăng từ 5.0
                            recommendedReels.add(reel to score)
                        }
                    }

                    // 📊 TỈ LỆ 90/10 - ✅ TĂNG TỈ LỆ RECOMMENDED
                    val numRecommended = (batchSize * 0.9).toInt()  // ← Tăng từ 0.8
                    val numRandom = batchSize - numRecommended

                    // 🎯 Lấy video đề xuất (sắp xếp theo điểm)
                    val recommendedPart = recommendedReels
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .take(numRecommended)

                    // 🎲 Lấy video ngẫu nhiên
                    val randomPart = allReels
                        .filter { it !in recommendedPart && !viewedIds.contains(it.reelId) && it.userId != currentUserId }
                        .shuffled()
                        .take(numRandom)

                    // 🔀 Trộn và xáo
                    val finalBatch = (recommendedPart + randomPart).shuffled()

                    Log.d(TAG, "✅ Batch: ${recommendedPart.size} recommended + ${randomPart.size} random = ${finalBatch.size}")
                    callback(finalBatch)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Fetch failed", error.toException())
                    callback(emptyList())
                }
            })
    }

    // 🎲 LOAD VIDEO NGẪU NHIÊN (KHI USER CHƯA CÓ TƯƠNG TÁC)
    private fun loadRandomReelsBatch(batchSize: Int, callback: (List<ReelModel>) -> Unit) {
        database.reference.child("Reels")
            .orderByChild("createdAt")
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reels = mutableListOf<ReelModel>()
                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java)
                        reel?.let {
                            if (it.userId != currentUserId) {
                                reels.add(it)
                            }
                        }
                    }

                    val randomBatch = reels.shuffled().take(batchSize)
                    Log.d(TAG, "🎲 Loaded ${randomBatch.size} random reels")
                    callback(randomBatch)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Load random failed", error.toException())
                    callback(emptyList())
                }
            })
    }

    // 🧮 TÍNH ĐIỂM CHO 1 VIDEO - ✅ FIXED
    private fun calculateReelScore(
        reel: ReelModel,
        hashtagScores: Map<String, Double>,
        viewedIds: List<String>
    ): Double {

        // 1️⃣ ĐIỂM TỪ HASHTAG (Điểm chính) - ✅ TRỌNG SỐ ĐÃ TĂNG
        val hashtagScore = reel.hashtags.sumOf {
            hashtagScores.getOrDefault(it, 0.0)
        }

        // 2️⃣ ĐIỂM PHỔ BIẾN (Video nhiều tương tác)
        val popularityScore = (reel.likeCount * 0.5) +
                (reel.commentCount * 0.3) +
                (reel.shareCount * 0.2)

        // 3️⃣ ĐIỂM MỚI (Video càng mới càng ưu tiên) - ✅ GIẢM BONUS
        val hoursSincePost = (System.currentTimeMillis() - reel.createdAt) / (1000 * 60 * 60)
        val recencyScore = when {
            hoursSincePost < 24 -> 15.0   // ← Giảm từ 50.0
            hoursSincePost < 72 -> 8.0    // ← Giảm từ 25.0
            hoursSincePost < 168 -> 3.0   // ← Giảm từ 10.0
            else -> 0.0
        }

        // 4️⃣ ĐIỂM ĐA DẠNG (Tránh 1 hashtag chiếm hết)
        val uniqueMatchedHashtags = reel.hashtags.intersect(hashtagScores.keys).size
        val diversityBonus = if (uniqueMatchedHashtags > 0) 5.0 else 0.0

        // 🎯 TỔNG ĐIỂM
        val totalScore = hashtagScore + popularityScore + recencyScore + diversityBonus

        // 📋 LOG CHI TIẾT
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, """
        📊 Score for ${reel.reelId.take(8)}:
           Hashtags: ${reel.hashtags}
           Hashtag Score: %.2f
           Popularity: %.2f
           Recency: %.2f (${hoursSincePost}h)
           Diversity: %.2f
           → TOTAL: %.2f
    """.trimIndent().format(hashtagScore, popularityScore, recencyScore, diversityBonus, totalScore))
        }

        return totalScore
    }

    // 🎲 TRỘN VIDEO ĐỀ XUẤT VỚI VIDEO NGẪU NHIÊN - ✅ FIXED
    private fun mixRecommendations(
        recommendedReels: List<ReelModel>,
        allReels: List<ReelModel>
    ): List<ReelModel> {

        // 📊 TỶ LỆ: 90% đề xuất + 10% ngẫu nhiên - ✅ TĂNG TỈ LỆ RECOMMENDED
        val exploitationRatio = 0.9  // ← Tăng từ 0.7
        val targetSize = 50

        val numRecommended = (targetSize * exploitationRatio).toInt()
        val numRandom = targetSize - numRecommended

        // 🎯 LẤY VIDEO ĐỀ XUẤT
        val recommendedPart = recommendedReels.take(numRecommended)

        // 🎲 LẤY VIDEO NGẪU NHIÊN (không trùng)
        val randomPart = allReels
            .filter { it !in recommendedReels }
            .shuffled()
            .take(numRandom)

        // 🔀 TRỘN VÀ XÁO
        val result = (recommendedPart + randomPart).shuffled()

        Log.d(TAG, "🎲 Mixed: ${recommendedPart.size} recommended + ${randomPart.size} random = ${result.size} total")

        return result
    }

    // 💾 GHI NHẬN TƯƠNG TÁC
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

        // ✅ FETCH HASHTAGS TỪ FIREBASE
        database.reference.child("Reels").child(reelId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hashtags = mutableListOf<String>()

                    // Đọc hashtags
                    val hashtagsSnapshot = snapshot.child("hashtags")
                    for (child in hashtagsSnapshot.children) {
                        child.getValue(String::class.java)?.let { hashtags.add(it) }
                    }

                    // ⚠️ NẾU KHÔNG CÓ → EXTRACT TỪ CAPTION
                    if (hashtags.isEmpty()) {
                        val caption = snapshot.child("caption").getValue(String::class.java)
                        if (!caption.isNullOrBlank()) {
                            val extractedHashtags = HashtagUtils.extractHashtags(caption)
                            if (extractedHashtags.isNotEmpty()) {
                                hashtags.addAll(extractedHashtags)
                                snapshot.ref.child("hashtags").setValue(extractedHashtags)
                                Log.d(TAG, "🔧 Extracted hashtags from caption: $extractedHashtags")
                            }
                        }
                    }

                    Log.d(TAG, "📌 Hashtags: $hashtags")
                    saveInteraction(reelId, interactionType, hashtags, duration)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Failed to fetch reel: ${error.message}")
                    saveInteraction(reelId, interactionType, emptyList(), duration)
                }
            })
    }

    // 💾 LƯU INTERACTION VÀO FIREBASE
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

        val interactionData = hashMapOf<String, Any>(
            "interactionId" to interactionId,
            "userId" to currentUserId,
            "reelId" to reelId,
            "interactionType" to interactionType,
            "hashtags" to hashtags,
            "timestamp" to ServerValue.TIMESTAMP,
            "duration" to duration
        )

        database.reference
            .child("ReelInteractions")
            .child(interactionId)
            .setValue(interactionData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ SAVED: $interactionType for ${reelId.take(8)}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "❌ Failed to save interaction", error)
            }
    }

    // 🐛 DEBUG: XEM ĐIỂM HASHTAG
    fun getHashtagScoresDebug(callback: (Map<String, Double>) -> Unit) {
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "⚠️ User not logged in")
            callback(emptyMap())
            return
        }

        getUserInteractions { interactions ->
            val scores = extractHashtagScores(interactions)

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 HASHTAG SCORES BREAKDOWN:")
            Log.d(TAG, "   Total interactions: ${interactions.size}")
            Log.d(TAG, "   Unique hashtags: ${scores.size}")

            scores.entries
                .sortedByDescending { it.value }
                .forEachIndexed { index, (tag, score) ->
                    val count = interactions.count { tag in it.hashtags }
                    Log.d(TAG, "   ${index + 1}. #$tag")
                    Log.d(TAG, "      Score: %.2f".format(score))
                    Log.d(TAG, "      Times: $count")
                }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

            callback(scores)
        }
    }
}