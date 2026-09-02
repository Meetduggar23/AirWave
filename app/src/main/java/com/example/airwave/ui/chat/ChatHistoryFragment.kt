package com.example.airwave.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.data.local.ConversationEntity
import com.example.airwave.data.local.DatabaseHelper
import com.example.airwave.ui.adapter.ConversationListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatHistoryFragment : Fragment() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var db: DatabaseHelper
    private val conversations = mutableListOf<ConversationEntity>()
    private lateinit var adapter: ConversationListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper.getInstance(requireContext())
        rvConversations = view.findViewById(R.id.rvConversations)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = ConversationListAdapter(conversations) { conversation ->
            val bundle = Bundle().apply {
                putString("deviceAddress", conversation.deviceAddress)
                putString("deviceName", conversation.deviceName)
            }
            findNavController().navigate(R.id.action_history_to_chat, bundle)
        }

        rvConversations.layoutManager = LinearLayoutManager(requireContext())
        rvConversations.adapter = adapter

        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun loadConversations() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                db.getAllConversations()
            }
            conversations.clear()
            conversations.addAll(list)
            adapter.notifyDataSetChanged()
            if (conversations.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvConversations.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvConversations.visibility = View.VISIBLE
            }
        }
    }
}
