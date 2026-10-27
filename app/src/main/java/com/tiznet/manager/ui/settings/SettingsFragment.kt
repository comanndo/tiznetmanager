package com.tiznet.manager.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tiznet.manager.R
import com.tiznet.manager.data.model.SettingCategory
import com.tiznet.manager.data.model.SettingField
import com.tiznet.manager.databinding.FragmentSettingsBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.dp
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var stateView: StateView

    private var categories: List<SettingCategory> = emptyList()
    private val values = linkedMapOf<String, JsonElement>()
    private val fieldViews = mutableMapOf<String, View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.advancedSwitch.isChecked = prefs.showAdvancedSettings
        binding.advancedSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.showAdvancedSettings = checked
            rebuild()
        }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = rebuild()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
        binding.fabSave.setOnClickListener {
            viewModel.save(values) { error ->
                if (error == null) {
                    Snackbar.make(binding.root, getString(R.string.operation_success), Snackbar.LENGTH_LONG).show()
                    viewModel.lastSave.value?.warnings?.takeIf { it.isNotEmpty() }?.let { warnings ->
                        Snackbar.make(binding.root, warnings.joinToString("\n"), Snackbar.LENGTH_INDEFINITE).show()
                    }
                } else binding.root.snack(error)
            }
        }

        viewModel.state.collectOnStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    categories = state.data
                    seedValues()
                    rebuild()
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun seedValues() {
        values.clear()
        categories.forEach { cat ->
            cat.fields.forEach { field ->
                field.value?.let { values[field.key] = it }
            }
        }
    }

    private fun rebuild() {
        val container = binding.categoriesContainer
        container.removeAllViews()
        fieldViews.clear()
        val query = binding.searchInput.text?.toString().orEmpty().trim().lowercase()
        val showAdvanced = binding.advancedSwitch.isChecked

        categories.forEach { category ->
            val visibleFields = category.fields.filter { field ->
                (showAdvanced || field.essential) &&
                    (query.isBlank() ||
                        field.label.lowercase().contains(query) ||
                        field.key.lowercase().contains(query))
            }
            if (visibleFields.isEmpty()) return@forEach
            val card = MaterialCardView(requireContext()).apply {
                radius = dp(20).toFloat()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                useCompatPadding = true
            }
            val column = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16))
            }
            column.addView(TextView(requireContext()).apply {
                text = category.label
                TextViewCompat.setTextAppearance(
                    this,
                    com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
                )
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            if (category.description.isNotBlank()) {
                column.addView(TextView(requireContext()).apply {
                    text = category.description
                    TextViewCompat.setTextAppearance(
                        this,
                        com.google.android.material.R.style.TextAppearance_Material3_BodySmall
                    )
                    setTextColor(context.getColor(R.color.brand_400))
                })
            }
            visibleFields.forEach { field -> column.addView(buildField(field)) }
            card.addView(column)
            container.addView(card)
        }
        if (container.childCount == 0) stateView.empty() else stateView.content()
    }

    private fun buildField(field: SettingField): View {
        return when (field.type) {
            "bool" -> buildSwitch(field)
            "select" -> buildSelect(field)
            else -> buildText(field)
        }
    }

    private fun buildSwitch(field: SettingField): View {
        return MaterialSwitch(requireContext()).apply {
            text = field.label
            isChecked = (values[field.key] as? JsonPrimitive)?.booleanOrNull ?: false
            setOnCheckedChangeListener { _, checked ->
                values[field.key] = SettingsViewModel.primitive(checked)
            }
            if (field.description.isNotBlank()) {
                setOnLongClickListener { binding.root.snack(stripHtml(field.description)); true }
            }
            fieldViews[field.key] = this
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
    }

    private fun buildSelect(field: SettingField): View {
        val layout = TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = field.label
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        val dropdown = AutoCompleteTextView(requireContext()).apply {
            inputType = android.text.InputType.TYPE_NULL
            val choices = field.choices.orEmpty()
            setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, choices.map { it.label }))
            val current = (values[field.key] as? JsonPrimitive)?.contentOrNull
            setText(choices.firstOrNull { it.value == current }?.label ?: current.orEmpty(), false)
            setOnItemClickListener { _, _, position, _ ->
                values[field.key] = SettingsViewModel.primitive(choices[position].value)
            }
        }
        layout.addView(dropdown)
        fieldViews[field.key] = layout
        return layout
    }

    private fun buildText(field: SettingField): View {
        if (field.type == "html") {
            return TextView(requireContext()).apply {
                text = Html.fromHtml(field.description.ifBlank { field.label }, Html.FROM_HTML_MODE_COMPACT)
                TextViewCompat.setTextAppearance(
                    this,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
        }
        val layout = TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = field.label
            helperText = stripHtml(field.description).takeIf { it.isNotBlank() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        val input = TextInputEditText(requireContext()).apply {
            val current = (values[field.key] as? JsonPrimitive)?.contentOrNull
            setText(current.orEmpty())
            if (field.type == "textarea") {
                minLines = 3
                gravity = android.view.Gravity.TOP
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    values[field.key] = SettingsViewModel.primitive(s?.toString().orEmpty())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })
        }
        layout.addView(input)
        fieldViews[field.key] = layout
        return layout
    }

    private fun stripHtml(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
