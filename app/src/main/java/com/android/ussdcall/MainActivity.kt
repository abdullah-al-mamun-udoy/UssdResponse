package com.android.ussdcall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.ussdcall.ui.theme.USSDcallTheme

const val PHONE_CALL_PERMISSION_REQUEST_CODE = 1
val ussdCode: String? = "*2#"
val simSlotIndex: Int = 1 // Change to the desired SIM slot index

class MainActivity : ComponentActivity() {

    // ActivityResultLauncher for permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            // Permissions were granted, retry the USSD request
            ussdCode?.let { dialUSSDCode(this, it, simSlotIndex) }
        } else {
            // Inform the user that permissions were denied
            Toast.makeText(this, "Permission denied. Cannot proceed with USSD request.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            USSDcallTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MissedCallAlertApp(paddingValues = innerPadding)
                }
            }
        }
    }

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

    @SuppressLint("HardwareIds")
    @RequiresApi(Build.VERSION_CODES.O)
    fun dialUSSDCode(context: Context, ussdCode: String, simSlotIndex: Int) {
        // Check for permissions before proceeding
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Request permissions if not granted
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.READ_SMS
                )
            )
            return
        }

        // Proceed with USSD request if permissions are granted
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            val subscription =SubscriptionManager.from(context).activeSubscriptionInfoList
            for (subscriptionInfo in subscription)
            {
                val number = subscriptionInfo.number
                Log.e("dialUSSDCode", " Number is  " + number)
            }

            Log.d("dialUSSDCode", "Number of active subscriptions: ${subscriptionInfoList.size}")

            if (simSlotIndex < subscriptionInfoList.size) {
                val subscriptionId = subscriptionInfoList[simSlotIndex].subscriptionId
                Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subscriptionId")

                val telephonyManager = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                    .createForSubscriptionId(subscriptionId)
                // Get the phone number for the specific subscription ID
                var phoneNumber: String? = null

                // Check if getPhoneNumber exists using reflection
                val method = try {
                    subscriptionManager.javaClass.getMethod("getPhoneNumber", Int::class.java)
                } catch (e: NoSuchMethodException) {
                    null
                }

                if (method != null) {
                    // Use getPhoneNumber method if available
                    phoneNumber = method.invoke(subscriptionManager, subscriptionId) as? String
                    Log.d("dialUSSDCode", "yes method exist")
                }
                else {
                    Log.d("dialUSSDCode", "yes method exist")
                    // Fallback to line1Number for older versions
//                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    phoneNumber = telephonyManager.line1Number
                }

                if (phoneNumber.isNullOrEmpty()) {
                    Log.d("dialUSSDCode", "Phone number for SIM slot $simSlotIndex: Not available")
                    Toast.makeText(context, "Phone number not available for SIM slot $simSlotIndex", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("dialUSSDCode", "Phone number for SIM slot $simSlotIndex: $phoneNumber")
                    Toast.makeText(context, "Phone number for SIM slot $simSlotIndex: $phoneNumber", Toast.LENGTH_SHORT).show()
                }

                telephonyManager.sendUssdRequest(
                    ussdCode,
                    object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager,
                            request: String,
                            response: CharSequence
                        ) {
                            Log.d("dialUSSDCode", "USSD Response: $response for request $request")
                            Toast.makeText(context, "USSD Response: $response for request $request", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "USSD Request Failed: $failureMessage for request $request with code $failureCode", Toast.LENGTH_LONG).show()
                            Log.e("dialUSSDCode", "USSD Request Failed: $failureMessage for request $request with code $failureCode")
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                Log.e("dialUSSDCode", "simSlotIndex $simSlotIndex out of bounds for subscriptionInfoList size: ${subscriptionInfoList.size}")
                Toast.makeText(context, "Invalid SIM slot index", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("dialUSSDCode", "Error while executing USSD request: ${e.message}", e)
            Toast.makeText(context, "Error while executing USSD request: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun USSDButton(context: Context) {
        Button(onClick = {
            ussdCode?.let { dialUSSDCode(context, it, simSlotIndex) }
        }) {
            Text("Dial USSD Code")
        }
    }
}
