package app.forum.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.forum.CommentsActivity
import app.forum.database.CommentModel
import app.forum.database.FirestoreRepository
import app.forum.databinding.FragmentCommentsBinding
import app.forum.ui.main.CommentsFragment.Types.FROM_YOU
import app.forum.ui.main.CommentsFragment.Types.LATEST
import app.forum.ui.main.CommentsFragment.Types.POPULAR
import app.forum.ui.main.CommentsFragment.Types.TO_YOU
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// This fragment shows a list of comments (given a post id)
class CommentsFragment : Fragment() {
    object Types {
        // Comments ordered by date
        const val LATEST = "latest"
        // Comments ordered by number of likes
        const val POPULAR = "popular"
        // Comments sent by the current user
        const val FROM_YOU = "from_you"
        // Replies to comments by the current user
        const val TO_YOU = "to_you"
    }

    private var _binding: FragmentCommentsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private lateinit var myAdapter : CommentsRecyclerViewAdapter
    private var postId : String = ""
    private lateinit var type : String
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        arguments?.getString(ARG_TYPE)?.let {
            type = it
        }
        arguments?.getString(ARG_POST_ID)?.let {
            postId = it
        }
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        val root = binding.root

        val user = Firebase.auth.currentUser
        user?.let { user ->
            // Retrieve likes made by the current user so we know if a comment
            // has been already liked by him or not
            var query =
                if (type == "latest" || type == "popular") {
                    FirestoreRepository().getLikes(user.uid, postId)
                } else {
                    // Get all likes
                    FirestoreRepository().getLikes(user.uid)
                }
            query.get().addOnSuccessListener { documents ->
                setupList(documents.map { it.id }.toMutableList())
            }
        }

        return root
    }

    private fun setupList(likes: MutableList<String>) {
        val emptyTextView: TextView = binding.textEmpty

        with(binding.list) {
            val repository = FirestoreRepository()
            val user = Firebase.auth.currentUser
            val query = when(type) {
                FROM_YOU -> repository.getCommentsFromMe(user?.uid ?: "")
                TO_YOU -> repository.getCommentsToMe(user?.uid ?: "")
                else -> repository.getComments(postId, type)
            }
            val inPost = type == LATEST || type == POPULAR
            val showReplies = type == TO_YOU
            val options = FirestoreRecyclerOptions.Builder<CommentModel>()
                .setLifecycleOwner(activity)
                .setQuery(query, CommentModel::class.java).build()

            myAdapter = CommentsRecyclerViewAdapter(options, likes, user?.uid ?: "", inPost, showReplies)

            // Show "no posts" if there are no posts
            myAdapter.setOnDataChanged {
                var itemCount = it
                val visibility = if (itemCount == 0) View.VISIBLE else View.GONE
                emptyTextView.visibility = visibility
                if (type == "latest" && computeVerticalScrollOffset() == 0) {
                    smoothScrollToPosition(0)
                }
            }

            // When the user likes a comment
            myAdapter.setOnLikeClicked { commentId, postId, liked ->
                user?.let {
                    val task = if (liked)
                        repository.removeLike(user.uid, commentId)
                    else
                        repository.addLike(user.uid, postId, commentId)
                    task.addOnSuccessListener {

                    }
                    .addOnFailureListener {
                        Log.e(CommentsActivity.TAG, "Error adding comment", it)
                    }
                }
            }

            // When the user wants to reply to a comment
            myAdapter.setOnInReplyTo { commentId, commentModel ->
                val commAct: CommentsActivity = activity as CommentsActivity
                commAct.reply(commentId, commentModel)
            }

            myAdapter.setOnReport {commentId ->
                val commAct: CommentsActivity = activity as CommentsActivity
                commAct.reportComment(commentId)
            }

            // When the user wants to delete one of their comments
            myAdapter.setOnDeleteClicked { commentId ->
                repository.removeComment(postId, commentId)
            }

            adapter = myAdapter
        }
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_TYPE = "type"

        @JvmStatic
        fun newInstance(postId: String, type: String): CommentsFragment {
            return CommentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_TYPE, type)
                }
            }
        }

        @JvmStatic
        fun newInstance(type: String): CommentsFragment {
            return CommentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Fix for Firebase recycler view bug
        if (::myAdapter.isInitialized) {
            myAdapter.notifyDataSetChanged()
            myAdapter.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::myAdapter.isInitialized) {
            myAdapter.stopListening()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}