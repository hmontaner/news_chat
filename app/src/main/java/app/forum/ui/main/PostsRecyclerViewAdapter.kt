package app.forum.ui.main

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import app.forum.R
import app.forum.database.PostModel
import coil.load
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions


class PostsRecyclerViewAdapter(
    options: FirestoreRecyclerOptions<PostModel>,
    private val mListener: (String, PostModel) -> Unit
) : FirestoreRecyclerAdapter<PostModel, PostsRecyclerViewAdapter.ViewHolder>(options) {

    inner class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        val title : TextView = view.findViewById(R.id.title)
        val description : TextView = view.findViewById(R.id.description)
        val commentCount : TextView = view.findViewById(R.id.comment_count)
        val image : ImageView = view.findViewById(R.id.image)
        val open : View = view.findViewById(R.id.open)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int, postModel: PostModel) {
        viewHolder.title.text = postModel.title
        viewHolder.commentCount.text = postModel.num_comments.toString()
        viewHolder.description.text = postModel.description
        viewHolder.image.load(postModel.image)
        viewHolder.itemView.setOnClickListener {
            mListener(snapshots.getSnapshot(position).id, postModel)
        }
        viewHolder.open.setOnClickListener {
            linkCallback(postModel.link)
        }
    }

    var callback: (itemCount: Int) -> Unit = {}
    fun setOnDataChanged(callback: (itemCount: Int) -> Unit){
        this.callback = callback
    }
    var linkCallback: (link: String) -> Unit = {}
    fun setOnLinkClick(callback: (link: String) -> Unit) {
        this.linkCallback = callback
    }
    override fun onDataChanged() {
        super.onDataChanged()
        callback(itemCount)
    }
}