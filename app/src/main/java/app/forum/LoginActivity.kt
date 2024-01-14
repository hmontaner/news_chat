package app.forum

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.forum.database.FirestoreRepository
import app.forum.database.UserModel
import app.forum.databinding.ActivityLoginBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {

    companion object {
        val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private val REQUEST_CODE_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.termsOfUse.movementMethod = LinkMovementMethod.getInstance()

        // Erase error message when the user types anything else
        binding.name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.error.text = ""
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // If the user wants to log in with a provider
        binding.login.setOnClickListener{
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
                    .setAvailableProviders(providers)
                    .build(),
                REQUEST_CODE_SIGN_IN
            )
        }

        // If the user does not want to log in with a provider for now
        binding.skipLogin.setOnClickListener{
            showSetName()
        }

        // When the user has set their name
        binding.done.setOnClickListener {
            val name = binding.name
            if (name.text.isNotEmpty()) {
                binding.wait.visibility = View.VISIBLE
                val nameString = name.text.toString().lowercase()
                // Name must be unique
                checkName(nameString) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if(uid == null) {
                        // Let's sign this user in anonymously
                        createFirebaseUser(nameString)
                    }else{
                        createUser(uid, nameString)
                    }
                }
            }else{
                binding.error.text = getString(R.string.empty_name)
            }
        }
    }

    private fun handleError(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $message")
        binding.wait.visibility = View.INVISIBLE
    }

    private fun checkName(userName: String, listener: () -> Unit) {
        val repository = FirestoreRepository()
        repository.isUserNameUnique((userName)).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    listener()
                } else {
                    binding.wait.visibility = View.INVISIBLE
                    binding.error.text = getString(R.string.name_taken)
                }
            }
            .addOnFailureListener { e ->
                handleError("Error checking name: $e")
            }
    }

    // Store the user information in our database
    private fun createUser(uid: String, name: String){
        val user = UserModel(
            uid = uid,
            name = name
        )
        FirestoreRepository().addUser(user)
            .addOnSuccessListener {
                binding.wait.visibility = View.INVISIBLE
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                handleError("Error storing user $e")
            }
    }

    private fun createFirebaseUser(userName: String){
        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously().addOnCompleteListener { task ->
            if(task.isSuccessful){
                val uid = auth.currentUser?.uid
                if(uid == null) {
                    handleError("Error authenticating: current user not found")
                } else {
                    createUser(uid, userName)
                }
            }else{
                handleError("Error authenticating: " + task.exception)
            }
        }
    }

    private fun showSetName(){
        binding.setName.visibility = View.VISIBLE
        binding.loginOrSkip.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_SIGN_IN){
            if(resultCode == Activity.RESULT_OK){
                val currentUser = FirebaseAuth.getInstance().currentUser
                if(currentUser != null) {
                    val uid = currentUser.uid
                    FirestoreRepository().getUser(uid).addOnSuccessListener {
                        if (it.exists()) {
                            // Already existing user
                            setResult(Activity.RESULT_OK)
                            finish()
                        }else{
                            showSetName()
                        }
                    }.addOnFailureListener{exception ->
                        handleError(exception.toString())
                        Log.e(TAG, "get failed with ", exception)
                    }
                }else{
                    handleError("Error authenticating: current user not found")
                }
            }else{
                handleError("Error in login")
                Log.e(TAG, "Error in login")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    override fun onBackPressed() {
        // No going back on login screen
    }
}