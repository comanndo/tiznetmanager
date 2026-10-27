package com.tiznet.manager.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.tiznet.manager.R
import com.tiznet.manager.databinding.FragmentUserEditBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack

class UserEditFragment : BaseFragment() {

    private var _binding: FragmentUserEditBinding? = null
    private val binding get() = _binding!!
    private val args: UserEditFragmentArgs by navArgs()
    private val viewModel: UserEditViewModel by viewModels()

    private val modes = listOf("no_reset", "monthly", "weekly", "daily")
    private var selectedOutbound: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isBulk = args.bulk
        binding.countLayout.visibility = if (isBulk) View.VISIBLE else View.GONE
        binding.nameLayout.hint =
            getString(if (isBulk) R.string.user_bulk_prefix else R.string.user_name)
        binding.uuidLayout.visibility = if (isBulk) View.GONE else View.VISIBLE
        binding.switchResetUsage.visibility = if (args.uuid != null) View.VISIBLE else View.GONE
        binding.switchResetDays.visibility = if (args.uuid != null) View.VISIBLE else View.GONE

        binding.dropdownMode.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                modes.map { modeLabel(it) }
            )
        )
        binding.dropdownMode.setText(modeLabel("no_reset"), false)

        viewModel.outbounds.collectOnStarted { outbounds ->
            val labels = listOf(getString(R.string.user_outbound_auto)) + outbounds.map { it.name }
            binding.dropdownOutbound.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
            )
            binding.dropdownOutbound.setText(labels.first(), false)
            binding.dropdownOutbound.setOnItemClickListener { _, _, position, _ ->
                selectedOutbound = if (position == 0) null else outbounds[position - 1].id
            }
        }

        viewModel.detail.collectOnStarted { detail ->
            if (detail != null && binding.inputName.text.isNullOrBlank()) {
                binding.inputName.setText(detail.name)
                binding.inputComment.setText(detail.comment)
                binding.inputUuid.setText(detail.uuid)
                binding.inputLimit.setText(detail.usageLimitGb.toBigDecimal().toPlainString())
                binding.inputDays.setText(detail.packageDays.toString())
                binding.inputTelegram.setText(detail.telegramId?.toString().orEmpty())
                binding.switchEnabled.isChecked = detail.enable
                binding.dropdownMode.setText(modeLabel(detail.mode), false)
                selectedOutbound = detail.preferredOutbound
            }
        }

        viewModel.busy.collectOnStarted { busy ->
            binding.btnSave.isEnabled = !busy
            binding.btnSave.text = if (busy) getString(R.string.state_loading) else getString(R.string.action_save)
        }

        binding.btnSave.setOnClickListener { submit(isBulk) }
        viewModel.load(args.uuid)
    }

    private fun submit(isBulk: Boolean) {
        val input = UserEditViewModel.Input(
            name = binding.inputName.text?.toString(),
            comment = binding.inputComment.text?.toString(),
            uuid = binding.inputUuid.text?.toString(),
            enabled = binding.switchEnabled.isChecked,
            limit = binding.inputLimit.text?.toString()?.toDoubleOrNull(),
            days = binding.inputDays.text?.toString()?.toIntOrNull(),
            mode = modes[binding.dropdownMode.listSelection.coerceAtLeast(0)],
            preferredOutbound = selectedOutbound,
            telegramId = binding.inputTelegram.text?.toString(),
            resetUsage = binding.switchResetUsage.isChecked,
            resetDays = binding.switchResetDays.isChecked,
            count = binding.inputCount.text?.toString()?.toIntOrNull() ?: 5,
            bulk = isBulk,
            existingUuid = args.uuid
        )
        if (input.name.isNullOrBlank()) {
            binding.root.snack(getString(R.string.enter_value))
            return
        }
        viewModel.submit(input) { error ->
            if (error == null) {
                binding.root.snack(getString(R.string.operation_success))
                findNavController().popBackStack()
            } else binding.root.snack(error)
        }
    }

    private fun modeLabel(mode: String) = when (mode) {
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
