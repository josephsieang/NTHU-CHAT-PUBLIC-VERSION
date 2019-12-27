package com.google.firebase.codelab.nthuchat


import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

private const val TAG = "Friends"

class Friends : Fragment() {

    inner class MessageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        internal var activityNameTv: TextView
        internal var activityCreatorTv: TextView
        internal var activityUid: TextView

        init {
            activityNameTv = itemView.findViewById(R.id.activityNameTv)
            activityCreatorTv = itemView.findViewById(R.id.activityCreatorTv)
            activityUid = itemView.findViewById(R.id.activityUid)
        }

//        fun bindView(activityMessage: AddFriendMessages) {
//            activityNameTv = itemView.findViewById(R.id.activityNameTv)
//            activityNameTv!!.text = activityMessage.title
//        }
    }

    private var mFirebaseDatabaseRef: DatabaseReference? = null
    private var mFirebaseAdapter: FirebaseRecyclerAdapter<AddFriendMessages, MessageViewHolder>? = null
    private var mLinearLayoutManager: CustomLinearLayoutManager? = null
    private var socialRecyclerView: RecyclerView? = null
    private var refreshBtn: MenuItem? = null
    private var infoImage: CircleImageView? = null
    private var mapImage: CircleImageView? = null
    private var mFirebaseUser: FirebaseUser? = null
    private lateinit var mFirebaseAuth: FirebaseAuth
    private var mUid: String? = null
    private var mProgressBar: ProgressBar? = null
    private var mapBtn: MenuItem? = null
    private var activityToFocus: String? = null

    //pagination variable
    private var friendMessagesList: MutableList<AddFriendMessages> = ArrayList()
    private var friendUidList: MutableList<String> = ArrayList()
    private val TOTAL_ITEM_TO_LOAD = 15
    private var mRefreshLayout: SwipyRefreshLayout? = null
    private var mCurrentPage = 1
    private var itemPos = 0
    private var mLastKey = ""
    private var mPrevKey = ""
    private var last_key = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //you can set the title for your toolbar here for different fragments different titles
        activity!!.title = getString(R.string.friends)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val contentView = inflater.inflate(R.layout.fragment_social, container, false)

        Log.d(TAG, "onCreateView()")

        mRefreshLayout = contentView.findViewById(R.id.swipyrefreshlayout) as SwipyRefreshLayout
        mProgressBar = contentView.findViewById<View>(R.id.progressBarSocial) as ProgressBar

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth.currentUser
        mUid = mFirebaseUser!!.uid
//        infoImage = contentView.findViewById(R.id.info_image) as CircleImageView
//        mapImage = contentView.findViewById(R.id.map_image) as CircleImageView

        mLinearLayoutManager = CustomLinearLayoutManager(activity!!) //fix blinking when load fragment
        mLinearLayoutManager!!.stackFromEnd = true //fix every time load fragment not scroll to top at first
        mLinearLayoutManager!!.reverseLayout = true
        socialRecyclerView = contentView.findViewById<View>(R.id.socialRecyclerView) as RecyclerView
        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().reference
        val options = FirebaseRecyclerOptions.Builder<AddFriendMessages>()
                .setQuery( mFirebaseDatabaseRef!!.child("friends"), AddFriendMessages::class.java)
                .setLifecycleOwner(this)
                .build()


        mFirebaseAdapter = object : FirebaseRecyclerAdapter<AddFriendMessages, Friends.MessageViewHolder>(options) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Friends.MessageViewHolder {
                return MessageViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.activity_message, parent, false)
                )
            }

            override fun onBindViewHolder(viewHolder: Friends.MessageViewHolder, position: Int, activityMessage: AddFriendMessages) {
                if(friendMessagesList.size > 0 ) {
//                    viewHolder.bindView(friendMessagesList.get(position))
                    mProgressBar!!.visibility = ProgressBar.INVISIBLE
                    viewHolder.activityNameTv.text = friendMessagesList[position].name
                    viewHolder.activityCreatorTv.text = getString(R.string.addTime) + " " + friendMessagesList[position].addedTime
                    viewHolder.itemView.setOnClickListener {
                        var options = arrayOf(getString(R.string.chat))
                        var builder = AlertDialog.Builder(context!!)
                        builder.setTitle(getString(R.string.selectOptionInActivity))
                        builder.setItems(options, DialogInterface.OnClickListener { dialogInterface, i ->
                            //chat
                            val intent = Intent(activity, PrivateChatActivity::class.java)
                            intent.putExtra("friendUid", friendUidList[position])
                            activity!!.startActivity(intent)
                        })

                        builder.show()
                    }

                }
            }

            override fun getItemCount(): Int {
                return friendMessagesList.size
            }

            override fun getItem(position: Int): AddFriendMessages {
                return friendMessagesList.get(position)
            }


        }

        //set the recyclerView
        socialRecyclerView!!.layoutManager = mLinearLayoutManager
        socialRecyclerView!!.adapter = mFirebaseAdapter
        socialRecyclerView!!.itemAnimator!!.changeDuration = 0 //fix blinking when load fragment
        socialRecyclerView!!.itemAnimator!!.moveDuration = 0 //fix blinking when load fragment
        socialRecyclerView!!.itemAnimator!!.removeDuration = 0 //fix blinking when load fragment
        socialRecyclerView!!.itemAnimator!!.addDuration = 0 //fix blinking when load fragment
        mFirebaseAdapter!!.startListening()

        loadMessages()

        mRefreshLayout!!.setOnRefreshListener {
            mCurrentPage++
            itemPos = 0
            loadMoreMessages()
        }


        return contentView
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume(): friendMessagesList.size: ${friendMessagesList.size}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause(): friendMessagesList.size: ${friendMessagesList.size}")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart(): friendMessagesList.size: ${friendMessagesList.size}")
    }

    private fun loadMessages() {
        val messagesRef = mFirebaseDatabaseRef!!.child("users").child(mUid.toString()).child("friends")
        val query: Query = messagesRef.limitToLast(mCurrentPage * TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(AddFriendMessages::class.java)

                itemPos++


                Log.d(TAG, "messageKey: ${dataSnapshot.key}, lastKey: ${mLastKey}")



                if(dataSnapshot.key != mLastKey) { //if endDate before current date means activity has expired, so do not add them to friendMessagesList
                    friendMessagesList.add(message!!)
                    friendUidList.add(dataSnapshot.key.toString())
                }


                if (itemPos == 1) {
                    val messageKey = dataSnapshot.key
                    mLastKey = messageKey!!
                    mPrevKey = messageKey

                }

                mFirebaseAdapter!!.notifyDataSetChanged()

//                socialRecyclerView!!.scrollToPosition(friendMessagesList.size - 1)
                mLinearLayoutManager!!.scrollToPositionWithOffset(friendMessagesList.size - 1, 0)
                Log.d(TAG, "loadMessage(): ${friendMessagesList.size - 1}")

                mRefreshLayout!!.isRefreshing = false
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
        })
    }

    private fun loadMoreMessages() {
        val messagesRef = mFirebaseDatabaseRef!!.child("users").child(mUid.toString()).child("friends")
        val query: Query = messagesRef.orderByKey().endAt(mLastKey).limitToLast(TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(AddFriendMessages::class.java)

                val messageKey = dataSnapshot.key

                if(mPrevKey != messageKey ) {
                    friendMessagesList.add(itemPos++, message!!)
                    friendUidList.add(itemPos-1, dataSnapshot.key.toString())
                } else {
                    mPrevKey = mLastKey
                }

                if(itemPos == 0) {
                    last_key = true
                }

                if(itemPos == 1) {
                    mLastKey = messageKey!!
                    Log.d(TAG, "mPrevKey: $mPrevKey")
                    Log.d(TAG, "mLastKey: $mLastKey")
                }
                Log.d(TAG, "itemPos: $itemPos")

                mFirebaseAdapter!!.notifyDataSetChanged()
//                mFirebaseAdapter.notifyItemRangeChanged(init, friendMessagesList.size)

                mRefreshLayout!!.isRefreshing = false
                mLinearLayoutManager!!.scrollToPositionWithOffset(0, 0)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        mapBtn = menu!!.findItem(R.id.mapBtn)
        mapBtn!!.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId) {
            R.id.mapBtn -> {
                val myIintent: Intent = Intent(activity, showFriendsMap::class.java)
                startActivityForResult(myIintent, 1000)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}





