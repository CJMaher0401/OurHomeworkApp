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
import com.google.firebase.firestore.SetOptions
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

    data class Course(
        val courseName: String = "",
        val courseColor: Int = 0,
        val courseDesc: String = " ",
        var currentHomeworks: List<Homework> = emptyList(),
        var completedHomeworks: List<Homework> = emptyList(),
        val courseId: String = " "
    )


    private lateinit var courseList: MutableList<Course>

    data class Homework(
        var documentId: String? = null,
        var courseName: String = "",
        var assignmentDesc: String = "",
        var dueDate: String = "",
        var reminderDate: String = "",
        var color: Int = 0,
        val courseDesc: String = "",
        var courseId: String = "",
        var isCompleted: Boolean = false
    )

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
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var regButton: Button

    private var binding: ActivityMainBinding? = null

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

        addHomeworkLayout = layoutInflater.inflate(R.layout.revamp_addhomeworkscreen_layout, null)
        editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)
        editHomeworkLayout = layoutInflater.inflate(R.layout.revamp_edithwscreen_layout, null)
        updateEditClassDescText = editHomeworkLayout.findViewById(R.id.editCourseDescText)

        homeAdapter = HomeworkAdapter(homeworkList, this, "home")
        currentUpcomingAdapter = HomeworkAdapter(homeworkList, this, "currentUpcoming")
        completedAdapter = HomeworkAdapter(completedHomeworkList, this, "completed")

        inflateLayout(R.layout.revamp_loginscreen_layout2)

        // Initialize Firebase Auth instance
        auth = FirebaseAuth.getInstance()
        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.firebase.ui.auth.R.string.default_web_client_id))
            .requestEmail()
            .build()
        // Set an OnClickListener for the Google Sign-In button
        findViewById<ImageButton>(R.id.gSignInBtn)?.setOnClickListener {
            // Initialize the GoogleSignInClient with the configured options
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            // Launch the Google Sign-In intent
            signInGoogle()
        }

        // Register a result handler for the Google Sign-In activity
        launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            // Check if the result was successful
            { result ->
                if (result.resultCode == RESULT_OK) {
                    // Get the GoogleSignInAccount from the result intent
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    // Handle the sign-in results
                    handleResults(task)
                } else {
                    // Show a toast message if Google Sign-In failed
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            }

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)
        // Initialize Firestore instance
        firestore = Firebase.firestore

        // Add an authentication state listener to FirebaseAuth
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            // Get the current authenticated user
            val user = auth.currentUser
            // If a user is authenticated, retrieve their courses and homework
            if (user != null) {
                retrieveCoursesAndHomework(user.uid)
            } else {
                // Log a warning if no user is authenticated
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
    private fun inflateLayout(layoutResID: Int, afterInflate: (() -> Unit)? = null) {
        if (currentLayout == R.layout.revamp_addhomeworkscreen_layout) {
            saveHomeworkInput()
        }
        currentLayout = layoutResID

        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(layoutResID, null)

        setContentView(layout)

        afterInflate?.invoke()


        when (layoutResID) {
            // Inflate the login screen layout
            R.layout.revamp_loginscreen_layout2 -> {
                // Find and initialize email and password input fields
                emailInput = findViewById(R.id.email_input)
                passwordInput = findViewById(R.id.password_input)

                // Set an OnClickListener for the register button
                findViewById<Button>(R.id.register_btn).setOnClickListener {
                    // Inflate the register screen layout
                    inflateLayout(R.layout.revamp_registerscreen_layout2)
                }

                // Set an OnClickListener for the login button
                findViewById<Button>(R.id.login_btn).setOnClickListener {
                    // Get the email and password from the input fields
                    val email = emailInput.text.toString()
                    val password = passwordInput.text.toString()

                    // Check if email and password are not empty
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        // Sign in with email and password
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                // If the login is successful
                                if (task.isSuccessful) {
                                    // Log the success and show a toast message
                                    Log.d("FirebaseAuth", "User signed in successfully")
                                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT)
                                        .show()

                                    // Inflate the home screen layout
                                    inflateLayout(R.layout.revamp_homescreen_layout)


                                } else {
                                    // Handle login errors
                                    handleFirebaseLoginError(task.exception)
                                }
                            }
                    } else {
                        // Show a toast message if email or password is empty
                        Toast.makeText(
                            this,
                            "Email and password cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                }
                // Set an OnClickListener for the Google sign-in button
                findViewById<ImageButton>(R.id.gSignInBtn).setOnClickListener {
                    // Function to update the UI after Google sign-in
                    fun updateUI(account: GoogleSignInAccount) {
                        // Get credentials using the Google sign-in account
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        // Sign in with the credentials
                        auth.signInWithCredential(credential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                // Inflate the home screen layout on successful sign-in
                                inflateLayout(R.layout.revamp_homescreen_layout)
                            } else {
                                // Show an error message if sign-in fails
                                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }

            // Inflate the register screen layout
            R.layout.revamp_registerscreen_layout2 -> {
                // Find and initialize email and password input fields and register button
                emailInput = findViewById(R.id.email_input)
                passwordInput = findViewById(R.id.password_input)
                regButton = findViewById(R.id.createAccount_btn)

                // Set an OnClickListener for the register button
                regButton.setOnClickListener {
                    // Get the email and password from the input fields
                    val email = emailInput.text.toString()
                    val password = passwordInput.text.toString()

                    // Check if email and password are not empty
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        // Create a new user with email and password
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                // If the registration is successful
                                if (task.isSuccessful) {
                                    // Log the success and show a toast message
                                    Log.d("FirebaseAuth", "User registered successfully")
                                    Toast.makeText(
                                        this,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Inflate the home screen layout
                                    inflateLayout(R.layout.revamp_homescreen_layout)
                                } else {
                                    // Handle any errors during registration
                                    handleFirebaseError(task.exception)
                                }
                            }
                    } else {
                        // Show a toast message if email or password is empty
                        Toast.makeText(
                            this,
                            "Email and password cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                // Set an OnClickListener for the go to login button
                findViewById<ImageButton>(R.id.cancelRegisterButton).setOnClickListener {
                    // Inflate the login screen layout
                    inflateLayout(R.layout.revamp_loginscreen_layout2)
                    // Reinitialize the Google sign-in button
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(com.firebase.ui.auth.R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    googleSignInClient = GoogleSignIn.getClient(this, gso)
                    findViewById<ImageButton>(R.id.gSignInBtn)?.setOnClickListener {
                        signInGoogle()
                    }
                }
            }

            R.layout.introscreen_welcome_layout -> {

                findViewById<Button>(R.id.welcomeNextButton).setOnClickListener {
                    inflateLayout(R.layout.introscreen_name_layout)
                }
            }

            R.layout.introscreen_name_layout -> {

                findViewById<Button>(R.id.welcomeNextButton).setOnClickListener {
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

                findViewById<Button>(R.id.welcomePhoneNum).setOnClickListener {
                    val phoneNumber = findViewById<EditText>(R.id.phoneNumInput).text.toString()
                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userPhoneNumber", phoneNumber)
                    editor.putBoolean("introCompleted", true)
                    editor.apply()

                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<EditText>(R.id.phoneNumInput).setOnClickListener {

                }
            }

            R.layout.revamp_homescreen_layout -> {
                requestSmsPermission()
                findViewById<Button>(R.id.profileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        downloadProfileInfo()
                    }
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_addhomeworkscreen_layout)
                }

                findViewById<ImageButton>(R.id.homeScreenButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<ImageButton>(R.id.homeScreenMyHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<ImageButton>(R.id.homeScreenMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        downloadProfileInfo()
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
                    uploadProfileData()
                    inflateLayout(R.layout.revamp_homescreen_layout)

                }
                findViewById<Button>(R.id.profileHomeButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<Button>(R.id.profileMyHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<Button>(R.id.profileMyProfileButton).setOnClickListener {
                    inflateLayout(R.layout.profilescreen_layout) {
                        downloadProfileInfo()
                    }
                }
                findViewById<Button>(R.id.signOutButton).setOnClickListener {
                    val googleUser = GoogleSignIn.getLastSignedInAccount(this)
                    if (googleUser != null) {
                        googleSignInClient.signOut().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Sign out of firebase
                                FirebaseAuth.getInstance().signOut()
                                // Update ui to show the login screen
                                inflateLayout(R.layout.revamp_loginscreen_layout2)
                                // Reinitialize the Google sign-in button
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(com.firebase.ui.auth.R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                                googleSignInClient = GoogleSignIn.getClient(this, gso)
                                findViewById<ImageButton>(R.id.gSignInBtn)?.setOnClickListener {
                                    signInGoogle()
                                }

                            } else {
                                Log.e("GoogleSignOut", "Sign-out failed", task.exception) // Log for debugging
                                Toast.makeText(this, "Failed to sign out. Please try again.", Toast.LENGTH_SHORT).show() // Inform the user
                            }
                        }
                    } else {
                        // If the user is not signed in via Google, just sign out from Firebase
                        FirebaseAuth.getInstance().signOut()
                        // Update ui to show the login screen
                        inflateLayout(R.layout.revamp_loginscreen_layout2)
                        // Reinitialize the Google sign-in button
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(com.firebase.ui.auth.R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        googleSignInClient = GoogleSignIn.getClient(this, gso)
                        findViewById<ImageButton>(R.id.gSignInBtn)?.setOnClickListener {
                            signInGoogle()
                        }
                    }
                }
            }

            R.layout.revamp_addhomeworkscreen_layout -> {
                loadHomeworkInput()
                findViewById<ImageButton>(R.id.cancelHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                        ?: return@setOnClickListener // Ensure user is authenticated
                    val courseDesc =
                        this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
                    val assignmentDesc =
                        findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
                    val dueDate = this.findViewById<EditText>(R.id.editDueDateText).text.toString()
                    val color = findViewById<EditText>(R.id.editCourseDescText).currentTextColor



                    //val homework = Homework(userId, courseDesc, assignmentDesc, dueDate, reminderDate, color)

                    //homework.courseName = courseId // Ensure course name is set
                    val homework = Homework(
                        courseName = courseDesc,
                        courseDesc = courseDesc,
                        assignmentDesc = assignmentDesc,
                        dueDate = dueDate,
                        color = color,
                        courseId = courseDesc
                    )

                    homeworkList.add(homework)
                    uploadHomeworkData(homework) // Upload homework to Firestore database
                    updateHomeworkRecyclerViews()
                    clearHomeworkInput()

                    inflateLayout(R.layout.revamped_currentupcominghw_layout)

                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val userName = sharedPreferences.getString("userName", "User")
                    val userPhoneNumber =
                        sharedPreferences.getString("userPhoneNumber", "5551234567")

                    val message =
                        "Hey $userName added an $courseDesc assignment titled $assignmentDesc and it is due on $dueDate."
                    if (userPhoneNumber != null) {
                        sendSMS(userPhoneNumber, message)
                    }
                }

                findViewById<EditText>(R.id.editCourseDescText).setOnClickListener {
                    inflateLayout(R.layout.revamp_yourcoursesscreen_layout)
                }

                val editDueDateText = findViewById<EditText>(R.id.editDueDateText)
                editDueDateText.setOnClickListener {
                    showDatePicker(editDueDateText)
                }


                editClassDescText = addHomeworkLayout.findViewById(R.id.editCourseDescText)

            }

            R.layout.revamp_yourcoursesscreen_layout -> {
                findViewById<ImageButton>(R.id.returnToAddHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_addhomeworkscreen_layout)
                }

                findViewById<ImageButton>(R.id.addNewClassButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_coursecreationscreen_layout)
                }

                val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)
                if (recyclerView != null) {
                    updateCourseRecyclerView()
                }
            }

            R.layout.revamp_coursecreationscreen_layout -> {
                findViewById<Button>(R.id.coursecancelButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_yourcoursesscreen_layout)
                }

                findViewById<Button>(R.id.coursesaveButton).setOnClickListener {
//                    val courseNameEditText = findViewById<EditText>(R.id.nameOfCourseText)
//                    val courseDescEditText = findViewById<EditText>(R.id.editCourseDescText)
//
//                    val courseName = courseNameEditText?.text?.toString().orEmpty()
//                    val courseColor = courseNameEditText?.currentTextColor ?: 0
//                    val courseDesc = courseDescEditText?.text?.toString().orEmpty()

                    val courseName = this.findViewById<EditText>(R.id.nameOfCourseText).text.toString()
                    val courseColor = findViewById<EditText>(R.id.nameOfCourseText).currentTextColor
                    val courseDesc = this.findViewById<EditText>(R.id.nameOfCourseText).text.toString()


                    if (!courseList.any { it.courseName == courseName }) {

                        saveCourse(courseName, courseColor, courseDesc)
                        updateCourseRecyclerView()
                        updateCourseRecyclerView()
                    }
                    inflateLayout(R.layout.revamp_yourcoursesscreen_layout)
                }

                findViewById<Button>(R.id.pickYourColorButton).setOnClickListener {
                    showColorWheel()
                }
            }

            R.layout.revamped_currentupcominghw_layout -> {
                findViewById<ImageButton>(R.id.cancelHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.curHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<Button>(R.id.compHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_completedhwscreen_layout)
                }

                val recyclerViewCurUpcomingHW: RecyclerView = findViewById(R.id.curHWRecycler)
                recyclerViewCurUpcomingHW.layoutManager = LinearLayoutManager(this)
                updateHomeworkRecyclerViews()
            }

            R.layout.revamped_completedhwscreen_layout -> {
                findViewById<ImageButton>(R.id.cancelHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamp_addhomeworkscreen_layout)
                }

                findViewById<Button>(R.id.curHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<Button>(R.id.compHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamped_completedhwscreen_layout)
                }

                findViewById<RecyclerView>(R.id.compHWRecycler).setOnClickListener {

                }
                val recyclerViewCompHW: RecyclerView = findViewById(R.id.compHWRecycler)
                recyclerViewCompHW.layoutManager = LinearLayoutManager(this)
                updateHomeworkRecyclerViews()
            }

            R.layout.revamp_edithwscreen_layout -> {
                findViewById<ImageButton>(R.id.cancelHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    val id = FirebaseAuth.getInstance().currentUser?.uid.toString()
                    val editCourseDesc =
                        this.findViewById<EditText>(R.id.editCourseDescText).text.toString()
                    val editAssignmentDesc =
                        findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
                    val editDueDate =
                        this.findViewById<EditText>(R.id.editDueDateText).text.toString()
                    val editReminderDate =
                        this.findViewById<EditText>(R.id.editReminderText).text.toString()
                    val editColor =
                        findViewById<EditText>(R.id.editCourseDescText).currentTextColor

                    if (editingHomeworkIndex != -1) {
                        val homework = homeworkList[editingHomeworkIndex]
                        homework.documentId = id
                        homework.courseName = editCourseDesc
                        homework.assignmentDesc = editAssignmentDesc
                        homework.dueDate = editDueDate
                        homework.reminderDate = editReminderDate
                        homework.color = editColor

                    } else {
                        val homework = Homework(
                            id,
                            editCourseDesc,
                            editAssignmentDesc,
                            editDueDate,
                            editReminderDate,
                            editColor
                        )
                        homeworkList.add(homework)
                    }

                    updateHomeworkRecyclerViews()

                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
                }

                findViewById<EditText>(R.id.editCourseDescText).setOnClickListener {
                    inflateLayout(R.layout.revamp_edithwcoursescreen_layout)
                }

                findViewById<EditText>(R.id.editAssignmentDescText).setOnClickListener {

                }

                findViewById<ImageButton>(R.id.completeHWButton).setOnClickListener {
                    val homework = homeworkList[editingHomeworkIndex]
                    homework.isCompleted = true
                    completedHomeworkList.add(homework)
                    Log.d(TAG, "Homework to move: $homework") // log to see if homework has its documentId set
                    moveHomeworkToCompleted(homework, homework.courseId)// Moves homework to completed in firestore
                    homeworkList.remove(homework)
                    updateHomeworkRecyclerViews()
                    inflateLayout(R.layout.revamped_completedhwscreen_layout)

                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val userName = sharedPreferences.getString("userName", "User")
                    val userPhoneNumber =
                        sharedPreferences.getString("userPhoneNumber", "5551234567")

                    val message =
                        "Hey $userName just completed the ${homework.assignmentDesc} for his ${homework.courseName} Class."
                    if (userPhoneNumber != null) {
                        sendSMS(userPhoneNumber, message)
                    }
                }

                val editDueDateText = findViewById<EditText>(R.id.editDueDateText)
                editDueDateText.setOnClickListener {
                    showDatePicker(editDueDateText)
                }


            }

            R.layout.revamp_edithwcoursescreen_layout -> {
                findViewById<Button>(R.id.edit_returnToAddHWButton).setOnClickListener {
                    inflateLayout(R.layout.revamp_edithwscreen_layout)
                }

                val editRecyclerView = findViewById<RecyclerView>(R.id.editCourseRecyclerView)
                if (editRecyclerView != null) {
                    updateCourseRecyclerView()
                }

                updateEditClassDescText =
                    editHomeworkLayout.findViewById(R.id.editCourseDescText)
            }

            R.layout.revamp_homeworkcompscreen_layout -> {
                findViewById<ImageButton>(R.id.cancelHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamped_completedhwscreen_layout)
                }
                findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
                    inflateLayout(R.layout.revamped_completedhwscreen_layout)
                }
//                findViewById<Button>(R.id.curHWButton).setOnClickListener {
//                    inflateLayout(R.layout.revamped_currentupcominghw_layout)
//                }
//                findViewById<Button>(R.id.completeUndoButton).setOnClickListener {
//                }
                findViewById<ImageButton>(R.id.completeDeleteButton).setOnClickListener {

                }
            }

            //code for handling the revamped settings page
            R.layout.revamped_settings_page -> {
                findViewById<ImageView>(R.id.settings_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamp_homescreen_layout)
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
                    inflateLayout(R.layout.revamp_homescreen_layout)
                }
            }

            //code for handling notification page
            R.layout.notifications_page -> {

                reminderSwitch = findViewById(R.id.reminder_switch)
                messageSwitch = findViewById(R.id.message_switch)

                findViewById<ImageView>(R.id.notification_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }

                sharedPreferences =
                    getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)

                // Load the saved states
                reminderSwitch.isChecked =
                    sharedPreferences.getBoolean("reminderSwitchState", false)
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

            //code for edit profile page
            R.layout.edit_profile -> {

                findViewById<ImageView>(R.id.edit_profile_back_button).setOnClickListener {
                    inflateLayout(R.layout.revamped_settings_page)
                }

                findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.change_photo_button).setOnClickListener {
                    //inflateLayout(R.layout.change_photo)
                }

                findViewById<ImageView>(R.id.change_name_button).setOnClickListener {
                    inflateLayout(R.layout.change_name)
                }

                findViewById<ImageView>(R.id.change_number_button).setOnClickListener {
                    inflateLayout(R.layout.change_number)
                }

                findViewById<ImageView>(R.id.add_teacher_button).setOnClickListener {
                    // inflateLayout(R.layout.add_teacher)
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

    // Function to handle Firebase authentication errors
    private fun handleFirebaseError(exception: Exception?) {
        when (exception) {
            // Handle the case where the email is already in use
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
            }

            // Handle the case where the email or password format is invalid
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email or password format", Toast.LENGTH_SHORT).show()
            }

            // Handle other types of exceptions
            else -> {
                Log.e("FirebaseAuth", "Error: $exception")
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // Function to initiate Google sign-in process
    private fun signInGoogle() {
        // Create an intent for the Google sign-in process
        val signInIntent = googleSignInClient.signInIntent
        // Launch the sign-in activity with the intent
        launcher.launch(signInIntent)
    }

    // Function to handle the results of the Google sign-in task
    private fun handleResults(task: Task<GoogleSignInAccount>) {
        // Check if the task was successful
        if (task.isSuccessful) {
            // Get the signed-in Google account
            val account: GoogleSignInAccount? = task.result
            // If the account is not null, log the email and update the UI
            if (account != null) {
                Log.d("SIGN_IN", "Account retrieved: ${account.email}")
                updateUI(account)
            }

        } else {
            // If the task failed, log the error and show a toast message
            Log.e("SIGN_IN", "Error: ${task.exception}")
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }

    }

    // Function to update the UI after successful Google sign-in
    private fun updateUI(account: GoogleSignInAccount) {
        // Get the Google account credential using the ID token
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        // Sign in to Firebase with the Google account credential
        auth.signInWithCredential(credential).addOnCompleteListener {
            // If the sign-in is successful, log the success and inflate the home screen layout
            if (it.isSuccessful) {
                Log.d("SIGN_IN", "Firebase authentication successful")
                inflateLayout(R.layout.revamp_homescreen_layout)

            } else {
                // If the sign-in fails, show a toast message with the error
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to handle Firebase login errors
    private fun handleFirebaseLoginError(exception: Exception?) {
        // Check the type of exception and handle accordingly
        when (exception) {
            // If the email is already in use
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
            }

            // If the email or password format is invalid
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email or password format", Toast.LENGTH_SHORT).show()
            }

            // For other types of exceptions
            else -> {
                Log.e("FirebaseAuth", "Error: $exception")
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

    }
    //Code dealing with firebase ends here

    //Code that has to do with the add homework screen starts here
    private fun showDatePicker(editText: EditText) {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun showDateAndTimePicker(editText: EditText) {
        val currentDateAndTime = Calendar.getInstance()
        val year = currentDateAndTime.get(Calendar.YEAR)
        val month = currentDateAndTime.get(Calendar.MONTH)
        val day = currentDateAndTime.get(Calendar.DAY_OF_MONTH)
        val hour = currentDateAndTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentDateAndTime.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val timePickerDialog = TimePickerDialog(
                    this,
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
    private fun showColorWheel() {
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

        ColorPickerDialog.Builder(this).setTitle("Pick a color for your course!")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton("Save", ColorEnvelopeListener
            { envelope, _ ->
                colorEditText.setTextColor(envelope.color)
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
    class CourseAdapter(
        private val courseList: List<Course>,
        private val mainActivity: MainActivity,
        private val editClassDescText: EditText
    ) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.courseitems, parent, false)
            return CourseViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
            val currentCourse = courseList[position]
            holder.bind(currentCourse)

            holder.itemView.setOnClickListener {
                editClassDescText.setText(currentCourse.courseName)
                mainActivity.inflateLayout(R.layout.addhomeworkscreen_layout)
            }
        }

        override fun getItemCount(): Int {
            return courseList.size
        }

        inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val courseNameButtonView: Button = itemView.findViewById(R.id.courseNameButtonView)
            private val courseDescTextView: TextView = itemView.findViewById(R.id.courseDescTextView)

            init {
                courseNameButtonView.setOnClickListener {
                    val course = courseList[adapterPosition]

                    if (mainActivity.currentLayout == R.layout.revamp_yourcoursesscreen_layout) {
                        mainActivity.inflateLayout(R.layout.revamp_addhomeworkscreen_layout) {
                            val editClassDescText = mainActivity.findViewById<EditText>(R.id.editCourseDescText)
                            editClassDescText?.apply {
                                setText(course.courseName)
                                setTextColor(course.courseColor)
                            }
                        }
                    } else {
                        mainActivity.inflateLayout(R.layout.revamp_edithwscreen_layout) {
                            val updateEditClassDescText = mainActivity.findViewById<EditText>(R.id.editCourseDescText)
                            updateEditClassDescText?.apply {
                                setText(course.courseName)
                                setTextColor(course.courseColor)
                            }
                        }
                    }
                }
            }

            fun bind(course: Course) {
                Log.d(TAG, "Binding course: $course") // Log course being bound
                courseNameButtonView.text = course.courseName
                courseNameButtonView.setTextColor(course.courseColor)
                courseDescTextView.text = course.courseDesc
            }
        }
    }

    private fun updateCourseRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.courseRecyclerView)
        if (recyclerView != null) {
            Log.d(TAG, "Course List Size: ${courseList.size}") // Log course list size
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        val editRecyclerView = findViewById<RecyclerView>(R.id.editCourseRecyclerView)
        if (editRecyclerView != null) {
            Log.d(TAG, "Edit Course List Size: ${courseList.size}") // Log course list size for edit
            val adapter = CourseAdapter(courseList, this, editClassDescText)
            editRecyclerView.adapter = adapter
            editRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }
    //Code that handles courses ends here!

    //Code that handles homework creation, storage and management begins here
    class HomeworkAdapter(
        private val homeworkList: MutableList<Homework>,
        private val mainActivity: MainActivity,
        private val origin: String
    ) : RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeworkViewHolder {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.homeworkitems, parent, false)
            return HomeworkViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: HomeworkViewHolder, position: Int) {
            val currentHomework = homeworkList[position]
            holder.bind(currentHomework)

            holder.itemView.findViewById<Button>(R.id.homeworkButtonView).setOnClickListener {
                when (origin) {
                    "home", "currentUpcoming" -> mainActivity.editHomework(
                        currentHomework,
                        position
                    )
                    "completed" -> mainActivity.viewCompletedHomework(currentHomework)
                }
            }
        }

        override fun getItemCount(): Int {
            return homeworkList.size
        }

        inner class HomeworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val homeworkRecyclerView: Button =
                itemView.findViewById(R.id.homeworkButtonView)

            init {
                homeworkRecyclerView.setOnClickListener {
                    val homework = homeworkList[adapterPosition]
                    //val color = homeworkRecyclerView.currentTextColor
                    (itemView.context as MainActivity).editHomework(homework, position) //,color)
                }
            }

            fun bind(homework: Homework) {
                homeworkRecyclerView.text =
                    "${homework.courseName}: ${homework.assignmentDesc} due ${homework.dueDate}"
                homeworkRecyclerView.setTextColor(homework.color)
            }
        }
    }

    private fun saveHomeworkInput() {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(
            "assignmentDesc",
            findViewById<EditText>(R.id.editAssignmentDescText).text.toString()
        )
        editor.putString("dueDate", findViewById<EditText>(R.id.editDueDateText).text.toString())
        editor.apply()
    }

    private fun loadHomeworkInput() {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        findViewById<EditText>(R.id.editAssignmentDescText).setText(
            sharedPreferences.getString(
                "assignmentDesc",
                ""
            )
        )
        findViewById<EditText>(R.id.editDueDateText).setText(
            sharedPreferences.getString(
                "dueDate",
                ""
            )
        )
        
    }

    private fun clearHomeworkInput() {
        val sharedPreferences = getSharedPreferences("AddHomeworkPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        editClassDescText.setText("")
        findViewById<EditText>(R.id.editAssignmentDescText).setText("")
        findViewById<EditText>(R.id.editDueDateText).setText("")

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

    private fun updateHomeworkRecyclerViews() {
        Log.d(
            TAG,
            "Updating RecyclerViews with ${homeworkList.size} currentHomework items and ${completedHomeworkList.size} completedHomework items"
        )
        val homeRecyclerView = findViewById<RecyclerView>(R.id.homeScreenRecycler)
        if (homeRecyclerView != null) {
            val homeAdapter = HomeworkAdapter(homeworkList, this, "home")
            homeRecyclerView.adapter = homeAdapter
            homeRecyclerView.layoutManager = LinearLayoutManager(this)
            homeAdapter.notifyDataSetChanged()
        }

        val currentRecyclerView = findViewById<RecyclerView>(R.id.curHWRecycler)
        if (currentRecyclerView != null) {
            val currentAdapter = HomeworkAdapter(homeworkList, this, "currentUpcoming")
            currentRecyclerView.adapter = currentAdapter
            currentRecyclerView.layoutManager = LinearLayoutManager(this)
            currentAdapter.notifyDataSetChanged()
        }

        val completedRecyclerView = findViewById<RecyclerView>(R.id.compHWRecycler)
        if (completedRecyclerView != null) {
            val completedAdapter = HomeworkAdapter(completedHomeworkList, this, "completed")
            completedRecyclerView.adapter = completedAdapter
            completedRecyclerView.layoutManager = LinearLayoutManager(this)
            completedAdapter.notifyDataSetChanged()
        }
    }

    fun editHomework(homework: Homework, index: Int) {
        editingHomeworkIndex = index
        inflateLayout(R.layout.revamp_edithwscreen_layout)
        {
            findViewById<EditText>(R.id.editCourseDescText).apply {
                setText(homework.courseName)
                setTextColor(homework.color)
            }
            findViewById<EditText>(R.id.editAssignmentDescText).setText(homework.assignmentDesc)
            findViewById<EditText>(R.id.editDueDateText).setText(homework.dueDate)
        }
        findViewById<ImageButton>(R.id.completeHWButton).setOnClickListener {
            homework.isCompleted = true
            completedHomeworkList.add(homework)
            moveHomeworkToCompleted(homework, homework.courseName) // Move homework from current to completed in Firestore
            homeworkList.remove(homework)
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.revamped_completedhwscreen_layout)
        }
    }

    fun viewCompletedHomework(homework: Homework) {
        inflateLayout(R.layout.revamp_homeworkcompscreen_layout)
        {
            findViewById<EditText>(R.id.editCourseDescText).apply {
                setText(homework.courseName)
                setTextColor(homework.color)
            }
            findViewById<EditText>(R.id.editAssignmentDescText).setText(homework.assignmentDesc)
            findViewById<EditText>(R.id.editDueDateText).setText(homework.dueDate)
        }
        findViewById<ImageButton>(R.id.addHWbutton).setOnClickListener {
            homework.isCompleted = false
            completedHomeworkList.remove(homework)
            homeworkList.add(homework)
            moveHomeworkToCurrent(homework, homework.courseName) // Move homework back to current in Firestore
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.revamped_currentupcominghw_layout)
        }
        findViewById<ImageButton>(R.id.completeDeleteButton).setOnClickListener {
            homework.isCompleted = true
            deleteHomeworkFromFireStore(homework, homework.courseName)
            completedHomeworkList.remove(homework) // Delete homework from firestore
            updateHomeworkRecyclerViews()

            inflateLayout(R.layout.revamped_completedhwscreen_layout)
        }
    }
    //Code that handles homework ends here!

    //code for uploading and editing data to the database starts here

    // Function to upload user profile data to Firestore
    private fun uploadProfileData() {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // Retrieve the profile data from the EditText fields
        val firstName = findViewById<EditText>(R.id.editFirstNameText).text.toString() // Get the first name from the EditText
        val lastName = findViewById<EditText>(R.id.editLastNameText).text.toString() // Get the last name from the EditText
        val email = findViewById<EditText>(R.id.editEmailText).text.toString() // Get the email from the EditText
        val parentPhoneNum = findViewById<EditText>(R.id.editParentPhoneNumText).text.toString() // Get the parent's phone number from the EditText

        // Create a map to hold user profile data
        val user: MutableMap<String, Any> = HashMap()
        user["firstName"] = firstName // Add first name to the map
        user["lastName"] = lastName // Add last name to the map
        user["email"] = email // Add email to the map
        user["parentPhoneNum"] = parentPhoneNum // Add parent's phone number to the map

        // Upload the user profile data to Firestore
        fireStoreDatabase.collection("users").document(userId).set(user)

            .addOnSuccessListener {
                // Log success message with the user ID
                Log.d(TAG, "Added document with ID: $userId")
            }
            .addOnFailureListener { e ->
                // Log failure message with the error
                Log.w(TAG, "Error adding document: $userId", e)
            }
    }

    // Function to download and display user profile information from Firestore
    private fun downloadProfileInfo() {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // Fetch the user's profile document from Firestore
        fireStoreDatabase.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Retrieve profile information from the document
                    val firstName = document.getString("firstName") ?: "" // Get first name or empty string if null
                    val lastName = document.getString("lastName") ?: "" // Get last name or empty string if null
                    val email = document.getString("email") ?: ""// Get email or empty string if null
                    val parentPhoneNum = document.getString("parentPhoneNum") ?: "" // Get parent's phone number or empty string if null


                    // Set the retrieved profile information in the corresponding EditText fields
                    findViewById<EditText>(R.id.editFirstNameText).setText(firstName)
                    findViewById<EditText>(R.id.editLastNameText).setText(lastName)
                    findViewById<EditText>(R.id.editEmailText).setText(email)
                    findViewById<EditText>(R.id.editParentPhoneNumText).setText(parentPhoneNum)

                    // Log success message with the user ID
                    Log.d(TAG, "Profile successfully downloaded for user ID: $userId")
                } else {
                    // Log message if no profile data is found
                    Log.d(TAG, "No profile data found for user ID: $userId")
                }
            }
            .addOnFailureListener { e ->
                // Log failure message with the error
                Log.w(TAG, "Error downloading profile for user ID: $userId", e)
            }

    }

    // Function to upload homework data to Firestore
    private fun uploadHomeworkData(homework: Homework) {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // Create a map to hold homework data
        val userHomeworkMap: MutableMap<String, Any> = HashMap()
        userHomeworkMap["courseDesc"] = homework.courseDesc // Set course description from homework
        userHomeworkMap["assignmentDesc"] = homework.assignmentDesc // Set assignment description from homework
        userHomeworkMap["dueDate"] = homework.dueDate // Set due date from homework
        userHomeworkMap["reminderDate"] = homework.reminderDate // Set reminder date from homework
        userHomeworkMap["color"] = homework.color // Set color from homework
        userHomeworkMap["courseId"] = homework.courseId // Set course ID from homework
        userHomeworkMap["isCompleted"] = homework.isCompleted // Set completion status from homework

        // Determine the collection path based on whether the homework is completed
        val collectionPath = if (homework.isCompleted) "completedHomework" else "currentHomework"

        // Generate a new document ID for the homework
        val documentId = homework.documentId ?: fireStoreDatabase
            .collection("users")
            .document(userId)
            .collection("courses")
            .document(homework.courseId)
            .collection(collectionPath)
            .document().id

        // Upload the homework data to Firestore
        fireStoreDatabase.collection("users")
            .document(userId)
            .collection("courses")
            .document(homework.courseId)
            .collection(collectionPath)
            .document(documentId)
            .set(userHomeworkMap)
            .addOnSuccessListener {
                // Log success message with the document ID
                Log.d(TAG, "Homework successfully uploaded with ID: $documentId")
                // Set the document ID in the homework object
                homework.documentId = documentId
            }
            .addOnFailureListener {
                // Log failure message with the error
                Log.w(TAG, "Error adding document $it")
            }
        // Save the course data
        saveCourseData(homework)
    }

    // Function to save course data associated with a homework item to Firestore
    private fun saveCourseData(homework: Homework) {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        // Create a map to hold course data
        val courseMap: MutableMap<String, Any> = HashMap()
        courseMap["courseName"] = homework.courseDesc // Set course name from homework description
        courseMap["courseColor"] = homework.color  // Set course color from homework color
        courseMap["courseId"] = homework.courseDesc // Set course ID from homework description
        courseMap["courseDesc"] = homework.courseDesc // Set course description from homework description

        // Save or update the course data in Firestore
        fireStoreDatabase.collection("users")
            .document(userId)
            .collection("courses")
            .document(homework.courseId)
            .set(courseMap, SetOptions.merge())  // Merge to avoid overwriting data
            .addOnSuccessListener {
                // Log success message with the course ID
                Log.d(TAG, "Course successfully saved/updated for ID: ${homework.courseId}")
            }
            .addOnFailureListener {
                // Log failure message with the error
                Log.w(TAG, "Error adding course document $it")
            }
    }

     //Function to retrieve courses and their associated homework from Firestore
    private fun retrieveCoursesAndHomework(userId: String) {
        homeworkList.clear() // Clear the existing list
        completedHomeworkList.clear() // Clear the completed list

        // Get an instance of Firestore
        val firestore = Firebase.firestore
        // Reference to the courses collection in Firestore
        firestore.collection("users")
            .document(userId)
            .collection("courses")
            .get()
            .addOnSuccessListener { documents ->
                // Get the total number of courses
                val totalCourses = documents.size()
                var coursesFetched = 0

                // Iterate through each course document in the fetched documents
                for (document in documents) {
                    try {
                        // Convert the document to a Course object
                        val course = document.toObject<Course>()
                        // Log for debugging what documents and course
                        Log.d(TAG, "Document ID: ${document.id}")
                        Log.d(TAG, "Course retrieved: $course")
                        // If the course object and its ID are not null or blank
                        if (course != null && course.courseId.isNotBlank()) {
                            Log.d(TAG, "Fetching homework for course ${course.courseId}")
                            // Add the course to the course list
                            courseList.add(course)
                            // Retrieve the homework data for the course
                            retrieveHomeworkData(userId, course.courseId)
                            coursesFetched++
                            // Update the RecyclerView after all courses have been fetched
                            if (coursesFetched == totalCourses) {
                                updateHomeworkRecyclerViews()
                            }
                        } else {
                            // Log warning if the course or course ID is null or blank
                            Log.w(TAG, "Course or courseId is null/blank for document: ${document.id}")
                            coursesFetched++
                            // Update the RecyclerView after all courses have been fetched
                            if (coursesFetched == totalCourses) {
                                updateHomeworkRecyclerViews()
                            }
                        }
                    } catch (e: Exception) {
                        // Log error if there is an issue converting the document
                        Log.e(TAG, "Error converting course document", e)
                        coursesFetched++
                        // Update the RecyclerView after all courses have been fetched
                        if (coursesFetched == totalCourses) {
                            updateHomeworkRecyclerViews()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Log failure message with the exception
                Log.w(TAG, "Error getting documents: ", exception)
            }


    }

    // Function to retrieve homework data (both current and completed) from Firestore
    private fun retrieveHomeworkData(userId: String, courseId: String) {
        // Get an instance of Firestore
        val firestore = Firebase.firestore


        // Reference to the current homework collection in Firestore
        val currentHomeworkRef = firestore.collection("users")
            .document(userId)
            .collection("courses")
            .document(courseId)
            .collection("currentHomework")

        // Log for debugging fetching current homework documents from Firestore
        Log.d(
            TAG,
            "Fetching currentHomework from path: users/$userId/courses/$courseId/currentHomework"
        )
        currentHomeworkRef.get()
            .addOnSuccessListener { documents ->
                // Log success message
                Log.d(TAG, "Successfully fetched currentHomework for course $courseId")
                // Iterate through each document in the fetched documents
                for (document in documents) {
                    try {
                        // Convert the document to a Homework object
                        val homework = document.toObject<Homework>()
                        // If conversion is successful, add it to the homeworkList

                        homework.courseName = courseId // Ensure course name is set

                        homework.documentId = document.id

                        if (homework != null) {
                            homeworkList.add(homework)
                            Log.d(TAG, "Homework retrieved: $homework") // Log homework details
                        }
                    } catch (e: Exception) {
                        // Log error if there is an issue converting the document
                        Log.e(TAG, "Error converting currentHomework document", e)
                    }
                }
                // Log for debugging after fetching the homework
                Log.d(
                    TAG,
                    "Fetching completedHomework from path: users/$userId/courses/$courseId/completedHomework"
                )
                // Fetch completed homework after current homework is fetched
                retrieveCompletedHW(userId, courseId)

            }
            .addOnFailureListener { exception ->
                // Log failure message with the exception
                Log.e(TAG, "Error getting currentHomework documents: ", exception)
            }
    }

     //Fetch Completed Homework
    private fun retrieveCompletedHW(userId: String, courseId: String) {
        // Get an instance of Firestore
        val firestore = Firebase.firestore
        // Reference to the completed homework collection in Firestore
        val completedHomeworkRef = firestore.collection("users")
            .document(userId)
            .collection("courses")
            .document(courseId)
            .collection("completedHomework")

        // Fetch the completed homework documents from Firestore
        completedHomeworkRef.get()
            .addOnSuccessListener { documents ->
                // Log success message
                Log.d(TAG, "Successfully fetched completedHomework for course $courseId")
                // Iterate through each document in the fetched documents
                for (document in documents) {
                    try {
                        // Convert the document to a Homework object
                        val homework = document.toObject<Homework>()
                        homework.courseName = courseId // Ensure course name is set
                        homework.documentId = document.id
                        // If conversion is successful, add it to the completedHomeworkList
                        if (homework != null) {
                            completedHomeworkList.add(homework)
                        }
                    } catch (e: Exception) {
                        // Log error if there is an issue converting the document
                        Log.e(TAG, "Error converting completedHomework document", e)
                    }
                }
                // Update RecyclerView after fetching all data
                updateHomeworkRecyclerViews()
            }
            .addOnFailureListener { exception ->
                // Log failure message with the exception
                Log.e(TAG, "Error getting completedHomework documents: ", exception)
            }
    }

    // Function to delete a homework item from Firestore
    private fun deleteHomeworkFromFireStore(homework: Homework, courseId: String) {
        // Get an instance of Firestore
        val firestore = Firebase.firestore
        // Get the current user's ID from FirebaseAuth, return if user is not authenticated
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid ?: return // Ensure user is authenticated

        homework.courseName = courseId // Ensure course name is set

        // Check if the homework item has a document ID
        homework.documentId?.let { documentId ->
            // Reference to the completed homework document in Firestore
            val completedHomeworkRef = firestore.collection("users")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .collection("completedHomework")
                .document(documentId) // Use homework ID or unique identifier
                // Delete the completed homework document from Firestore
                .delete()
                .addOnSuccessListener {
                    // Log success message with the document ID
                    Log.d(TAG, "Homework successfully deleted with ID: $documentId")
                }
                .addOnFailureListener { e ->
                    // Log failure message with the error
                    Log.w(TAG, "Error deleting document", e)
                }
        }

    }

    // Function to move a homework item from "current" to "completed" in Firestore
    private fun moveHomeworkToCompleted(homework: Homework, courseId: String) {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        if (userId == null) {
            Log.w(TAG, "User is not authenticated")
            return
        }


        homework.courseName = courseId // Ensure course name is set

        // Check if the homework item has a document ID
        homework.documentId?.let { documentId ->
            // Reference to the current homework document in Firestore
            val currentHomeworkRef = fireStoreDatabase
                .collection("users")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .collection("currentHomework")
                .document(documentId)

            // Reference to the completed homework document in Firestore
            val completedHomeworkRef = fireStoreDatabase
                .collection("users")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .collection("completedHomework")
                .document(documentId)

            // Log the path of the current homework document for debugging
            Log.d(TAG, "Moving homework to completed with path: ${currentHomeworkRef.path}")

            // Run a Firestore transaction to move the homework item
            fireStoreDatabase.runTransaction { transaction ->
                // Get the current homework document snapshot
                val snapshot = transaction.get(currentHomeworkRef)
                if (snapshot.exists()) {
                    // Set the completed homework document with the data from the current homework document
                    transaction.set(completedHomeworkRef, snapshot.data!!)
                    // Delete the current homework document
                    transaction.delete(currentHomeworkRef)
                } else {
                    Log.w(TAG, "Current homework document does not exist")
                    throw Exception("Current homework document does not exist")
                }
            }.addOnSuccessListener {
                // Log success message with the document ID
                Log.d(TAG, "Homework moved to completed with ID: $documentId")
            }.addOnFailureListener { e ->
                // Log failure message with the error
                Log.w(TAG, "Error moving document", e)
            }
        } ?: Log.w(TAG, "Document ID is null")
    }

    // Function to move a homework item from "completed" to "current" in Firestore
    private fun moveHomeworkToCurrent(homework: Homework, courseId: String) {
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        homework.courseName = courseId // Ensure course name is set

        // Check if the homework item has a document ID
        homework.documentId?.let { documentId ->
            // Reference to the completed homework document in Firestore
            val completedHomeworkRef = fireStoreDatabase
                .collection("users")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .collection("completedHomework")
                .document(documentId)

            // Reference to the current homework document in Firestore
            val currentHomeworkRef = fireStoreDatabase
                .collection("users")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .collection("currentHomework")
                .document(documentId)

            // Run a Firestore transaction to move the homework item
            fireStoreDatabase.runTransaction { transaction ->
                // Get the completed homework document snapshot
                val snapshot = transaction.get(completedHomeworkRef)
                if (snapshot.exists()) {
                    // Set the current homework document with the data from the completed homework document
                    transaction.set(currentHomeworkRef, snapshot.data!!)
                    // Delete the completed homework document
                    transaction.delete(completedHomeworkRef)
                } else {
                    Log.w(TAG, "Completed homework document does not exist")
                    throw Exception("Completed homework document does not exist")
                }
            }.addOnSuccessListener {
                // Log success message with the document ID
                Log.d(TAG, "Homework moved back to current with ID: $documentId")
            }.addOnFailureListener { e ->
                // Log failure message with the error
                Log.w(TAG, "Error moving document", e)
            }
        }?: Log.w(TAG, "Document ID is null")
    }

    // Function to save a course to Firestore
    private fun saveCourse(courseName: String, courseColor: Int, courseDesc: String) {
        // Get the current user's ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser!!.uid ?: return
        // Get an instance of Firestore
        val fireStoreDatabase = FirebaseFirestore.getInstance()
        // Generate a unique ID for the new course document
        val courseId = fireStoreDatabase.collection("users")
            .document(userId)
            .collection("courses")
            .document().id
        // Create a Course object with the provided details
        val course = Course(
            courseName = courseName,
            courseColor = courseColor,
            //courseDesc = courseDesc,
            //courseId = courseId
        )

        // Optionally log the Course object
        // Log.d(TAG, "Course to be saved: $course")

        // Create a map to represent the course data for Firestore
        val courseMap: MutableMap<String, Any> = HashMap()
        courseMap["courseName"] = courseName
        courseMap["courseColor"] = courseColor
        courseMap["courseDesc"] = courseDesc
        courseMap["courseId"] = courseId

        // Log the course data map for debugging
        Log.d(TAG, "Course to be saved: $courseMap")

        // Save the course data map to Firestore
        fireStoreDatabase.collection("users")
            .document(userId)
            .collection("courses")
            .document(courseId)
            .set(courseMap)
            .addOnSuccessListener {
                // Log success and add the course to the local course list
                Log.d(TAG, "Course successfully saved with ID: $courseId")
                courseList.add(course)
            }
            .addOnFailureListener {
                // Log failure with the error message
                Log.w(TAG, "Error adding course document $it")
            }
    }
}

    //code for uploading, retrieving, and editing data to the database ends here













