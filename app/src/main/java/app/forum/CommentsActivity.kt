package app.forum

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import app.forum.database.CommentModel
import app.forum.database.FirestoreRepository
import app.forum.database.InReplyTo
import app.forum.database.UserModel
import app.forum.databinding.ActivityCommentsBinding
import app.forum.ui.main.CommentsPagerAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

// This activity contains two fragments with the comments of this post
class CommentsActivity : AppCompatActivity() {

    companion object {
        val TAG = "CommentsActivity"
    }

    private lateinit var binding: ActivityCommentsBinding
    private var userName : String? = null
    private lateinit var postId : String
    private lateinit var postLink : String
    // Whether or not a click on a comment means report
    private var reporting : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // This activity needs a post id
        postId = intent.getStringExtra("post_id")!!
        val postTitle = intent.getStringExtra("post_title").orEmpty()
        postLink = intent.getStringExtra("post_link").orEmpty()
        binding.title.text = postTitle

        val sectionsPagerAdapter = CommentsPagerAdapter(this, supportFragmentManager, postId)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        retrieveUser()
        // Allow the user to write comments
        activateSending(postId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.comments, menu)
        return true
    }

    // Handle clicks on the top right menu
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_link -> {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(postLink))
            startActivity(browserIntent)
            true
        }
        R.id.action_report -> {
            AlertDialog.Builder(this)
                .setTitle(R.string.reporting_enabled)
                .setMessage(R.string.reporting_enabled_info)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    reporting = true
                }
                .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int ->
                    reporting = false
                }
                .show()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    fun reportComment(commentId: String) {
        if (!reporting) {
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.reporting_comment)
            .setMessage(R.string.reporting_comment_info)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                val repository = FirestoreRepository()
                val user = Firebase.auth.currentUser
                user?.let {
                    repository.reportComment(user.uid, postId, commentId).addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.comment_reported), Toast.LENGTH_SHORT).show()
                    }
                }
                reporting = false
            }
            .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int -> }
            .show()
    }

    private fun retrieveUser() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_user), Toast.LENGTH_SHORT).show()
            return
        }
        val repository = FirestoreRepository()
        repository.getUser(user.uid).addOnSuccessListener {
            userName = it.toObject<UserModel>()?.name
        }
    }

    private var inReplyToCommentId: String = ""
    private var inReplyCommentModel: CommentModel? = null
    private fun clearInReply() {
        inReplyToCommentId = ""
        inReplyCommentModel = null
        binding.replying.visibility = View.INVISIBLE
    }

    private fun activateSending(postId: String) {
        binding.send.setOnClickListener {
            val repository = FirestoreRepository()
            if (binding.input.text.isEmpty()) {
                return@setOnClickListener
            }
            val name = userName
            if (name == null) {
                Toast.makeText(this, getString(R.string.error_user), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val user = Firebase.auth.currentUser
            user?.let {
                val commentModel = CommentModel(
                    author_id = user.uid,
                    text = binding.input.text.toString(),
                    author_name = name,
                    author_image = "",
                    // If this comment is a reply to another comment
                    in_reply_to = if (inReplyToCommentId == "") null else InReplyTo(
                        comment_id = inReplyToCommentId,
                        author_name =  inReplyCommentModel?.author_name ?: "",
                        author_id = inReplyCommentModel?.author_id ?: "",
                        text = inReplyCommentModel?.text ?: "",
                    )
                )
                repository.addComment(postId, commentModel)
                    .addOnSuccessListener { _ ->
                        binding.input.text.clear()
                        clearInReply()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding comment", e)
                    }
            }
        }
        binding.replyingClose.setOnClickListener {
            clearInReply()
        }
    }

    // Show the "replying to" visual bubble
    fun reply(commentId: String, commentModel: CommentModel) {
        inReplyToCommentId = commentId
        inReplyCommentModel = commentModel
        binding.replying.visibility = View.VISIBLE
        binding.replyName.text = commentModel.author_name
    }
}