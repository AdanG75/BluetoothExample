package com.example.bluetoothexample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.util.*

public const val CONNECTING_STATUS: Int = 1
public const val MESSAGE_READ: Int = 2

class BtClass {
    private var mConnectThread: ConnectThread? = null

    public fun connectOutgoingDevice() {
        mConnectThread?.cancel()

        mConnectThread = ConnectThread()
        mConnectThread?.run()
    }

    public fun stop() {
        mConnectThread?.cancel()
        mConnectThread = null
    }

    private inner class ConnectThread() : Thread() {

        private lateinit var bluetoothAdapter: BluetoothAdapter
        private lateinit var device: BluetoothDevice
        init {
            bluetoothManager?.let {
                bluetoothAdapter = it.adapter
            }
            device = bluetoothAdapter.getRemoteDevice(btAddress)
        }

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            btUuid?.let {
                val uuidString: String = it.toString()
                val uuidDevice: UUID = UUID.fromString(uuidString)
                device.createRfcommSocketToServiceRecord(uuidDevice)
            }

        }

        public override fun run() {
            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                //socket.connect()
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    Log.e("Status", "Device connected")
                    handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
                } catch (connectException: IOException) {
                    // Unable to connect; close the socket and return.
                    try {
                        socket.close()
                        Log.e("Status", "Cannot connect to device")
                        handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
                    } catch (closeException: IOException) {
                        Log.e(TAG, "Could not close the client socket", closeException)
                    }
                    return
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.

                //manageMyConnectedSocket(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    companion object {
        public var bluetoothManager: BluetoothManager? = null
        public var btDeviceName: String? = null
        public var btAddress: String? = null
        public var btUuid: ParcelUuid? = null

        public val handler: Handler = Handler(Looper.getMainLooper()) { message ->
            when (message.what) {
                CONNECTING_STATUS -> {
                    when(message.arg1) {
                        1 -> {
                            Log.println(Log.INFO, "CONNECT", "Connect to $btDeviceName")
                            true
                        }
                        -1 -> {
                            Log.println(Log.INFO, "CONNECT_ERROR", "Device fails to connect")
                            false
                        }
                        else -> {
                            Log.println(Log.INFO, "CONNECT_ERROR_UNEXPECTED", "Unexpected error")
                            false
                        }
                    }
                }
                MESSAGE_READ -> {
                    val arduinoMessage: String = message.toString()
                    Log.println(Log.INFO, "Arduino Message", arduinoMessage)
                    true
                }
                else -> {
                    Log.println(Log.INFO, "UNEXPECTED_CASE", "Arduino Message")
                    false
                }
            }
        }


    }
}