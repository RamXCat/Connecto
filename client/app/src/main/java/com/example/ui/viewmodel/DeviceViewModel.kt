package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.DeviceApiClient
import com.example.data.database.AppDatabase
import com.example.data.database.DeviceDao
import com.example.data.model.Device
import com.example.data.security.CryptoHelper
import com.example.data.storage.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DeviceOnlineStatus {
    object Unknown : DeviceOnlineStatus()
    object Checking : DeviceOnlineStatus()
    object Online : DeviceOnlineStatus()
    object Offline : DeviceOnlineStatus()
}

sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val platform: String,
    val lastSeen: Long = System.currentTimeMillis()
)

class DeviceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.deviceDao()
    val settings = SettingsManager(application)

    // Flow of all devices
    val allDevices: StateFlow<List<Device>> = dao.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of current active device
    val activeDevice: StateFlow<Device?> = dao.getActiveDevice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Device online statuses map (maps devId -> online/offline)
    val deviceStatuses = mutableStateMapOf<Int, DeviceOnlineStatus>()

    // Map of lowercase device name -> last UDP broadcast system time received
    val lastSeenBroadcastTime = mutableStateMapOf<String, Long>()

    // Flow of discovered local network devices
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var discoverySocket: java.net.DatagramSocket? = null

    // Toast message state flow to show alerts to user safely
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Command in progress state (controls button loading effect)
    private val _isCommandInProgress = MutableStateFlow(false)
    val isCommandInProgress = _isCommandInProgress.asStateFlow()

    init {
        // Automatically fetch online status for devices when active device changes
        viewModelScope.launch {
            activeDevice.collect { device ->
                device?.let { checkDeviceStatus(it) }
            }
        }
        // Start background network listening for UDP broadcasts
        startUdpDiscovery()
    }

    private fun startUdpDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = java.net.DatagramSocket(55555).apply {
                    reuseAddress = true
                    broadcast = true
                }
                discoverySocket = socket
                val buffer = ByteArray(2048)
                val packet = java.net.DatagramPacket(buffer, buffer.size)

                while (discoverySocket != null && !socket.isClosed) {
                    try {
                        socket.receive(packet)
                        val jsonStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        parseDiscoveryPacket(jsonStr)
                    } catch (e: java.net.SocketException) {
                        break
                    } catch (e: Exception) {
                        android.util.Log.e("UDP_DISCOVERY", "Error receiving packet: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UDP_DISCOVERY", "UDP server startup error: ${e.message}")
            }
        }
    }

    private fun parseDiscoveryPacket(jsonStr: String) {
        try {
            val json = org.json.JSONObject(jsonStr)
            val service = json.optString("service", "")
            if (service.equals("stitch", ignoreCase = true) || service.equals("connecto", ignoreCase = true)) {
                val name = json.getString("device")
                val ip = json.getString("ip")
                val port = json.getInt("port")
                val platform = json.optString("platform", "Windows")

                viewModelScope.launch {
                    val current = _discoveredDevices.value.toMutableList()
                    val existingIndex = current.indexOfFirst { it.ip == ip && it.port == port }
                    val dev = DiscoveredDevice(name, ip, port, platform)
                    if (existingIndex >= 0) {
                        current[existingIndex] = dev
                    } else {
                        current.add(0, dev)
                    }
                    _discoveredDevices.value = current

                    // Record UDP last seen timestamp
                    lastSeenBroadcastTime[name.trim().lowercase()] = System.currentTimeMillis()

                    // Auto-update IP address if saved device name matches and IP differs
                    val savedDevices = allDevices.value
                    val matchingSaved = savedDevices.find { it.name.trim().lowercase() == name.trim().lowercase() }
                    if (matchingSaved != null && matchingSaved.ipAddress != ip) {
                        val updated = matchingSaved.copy(ipAddress = ip)
                        withContext(Dispatchers.IO) {
                            dao.updateDevice(updated)
                        }
                        android.util.Log.d("UDP_DISCOVERY", "Auto-updated IP configuration for: $name -> $ip")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UDP_DISCOVERY", "Invalid discovery packet: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            discoverySocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    // Ping status endpoint of a device
    fun checkDeviceStatus(device: Device) {
        viewModelScope.launch {
            deviceStatuses[device.id] = DeviceOnlineStatus.Checking
            val decryptedToken = CryptoHelper.decrypt(device.encryptedToken, device.iv)
            val url = DeviceApiClient.buildUrl(device.ipAddress, "/status")
            val auth = DeviceApiClient.buildAuthHeader(decryptedToken)

            val isOnline = withContext(Dispatchers.IO) {
                try {
                    val response = DeviceApiClient.service.checkStatus(url, auth)
                    response.isSuccessful
                } catch (e: Exception) {
                    false
                }
            }

            deviceStatuses[device.id] = if (isOnline) {
                DeviceOnlineStatus.Online
            } else {
                DeviceOnlineStatus.Offline
            }
        }
    }

    // Refresh statuses of all saved devices
    fun refreshAllStatuses() {
        allDevices.value.forEach { device ->
            checkDeviceStatus(device)
        }
    }

    // Test connection helper (used in AddDevice screen step 1)
    fun testConnection(ip: String, token: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val url = DeviceApiClient.buildUrl(ip, "/status")
            val auth = DeviceApiClient.buildAuthHeader(token)

            val result = withContext(Dispatchers.IO) {
                try {
                    val response = DeviceApiClient.service.checkStatus(url, auth)
                    if (response.isSuccessful) {
                        Pair(true, "✅ Connection Established.")
                    } else if (response.code() == 401) {
                        Pair(false, "❌ Invalid Token. Check credentials.")
                    } else {
                        Pair(false, "❌ Failed (HTTP ${response.code()}).")
                    }
                } catch (e: Exception) {
                    Pair(false, "❌ Device unreachable. Check IP / router.")
                }
            }
            onResult(result.first, result.second)
        }
    }

    // Save device to Room DB including safe encryption
    fun addDevice(name: String, ipAddress: String, rawToken: String, type: String) {
        viewModelScope.launch {
            val (encrypted, iv) = CryptoHelper.encrypt(rawToken)
            val device = Device(
                name = name.trim(),
                ipAddress = ipAddress.trim(),
                encryptedToken = encrypted,
                iv = iv,
                type = type,
                isActive = allDevices.value.isEmpty() // Set active if first device
            )
            val newId = withContext(Dispatchers.IO) {
                dao.insertDevice(device)
            }
            if (device.isActive) {
                withContext(Dispatchers.IO) {
                    dao.setActiveDevice(newId.toInt())
                }
            }
            _toastEvent.emit("Device '$name' saved successfully")
        }
    }

    // Set active device
    fun selectActiveDevice(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.setActiveDevice(id)
            }
            _toastEvent.emit("Active device updated")
        }
    }

    // Delete device
    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteDevice(device)
            }
            deviceStatuses.remove(device.id)
            _toastEvent.emit("Device removed")
        }
    }

    // Trigger action command
    fun sendCommand(device: Device, action: String) {
        viewModelScope.launch {
            _isCommandInProgress.value = true
            val decryptedToken = CryptoHelper.decrypt(device.encryptedToken, device.iv)
            val endpointPath = when (action) {
                "shutdown" -> "/shutdown"
                "sleep" -> "/sleep"
                "restart" -> "/restart"
                "lock" -> "/lock"
                else -> "/status"
            }
            val url = DeviceApiClient.buildUrl(device.ipAddress, endpointPath)
            val auth = DeviceApiClient.buildAuthHeader(decryptedToken)

            val result: CommandResult = withContext(Dispatchers.IO) {
                try {
                    val response = when (action) {
                        "shutdown" -> DeviceApiClient.service.shutdown(url, auth)
                        "sleep" -> DeviceApiClient.service.sleep(url, auth)
                        "restart" -> DeviceApiClient.service.restart(url, auth)
                        "lock" -> DeviceApiClient.service.lock(url, auth)
                        else -> return@withContext CommandResult.Error("Unsupported command")
                    }

                    if (response.isSuccessful) {
                        CommandResult.Success(
                            when (action) {
                                "shutdown" -> "Shutdown command sent ✓"
                                "sleep" -> "Sleep command sent ✓"
                                "restart" -> "Restart command sent ✓"
                                "lock" -> "Lock command sent ✓"
                                else -> "Command executed successfully"
                            }
                        )
                    } else if (response.code() == 401) {
                        CommandResult.Error("Invalid token. Check settings.")
                    } else {
                        CommandResult.Error("Action failed (Code ${response.code()})")
                    }
                } catch (e: Exception) {
                    CommandResult.Error("Could not reach device")
                }
            }

            _isCommandInProgress.value = false
            when (result) {
                is CommandResult.Success -> {
                    _toastEvent.emit(result.message)
                    // If it was a shutdown, automatically flip device offline
                    if (action == "shutdown") {
                        deviceStatuses[device.id] = DeviceOnlineStatus.Offline
                    }
                }
                is CommandResult.Error -> {
                    _toastEvent.emit(result.message)
                }
            }
        }
    }

    // Save and activate QR scanned device
    fun addAndActivateScannedDevice(
        name: String,
        ipAddress: String,
        rawToken: String,
        type: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val url = DeviceApiClient.buildUrl(ipAddress, "/status")
            val auth = DeviceApiClient.buildAuthHeader(rawToken)

            val isOnline = withContext(Dispatchers.IO) {
                try {
                    val response = DeviceApiClient.service.checkStatus(url, auth)
                    response.isSuccessful
                } catch (e: Exception) {
                    false
                }
            }

            if (isOnline) {
                val (encrypted, iv) = CryptoHelper.encrypt(rawToken)
                val device = Device(
                    name = name.trim(),
                    ipAddress = ipAddress.trim(),
                    encryptedToken = encrypted,
                    iv = iv,
                    type = type,
                    isActive = true
                )
                
                withContext(Dispatchers.IO) {
                    val newId = dao.insertDevice(device)
                    dao.setActiveDevice(newId.toInt())
                }
                _toastEvent.emit("Device '$name' connected successfully")
                onResult(true, "✅ Connected!")
            } else {
                onResult(false, "❌ Device unreachable. Check laptop is running.")
            }
        }
    }
}
