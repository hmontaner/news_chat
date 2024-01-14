package app.forum.database

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class LikeModel (
    val post_id: String = "",
    @ServerTimestamp
    val time_creation_server: Date? = null
) : Parcelable