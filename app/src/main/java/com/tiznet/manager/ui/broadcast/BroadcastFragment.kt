package com.tiznet.manager.ui.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.tiznet.manager.R
import com.tiznet.manager.databinding.FragmentBroadcastBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack

class BroadcastFragment : BaseFragment() {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BroadcastViewModel by viewModels()
    private lateinit var stateView: StateView

    private val audiences = listOf(
        "all", "active", "expired", "offline_1h", "offline_1d", "offline_1w", "selected"
    )
    private val formats = listOf("plain", "markdown", "html")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.contentCard.hide()

        binding.dropdownAudience.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, audiences)
        )
        binding.dropdownAudience.setText(audiences.first(), false)
        binding.dropdownFormat.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, formats)
        )
        binding.dropdownFormat.setText(formats.first(), false)

        binding.btnSend.setOnClickListener {
            val text = binding.inputMessage.text?.toString().orEmpty()
            if (text.isBlank()) {
                binding.root.snack(getString(R.string.enter_value))
                return@setOnClickListener
            }
            viewModel.send(
                text = text,
                format = formats[binding.dropdownFormat.listSelection.coerceAtLeast(0)],
                audience = audiences[binding.dropdownAudience.listSelection.coerceAtLeast(0)]
            ) { error ->
                binding.root.snack(error ?: getString(R.string.operation_success))
                if (error == null) binding.inputMessage.text?.clear()
            }
        }

        viewModel.state.collectOnStarted { state ->
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    binding.contentCard.show()
                    binding.botStatus.text = if (state.data.bot)
                        "Telegram: @${state.data.botUsername}"
                    else getString(R.string.broadcast_bot_off)
                    binding.btnSend.isEnabled = state.data.bot
                }
                UiState.Idle -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
