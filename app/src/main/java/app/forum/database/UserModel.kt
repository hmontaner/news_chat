package app.forum.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel (
    val uid : String = "",
    val name: String = ""
) : Parcelable