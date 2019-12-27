package com.google.firebase.codelab.nthuchat


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "Amusement"

class Amusement : Fragment() {

    inner class MessageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        internal var activityNameTv: TextView
        internal var activityCreatorTv: TextView
        internal var activityUid: TextView

        init {
            activityNameTv = itemView.findViewById(R.id.activityNameTv)
            activityCreatorTv = itemView.findViewById(R.id.activityCreatorTv)
            activityUid = itemView.findViewById(R.id.activityUid)
        }
    }

    private var mFirebaseDatabaseRef: DatabaseReference? = null
    private var mFirebaseAdapter: FirebaseRecyclerAdapter<ActivityMessages, MessageViewHolder>? = null
    private var mLinearLayoutManager: CustomLinearLayoutManager? = null
    private var entertainmentRecyclerView: RecyclerView? = null
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
    private var activityMessageList: MutableList<ActivityMessages> = ArrayList()
    private var activityUidList: MutableList<String> = ArrayList()
    private val TOTAL_ITEM_TO_LOAD = 15
    private var mRefreshLayout: SwipyRefreshLayout? = null
    private var mCurrentPage = 1
    private var itemPos = 0
    private var mLastKey = ""
    private var mPrevKey = ""
    private var last_key = false

    //calendar
    private var targetCalendarId: String = ""
    // list for handle calendar event
    private val beginList: MutableList<Long> = ArrayList()
    private val endList: MutableList<Long> = ArrayList()
    private val titleList: MutableList<String> = ArrayList()
    private val descriptionList: MutableList<String> = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //you can set the title for your toolbar here for different fragments different titles
        activity!!.title = getString(R.string.entertainment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val contentView = inflater.inflate(R.layout.fragment_amusement, container, false)

        Log.d(TAG, "onCreateView()")

        mRefreshLayout = contentView.findViewById(R.id.swipyrefreshlayout) as SwipyRefreshLayout
        mProgressBar = contentView.findViewById<View>(R.id.progressBarEntertainment) as ProgressBar

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth.currentUser
        mUid = mFirebaseUser!!.uid
//        infoImage = contentView.findViewById(R.id.info_image) as CircleImageView
//        mapImage = contentView.findViewById(R.id.map_image) as CircleImageView

        query_calendar()

        mLinearLayoutManager = CustomLinearLayoutManager(activity!!) //fix blinking when load fragment
        mLinearLayoutManager!!.stackFromEnd = true //fix every time load fragment not scroll to top at first
        mLinearLayoutManager!!.reverseLayout = true
        entertainmentRecyclerView = contentView.findViewById<View>(R.id.entertainmentRecyclerView) as RecyclerView
        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().reference
        val options = FirebaseRecyclerOptions.Builder<ActivityMessages>()
                .setQuery( mFirebaseDatabaseRef!!.child("activity").child("amusement"), ActivityMessages::class.java)
                .setLifecycleOwner(this)
                .build()


        mFirebaseAdapter = object : FirebaseRecyclerAdapter<ActivityMessages, Amusement.MessageViewHolder>(options) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Amusement.MessageViewHolder {
                return MessageViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.activity_message, parent, false)
                )
            }

            override fun onBindViewHolder(viewHolder: Amusement.MessageViewHolder, position: Int, activityMessage: ActivityMessages) {
                if(activityMessageList.size > 0 ) {
//                    viewHolder.bindView(activityMessageList.get(position))
                    mProgressBar!!.visibility = ProgressBar.INVISIBLE
                    viewHolder.activityNameTv.text = activityMessageList[position].title
                    viewHolder.activityCreatorTv.text = getString(R.string.creator) + " " + activityMessageList[position].creatorName
                    viewHolder.activityUid.text = activityUidList[position]
                    if(activityMessageList[position].creator == mUid) {
                        viewHolder.itemView.setOnClickListener {
                            val options = arrayOf(getString(R.string.Info), getString(R.string.delete), getString(R.string.addToCalendar))
                            val builder = AlertDialog.Builder(context!!)
                            builder.setTitle(getString(R.string.selectOptionInActivity))
                            builder.setItems(options, DialogInterface.OnClickListener { dialogInterface, i ->

                                if (i == 0) {
                                    //show info
                                    val intent = Intent(activity, InfoActivityAmusement::class.java)
                                    intent.putExtra("activityUid", activityUidList[position])
                                    Log.d(TAG, "activityUid: ${activityUidList[position]}")
                                    activity!!.startActivity(intent)
                                } else if (i == 1){
                                    //delete
                                    mFirebaseDatabaseRef!!.child("activity").child("amusement").addListenerForSingleValueEvent(object: ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError) {

                                        }

                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            for(childSnapshot in dataSnapshot.children) {
                                                if(childSnapshot.key == viewHolder.activityUid.text) {
                                                    val builder: AlertDialog.Builder  = AlertDialog.Builder(activity!!)
                                                    builder.setTitle(getString(R.string.delete))
                                                    builder.setMessage(getString(R.string.deleteDetails))
                                                    builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                                                        Log.d(TAG, "deleteKey: ${childSnapshot.key }")
                                                        mFirebaseDatabaseRef!!.child("activity").child("amusement").child(viewHolder.activityUid.text.toString()).removeValue()
                                                        activityMessageList.removeAt(position)
                                                        activityUidList.removeAt(position)
                                                        mFirebaseAdapter!!.notifyDataSetChanged()

                                                        mCurrentPage = 1
                                                        itemPos = 0
                                                        mLastKey = ""
                                                        mPrevKey = ""
                                                        last_key = false
                                                        activityMessageList.clear()
                                                        activityUidList.clear()
                                                        mFirebaseAdapter!!.notifyDataSetChanged()

                                                        loadMessages()
                                                    }
                                                    builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                                                        Log.d(TAG, "delete cancel")
                                                    }
                                                    val dialog = builder.create()
                                                    dialog.show()
                                                }
                                            }
                                        }
                                    })
                                } else {
                                    val builder: AlertDialog.Builder  = AlertDialog.Builder(activity!!)
                                    builder.setTitle(getString(R.string.addToCalendar))
                                    builder.setMessage(getString(R.string.YesOrNo))
                                    builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                                        Log.d(TAG, "addToCalendar cancel")
                                    }
                                    builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                                        val title = activityMessageList[position].title
                                        val startDate = activityMessageList[position].startdate
                                        val endDate = activityMessageList[position].enddate
                                        val description = activityMessageList[position].description
                                        insert_event(title.toString(), startDate.toString(), endDate.toString(), description.toString())
                                    }
                                    val dialog = builder.create()
                                    dialog.show()
                                }
                            })

                            builder.show()
                        }
                    } else {
                        viewHolder.itemView.setOnClickListener {
                            val options = arrayOf(getString(R.string.Info), getString(R.string.addToCalendar))
                            val builder = AlertDialog.Builder(context!!)
                            builder.setTitle(getString(R.string.selectOptionInActivity))
                            builder.setItems(options, DialogInterface.OnClickListener { dialogInterface, i ->
                                if(i == 0) {
                                    //show info
                                    val intent = Intent(activity, InfoActivityAmusement::class.java)
                                    intent.putExtra("activityUid", activityUidList[position])
                                    activity!!.startActivity(intent)
                                } else {
                                    //add to calendar
                                    val builder: AlertDialog.Builder  = AlertDialog.Builder(activity!!)
                                    builder.setTitle(getString(R.string.addToCalendar))
                                    builder.setMessage(getString(R.string.YesOrNo))
                                    builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                                        Log.d(TAG, "addToCalendar cancel")
                                    }
                                    builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                                        val title = activityMessageList[position].title
                                        val startDate = activityMessageList[position].startdate
                                        val endDate = activityMessageList[position].enddate
                                        val description = activityMessageList[position].description
                                        insert_event(title.toString(), startDate.toString(), endDate.toString(), description.toString())
                                    }
                                    val dialog = builder.create()
                                    dialog.show()
                                }
                            })

                            builder.show()
                        }
                    }

                }
            }

            override fun getItemCount(): Int {
                return activityMessageList.size
            }

            override fun getItem(position: Int): ActivityMessages {
                return activityMessageList.get(position)
            }


        }

        //set the recyclerView
        entertainmentRecyclerView!!.layoutManager = mLinearLayoutManager
        entertainmentRecyclerView!!.adapter = mFirebaseAdapter
        entertainmentRecyclerView!!.itemAnimator!!.changeDuration = 0 //fix blinking when load fragment
        entertainmentRecyclerView!!.itemAnimator!!.moveDuration = 0 //fix blinking when load fragment
        entertainmentRecyclerView!!.itemAnimator!!.removeDuration = 0 //fix blinking when load fragment
        entertainmentRecyclerView!!.itemAnimator!!.addDuration = 0 //fix blinking when load fragment
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
        query_event()
        Log.d(TAG, "onResume(): activityMessageList.size: ${activityMessageList.size}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause(): activityMessageList.size: ${activityMessageList.size}")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart(): activityMessageList.size: ${activityMessageList.size}")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    private fun loadMessages() {
        val messagesRef = mFirebaseDatabaseRef!!.child("activity").child("amusement")
        val query: Query = messagesRef.limitToLast(mCurrentPage * TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(ActivityMessages::class.java)

                itemPos++


                Log.d(TAG, "messageKey: ${dataSnapshot.key}, lastKey: ${mLastKey}")

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dateFromString = format.parse(message!!.enddate)

                if(dataSnapshot.key != mLastKey && dateFromString > Calendar.getInstance().time) { //if endDate before current date means activity has expired, so do not add them to activityMessageList
                    activityMessageList.add(message!!)
                    activityUidList.add(dataSnapshot.key.toString())
                }

                //delete from db for expired activity
//                if(dateFromString < Calendar.getInstance().time) {
//
//                }

                if (itemPos == 1) {
                    val messageKey = dataSnapshot.key
                    mLastKey = messageKey!!
                    mPrevKey = messageKey

                }

                mFirebaseAdapter!!.notifyDataSetChanged()

//                socialRecyclerView!!.scrollToPosition(activityMessageList.size - 1)
                mLinearLayoutManager!!.scrollToPositionWithOffset(activityMessageList.size - 1, 0)
                Log.d(TAG, "loadMessage(): ${activityMessageList.size - 1}")

                mRefreshLayout!!.isRefreshing = false
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
        })
    }

    private fun loadMoreMessages() {
        val messagesRef = mFirebaseDatabaseRef!!.child("activity").child("amusement")
        val query: Query = messagesRef.orderByKey().endAt(mLastKey).limitToLast(TOTAL_ITEM_TO_LOAD)

        query.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                var message = dataSnapshot.getValue(ActivityMessages::class.java)

                val messageKey = dataSnapshot.key

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dateFromString = format.parse(message!!.enddate)

                if(mPrevKey != messageKey && !last_key && dateFromString > Calendar.getInstance().time) { //if endDate before current date means activity has expired, so do not add them to activityMessageList
                    activityMessageList.add(itemPos++, message!!)
                    activityUidList.add(itemPos-1, dataSnapshot.key.toString())
                } else {
                    mPrevKey = mLastKey
                }

                //delete from db for expired activity
//                if(dateFromString < Calendar.getInstance().time) {
//
//                }

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
//                mFirebaseAdapter.notifyItemRangeChanged(init, activityMessageList.size)

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
                val myIintent: Intent = Intent(activity, showAmusementActivityMap::class.java)
                startActivityForResult(myIintent, 1000)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun query_calendar() {
        val EVENT_PROJECTION = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        // Based on the seeting above, define the index for data
        val PROJECTION_ID_INDEX = 0
        val PROJECTION_ACCOUNT_NAME_INDEX = 1
        val PROJECTION_DISPLAY_NAME_INDEX = 2
        val PROJECTION_OWNER_ACCOUNT_INDEX = 3
        val PROJECTION_CALENDAR_ACCESS_LEVEL = 4

        val targetAccount = accountString
        // get calendar
        var cur: Cursor
        val cr: ContentResolver = activity!!.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI
        //define query rules, find out the calendar that can be fully controllable from google account
        val selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("  + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND (" + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " = ?))";
        val selectionArgs = arrayOf(targetAccount, "com.google",
                Integer.toString(CalendarContract.Calendars.CAL_ACCESS_OWNER))

        // Create list to save user's selection
        val accountNameList = ArrayList<String>()
        val calendarIdList = ArrayList<Int>()

        // check permission
        val permissionCheck = ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_CALENDAR)
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)
            if (cur != null) {
                while (cur.moveToNext()) {
                    // extract the data from calendar
                    val calendarId = cur.getLong(PROJECTION_ID_INDEX);
                    val accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
                    val displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
                    val ownerAccount = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
                    val accessLevel = cur.getInt(PROJECTION_CALENDAR_ACCESS_LEVEL)
                    Log.d("query_calendar", String.format("calendarId=%s", calendarId))
                    Log.d("query_calendar", String.format("accountName=%s", accountName))
                    Log.d("query_calendar", String.format("displayName=%s", displayName))
                    Log.d("query_calendar", String.format("ownerAccount=%s", ownerAccount))
                    Log.d("query_calendar", String.format("accessLevel=%s", accessLevel))
                    // temporarily save user's selection
                    accountNameList.add(displayName)
                    calendarIdList.add(calendarId.toInt())
                }
                cur.close()
            }
            if (calendarIdList.size != 0) {
                targetCalendarId = String.format("%s", calendarIdList[0])
            }
            else {
                Log.d("query_calendar", "cannot find calendar")
            }
        }
    }

    private fun query_event() {
        val INSTANCE_PROJECTION = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION

        )
        // Based on the seeting above, define the index for data
        val PROJECTION_ID_INDEX = 0
        val PROJECTION_BEGIN_INDEX = 1
        val PROJECTION_END_INDEX = 2
        val PROJECTION_TITLE_INDEX = 3
        val PROJECTION_DESCRIPTION_INDEX = 4

        // get calendar ID
        val targetCalendar = targetCalendarId
        // specific the time and check all the events in the specific time
        // month starting from 0, 0-11
        val beginTime = Calendar.getInstance()
        beginTime.set(2019, 3, 1, 8, 0)
        val startMillis = beginTime.timeInMillis
        val endTime = Calendar.getInstance()
        endTime.set(2500, 6, 1, 8, 0)
        val endMillis = endTime.timeInMillis

        // get events
        val cr = activity!!.contentResolver
        val builder: Uri.Builder  = CalendarContract.Instances.CONTENT_URI.buildUpon()

        // define query rules, find out all the events in the specific fime from the calendar
        val selection = CalendarContract.Events.CALENDAR_ID + " = ?"
        val selectionArgs = arrayOf(targetCalendar)
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        // check permission
        val permissionCheck = ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.READ_CALENDAR)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val cur = cr.query(builder.build(),
                    INSTANCE_PROJECTION,
                    selection,
                    selectionArgs,
                    null)
            if (cur != null) {
                while (cur.moveToNext()) {
                    // get all the data
                    val eventID = cur.getLong(PROJECTION_ID_INDEX)
                    val beginVal = cur.getLong(PROJECTION_BEGIN_INDEX)
                    val endVal = cur.getLong(PROJECTION_END_INDEX)
                    val title = cur.getString(PROJECTION_TITLE_INDEX)
                    val des = cur.getString(PROJECTION_DESCRIPTION_INDEX)
                    Log.d("query_event", String.format("eventID=%s", eventID))
                    Log.d("query_event", String.format("beginVal=%s", beginVal))
                    Log.d("query_event", String.format("endVal=%s", endVal))
                    Log.d("query_event", String.format("title=%s", title))
                    Log.d("query_event", String.format("des=%s", des))

                    beginList.add(beginVal)
                    endList.add(endVal)
                    titleList.add(title.toString())
                    descriptionList.add(des)
                }
                cur.close()
            }
        }
    }

    private fun insert_event(title: String, startDate: String, endDate: String, description: String) {
        //get calendar ID
        val calendarId = targetCalendarId.toLong()

        //date formatter
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        formatter.isLenient = false

        val targetStartDate = formatter.parse(startDate)
        val startMillis = targetStartDate.time

        val targetEndDate = formatter.parse(endDate)
        val endMillis = targetEndDate.time

        // new event to calendar
        val cr = activity!!.contentResolver
        val values  = ContentValues()
        values.put(CalendarContract.Events.DTSTART, startMillis)
        values.put(CalendarContract.Events.DTEND, endMillis)
        values.put(CalendarContract.Events.TITLE, title)
        values.put(CalendarContract.Events.DESCRIPTION, description)
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().displayName)

        val permissionCheck = ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.WRITE_CALENDAR);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            var eventCounter = 0
            var add = true
            for(eventTitle in titleList) {
                if(eventTitle == title) {
                    if(beginList[eventCounter] == startMillis && endList[eventCounter] == endMillis && descriptionList[eventCounter] == description) {
                        add = false
                        Log.d("insert_event_add", eventTitle)
                        break
                    }
                }
                Log.d("insert_event", eventTitle)
                eventCounter++
            }
            if(add) {
                val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    titleList.add(title)
                    beginList.add(startMillis)
                    endList.add(endMillis)
                    descriptionList.add(description)
                }
            }
        }
    }
}
