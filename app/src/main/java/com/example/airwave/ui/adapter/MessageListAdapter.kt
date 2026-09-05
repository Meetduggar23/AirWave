package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageListAdapter(
    messages: List<ChatMessage> = emptyList()
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    init {
        submitList(messages)
    }

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                // Messages are immutable once created, so full content equality
                // is a safe item identity (the list only ever grows).
                return oldItem.senderName == newItem.senderName &&
                    oldItem.text == newItem.text &&
                    oldItem.isSent == newItem.isSent &&
                    oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvSender: TextView = view.findViewById(R.id.tvSender)
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val time = formatTime(message.timestamp)

        when (holder) {
            is SentViewHolder -> {
                holder.tvMessage.text = message.text
                holder.tvTime.text = time
            }
            is ReceivedViewHolder -> {
                holder.tvMessage.text = message.text
                holder.tvTime.text = time
                holder.tvSender.text = message.senderName
            }
        }
    }

    /** Replaces the backing list and refreshes the list. Called whenever the message list changes. */
    fun updateData(newMessages: List<ChatMessage>) {
        submitList(newMessages)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}