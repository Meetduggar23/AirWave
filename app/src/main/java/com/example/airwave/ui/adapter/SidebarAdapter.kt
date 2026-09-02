package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.data.local.ConversationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SidebarAdapter(
    private var conversations: List<ConversationEntity>,
    private val listener: ConversationListener
) : RecyclerView.Adapter<SidebarAdapter.ViewHolder>() {

    interface ConversationListener {
        fun onConversationClick(conversation: ConversationEntity)
        fun onPin(conversation: ConversationEntity)
        fun onMute(conversation: ConversationEntity)
        fun onMarkRead(conversation: ConversationEntity)
        fun onMarkUnread(conversation: ConversationEntity)
        fun onUnmute(conversation: ConversationEntity)
        fun onDelete(conversation: ConversationEntity)
        fun onViewProfile(conversation: ConversationEntity)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val convName: TextView = view.findViewById(R.id.tvSidebarConvName)
        val convLastMsg: TextView = view.findViewById(R.id.tvSidebarConvLastMsg)
        val convTime: TextView = view.findViewById(R.id.tvSidebarConvTime)
        val pinIcon: ImageView = view.findViewById(R.id.ivPinIcon)
        val unreadBadge: TextView = view.findViewById(R.id.tvUnreadBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sidebar_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        val context = holder.itemView.context

        holder.convName.text = conversation.deviceName
        holder.convLastMsg.text = conversation.lastMessage
        holder.convTime.text = formatTime(conversation.lastMessageTime)

        // Pin icon
        holder.pinIcon.visibility = if (conversation.isPinned) View.VISIBLE else View.GONE

        // Unread badge
        if (conversation.unreadCount > 0) {
            holder.unreadBadge.visibility = View.VISIBLE
            holder.unreadBadge.text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString()
        } else {
            holder.unreadBadge.visibility = View.GONE
        }

        // Click
        holder.itemView.setOnClickListener {
            listener.onConversationClick(conversation)
        }

        // Long press context menu
        holder.itemView.setOnLongClickListener { view ->
            showContextMenu(view, conversation)
            true
        }
    }

    override fun getItemCount(): Int = conversations.size

    fun updateData(newConversations: List<ConversationEntity>) {
        conversations = newConversations
        notifyDataSetChanged()
    }

    private fun showContextMenu(anchor: View, conversation: ConversationEntity) {
        val popup = PopupMenu(anchor.context, anchor)
        val inflater = popup.menuInflater

        inflater.inflate(R.menu.sidebar_conversation_menu, popup.menu)

        // Adjust menu items based on conversation state
        popup.menu.findItem(R.id.action_pin)?.title =
            anchor.context.getString(if (conversation.isPinned) R.string.sidebar_menu_unpin else R.string.sidebar_menu_pin)
        popup.menu.findItem(R.id.action_mute)?.title =
            anchor.context.getString(if (conversation.isMuted) R.string.sidebar_menu_unmute else R.string.sidebar_menu_mute)
        popup.menu.findItem(R.id.action_mark_read)?.title =
            anchor.context.getString(if (conversation.unreadCount > 0) R.string.sidebar_menu_mark_read else R.string.sidebar_menu_mark_unread)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open -> {
                    listener.onConversationClick(conversation)
                    true
                }
                R.id.action_pin -> {
                    listener.onPin(conversation)
                    true
                }
                R.id.action_mark_read -> {
                    if (conversation.unreadCount > 0) {
                        listener.onMarkRead(conversation)
                    } else {
                        listener.onMarkUnread(conversation)
                    }
                    true
                }
                R.id.action_mute -> {
                    if (conversation.isMuted) {
                        listener.onUnmute(conversation)
                    } else {
                        listener.onMute(conversation)
                    }
                    true
                }
                R.id.action_delete -> {
                    listener.onDelete(conversation)
                    true
                }
                R.id.action_view_profile -> {
                    listener.onViewProfile(conversation)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = java.util.Calendar.getInstance()

        return when {
            diff < 60_000 -> "now"
            diff < 3600_000 -> "${diff / 60_000}m"
            cal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) &&
                cal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            cal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) - 1 &&
                cal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) -> {
                "Yesterday"
            }
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
