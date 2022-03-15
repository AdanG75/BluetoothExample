package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BtClass(val handler: Handler) {
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    var mState: Int = STATE_NONE


    fun connectOutgoingDevice(btName: String?, btAddress: String?, btUUID: UUID?) {

        if (btName == null || btAddress == null || btUUID == null) {
            handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
        } else {
            mConnectedThread?.cancel()
            mConnectedThread = null

            mConnectThread?.cancel()
            mConnectThread = null

            mConnectThread = ConnectThread(btName, btAddress, btUUID)
            mState = STATE_CONNECTING
            mConnectThread?.run()
        }
    }

    fun stopBluetooth() {
        mConnectedThread?.cancel()
        mConnectedThread = null

        mConnectThread?.cancel()
        mConnectThread = null

        mState = STATE_NONE
        handler.obtainMessage(CONNECTING_STATUS, 2, -1).sendToTarget()
    }

    private fun startConnectedThread(socket: BluetoothSocket?): Unit {
        if (socket != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null

            mConnectedThread = ConnectedThread(socket)
            mConnectedThread?.start()
        } else {
            handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
        }
    }

    fun write(out: ByteArray) {
        // Create temporary object
        var r: ConnectedThread? = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) {
                return
            }
            mConnectedThread?.let {
                r = mConnectedThread
            }
        }
        // Perform the write un synchronized
        r?.let {
            it.write(out)
        }
    }

    private inner class ConnectThread(private val btName: String,
                                      private val btAddress: String,
                                      private val btUUID: UUID) : Thread() {

        private lateinit var bluetoothAdapter: BluetoothAdapter
        private val device: BluetoothDevice

        init {
            bluetoothManager?.let {
                bluetoothAdapter = it.adapter
            }
            device = bluetoothAdapter.getRemoteDevice(btAddress)
        }

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            // device.createRfcommSocketToServiceRecord(btUUID)
            device.createInsecureRfcommSocketToServiceRecord(btUUID)
        }

        override fun run() {
            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                //socket.connect()
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    mState = STATE_CONNECTED
                    Log.e("Status", "Device $btName connected")
                    handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
                } catch (connectException: IOException) {
                    mState = STATE_NONE
                    // Unable to connect; close the socket and return.
                    try {
                        socket.close()
                        Log.e("Status", "Cannot connect to device")
                        handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
                    } catch (closeException: IOException) {
                        stopBluetooth()
                        Log.e(TAG, "Could not close the client socket", closeException)
                    }
                    return
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                startConnectedThread(socket)
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

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var bytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (mState == STATE_CONNECTED) {
                // Read from the InputStream.
                try {
                    bytes = mmInStream.read(mmBuffer)
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, mmBuffer).sendToTarget()
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    stopBluetooth()
                    break
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
                handler.obtainMessage(MESSAGE_WRITE, 1, -1, mmBuffer).sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                stopBluetooth()
                handler.obtainMessage(MESSAGE_WRITE, -1, -1).sendToTarget()
                return
            }

            // Share the sent message with the UI activity.

        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

    }

    companion object {
        const val STATE_NONE: Int = 0 // we're doing nothing
        const val STATE_CONNECTING: Int = 1 // now initiating an outgoing connection
        const val STATE_CONNECTED: Int = 2 // now connected to a remote device
        const val HORIZONTAL_TAB: Char = '\t'

        var bluetoothManager: BluetoothManager? = null

        var possibleFingerprint: Boolean = false
        var firstMessage: Boolean = false
        var position: Int = 0
        var gettingFingerprintRawProcess: Boolean = false
    }
}