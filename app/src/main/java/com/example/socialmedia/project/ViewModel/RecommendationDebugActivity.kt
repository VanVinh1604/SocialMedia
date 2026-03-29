package com.example.socialmedia.project.Activity

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.example.socialmedia.project.Utils.RecommendationEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class RecommendationDebugActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var recommendationEngine: RecommendationEngine
    private lateinit var scrollView: ScrollView
    private lateinit var tvResults: TextView
    private lateinit var progressBar: ProgressBar

    private val TAG = "RecommendationDebug"
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation_debug)

        database = FirebaseDatabase.getInstance()
        recommendationEngine = RecommendationEngine(database)

        initViews()
        setupButtons()

    }

    private fun initViews() {
        scrollView = findViewById(R.id.scrollViewResults)
        tvResults = findViewById(R.id.tvDebugResults)
        progressBar = findViewById(R.id.progressBar)

        tvResults.setTextIsSelectable(true)
    }

    private fun setupButtons() {
        // 🔍 1. Xem hashtag scores
        findViewById<Button>(R.id.btnHashtagScores).setOnClickListener {
            clearLog()
            showHashtagScores()
        }

        // 📊 2. Xem tất cả interactions
        findViewById<Button>(R.id.btnViewInteractions).setOnClickListener {
            clearLog()
            showAllInteractions()
        }

        // 🎯 3. Test recommendation (chi tiết)
        findViewById<Button>(R.id.btnTestRecommendation).setOnClickListener {
            clearLog()
            testRecommendationDetailed()
        }

        // 📈 4. Phân tích từng video được scoring
        findViewById<Button>(R.id.btnAnalyzeScoring).setOnClickListener {
            clearLog()
            analyzeVideoScoring()
        }

        // 🎲 5. So sánh với random
        findViewById<Button>(R.id.btnCompareRandom).setOnClickListener {
            clearLog()
            compareRecommendedVsRandom()
        }

        // 📋 6. Xuất report đầy đủ
        findViewById<Button>(R.id.btnFullReport).setOnClickListener {
            clearLog()
            generateFullReport()
        }

        // 🗑️ 7. Clear log
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearLog()
        }
    }



    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🔍 1. XEM HASHTAG SCORES
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun showHashtagScores() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("🔍 HASHTAG SCORES ANALYSIS")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showProgress()

        recommendationEngine.getHashtagScoresDebug { scores ->
            hideProgress()

            if (scores.isEmpty()) {
                appendLog("⚠️ Không có hashtag scores (chưa có tương tác)")
                return@getHashtagScoresDebug
            }

            appendLog("\n📊 TỔNG QUAN:")
            appendLog("   Tổng hashtags: ${scores.size}")
            appendLog("   Tổng điểm: %.2f".format(scores.values.sum()))
            appendLog("")

            // Phân loại hashtags
            val high = scores.filter { it.value >= 15 }
            val medium = scores.filter { it.value in 5.0..14.99 }
            val low = scores.filter { it.value in 0.01..4.99 }
            val negative = scores.filter { it.value <= 0 }

            appendLog("🔥 HIGH SCORE (≥15 điểm): ${high.size}")
            high.entries.sortedByDescending { it.value }.forEach { (tag, score) ->
                appendLog("   #$tag → %.2f điểm".format(score))
            }

            appendLog("\n📈 MEDIUM SCORE (5-15 điểm): ${medium.size}")
            medium.entries.sortedByDescending { it.value }.forEach { (tag, score) ->
                appendLog("   #$tag → %.2f điểm".format(score))
            }

            appendLog("\n📉 LOW SCORE (0-5 điểm): ${low.size}")
            low.entries.sortedByDescending { it.value }.forEach { (tag, score) ->
                appendLog("   #$tag → %.2f điểm".format(score))
            }

            if (negative.isNotEmpty()) {
                appendLog("\n⛔ NEGATIVE SCORE (≤0 điểm): ${negative.size}")
                negative.entries.sortedByDescending { it.value }.forEach { (tag, score) ->
                    appendLog("   #$tag → %.2f điểm".format(score))
                }
            }



        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 📊 2. XEM TẤT CẢ INTERACTIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun showAllInteractions() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("📊 USER INTERACTIONS HISTORY")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showProgress()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            hideProgress()
            appendLog("❌ Chưa đăng nhập!")
            return
        }

        database.reference.child("ReelInteractions")
            .orderByChild("userId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideProgress()

                    val interactions = snapshot.children.toList()
                    appendLog("\n📈 Tổng số tương tác: ${interactions.size}")
                    appendLog("")

                    // Phân loại theo type
                    val byType = interactions.groupBy {
                        it.child("interactionType").getValue(String::class.java) ?: "UNKNOWN"
                    }

                    byType.forEach { (type, list) ->
                        appendLog("$type: ${list.size} lần")
                    }

                    appendLog("\n━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLog("📋 CHI TIẾT (10 gần nhất):")
                    appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    interactions
                        .sortedByDescending {
                            it.child("timestamp").getValue(Long::class.java) ?: 0L
                        }
                        .take(10)
                        .forEachIndexed { index, snapshot ->
                            val type = snapshot.child("interactionType").getValue(String::class.java)
                            val reelId = snapshot.child("reelId").getValue(String::class.java)
                            val hashtags = mutableListOf<String>()
                            snapshot.child("hashtags").children.forEach {
                                it.getValue(String::class.java)?.let { tag -> hashtags.add(tag) }
                            }

                            appendLog("\n${index + 1}. $type")
                            appendLog("   ReelID: ${reelId?.take(8)}...")
                            appendLog("   Hashtags: $hashtags")
                        }

                    appendLog("")
                }

                override fun onCancelled(error: DatabaseError) {
                    hideProgress()
                    appendLog("❌ Lỗi: ${error.message}")
                }
            })
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🎯 3. TEST RECOMMENDATION CHI TIẾT
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun testRecommendationDetailed() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("🎯 RECOMMENDATION TEST (CHI TIẾT)")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showProgress()

        recommendationEngine.getRecommendedReels { reels ->
            hideProgress()

            if (reels.isEmpty()) {
                appendLog("⚠️ Không có đề xuất (có thể chưa có interactions)")
                return@getRecommendedReels
            }

            appendLog("\n✅ Nhận được ${reels.size} videos")
            appendLog("")

            // Đếm hashtags xuất hiện
            val hashtagCount = mutableMapOf<String, Int>()
            reels.forEach { reel ->
                reel.hashtags.forEach { tag ->
                    hashtagCount[tag] = hashtagCount.getOrDefault(tag, 0) + 1
                }
            }

            appendLog("📊 HASHTAG DISTRIBUTION:")
            hashtagCount.entries
                .sortedByDescending { it.value }
                .forEach { (tag, count) ->
                    val percentage = (count * 100.0 / reels.size)
                    appendLog("   #$tag: $count lần (%.1f%%)".format(percentage))
                }

            appendLog("\n━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLog("🎬 TOP 10 RECOMMENDED VIDEOS:")
            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━")

            reels.take(10).forEachIndexed { index, reel ->
                appendLog("\n${index + 1}. ${reel.caption?.take(40) ?: "No caption"}")
                appendLog("   ID: ${reel.reelId.take(8)}...")
                appendLog("   Hashtags: ${reel.hashtags}")
                appendLog("   Likes: ${reel.likeCount}, Comments: ${reel.commentCount}")

                val hoursSincePost = (System.currentTimeMillis() - reel.createdAt) / (1000 * 60 * 60)
                appendLog("   Tuổi: ${hoursSincePost}h")
            }

            appendLog("")
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 📈 4. PHÂN TÍCH SCORING CHI TIẾT
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 📈 4. PHÂN TÍCH SCORING CHI TIẾT - ✅ CẬP NHẬT DECAY
    private fun analyzeVideoScoring() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("📈 DETAILED SCORING ANALYSIS (WITH STRONG DECAY)")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showProgress()

        // Lấy hashtag scores trước
        recommendationEngine.getHashtagScoresDebug { hashtagScores ->

            // Lấy sample 10 videos để phân tích
            database.reference.child("Reels")
                .orderByChild("createdAt")
                .limitToLast(15)  // Tăng lên 15 để test decay
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        hideProgress()

                        appendLog("\n🔍 Phân tích 15 videos với TIME DECAY MẠNH:\n")

                        snapshot.children.forEach { child ->
                            val reel = child.getValue(ReelModel::class.java) ?: return@forEach

                            // 🎯 TÍNH ĐIỂM THEO CÔNG THỨC MỚI
                            val hoursSincePost = (System.currentTimeMillis() - reel.createdAt) / (1000 * 60 * 60)
                            val daysSincePost = hoursSincePost / 24.0

                            // Hệ số decay
                            val recencyDecay = when {
                                daysSincePost < 1.0 -> 1.0
                                daysSincePost < 3.0 -> 0.7
                                daysSincePost < 5.0 -> 0.4
                                daysSincePost < 7.0 -> 0.2
                                daysSincePost < 14.0 -> 0.1
                                else -> 0.05
                            }

                            val hashtagScore = reel.hashtags.sumOf {
                                hashtagScores.getOrDefault(it, 0.0)
                            }

                            val popularityScore = (reel.likeCount * 0.5) +
                                    (reel.commentCount * 0.3) +
                                    (reel.shareCount * 0.2)

                            // Bonus cho video mới
                            val recencyBonus = when {
                                hoursSincePost < 12 -> 8.0
                                hoursSincePost < 24 -> 5.0
                                hoursSincePost < 48 -> 3.0
                                hoursSincePost < 72 -> 1.0
                                else -> 0.0
                            }

                            val decayedHashtagScore = hashtagScore * recencyDecay
                            val decayedPopularityScore = popularityScore * recencyDecay
                            val diversityBonus = if (reel.hashtags.intersect(hashtagScores.keys).isNotEmpty())
                                5.0 * recencyDecay else 0.0

                            val totalScore = decayedHashtagScore + decayedPopularityScore + recencyBonus + diversityBonus

                            appendLog("📹 ${reel.caption?.take(30) ?: "No caption"}")
                            appendLog("   Tuổi: ${daysSincePost.toInt()} ngày (${hoursSincePost.toInt()}h)")
                            appendLog("   Hashtags: ${reel.hashtags}")
                            appendLog("   ├─ Hashtag Score: %.2f → %.2f (decay %.0f%%)".format(
                                hashtagScore, decayedHashtagScore, recencyDecay * 100))
                            appendLog("   ├─ Popularity: %.2f → %.2f (decay %.0f%%)".format(
                                popularityScore, decayedPopularityScore, recencyDecay * 100))
                            appendLog("   ├─ Recency Bonus: %.2f".format(recencyBonus))
                            appendLog("   ├─ Diversity: %.2f".format(diversityBonus))
                            appendLog("   └─ TOTAL: %.2f".format(totalScore))

                            // 🎯 THRESHOLD MỚI CHO DECAY
                            val threshold = if (daysSincePost < 3) 20.0 else if (daysSincePost < 7) 30.0 else 50.0

                            if (totalScore > threshold) {
                                appendLog("   ✅ PASS (>${threshold.toInt()}) - SẼ TOP")
                            } else {
                                appendLog("   ❌ FAIL (≤${threshold.toInt()}) - BỊ LỌC")
                            }
                            appendLog("")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        hideProgress()
                        appendLog("❌ Lỗi: ${error.message}")
                    }
                })
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🎲 5. SO SÁNH VỚI RANDOM
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun compareRecommendedVsRandom() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("🎲 RECOMMENDED vs RANDOM")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        showProgress()

        // Lấy recommended
        recommendationEngine.getRecommendedReels { recommended ->

            // Lấy random
            database.reference.child("Reels")
                .limitToLast(50)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        hideProgress()

                        val random = snapshot.children.mapNotNull {
                            it.getValue(ReelModel::class.java)
                        }.shuffled().take(50)

                        // So sánh hashtag distribution
                        val recommendedHashtags = mutableMapOf<String, Int>()
                        val randomHashtags = mutableMapOf<String, Int>()

                        recommended.forEach { reel ->
                            reel.hashtags.forEach { tag ->
                                recommendedHashtags[tag] = recommendedHashtags.getOrDefault(tag, 0) + 1
                            }
                        }

                        random.forEach { reel ->
                            reel.hashtags.forEach { tag ->
                                randomHashtags[tag] = randomHashtags.getOrDefault(tag, 0) + 1
                            }
                        }

                        appendLog("\n📊 SO SÁNH TOP 10 HASHTAGS:\n")
                        appendLog("%-20s %10s %10s".format("Hashtag", "Recommend", "Random"))
                        appendLog("━".repeat(42))

                        val allTags = (recommendedHashtags.keys + randomHashtags.keys).distinct()
                        allTags.sortedByDescending { recommendedHashtags.getOrDefault(it, 0) }
                            .take(10)
                            .forEach { tag ->
                                val recCount = recommendedHashtags.getOrDefault(tag, 0)
                                val randCount = randomHashtags.getOrDefault(tag, 0)
                                appendLog("%-20s %10d %10d".format("#$tag", recCount, randCount))
                            }


                    }

                    override fun onCancelled(error: DatabaseError) {
                        hideProgress()
                        appendLog("❌ Lỗi: ${error.message}")
                    }
                })
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 📋 6. FULL REPORT
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun generateFullReport() {
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("📋 FULL RECOMMENDATION REPORT")
        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("Đang tạo báo cáo đầy đủ...")
        appendLog("")

        // Chạy tuần tự
        showHashtagScores()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showAllInteractions()
        }, 1500)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testRecommendationDetailed()
        }, 3000)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            compareRecommendedVsRandom()
        }, 4500)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // HELPER FUNCTIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private fun appendLog(message: String) {
        runOnUiThread {
            logBuilder.append(message).append("\n")
            tvResults.text = logBuilder.toString()

            // Auto scroll to bottom
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

        Log.d(TAG, message)
    }

    private fun clearLog() {
        logBuilder.clear()
        tvResults.text = ""
    }

    private fun showProgress() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgress() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
    }
}