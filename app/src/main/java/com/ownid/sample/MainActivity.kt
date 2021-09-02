package com.ownid.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ownid.sample.databinding.ActivityMainBinding
import com.ownid.sdk.*
import com.ownid.sdk.exception.EmailAndPasswordRequired
import kotlinx.coroutines.CancellationException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ownIdFirebase: OwnIdFirebase

    private val ownIdRegisterResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.root.updateOwnIdView(result.resultCode)
            runCatching { result.data.getOwnIdResponse(result.resultCode) }
                .onSuccess { ownIdResponse: OwnIdResponse ->
                    Toast.makeText(
                        this@MainActivity,
                        "Register success with response $ownIdResponse", Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@MainActivity,
                        "Register fail with error $error", Toast.LENGTH_LONG
                    ).show()
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
            ownIdRegisterResultLauncher.launch(registerIntent)
        }
    }

    fun onOwnIdLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.login(ownIdResponse)
            .addOnSuccessListener {

            }
            .addOnFailureListener { cause ->
                when (cause) {
                    is CancellationException -> throw cause
                    is EmailAndPasswordRequired -> onOwnIdLinkOnLoginResponse(ownIdResponse)
                    else -> showError(cause)
                }
            }
    }

    fun onOwnIdLinkOnLoginResponse(ownIdResponse: OwnIdResponse) {
        ownIdFirebase.loginAndLink("", "", ownIdResponse)
            .addOnSuccessListener {
            }
            .addOnFailureListener { cause ->
                if (cause is CancellationException) throw cause
                showError(cause)
            }
    }
}