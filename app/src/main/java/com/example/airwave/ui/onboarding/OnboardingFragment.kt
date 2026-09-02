package com.example.airwave.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView

    private val onboardingItems = listOf(
        OnboardingItem(
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1,
            iconRes = R.drawable.ic_airwave_logo
        ),
        OnboardingItem(
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2,
            iconRes = R.drawable.ic_airwave_logo
        ),
        OnboardingItem(
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3,
            iconRes = R.drawable.ic_airwave_logo
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        btnNext = view.findViewById(R.id.btnNext)
        btnSkip = view.findViewById(R.id.btnSkip)

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingItems.lastIndex) {
                    btnNext.text = getString(R.string.onboarding_get_started)
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.text = getString(R.string.onboarding_next)
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < onboardingItems.lastIndex) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        PreferencesHelper.onboardingDone = true
        findNavController().navigate(R.id.action_onboarding_to_login)
    }

    data class OnboardingItem(
        val titleRes: Int,
        val descRes: Int,
        val iconRes: Int
    )
}
