package com.tiznet.manager.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.tiznet.manager.BuildConfig
import com.tiznet.manager.R
import com.tiznet.manager.databinding.FragmentAboutBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.util.LocaleManager

class AboutFragment : BaseFragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.versionText.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        when (prefs.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.chipThemeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.chipThemeDark.isChecked = true
            else -> binding.chipThemeSystem.isChecked = true
        }
        binding.themeChips.setOnCheckedStateChangeListener { group, _ ->
            val mode = when (group.checkedChipId) {
                R.id.chipThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.chipThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.dynamicSwitch.isChecked = prefs.dynamicColor
        binding.dynamicSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.dynamicColor = checked
            if (checked) DynamicColors.applyToActivityIfAvailable(requireActivity())
            else requireActivity().recreate()
        }

        when (prefs.language) {
            LocaleManager.PERSIAN -> binding.chipLangFa.isChecked = true
            LocaleManager.ENGLISH -> binding.chipLangEn.isChecked = true
            else -> binding.chipLangSystem.isChecked = true
        }
        binding.languageChips.setOnCheckedStateChangeListener { group, _ ->
            val tag = when (group.checkedChipId) {
                R.id.chipLangFa -> LocaleManager.PERSIAN
                R.id.chipLangEn -> LocaleManager.ENGLISH
                else -> LocaleManager.SYSTEM
            }
            prefs.language = tag
            LocaleManager.apply(tag)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
