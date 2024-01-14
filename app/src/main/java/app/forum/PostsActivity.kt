package app.forum

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import app.forum.database.FirestoreRepository
import app.forum.ui.main.PostsPagerAdapter
import app.forum.databinding.ActivityPostsBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

// This is the main activity. It contains two fragments, each of them listing the posts (news
// articles) in a different order, latest and popular.
class PostsActivity : AppCompatActivity() {

    companion object {
        val TAG = "PostActivity"
    }

    private lateinit var binding: ActivityPostsBinding
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPlayServices(this)

        // If the user is not logged in, send him to the login activity
        checkAuthentication()

        binding = ActivityPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val sectionsPagerAdapter = PostsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        setupPushNotificationToken()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    private val REQUEST_CODE_LOGOUT = 2

    // Handle clicks on the top right menu
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_messages -> {
            val intent = Intent(this, MyCommentsActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_LOGOUT)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun checkPlayServices(context: Context){
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        if(resultCode != ConnectionResult.SUCCESS){
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        }
    }

    private val REQUEST_CODE_USER_CREATION = 1
    private fun checkAuthentication() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // User needs to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_USER_CREATION)
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_USER_CREATION) {
            // Back from login activity
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Error from login activity")
            } else {
                setupPushNotificationToken()
            }
        } else if (requestCode == REQUEST_CODE_LOGOUT) {
            // Back from settings activity
            if (resultCode == Activity.RESULT_OK) {
                val logout = data?.getBooleanExtra("logout", false)
                // If the user asked to be logged out
                logout?.let {
                    if (it) {
                        FirebaseAuth.getInstance().signOut()
                        val sp = getSharedPreferences(getString(R.string.shared_preferences_file_name), 0)
                        sp.edit().putBoolean("token_registered", false).apply()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, REQUEST_CODE_USER_CREATION)
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupPushNotificationToken() {
        val sp = getSharedPreferences(getString(R.string.shared_preferences_file_name), 0)
        if(!sp.getBoolean("token_registered", false)) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    token?.let {
                        // Send token to server
                        Log.d(TAG, token)
                        FirestoreRepository().addToken(token, sp)
                    }
                })
        }
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}