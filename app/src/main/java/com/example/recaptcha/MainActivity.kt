package com.example.recaptcha

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val siteKey = "6LcCrcYaAAAAABMgbAV4rQQWMoORE1ILjkRA3_s4"
    private val secretKey = "6LcCrcYaAAAAAFcgceHN2ZrB1Dd8YC9vqvFRzN1H"

    private lateinit var launchButton: Button
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        queue = Volley.newRequestQueue(applicationContext)
        launchButton = findViewById(R.id.button)
        launchButton.setOnClickListener { launchRecaptcha() }
    }

    private fun launchRecaptcha() {
        SafetyNet.getClient(this).verifyWithRecaptcha(siteKey)
            .addOnSuccessListener {
                handleSuccess(it)
            }
            .addOnFailureListener {
                handleFailure(it)
            }
    }

    private fun handleSuccess(recaptchaTokenResponse: SafetyNetApi.RecaptchaTokenResponse) {
        if (recaptchaTokenResponse.tokenResult.isNotEmpty()) {
            handleSiteVerification(recaptchaTokenResponse.tokenResult)
        }
    }

    private fun handleFailure(exception: Exception) {
        if (exception is ApiException) {
            Log.d(
                "INFO",
                "Error message: " + CommonStatusCodes.getStatusCodeString(exception.statusCode)
            )
        } else {
            Log.d("INFO", "Unknown type of error: " + exception.message)
        }
    }

    private fun handleSiteVerification(tokenResult: String) {
        val url = "https://www.google.com/recaptcha/api/siteverify"
        val request: StringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        Toast.makeText(applicationContext, "It worked!",Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            jsonObject.getString("error-codes").toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (ex: Exception) {
                    Log.d("INFO", "JSON exception: " + ex.message)
                }
            },
            Response.ErrorListener { error -> Log.d("INFO", "Error message: " + error.message) }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["secret"] = secretKey
                params["response"] = tokenResult
                return params
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            50000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }
}