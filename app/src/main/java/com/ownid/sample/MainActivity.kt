package com.ownid.sample

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ownid.sample.databinding.ActivityMainBinding
import com.ownid.sdk.*
import com.ownid.sdk.exception.EmailAndPasswordRequired
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ownIdFirebase: OwnIdFirebase

    private val emailAddress: String
        get() = binding.email.text.toString()

    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.root.updateOwnIdView(result.resultCode)
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { onOwnIdRegisterResponse(it) }
                .onFailure { toast(it.message) }
        }

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.root.updateOwnIdView(result.resultCode)
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { ownIdResponse ->
                    if (ownIdResponse.isLogin()) onOwnIdLoginResponse(ownIdResponse)
                    else onOwnIdLinkOnLoginResponse(ownIdResponse)
                }
                .onFailure { toast(it.message) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        integrateOwnId()
    }

    private fun integrateOwnId() {
        OwnIdLogger.enabled = true
        ownIdFirebase = OwnIdFirebaseFactory.createOwnId(this, Firebase.auth)

        binding.root.setOwnIdView { onSkipPasswordClicked() }
    }

    private fun onSkipPasswordClicked() {
        if (validateEmail(emailAddress)) {
            val registerIntent = ownIdFirebase.createRegisterIntent(this, "en", emailAddress)
            registerLauncher.launch(registerIntent)
        } else {
            binding.inputLayout.error = "The email is not valid"
        }
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun toast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun onOwnIdRegisterResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.register("Tien Dzung", emailAddress, ownIdResponse)
            .addOnSuccessListener { toast("Registered successfully") }
            .addOnFailureListener { cause -> toast(cause.message) }
    }

    private fun onOwnIdLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.login(ownIdResponse)
            .addOnSuccessListener { toast("Logged in successfully") }
            .addOnFailureListener {
                when (it) {
                    is CancellationException -> throw it
                    is EmailAndPasswordRequired -> onOwnIdLinkOnLoginResponse(ownIdResponse)
                    else -> toast(it.message)
                }
            }
    }

    private fun onOwnIdLinkOnLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.loginAndLink(emailAddress, "password", ownIdResponse)
            .addOnSuccessListener { toast("Login with password successfully") }
            .addOnFailureListener { toast(it.message) }
    }
}