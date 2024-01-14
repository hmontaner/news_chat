package app.forum

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.forum.database.FirestoreRepository
import app.forum.database.UserModel
import app.forum.databinding.ActivitySettingsBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity() {
    private val REQUEST_CODE_SIGN_IN = 1
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Retrieve user name
        val user = Firebase.auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_user), Toast.LENGTH_SHORT).show()
        } else {
            val repository = FirestoreRepository()
            repository.getUser(user.uid).addOnSuccessListener {
                val name = it.toObject<UserModel>()?.name
                binding.name.text = name
            }
        }

        firebaseAnalytics = Firebase.analytics

        if (!FirebaseAuth.getInstance().currentUser!!.isAnonymous) {
            // The user has logged in with a provider (e.g. Google) so the account is secure,
            // that is, the user can recover their account if they change phones.
            setAccountSaved()
        } else {
            // The user is "anonymous" and may want to log in using a provider
            binding.saveAccount.setOnClickListener {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.GoogleBuilder().build()
                    //AuthUI.IdpConfig.EmailBuilder().build(),
                    //AuthUI.IdpConfig.PhoneBuilder().build(),
                    //AuthUI.IdpConfig.FacebookBuilder().build(),
                    //AuthUI.IdpConfig.TwitterBuilder().build()
                )
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .enableAnonymousUsersAutoUpgrade()
                        .setAvailableProviders(providers)
                        .build(),
                    REQUEST_CODE_SIGN_IN
                )
            }
        }

        binding.logout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.really_log_out)
                .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                    val resultIntent = Intent()
                    resultIntent.putExtra("logout", true)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
                .show()
        }
    }

    private fun setAccountSaved() {
        binding.saveAccountWrapper.visibility = View.GONE
        binding.accountSaved.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_SIGN_IN){
            // Explicit login
            if(resultCode == Activity.RESULT_OK){
                setAccountSaved()
                firebaseAnalytics.logEvent("account_saved"){}
            }else{
                val response = IdpResponse.fromResultIntent(data)
                if (response?.error?.errorCode == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                    // If the user signs in with a provider (e.g. Google), then logs out, then
                    // logs in again anonymously, and tries to sign in with that provider, this
                    // error will show.
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.error_in_login))
                        .setMessage(getString(R.string.account_merging_not_supported))
                        .show()
                }else{
                    Log.e(PostsActivity.TAG, "Error in login")
                }
                firebaseAnalytics.logEvent("account_saving_error"){}
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}