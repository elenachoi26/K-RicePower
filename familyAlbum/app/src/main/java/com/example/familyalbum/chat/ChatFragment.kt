package com.example.familyalbum.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familyalbum.MainActivity
import com.example.familyalbum.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatFragment : Fragment() {
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var binding: FragmentChatBinding
    private lateinit var currentUserID: String
    private lateinit var chatRoomId: String

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = (activity as MainActivity).selectedGroupId
        chatRoomId = "group_$groupId"

        currentUserID = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        init()
    }

    private fun init() {
        messageList = ArrayList()
        messageAdapter = MessageAdapter(messageList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = messageAdapter

        // 채팅 메시지 불러오기 및 실시간 감지
        loadAndListenForMessages(chatRoomId)

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadAndListenForMessages(chatRoomId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, _ ->
                val messages = querySnapshot?.documents?.mapNotNull { document ->
                    val senderId = document.getString("senderId")
                    val message = document.getString("message")
                    val timestamp = document.getTimestamp("timestamp")?.toDate()
                    if (senderId != null && message != null && timestamp != null) {
                        Message(message, senderId, timestamp)
                    } else {
                        null
                    }
                } ?: emptyList()

                messageList.clear()
                messageList.addAll(messages)
                messageAdapter.notifyDataSetChanged()

                // Scroll to the bottom
                binding.chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
            }
    }


    private fun sendMessage() {
        val messageText = binding.messageEdit.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val db = FirebaseFirestore.getInstance()
            val messageData = hashMapOf(
                "senderId" to currentUserID,
                "message" to messageText,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener {
                    binding.messageEdit.text.clear()
                }
        }
    }
}
