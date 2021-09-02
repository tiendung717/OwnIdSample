package com.ownid.sample

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ownid.sample.databinding.ActivityMainBinding
import com.ownid.sdk.*
import com.ownid.sdk.exception.EmailAndPasswordRequired
import kotlinx.coroutines.CancellationException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ownIdFirebase: OwnIdFirebase
    private var email = ""
    private val ownIdRegisterResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.root.updateOwnIdView(result.resultCode)
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { ownIdResponse: OwnIdResponse ->
                    onOwnIdRegisterResponse(ownIdResponse)
                    Log.d(
                        TAG,
                        "ownIdResponse data:${ownIdResponse.data} nonce: ${ownIdResponse.nonce}"
                    )
                }
                .onFailure { error ->
                    showError(error)
                    Log.e(
                        TAG,
                        "fail with $error"
                    )
                }
        }

    private fun showError(s: Throwable) {
        Toast.makeText(this@MainActivity, s.message, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        OwnIdLogger.enabled = true
        ownIdFirebase = OwnIdFirebaseFactory.createOwnId(
            this,
            Firebase.auth
        )

        val registerIntent = ownIdFirebase.createRegisterIntent(this, "en", "example@gmail.com")

        binding.root.setOwnIdView {
            val email = binding.email.text.toString()
            if (email.isEmpty()) {
                binding.inputLayout.error = "Please enter email"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.inputLayout.error = "The email is not valid"
            } else {
                binding.inputLayout.error = ""
                ownIdRegisterResultLauncher.launch(registerIntent)
            }
        }
    }

    private fun onOwnIdRegisterResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.register("Sample Name", email, ownIdResponse)
            .addOnSuccessListener {
                AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Logged in with email $email")
                    .show()
            }
            .addOnFailureListener { cause ->
                Log.e(
                    TAG,
                    cause.message.toString()
                )
                when (cause) {
                    is CancellationException -> throw cause
                    is EmailAndPasswordRequired -> onOwnIdLinkOnLoginResponse(ownIdResponse)
                    else -> showError(cause)
                }
            }
    }

    private fun onOwnIdLinkOnLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.loginAndLink(email, PASS_SAMPLE, ownIdResponse)
            .addOnSuccessListener {
                AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Logged in with email $email")
                    .show()
                Log.d(
                    TAG,
                    "Success login with response"
                )
            }
            .addOnFailureListener { cause ->
                Log.e(
                    TAG,
                    cause.message.toString()
                )
                if (cause is CancellationException) throw cause
                showError(cause)
            }
    }

    companion object {
        const val TAG = "OwnIdSample"
        const val PASS_SAMPLE = "1234@5678"
    }
}