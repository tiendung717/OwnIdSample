package com.ownid.sample

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ownid.sample.databinding.ActivityMainBinding
import com.ownid.sdk.*
import com.ownid.sdk.exception.EmailAndPasswordRequired
import com.ownid.sdk.exception.ServerError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ownIdFirebase: OwnIdFirebase

    private val username: String
        get() = binding.username.text.toString()

    private val emailAddress: String
        get() = binding.email.text.toString()

    private val password: String
        get() = binding.password.text.toString()

    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.root.updateOwnIdView(result.resultCode)
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { onOwnIdRegisterResponse(it) }
                .onFailure {
                    when (it) {
                        is ServerError -> {
                            showMessage(it.message)
                            gotoLoginPage()
                        }
                        else -> showMessage(it.message)
                    }
                }
        }

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { ownIdResponse ->
                    if (ownIdResponse.isLogin()) onOwnIdLoginResponse(ownIdResponse)
                    else onOwnIdLinkOnLoginResponse(ownIdResponse)
                }
                .onFailure { showMessage(it.message) }
        }

    private var registerVisible by Delegates.observable(true) { _, _, newValue ->
        binding.btnRegister.isEnabled = newValue
    }

    private var loginVisible by Delegates.observable(true) { _, _, newValue ->
        binding.btnLogin.isEnabled = newValue
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        gotoRegisterPage()
        integrateOwnId()
    }

    private fun integrateOwnId() {
        OwnIdLogger.enabled = true
        ownIdFirebase = OwnIdFirebaseFactory.createOwnId(this, Firebase.auth)

        binding.root.setOwnIdView { registerWithSkipPassword() }
        binding.btnRegister.setOnClickListener { registerWithPassword() }
        binding.btnLogin.setOnClickListener { login() }
    }

    private fun login() {
        val loginIntent = ownIdFirebase.createLoginIntent(this, "en", emailAddress)
        loginLauncher.launch(loginIntent)
    }

    private fun registerWithPassword() {
        Firebase.auth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnSuccessListener { gotoLoginPage() }
            .addOnFailureListener { showMessage(it.message) }
    }

    private fun registerWithSkipPassword() {
        if (validateEmail(emailAddress)) {
            val registerIntent = ownIdFirebase.createRegisterIntent(this, "en", emailAddress)
            registerLauncher.launch(registerIntent)
        } else {
            binding.emailLayout.error = "The email is not valid"
        }
    }

    private fun gotoRegisterPage() {
        registerVisible = true
        loginVisible = false
    }

    private fun gotoLoginPage() {
        registerVisible = false
        loginVisible = true
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showMessage(message: String?) {
        binding.tvMessage.text = message
    }

    private fun onOwnIdRegisterResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.register(username, emailAddress, ownIdResponse)
            .addOnSuccessListener {
                showMessage("Registered successfully")
                gotoLoginPage()
            }
            .addOnFailureListener { cause -> showMessage(cause.message) }
    }

    private fun onOwnIdLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.login(ownIdResponse)
            .addOnSuccessListener { showMessage("Logged in successfully") }
            .addOnFailureListener {
                when (it) {
                    is CancellationException -> throw it
                    is EmailAndPasswordRequired -> onOwnIdLinkOnLoginResponse(ownIdResponse)
                    else -> showMessage(it.message)
                }
            }
    }

    private fun onOwnIdLinkOnLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.loginAndLink(emailAddress, password, ownIdResponse)
            .addOnSuccessListener { showMessage("Link and login successfully") }
            .addOnFailureListener { showMessage(it.message) }
    }
}