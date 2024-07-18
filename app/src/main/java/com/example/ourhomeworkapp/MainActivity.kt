package com.example.ourhomeworkapp

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ourhomeworkapp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val TAG = "FIRESTORE"

class MainActivity : ComponentActivity() {

    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorEditText: EditText
    data class Course(val courseName: String, val courseColor: Int)
    private lateinit var courseList: MutableList<Course>
    data class Homework(
        var documentId: String? = null,
        var courseName: String = "",
        var assignmentDesc: String = "",
        var dueDate: String = "",
        var reminderDate: String = "",
        var color: Int = 0,
        var isCompleted: Boolean = false)
    private lateinit var homeworkList: MutableList<Homework>

    private lateinit var completedHomeworkList: MutableList<Homework>

    private lateinit var homeAdapter: HomeworkAdapter
    private lateinit var currentUpcomingAdapter: HomeworkAdapter
    private lateinit var completedAdapter: HomeworkAdapter

    private lateinit var editClassDescText: EditText
    private lateinit var addHomeworkLayout: View

    private lateinit var updateEditClassDescText: EditText
    private lateinit var editHomeworkLayout: View

    private var editingHomeworkIndex: Int = -1

    private var currentLayout: Int = R.layout.homescreen_layout

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var emailInput : EditText
    private lateinit var passwordInput : EditText
    private lateinit var regButton : Button

    private var binding : ActivityMainBinding? = null

    private lateinit var firestore: FirebaseFirestore
    private val SMS_PERMISSION_CODE = 101

    private lateinit var reminderSwitch: Switch
    private lateinit var messageSwitch: Switch
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var changeNumberEditText: EditText
    private lateinit var confirmNumberEditText: EditText
    private lateinit var saveButton: AppCompatButton
    private lateinit var cancelButton: AppCompatButton

    private lateinit var changeNameEditText: EditText
    private lateinit var confirmNameEditText: EditText
    private lateinit var savesButton: AppCompatButton
    private lateinit var cancelsButton: AppCompatButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        courseList = mutableListOf()
        homeworkList = mutableListOf()
        completedHomeworkList = mutableListOf()

        addHomeworkLayout = layoutInflater.inflate(R.layout.addhomeworkscreen_layout, null)
        editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)
        editHomeworkLayout = layoutInflater.inflate(R.layout.edithwscreen_layout, null)
        updateEditClassDescText = editHomeworkLayout.findViewById(R.id.edit_editClassDescText)

        homeAdapter = HomeworkAdapter(homeworkList, this, "home")
        currentUpcomingAdapter = HomeworkAdapter(homeworkList, this, "currentUpcoming")
        completedAdapter = HomeworkAdapter(completedHomeworkList, this, "completed")

        inflateLayout(R.layout.revamped_settings_page)

        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.firebase.ui.auth.R.string.default_web_client_id))
            .requestEmail()
            .build()
        findViewById<ImageButton>(R.id.gSignInBtn)?.setOnClickListener {
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            signInGoogle()
        }

        launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == RESULT_OK)
                {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleResults(task)
                }
                else
                {
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            }

        FirebaseFirestore.setLoggingEnabled(true)
        firestore = Firebase.firestore

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                retrieveHomeworkData(user.uid)
            } else {
                Log.w(TAG, "User not authenticated, cannot retrieve homework data.")
            }
        }

        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val introCompleted = sharedPreferences.getBoolean("introCompleted", false)

//        if (introCompleted) {
//            inflateLayout(R.layout.homescreen_layout)
//        } else {
//            inflateLayout(R.layout.introscreen_welcome_layout)
//        }
    }

    //Code that handles anything and everything to do with navigating the app starts here, including what happens when a button is pressed,
    //what layout to open when a button is pressed, what actions to preform when a button is pressed, updating recycler views, and more.
    private fun inflateLayout(layoutResID: Int, afterInflate: (() -> Unit)? = null)
    {
        if (currentLayout == R.layout.addhomeworkscreen_layout)
        {
            saveHomeworkInput()
        }
        currentLayout = layoutResID

        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(layoutResID, null)

        setContentView(layout)

        afterInflate?.invoke()


        when (layoutResID)
        {
            R.layout.loginscreen_layout ->{
                emailInput = findViewById(R.id.email_input)
                passwordInput = findViewById(R.id.password_input)

                findViewById<Button>(R.id.register_btn).setOnClickListener{
                    inflateLayout(R.layout.registerscreen_layout)
                }

                findViewById<Button>(R.id.login_btn).setOnClickListener{
                    val email = emailInput.text.toString()
                    val password = passwordInput.text.toString()

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Login successful
                                Log.d("FirebaseAuth", "User signed in successfully")
                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                                // Redirect to a different activity or update the UI
                                inflateLayout(R.layout.homescreen_layout)


                            } else {
                                // Handle login errors
                                handleFirebaseLoginError(task.exception)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                    }


                }
                findViewById<ImageButton>(R.id.gSignInBtn).setOnClickListener{
                     fun updateUI(account: GoogleSignInAccount) {
                         val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                         auth.signInWithCredential(credential).addOnCompleteListener {
                             if (it.isSuccessful)
                             {

                                 inflateLayout(R.layout.homescreen_layout)

                             }
                             else
                             {
                                 Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT)
                                     .show()
                             }
                         }
                     }
                }
            }

            R.layout.registerscreen_layout->{
                emailInput = findViewById(R.id.email_input)
                passwordInput = findViewById(R.id.password_input)
                regButton = findViewById(R.id.createAccount_btn)


                regButton.setOnClickListener {
                    val email = emailInput.text.toString()
                    val password = passwordInput.text.toString()

                    if(email.isNotEmpty() && password.isNotEmpty()){
                        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{task ->
                            if (task.isSuccessful){
                                //reg was successful
                                Log.d("FirebaseAuth", "User registered successfully")
                                Toast.makeText(this,"Account created successfully!", Toast.LENGTH_SHORT).show()

                                inflateLayout(R.layout.homescreen_layout)
                            } else {
                                handleFirebaseError(task.exception)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                findViewById<Button>(R.id.goToLogin).setOnClickListener {
                    inflateLayout(R.layout.loginscreen_layout)
                }
            }
            R.layout.introscreen_welcome_layout -> {

                findViewById<Button>(R.id.welcomeNextButton).setOnClickListener {
                    inflateLayout(R.layout.introscreen_name_layout)
                }
            }

            R.layout.introscreen_name_layout -> {

                findViewById<Button>(R.id.nameNextButton).setOnClickListener {
                    val name = findViewById<EditText>(R.id.nameInput).text.toString()
                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userName", name)
                    editor.apply()

                    inflateLayout(R.layout.introscreen_phonenum_layout)
                }

                findViewById<EditText>(R.id.nameInput).setOnClickListener {

                }
            }

            R.layout.introscreen_phonenum_layout -> {

                findViewById<Button>(R.id.phoneNumNextButton).setOnClickListener {
                    val phoneNumber = findViewById<EditText>(R.id.phoneNumInput).text.toString()
                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userPhoneNumber", phoneNumber)
                    editor.putBoolean("introCompleted", true)
                    editor.apply()

                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<EditText>(R.id.phoneNumInput).setOnClickListener {

                }
            }

            R.layout.homescreen_layout -> {
                requestSmsPermission()
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
                setupRecyclerViews()
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
                    uploadProfileData()
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
                findViewById<Button>(R.id.signOutButton).setOnClickListener {
                    googleSignInClient.signOut().addOnCompleteListener{task ->
                        if (task.isSuccessful){
                            inflateLayout(R.layout.loginscreen_layout)

                        }
                        else{
                            Log.e("GoogleSignOut", "Sign-out failed", task.exception) // Log for debugging
                            Toast.makeText(this, "Failed to sign out. Please try again.", Toast.LENGTH_SHORT).show() // Inform the user
                        }

                    }
                }
            }

            R.layout.addhomeworkscreen_layout -> {
                loadHomeworkInput()
                findViewById<Button>(R.id.addHWcancelButton).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }

                findViewById<Button>(R.id.addHWsaveButton).setOnClickListener {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener // Ensure user is authenticated
                    val courseDesc = this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
                    val assignmentDesc = findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
                    val dueDate = this.findViewById<EditText>(R.id.editDueDateText).text.toString()
                    val color = findViewById<EditText>(R.id.editCourseDescText).currentTextColor
                    val reminderDate = this.findViewById<EditText>(R.id.editReminderText).text.toString()

                    val homework = Homework(userId, courseDesc, assignmentDesc, dueDate, reminderDate, color)

                    homeworkList.add(homework)

                    uploadHomeworkData(homework)

                    updateHomeworkRecyclerViews()
                    clearHomeworkInput()

                    inflateLayout(R.layout.currentupcominghw_layout)

                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val userName = sharedPreferences.getString("userName", "User")
                    val userPhoneNumber = sharedPreferences.getString("userPhoneNumber", "5551234567")

                    val message = "Hey $userName added an $courseDesc assignment titled $assignmentDesc and it is due on $dueDate."
                    if (userPhoneNumber != null) {
                        sendSMS(userPhoneNumber, message)
                    }
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

                val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)
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
                    val id = FirebaseAuth.getInstance().currentUser?.uid.toString()
                    val editCourseDesc = this.findViewById<EditText>(R.id.edit_editClassDescText).text.toString()
                    val editAssignmentDesc = findViewById<EditText>(R.id.edit_editAssignmentDescText).text.toString()
                    val editDueDate = this.findViewById<EditText>(R.id.edit_editDueDateText).text.toString()
                    val editReminderDate = this.findViewById<EditText>(R.id.editReminderText).text.toString()
                    val editColor = findViewById<EditText>(R.id.edit_editClassDescText).currentTextColor

                    if (editingHomeworkIndex != -1)
                    {
                        val homework = homeworkList[editingHomeworkIndex]
                        homework.documentId = id
                        homework.courseName = editCourseDesc
                        homework.assignmentDesc = editAssignmentDesc
                        homework.dueDate = editDueDate
                        homework.reminderDate = editReminderDate
                        homework.color = editColor

                    }
                    else
                    {
                        val homework = Homework(id, editCourseDesc, editAssignmentDesc, editDueDate, editReminderDate, editColor)
                        homeworkList.add(homework)
                    }

                    updateHomeworkRecyclerViews()

                    inflateLayout(R.layout.currentupcominghw_layout)
                }

                findViewById<EditText>(R.id.edit_editClassDescText).setOnClickListener {
                    inflateLayout(R.layout.edithwcoursescreen_layout)
                }

                findViewById<EditText>(R.id.edit_editAssignmentDescText).setOnClickListener {

                }

                findViewById<Button>(R.id.completeHWButton).setOnClickListener {
                    val homework = homeworkList[editingHomeworkIndex]
                    homework.isCompleted = true
                    completedHomeworkList.add(homework)
                    moveHomeworkToCompleted(homework)//moves homework to completed in firestore
                    homeworkList.remove(homework)
                    updateHomeworkRecyclerViews()
                    inflateLayout(R.layout.completedhwscreen_layout)

                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val userName = sharedPreferences.getString("userName", "User")
                    val userPhoneNumber = sharedPreferences.getString("userPhoneNumber", "5551234567")

                    val message = "Hey $userName just completed the ${homework.assignmentDesc} for his ${homework.courseName} Class."
                    if (userPhoneNumber != null) {
                        sendSMS(userPhoneNumber, message)
                    }
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

                val editRecyclerView = findViewById<RecyclerView>(R.id.editCourseRecyclerView)
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

            //code for handling the revamped settings page
            R.layout.revamped_settings_page -> {
                findViewById<ImageView>(R.id.settings_back_button).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }
                val usernameDisplay: TextView = findViewById(R.id.name_display)
                displaySavedName(usernameDisplay)


                findViewById<ImageView>(R.id.change_name_button).setOnClickListener {
                    inflateLayout(R.layout.change_name)
                }

                findViewById<ImageView>(R.id.change_number_button).setOnClickListener {
                    inflateLayout(R.layout.change_number)
                }


                findViewById<ImageView>(R.id.notification_menu_button).setOnClickListener {
                    inflateLayout(R.layout.notifications_page)
                }


                findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.sign_out_button).setOnClickListener {
                    inflateLayout(R.layout.homescreen_layout)
                }
            }

            //code for handling notification page
            R.layout.notifications_page -> {

                reminderSwitch = findViewById(R.id.reminder_switch)
                messageSwitch = findViewById(R.id.message_switch)

                findViewById<ImageView>(R.id.notification_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }

                sharedPreferences = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)

                // Load the saved states
                reminderSwitch.isChecked = sharedPreferences.getBoolean("reminderSwitchState", false)
                messageSwitch.isChecked = sharedPreferences.getBoolean("messageSwitchState", false)

                // Set listeners for the switches
                reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
                    saveSwitchState("reminderSwitchState", isChecked)

                }

                messageSwitch.setOnCheckedChangeListener { _, isChecked ->
                    saveSwitchState("messageSwitchState", isChecked)

                }

                findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.save_notification_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }

            }

            //code for change name
            R.layout.change_name -> {

                findViewById<ImageView>(R.id.change_name_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }

                changeNameEditText = findViewById(R.id.change_name_Text)
                confirmNameEditText = findViewById(R.id.confirm_name_text)
                saveButton = findViewById(R.id.change_name_save_button)
                cancelButton = findViewById(R.id.change_name_cancel_button)

                // Set click listener for Save button
                saveButton.setOnClickListener {
                    saveName()
                    inflateLayout(R.layout.revamped_settings_page)
                }

                // Set click listener for Cancel button
                cancelButton.setOnClickListener {
                    // Handle cancel logic here
                    inflateLayout(R.layout.revamped_settings_page)
                }

            }
            //code for handling change number
            R.layout.change_number -> {

                findViewById<ImageView>(R.id.change_number_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }


                // Initialize views
                changeNumberEditText = findViewById(R.id.change_number_text)
                confirmNumberEditText = findViewById(R.id.confirm_number_text)
                saveButton = findViewById(R.id.change_number_save_button)
                cancelButton = findViewById(R.id.change_number_cancel_button)

                // Set click listener for Save button
                saveButton.setOnClickListener {
                    saveNumbers()
                    inflateLayout(R.layout.revamped_settings_page)
                }

                // Set click listener for Cancel button
                cancelButton.setOnClickListener {
                    // Handle cancel logic here
                    inflateLayout(R.layout.revamped_settings_page)
                }
            }
        }
    }

    //displays saved name to settings page
    private fun displaySavedName(textView: TextView) {
        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedName = sharedPref.getString("user_name", "Default Name") // "Default Name" is a fallback if no name is saved
        textView.text = savedName
        Log.d("SettingsActivity", "Retrieved name: $savedName")

    }

    private fun saveName() {
        val newName = changeNameEditText.text.toString()
        val confirmName = confirmNameEditText.text.toString()

        // Perform validation
        if (newName.isEmpty()) {
            changeNameEditText.error = "Please enter a new name"
            return
        }

        if (confirmName.isEmpty()) {
            confirmNameEditText.error = "Please confirm the name"
            return
        }

        if (newName != confirmName) {
            confirmNameEditText.error = "Names do not match"
            return
        }

        // Save the name
        saveNameToSharedPreferences(newName)
        sendConfirmationSMS()

        // show a success message or navigate away
        Toast.makeText(this, "Name saved!", Toast.LENGTH_SHORT).show()
    }

    private fun saveNameToSharedPreferences(newName: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_name", newName)
        editor.apply()
        Log.d("ChangeNameActivity", "Name saved: $newName")
    }


    private fun saveNumbers() {
        val newNumber = changeNumberEditText.text.toString().trim()
        val confirmNumber = confirmNumberEditText.text.toString().trim()

        // Perform validation if necessary
        if (newNumber.isEmpty()) {
            changeNumberEditText.error = "Please enter a new number"
            return
        }

        if (confirmNumber.isEmpty()) {
            confirmNumberEditText.error = "Please confirm the number"
            return
        }

        if (newNumber != confirmNumber) {
            confirmNumberEditText.error = "Numbers do not match"
            return
        }

        // Save the numbers
        saveNumberToSharedPreferences(newNumber)

        // show a success message or navigate away
        Toast.makeText(this, "Numbers saved!", Toast.LENGTH_SHORT).show()
    }

    private fun saveNumberToSharedPreferences(newNumber: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("savedNumber", newNumber)
        editor.apply()
    }

    //handles getting the user name and the saved phone number in order to send notifications to it
    private fun getUserDetails(): Pair<String?, String?> {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedNumber = sharedPreferences.getString("savedNumber", null)
        val savedName = sharedPreferences.getString("savedName", null)
        return Pair(savedNumber, savedName)
    }


    private fun saveSwitchState(key: String, state: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, state)
        editor.apply()
    }

    //Code involved with settings page ends here

    //Code that handles everything and anything to do with firebase starts here
    private fun handleFirebaseError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email or password format", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e("FirebaseAuth", "Error: $exception")
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun signInGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                Log.d("SIGN_IN", "Account retrieved: ${account.email}")
                updateUI(account)
            }

        }else{
            Log.e("SIGN_IN", "Error: ${task.exception}")
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }

    }

    private fun updateUI(account: GoogleSignInAccount)
    {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        auth.signInWithCredential(credential).addOnCompleteListener{
            if (it.isSuccessful){
                Log.d("SIGN_IN", "Firebase authentication successful")
                inflateLayout(R.layout.homescreen_layout)

            }else{
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun handleFirebaseLoginError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email or password format", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e("FirebaseAuth", "Error: $exception")
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

    }
    //Code dealing with firebase ends here

    //Code that has to do with the add homework screen starts here
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
    //Code that deals with the add homework screen ends here!

    //Code that deals with color coding courses starts here
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
    //Code that deals with color coding courses ends here!

    //Code that handles SMS messaging starts here!
    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    //Function to send SMS using the dynamically saved phone number
    private fun sendSMSWithSavedNumber(message: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedNumber = sharedPreferences.getString("savedNumber", null)

        if (savedNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show()
            return
        }

        sendSMS(savedNumber, message)
    }

    private fun sendConfirmationSMS() {
        val message = "Your phone number has been updated successfully."
        sendSMSWithSavedNumber(message)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager: SmsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show()
                Log.d("SMS", "SMS sent to $phoneNumber: $message")
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
                Log.e("SMS", "Failed to send SMS", e)
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
            requestSmsPermission()
        }
    }

    //Code that handles the course creation, storage and management starts here
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

    private fun updateCourseRecyclerView()
    {
        val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)
        if (recyclerView != null)
        {
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        val editRecyclerView = findViewById<RecyclerView>(R.id.editCourseRecyclerView)
        if (editRecyclerView != null)
        {
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            editRecyclerView.adapter = adapter
            editRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun saveCourse(courseName: String, courseColor: Int)
    {
        val course = Course(courseName, courseColor)
        courseList.add(course)
    }
    //Code that handles courses ends here!

    //Code that handles homework creation, storage and management begins here
    class HomeworkAdapter(private val homeworkList: MutableList<Homework>, private val mainActivity: MainActivity, private val origin: String ) : RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>()
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

            holder.itemView.findViewById<Button>(R.id.homeworkButtonView).setOnClickListener{
                when(origin){
                    "home", "currentUpcoming" -> mainActivity.editHomework(currentHomework, position)
                    "completed" -> mainActivity.viewCompletedHomework(currentHomework)
                }
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
                    (itemView.context as MainActivity).editHomework(homework, position) //,color)
                }
            }
            fun bind(homework: Homework)
            {
                homeworkRecyclerView.text = "${homework.courseName}: ${homework.assignmentDesc} due ${homework.dueDate}"
                homeworkRecyclerView.setTextColor(homework.color)
            }
        }
    }

    private fun saveHomeworkInput()
    {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("assignmentDesc", findViewById<EditText>(R.id.editAssignmentDescText).text.toString())
        editor.putString("dueDate", findViewById<EditText>(R.id.editDueDateText).text.toString())
        editor.putString("reminder", findViewById<EditText>(R.id.editReminderText).text.toString())
        editor.apply()
    }

    private fun loadHomeworkInput()
    {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        findViewById<EditText>(R.id.editAssignmentDescText).setText(sharedPreferences.getString("assignmentDesc", ""))
        findViewById<EditText>(R.id.editDueDateText).setText(sharedPreferences.getString("dueDate", ""))
        findViewById<EditText>(R.id.editReminderText).setText(sharedPreferences.getString("reminder", ""))
    }

    private fun clearHomeworkInput()
    {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        editClassDescText.setText("")
        findViewById<EditText>(R.id.editAssignmentDescText).setText("")
        findViewById<EditText>(R.id.editDueDateText).setText("")
        findViewById<EditText>(R.id.editReminderText).setText("")
    }

    private fun setupRecyclerViews() {
        findViewById<RecyclerView>(R.id.homeScreenRecycler)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = HomeworkAdapter(homeworkList, this@MainActivity, "home")
        }
        findViewById<RecyclerView>(R.id.curHWRecycler)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = HomeworkAdapter(homeworkList, this@MainActivity, "currentUpcoming")
        }
        findViewById<RecyclerView>(R.id.compHWRecycler)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = HomeworkAdapter(completedHomeworkList, this@MainActivity, "completed")
        }
    }

    private fun updateHomeworkRecyclerViews()
    {
        val homeRecyclerView = findViewById<RecyclerView>(R.id.homeScreenRecycler)
        if (homeRecyclerView != null)
        {
            val homeAdapter = HomeworkAdapter(homeworkList, this, "home")
            homeRecyclerView.adapter = homeAdapter
            homeRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val currentRecyclerView = findViewById<RecyclerView>(R.id.curHWRecycler)
        if (currentRecyclerView != null)
        {
            val currentAdapter = HomeworkAdapter(homeworkList, this, "currentUpcoming")
            currentRecyclerView.adapter = currentAdapter
            currentRecyclerView.layoutManager = LinearLayoutManager(this)
        }

        val completedRecyclerView = findViewById<RecyclerView>(R.id.compHWRecycler)
        if (completedRecyclerView != null)
        {
            val completedAdapter = HomeworkAdapter(completedHomeworkList, this, "completed")
            completedRecyclerView.adapter = completedAdapter
            completedRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    fun editHomework(homework: Homework, index: Int)
    {
        editingHomeworkIndex = index
        inflateLayout(R.layout.edithwscreen_layout)
        {
            findViewById<EditText>(R.id.edit_editClassDescText).apply {
                setText(homework.courseName)
                setTextColor(homework.color)
            }
            findViewById<EditText>(R.id.edit_editAssignmentDescText).setText(homework.assignmentDesc)
            findViewById<EditText>(R.id.edit_editDueDateText).setText(homework.dueDate)
        }
        findViewById<Button>(R.id.completeHWButton).setOnClickListener {
            homework.isCompleted = true
            completedHomeworkList.add(homework)
            moveHomeworkToCompleted(homework)
            homeworkList.remove(homework)
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.completedhwscreen_layout)
        }
    }

    fun viewCompletedHomework(homework: Homework)
    {
        inflateLayout(R.layout.homeworkcompscreen_layout)
        {
            findViewById<EditText>(R.id.edit_completeClassDescText).apply {
                setText(homework.courseName)
                setTextColor(homework.color)
            }
            findViewById<EditText>(R.id.edit_completeAssignmentDescText).setText(homework.assignmentDesc)
            findViewById<EditText>(R.id.edit_completeDueDateText).setText(homework.dueDate)
        }
        findViewById<Button>(R.id.completeUndoButton).setOnClickListener {
            homework.isCompleted = false
            completedHomeworkList.remove(homework)
            homeworkList.add(homework)
            moveHomeworkToCurrent(homework) // Move homework back to current in Firestore
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.currentupcominghw_layout)
        }
        findViewById<Button>(R.id.completeDeleteButton).setOnClickListener {
            homework.isCompleted = true
            deleteHomeworkFromFireStore(homework)
            completedHomeworkList.remove(homework)
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.completedhwscreen_layout)
        }
    }
    //Code that handles homework ends here!

    //code for uploading and editing data to the database starts here

    private fun uploadProfileData(){

        val fireStoreDatabase = FirebaseFirestore.getInstance()

        val firstName = findViewById<EditText>(R.id.editFirstNameText).text.toString()
        val lastName = findViewById<EditText>(R.id.editLastNameText).text.toString()
        val email = findViewById<EditText>(R.id.editEmailText).text.toString()
        val parentPhoneNum = findViewById<EditText>(R.id.editParentPhoneNumText).text.toString()

        //val userId = FirebaseAuth.getInstance().currentUser.set(user)

        val user: MutableMap<String, Any> = HashMap()
        user["firstName"] = firstName
        user["lastName"] = lastName
        user["email"] = email
        user["parentPhoneNum"] = parentPhoneNum

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        fireStoreDatabase.collection("users").document(userId).set(user)

            .addOnSuccessListener {
                Log.d(TAG, "Added document with ID")
            }
            .addOnFailureListener {
                Log.w(TAG, "Error adding document")
            }
    }

    private fun uploadHomeworkData(homework: Homework){
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        //
        val courseDesc = this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
        val assignmentDesc = findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
        val dueDate = this.findViewById<EditText>(R.id.editDueDateText).text.toString()
        val color = findViewById<EditText>(R.id.editCourseDescText).currentTextColor
        val reminderDate = this.findViewById<EditText>(R.id.editReminderText).toString()

        //val homework = MainActivity.Homework(id, courseDesc, assignmentDesc, dueDate, reminderDate,  color)

        val userHomeworkMap : MutableMap<String, Any> = HashMap()
        //userHomeworkMap["id"] = id
        userHomeworkMap["courseDesc"] = courseDesc
        userHomeworkMap["assignmentDesc"] = assignmentDesc
        userHomeworkMap["dueDate"] = dueDate
        userHomeworkMap["reminderDate"] = reminderDate
        userHomeworkMap["color"] = color

        val documentId = fireStoreDatabase.collection("users")
            .document(userId)
            .collection("userHomework")
            .document("currentHomework")
            .collection("homeworks")
            .document().id

        fireStoreDatabase.collection("users")
            .document(userId)
            .collection("userHomework")
            .document("currentHomework")
            .collection("homeworks")
            .document(documentId)
            .set(userHomeworkMap)
            .addOnSuccessListener {
                Log.d(TAG, "Homework successfully uploaded with ID: $documentId")
                homework.documentId = documentId
            }
            .addOnFailureListener {
                Log.w(TAG, "Error adding document $it")
            }

            // this section was supposed to upload the course name and color but is currently causing the app to crash
            //whenever the homework gets saved. i'll try to make it its own method and it probably has something to do with
            // that its not an edit text but a text view.

        //val courseName = this.findViewById<EditText>(R.id.courseNameText).text.toString()
        //val courseColor = findViewById<EditText>(R.id.courseColorView).currentTextColor

       // val userCourseMap : MutableMap<String, Any> = HashMap()
        //userCourseMap["courseName"] = courseName
        //userCourseMap["courseColor"] = courseColor

        //fireStoreDatabase.collection("users").document(userId).collection("userCourses").add(userCourseMap)
            //.addOnSuccessListener {
                //Log.d(TAG, "Added document with ID $it")
            //}
           // .addOnFailureListener {
            //    Log.w(TAG, "Error adding document $it")
           // }

    }


    private fun retrieveHomeworkData(userId: String){
        homeworkList.clear() // Clear the existing list
        completedHomeworkList.clear() // Clear the completed list

        // Fetch data of both homeworks from Firestore
        val firestore = Firebase.firestore
        val currentHomeworkRef = firestore.collection("users")
            .document(userId)
            .collection("userHomework")
            .document("currentHomework")
            .collection("homeworks")

        val completedHomeworkRef = firestore.collection("users")
            .document(userId)
            .collection("userHomework")
            .document("completedHomework")
            .collection("homeworks")

        // Fetch current homework
        currentHomeworkRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    try{
                        val homework = document.toObject<Homework>()
                        if (homework != null) {

                            homeworkList.add(homework)
                        }
                    } catch (e: Exception){
                        Log.e(TAG, "Error converting document", e)
                    }
                }
                // Fetch completed homework after current homework is fetched
                retrieveCompletedHW(userId)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents: ", exception)
            }
    }

    // Fetch Completed Homework
    private fun retrieveCompletedHW(userId: String){
        val firestore = Firebase.firestore
        val completedHomeworkRef = firestore.collection("users")
            .document(userId)
            .collection("userHomework")
            .document("completedHomework")
            .collection("homeworks")

        completedHomeworkRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    try {
                        val homework = document.toObject<Homework>()
                        if (homework != null) {
                            completedHomeworkList.add(homework)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document", e)
                    }
                }
                updateHomeworkRecyclerViews() // Update RecyclerView after fetching all data
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents: ", exception)
            }
    }

    private fun deleteHomeworkFromFireStore(homework:Homework){
        val firestore = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Ensure user is authenticated


        homework.documentId?.let {documentId ->
            val completedHomeworkRef =firestore.collection("users")
                .document(userId)
                .collection("userHomework")
                .document("completedHomework")
                .collection("homeworks")
                .document(documentId) // Use homework ID or unique identifier
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Homework successfully deleted with ID: $documentId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error deleting document", e)
                }
        }

    }
    private fun moveHomeworkToCompleted(homework: Homework) {
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        homework.documentId?.let { documentId ->
            val currentHomeworkRef = fireStoreDatabase.collection("users")
                .document(userId)
                .collection("userHomework")
                .document("currentHomework")
                .collection("homeworks")
                .document(documentId)

            val completedHomeworkRef = fireStoreDatabase.collection("users")
                .document(userId)
                .collection("userHomework")
                .document("completedHomework")
                .collection("homeworks")
                .document(documentId)

            fireStoreDatabase.runTransaction { transaction ->
                val snapshot = transaction.get(currentHomeworkRef)
                transaction.set(completedHomeworkRef, snapshot.data!!)
                transaction.delete(currentHomeworkRef)
            }.addOnSuccessListener {
                Log.d(TAG, "Homework moved to completed with ID: $documentId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error moving document", e)
            }
        }
    }
    private fun moveHomeworkToCurrent(homework: Homework) {
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        homework.documentId?.let { documentId ->
            val completedHomeworkRef = fireStoreDatabase.collection("users")
                .document(userId)
                .collection("userHomework")
                .document("completedHomework")
                .collection("homeworks")
                .document(documentId)

            val currentHomeworkRef = fireStoreDatabase.collection("users")
                .document(userId)
                .collection("userHomework")
                .document("currentHomework")
                .collection("homeworks")
                .document(documentId)

            fireStoreDatabase.runTransaction { transaction ->
                val snapshot = transaction.get(completedHomeworkRef)
                transaction.set(currentHomeworkRef, snapshot.data!!)
                transaction.delete(completedHomeworkRef)
            }.addOnSuccessListener {
                Log.d(TAG, "Homework moved back to current with ID: $documentId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error moving document", e)
            }
        }
    }

    //code for uploading, retrieving, and editing data to the database ends here

    //Code that handles profile info begins here, BEWARE: will probably be deleted!
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
//Code that handles profile info ends here!












