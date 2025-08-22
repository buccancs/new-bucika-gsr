package com.multisensor.recording.ui
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.multisensor.recording.R
import com.multisensor.recording.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ShimmerDeviceSelectionDialog : DialogFragment() {
    interface DeviceSelectionListener {
        fun onDeviceSelected(macAddress: String, deviceName: String)
        fun onSelectionCancelled()
    }
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val REQUEST_CONNECT_SHIMMER = 2
        fun newInstance(): ShimmerDeviceSelectionDialog {
            return ShimmerDeviceSelectionDialog()
        }
    }
    private lateinit var deviceListView: ListView
    private lateinit var scanButton: Button
    private lateinit var connectButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()
    private var selectedDevice: BluetoothDevice? = null
    private var deviceSelectionListener: DeviceSelectionListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Select Shimmer Device")
        return dialog
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_shimmer_device_selection, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupBluetoothAdapter()
        setupClickListeners()
        loadPairedDevices()
    }
    private fun initializeViews(view: View) {
        deviceListView = view.findViewById(R.id.device_list)
        scanButton = view.findViewById(R.id.scan_button)
        connectButton = view.findViewById(R.id.connect_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        progressBar = view.findViewById(R.id.progress_bar)
        statusText = view.findViewById(R.id.status_text)
        connectButton.isEnabled = false
    }
    private fun setupBluetoothAdapter() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            statusText.text = "Bluetooth not supported on this device"
            scanButton.isEnabled = false
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            statusText.text = "Please enable Bluetooth first"
            scanButton.isEnabled = false
            return
        }
        statusText.text = "Ready to scan for devices"
    }
    private fun setupClickListeners() {
        scanButton.setOnClickListener {
            startDeviceScan()
        }
        connectButton.setOnClickListener {
            selectedDevice?.let { device ->
                deviceSelectionListener?.onDeviceSelected(device.address, device.name ?: "Shimmer")
                dismiss()
            }
        }
        cancelButton.setOnClickListener {
            deviceSelectionListener?.onSelectionCancelled()
            dismiss()
        }
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            if (position < deviceList.size) {
                selectedDevice = deviceList[position]
                connectButton.isEnabled = true
                updateDeviceSelection(position)
            }
        }
    }
    private fun loadPairedDevices() {
        lifecycleScope.launch {
            try {
                val pairedDevices = withContext(Dispatchers.IO) {
                    bluetoothAdapter?.bondedDevices?.filter { device ->
                        val nameContainsShimmer = device.name?.contains("Shimmer", ignoreCase = true) == true
                        val nameContainsRN42 = device.name?.contains("RN42", ignoreCase = true) == true
                        nameContainsShimmer || nameContainsRN42
                    } ?: emptyList()
                }
                deviceList.clear()
                deviceNames.clear()
                pairedDevices.forEach { device ->
                    deviceList.add(device)
                    val displayName = "${device.name ?: "Unknown"} (${device.address})"
                    deviceNames.add(displayName)
                }
                updateDeviceListView()
                if (deviceList.isEmpty()) {
                    statusText.text = "No paired Shimmer devices found. Please pair your device first."
                } else {
                    statusText.text = "Found ${deviceList.size} paired Shimmer device(s)"
                }
            } catch (e: SecurityException) {
                statusText.text = "Permission error accessing Bluetooth devices"
            } catch (e: Exception) {
                statusText.text = "Error loading devices: ${e.message}"
            }
        }
    }
    private fun startDeviceScan() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                scanButton.isEnabled = false
                statusText.text = "Scanning for devices..."
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(2000)
                }
                loadPairedDevices()
            } catch (e: Exception) {
                statusText.text = "Scan failed: ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
                scanButton.isEnabled = true
            }
        }
    }
    private fun updateDeviceListView() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_single_choice,
            deviceNames
        )
        deviceListView.adapter = adapter
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }
    private fun updateDeviceSelection(position: Int) {
        deviceListView.setItemChecked(position, true)
        val selectedName = deviceNames[position]
        statusText.text = "Selected: $selectedName"
    }
    fun setDeviceSelectionListener(listener: DeviceSelectionListener) {
        this.deviceSelectionListener = listener
    }
    override fun onDestroy() {
        super.onDestroy()
        deviceSelectionListener = null
    }
}