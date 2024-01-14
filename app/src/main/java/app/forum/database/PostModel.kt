package app.forum.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostModel (
    val uid : String = "",
    val title: String = "",
    val description: String = "",
    val num_comments: Int = 0,
    val image: String = "",
    val link: String = "",
) : Parcelable