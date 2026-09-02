package com.example.airwave.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R

class OnboardingAdapter(
    private val items: List<OnboardingFragment.OnboardingItem>
) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.onboardingIcon)
        val title: TextView = view.findViewById(R.id.onboardingTitle)
        val description: TextView = view.findViewById(R.id.onboardingDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.title.setText(item.titleRes)
        holder.description.setText(item.descRes)
    }

    override fun getItemCount() = items.size
}
