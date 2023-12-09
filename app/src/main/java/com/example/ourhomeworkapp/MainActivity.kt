package com.example.ourhomeworkapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorEditText: EditText

    data class Course(val courseName: String, val courseColor: Int)
    private lateinit var courseList: MutableList<Course>
    private lateinit var addHomeworkLayout: View
    private lateinit var editClassDescText: EditText

    data class Homework(val courseName: String, val assignmentDesc: String, val dueDate: String, val color: Int)
    private lateinit var homeworkList: MutableList<Homework>

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        courseList = mutableListOf()
        addHomeworkLayout = layoutInflater.inflate(R.layout.addhomeworkscreen_layout, null)
        editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)

        homeworkList = mutableListOf()

        inflateLayout(R.layout.homescreen_layout)
    }


    private fun inflateLayout(layoutResID: Int, afterInflate: (() -> Unit)? = null) {

        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(layoutResID, null)

        setContentView(layout)

        afterInflate?.invoke()

        when (layoutResID)
        {
            R.layout.homescreen_layout -> {
                findViewById<Button>(R.id.profileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout)
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.homeScreenButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.homeScreenMyHWButton).setOnClickListener {
                    switchToCurrentUpcomingLayout()
                }

                findViewById<Button>(R.id.homeScreenMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout)
                }
                val recyclerViewOne: RecyclerView = findViewById(R.id.homeScreenRecycler) ?: run {
                    Log.e("inflateLayout", "RecyclerView (R.id.homeScreenRecycler) not found in homescreen_layout")
                    return
                }
                recyclerViewOne.layoutManager = LinearLayoutManager(this)
                if (recyclerViewOne != null) {
                    updateHomeworkRecyclerViews()
                }
            }

            R.layout.profilescreen_layout -> {
                findViewById<Button>(R.id.profileHomeButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.profileMyHWButton).setOnClickListener {
                    switchToCurrentUpcomingLayout()
                }

                findViewById<Button>(R.id.profileMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout)
                }
            }

            R.layout.addhomeworkscreen_layout -> {
                findViewById<Button>(R.id.addHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.addHWsaveButton).setOnClickListener {
                    Log.d("SaveButton", "Save button clicked")
                    val courseDesc = this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
                    val assignmentDesc = findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
                    val dueDate = this.findViewById<EditText>(R.id.editDueDateText).text.toString()
                    val color = findViewById<EditText>(R.id.editCourseDescText).currentTextColor

                    val homework = Homework(courseDesc, assignmentDesc, dueDate, color)

                    homeworkList.add(homework)

                    Log.d("HomeworkList", homeworkList.toString())

                    updateHomeworkRecyclerViews()

                    switchToCurrentUpcomingLayout()
                }

                findViewById<EditText>(R.id.editCourseDescText).setOnClickListener {
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }

                val editDueDateText = findViewById<EditText>(R.id.editDueDateText)
                editDueDateText.setOnClickListener {
                    showDatePicker(editDueDateText)
                }

                val editReminderText = findViewById<EditText>(R.id.editReminderText)
                editReminderText.setOnClickListener {
                    showDateAndTimePicker(editReminderText)
                }

                editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)
            }

            R.layout.yourcoursesscreen_layout -> {
                findViewById<ImageButton>(R.id.returnToAddHWButton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<ImageButton>(R.id.addNewClassButton).setOnClickListener {
                    inflateLayout(R.layout.coursecreationscreen_layout)
                }

                val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
                if (recyclerView != null)
                {
                    updateCourseRecyclerView()
                }
            }

            R.layout.coursecreationscreen_layout -> {
                findViewById<Button>(R.id.coursecancelButton).setOnClickListener {
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }

                findViewById<Button>(R.id.coursesaveButton).setOnClickListener {
                    val courseName = this.findViewById<EditText>(R.id.nameOfCourseText).text.toString()
                    val courseColor = findViewById<EditText>(R.id.nameOfCourseText).currentTextColor

                    if(!courseList.any {it.courseName == courseName})
                    {
                        saveCourse(courseName, courseColor)

                        updateCourseRecyclerView()

                    }
                    inflateLayout(R.layout.yourcoursesscreen_layout)
                }

                findViewById<Button>(R.id.pickYourColorButton).setOnClickListener {
                    showColorWheel()
                }
            }

            R.layout.currentupcominghw_layout -> {
                findViewById<Button>(R.id.curHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<ImageButton>(R.id.curHWaddHWbutton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.curHWButton).setOnClickListener {
                    switchToCurrentUpcomingLayout()
                }

                findViewById<Button>(R.id.compHWButton).setOnClickListener {
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
                val recyclerViewTwo: RecyclerView = findViewById(R.id.homeScreenRecycler) ?: run {
                    Log.e("inflateLayout", "RecyclerView (R.id.homeScreenRecycler) not found in homescreen_layout")
                    return
                }
                recyclerViewTwo.layoutManager = LinearLayoutManager(this)
                if (recyclerViewTwo != null) {
                    updateHomeworkRecyclerViews()
                }
            }

            R.layout.completedhwscreen_layout -> {
                findViewById<Button>(R.id.compHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<ImageButton>(R.id.compHWaddHWbutton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.compCurHWButton).setOnClickListener {
                    switchToCurrentUpcomingLayout()
                }

                findViewById<Button>(R.id.compCompHWButton).setOnClickListener {
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
            }
        }
    }

    private fun showDatePicker(editText: EditText)
    {
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
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun showDateAndTimePicker(editText: EditText)
    {
        val currentDateAndTime = Calendar.getInstance()
        val year = currentDateAndTime.get(Calendar.YEAR)
        val month = currentDateAndTime.get(Calendar.MONTH)
        val day = currentDateAndTime.get(Calendar.DAY_OF_MONTH)
        val hour = currentDateAndTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentDateAndTime.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val timePickerDialog = TimePickerDialog(this,
                    { _, selectedHour, selectedMinute ->
                        val selectedDateTime = Calendar.getInstance()
                        selectedDateTime.set(
                            selectedYear,
                            selectedMonth,
                            selectedDay,
                            selectedHour,
                            selectedMinute
                        )

                        val dateFormat =
                            SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                        editText.setText(dateFormat.format(selectedDateTime.time))
                    }, hour, minute, false
                )
                timePickerDialog.show()
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun showColorWheel()
    {
        colorEditText = if (!::colorEditText.isInitialized) {
            findViewById(R.id.nameOfCourseText)
        } else {
            findViewById(R.id.nameOfCourseText)
        }

        colorPickerView = if (!::colorPickerView.isInitialized) {
            findViewById(R.id.colorPickerView)
        } else {
            findViewById(R.id.colorPickerView)
        }

        colorPickerView.visibility = View.VISIBLE

        ColorPickerDialog.Builder(this).setTitle("Pick a color for your course!").setPreferenceName("MyColorPickerDialog")
            .setPositiveButton("Save", ColorEnvelopeListener
            { envelope, _ -> colorEditText.setTextColor(envelope.color)
                val courseName = findViewById<EditText>(R.id.nameOfCourseText).text.toString()
                val courseColor = envelope.color
                val course = Course(courseName, courseColor)
                courseList.add(course)

                updateCourseRecyclerView()

                colorPickerView.visibility = View.GONE
            })
            .setNegativeButton("Cancel")
            { _, _ ->
                colorPickerView.visibility = View.GONE
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .show()
    }

    private fun saveCourse(courseName: String, courseColor: Int)
    {
        val course = Course(courseName, courseColor)
        courseList.add(course)
    }

    private fun updateCourseRecyclerView()
    {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        if (recyclerView != null)
        {
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    class CourseAdapter(private val courseList: List<Course>, private val mainActivity: MainActivity, private val editClassDescText: EditText)
        : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.courseitems, parent, false)
            return CourseViewHolder(itemView)
        }
        override fun onBindViewHolder(holder: CourseViewHolder, position: Int)
        {
            val currentCourse = courseList[position]
            holder.bind(currentCourse)

            holder.itemView.setOnClickListener {
                editClassDescText.setText(currentCourse.courseName)
                mainActivity.inflateLayout(R.layout.addhomeworkscreen_layout)
            }
        }
        override fun getItemCount(): Int
        {
            return courseList.size
        }
        inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val courseNameButtonView: Button = itemView.findViewById(R.id.courseNameButtonView)
            init {
                courseNameButtonView.setOnClickListener {
                    val course = courseList[adapterPosition]
                    mainActivity.inflateLayout(R.layout.addhomeworkscreen_layout)
                    {
                        val editClassDescText = mainActivity.findViewById<EditText>(R.id.editCourseDescText)
                        editClassDescText?.apply {
                            setText(course.courseName)
                            setTextColor(course.courseColor)
                        }
                    }
                }
            }
            fun bind(course: Course)
            {
                courseNameButtonView.text = course.courseName
                courseNameButtonView.setTextColor(course.courseColor)
            }
        }
    }

    private fun updateHomeworkRecyclerViews()
    {
        val homeRecyclerView = findViewById<RecyclerView>(R.id.homeScreenRecycler)
        if (homeRecyclerView != null) {
            val homeAdapter = HomeworkAdapter(homeworkList)
            homeRecyclerView.adapter = homeAdapter
            homeRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val currentRecyclerView = findViewById<RecyclerView>(R.id.curHWRecycler)
        if (currentRecyclerView != null) {
            val currentAdapter = HomeworkAdapter(homeworkList)
            currentRecyclerView.adapter = currentAdapter
            currentRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun switchToCurrentUpcomingLayout() {
        inflateLayout(R.layout.currentupcominghw_layout)
        updateHomeworkRecyclerViews()
        Log.d("SwitchLayout", "Switched to currentupcominghw_layout")
    }

    class HomeworkAdapter(private val homeworkList: List<Homework>) : RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeworkViewHolder
        {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.homeworkitems, parent, false)
            return HomeworkViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: HomeworkViewHolder, position: Int)
        {
            val currentHomework = homeworkList[position]
            holder.bind(currentHomework)
        }

        override fun getItemCount(): Int
        {
            return homeworkList.size
        }

        inner class HomeworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val homeworkTextView: Button = itemView.findViewById(R.id.homeworkButtonView)
            fun bind(homework: Homework)
            {
                homeworkTextView.text = "${homework.courseName}: ${homework.assignmentDesc} due ${homework.dueDate}"
                homeworkTextView.setTextColor(homework.color)
            }
        }
    }

}

