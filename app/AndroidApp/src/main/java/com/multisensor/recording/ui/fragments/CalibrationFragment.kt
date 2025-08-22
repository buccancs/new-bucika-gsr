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
import com.multisensor.recording.databinding.FragmentCalibrationBinding
import com.multisensor.recording.ui.MainUiState
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.multisensor.recording.ui.MainViewModel

@AndroidEntryPoint
class CalibrationFragment : Fragment() {

    private var _binding: FragmentCalibrationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Toast.makeText(requireContext(), "Calibration Fragment Loaded", Toast.LENGTH_SHORT).show()

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            startCalibrationButton.setOnClickListener {
                viewModel.startCalibration()
            }

            stopCalibrationButton.setOnClickListener {
                viewModel.stopCalibration()
            }

            saveCalibrationButton.setOnClickListener {
                viewModel.saveCalibration()
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
            calibrationStatusText.text = when {
                state.isCalibrating -> "Calibration in progress..."
                state.calibrationComplete -> "Calibration complete"
                else -> "Ready for calibration"
            }

            startCalibrationButton.isEnabled = !state.isCalibrating
            stopCalibrationButton.isEnabled = state.isCalibrating
            saveCalibrationButton.isEnabled = state.calibrationComplete
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
