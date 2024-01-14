package app.forum.database

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class InReplyTo (
    val comment_id: String = "",
    val author_id: String = "",
    val author_name: String = "",
    val text: String = "",
) : Parcelable

@Parcelize
data class CommentModel (
    val text: String = "",
    val author_id : String = "",
    val author_name: String = "",
    val author_image: String = "",
    val likes: Int = 0,
    val in_reply_to: InReplyTo? = null,
    @ServerTimestamp
    val time_creation_server: Date? = null
) : Parcelable