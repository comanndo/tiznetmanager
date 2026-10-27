package com.tiznet.manager.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.UNLIMITED_DAYS
import com.tiznet.manager.data.model.UNLIMITED_GB
import com.tiznet.manager.data.model.UserDetail
import com.tiznet.manager.data.remote.RetrofitFactory
import com.tiznet.manager.databinding.FragmentUserDetailBinding
import com.tiznet.manager.databinding.ItemInfoRowBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.QrUtil
import com.tiznet.manager.util.copyToClipboard
import com.tiznet.manager.util.formatBytes
import com.tiznet.manager.util.formatDate
import com.tiznet.manager.util.formatGb
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack

class UserDetailFragment : BaseFragment() {

    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!
    private val args: UserDetailFragmentArgs by navArgs()
    private val viewModel: UserDetailViewModel by viewModels()
    private lateinit var stateView: StateView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.content.hide()
        viewModel.load(args.uuid)

        viewModel.state.collectOnStarted { state ->
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load(args.uuid) }
                is UiState.Success -> {
                    stateView.content()
                    binding.content.show()
                    bind(state.data)
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun bind(user: UserDetail) {
        binding.userName.text = user.name.ifBlank { user.uuid.take(8) }
        binding.userStatus.text = statusText(user.status)
        binding.enableSwitch.isChecked = user.enable
        binding.enableSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != user.enable) viewModel.toggle(user)
        }
        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.userEditFragment,
                bundleOf("uuid" to user.uuid, "bulk" to false)
            )
        }
        binding.btnResetUsage.setOnClickListener {
            viewModel.resetUsage(user) { error ->
                error?.let { binding.root.snack(it) } ?: binding.root.snack(getString(R.string.saved))
            }
        }
        binding.btnDelete.setOnClickListener { confirmDelete(user) }

        val panel = session.requirePanel()
        val link = buildString {
            append(RetrofitFactory.normalizeBaseUrl(panel.baseUrl))
            append("/").append(user.uuid).append("/#").append(user.name)
        }
        binding.subLink.text = link
        binding.qrImage.post {
            QrUtil.generate(link, 600)?.let { binding.qrImage.setImageBitmap(it) }
        }
        binding.btnCopy.setOnClickListener {
            requireContext().copyToClipboard("subscription", link)
            binding.root.snack(getString(R.string.action_copied))
        }
        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, link)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
        }

        binding.infoContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        fun addRow(label: String, value: String) {
            val row = ItemInfoRowBinding.inflate(inflater, binding.infoContainer, false)
            row.infoLabel.text = label
            row.infoValue.text = value
            binding.infoContainer.addView(row.root)
        }
        addRow(getString(R.string.user_detail_status), statusText(user.status))
        addRow(
            getString(R.string.user_detail_usage),
            "${formatGb(user.currentUsageGb)} / " +
                if (user.usageLimitGb >= UNLIMITED_GB) getString(R.string.user_unlimited)
                else formatGb(user.usageLimitGb)
        )
        addRow(
            getString(R.string.user_detail_remaining_days),
            if (user.packageDays >= UNLIMITED_DAYS) getString(R.string.user_unlimited)
            else "${user.remainingDays} ${getString(R.string.days)}"
        )
        addRow(getString(R.string.user_detail_expire), formatDate(user.expireDate))
        addRow(getString(R.string.user_detail_start), formatDate(user.startDate))
        addRow(getString(R.string.user_detail_last_online),
            user.lastOnline?.let { com.tiznet.manager.util.relativeTime(it) } ?: getString(R.string.user_never))
        addRow(getString(R.string.user_detail_last_reset), formatDate(user.lastResetTime))
        addRow(getString(R.string.user_detail_owner), user.ownerName.ifBlank { "—" })
        addRow(getString(R.string.user_mode), modeText(user.mode))
        addRow("UUID", user.uuid)
    }

    private fun confirmDelete(user: UserDetail) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(R.string.users_confirm_delete)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.delete(user.uuid) { error ->
                    if (error == null) findNavController().popBackStack()
                    else binding.root.snack(error)
                }
            }.show()
    }

    private fun statusText(status: String) = when (status) {
        "active" -> getString(R.string.status_active)
        "disabled" -> getString(R.string.status_disabled)
        "expired" -> getString(R.string.status_expired)
        "no_data" -> getString(R.string.status_no_data)
        else -> status
    }

    private fun modeText(mode: String) = when (mode) {
        "no_reset" -> getString(R.string.user_mode_no_reset)
        "monthly" -> getString(R.string.user_mode_monthly)
        "weekly" -> getString(R.string.user_mode_weekly)
        "daily" -> getString(R.string.user_mode_daily)
        else -> mode
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
