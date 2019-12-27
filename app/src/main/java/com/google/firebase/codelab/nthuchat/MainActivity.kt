package com.google.firebase.codelab.nthuchat

import android.Manifest
import android.Manifest.permission.*
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.support.v4.app.Fragment
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.annotation.NonNull
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AlertDialog
import android.text.InputFilter
import android.util.Log
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*

import com.facebook.stetho.Stetho
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.AccountPicker
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.picasso.Picasso

import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_schoolchat.*
import kotlinx.android.synthetic.main.content_main.*

private const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
private const val REQUEST_CODE_ACCESS_FINE_LOCATION = 2
private const val REQUEST_GET_ACCOUNT_NAME = 3
var navigationView: NavigationView? = null//set to public to setCheckedItem in fragment
var accountString: String? = null

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    // Firebase instance variables
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    //private val mFirebaseRemoteConfig: FirebaseRemoteConfig? = null  //WTF
    //private val mFirebaseAnalytics: FirebaseAnalytics? = null  //WTF
    //private val mAdView: AdView? = null  //WTF
    var mUsername: String? = null
    var mPhotoUrl: String? = null
    var mUid: String? = null
    lateinit private var mNameView: TextView
    //private var mEmailView: TextView? = null
    //private var mIconView: ImageView? = null
    var drawer: DrawerLayout? = null
    //private var headerView: View? = null
    //var fab: FloatingActionButton? = null  //WTF
    lateinit var currentFragment: Fragment
    private var mFirebaseDB: DatabaseReference? = null
    //private val mFBdiv: DatabaseReference? = null  //WTF
    var dbinstance: AppDatabase? = null
    var user: User? = null
    var sub1: Menu? = null

    //retrieve user location
    private var mLocationRequest: LocationRequest? = null
//    private val UPDATE_INTERVAL: Long = 600 * 1000 //10 minutes = 600 seconds -> the unit is milliseconds for this variable
    private val UPDATE_INTERVAL: Long = 10 * 1000
//    private val FASTEST_INTERVAL: Long = 60 * 1000
    private val FASTEST_INTERVAL: Long = 2000



    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Stetho.initializeWithDefaults(this);
        setContentView(R.layout.activity_main)
        navigationView = findViewById(R.id.nav_view)
        var headerView: View = navigationView?.getHeaderView(0) as View
        mNameView = headerView.findViewById(R.id.nameView)
        var mEmailView: TextView = headerView.findViewById(R.id.emailView)
        var mIconView: ImageView = headerView.findViewById(R.id.iconView)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        dbinstance = AppDatabase.getAppDatabase(applicationContext)
        user = dbinstance?.userDao()?.getUser()

        mFirebaseDB = FirebaseDatabase.getInstance().reference

        drawer = findViewById(R.id.drawer_layout)
        val toggle = object : ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset != 0f) {
                    hideKeyboard(this@MainActivity)
                }
            }
        }

        drawer?.addDrawerListener(toggle)
        toggle.syncState()

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth!!.getCurrentUser()
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            if (mFirebaseUser!!.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser!!.getPhotoUrl().toString()
                mUsername = mFirebaseUser!!.getDisplayName()
                mUid = mFirebaseUser!!.getUid()
                if (mPhotoUrl != null && mPhotoUrl!!.contains("..")) {
                    mPhotoUrl = "https://nthuchat.com" + mPhotoUrl!!.replace("..", "")
                }
                //Toast.makeText(this, "name:  "+mPhotoUrl, Toast.LENGTH_SHORT).show();
                mNameView.setText(mUsername)
                mEmailView.setText(mFirebaseUser!!.getEmail())
                Picasso.with(this@MainActivity).load(mPhotoUrl).transform(CropCircleTransformation()).into(mIconView)
                //mIconView.setImageURI(Uri.parse(mPhotoUrl));
            } else {
                val picnum = Math.round(Math.random() * 12 + 1).toInt()
                val namelist = arrayOf("葉葉", "畫眉", "JIMMY", "阿醜", "茶茶", "麥芽", "皮蛋", "小豬", "布丁", "黑嚕嚕", "憨吉", "LALLY", "花捲")
                val namenum = Math.round(Math.random() * namelist.size).toInt()
                val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(namelist[namenum])
                        .setPhotoUri(Uri.parse("../images/user$picnum.jpg")).build()

//                Original Error Kotlin Code
//                mFirebaseUser!!.updateProfile(profileUpdate)
//                        .addOnCompleteListener( object : OnCompleteListener<Void>() {
//                             fun onComplete(@NonNull task: Task<Void>) {
//                                if (task.isSuccessful()) {
//                                    mUsername = mFirebaseUser!!.getDisplayName()
//                                    mUid = mFirebaseUser!!.getUid()
//                                    mPhotoUrl = mFirebaseUser!!.getPhotoUrl().toString()
//                                    mNameView!!.setText(mUsername)
//                                    mEmailView!!.setText(mFirebaseUser!!.getEmail())
//                                    if (mPhotoUrl != null && mPhotoUrl!!.contains("..")) {
//                                        mPhotoUrl = "https://nthuchat.com" + mPhotoUrl!!.replace("..", "")
//                                    }
//                                    Picasso.with(this@MainActivity).load(mPhotoUrl).transform(CropCircleTransformation()).into(mIconView)
//                                }
//                            }
//                        })

                mFirebaseUser!!.updateProfile(profileUpdate)
                        .addOnCompleteListener{ task ->
                            fun onComplete(@NonNull task: Task<Void>) {
                                if (task.isSuccessful()) {
                                    mUsername = mFirebaseUser!!.getDisplayName()
                                    mUid = mFirebaseUser!!.getUid()
                                    mPhotoUrl = mFirebaseUser!!.getPhotoUrl().toString()
                                    mNameView.setText(mUsername)
                                    mEmailView.setText(mFirebaseUser!!.getEmail())
                                    if (mPhotoUrl != null && mPhotoUrl!!.contains("..")) {
                                        mPhotoUrl = "https://nthuchat.com" + mPhotoUrl!!.replace("..", "")
                                    }
                                    Picasso.with(this@MainActivity).load(mPhotoUrl).transform(CropCircleTransformation()).into(mIconView)
                                }
                            }
                        }

            }

        }

        if (user != null) {
            navigationView?.getMenu()?.findItem(R.id.div)?.setTitle(user!!.Div)
            val coursename = user!!.Classes
            val course_title = coursename.split("#".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            //Toast.makeText(this, "course.length: "+course_title.length, Toast.LENGTH_SHORT).show();
            if (course_title.size > 1) {
                sub1 = navigationView?.getMenu()?.addSubMenu(R.id.course_menu, 49, 49, R.string.courses)
                for (id in 0..course_title.size - 1) {
                    //Toast.makeText(MainActivity.this, course_title[id], Toast.LENGTH_SHORT).show();
                    sub1?.add(0, 50 + id, 50 + id, course_title[id])?.setIcon(R.drawable.ic_assignment_black_18dp)!!.isCheckable = true
                }
            }
        } else {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        navigationView?.setNavigationItemSelectedListener(this)
        displaySelectedScreen(R.id.school)

        //request permission
        val listPermissionsNeeded = ArrayList<String>()
        val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
        val hasCameraPermission = ContextCompat.checkSelfPermission(this, CAMERA)
        val hasReadExternalPermission = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
        val hasWriteExternalPermission = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, READ_CALENDAR)
        val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, WRITE_CALENDAR)

        if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(ACCESS_FINE_LOCATION)
        } else {
            //retrieve user location
            startLocationUpdates()
        }

        if(hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(CAMERA)
        }
        if(hasReadExternalPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(READ_EXTERNAL_STORAGE)
        }
        if(hasWriteExternalPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(WRITE_EXTERNAL_STORAGE)
        }
        if(hasReadCalendarPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(READ_CALENDAR)
        }
        if(hasWriteCalendarPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(WRITE_CALENDAR)
        }

        if(!listPermissionsNeeded.isEmpty()) {
            val array = arrayOfNulls<String>(listPermissionsNeeded.size)
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(array), REQUEST_ID_MULTIPLE_PERMISSIONS)
        }

        mFirebaseDB!!.child("users").child(mUid.toString()).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(childSnapshot in dataSnapshot.children)
                    if(childSnapshot.key == "accountName")
                        accountString = childSnapshot.value.toString()
                if(accountString == null) {
                    get_account_name()
                }
            }
        })
    }

    protected fun startLocationUpdates() {
        // initialize location request object
        mLocationRequest = LocationRequest.create()
        mLocationRequest!!.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
            setFastestInterval(FASTEST_INTERVAL)
        }

        // initialize locationo setting request builder object
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        // initialize location service object
        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient!!.checkLocationSettings(locationSettingsRequest)

        // call register location listner
        registerLocationListner()



    }
    private fun registerLocationListner() {
        // initialize location callback object
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                onLocationChanged(locationResult!!.getLastLocation())
            }
        }
        // add permission if android version is greater then 23
        if(Build.VERSION.SDK_INT >= 23 && checkLocationPermission()) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper())
        }

    }

    private fun onLocationChanged(location: Location) {
        // create message for toast with updated latitude and longitude
        val sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
        val isSharedLocation = sharedPreferences.getBoolean("SwitchBtn", false)
        if(isSharedLocation) { //to handle the share location is closed by user after open it
            var msg = "Updated Location: " + location.latitude  + " , " + location.longitude
            Log.d("user location", "user location: $msg")
            val gps = location.latitude.toString()  + " , " + location.longitude.toString()
            mFirebaseDB!!.child("users").child(mUid.toString()).child("gps").setValue(gps)
        }

    }

    private fun checkLocationPermission() : Boolean {
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            requestLocationPermission()
            return false
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf("Manifest.permission.ACCESS_FINE_LOCATION"), REQUEST_CODE_ACCESS_FINE_LOCATION)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION ) {
//                registerLocationListner()
                //retrieve user location
                startLocationUpdates()
            }
        }
    }

    override fun onBackPressed() {
        drawer = findViewById(R.id.drawer_layout)
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        //getSupportFragmentManager().beginTransaction().add(R.id.content_frame, currentFragment).commit();
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.getItemId()) {
            R.id.addFriend -> {
                //QR addFriend
                displaySelectedScreen(R.id.addFriend)
                navigationView!!.setCheckedItem(R.id.menu_none)
                return true
            }
            R.id.addInspiration -> {
                displaySelectedScreen(R.id.addInspiration)
                navigationView!!.setCheckedItem(R.id.menu_none)
                return true
            }
            R.id.addStudy -> {
                displaySelectedScreen(R.id.addStudy)
                navigationView!!.setCheckedItem(R.id.menu_none)
                return true
            }
            R.id.addEntertainment -> {
                displaySelectedScreen(R.id.addEntertainment)
                navigationView!!.setCheckedItem(R.id.menu_none)
                return true
            }
            R.id.addSocial -> {
                displaySelectedScreen(R.id.addSocial)
                navigationView!!.setCheckedItem(R.id.menu_none)
                return true
            }
            else -> {
                hideKeyboard(this)
                return super.onOptionsItemSelected(item)
            }
        }
    }


    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.getItemId()
        when (id) {
            R.id.school -> {
                displaySelectedScreen(R.id.school)
            }
            R.id.div -> {
                displaySelectedScreen(R.id.div)
            }
            R.id.change_name -> {
                val title = getString(R.string.change_name)
                val intro = getString(R.string.change_name_intro)
                val confirm = getString(R.string.confirm)
                val cancel = getString(R.string.cancel)
                val lastname = mFirebaseUser!!.getDisplayName()

                val alertdialog = AlertDialog.Builder(this@MainActivity)
                val editText = EditText(this@MainActivity)
                val container = FrameLayout(this@MainActivity)
                val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin)
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin)
                editText.setLayoutParams(params)
                editText.setHint(lastname)
                editText.setMaxLines(1)
                editText.setSingleLine()
                editText.setFilters(arrayOf<InputFilter>(InputFilter.LengthFilter(20)))
                container.addView(editText)

                alertdialog.setTitle(title)//設定視窗標題
                        .setIcon(R.mipmap.ic_launcher)//設定對話視窗圖示
                        .setMessage(intro)//設定顯示的文字
                        .setView(container)
//                        Original Kotlin Error Codre
//                        .setNegativeButton(cancel, object : DialogInterface.OnClickListener() {
//                            override fun onClick(dialog: DialogInterface, which: Int) {
//                                Toast.makeText(this@MainActivity, "Canceled Change Name", Toast.LENGTH_SHORT).show()
//                            }


                        .setNegativeButton(cancel, object : DialogInterface.OnClickListener {
                             override fun onClick(dialog: DialogInterface, which: Int) {
                                Toast.makeText(this@MainActivity, "Canceled Change Name", Toast.LENGTH_SHORT).show()
                            }

                        })//設定結束的子視窗
                        .setPositiveButton(confirm, object : DialogInterface.OnClickListener {

                            override fun onClick(dialog: DialogInterface, which: Int) {
                                val changename = editText.getText().toString()
                                if (changename.contains(" ")) {
                                    changename.replace(" ".toRegex(), "")
                                }
                                if (changename.trim({ it <= ' ' }).length > 0) {
                                    val profileUpdate = UserProfileChangeRequest.Builder()
                                            .setDisplayName(changename).build()
//                                  Original Error Kotlin
//                                    mFirebaseUser!!.updateProfile(profileUpdate)
//                                            .addOnCompleteListener(object : OnCompleteListener<Void>() {
//                                                override fun onComplete(@NonNull task: Task<Void>) {
//                                                    if (task.isSuccessful()) {
//                                                        mUsername = mFirebaseUser!!.getDisplayName()
//                                                        mUid = mFirebaseUser!!.getUid()
//                                                        mPhotoUrl = mFirebaseUser!!.getPhotoUrl().toString()
//                                                        Toast.makeText(this@MainActivity, "Now your name: $mUsername", Toast.LENGTH_SHORT).show()
//                                                        mNameView!!.setText(mUsername)
//                                                    }
//                                                }
//                                            })

                                    mFirebaseUser!!.updateProfile(profileUpdate)
                                            .addOnCompleteListener { task ->
                                                 fun onComplete(@NonNull task: Task<Void>) {
                                                    if (task.isSuccessful()) {
                                                        mUsername = mFirebaseUser!!.getDisplayName()
                                                        mUid = mFirebaseUser!!.getUid()
                                                        mPhotoUrl = mFirebaseUser!!.getPhotoUrl().toString()
                                                        Toast.makeText(this@MainActivity, "Now your name: $mUsername", Toast.LENGTH_SHORT).show()
                                                        mNameView.setText(mUsername)
                                                    }
                                                }
                                            }


                                }
                            }
                        })//設定結束的子視窗
                        .show()
            }
            R.id.share_location -> {
                displayShareLocationDialog()
            }
            R.id.sign_out_menu -> {
                mFirebaseAuth!!.signOut()
                dbinstance?.userDao()?.delete(dbinstance?.userDao()?.getUser())
                AppDatabase.destroyInstance()
                startAnimatedActivity(Intent(this, SignInActivity::class.java))
            }
            R.id.drawer_friends -> {
                //load switch state
                val sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(applicationContext)

                if(sharedPreferences.getBoolean("SwitchBtn", false)) {
                    displaySelectedScreen(R.id.drawer_friends)
                } else {
                    var sw: Switch  = Switch(this)
                    sw.textOn = "start"
                    sw.textOff = "close"

                    //load switch state
                    val sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(applicationContext)
                    sw.isChecked = sharedPreferences.getBoolean("SwitchBtn", false)  //default is false
                    //Toast.makeText(this, "switch state: " + sharedPreferences.getBoolean("SwitchBtn", false), Toast.LENGTH_LONG).show()

                    var linearLayout: LinearLayout  = LinearLayout(this)
                    linearLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
                    linearLayout.gravity = Gravity.CENTER_HORIZONTAL
                    linearLayout.addView(sw)


                    var myDialog: AlertDialog.Builder  = AlertDialog.Builder(this)
                    myDialog.setTitle(getString(R.string.share_location))
                    myDialog.setMessage(getString(R.string.share_location_details))
                    myDialog.setView(linearLayout)

                    myDialog.show()

                    //save switch state
                    sw.setOnClickListener {
                        val sharedPreferences = PreferenceManager
                                .getDefaultSharedPreferences(applicationContext)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("SwitchBtn", sw.isChecked)
                        editor.commit()

                        if(sharedPreferences.getBoolean("SwitchBtn", false)){
                            displaySelectedScreen(R.id.drawer_friends)
                        }
                    }
                    //navigationView!!.menu.findItem(R.id.school).isChecked = true
                    //displaySelectedScreen(R.id.school)
                    //Toast.makeText(this, "Please open share location in settings", Toast.LENGTH_LONG).show()
                }
            }

            else -> displaySelectedScreen(id)
        }
        hideKeyboard(this)
        drawer = findViewById(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }

    protected fun startAnimatedActivity(intent: Intent) {
        startActivity(intent)
        hideKeyboard(this)
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
    }

    private fun displaySelectedScreen(itemId: Int) {

        //creating fragment object
        var fragment: Fragment? = null
        //initializing the fragment object which is selected
        when (itemId) {
            R.id.addFriend -> {
                fragment = showOwnQR()
            }
            R.id.addInspiration -> {
                val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

                if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_ACCESS_FINE_LOCATION)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = AddInspirationActivityFragment()
                }
            }
            R.id.addStudy -> {
                val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

                if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_ACCESS_FINE_LOCATION)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = AddStudyActivityFragment()
                }
            }
            R.id.addEntertainment -> {
                val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

                if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_ACCESS_FINE_LOCATION)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = AddEntertainmentActivityFragment()
                }
            }
            R.id.addSocial -> {
                val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

                if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_ACCESS_FINE_LOCATION)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = AddSocialActivityFragment()
                }
            }
            R.id.school -> {
                fragment = Schoolchat()
                navigationView?.setCheckedItem(itemId)
            }
            R.id.div -> {
                fragment = Department()
                navigationView?.setCheckedItem(itemId)
            }
            R.id.drawer_inspiration -> {
                val listPermissionsNeeded = ArrayList<String>()
                val hasLocationPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, READ_CALENDAR)
                val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, WRITE_CALENDAR)

                if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(ACCESS_FINE_LOCATION)
                }
                if(hasReadCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(READ_CALENDAR)
                }
                if(hasWriteCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(WRITE_CALENDAR)
                }

                if (!listPermissionsNeeded.isEmpty()) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, READ_CALENDAR) || ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_CALENDAR)) {
                        val array = arrayOfNulls<String>(listPermissionsNeeded.size)
                        ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(array), REQUEST_ID_MULTIPLE_PERMISSIONS)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = Inspiration()
                    navigationView?.setCheckedItem(itemId)
                }
            }
            R.id.drawer_study -> {
                val listPermissionsNeeded = ArrayList<String>()
                val hasLocationPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, READ_CALENDAR)
                val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, WRITE_CALENDAR)

                if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(ACCESS_FINE_LOCATION)
                }
                if(hasReadCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(READ_CALENDAR)
                }
                if(hasWriteCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(WRITE_CALENDAR)
                }

                if (!listPermissionsNeeded.isEmpty()) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, READ_CALENDAR) || ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_CALENDAR)) {
                        val array = arrayOfNulls<String>(listPermissionsNeeded.size)
                        ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(array), REQUEST_ID_MULTIPLE_PERMISSIONS)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = Study()
                    navigationView?.setCheckedItem(itemId)
                }
            }
            R.id.drawer_social -> {
                val listPermissionsNeeded = ArrayList<String>()
                val hasLocationPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, READ_CALENDAR)
                val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, WRITE_CALENDAR)

                if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(ACCESS_FINE_LOCATION)
                }
                if(hasReadCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(READ_CALENDAR)
                }
                if(hasWriteCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(WRITE_CALENDAR)
                }

                if (!listPermissionsNeeded.isEmpty()) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, READ_CALENDAR) || ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_CALENDAR)) {
                        val array = arrayOfNulls<String>(listPermissionsNeeded.size)
                        ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(array), REQUEST_ID_MULTIPLE_PERMISSIONS)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = Social()
                    navigationView?.setCheckedItem(itemId)
                }
            }
            R.id.drawer_entertainment -> {
                val listPermissionsNeeded = ArrayList<String>()
                val hasLocationPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, READ_CALENDAR)
                val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, WRITE_CALENDAR)

                if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(ACCESS_FINE_LOCATION)
                }
                if(hasReadCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(READ_CALENDAR)
                }
                if(hasWriteCalendarPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(WRITE_CALENDAR)
                }

                if (!listPermissionsNeeded.isEmpty()) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, READ_CALENDAR) || ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_CALENDAR)) {
                        val array = arrayOfNulls<String>(listPermissionsNeeded.size)
                        ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(array), REQUEST_ID_MULTIPLE_PERMISSIONS)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = Amusement()
                    navigationView?.setCheckedItem(itemId)
                }
            }
            R.id.drawer_friends -> {
                val hasAcccessFineLocationtPermission = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

                if (hasAcccessFineLocationtPermission != PackageManager.PERMISSION_GRANTED) { //permission

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_CODE_ACCESS_FINE_LOCATION)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, "Snackbar onClick: Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.packageName, null)
                        Log.d(TAG, "Snackbar onClick: Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else { //check share location states
                    fragment = Friends()
                    navigationView?.setCheckedItem(itemId)
                }
            }
            else -> {
                fragment = Course(sub1?.findItem(itemId).toString())
                navigationView?.setCheckedItem(itemId)
            }
        }

        //replacing the fragment
        if (fragment != null) {
            hideKeyboard(this)
            val ft = getSupportFragmentManager().beginTransaction()
            ft.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
            ft.replace(R.id.content_frame, fragment)
            ft.commit()
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        for (fragment: Fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
        if (requestCode == REQUEST_GET_ACCOUNT_NAME && resultCode == RESULT_OK) {
            accountString = data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) // Account that selected by user
            mFirebaseDB!!.child("users").child(mUid.toString()).child("accountName").setValue(accountString)
            Log.d(TAG, "accountName: $accountString")
        }
    }

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val TAG = "MainActivity"

        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity.getCurrentFocus()
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //super.onSaveInstanceState(outState)   //to stop activity save state of fragment to prevent fragment overlay bug
    }

    fun displayShareLocationDialog(){
        var sw: Switch  = Switch(this)
        sw.textOn = "start"
        sw.textOff = "close"

        //load switch state
        val sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
        sw.isChecked = sharedPreferences.getBoolean("SwitchBtn", false)  //default is false
        //Toast.makeText(this, "switch state: " + sharedPreferences.getBoolean("SwitchBtn", false), Toast.LENGTH_LONG).show()

        var linearLayout: LinearLayout  = LinearLayout(this)
        linearLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        linearLayout.gravity = Gravity.CENTER_HORIZONTAL
        linearLayout.addView(sw)


        var myDialog: AlertDialog.Builder  = AlertDialog.Builder(this)
        myDialog.setTitle(getString(R.string.share_location))
        myDialog.setMessage(getString(R.string.share_location_details))
        myDialog.setView(linearLayout)

        myDialog.show()

        //save switch state
        sw.setOnClickListener {
            val sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(applicationContext)
            val editor = sharedPreferences.edit()
            editor.putBoolean("SwitchBtn", sw.isChecked)
            editor.commit()
        }
    }

    private fun get_account_name() {
        try {
            val intent = AccountPicker.newChooseAccountIntent(null, null,
                    arrayOf( GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE ), false, null, null, null, null)
            startActivityForResult(intent, REQUEST_GET_ACCOUNT_NAME )
        } catch (e: ActivityNotFoundException) {

        }
    }

}
