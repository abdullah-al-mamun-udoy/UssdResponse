package com.android.ussdcall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.ussdcall.ui.theme.USSDcallTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            USSDcallTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MissedCallAlertApp(paddingValues = innerPadding)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MissedCallAlertApp(paddingValues: PaddingValues) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        USSDButton(context)
    }

}

const val PHONE_CALL_PERMISSION_REQUEST_CODE = 1

private fun requestPhonePermission(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            PHONE_CALL_PERMISSION_REQUEST_CODE
        )
    }
}
fun dialUSSDCode(context: Context, ussdCode: String) {
    val intent = Intent(Intent.ACTION_CALL)
    intent.data = Uri.parse("tel:${Uri.encode(ussdCode)}")

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
        == PackageManager.PERMISSION_GRANTED) {
        context.startActivity(intent)
    } else {
        requestPhonePermission(context)
    }
}

/*@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ServiceCast")
fun dialUSSDCode(context: Context, ussdCode: String) {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // Check for the required permission
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        // Handle the case where permission is not granted (request it)
        return
    }

   *//* telephonyManager.sendUssdRequest(ussdCode, object : TelephonyManager.UssdResponseCallback() {
        fun onResponse(request: String, response: CharSequence) {
            // Handle the USSD response
            // Here you can process the successful response
            println("USSD Response: $response")
        }

        fun onResponse(request: String, response: CharSequence, resultCode: Int) {
            // Handle the USSD response with result code
            when (resultCode) {

                TelephonyManager.USSD_RESPONSE_SUCCESS -> {
                    // Handle success
                    println("USSD Request Success: $response")
                }
                TelephonyManager.USSD_RESPONSE_FAILURE -> {
                    // Handle failure
                    println("USSD Request Failed")
                }
                TelephonyManager.USSD_RESPONSE_ERROR -> {
                    // Handle error
                    println("USSD Request Error")
                }
                else -> {
                    // Handle other cases
                    println("Unknown USSD Response")
                }
            }
        }
    }, null) // Add a handler if needed*//*
}*/

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun USSDButton(context: Context) {
    var ussdSent by remember { mutableStateOf<Boolean?>(null) }

    val ussdCode: String? = "*123#"
    Button(onClick = {
        try {
            if (ussdCode != null) {
                dialUSSDCode(context, ussdCode)
            }
            ussdSent = true
//            Toast.makeText(context, "USSD Code Sent Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            ussdSent = false
        }
    }) {
        Text("Dial USSD Code")
    }

    ussdSent?.let { isSuccess ->
        FeedbackSnackbar(isSuccess = isSuccess, ussdCode = ussdCode ?: "*222#")
        LaunchedEffect(isSuccess) {
            kotlinx.coroutines.delay(5000)
            ussdSent = null
        }
    }
}

@Composable
fun FeedbackSnackbar(isSuccess: Boolean, ussdCode: String?) {
    if (isSuccess) {
        Snackbar(
            modifier = Modifier.padding(top = 30.dp)
        ) {
            Text(text = "USSD Code Sent Successfully, the code is $ussdCode ")
        }
    } else {
        Snackbar(
            modifier = Modifier.padding(top = 30.dp)
        ) {
            Text(text = "USSD Code Failed to Send, the code is $ussdCode ")
        }
    }
}


/*
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted
            } else {
                // Permission denied
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MissedCallAlertApp()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun MissedCallAlertApp() {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            USSDCode(context = context)
        }
    }
    @Composable
    fun USSDCode(context: Context) {
        // State for holding USSD code input
        var ussdCode by remember { mutableStateOf("") }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp) // Add padding for clarity
        ) {
            Text("Type your USSD Code Below")

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ussdCode,
                onValueChange = { newValue ->
                    ussdCode = newValue  // Updates the ussdCode when input changes
                },
                placeholder = { Text("*222#") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Whenever ussdCode changes, this composable will automatically recompose
            if (ussdCode.isNotEmpty()) {
                USSDButton(context, ussdCode)
            }
        }

        Log.d("ussd", "USSD code is $ussdCode")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun USSDButton(context: Context, ussdCode: String) {
        var ussdSent by remember { mutableStateOf<Boolean?>(null) }
        var ussdResult by remember { mutableStateOf("") }
        val ussdCode = ussdCode

        Button(onClick = {
            // Check for permission before dialing
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Log.d("ussd", "if execute in button $ussdCode")
                dialUSSDCode(context, ussdCode)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }) {
            Text("Dial USSD Code")
        }

        ussdSent?.let { isSuccess ->
            FeedbackSnackbar(isSuccess = isSuccess, ussdCode = ussdCode, ussdResult = ussdResult)
            LaunchedEffect(isSuccess) {
                kotlinx.coroutines.delay(5000)
                ussdSent = null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun dialUSSDCode(
        context: Context,
        ussdCode: String,
    ) {

        Log.d("ussd", "dialUSSDCode execute and ussdcode is $ussdCode")
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            telephonyManager.sendUssdRequest(ussdCode, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager,
                    request: String,
                    response: CharSequence
                ) {
                    Log.d("ussd", "USSD Response: $response for request $request")
                    println("USSD Response: $response for request $request")
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager,
                    request: String,
                    failureCode: Int
                ) {
                    val failureMessage = when (failureCode) {
                        TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "Service Unavailable"
                        TelephonyManager.USSD_RETURN_FAILURE -> "USSD Request Failed"
                        else -> "Unknown Error"
                    }
                    Log.e("ussd", "USSD Request Failed: $failureMessage for request $request with code $failureCode")
                }
            }, Handler(Looper.getMainLooper()))

        } catch (e: Exception) {
            Log.e("ussd", "Error while executing USSD request: ${e.message}")
            e.printStackTrace()
        }
    }

    @Composable
    fun FeedbackSnackbar(isSuccess: Boolean, ussdCode: String, ussdResult: String) {
        Snackbar(
            modifier = Modifier.padding(top = 30.dp)
        ) {
            Text(text = if (isSuccess) {
                "USSD Code Sent Successfully: $ussdResult"
            } else {
                "USSD Code Failed to Send: $ussdResult"
            })
        }
    }
}
*/
