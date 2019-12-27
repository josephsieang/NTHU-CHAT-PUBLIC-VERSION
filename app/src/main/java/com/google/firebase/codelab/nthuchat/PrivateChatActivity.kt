package com.google.firebase.codelab.nthuchat

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

private const val TAG = "PrivateChatActivity"

class PrivateChatActivity : AppCompatActivity() {

    private var backBtn: ImageButton? = null
    private var activityTitle: TextView? = null
    private var friendUid: String? = null
    private lateinit var mFirebaseDatabaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_chat)

        friendUid = intent.getStringExtra("friendUid")
        Log.d(TAG, "friendUid: $friendUid")

        showActionBar()

        val fragment = PrivateChat()
        val args = Bundle()
        args.putString("friendUid", friendUid)
        fragment.arguments = args
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.chat_frame, fragment)
        ft.commit()

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

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference.child("users").child(friendUid.toString())
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(childSnapshot in dataSnapshot.children) {
                    Log.d(TAG, "childSnapshot.key: ${childSnapshot.key}")
                    if(childSnapshot.key == "name") {
                        val friendName = childSnapshot.value.toString()
                        val title = getString(R.string.Private) + " - " + friendName
                        activityTitle = v.findViewById(R.id.activityTitle)
                        activityTitle!!.text = title
                    }
                }
            }
        })

    }
}
