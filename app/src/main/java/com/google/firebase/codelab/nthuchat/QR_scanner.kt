package com.google.firebase.codelab.nthuchat



import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import java.util.regex.Matcher
import java.util.regex.Pattern
import android.content.DialogInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "QRSCANNER"

class QR_scanner : Fragment() {

    private lateinit var codeScanner: CodeScanner
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private var mUid: String? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var contentView = inflater.inflate(R.layout.fragment_qr_scanner, container, false)

        return contentView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scannerView = view.findViewById<CodeScannerView>(R.id.scanner_view)
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, scannerView)
        codeScanner.decodeCallback = DecodeCallback {
            activity.runOnUiThread {
//                Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
                val input = it.text.trim()
                val pattern: Pattern = Pattern.compile("[a-zA-Z0-9]*")
                val matcher: Matcher = pattern.matcher(input)

                if(input.isBlank()) {
                    Log.d(TAG, "isBlank")
                    codeScanner.startPreview()
                } else if(!matcher.matches()) {
                    Log.d(TAG, "contains special character")
                    codeScanner.startPreview()
                } else {
                    //save to firebase
                    mFirebaseAuth = FirebaseAuth.getInstance()
                    mFirebaseUser = mFirebaseAuth!!.currentUser
                    mUid = mFirebaseUser!!.uid
                    Log.d(TAG, "mUid: $mUid")

                    mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference
                    val friendNameRef = mFirebaseDatabaseReference.child("users").child(input).child("name")
                    val ownNameRef = mFirebaseDatabaseReference.child("users").child(mUid.toString()).child("name")
                    val friendRef = mFirebaseDatabaseReference.child("users").child(input).child("friends")
                    val ownRef = mFirebaseDatabaseReference.child("users").child(mUid.toString()).child("friends")

                    //add both uid to both account to be friends
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val currentDate = sdf.format(Date())
                    var friendName: String? = null
                    var ownName: String? = null
                    Log.d(TAG, "currentDate: $currentDate")

                    ownRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.hasChild(input)) {
                                Log.d(TAG, "input: $input")
                                var builder: AlertDialog.Builder  = AlertDialog.Builder(getActivity()!!)
                                builder.setTitle(getString(R.string.addFriend))
                                builder.setMessage(getString(R.string.addFriendDuplicate))
                                builder.setPositiveButton("Ok") { dialog, which ->
                                    Log.d(TAG, "OK")
                                    codeScanner.startPreview()
                                    var transaction = fragmentManager!!.beginTransaction()
                                    transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                                    transaction.replace(R.id.content_frame, Schoolchat())
                                    transaction.commit()
                                }
                                val dialog = builder.create()
                                dialog.show()
                            } else {
                                ownNameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        ownName = snapshot.value.toString()
                                        friendRef.child(mUid.toString()).child("addedTime").setValue(currentDate)
                                        friendRef.child(mUid.toString()).child("name").setValue(ownName)
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {}
                                })

                                Log.d(TAG, "input: $input")
                                var builder: AlertDialog.Builder  = AlertDialog.Builder(getActivity()!!)
                                builder.setTitle(getString(R.string.addFriend))
                                builder.setMessage(getString(R.string.addFriendSuccess))
                                builder.setPositiveButton("Ok") { dialog, which ->
                                    Log.d(TAG, "OK")
                                    codeScanner.startPreview()
                                    var transaction = fragmentManager!!.beginTransaction()
                                    transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                                    transaction.replace(R.id.content_frame, Schoolchat())
                                    transaction.commit()
                                }
                                val dialog = builder.create()
                                dialog.show()

                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })

                    friendRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.hasChild(mUid.toString())) {
                                return
                            } else {
                                friendNameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        friendName = snapshot.value.toString()
                                        ownRef.child(input).child("addedTime").setValue(currentDate)
                                        ownRef.child(input).child("name").setValue(friendName)
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {}
                                })
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })







//                    scannerView.setOnClickListener {
//
//                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }


}
