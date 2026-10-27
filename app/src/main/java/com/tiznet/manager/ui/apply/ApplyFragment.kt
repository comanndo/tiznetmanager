package com.tiznet.manager.ui.apply

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tiznet.manager.R
import com.tiznet.manager.databinding.FragmentApplyBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.util.snack

class ApplyFragment : BaseFragment() {

    private var _binding: FragmentApplyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ApplyViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentApplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadState()

        binding.btnApply.setOnClickListener { viewModel.run("apply") }
        binding.btnInstall.setOnClickListener { viewModel.run("install") }
        binding.btnUpdate.setOnClickListener { viewModel.run("update") }
        binding.btnRestart.setOnClickListener { viewModel.run("restart") }
        binding.btnClearLog.setOnClickListener { viewModel.clearLog() }

        viewModel.state.collectOnStarted { state ->
            binding.stateText.text = buildString {
                append(getString(R.string.apply_title))
                if (state?.running?.isNotEmpty() == true) {
                    append(" · ").append(getString(R.string.apply_running))
                    append(" (").append(state.running.joinToString()).append(")")
                }
            }
        }
        viewModel.log.collectOnStarted { log ->
            binding.logText.text = log
            if (log.isNotBlank()) binding.root.post {
                binding.root.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
        viewModel.running.collectOnStarted { running ->
            listOf(binding.btnApply, binding.btnInstall, binding.btnUpdate, binding.btnRestart)
                .forEach { it.isEnabled = !running }
        }
        viewModel.error.collectOnStarted { error ->
            if (!error.isNullOrBlank()) {
                binding.root.snack(error)
                viewModel.consumeError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
