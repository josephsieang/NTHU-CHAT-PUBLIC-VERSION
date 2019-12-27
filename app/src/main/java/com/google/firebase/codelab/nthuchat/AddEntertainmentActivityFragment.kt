package com.google.firebase.codelab.nthuchat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.fragment_add_entertainment_activity.*
import java.util.*

private const val TAG = "AddEntertainment"

class AddEntertainmentActivityFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mGoogleMap: GoogleMap? = null
    private var mMapView: MapView? = null
    private var lat: String? = null
    private var long: String? = null
    private var latLng: LatLng? = null
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var doneBtn: MenuItem? = null
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private var mUid: String? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var additionalDetailsEt: EditText? = null
    private var activityNameEt: EditText? = null
    private var setDate = false
    private var setGPS = false
    private var startDateFirebase: String? = null
    private var endDateFirebase: String? = null
    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private var countLengthActivityName: Long = 0
    private var countLengthActivityDescription: Long = 0
    private lateinit var mSharedPreferences: SharedPreferences
    private val DEFAULT_ACTIVITY_NAME_LENGTH_LIMIT = 30
    private val DEFAULT_ACTIVITY_DESCRIPTION_LENGTH_LIMIT = 100
    private lateinit var countLabelActivityName: TextView
    private lateinit var countLabelActivityDescription: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //you can set the title for your toolbar here for different fragments different titles
        activity!!.title = getString(R.string.addEntertainment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val contentView =  inflater.inflate(R.layout.fragment_add_entertainment_activity, container, false)

        countLabelActivityName = contentView.findViewById(R.id.countLabelActivityName)
        countLabelActivityDescription = contentView.findViewById(R.id.countLabelActivityDescription)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        additionalDetailsEt = contentView.findViewById(R.id.additionalDetailsEt)
        activityNameEt = contentView.findViewById(R.id.activityNameEt)

        additionalDetailsEt!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences().ACTIVITY_DESCRIPTION_LENGTH, DEFAULT_ACTIVITY_DESCRIPTION_LENGTH_LIMIT)))
        additionalDetailsEt!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.toString().trim { it <= ' ' }.isNotEmpty()) {
                    //Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
                    val current_length = charSequence.toString().trim { it <= ' ' }.length
                    countLabelActivityDescription.text = "(" + current_length.toString() + "/" + countLengthActivityDescription + ")"
                } else {
                    //Toast.makeText(MainActivity.this, "false", Toast.LENGTH_SHORT).show();
                    countLabelActivityDescription.text = "(" + "0/$countLengthActivityDescription" + ")"
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        activityNameEt!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences().ACTIVITY_NAME_LENGTH, DEFAULT_ACTIVITY_NAME_LENGTH_LIMIT)))
        activityNameEt!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.toString().trim { it <= ' ' }.isNotEmpty()) {
                    //Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
                    val current_length = charSequence.toString().trim { it <= ' ' }.length
                    countLabelActivityName.text = "(" + current_length.toString() + "/" + countLengthActivityName + ")"
                } else {
                    //Toast.makeText(MainActivity.this, "false", Toast.LENGTH_SHORT).show();
                    countLabelActivityName.text = "(" + "0/$countLengthActivityName" + ")"
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        //hide keyboard when touch view
        contentView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                //do something
                val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, 0)
                contentView.isFocusable = false
                contentView.isFocusableInTouchMode = false
                contentView.isFocusable = true
                contentView.isFocusableInTouchMode = true
            }
            true
        }

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN) //move layout up when keyboard appear

        mMapView = contentView.findViewById(R.id.mapViewAddEntertainmentActivity)
        mMapView!!.onCreate(savedInstanceState)
        mMapView!!.onCreate(savedInstanceState)
        mMapView!!.onResume()

        val gpsLongLat = contentView.findViewById<Button>(R.id.gpsLongLat)
        gpsLongLat.setOnClickListener {
            contentView!!.clearFocus()
            val myIintent: Intent = Intent(activity, AddActivityMap::class.java)
            startActivityForResult(myIintent, 999)
        }

        val endTime = contentView.findViewById<Button>(R.id.endTime)
        endTime.setOnClickListener {
            contentView!!.clearFocus()
            SingleDateAndTimePickerDialog.Builder(context)
                    .title(getString(R.string.activityStartTime))
                    .listener { date ->
                        Log.d(TAG, "selected date: $date")
                        endDate = date
                        if(endDate!! <= startDate) { //detect endDate must after startDate
                            Log.d(TAG, "endDate!! <= startDate!!!")
                            var builder: AlertDialog.Builder  = AlertDialog.Builder(getActivity()!!)
                            builder.setTitle(getString(R.string.error))
                            builder.setMessage(getString(R.string.dateError))
                            builder.setPositiveButton("Ok") { dialog, which ->
                                Log.d(TAG, "OK")
                            }
                            val dialog = builder.create()
                            dialog.show()
                        } else {
                            val result = date.toString().split(" ").map { it.trim() }
                            val months = arrayListOf<String>("Jan", "Feb", "Mar", "Apr","May", "Jun", "Jul", "Aug", "Sep","Oct", "Nov", "Dec")
                            endDateFirebase = result[5] + "-" + (months.indexOf(result[1]) + 1) + "-" + result[2] + " " + result[3]
                            Log.d(TAG, "endDate: $endDateFirebase")

                            val endDateBtn = result[5] + "/" + (months.indexOf(result[1]) + 1) + "/" + result[2] + ", " + result[3]
                            endTime.text = endDateBtn
                        }
                    }.display()
        }

        val startTime = contentView.findViewById<Button>(R.id.startTime)
        startTime.setOnClickListener {
            contentView!!.clearFocus()
            SingleDateAndTimePickerDialog.Builder(context)
                    .title(getString(R.string.activityStartTime))
                    .listener { date ->
                        Log.d(TAG, "selected date: $date")
                        startDate = date


                        val result = date.toString().split(" ").map { it.trim() }
                        val months = arrayListOf<String>("Jan", "Feb", "Mar", "Apr","May", "Jun", "Jul", "Aug", "Sep","Oct", "Nov", "Dec")
                        startDateFirebase = result[5] + "-" + (months.indexOf(result[1]) + 1) + "-" + result[2] + " " + result[3]
                        Log.d(TAG, "startDate: $startDateFirebase")

                        var endHour = String.format("%2d", date.hours).replace(" ", "0")
                        var endMin = String.format("%2d", date.minutes).replace(" ", "0")
                        var endSec = (date.seconds + 10).toString()

                        val startDateBtn = result[5] + "/" + (months.indexOf(result[1]) + 1) + "/" + result[2] + ", " + result[3]
                        val endDateBtn = result[5] + "/" + (months.indexOf(result[1]) + 1) + "/" + result[2] + ", " + endHour + ":" + endMin + ":" + endSec
                        endDateFirebase = result[5] + "-" + (months.indexOf(result[1]) + 1) + "-" + result[2]  + " " +  endHour + ":" + endMin + ":" + endSec

                        startTime.text = startDateBtn
                        endTime.text = endDateBtn
                        setDate = true
                    }.display()
        }

        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        // Define Firebase Remote Config Settings.
        val firebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build()

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap["activity_name_length"] = 10L

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings)
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap)

        // Fetch remote config.
        fetchConfig()

        return contentView
    }

    // Fetch the config to determine the allowed length of messages.
    fun fetchConfig() {
        var cacheExpiration: Long = 3600 // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that
        // each fetch goes to the server. This should not be used in release
        // builds.
        if (mFirebaseRemoteConfig.info.configSettings
                        .isDeveloperModeEnabled) {
            cacheExpiration = 0
            Log.d(TAG, "mFirebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled")
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener {
                    // Make the fetched config available via
                    // FirebaseRemoteConfig get<type> calls.
                    mFirebaseRemoteConfig.activateFetched()
                    applyRetrievedLengthLimit()
                }
                .addOnFailureListener { e ->
                    // There has been an error fetching the config
                    Log.w(TAG, "Error fetching config: " + e.message)
                    applyRetrievedLengthLimit()
                }
    }

    private fun applyRetrievedLengthLimit() {
        val activity_name_length = mFirebaseRemoteConfig.getLong("activity_name_length")
        activityNameEt!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(activity_name_length.toInt()))
        countLengthActivityName = activity_name_length
        Log.d(TAG, "activity_name_length is: $activity_name_length")

        val activity_description_length = mFirebaseRemoteConfig.getLong("activity_description_length")
        additionalDetailsEt!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(activity_description_length.toInt()))
        countLengthActivityDescription = activity_description_length
        Log.d(TAG, "activity_description_length is: $activity_description_length")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGoogleApiClient = GoogleApiClient.Builder(activity!!)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        setHasOptionsMenu(true)
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d(TAG, "onConnectionSuspended")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed")
    }

    override fun onLocationChanged(location: Location) {
        //handleNewLocation(location)

        Log.d(TAG, "onLocationChanged")
    }

    override fun onResume() {
        super.onResume()
        mGoogleApiClient!!.connect()
        mMapView!!.onResume()
        setUpMap()

        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        if (mGoogleApiClient!!.isConnected)
            mGoogleApiClient!!.disconnect()
        mMapView!!.onPause()

        Log.d(TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView!!.onDestroy()
        if (mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.disconnect();
        }

        Log.d(TAG, "onDestroy")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView!!.onLowMemory()

        Log.d(TAG, "onLowMemory")
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            return

        var nthuLat = 24.794657
        var nthuLong = 120.993240

        if(lat != null) {
            mGoogleMap!!.clear()
            val options = MarkerOptions()
                    .position(latLng!!)
            mGoogleMap!!.addMarker(options)

            mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5F))
        } else {
            val latLng = LatLng(nthuLat, nthuLong)

            val options = MarkerOptions()
                    .position(latLng)
            mGoogleMap!!.addMarker(options)

            mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5F))
        }



        Log.d(TAG, "onConnected")
    }


    private fun setUpMap() {
        if (mGoogleMap == null) {
            mMapView!!.getMapAsync(OnMapReadyCallback { googleMap -> mGoogleMap = googleMap })
        }

    }


    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity!!, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var extras: Bundle = data!!.extras;
        var result =   extras.getString("lat").split(",").map { it.trim() }

        lat = result[0]
        long = result[1]

        var gpsBtn = view!!.findViewById<Button>(R.id.gpsLongLat)
        gpsBtn.text = extras.getString("lat")

        latLng = LatLng(lat!!.toDouble(), long!!.toDouble())
        setGPS = true


        Log.d(TAG, "lat: $lat, long: $long")
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        doneBtn = menu.findItem(R.id.doneBtn)
        doneBtn!!.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.getItemId()) {
            R.id.doneBtn -> {
                if(activityNameEt!!.text.toString() != "" && setDate && setGPS) {
                    //save data to db
                    mFirebaseAuth = FirebaseAuth.getInstance()
                    mFirebaseUser = mFirebaseAuth!!.currentUser
                    val creator = mFirebaseUser!!.uid
                    val description = additionalDetailsEt!!.text.toString()
                    val enddate = endDateFirebase.toString()
                    val location = gpsLongLat.text.toString()
                    val startdate = startDateFirebase.toString()
                    val title = activityNameEt!!.text.toString()
                    val creatorName = mFirebaseUser!!.displayName.toString()

                    //val activityMessages = ActivityMessages(creator, description, enddate, location, startdate, title)
                    var activityMessagesObject = HashMap<String, String>()
                    activityMessagesObject.put("creator", creator)
                    activityMessagesObject.put("description", description)
                    activityMessagesObject.put("enddate", enddate)
                    activityMessagesObject.put("location", location)
                    activityMessagesObject.put("startdate", startdate)
                    activityMessagesObject.put("title", title)
                    activityMessagesObject.put("creatorName", creatorName)
                    mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference.child("activity").child("amusement")
                    mFirebaseDatabaseReference.push().setValue(activityMessagesObject)

                    doneBtn!!.isVisible = false
                    navigationView!!.setCheckedItem(R.id.school)
                    val transaction = fragmentManager!!.beginTransaction()
                    transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                    transaction.replace(R.id.content_frame, Schoolchat())
                    transaction.commit()

                } else {
                    Log.d(TAG, "Please fill in information")
                    var builder: AlertDialog.Builder  = AlertDialog.Builder(activity!!)
                    builder.setTitle(getString(R.string.error))
                    builder.setMessage(getString(R.string.formError))
                    builder.setPositiveButton("Ok") { dialog, which ->
                        Log.d(TAG, "OK")
                    }
                    val dialog = builder.create()
                    dialog.show()
                }
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

}
