package com.example.socialmedia.project.Utils

import android.util.Log
import com.google.firebase.database.*

object DatabaseMigration {

    private const val TAG = "DatabaseMigration"

    fun addHashtagsToExistingReels(database: FirebaseDatabase, onComplete: (Int) -> Unit = {}) {
        Log.d(TAG, "🔄 Starting hashtags migration...")

        database.reference.child("Reels")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalReels = 0
                    var reelsUpdated = 0
                    var reelsWithHashtags = 0

                    for (reelSnapshot in snapshot.children) {
                        totalReels++

                        val existingHashtags = mutableListOf<String>()
                        reelSnapshot.child("hashtags").children.forEach { hashtagSnapshot ->
                            hashtagSnapshot.getValue(String::class.java)?.let {
                                existingHashtags.add(it)
                            }
                        }

                        if (existingHashtags.isNotEmpty()) {
                            reelsWithHashtags++
                            Log.d(TAG, "✓ ${reelSnapshot.key?.take(8)} has: $existingHashtags")
                        } else {
                            val caption = reelSnapshot.child("caption").getValue(String::class.java)
                            val extractedHashtags = HashtagUtils.extractHashtags(caption)

                            if (extractedHashtags.isNotEmpty()) {
                                reelSnapshot.ref.child("hashtags").setValue(extractedHashtags)
                                    .addOnSuccessListener {
                                        reelsUpdated++
                                        Log.d(TAG, "✅ ${reelSnapshot.key?.take(8)}: $extractedHashtags")
                                    }
                            } else {
                                // ✅ Không có hashtags trong caption → Lưu empty array
                                reelSnapshot.ref.child("hashtags").setValue(emptyList<String>())
                                Log.d(TAG, "⚠️ ${reelSnapshot.key?.take(8)}: No hashtags, saved empty array")
                            }
                        }
                    }

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "📊 Migration Summary:")
                        Log.d(TAG, "   Total reels: $totalReels")
                        Log.d(TAG, "   Already have hashtags: $reelsWithHashtags")
                        Log.d(TAG, "   Updated: $reelsUpdated")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        onComplete(reelsUpdated)
                    }, 2000)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Migration failed: ${error.message}")
                }
            })
    }

    fun verifyHashtags(database: FirebaseDatabase) {
        Log.d(TAG, "🔍 Verifying hashtags...")

        database.reference.child("Reels")
            .limitToFirst(5)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📋 First 5 Reels Hashtags:")

                    for (reelSnapshot in snapshot.children) {
                        val reelId = reelSnapshot.key?.take(8) ?: "unknown"
                        val caption = reelSnapshot.child("caption").getValue(String::class.java)

                        val hashtags = mutableListOf<String>()
                        reelSnapshot.child("hashtags").children.forEach { hashtagSnapshot ->
                            hashtagSnapshot.getValue(String::class.java)?.let { hashtags.add(it) }
                        }

                        Log.d(TAG, "  $reelId:")
                        Log.d(TAG, "    Caption: ${caption?.take(30)}")
                        Log.d(TAG, "    Hashtags: $hashtags")
                    }

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Verification failed: ${error.message}")
                }
            })
    }
}