package app.forum.ui.main

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import app.forum.R
import app.forum.database.CommentModel
import app.forum.database.FirestoreRepository
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.ktx.toObject
import org.w3c.dom.Comment


class CommentsRecyclerViewAdapter(
    options: FirestoreRecyclerOptions<CommentModel>,
    private val likes: MutableList<String>,
    private val uid: String,
    private val inPost: Boolean,
    private val showReplies: Boolean,
) : FirestoreRecyclerAdapter<CommentModel, CommentsRecyclerViewAdapter.ViewHolder>(options) {

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_REPLY = 1
    }

    inner class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        val title : TextView = view.findViewById(R.id.title)
        val name : TextView = view.findViewById(R.id.name)
        val like : ImageButton = view.findViewById(R.id.like)
        val likeCount : TextView = view.findViewById(R.id.like_count)
        val inReplyTo : View = view.findViewById(R.id.in_reply_to)
        val reply : View = view.findViewById(R.id.reply)
        val delete : View = view.findViewById(R.id.delete)
        var likeCountInt : Int = 0
        val parentTitle : TextView? = view.findViewById(R.id.parent_title)
        val parentName : TextView? = view.findViewById(R.id.parent_name)
        val commentWrapper : View = view.findViewById(R.id.comment_wrapper)
    }

    private val openCommentIds = mutableSetOf<String>()

    override fun getItemViewType(position: Int): Int {
        val id = snapshots.getSnapshot(position).id
        if (showReplies) return TYPE_REPLY
        return if (openCommentIds.contains(id)) TYPE_REPLY else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == TYPE_NORMAL) R.layout.comment else R.layout.comment_with_reply
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int, commentModel: CommentModel) {
        viewHolder.title.text = commentModel.text
        viewHolder.name.text = commentModel.author_name
        viewHolder.likeCountInt = commentModel.likes
        viewHolder.likeCount.text = commentModel.likes.toString()
        val snapshot = snapshots.getSnapshot(position)
        val commentId = snapshot.id
        val liked = likes.contains(commentId)
        val colorId = if (liked) R.color.liked else R.color.not_liked
        val color = ContextCompat.getColor(viewHolder.like.context, colorId)
        viewHolder.like.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        viewHolder.like.setOnClickListener {
            // Update UI by hand for immediate effect
            val liked = likes.contains(commentId)
            if (liked) likes.remove(commentId) else likes.add(commentId)
            viewHolder.likeCountInt += if (liked) -1 else 1
            viewHolder.likeCount.text = viewHolder.likeCountInt.toString()
            val colorId = if (liked) R.color.not_liked else R.color.liked
            val color = ContextCompat.getColor(viewHolder.like.context, colorId)
            viewHolder.like.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            val postId = snapshot.reference.parent.parent!!.id
            onLikeCallback(commentId, postId, liked)
        }
        if (!inPost) {
            viewHolder.reply.visibility = View.GONE
        }
        viewHolder.reply.setOnClickListener {
            onInReplyToCallback(commentId, commentModel)
        }
        if (uid == commentModel.author_id) {
            viewHolder.delete.visibility = View.VISIBLE
            viewHolder.delete.setOnClickListener {
                onDeleteCallback(commentId)
            }
        } else {
            viewHolder.delete.visibility = View.GONE
        }
        if (showReplies || commentModel.in_reply_to == null) {
            viewHolder.inReplyTo.visibility = View.GONE
        } else {
            viewHolder.inReplyTo.visibility = View.VISIBLE
            viewHolder.inReplyTo.setOnClickListener {
                if (openCommentIds.contains(commentId)) {
                    openCommentIds.remove(commentId)
                } else {
                    openCommentIds.add(commentId)
                }
                notifyItemChanged(position)
            }
        }
        if (commentModel.in_reply_to != null) {
            viewHolder.parentTitle?.text = commentModel.in_reply_to.text
            viewHolder.parentName?.text = commentModel.in_reply_to.author_name
        }
        viewHolder.commentWrapper.setOnClickListener {
            onReportCallback(commentId)
        }
    }

    var onLikeCallback: (commentId: String, postId: String, liked: Boolean) -> Unit = { _: String, _: String, _: Boolean -> }
    fun setOnLikeClicked(callback: (commentId: String, postId: String, liked: Boolean) -> Unit) {
        this.onLikeCallback = callback
    }

    var onDeleteCallback: (commentId: String) -> Unit = {}
    fun setOnDeleteClicked(callback: (commentId: String) -> Unit) {
        this.onDeleteCallback = callback
    }

    var onInReplyToCallback: (commentId: String, commentModel: CommentModel) -> Unit = { _: String, _: CommentModel -> }
    fun setOnInReplyTo(callback: (commentId: String, commentModel: CommentModel) -> Unit) {
        this.onInReplyToCallback = callback
    }

    var onReportCallback: (commentId: String) -> Unit = { _: String -> }
    fun setOnReport(callback: (commentId: String) -> Unit) {
        this.onReportCallback = callback
    }


    var callback: (itemCount: Int) -> Unit = {}
    fun setOnDataChanged(callback: (itemCount: Int) -> Unit){
        this.callback = callback
    }
    override fun onDataChanged() {
        super.onDataChanged()
        callback(itemCount)
    }
}