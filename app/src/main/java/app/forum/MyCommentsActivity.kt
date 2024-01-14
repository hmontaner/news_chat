package app.forum

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import app.forum.database.FirestoreRepository
import app.forum.databinding.ActivityMyCommentsBinding
import app.forum.ui.main.MyCommentsPagerAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// This class contains two fragments that list the comments written by the user or replies to
// those comments.
class MyCommentsActivity : AppCompatActivity() {

    companion object {
        val TAG = "MyCommentsActivity"
    }

    private lateinit var binding: ActivityMyCommentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupPager(0)
    }

    private fun setupPager(currentItem: Int) {
        val sectionsPagerAdapter = MyCommentsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
        viewPager.currentItem = currentItem
    }
}