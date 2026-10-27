package com.tiznet.manager.ui.backup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.BackupFile
import com.tiznet.manager.data.model.BackupState
import com.tiznet.manager.databinding.FragmentBackupBinding
import com.tiznet.manager.databinding.ItemBackupFileBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.formatBytes
import com.tiznet.manager.util.formatDate
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack
import kotlinx.coroutines.launch
import java.io.File

class BackupFragment : BaseFragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackupViewModel by viewModels()
    private lateinit var stateView: StateView
    private val filesAdapter = FilesAdapter { file ->
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.name)
            .setItems(arrayOf(getString(R.string.backup_restore), getString(R.string.action_delete))) { _, which ->
                if (which == 1) viewModel.deleteServerFile(file) { e ->
                    binding.root.snack(e ?: getString(R.string.deleted))
                }
            }.show()
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) showRestoreDialog(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.filesList.layoutManager = LinearLayoutManager(requireContext())
        binding.filesList.adapter = filesAdapter
        binding.content.hide()

        binding.btnCreate.setOnClickListener {
            viewModel.createBackup { error ->
                binding.root.snack(error ?: getString(R.string.operation_success))
            }
        }
        binding.btnDownload.setOnClickListener { downloadAndShare() }
        binding.btnTelegram.setOnClickListener {
            viewModel.sendTelegram { error ->
                binding.root.snack(error ?: getString(R.string.operation_success))
            }
        }
        binding.btnUpload.setOnClickListener { pickFile.launch("*/*") }

        viewModel.state.collectOnStarted { state ->
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    binding.content.show()
                    bind(state.data)
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun bind(state: BackupState) {
        binding.countsText.text = getString(
            R.string.backup_counts,
            state.current.users,
            state.current.admins,
            state.current.domains,
            state.current.nodes
        )
        binding.directoryText.text = state.directory
        filesAdapter.submitList(state.files)
    }

    private fun downloadAndShare() {
        viewLifecycleOwner.lifecycleScope.launch {
            val file = viewModel.downloadToCache()
            if (file == null) {
                binding.root.snack(getString(R.string.operation_failed))
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gzip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.backup_download)))
        }
    }

    private fun showRestoreDialog(uri: android.net.Uri) {
        val options = booleanArrayOf(true, true, true, false)
        val labels = arrayOf(
            getString(R.string.backup_options_settings),
            getString(R.string.backup_options_users),
            getString(R.string.backup_options_domains),
            getString(R.string.backup_options_owner)
        )
        val checked = booleanArrayOf(true, true, true, false)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup_restore)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> options[which] = isChecked }
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                val temp = File(requireContext().cacheDir, "restore-${System.currentTimeMillis()}")
                runCatching {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { input.copyTo(it) }
                    }
                    viewModel.restore(
                        temp,
                        settings = options[0],
                        users = options[1],
                        domains = options[2],
                        replaceOwner = options[3]
                    ) { error ->
                        binding.root.snack(error ?: getString(R.string.operation_success))
                    }
                }.onFailure { binding.root.snack(getString(R.string.operation_failed)) }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class FilesAdapter(
    private val onClick: (BackupFile) -> Unit
) : ListAdapter<BackupFile, FilesAdapter.VH>(DIFF) {

    inner class VH(val b: ItemBackupFileBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemBackupFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = getItem(position)
        with(holder.b) {
            fileName.text = file.name
            fileMeta.text = "${formatDate(file.created)} · ${formatBytes(file.size)}"
            root.setOnClickListener { onClick(file) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BackupFile>() {
            override fun areItemsTheSame(o: BackupFile, n: BackupFile) = o.name == n.name
            override fun areContentsTheSame(o: BackupFile, n: BackupFile) = o == n
        }
    }
}
