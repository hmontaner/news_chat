package app.forum.database

import android.content.SharedPreferences
import android.os.Parcel
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.time.LocalDate
import java.util.*

class FirestoreRepository {

    companion object {
        private val TAG = toString()
        // This is where you should add the Firestore document id of your RSS feed
        private val SOURCE = "<YOUR_SOURCE_ID>"
    }

    val firestoreDB = FirebaseFirestore.getInstance()

    init{
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestoreDB.firestoreSettings = settings
    }

    fun getPosts(type: String): Query {
        val query = firestoreDB.collection("sources").document(SOURCE)
                .collection("posts").whereEqualTo("new", true)
        if (type == "popular") {
            return query.orderBy("num_comments", Query.Direction.DESCENDING).limit(100)
        }
        return query.orderBy("pubDate", Query.Direction.DESCENDING).limit(100)
    }

    fun getComments(postId: String, type: String): Query {
        val query = firestoreDB.collection("sources").document(SOURCE)
                .collection("posts").document(postId)
                .collection("comments")
        if (type == "popular") {
            return query.orderBy("likes", Query.Direction.DESCENDING).limit(100)
        }
        return query.orderBy("time_creation_server", Query.Direction.DESCENDING).limit(100)
    }

    fun getCommentsFromMe(userId: String): Query {
        return firestoreDB.collectionGroup("comments")
                    .whereEqualTo("author_id", userId)
                    .orderBy("time_creation_server", Query.Direction.DESCENDING)
                    .limit(100)
    }

    fun getCommentsToMe(userId: String): Query {
        return firestoreDB.collectionGroup("comments")
            .whereEqualTo("in_reply_to.author_id", userId)
            .orderBy("time_creation_server", Query.Direction.DESCENDING)
            .limit(100)
    }

    fun getLikes(userId: String, postId: String): Query {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(userId)
            .collection("likes").whereEqualTo("post_id", postId).limit(100)
    }

    fun getLikes(userId: String): Query {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(userId)
            .collection("likes").limit(1000)
    }

    fun addComment(postId: String, comment: CommentModel): Task<DocumentReference> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("posts").document(postId)
            .collection("comments").add(comment)
    }

    fun isUserNameUnique(name: String): Query {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").whereEqualTo("name", name)
    }

    fun addUser(user: UserModel): Task<Void> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(user.uid).set(user)
    }

    fun getUser(userId: String): Task<DocumentSnapshot> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(userId).get()
    }

    fun getComment(postId: String, commentId: String): Task<DocumentSnapshot> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("posts").document(postId)
            .collection("comments").document(commentId).get()
    }

    fun addLike(userId: String, postId: String, commentId: String): Task<Void> {
        val like = LikeModel(postId)
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(userId)
            .collection("likes").document(commentId).set(like)
    }
    fun removeLike(userId: String, commentId: String): Task<Void> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(userId)
            .collection("likes").document(commentId).delete()
    }
    fun removeComment(postId: String, commentId: String): Task<Void> {
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("posts").document(postId)
            .collection("comments").document(commentId).delete()
    }
    fun getUserUid() : String?{
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid
    }
    fun addToken(token: String, sp: SharedPreferences? = null){
        val uid = getUserUid()
        if(uid == null){
            Log.e("", "uid is null")
            return
        }
        firestoreDB.collection("sources").document(SOURCE)
            .collection("users").document(uid)
            .update("tokens", FieldValue.arrayUnion(token))
            .addOnSuccessListener { _ ->
                sp?.edit()?.putBoolean("token_registered", true)?.apply()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding token", e)
            }
    }
    fun reportComment(userId: String, postId: String, commentId: String): Task<Void> {
        val report = ReportModel()
        return firestoreDB.collection("sources").document(SOURCE)
            .collection("posts").document(postId)
            .collection("comments").document(commentId)
            .collection("reports").document(userId).set(report)
    }
}