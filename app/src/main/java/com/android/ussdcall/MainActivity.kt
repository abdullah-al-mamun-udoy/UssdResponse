package com.android.ussdcall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
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
import java.lang.reflect.Method

const val PHONE_CALL_PERMISSION_REQUEST_CODE = 1
val ussdCode: String? = "+8801864631440"

val simSlotIndex: Int = 1
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



private fun requestPhonePermission(context: Context) {
    val permissions = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_SMS
    )

    if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {

        ActivityCompat.requestPermissions(
            context as Activity,
            permissions,
            PHONE_CALL_PERMISSION_REQUEST_CODE
        )
    } else {
        // Permissions already granted, proceed with phone-related operations
    }
}
/*@RequiresApi(Build.VERSION_CODES.Q)
fun dialUSSDCode(context: Context, ussdCode: String, simSlotIndex: Int) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:${Uri.encode(ussdCode)}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    Log.d("dialUSSDCode", "Using SIM slot index: $simSlotIndex")

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            // Log number of active subscriptions
            Log.d("dialUSSDCode", "Number of active subscriptions: ${subscriptionInfoList.size}")

            if (simSlotIndex < subscriptionInfoList.size) {
                // Correctly access the subscription info by using simSlotIndex as an index
                val subscriptionInfo = subscriptionInfoList[simSlotIndex]
                val subIdForSlot = subscriptionInfo.subscriptionId
                Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subIdForSlot")

                // Get carrier name
                val carrierName = subscriptionInfo.carrierName


                Log.d("dialUSSDCode", "Carrier Name for SIM slot $simSlotIndex: $carrierName")
//                getCarrierName(context,subscriptionManager,simSlotIndex)

                // Set the phone account handle for the specific SIM
                val componentName = ComponentName(
                    "com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService"
                )
                val phoneAccountHandle = PhoneAccountHandle(componentName, subIdForSlot.toString())
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)

                context.startActivity(intent)
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val phoneNumber = telephonyManager.line1Number
                Log.d("dialUSSDCode", "Phone Number: $phoneNumber")
                try {
                    telephonyManager.sendUssdRequest(
                        ussdCode,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(
                                telephonyManager: TelephonyManager,
                                request: String,
                                response: CharSequence
                            ) {
                                Log.d(
                                    "dialUSSDCode",
                                    "USSD Response: $response for request $request"
                                )
//                            println("USSD Response: $response for request $request")
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
                                Log.e(
                                    "dialUSSDCode",
                                    "USSD Request Failed: $failureMessage for request $request with code $failureCode"
                                )
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )

                } catch (e: Exception) {
                    Log.e("dialUSSDCode", "Error while executing USSD request: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(
                    "dialUSSDCode",
                    "simSlotIndex $simSlotIndex out of bounds for subscriptionInfoList size: ${subscriptionInfoList.size}"
                )
                Toast.makeText(context, "Invalid SIM slot index", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("dialUSSDCode", "Error while initiating call: ${e.message}", e)
            Toast.makeText(context, "Error while initiating call: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    } else {
        requestPhonePermission(context)
    }
}*/


@RequiresApi(Build.VERSION_CODES.Q)
fun dialUSSDCodes(context: Context, ussdCode: String, simSlotIndex: Int) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:${Uri.encode(ussdCode)}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    Log.d("dialUSSDCode", "Using SIM slot index: $simSlotIndex")

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val getSubIdMethod: Method = SubscriptionManager::class.java.getDeclaredMethod("getSubId", Int::class.java)
            getSubIdMethod.isAccessible = true

            // Get the Subscription ID for the specified SIM slot
            val subIdArray = getSubIdMethod.invoke(subscriptionManager, simSlotIndex) as IntArray
            if (subIdArray.isNotEmpty()) {
                // Validate simSlotIndex to ensure it's within bounds
                if (simSlotIndex < subIdArray.size) {
                    val subIdForSlot = subIdArray[simSlotIndex]
                    Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subIdForSlot")

                    // Get carrier name
//                    val carrierName = getCarrierName(context, subscriptionManager, simSlotIndex)
//                    Log.d("dialUSSDCode", "Carrier Name for SIM slot $simSlotIndex: $carrierName")

                    // Set the phone account handle for the specific SIM
                    val componentName = ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService")
                    val phoneAccountHandle = PhoneAccountHandle(componentName, subIdForSlot.toString())
                    intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)

                    context.startActivity(intent)
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val phoneNumber = telephonyManager.line1Number
                    Log.d("dialUSSDCode", "Phone Number: $phoneNumber")
                    try {
                        telephonyManager.sendUssdRequest(
                            ussdCode,
                            object : TelephonyManager.UssdResponseCallback() {
                                override fun onReceiveUssdResponse(
                                    telephonyManager: TelephonyManager,
                                    request: String,
                                    response: CharSequence
                                ) {
                                    Log.d(
                                        "dialUSSDCode",
                                        "USSD Response: $response for request $request"
                                    )
//                            println("USSD Response: $response for request $request")
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
                                    Log.e(
                                        "dialUSSDCode",
                                        "USSD Request Failed: $failureMessage for request $request with code $failureCode"
                                    )
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )

                    } catch (e: Exception) {
                        Log.e("dialUSSDCode", "Error while executing USSD request: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    Log.e("dialUSSDCode", "simSlotIndex $simSlotIndex out of bounds for subIdArray size: ${subIdArray.size}")
                    Toast.makeText(context, "Invalid SIM slot index", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("dialUSSDCode", "No subscription ID found for SIM slot $simSlotIndex")
                Toast.makeText(context, "No subscription ID found for the selected SIM slot", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("dialUSSDCode", "Error while initiating call: ${e.message}", e)
            Toast.makeText(context, "Error while initiating call: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        requestPhonePermission(context)
    }
}

// the working one but second one is implementing
@RequiresApi(Build.VERSION_CODES.Q)
fun dialUSSDCode1(context: Context, ussdCode: String, simSlotIndex: Int) {
    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + ussdCode + Uri.encode("#")))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    Log.d("dialUSSDCode", "Using SIM slot index: $simSlotIndex")

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            // Log number of active subscriptions
            Log.d("dialUSSDCode", "Number of active subscriptions: ${subscriptionInfoList.size}")

            if (simSlotIndex < subscriptionInfoList.size) {
                // Correctly access the subscription info by using simSlotIndex as an index
                val subscriptionInfo = subscriptionInfoList[simSlotIndex]
                val subIdForSlot = subscriptionInfo.subscriptionId
                Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subIdForSlot")

                // Get carrier name
                val carrierName = subscriptionInfo.carrierName


                Log.d("dialUSSDCode", "Carrier Name for SIM slot $simSlotIndex: $carrierName")
//                getCarrierName(context,subscriptionManager,simSlotIndex)

                // Set the phone account handle for the specific SIM
                val componentName = ComponentName(
                    "com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService"
                )
                val phoneAccountHandle = PhoneAccountHandle(componentName, subIdForSlot.toString())
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)

                context.startActivity(intent)

                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                try {
                    telephonyManager.sendUssdRequest(
                        ussdCode,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(
                                telephonyManager: TelephonyManager,
                                request: String,
                                response: CharSequence
                            ) {
                                Log.d(
                                    "dialUSSDCode",
                                    "USSD Response: $response for request $request"
                                )
//                            println("USSD Response: $response for request $request")
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
                                Log.e(
                                    "dialUSSDCode",
                                    "USSD Request Failed: $failureMessage for request $request with code $failureCode"
                                )
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )

                } catch (e: Exception) {
                    Log.e("dialUSSDCode", "Error while executing USSD request: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(
                    "dialUSSDCode",
                    "simSlotIndex $simSlotIndex out of bounds for subscriptionInfoList size: ${subscriptionInfoList.size}"
                )
                Toast.makeText(context, "Invalid SIM slot index", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("dialUSSDCode", "Error while initiating call: ${e.message}", e)
            Toast.makeText(context, "Error while initiating call: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    } else {
        requestPhonePermission(context)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun dialUSSDCode(context: Context, ussdCode: String, simSlotIndex: Int) {

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CALL_PHONE),
            PHONE_CALL_PERMISSION_REQUEST_CODE
        )
        return
    }

    try {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

        Log.d("dialUSSDCode", "Number of active subscriptions: ${subscriptionInfoList.size}")

        if (simSlotIndex < subscriptionInfoList.size) {
            val subscriptionId = subscriptionInfoList[simSlotIndex].subscriptionId
            Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subscriptionId")

            val telephonyManager = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                .createForSubscriptionId(subscriptionId)

            telephonyManager.sendUssdRequest(
                "*222#",
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        Log.d("dialUSSDCode", "USSD Response: $response for request $request")
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
        Toast.makeText(context, "Error while executing USSD request: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


//  differen approach for using intent in the end
@RequiresApi(Build.VERSION_CODES.O)
fun differentdialUSSDCode(context: Context, ussdCode: String, simSlotIndex: Int) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CALL_PHONE),
            PHONE_CALL_PERMISSION_REQUEST_CODE
        )
        return
    }

    val formattedUssdCode = "*222" + Uri.encode("#") // Ensure correct formatting

    try {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

        Log.d("dialUSSDCode", "Number of active subscriptions: ${subscriptionInfoList.size}")

        if (simSlotIndex < subscriptionInfoList.size) {
            val subscriptionId = subscriptionInfoList[simSlotIndex].subscriptionId
            Log.d("dialUSSDCode", "Subscription ID for SIM slot $simSlotIndex: $subscriptionId")

            val telephonyManager = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                .createForSubscriptionId(subscriptionId)

            telephonyManager.sendUssdRequest(
                formattedUssdCode,
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        Log.d("dialUSSDCode", "USSD Response: $response for request $request")
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
                        Log.e("dialUSSDCode", "USSD Request Failed: $failureMessage for request $request with code $failureCode")

                        // Fallback to intent if USSD request fails
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$formattedUssdCode"))
                        context.startActivity(intent)
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
        Toast.makeText(context, "Error while executing USSD request: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}





@RequiresApi(Build.VERSION_CODES.Q)
private fun getCarrierName(
    context: Context,
    subscriptionManager: SubscriptionManager,
    simSlotIndex: Int
): String? {
    // Check for READ_PHONE_STATE permission
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.e("getCarrierName", "READ_PHONE_STATE permission not granted")
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            100
        )
        return null // Permission not granted, return null and request permission
    }

    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) { // API level 22
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

        if (subscriptionInfoList.size > 1) {
            val simInfo = subscriptionInfoList[simSlotIndex]
            Log.d("getCarrierName", "SIM slot index: $simSlotIndex")
            simInfo.displayName.toString() // Return the display name for the specific SIM slot
        } else if (subscriptionInfoList.isNotEmpty()) {
            subscriptionInfoList[0].displayName.toString() // Return the display name for the single SIM
        } else {
            Log.e("getCarrierName", "No active subscription found")
            null
        }
    } else {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.networkOperatorName // Return the network operator name
    }
}


// Function to retrieve the carrier name using the Subscription ID
//@RequiresApi(Build.VERSION_CODES.Q)
//private fun getCarrierName(context: Context, subscriptionManager: SubscriptionManager, subId: Int): String? {
//    // Check for READ_PHONE_STATE permission
//    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//        // If permission is not granted, you can request it here or handle the lack of permission
//        Log.e("getCarrierName", "READ_PHONE_STATE permission not granted")
//        return null
//    }
//
//    // If permission is granted, retrieve the carrier name
//    val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
//    for (subscriptionInfo in subscriptionInfoList) {
//        if (subscriptionInfo.carrierId == subId) {
//            return subscriptionInfo.carrierName.toString()
//        }
//    }
//    return null
//}


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

    Button(onClick = {
        try {
            if (ussdCode != null) {
                dialUSSDCode(context, ussdCode, simSlotIndex)
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
