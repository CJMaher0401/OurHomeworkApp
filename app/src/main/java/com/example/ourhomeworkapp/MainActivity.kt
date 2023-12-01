package com.example.ourhomeworkapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    //created my own personal branch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen_layout)

        val homeProfileButton : Button = findViewById(R.id.profileButton)
        val addHWbutton : ImageButton = findViewById(R.id.addHWbutton)

        homeProfileButton.setOnClickListener{
            profileScreen()
        }

        addHWbutton.setOnClickListener{
            addHWScreen()
        }
    }
    private fun profileScreen()
    {
        setContentView(R.layout.profilescreen_layout)
    }
    private fun addHWScreen()
    {
        setContentView(R.layout.addhomeworkscreen_layout)
    }
}
