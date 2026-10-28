package com.tiznet.manager.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.databinding.FragmentLoginBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.util.applyImeInsets
import com.tiznet.manager.util.snack
import com.tiznet.manager.util.visible

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var stateView: StateView

    private val panelsAdapter by lazy {
        SavedPanelsAdapter(
            onClick = { viewModel.quickConnect(it) { navigateToDashboard() } },
            onMenu = { showPanelMenu(it) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyImeInsets()
        stateView = StateView(binding.stateView.root)
        binding.savedPanelsList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPanelsList.adapter = panelsAdapter

        binding.btnConnect.setOnClickListener { submit() }

        viewModel.panels.collectOnStarted { panels ->
            panelsAdapter.submitList(panels)
            binding.savedTitle.visible(panels.isNotEmpty())
        }
        viewModel.busy.collectOnStarted { busy ->
            if (busy) stateView.loading(getString(R.string.login_testing)) else stateView.content()
        }
        viewModel.error.collectOnStarted { message ->
            if (!message.isNullOrBlank()) {
                binding.root.snack(message)
                viewModel.consumeError()
            }
        }
    }

    private fun submit() {
        viewModel.connect(
            name = binding.inputPanelName.text?.toString().orEmpty(),
            url = binding.inputUrl.text?.toString().orEmpty(),
            proxyPath = binding.inputProxyPath.text?.toString().orEmpty(),
            username = binding.inputUsername.text?.toString().orEmpty(),
            password = binding.inputPassword.text?.toString().orEmpty()
        ) { navigateToDashboard() }
    }

    private fun navigateToDashboard() {
        findNavController().navigate(R.id.dashboardFragment)
    }

    private fun showPanelMenu(panel: PanelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(panel.name)
            .setItems(arrayOf(getString(R.string.panels_switch), getString(R.string.panels_delete))) { _, which ->
                when (which) {
                    0 -> viewModel.quickConnect(panel) { navigateToDashboard() }
                    1 -> confirmDelete(panel)
                }
            }.show()
    }

    private fun confirmDelete(panel: PanelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.panels_delete)
            .setMessage(R.string.panels_confirm_delete)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(panel) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
