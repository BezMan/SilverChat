package com.dev.silverchat.views.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dev.silverchat.R
import com.dev.silverchat.helpers.DateUtils
import com.dev.silverchat.model.entities.ChatMessage
import com.dev.silverchat.model.entities.UnreadMessages
import com.dev.silverchat.model.entities.User
import com.dev.silverchat.views.activities.MainListActivity.Companion.LATEST_MESSAGES
import com.dev.silverchat.views.activities.MainListActivity.Companion.UNREAD_MESSAGES
import com.dev.silverchat.views.activities.MainListActivity.Companion.USERS
import com.dev.silverchat.views.activities.MainListActivity.Companion.USER_MESSAGES
import com.dev.silverchat.views.activities.MainListActivity.Companion.firebaseDatabase
import com.dev.silverchat.views.activities.MainListActivity.Companion.myId
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.chat_activity.*
import kotlinx.android.synthetic.main.chat_row_from.view.*
import kotlinx.android.synthetic.main.chat_row_to.view.*
import kotlinx.android.synthetic.main.custom_chat_bar.*

class ChatLogActivity : AppCompatActivity() {

    private val className: String = this.javaClass.simpleName
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private var selectedUser: User? = null
    private var toId: String? = null
    private var toName: String? = null
    private var toImageUrl: String? = null
    private var toStatusAbout: String? = null
    private var toTimeJoined: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity)

        getUserInfo()

        initViews()

        listenForMessages()

        adjustListToKeyboard()

    }

    private fun getUserInfo() {
        selectedUser = intent.getParcelableExtra(FindFriendsActivity.USER_KEY)
        toId = selectedUser?.uid
        toName = selectedUser?.userName
        toImageUrl = selectedUser?.imageUrl
        toStatusAbout = selectedUser?.statusText
        toTimeJoined = selectedUser?.timeJoined
    }


    private fun initViews() {
        //ACTIONBAR views//
        val actionBar = supportActionBar
        actionBar?.setDisplayShowCustomEnabled(true)
        val actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null)
        actionBar?.customView = actionBarView
        chat_toolbar.setOnClickListener {
            openFriendInfoDialog()
        }
        custom_profile_name.text = toName
        if(!toImageUrl.isNullOrEmpty()){
            Picasso.get().load(toImageUrl).into(custom_profile_image)
        }
        displayLastSeen()

        //RECYCLER views//
        recyclerview_chat_log.adapter = adapter
    }

    private fun openFriendInfoDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val inflater = layoutInflater
//        builder.setTitle(toName)
        val layoutInfoDialog = inflater.inflate(R.layout.chat_info_dialog, null)
        val chatInfoImage  = layoutInfoDialog.findViewById<ImageView>(R.id.chat_info_image)
        val chatInfoName  = layoutInfoDialog.findViewById<TextView>(R.id.chat_info_name)
        val chatInfoAbout  = layoutInfoDialog.findViewById<TextView>(R.id.chat_info_status_about)
        val chatInfoDateJoined  = layoutInfoDialog.findViewById<TextView>(R.id.chat_info_time_joined)

        val formattedTimeJoined = DateUtils.getFormattedTimeChatLog(toTimeJoined?.toLong()!!)

        chatInfoName.text = toName
        chatInfoAbout.text = toStatusAbout
        chatInfoDateJoined.text = "Joined: $formattedTimeJoined"
        if(!toImageUrl.isNullOrEmpty()) {
            Picasso.get().load(toImageUrl).into(chatInfoImage)
        }
        dialog.setView(layoutInfoDialog)
        dialog.show()

        layoutInfoDialog.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun displayLastSeen() {
        firebaseDatabase.reference.child(USERS).child(toId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.hasChild("online") && dataSnapshot.hasChild("last_seen")) {
                        val isOnline= dataSnapshot.child("online").value
                        val lastSeen = dataSnapshot.child("last_seen").value.toString()

                        val formattedLastSeen = DateUtils.getFormattedTimeLatestMessage(lastSeen.toLong())
                        if (isOnline == true) {
                            custom_user_last_seen.text = "online"
                        } else {
                            custom_user_last_seen.text = "Last Seen: $formattedLastSeen"
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }



    override fun onPause() {
        super.onPause()
        resetUnreadMessages()
    }


    private fun resetUnreadMessages() {
        firebaseDatabase.getReference("/$UNREAD_MESSAGES/$myId/$toId")
            .setValue(UnreadMessages(0))
    }


    /** pushes up recycler view when softkeyboard pops up */
    private fun adjustListToKeyboard() {
        recyclerview_chat_log.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                recyclerview_chat_log.postDelayed({
                    recyclerview_chat_log.scrollToPosition(
                        recyclerview_chat_log.adapter!!.itemCount - 1
                    )
                }, 100)
            }
        }
    }


    /** populate list on initial load and also on children added */
    private fun listenForMessages() {
        val ref = firebaseDatabase.getReference("/$USER_MESSAGES/$myId/$toId")
        ref.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if(chatMessage != null) {

                    if(chatMessage.fromId == myId) {
                        adapter.add(ChatItemFrom(chatMessage.messageText, chatMessage.timeStamp))
                    } else{
                        adapter.add(ChatItemTo(chatMessage.messageText, chatMessage.timeStamp))
                    }
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }

        })
    }


    fun sendMessageButtonClicked(view: View) {
        performSendMessage()
    }


    private fun performSendMessage() {

        val messageText = edittext_input_chat_log.text.toString()

        if (messageText.trim().isNotEmpty()) {

            val refMessageListSender = firebaseDatabase.getReference("/$USER_MESSAGES/$myId/$toId").push() //push == add
            val refMessageListReceiver = firebaseDatabase.getReference("/$USER_MESSAGES/$toId/$myId").push() //push == add
            val refLatestMessageSender = firebaseDatabase.getReference("/$LATEST_MESSAGES/$myId/$toId") //no push == replace
            val refLatestMessageReceiver = firebaseDatabase.getReference("/$LATEST_MESSAGES/$toId/$myId") //no push == replace

            val chatMessage = ChatMessage(
                refMessageListSender.key,
                messageText,
                myId,
                toId
            )

            refMessageListSender.setValue(chatMessage)
            refMessageListReceiver.setValue(chatMessage)
            refLatestMessageSender.setValue(chatMessage)
            refLatestMessageReceiver.setValue(chatMessage)

            edittext_input_chat_log.text.clear()

            incrementUnreadMessagesCount()

        }

    }

    private fun incrementUnreadMessagesCount() {
        val refIncrementUnread = firebaseDatabase.getReference("/$UNREAD_MESSAGES/$toId/$myId") //no push == replace

        refIncrementUnread.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                val unreadMessages = p0.getValue(UnreadMessages::class.java)
                if (unreadMessages == null) {
                    refIncrementUnread.setValue(UnreadMessages(1))
                } else {
                    refIncrementUnread.setValue(UnreadMessages(unreadMessages.count?.plus(1)))
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

}



// Multiple Adapters for multiple recycler item layouts

//adapter 1//
class ChatItemFrom(private val messageText: String, private val timestamp: Long): Item(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textview_chat_row_from.text = messageText
        viewHolder.itemView.time_chat_row_from.text = DateUtils.getFormattedTimeChatLog(timestamp)
    }

    override fun getLayout() = R.layout.chat_row_from
}

//adapter 2//
class ChatItemTo(private val messageText: String, private val timestamp: Long): Item(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textview_chat_row_to.text = messageText
        viewHolder.itemView.time_chat_row_to.text = DateUtils.getFormattedTimeChatLog(timestamp)
    }

    override fun getLayout() = R.layout.chat_row_to
}