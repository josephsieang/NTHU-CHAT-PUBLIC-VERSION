package com.google.firebase.codelab.nthuchat

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "showAmusementMap"

class showAmusementActivityMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var backBtn: ImageButton? = null
    private var mFirebaseDatabaseRef: DatabaseReference? = null
    private var activityTitle: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_map)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapShowActivity) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //set back custom back button to left
        showActionBar()
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
        activityTitle!!.text = getString(R.string.entertainmentMap)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
//        val nthu = LatLng(24.794657, 120.993240)
//        mMap.addMarker(MarkerOptions().position(nthu).title(getString(R.string.drag)).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(nthu))
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nthu, 16.5F))

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().reference.child("activity").child("amusement")
        mFirebaseDatabaseRef!!.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(childSnapshot in dataSnapshot.children) {
                    val activityMessage = childSnapshot.getValue(ActivityMessages::class.java)
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val dateFromString = format.parse(activityMessage!!.enddate)
                    if(dateFromString > Calendar.getInstance().time) { //load activity that not expired
                        Log.d(TAG, "activity title: ${activityMessage!!.title}")
                        val gpsDB = activityMessage.location
                        val gpsArray = gpsDB!!.split(",").map { it.trim() }
                        val lat = gpsArray[0].toDouble()
                        val long = gpsArray[1].toDouble()
                        var marker = mMap.addMarker(MarkerOptions().position(LatLng(lat, long)).title(activityMessage.title))
                        marker.tag = childSnapshot.key
                        mMap.setOnInfoWindowClickListener {
                            val intent = Intent(baseContext, InfoActivityAmusement::class.java)
                            val activityUid = it.tag
                            Log.d(TAG, "activityUid: $activityUid")
                            intent.putExtra("activityUid", activityUid.toString())
                            startActivity(intent)
                        }
                    }
                }
                val nthu = LatLng(24.794657, 120.993240)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nthu, 15.0F))

            }
        })
    }
}
