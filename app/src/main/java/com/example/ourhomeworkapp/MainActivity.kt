package com.example.ourhomeworkapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {

    private lateinit var phoneNumberText: EditText
    private lateinit var messageText: EditText
    private lateinit var confirmText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.smstest_layout)

        fun hasSMSperm(): Boolean
        {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        }

        fun requestSMSperm()
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1318)
        }

        fun clearTextFields()
        {
            phoneNumberText.text.clear()
            messageText.text.clear()
        }

        fun sendSMSmessage()
        {
            val phoneNumber = phoneNumberText.text.toString()
            val message = messageText.text.toString()

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            confirmText.text = "Sent SMS Message to $phoneNumber"
            confirmText.visibility = TextView.VISIBLE
            clearTextFields()
        }

        fun onReqPermResult(requestCode: Int, permissions: Array<out String>, permGrantResult: IntArray)
        {
            super.onRequestPermissionsResult(requestCode, permissions, permGrantResult)

            if(requestCode == 1318)
            {
                if(permGrantResult.isNotEmpty() && permGrantResult[0] == PackageManager.PERMISSION_GRANTED)
                {
                    sendSMSmessage()
                }
                else
                {
                    Toast.makeText(this, "SMS permission was denied", Toast.LENGTH_SHORT)
                }
            }
        }

        phoneNumberText = findViewById(R.id.editTextPhoneNumber)
        messageText = findViewById(R.id.editTextMessage)
        confirmText = findViewById(R.id.textViewConfirmation)

        val sendSMSButton: Button = findViewById(R.id.buttonSend)
        sendSMSButton.setOnClickListener{
            if(hasSMSperm())
            {
                sendSMSmessage()
            }
            else
            {
                requestSMSperm()
            }
        }
    }
}
