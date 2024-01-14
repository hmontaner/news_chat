package app.forum.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.forum.CommentsActivity
import app.forum.R
import app.forum.database.FirestoreRepository
import app.forum.database.PostModel
import app.forum.databinding.FragmentPostsBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions

// This class shows a list of posts (news articles)
class PostsFragment : Fragment() {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private lateinit var myAdapter : PostsRecyclerViewAdapter
    private lateinit var type : String
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        val root = binding.root

        arguments?.getString(ARG_TYPE)?.let {
            type = it
        }

        val emptyTextView: TextView = binding.textEmpty
        emptyTextView.text = getString(R.string.empty_post_list)

        with(binding.list) {
            val repository = FirestoreRepository()
            val query = repository.getPosts(type)
            val options = FirestoreRecyclerOptions.Builder<PostModel>()
                .setQuery(query, PostModel::class.java).build()
            myAdapter = PostsRecyclerViewAdapter(options) { postModelId, postModel ->
                val intent = Intent(context, CommentsActivity::class.java)
                intent.putExtra("post_id", postModelId)
                intent.putExtra("post_title", postModel.title)
                intent.putExtra("post_link", postModel.link)
                activity?.startActivity(intent)
            }
            myAdapter.setOnDataChanged {
                var itemCount = it
                val visibility = if (itemCount == 0) View.VISIBLE else View.INVISIBLE
                emptyTextView.visibility = visibility
            }
            myAdapter.setOnLinkClick {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                startActivity(browserIntent)
            }
            adapter = myAdapter
        }

        return root
    }

    companion object {
        private const val ARG_TYPE = "type"

        @JvmStatic
        fun newInstance(type: String): PostsFragment {
            return PostsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Fix for recycler view bug
        myAdapter.notifyDataSetChanged()
        myAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        myAdapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}