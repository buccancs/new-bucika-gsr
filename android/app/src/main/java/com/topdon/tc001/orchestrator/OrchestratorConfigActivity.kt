package com.topdon.tc001.orchestrator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ActivityOrchestratorConfigBinding

class OrchestratorConfigActivity : AppCompatActivity(), OrchestratorService.ServiceListener {
    
    companion object {
        private const val TAG = "OrchestratorConfig"
    }

    private lateinit var binding: ActivityOrchestratorConfigBinding
    private var orchestratorService: OrchestratorService? = null
    private var serviceBound = false
    
    private lateinit var discoveredServicesAdapter: DiscoveredServicesAdapter
    private val discoveredServices = mutableListOf<NsdServiceInfo>()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as OrchestratorService.OrchestratorBinder
            orchestratorService = binder.getService()
            orchestratorService?.setServiceListener(this@OrchestratorConfigActivity)
            serviceBound = true
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            orchestratorService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrchestratorConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        bindToService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            orchestratorService?.setServiceListener(null)
            unbindService(serviceConnection)
        }
    }

    private fun setupUI() {
        binding.toolbar.title = "PC Orchestrator"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        discoveredServicesAdapter = DiscoveredServicesAdapter(discoveredServices) { service ->
            connectToService(service)
        }
        binding.recyclerDiscovered.adapter = discoveredServicesAdapter
        binding.recyclerDiscovered.layoutManager = LinearLayoutManager(this)

        binding.editServerUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateConnectButton()
            }
        })

        binding.btnDiscover.setOnClickListener {
            startDiscovery()
        }

        binding.btnConnect.setOnClickListener {
            connectManually()
        }

        binding.btnDisconnect.setOnClickListener {
            disconnect()
        }

        updateConnectButton()
    }

    private fun bindToService() {
        val intent = Intent(this, OrchestratorService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUI() {
        val service = orchestratorService
        if (service != null) {
            val isConnected = service.isConnected()
            val serverUrl = service.getServerUrl()
            
            binding.textStatus.text = if (isConnected) {
                "Connected to: $serverUrl"
            } else {
                "Not connected"
            }
            
            binding.layoutDiscovery.visibility = if (isConnected) View.GONE else View.VISIBLE
            binding.layoutManualConnect.visibility = if (isConnected) View.GONE else View.VISIBLE
            binding.btnDisconnect.visibility = if (isConnected) View.VISIBLE else View.GONE
            
            discoveredServices.clear()
            discoveredServices.addAll(service.getDiscoveredServices())
            discoveredServicesAdapter.notifyDataSetChanged()
            
            binding.textDiscoveredCount.text = "${discoveredServices.size} orchestrator(s) found"
        }
    }

    private fun updateConnectButton() {
        val url = binding.editServerUrl.text.toString().trim()
        val isValidUrl = url.startsWith("ws://") && url.contains(":") && url.length > 10
        binding.btnConnect.isEnabled = isValidUrl && orchestratorService?.isConnected() != true
    }

    private fun startDiscovery() {
        Log.i(TAG, "Starting orchestrator discovery")
        binding.progressDiscovery.visibility = View.VISIBLE
        binding.btnDiscover.isEnabled = false
        binding.textStatus.text = "Discovering orchestrators..."
        
        val intent = Intent(this, OrchestratorService::class.java).apply {
            action = OrchestratorService.ACTION_CONNECT
        }
        startService(intent)
        
        binding.btnDiscover.postDelayed({
            binding.btnDiscover.isEnabled = true
            binding.progressDiscovery.visibility = View.GONE
        }, 5000)
    }

    private fun connectToService(service: NsdServiceInfo) {
        Log.i(TAG, "Connecting to discovered service: ${service.serviceName}")
        
        val serverUrl = "ws://${service.host.hostAddress}:${service.port}"
        binding.editServerUrl.setText(serverUrl)
        connectManually()
    }

    private fun connectManually() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        
        if (serverUrl.isEmpty()) {
            showError("Please enter a server URL")
            return
        }
        
        if (!serverUrl.startsWith("ws://")) {
            showError("URL must start with ws://")
            return
        }
        
        Log.i(TAG, "Manually connecting to: $serverUrl")
        binding.textStatus.text = "Connecting to $serverUrl..."
        
        val intent = Intent(this, OrchestratorService::class.java).apply {
            action = OrchestratorService.ACTION_MANUAL_CONNECT
            putExtra(OrchestratorService.EXTRA_SERVER_URL, serverUrl)
        }
        startService(intent)
    }

    private fun disconnect() {
        Log.i(TAG, "Disconnecting from orchestrator")
        
        val intent = Intent(this, OrchestratorService::class.java).apply {
            action = OrchestratorService.ACTION_DISCONNECT
        }
        startService(intent)
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onConnected() {
        runOnUiThread {
            updateUI()
            showSuccess("Connected to orchestrator")
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            updateUI()
            showError("Disconnected from orchestrator")
        }
    }

    override fun onRegistered(role: String) {
        runOnUiThread {
            updateUI()
            showSuccess("Registered as $role")
        }
    }

    override fun onConnectionError(error: String) {
        runOnUiThread {
            binding.textStatus.text = "Connection error"
            showError("Connection error: $error")
        }
    }

    override fun onDiscoveryError(error: String) {
        runOnUiThread {
            binding.progressDiscovery.visibility = View.GONE
            binding.btnDiscover.isEnabled = true
            binding.textStatus.text = "Discovery failed"
            showError("Discovery error: $error")
        }
    }

    override fun onStartSession(sessionId: String, config: Map<String, Any>) {
        runOnUiThread {
            showSuccess("Recording session started")
        }
    }

    override fun onStopSession() {
        runOnUiThread {
            showSuccess("Recording session stopped")
        }
    }

    override fun onSyncMark(markerId: String, referenceTime: Long) {
        runOnUiThread {
            showSuccess("Sync mark: $markerId")
        }
    }

    override fun onError(errorCode: String, message: String, details: Map<String, Any>) {
        runOnUiThread {
            showError("$errorCode: $message")
        }
    }

    private class DiscoveredServicesAdapter(
        private val services: List<NsdServiceInfo>,
        private val onServiceClick: (NsdServiceInfo) -> Unit
    ) : RecyclerView.Adapter<DiscoveredServicesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_discovered_service, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val service = services[position]
            holder.bind(service, onServiceClick)
        }

        override fun getItemCount(): Int = services.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textName: TextView = itemView.findViewById(R.id.textServiceName)
            private val textAddress: TextView = itemView.findViewById(R.id.textServiceAddress)
            private val btnConnect: Button = itemView.findViewById(R.id.btnConnectService)

            fun bind(service: NsdServiceInfo, onConnect: (NsdServiceInfo) -> Unit) {
                textName.text = service.serviceName ?: "Unknown Service"
                textAddress.text = "${service.host?.hostAddress ?: "Unknown"}:${service.port}"
                
                btnConnect.setOnClickListener {
                    onConnect(service)
                }
            }
        }
    }
