package com.tiznet.manager.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.tiznet.manager.R
import com.tiznet.manager.data.model.DashboardNodeStats
import com.tiznet.manager.data.model.DashboardProcess
import com.tiznet.manager.data.model.DashboardResponse
import com.tiznet.manager.databinding.FragmentDashboardBinding
import com.tiznet.manager.databinding.ItemNodeMiniBinding
import com.tiznet.manager.databinding.ItemProcessBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.formatBytes
import com.tiznet.manager.util.formatGb
import com.tiznet.manager.util.formatPercent
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.uptimeText
import java.util.Locale

class DashboardFragment : BaseFragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var stateView: StateView
    private val processAdapter = ProcessAdapter()
    private val nodeAdapter = NodeAdapter()

    private val ranges = listOf(7, 14, 30, 90, 180, 365)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.processesList.layoutManager = LinearLayoutManager(requireContext())
        binding.processesList.adapter = processAdapter
        binding.nodesList.layoutManager = LinearLayoutManager(requireContext())
        binding.nodesList.adapter = nodeAdapter

        ranges.forEach { days ->
            val chip = Chip(requireContext()).apply {
                text = "$days " + getString(R.string.days)
                isCheckable = true
                id = days
                setOnClickListener { viewModel.setDays(days) }
            }
            binding.rangeChips.addView(chip)
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.nodesCard.setOnClickListener {
            findNavController().navigate(R.id.nodesFragment)
        }

        viewModel.days.collectOnStarted { days ->
            for (i in 0 until binding.rangeChips.childCount) {
                val chip = binding.rangeChips.getChildAt(i) as Chip
                chip.isChecked = chip.id == days
            }
        }
        viewModel.state.collectOnStarted { state -> render(state) }
    }

    private fun render(state: UiState<DashboardResponse>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            UiState.Loading -> {
                stateView.loading()
                binding.content.hide()
            }
            is UiState.Error -> {
                stateView.error(state.message) { viewModel.load() }
                binding.content.hide()
            }
            is UiState.Success -> {
                stateView.content()
                binding.content.show()
                bindData(state.data)
            }
            UiState.Idle -> Unit
        }
    }

    private fun bindData(data: DashboardResponse) {
        val panel = session.activePanel.value
        binding.panelName.text = panel?.name ?: getString(R.string.app_name)
        binding.panelVersion.text =
            data.system.host.panelVersion?.let { "${getString(R.string.dash_panel_version)}: v$it" }
                ?: session.bootstrap.value?.panelVersion?.let { "v$it" }.orEmpty()

        binding.statUsers.statValue.text = data.users.total.toString()
        binding.statUsers.statLabel.text = getString(R.string.dash_users_total)
        binding.statActive.statValue.text = data.users.active.toString()
        binding.statActive.statLabel.text = getString(R.string.dash_users_active)
        binding.statOnline.statValue.text = data.users.online.h24.toString()
        binding.statOnline.statLabel.text = getString(R.string.dash_users_online)

        binding.usageChart.usage = data.series.map { it.usage }
        binding.usageChart.online = data.series.map { it.online }

        binding.statToday.statIcon.setImageResource(R.drawable.ic_speed)
        binding.statToday.statValue.text = formatBytes(data.usage.totals.today)
        binding.statToday.statLabel.text = getString(R.string.dash_usage_today)
        binding.statWeek.statIcon.setImageResource(R.drawable.ic_calendar)
        binding.statWeek.statValue.text = formatBytes(data.usage.totals.week)
        binding.statWeek.statLabel.text = getString(R.string.dash_usage_week)
        binding.statMonth.statIcon.setImageResource(R.drawable.ic_chart)
        binding.statMonth.statValue.text = formatBytes(data.usage.totals.month)
        binding.statMonth.statLabel.text = getString(R.string.dash_usage_month)
        binding.statTotal.statIcon.setImageResource(R.drawable.ic_database)
        binding.statTotal.statValue.text = formatBytes(data.usage.totals.total)
        binding.statTotal.statLabel.text = getString(R.string.dash_usage_total)

        with(data.system) {
            binding.ringCpu.setProgress(cpu.percent)
            binding.ringCpu.label = formatPercent(cpu.percent)
            binding.ringMemory.setProgress(memory.percent, R.color.status_active)
            binding.ringMemory.label = formatPercent(memory.percent)
            binding.ringDisk.setProgress(disk.percent, R.color.status_no_data)
            binding.ringDisk.label = formatPercent(disk.percent)

            binding.networkInfo.text = buildString {
                append("${getString(R.string.dash_recv)}: ${formatGb(network.recvGb)}    ")
                append("${getString(R.string.dash_sent)}: ${formatGb(network.sentGb)}\n")
                append("${getString(R.string.dash_connections)}: ${network.connections}    ")
                append("${getString(R.string.dash_unique_ips)}: ${network.uniqueIps}")
            }
            binding.uptimeInfo.text =
                "${getString(R.string.dash_uptime)}: ${uptimeText(host.uptimeS)} · ${host.hostname}"
        }

        processAdapter.submitList(data.processes.cpu.take(8))
        nodeAdapter.submitList(data.nodeStats)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ProcessAdapter :
        ListAdapter<DashboardProcess, ProcessAdapter.VH>(DIFF) {
        inner class VH(val b: ItemProcessBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemProcessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = getItem(position)
            holder.b.processName.text = p.name
            holder.b.processCpu.text = String.format(Locale.US, "%.1f%%", p.cpuPercent ?: p.percent)
            holder.b.processMemory.text =
                p.memoryGb?.let { String.format(Locale.US, "%.2f GB", it) } ?: ""
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<DashboardProcess>() {
                override fun areItemsTheSame(o: DashboardProcess, n: DashboardProcess) =
                    o.name == n.name && o.path == n.path

                override fun areContentsTheSame(o: DashboardProcess, n: DashboardProcess) = o == n
            }
        }
    }

    private class NodeAdapter :
        ListAdapter<DashboardNodeStats, NodeAdapter.VH>(DIFF) {
        inner class VH(val b: ItemNodeMiniBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemNodeMiniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val node = getItem(position)
            holder.b.nodeName.text = node.name
            holder.b.nodeStatus.text = if (node.ok) "OK" else node.error ?: "ERR"
            holder.b.nodeDot.setBackgroundResource(
                if (node.ok) R.drawable.bg_dot_online else R.drawable.bg_dot_offline
            )
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<DashboardNodeStats>() {
                override fun areItemsTheSame(o: DashboardNodeStats, n: DashboardNodeStats) =
                    o.id == n.id

                override fun areContentsTheSame(o: DashboardNodeStats, n: DashboardNodeStats) = o == n
            }
        }
    }
}
