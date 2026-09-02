package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.data.local.ConversationEntity
import java.text.SimpleDateFormat
import java.util.*

class ConversationListAdapter(
    private val conversations: List<ConversationEntity>,
    private val onClick: (ConversationEntity) -> Unit
) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvConversationName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.tvName.text = conversation.deviceName
        holder.tvLastMessage.text = conversation.lastMessage
        holder.tvTime.text = formatTime(conversation.lastMessageTime)
        holder.itemView.setOnClickListener { onClick(conversation) }
    }

    override fun getItemCount() = conversations.size

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
