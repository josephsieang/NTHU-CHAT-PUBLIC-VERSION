package com.google.firebase.codelab.nthuchat

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

private const val TAG = "infoActivityInspiration"

class InfoActivityInspiration : AppCompatActivity(), OnMapReadyCallback {

    private var mGoogleMap: GoogleMap? = null
    private var infoNameFromDB: TextView? = null
    private var infoStartDateFromDB: TextView? = null
    private var infoEndDateFromDB: TextView? = null
    private var infoCreatorFromDB: TextView? = null
    private var infoAdditionalDetailsFromDB: TextView? = null
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private var activityUid: String? = null
    private var backBtn: ImageButton? = null
    private var activityTitle: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_inspiration)

        showActionBar()

        title = getString(R.string.activityInfo)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.infoMapActivity) as SupportMapFragment
        mapFragment.getMapAsync(this)

        activityUid = intent.getStringExtra("activityUid")
        Log.d(TAG, "activityUid: $activityUid")

        infoNameFromDB = findViewById(R.id.infoNameFromDB)
        infoStartDateFromDB = findViewById(R.id.infoStartDateFromDB)
        infoEndDateFromDB = findViewById(R.id.infoEndDateFromDB)
        infoCreatorFromDB = findViewById(R.id.infoCreatorFromDB)
        infoAdditionalDetailsFromDB = findViewById(R.id.infoAdditionalDetailsFromDB)


        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference.child("activity").child("innovation").child(activityUid.toString())
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val activityMessage = dataSnapshot.getValue(ActivityMessages::class.java)
                infoNameFromDB!!.text =  activityMessage!!.title
                infoStartDateFromDB!!.text= activityMessage.startdate
                infoEndDateFromDB!!.text= activityMessage.enddate
                infoCreatorFromDB!!.text= activityMessage.creatorName
                infoAdditionalDetailsFromDB!!.text= activityMessage.description
                val gpsLoc = activityMessage.location

                val gpsArray = gpsLoc!!.split(",").map { it.trim() }
                val lat = gpsArray[0]
                val long = gpsArray[1]
                val latLng = LatLng(lat.toDouble(), long.toDouble())

                val options = MarkerOptions()
                        .position(latLng)
                mGoogleMap!!.addMarker(options)

                mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5F))
            }
        })

        infoAdditionalDetailsFromDB!!.movementMethod = ScrollingMovementMethod()
    }

    private fun showActionBar() {
        val inflator = this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflator.inflate(R.layout.custom_action_bar, null)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(false)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.customView = v
        backBtn = v.findViewById(R.id.backBtn)
        backBtn!!.setOnClickListener {
            finish()
        }
        activityTitle = v.findViewById(R.id.activityTitle)
        activityTitle!!.text = getString(R.string.activityInfo)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
    }
}
