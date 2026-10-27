package com.tiznet.manager.ui.cleanip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tiznet.manager.R
import com.tiznet.manager.data.model.RealityState
import com.tiznet.manager.databinding.FragmentCleanIpBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.snack

class CleanIpFragment : BaseFragment() {

    private var _binding: FragmentCleanIpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanIpViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCleanIpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.reality.collectOnStarted { state ->
            if (state is UiState.Success) bindReality(state.data)
        }
        viewModel.networks.collectOnStarted { networks ->
            binding.networkList.text = if (networks.isEmpty()) "—"
            else networks.joinToString("\n") { "${it.cidr} · ${it.country} · ${if (it.ok) "ok" else "fail"}" }
        }

        binding.btnSaveReality.setOnClickListener {
            val state = RealityState(
                enabled = binding.realitySwitch.isChecked,
                sni = binding.inputSni.text?.toString().orEmpty(),
                dest = binding.inputDest.text?.toString().orEmpty()
            )
            viewModel.saveReality(state) { error ->
                binding.root.snack(error ?: getString(R.string.saved))
            }
        }
        binding.btnRunScan.setOnClickListener {
            val kind = if (binding.chipIp.isChecked) "ip" else "sni"
            viewModel.runScan(kind) { error ->
                binding.root.snack(error ?: getString(R.string.operation_success))
            }
        }
    }

    private fun bindReality(state: RealityState) {
        binding.realitySwitch.isChecked = state.enabled
        if (binding.inputSni.text.isNullOrBlank()) binding.inputSni.setText(state.sni)
        if (binding.inputDest.text.isNullOrBlank()) binding.inputDest.setText(state.dest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
