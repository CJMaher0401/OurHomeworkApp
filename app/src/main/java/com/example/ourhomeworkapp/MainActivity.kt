package com.example.ourhomeworkapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    data class Homework(val courseName: String, val assignmentDesc: String, val dueDate: String, val color: Int, var isCompleted: Boolean = false)
    private lateinit var homeworkList: MutableList<Homework>

    private lateinit var completedHomeworkList: MutableList<Homework>

    private lateinit var editClassDescText: EditText
    private lateinit var addHomeworkLayout: View

    private lateinit var updateEditClassDescText: EditText
    private lateinit var editHomeworkLayout: View

    private var currentLayout: Int = R.layout.homescreen_layout

    private lateinit var auth: FirebaseAuth

    //hello

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        courseList = mutableListOf()

        addHomeworkLayout = layoutInflater.inflate(R.layout.addhomeworkscreen_layout, null)
        editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)

        editHomeworkLayout = layoutInflater.inflate(R.layout.edithwscreen_layout, null)
        updateEditClassDescText = editHomeworkLayout.findViewById(R.id.edit_editClassDescText)

        homeworkList = mutableListOf()

        completedHomeworkList = mutableListOf()

        inflateLayout(R.layout.homescreen_layout)
    }

    private fun inflateLayout(layoutResID: Int, afterInflate: (() -> Unit)? = null)
    {
        currentLayout = layoutResID

        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(layoutResID, null)

        setContentView(layout)

        afterInflate?.invoke()

        when (layoutResID)
        {
            R.layout.homescreen_layout -> {
                findViewById<Button>(R.id.profileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        loadUpdatedProfileInfo()
                    }
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.homeScreenButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.homeScreenMyHWButton).setOnClickListener {
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<Button>(R.id.homeScreenMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        loadUpdatedProfileInfo()
                    }
                }

                val recyclerViewHomeScreen: RecyclerView = findViewById(R.id.homeScreenRecycler)
                recyclerViewHomeScreen.layoutManager = LinearLayoutManager(this)
                updateHomeworkRecyclerViews()
            }

            R.layout.profilescreen_layout -> {
                findViewById<EditText>(R.id.editFirstNameText).setOnClickListener {

                }
                findViewById<EditText>(R.id.editLastNameText).setOnClickListener {

                }
                findViewById<EditText>(R.id.editEmailText).setOnClickListener {

                }
                findViewById<EditText>(R.id.editParentPhoneNumText).setOnClickListener {

                }
                findViewById<Button>(R.id.saveChangesButton).setOnClickListener {
                    saveProfileInfo()
                    inflateLayout(R.layout.homescreen_layout)

                }
                findViewById<Button>(R.id.profileHomeButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.profileMyHWButton).setOnClickListener {
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<Button>(R.id.profileMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        loadUpdatedProfileInfo()
                    }
                }
            }

            R.layout.addhomeworkscreen_layout -> {
                findViewById<Button>(R.id.addHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.addHWsaveButton).setOnClickListener {
                    val courseDesc = this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
                    val assignmentDesc = findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
                    val dueDate = this.findViewById<EditText>(R.id.editDueDateText).text.toString()
                    val color = findViewById<EditText>(R.id.editCourseDescText).currentTextColor

                    val homework = Homework(courseDesc, assignmentDesc, dueDate, color)

                    homeworkList.add(homework)

                    updateHomeworkRecyclerViews()

                    inflateLayout(R.layout.currentupcominghw_layout)
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
                findViewById<Button>(R.id.returnToAddHWButton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.addNewClassButton).setOnClickListener {
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
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<Button>(R.id.compHWButton).setOnClickListener {
                    inflateLayout(R.layout.completedhwscreen_layout)
                }

                val recyclerViewCurUpcomingHW: RecyclerView = findViewById(R.id.curHWRecycler)
                recyclerViewCurUpcomingHW.layoutManager = LinearLayoutManager(this)
                updateHomeworkRecyclerViews()
            }

            R.layout.completedhwscreen_layout -> {
                findViewById<Button>(R.id.compHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<ImageButton>(R.id.compHWaddHWbutton).setOnClickListener {
                    inflateLayout(R.layout.addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.compCurHWButton).setOnClickListener {
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<Button>(R.id.compCompHWButton).setOnClickListener {
                    inflateLayout(R.layout.completedhwscreen_layout)
                }

                findViewById<RecyclerView>(R.id.compHWRecycler).setOnClickListener{

                }
                val recyclerViewCompHW: RecyclerView = findViewById(R.id.compHWRecycler)
                recyclerViewCompHW.layoutManager = LinearLayoutManager(this)
                updateHomeworkRecyclerViews()
            }

            R.layout.edithwscreen_layout -> {
                findViewById<Button>(R.id.editcancelButton).setOnClickListener {
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<Button>(R.id.editsaveButton).setOnClickListener {
                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<EditText>(R.id.edit_editClassDescText).setOnClickListener {
                    inflateLayout(R.layout.edithwcoursescreen_layout)
                }

                findViewById<EditText>(R.id.edit_editAssignmentDescText).setOnClickListener {

                }

                findViewById<Button>(R.id.completeHWButton).setOnClickListener {
                    inflateLayout(R.layout.completedhwscreen_layout)
                }

                val editDueDateText = findViewById<EditText>(R.id.edit_editDueDateText)
                editDueDateText.setOnClickListener {
                    showDatePicker(editDueDateText)
                }

                val editReminderText = findViewById<EditText>(R.id.edit_editReminderText)
                editReminderText.setOnClickListener {
                    showDateAndTimePicker(editReminderText)
                }

            }
            R.layout.edithwcoursescreen_layout -> {
                findViewById<Button>(R.id.edit_returnToAddHWButton).setOnClickListener {
                    inflateLayout(R.layout.edithwscreen_layout)
                }

                val editRecyclerView = findViewById<RecyclerView>(R.id.editHWRecyclerView)
                if (editRecyclerView != null)
                {
                    updateCourseRecyclerView()
                }

                updateEditClassDescText = editHomeworkLayout.findViewById(R.id.edit_editClassDescText)
            }
            R.layout.homeworkcompscreen_layout ->
            {
                findViewById<Button>(R.id.completeCancelButton).setOnClickListener{
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
                findViewById<Button>(R.id.completeSaveButton).setOnClickListener{
                    inflateLayout(R.layout.completedhwscreen_layout)
                }
                findViewById<Button>(R.id.completeUndoButton).setOnClickListener{

                }
                findViewById<Button>(R.id.completeDeleteButton).setOnClickListener{

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
            }.attachAlphaSlideBar(true)
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

        val editRecyclerView = findViewById<RecyclerView>(R.id.editHWRecyclerView)
        if (editRecyclerView != null)
        {
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            editRecyclerView.adapter = adapter
            editRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    class CourseAdapter(private val courseList: List<Course>, private val mainActivity: MainActivity, private val editClassDescText: EditText) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder
        {
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

                    if(mainActivity.currentLayout == R.layout.yourcoursesscreen_layout)
                    {
                        mainActivity.inflateLayout(R.layout.addhomeworkscreen_layout)
                        {
                            val editClassDescText = mainActivity.findViewById<EditText>(R.id.editCourseDescText)
                            editClassDescText?.apply {
                                setText(course.courseName)
                                setTextColor(course.courseColor)
                            }
                        }
                    }
                    else
                    {
                        mainActivity.inflateLayout(R.layout.edithwscreen_layout)
                        {
                            val updateEditClassDescText = mainActivity.findViewById<EditText>(R.id.edit_editClassDescText)
                            updateEditClassDescText?.apply {
                                setText(course.courseName)
                                setTextColor(course.courseColor)
                            }
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
        if (homeRecyclerView != null)
        {
            val homeAdapter = HomeworkAdapter(homeworkList, this)
            homeRecyclerView.adapter = homeAdapter
            homeRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val currentRecyclerView = findViewById<RecyclerView>(R.id.curHWRecycler)
        if (currentRecyclerView != null)
        {
            val currentAdapter = HomeworkAdapter(homeworkList, this)
            currentRecyclerView.adapter = currentAdapter
            currentRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val completedRecyclerView = findViewById<RecyclerView>(R.id.compHWRecycler)
        if (completedRecyclerView != null)
        {
            val completedAdapter = HomeworkAdapter(completedHomeworkList, this)
            completedRecyclerView.adapter = completedAdapter
            completedRecyclerView.layoutManager = LinearLayoutManager(this)
        }

    }
    class HomeworkAdapter(private val homeworkList: List<Homework>, private val mainActivity: MainActivity) : RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeworkViewHolder
        {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.homeworkitems, parent, false)
            return HomeworkViewHolder(itemView)
        }
        override fun onBindViewHolder(holder: HomeworkViewHolder, position: Int)
        {
            val currentHomework = homeworkList[position]
            if(!currentHomework.isCompleted)
            {
                holder.bind(currentHomework)
            }
            else if(currentHomework.isCompleted)
            {
                holder.bind(currentHomework)
            }
        }
        override fun getItemCount(): Int
        {
            return homeworkList.size
        }
        inner class HomeworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val homeworkRecyclerView: Button = itemView.findViewById(R.id.homeworkButtonView)
            init {
                homeworkRecyclerView.setOnClickListener {
                    val homework = homeworkList[adapterPosition]
                    val color = homeworkRecyclerView.currentTextColor
                    (itemView.context as MainActivity).editHomework(homework, color)
                }
            }
            fun bind(homework: Homework)
            {
                homeworkRecyclerView.text = "${homework.courseName}: ${homework.assignmentDesc} due ${homework.dueDate}"
                homeworkRecyclerView.setTextColor(homework.color)
            }
        }
    }
    fun editHomework(homework: Homework, color: Int)
    {
        inflateLayout(R.layout.edithwscreen_layout) {
            findViewById<EditText>(R.id.edit_editClassDescText).apply {
                setText(homework.courseName)
                setTextColor(color)
            }
            findViewById<EditText>(R.id.edit_editAssignmentDescText).setText(homework.assignmentDesc)
            findViewById<EditText>(R.id.edit_editDueDateText).setText(homework.dueDate)
        }
        findViewById<Button>(R.id.completeHWButton).setOnClickListener {
            homework.isCompleted = true
            completedHomeworkList.add(homework)
            homeworkList.remove(homework)
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.completedhwscreen_layout)
        }
    }
    private fun saveProfileInfo()
    {
        val profileInfo = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = profileInfo.edit()

        val firstName = findViewById<EditText>(R.id.editFirstNameText).text.toString()
        val lastName = findViewById<EditText>(R.id.editLastNameText).text.toString()
        val email = findViewById<EditText>(R.id.editEmailText).text.toString()
        val parentPhoneNum = findViewById<EditText>(R.id.editParentPhoneNumText).text.toString()

        editor.putString("firstName", firstName)
        editor.putString("lastName", lastName)
        editor.putString("email", email)
        editor.putString("parentPhoneNum", parentPhoneNum)

        editor.apply()
    }
    private fun loadUpdatedProfileInfo()
    {
        val profileInfo = getSharedPreferences("UserData", Context.MODE_PRIVATE)

        val firstName = profileInfo.getString("firstName", "")
        val lastName = profileInfo.getString("lastName", "")
        val email = profileInfo.getString("email", "")
        val parentPhoneNum = profileInfo.getString("parentPhoneNum", "")

        findViewById<EditText>(R.id.editFirstNameText).setText(firstName)
        findViewById<EditText>(R.id.editLastNameText).setText(lastName)
        findViewById<EditText>(R.id.editEmailText).setText(email)
        findViewById<EditText>(R.id.editParentPhoneNumText).setText(parentPhoneNum)
    }
}

