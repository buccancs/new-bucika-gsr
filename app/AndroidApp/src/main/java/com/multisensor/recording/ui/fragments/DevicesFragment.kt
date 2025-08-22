package com.multisensor.recording.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.multisensor.recording.databinding.FragmentDevicesBinding
import com.multisensor.recording.ui.MainUiState
import com.multisensor.recording.ui.SystemHealthStatus
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.multisensor.recording.ui.MainViewModel

@AndroidEntryPoint
class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Toast.makeText(requireContext(), "Devices Fragment Loaded", Toast.LENGTH_SHORT).show()

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            connectDevicesButton.setOnClickListener {
                Toast.makeText(requireContext(), "Connect Devices clicked!", Toast.LENGTH_SHORT).show()
                viewModel.connectAllDevices()
            }

            scanDevicesButton.setOnClickListener {
                Toast.makeText(requireContext(), "Scan Devices clicked!", Toast.LENGTH_SHORT).show()
                viewModel.scanForDevices()
            }

            refreshDevicesButton.setOnClickListener {
                Toast.makeText(requireContext(), "Refresh clicked!", Toast.LENGTH_SHORT).show()
                viewModel.refreshSystemStatus()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: MainUiState) {
        binding.apply {
            pcConnectionStatus.text = when (state.systemHealth.pcConnection) {
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.CONNECTED -> "PC: Connected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.DISCONNECTED -> "PC: Disconnected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.ERROR -> "PC: Error"
                else -> "PC: Unknown"
            }

            shimmerConnectionStatus.text = when (state.systemHealth.shimmerConnection) {
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.CONNECTED -> "Shimmer: Connected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.DISCONNECTED -> "Shimmer: Disconnected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.ERROR -> "Shimmer: Error"
                else -> "Shimmer: Unknown"
            }

            thermalConnectionStatus.text = when (state.systemHealth.thermalCamera) {
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.CONNECTED -> "Thermal: Connected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.DISCONNECTED -> "Thermal: Disconnected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.ERROR -> "Thermal: Error"
                else -> "Thermal: Unknown"
            }

            networkConnectionStatus.text = when (state.systemHealth.networkConnection) {
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.CONNECTED -> "Network: Connected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.DISCONNECTED -> "Network: Disconnected"
                com.multisensor.recording.ui.SystemHealthStatus.HealthStatus.ERROR -> "Network: Error"
                else -> "Network: Unknown"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
