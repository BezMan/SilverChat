package com.dev.kotlinmessenger.views.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dev.kotlinmessenger.R
import com.dev.kotlinmessenger.model.entities.ChatMessage
import com.dev.kotlinmessenger.model.entities.User
import com.dev.kotlinmessenger.views.activities.MessagesListActivity.Companion.firebaseDatabase
import com.dev.kotlinmessenger.views.activities.MessagesListActivity.Companion.myId
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_row_item_from.view.*
import kotlinx.android.synthetic.main.chat_row_item_to.view.*

class ChatLogActivity : AppCompatActivity() {

    private val className: String = this.javaClass.simpleName
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private var selectedUser: User? = null
    private var toId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        selectedUser = intent.getParcelableExtra(ComposeMessageActivity.USER_KEY)
        toId = selectedUser?.uid

        supportActionBar?.title = selectedUser?.userName

        recyclerview_chat_log.adapter = adapter

        listenForMessages()

        adjustListToKeyboard()
    }


    /** pushes up recycler view when softkeyboard popups up */
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
        val ref = firebaseDatabase.getReference("/user-messages/$myId/$toId")
        ref.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if(chatMessage != null) {
                    Log.d(className, chatMessage.messageText)

                    if(chatMessage.fromId == myId) {
                        adapter.add(
                            ChatItemFrom(
                                chatMessage.messageText,
                                MessagesListActivity.currentUser
                            )
                        )
                    } else{
                        adapter.add(
                            ChatItemTo(
                                chatMessage.messageText,
                                selectedUser
                            )
                        )

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

            val refMessageListSender = firebaseDatabase.getReference("/user-messages/$myId/$toId").push() //push == add
            val refMessageListReceiver = firebaseDatabase.getReference("/user-messages/$toId/$myId").push() //push == add
            val refLatestMessageSender = firebaseDatabase.getReference("/latest-messages/$myId/$toId") //no push == replace
            val refLatestMessageReceiver = firebaseDatabase.getReference("/latest-messages/$toId/$myId") //no push == replace

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
        }

    }

}



// Multiple Adapters for multiple recycler item layouts

//adapter 1
class ChatItemFrom(private val messageText: String, private val user: User?): Item(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textview_chat_row_from.text = messageText
        val targetImageView = viewHolder.itemView.circleimageview_chat_row_from
        Picasso.get().load(user?.profileImageUrl).into(targetImageView)
    }

    override fun getLayout() =
        R.layout.chat_row_item_from
}

//adapter 2
class ChatItemTo(private val messageText: String, private val user: User?): Item(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textview_chat_row_to.text = messageText
        val targetImageView = viewHolder.itemView.circleimageview_chat_row_to
        Picasso.get().load(user?.profileImageUrl).into(targetImageView)
    }

    override fun getLayout() = R.layout.chat_row_item_to
}