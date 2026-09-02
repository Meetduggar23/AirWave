package com.example.airwave.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDivider
import com.example.airwave.R
import com.example.airwave.data.local.ConversationEntity
import com.example.airwave.data.local.DatabaseHelper
import com.example.airwave.util.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var btnFindUsers: MaterialButton
    private lateinit var btnChatsHome: MaterialCardView
    private lateinit var btnProfileHome: MaterialCardView
    private lateinit var btnSettingsHome: MaterialCardView
    private lateinit var bluetoothStatusDot: View
    private lateinit var connectionStatusDot: View
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    // Sidebar (lives in activity_main.xml, must use requireActivity().findViewById)
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebarContent: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var btnCloseSidebar: ImageButton
    private lateinit var tvSidebarNickname: TextView
    private lateinit var tvSidebarProfileType: TextView
    private lateinit var db: DatabaseHelper
    private var allConversations: List<ConversationEntity> = emptyList()
    private var isSearchActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper.getInstance(requireContext())

        // Main content (in fragment_home.xml)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvBluetoothStatus = view.findViewById(R.id.tvBluetoothStatus)
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        btnFindUsers = view.findViewById(R.id.btnFindUsers)
        btnChatsHome = view.findViewById(R.id.btnChats)
        btnProfileHome = view.findViewById(R.id.btnProfile)
        btnSettingsHome = view.findViewById(R.id.btnSettings)
        bluetoothStatusDot = view.findViewById(R.id.bluetoothStatusDot)
        connectionStatusDot = view.findViewById(R.id.connectionStatusDot)
        toolbar = view.findViewById(R.id.toolbar)

        // Sidebar setup (views are in activity_main.xml, not fragment layout)
        val activity = requireActivity()
        drawerLayout = activity.findViewById(R.id.drawerLayout)
        sidebarContent = activity.findViewById(R.id.sidebarContent)
        etSearch = activity.findViewById(R.id.etSearch)
        btnClearSearch = activity.findViewById(R.id.btnClearSearch)
        btnCloseSidebar = activity.findViewById(R.id.btnCloseSidebar)
        tvSidebarNickname = activity.findViewById(R.id.tvSidebarNickname)
        tvSidebarProfileType = activity.findViewById(R.id.tvSidebarProfileType)

        // Hamburger menu
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Close sidebar
        btnCloseSidebar.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Sidebar navigation
        activity.findViewById<LinearLayout>(R.id.btnNewChat).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_nearby)
        }
        activity.findViewById<LinearLayout>(R.id.btnNearbyUsers).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_nearby)
        }
        activity.findViewById<LinearLayout>(R.id.btnChatsSidebar).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_chat_history)
        }
        activity.findViewById<LinearLayout>(R.id.btnFavorites).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showFavorites()
        }
        activity.findViewById<LinearLayout>(R.id.btnProfileSidebar).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_profile)
        }
        activity.findViewById<LinearLayout>(R.id.btnSettingsSidebar).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_settings)
        }
        activity.findViewById<LinearLayout>(R.id.btnAbout).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_about)
        }

        // Profile area click
        activity.findViewById<LinearLayout>(R.id.sidebarProfileArea).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_home_to_profile)
        }

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                if (query.isNotEmpty()) {
                    searchConversations(query)
                } else {
                    isSearchActive = false
                    loadSidebarConversations()
                }
            }
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            isSearchActive = false
            loadSidebarConversations()
        }

        // Main content clicks
        btnFindUsers.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_nearby)
        }
        btnChatsHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat_history)
        }
        btnProfileHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
        btnSettingsHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        loadSidebarConversations()
    }

    private fun updateUI() {
        val nickname = PreferencesHelper.nickname
        tvGreeting.text = getString(R.string.home_greeting, nickname)
        tvSidebarNickname.text = nickname
        tvSidebarProfileType.text = getString(R.string.sidebar_profile_type_user)
        updateBluetoothStatus()
    }

    private fun updateBluetoothStatus() {
        val btManager = requireContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val btAdapter = btManager?.adapter
        val isEnabled = btAdapter?.isEnabled == true

        if (isEnabled) {
            tvBluetoothStatus.text = getString(R.string.bluetooth_on)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_connected)
        } else {
            tvBluetoothStatus.text = getString(R.string.bluetooth_off)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
        }

        btnFindUsers.isEnabled = isEnabled
    }

    private fun loadSidebarConversations() {
        viewLifecycleOwner.lifecycleScope.launch {
            val conversations = withContext(Dispatchers.IO) {
                isSearchActive = false
                db.getAllConversations()
            }
            if (isAdded) {
                allConversations = conversations
                renderConversations(conversations)
            }
        }
    }

    private fun searchConversations(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                isSearchActive = true
                db.searchConversations(query)
            }
            if (isAdded) renderConversations(results)
        }
    }

    private fun showFavorites() {
        isSearchActive = true
        val favorites = allConversations.filter { it.isFavorite }
        if (favorites.isEmpty()) {
            sidebarContent.removeAllViews()
            val emptyText = TextView(requireContext()).apply {
                text = getString(R.string.sidebar_no_favorites)
                textSize = 14f
                setTextColor(resources.getColor(R.color.airwave_light_text_secondary, null))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 48, 16, 48)
            }
            sidebarContent.addView(emptyText)
        } else {
            renderConversations(favorites)
        }
    }

    private fun renderConversations(conversations: List<ConversationEntity>) {
        sidebarContent.removeAllViews()

        if (conversations.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = if (isSearchActive) getString(R.string.sidebar_no_results) else getString(R.string.sidebar_no_conversations)
                textSize = 14f
                setTextColor(resources.getColor(R.color.airwave_light_text_secondary, null))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 48, 16, 48)
            }
            sidebarContent.addView(emptyText)
            return
        }

        val pinned = conversations.filter { it.isPinned }
        val recent = conversations.filter { !it.isPinned }

        if (pinned.isNotEmpty()) {
            val pinnedLabel = TextView(requireContext()).apply {
                text = getString(R.string.sidebar_pinned)
                textSize = 12f
                setTextColor(resources.getColor(R.color.airwave_light_text_secondary, null))
                setPadding(16, 12, 16, 4)
            }
            sidebarContent.addView(pinnedLabel)

            pinned.forEach { conv ->
                sidebarContent.addView(createConversationItem(conv))
            }
        }

        if (recent.isNotEmpty()) {
            if (pinned.isNotEmpty()) {
                val divider = MaterialDivider(requireContext()).apply {
                    setDividerColor(resources.getColor(R.color.divider_light, null))
                    setPadding(16, 8, 16, 8)
                }
                sidebarContent.addView(divider)
            }

            val recentLabel = TextView(requireContext()).apply {
                text = getString(R.string.sidebar_recent)
                textSize = 12f
                setTextColor(resources.getColor(R.color.airwave_light_text_secondary, null))
                setPadding(16, 12, 16, 4)
            }
            sidebarContent.addView(recentLabel)

            recent.forEach { conv ->
                sidebarContent.addView(createConversationItem(conv))
            }
        }
    }

    private fun createConversationItem(conversation: ConversationEntity): View {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_sidebar_conversation, sidebarContent, false)

        val convName: TextView = itemView.findViewById(R.id.tvSidebarConvName)
        val convLastMsg: TextView = itemView.findViewById(R.id.tvSidebarConvLastMsg)
        val convTime: TextView = itemView.findViewById(R.id.tvSidebarConvTime)
        val pinIcon: ImageView = itemView.findViewById(R.id.ivPinIcon)
        val unreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)

        convName.text = conversation.deviceName
        convLastMsg.text = conversation.lastMessage
        convTime.text = formatTime(conversation.lastMessageTime)

        pinIcon.visibility = if (conversation.isPinned) View.VISIBLE else View.GONE

        if (conversation.unreadCount > 0) {
            unreadBadge.visibility = View.VISIBLE
            unreadBadge.text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString()
        } else {
            unreadBadge.visibility = View.GONE
        }

        itemView.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            onConversationClick(conversation)
        }

        itemView.setOnLongClickListener { view ->
            showContextMenu(view, conversation)
            true
        }

        return itemView
    }

    private fun showContextMenu(anchor: View, conversation: ConversationEntity) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.sidebar_conversation_menu, popup.menu)

        popup.menu.findItem(R.id.action_pin)?.title =
            getString(if (conversation.isPinned) R.string.sidebar_menu_unpin else R.string.sidebar_menu_pin)
        popup.menu.findItem(R.id.action_mute)?.title =
            getString(if (conversation.isMuted) R.string.sidebar_menu_unmute else R.string.sidebar_menu_mute)
        popup.menu.findItem(R.id.action_mark_read)?.title =
            getString(if (conversation.unreadCount > 0) R.string.sidebar_menu_mark_read else R.string.sidebar_menu_mark_unread)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open -> {
                    onConversationClick(conversation)
                    true
                }
                R.id.action_pin -> {
                    onPin(conversation)
                    true
                }
                R.id.action_mark_read -> {
                    if (conversation.unreadCount > 0) onMarkRead(conversation) else onMarkUnread(conversation)
                    true
                }
                R.id.action_mute -> {
                    if (conversation.isMuted) onUnmute(conversation) else onMute(conversation)
                    true
                }
                R.id.action_delete -> {
                    onDelete(conversation)
                    true
                }
                R.id.action_view_profile -> {
                    onViewProfile(conversation)
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
            cal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) - 1 -> {
                "Yesterday"
            }
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun onConversationClick(conversation: ConversationEntity) {
        val bundle = Bundle().apply {
            putString("deviceAddress", conversation.deviceAddress)
            putString("deviceName", conversation.deviceName)
            putString("chatId", conversation.chatId)
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.markAsRead(conversation.chatId)
        }
        findNavController().navigate(R.id.action_home_to_chat, bundle)
    }

    private fun onPin(conversation: ConversationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.togglePin(conversation.chatId)
            val updated = db.getAllConversations()
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    allConversations = updated
                    renderConversations(updated)
                    Toast.makeText(requireContext(), getString(R.string.sidebar_toast_pinned), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onMute(conversation: ConversationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.toggleMute(conversation.chatId)
            val updated = db.getAllConversations()
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    allConversations = updated
                    renderConversations(updated)
                    Toast.makeText(requireContext(), getString(R.string.sidebar_toast_muted), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onMarkRead(conversation: ConversationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.markAsRead(conversation.chatId)
            val updated = db.getAllConversations()
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    allConversations = updated
                    renderConversations(updated)
                }
            }
        }
    }

    private fun onMarkUnread(conversation: ConversationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.markAsUnread(conversation.chatId)
            val updated = db.getAllConversations()
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    allConversations = updated
                    renderConversations(updated)
                }
            }
        }
    }

    private fun onUnmute(conversation: ConversationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.toggleMute(conversation.chatId)
            val updated = db.getAllConversations()
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    allConversations = updated
                    renderConversations(updated)
                    Toast.makeText(requireContext(), getString(R.string.sidebar_toast_unmuted), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onDelete(conversation: ConversationEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.sidebar_delete_title)
            .setMessage(getString(R.string.sidebar_delete_message, conversation.deviceName))
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.deleteConversation(conversation.chatId)
                    db.deleteMessagesForChat(conversation.chatId)
                    val updated = db.getAllConversations()
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            allConversations = updated
                            renderConversations(updated)
                            Toast.makeText(requireContext(), getString(R.string.sidebar_toast_deleted), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onViewProfile(conversation: ConversationEntity) {
        Toast.makeText(requireContext(), getString(R.string.sidebar_toast_profile, conversation.deviceName), Toast.LENGTH_SHORT).show()
    }
}
