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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

private const val TAG = "showFriendsMap"

class showFriendsMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var backBtn: ImageButton? = null
    private var mFirebaseDatabaseRef: DatabaseReference? = null
    private var mFirebaseDatabaseRefFriend: DatabaseReference? = null
    private var mFirebaseDatabaseRefFriends: DatabaseReference? = null
    private var mFirebaseUser: FirebaseUser? = null
    private lateinit var mFirebaseAuth: FirebaseAuth
    private var mUid: String? = null
    private var activityTitle: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friends_show_map)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapAddFriends) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //set back custom back button to left
        showActionBar()

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth.currentUser
        mUid = mFirebaseUser!!.uid
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
        activityTitle!!.text = getString(R.string.friendMap)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().reference
        //show friends
        mFirebaseDatabaseRefFriends = mFirebaseDatabaseRef!!.child("users").child(mUid.toString()).child("friends")
        mFirebaseDatabaseRefFriends!!.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(friendSnapshot in dataSnapshot.children) {

                    val friendKey = friendSnapshot.key
                    var gpsLoc = ""
                    var name = ""
                    mFirebaseDatabaseRefFriend = FirebaseDatabase.getInstance().reference.child("users").child(friendKey.toString())
                    mFirebaseDatabaseRefFriend!!.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {

                        }

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for(childSnapshot in dataSnapshot.children) {
                                if(childSnapshot.key == "gps") {
                                    gpsLoc = childSnapshot.value.toString()
                                    Log.d(TAG, "childSnapshot.value.toString(): ${childSnapshot.value.toString()}")
                                }
                                if(childSnapshot.key == "name") {
                                    name = childSnapshot.value.toString()
                                }
                            }
                            val gpsArray = gpsLoc!!.split(",").map { it.trim() }
                            var lat = 0.0
                            var long = 0.0
                            if(gpsArray[0] == "") {
                                lat = 24.794657
                                long = 120.993240
                            } else {
                                lat = gpsArray[0].toDouble()
                                long = gpsArray[1].toDouble()
                            }
                            var marker = mMap.addMarker(MarkerOptions().position(LatLng(lat, long)).title(name))
                            marker.tag = friendKey
                            mMap.setOnInfoWindowClickListener {
                                if(it.tag != mUid.toString()) {
                                    val intent = Intent(baseContext, PrivateChatActivity::class.java)
                                    val friendUid = it.tag
                                    Log.d(TAG, "friendUid: $friendUid")
                                    intent.putExtra("friendUid", friendUid.toString())
                                    startActivity(intent)
                                }
                            }
                            Log.d(TAG, "one user end")
                        }
                    })
                }
                val nthu = LatLng(24.794657, 120.993240)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nthu, 13.5F))

            }
        })

        //show own
        mFirebaseDatabaseRef!!.child("users").child(mUid.toString()).addListenerForSingleValueEvent(object: ValueEventListener {
            var gpsLoc = ""
            var name = ""
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ownSnapshot in dataSnapshot.children) {
                    if (ownSnapshot.key == "gps") {
                        gpsLoc = ownSnapshot.value.toString()
                        Log.d(TAG, "childSnapshot.value.toString(): ${ownSnapshot.value.toString()}")
                    }
                    if (ownSnapshot.key == "name") {
                        name = ownSnapshot.value.toString()
                    }
                }
                val gpsArray = gpsLoc!!.split(",").map { it.trim() }
                val lat = gpsArray[0].toDouble()
                val long = gpsArray[1].toDouble()
                val marker = mMap.addMarker(MarkerOptions().position(LatLng(lat, long)).title(name).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
                marker.tag = mUid
            }
        })

    }
}
