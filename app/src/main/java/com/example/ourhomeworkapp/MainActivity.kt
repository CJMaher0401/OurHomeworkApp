package com.example.ourhomeworkapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
                val editDueDateText = findViewById<EditText>(R.id.editDueDateText)
                editDueDateText.setOnClickListener{
                    showDatePicker(editDueDateText)
                }
                val editReminderText = findViewById<EditText>(R.id.editReminderText)
                editReminderText.setOnClickListener{
                    showDateAndTimePicker(editReminderText)
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
                findViewById<Button>(R.id.dueDateCancelButton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<Button>(R.id.dueDateSaveButton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
            }
            R.layout.reminderdateselectionscreen_layout -> {
                findViewById<Button>(R.id.reminderCancelButton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
                findViewById<Button>(R.id.reminderSaveButton).setOnClickListener{
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }
            }
            R.layout.currentupcominghw_layout -> {
                findViewById<Button>(R.id.curHWcancelButton).setOnClickListener{
                    inflateLayout(R.layout.homescreen_layout)
                }
                findViewById<ImageButton>(R.id.curHWaddHWbutton).setOnClickListener{
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
                findViewById<ImageButton>(R.id.compHWaddHWbutton).setOnClickListener{
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

    private fun showDatePicker(editText: EditText) {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showDateAndTimePicker(editText: EditText) {
        val currentDateTime = Calendar.getInstance()
        val year = currentDateTime.get(Calendar.YEAR)
        val month = currentDateTime.get(Calendar.MONTH)
        val day = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val hour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentDateTime.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val timePickerDialog = TimePickerDialog(this,
                    { _, selectedHour, selectedMinute ->
                        val selectedDateTime = Calendar.getInstance()
                        selectedDateTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)

                        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                        editText.setText(dateFormat.format(selectedDateTime.time))
                    },
                    hour,
                    minute,
                    false
                )

                timePickerDialog.show()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}
