package com.google.firebase.codelab.nthuchat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings


import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class Schoolchat : Fragment(), GoogleApiClient.OnConnectionFailedListener {

    inner class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var messageTextView: TextView
        internal var messengerTextView: TextView
        internal var messengerImageView: CircleImageView
        internal var view: View

        init {
            messageTextView = itemView.findViewById<View>(R.id.messageTextView) as TextView
            messengerTextView = itemView.findViewById<View>(R.id.messengerTextView) as TextView
            messengerImageView = itemView.findViewById<View>(R.id.messengerImageView) as CircleImageView
            view = itemView
        }

        fun setOnItemClick(l: View.OnClickListener) {
            this.view.setOnClickListener(l)
        }
    }

//    var mUsername: String? = null
//    var mPhotoUrl: String
//    var mUid: String

    companion object {

        private val TAG = "Schoolchat"
        private val MESSAGES_CHILD = "messages"
        private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
        val DEFAULT_MSG_LENGTH_LIMIT = 10
        val ANONYMOUS = "anonymous"
    }

    private lateinit var mSharedPreferences: SharedPreferences
    private val mGoogleApiClient: GoogleApiClient? = null

    private lateinit var mSendButton: FloatingActionButton
    private lateinit var mMessageRecyclerView: RecyclerView
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mMessageEditText: EditText

    // Firebase instance variables
    private var mFirebaseUser: FirebaseUser? = null
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<FriendlyMessage, Schoolchat.MessageViewHolder>
    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val mAdView: AdView? = null
    private lateinit var countLabel: TextView
    private var countlength: Long = 0

    //pagination variable
    private var friendlyMessageList: MutableList<FriendlyMessage> = ArrayList()
    private val TOTAL_ITEM_TO_LOAD = 15
    private var mRefreshLayout: SwipeRefreshLayout?= null
    private var mCurrentPage = 1
    private var itemPos = 0
    private var mLastKey = ""
    private var mPrevKey = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
        //inflate your activity layout here!
        @SuppressLint("InflateParams")
        val contentView = inflater.inflate(R.layout.activity_schoolchat, container, false)

        //MobileAds.initialize(getActivity(), "ca-app-pub-3589269405021012~8631287778");

        mRefreshLayout = contentView.findViewById(R.id.messageSwipeLayout) as SwipeRefreshLayout
//        getLastKey()


        countLabel = contentView.findViewById(R.id.countLabel)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        // Set default username is anonymous.
        var mUsername: String = "ANONYMOUS"

        lateinit var mPhotoUrl: String
        lateinit var mUid: String

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth.currentUser

        if (mFirebaseUser != null) {
            if (mFirebaseUser?.photoUrl != null) {
                mPhotoUrl = mFirebaseUser?.photoUrl.toString()
                mUsername = mFirebaseUser?.displayName as String
                mUid = mFirebaseUser?.uid as String
            }
        } else {
            val picnum = Math.round(Math.random() * 12 + 1).toInt()
            val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName("機器人404號")
                    .setPhotoUri(Uri.parse("../images/user$picnum.jpg")).build()
            mFirebaseUser!!.updateProfile(profileUpdate)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            //Toast.makeText(MainActivity.this, "1Update AC Success", Toast.LENGTH_SHORT).show();
                            mUsername = mFirebaseUser?.displayName as String
                            mUid = mFirebaseUser?.uid as String
                            mPhotoUrl = mFirebaseUser?.photoUrl.toString()
                            //Toast.makeText(MainActivity.this, "1.5Displayname: "+mUsername, Toast.LENGTH_SHORT).show();
                            //Toast.makeText(MainActivity.this, "1.5PhotoUrl: "+mPhotoUrl, Toast.LENGTH_SHORT).show();
                        }
                    }
        }

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = contentView.findViewById<View>(R.id.progressBar) as ProgressBar
        mMessageRecyclerView = contentView.findViewById<View>(R.id.messageRecyclerView) as RecyclerView
        mLinearLayoutManager = LinearLayoutManager(activity)
        mLinearLayoutManager.stackFromEnd = true
//        mMessageRecyclerView.setHasFixedSize(true)
        mMessageRecyclerView.layoutManager = mLinearLayoutManager

        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser<FriendlyMessage> { dataSnapshot ->
            val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
            friendlyMessage?.setId(dataSnapshot.key!!)
            friendlyMessage as FriendlyMessage
        }

        val messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD)
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                .setQuery(messagesRef, parser)
                .build()
        mFirebaseAdapter = object : FirebaseRecyclerAdapter<FriendlyMessage, Schoolchat.MessageViewHolder>(options) {

            override fun getItemCount(): Int {
                return friendlyMessageList.size
            }

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): Schoolchat.MessageViewHolder {
                val inflaterLocal = LayoutInflater.from(viewGroup.context)
                when (i) {
                    0 -> return Schoolchat().MessageViewHolder(inflaterLocal.inflate(R.layout.item_message, viewGroup, false))
                    1 -> return Schoolchat().MessageViewHolder(inflaterLocal.inflate(R.layout.item_message_me, viewGroup, false))
                    else -> return Schoolchat().MessageViewHolder(inflaterLocal.inflate(R.layout.item_message, viewGroup, false))
                }
            }

            override fun getItemViewType(position: Int): Int {
                return if (friendlyMessageList.get(position) != null && friendlyMessageList.get(position).getUid() != null) {
                    if (friendlyMessageList.get(position).getUid() == mUid) {
                        1
                    } else {
                        0
                    }
                } else 0
            }

            override fun onBindViewHolder(viewHolder: Schoolchat.MessageViewHolder,
                                          position: Int,
                                          friendlyMessage: FriendlyMessage) {
                when (viewHolder.itemViewType) {
                    0 -> {
                        mProgressBar.visibility = ProgressBar.INVISIBLE
                        if (friendlyMessageList.get(position).getText() != null) {
                            viewHolder.messageTextView.text = friendlyMessageList.get(position).getText()
                            viewHolder.messageTextView.visibility = TextView.VISIBLE
                        }
                        viewHolder.messengerTextView.text = friendlyMessageList.get(position).getName()
                        if (friendlyMessageList.get(position).getPhotoUrl() == null) {
                            viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(activity!!,
                                    R.drawable.ic_account_circle_black_36dp))
                        } else {
                            Glide.with(this@Schoolchat)
                                    .load(friendlyMessageList.get(position).getPhotoUrl())
                                    .into(viewHolder.messengerImageView)
                        }
                    }
                    1 -> {
                        mProgressBar.visibility = ProgressBar.INVISIBLE
                        if (friendlyMessageList.get(position).getText() != null) {
                            viewHolder.messageTextView.text = friendlyMessageList.get(position).getText()
                            viewHolder.messageTextView.visibility = TextView.VISIBLE
                        }
                        viewHolder.messengerTextView.text = friendlyMessageList.get(position).getName()
                        if (friendlyMessageList.get(position).getPhotoUrl() == null) {
                            viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(activity!!,
                                    R.drawable.ic_account_circle_black_36dp))
                        } else {
                            Glide.with(this@Schoolchat)
                                    .load(friendlyMessageList.get(position).getPhotoUrl())
                                    .into(viewHolder.messengerImageView)
                        }
                    }
                }
                viewHolder.setOnItemClick(View.OnClickListener {
                    //設定你點擊每個Item後，要做的事情
                    val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, 0)
                })
            }
        }

//        mFirebaseAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                super.onItemRangeInserted(positionStart, itemCount)
//                val friendlyMessageCount = mFirebaseAdapter.itemCount
//                val lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition()
//                // If the recycler view is initially being loaded or the
//                // user is at the bottom of the list, scroll to the bottom
//                // of the list to show the newly added message.
//                if (lastVisiblePosition == -1 || positionStart >= friendlyMessageCount - 1 && lastVisiblePosition == positionStart - 1) {
//                    mMessageRecyclerView.scrollToPosition(positionStart)
//                }
//                Log.d(TAG, "positionStart: $positionStart, friendlyMessageCount - 1: ${friendlyMessageCount - 1}")
//            }
//        })

        mMessageRecyclerView.adapter = mFirebaseAdapter
        mFirebaseAdapter.startListening() // put at here to fix crash bug

        loadMessages()

        mRefreshLayout!!.setOnRefreshListener {
            mCurrentPage++
            itemPos = 0
            loadMoreMessages()
        }

//        getMessages()
//
//        mMessageRecyclerView!!.addOnScrollListener(object: RecyclerView.OnScrollListener(){
//            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                total_item = mLinearLayoutManager!!.itemCount
//                last_visible = mLinearLayoutManager!!.findLastVisibleItemPosition()
//                Log.d(TAG, "total_item: ${total_item}, last_visible: $last_visible")
//
//                if(!isLoading && total_item <= last_visible + ITEM_COUNT) {
//                    getMessages()
//                    isLoading = true
//                    Log.d(TAG, "if(!isLoading && total_item <= last_visible + ITEM_COUNT)")
//                }
//            }
//
//        })

        mMessageEditText = contentView.findViewById<View>(R.id.messageEditText) as EditText
        mMessageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences().FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT)))
        mMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.toString().trim { it <= ' ' }.length > 0) {
                    //Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
                    val current_length = charSequence.toString().trim { it <= ' ' }.length
                    countLabel.text = current_length.toString() + "/" + countlength
                    mSendButton.isEnabled = true
                } else {
                    //Toast.makeText(MainActivity.this, "false", Toast.LENGTH_SHORT).show();
                    countLabel.text = "0/$countlength"
                    mSendButton.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        mSendButton = contentView.findViewById<View>(R.id.sendButton) as FloatingActionButton
        mSendButton.setOnClickListener {
            mUsername = mFirebaseUser?.getDisplayName() as String
            val friendlyMessage = FriendlyMessage(mMessageEditText.text.toString(), mUsername, mPhotoUrl, mUid)
            friendlyMessage.setUid(mUid)
            friendlyMessage.setPhotoUrl(mPhotoUrl)
            mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                    .push().setValue(friendlyMessage)
            mMessageEditText.setText("")

//            isMaxData = false
//            last_node = last_key
//
//            //fun removeLastItem
//            friendlyMessageList.removeAt(0)
//
//            mFirebaseAdapter!!.notifyDataSetChanged()
//            getLastKey()
//            getMessages()
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
        defaultConfigMap["friendly_msg_length"] = 10L

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings)
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap)

        // Fetch remote config.
        fetchConfig()

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity!!)

        //mAdView = (AdView) contentView.findViewById(R.id.adView);
        //AdRequest adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);
        return contentView
    }

    private fun loadMessages() {
        val messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD)
        val query: Query = messagesRef.limitToLast(mCurrentPage * TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(FriendlyMessage::class.java)

                itemPos++

                if (itemPos == 1) {
                    val messageKey = dataSnapshot.key
                    mLastKey = messageKey!!
                    mPrevKey = messageKey

                }
                Log.d(TAG, "messageKey: ${dataSnapshot.key}")

                friendlyMessageList.add(message!!)
                mFirebaseAdapter.notifyDataSetChanged()

                mMessageRecyclerView.scrollToPosition(friendlyMessageList.size - 1)

                mRefreshLayout!!.isRefreshing = false
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
        })
    }

    private fun loadMoreMessages() {
        val messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD)
        val query: Query = messagesRef.orderByKey().endAt(mLastKey).limitToLast(TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(FriendlyMessage::class.java)
                Log.d(TAG, "dataSnapshot.key: ${dataSnapshot.key}")
                val messageKey = dataSnapshot.key

                if(mPrevKey != messageKey){
                    friendlyMessageList.add(itemPos++, message!!)
                } else {
                    mPrevKey = mLastKey
                }

                if(itemPos == 1) {
                    mLastKey = messageKey!!
                    Log.d(TAG, "mPrevKey: $mPrevKey")
                    Log.d(TAG, "mLastKey: $mLastKey")
                }
                Log.d(TAG, "itemPos: $itemPos")

                val init = friendlyMessageList.size
                mFirebaseAdapter.notifyDataSetChanged()
//                mFirebaseAdapter.notifyItemRangeChanged(init, friendlyMessageList.size)

                mRefreshLayout!!.isRefreshing = false
                mLinearLayoutManager.scrollToPositionWithOffset(itemPos - 3, 0)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
        })
    }

//    private fun getLastKey() {
//        val get_last_ley = FirebaseDatabase.getInstance().reference
//                .child(MESSAGES_CHILD)
//                .orderByKey()
//                .limitToLast(1)
//
//        get_last_ley.addListenerForSingleValueEvent(object: ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {
//
//            }
//
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                for(messageSnapshot in dataSnapshot.children) {
//                    last_key = messageSnapshot.key
//                }
//            }
//        })
//    }
//
//    private fun getMessages() {
//        if(!isMaxData) {
//            val query: Query
//            if(TextUtils.isEmpty(last_node)) {
//                query = FirebaseDatabase.getInstance().reference
//                        .child(MESSAGES_CHILD)
//                        .orderByKey()
//                        .limitToFirst(ITEM_COUNT)
//            } else {
//                query = FirebaseDatabase.getInstance().reference
//                        .child(MESSAGES_CHILD)
//                        .orderByKey()
//                        .startAt(last_node)
//                        .limitToFirst(ITEM_COUNT)
//            }
//
//            query.addListenerForSingleValueEvent(object: ValueEventListener{
//                override fun onCancelled(error: DatabaseError) {
//
//                }
//
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    if(dataSnapshot.hasChildren()) {
//                        val messageList = ArrayList<FriendlyMessage>()
//                        for(snapshot in dataSnapshot.children) {
//                            messageList.add(snapshot.getValue(FriendlyMessage::class.java)!!)
//                            last_node = snapshot.key.toString()
//                        }
//
//                        if(!last_node.equals(last_key)) {
//                            messageList.removeAt(messageList.size - 1)
//                        } else {
//                            last_node = "end"
//                        }
//
//                        //fun addAll()
//                        val init = friendlyMessageList.size
//                        friendlyMessageList.addAll(messageList)
//                        mFirebaseAdapter!!.notifyItemRangeChanged(init, messageList.size)
//
//                        isLoading = false
//                    } else {
//                        isLoading = false
//                        isMaxData = true
//                    }
//                }
//            })
//        }
//    }

    // Fetch the config to determine the allowed length of messages.
    fun fetchConfig() {
        var cacheExpiration: Long = 3600 // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that
        // each fetch goes to the server. This should not be used in release
        // builds.
        if (mFirebaseRemoteConfig.info.configSettings
                        .isDeveloperModeEnabled) {
            cacheExpiration = 0
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

    /**
     * Apply retrieved length limit to edit text field.
     * This result may be fresh from the server or it may be from cached
     * values.
     */
    private fun applyRetrievedLengthLimit() {
        val friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length")
        mMessageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(friendly_msg_length.toInt()))
        countlength = friendly_msg_length
        Log.d(TAG, "FML is: $friendly_msg_length")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //you can set the title for your toolbar here for different fragments different titles
        activity!!.setTitle(R.string.school)
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in.
        mSendButton.isEnabled = false
        Log.d(TAG, "onStart()")
    }

    override fun onPause() {
        /*if (mAdView != null) {
            mAdView.pause();
        }*/
//        mFirebaseAdapter.stopListening()
        super.onPause()
        Log.d(TAG, "onPause()")
    }

    /** Called when returning to the activity  */
    override fun onResume() {
        super.onResume()
//        mFirebaseAdapter.startListening()
        Log.d(TAG, "onResume()")
        /*if (mAdView != null) {
            mAdView.resume();
        }*/
    }

    /** Called before the activity is destroyed  */
    override fun onDestroy() {
        /*if (mAdView != null) {
            mAdView.destroy();
        }*/
        super.onDestroy()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(activity, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

}