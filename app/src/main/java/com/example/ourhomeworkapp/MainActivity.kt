package com.example.ourhomeworkapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import java.util.Stack


class MainActivity : ComponentActivity() {

    private val layoutStack: Stack<Int> = Stack()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen_layout)

        findViewById<Button>(R.id.profileButton).setOnClickListener{
            inflateLayout(R.layout.profilescreen_layout)
        }
        findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener{
            inflateLayout(R.layout.addhomeworkscreen_layout)
        }
        findViewById<Button>(R.id.homeScreenButton).setOnClickListener{
            inflateLayout(R.layout.homescreen_layout)
        }
        findViewById<Button>(R.id.homeScreenMyHWButton).setOnClickListener{
            inflateLayout(R.layout.currentupcominghw_layout)
        }
        findViewById<Button>(R.id.homeScreenMyProfileButton).setOnClickListener{
            inflateLayout(R.layout.profilescreen_layout)
        }
    }

    private fun inflateLayout(layoutResID: Int)
    {

        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(layoutResID, null)

        setContentView(layout)

        when (layoutResID) {
            R.layout.homescreen_layout -> {
                findViewById<Button>(R.id.profileButton).setOnClickListener{
                    inflateLayout(R.layout.profilescreen_layout)
                }
                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<Button>(R.id.homeScreenButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<Button>(R.id.homeScreenMyHWButton).setOnClickListener{
                    inflateLayout(R.layout.currentupcominghw_layout)
                }
                findViewById<Button>(R.id.homeScreenMyProfileButton).setOnClickListener{
                    inflateLayout(R.layout.profilescreen_layout)
                }
            }
            R.layout.profilescreen_layout -> {
                findViewById<Button>(R.id.profileHomeButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<Button>(R.id.profileMyHWButton).setOnClickListener{
                    inflateLayout(R.layout.currentupcominghw_layout)
                }
                findViewById<Button>(R.id.profileMyProfileButton).setOnClickListener{
                    inflateLayout(R.layout.profilescreen_layout)
                }
            }
            R.layout.addhomeworkscreen_layout -> {
                findViewById<Button>(R.id.addHWcancelButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<Button>(R.id.addHWsaveButton).setOnClickListener{
                    inflateLayout(R.layout.currentupcominghw_layout)
                }
                findViewById<EditText>(R.id.editClassDescText).setOnClickListener{
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }
                findViewById<EditText>(R.id.editDueDateText).setOnClickListener{
                    inflateLayout(R.layout.duedateselectionscreen_layout)
                }
                findViewById<EditText>(R.id.editReminderText).setOnClickListener{
                    inflateLayout(R.layout.reminderdateselectionscreen_layout)
                }
            }
            R.layout.yourcoursesscreen_layout -> {
                findViewById<ImageButton>(R.id.returnToAddHWButton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<ImageButton>(R.id.addNewClassButton).setOnClickListener{
                    inflateLayout(R.layout.coursecreationscreen_layout)
                }
            }
            R.layout.coursecreationscreen_layout -> {
                findViewById<Button>(R.id.coursecancelButton).setOnClickListener{
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }
                findViewById<Button>(R.id.coursesaveButton).setOnClickListener{
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }
            }
            R.layout.duedateselectionscreen_layout -> {

            }
            R.layout.currentupcominghw_layout -> {
                findViewById<Button>(R.id.curHWcancelButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<Button>(R.id.curHWaddHWbutton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<Button>(R.id.curHWButton).setOnClickListener{
                    inflateLayout(R.layout.currentupcominghw_layout)
                }
                findViewById<Button>(R.id.compHWButton).setOnClickListener{
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
            }
            R.layout.completedhwscreen_layout -> {
                findViewById<Button>(R.id.compHWcancelButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<Button>(R.id.compHWaddHWbutton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<Button>(R.id.compCurHWButton).setOnClickListener{
                    inflateLayout(R.layout.currentupcominghw_layout)
                }
                findViewById<Button>(R.id.compCompHWButton).setOnClickListener{
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
            }
        }
    }
}
