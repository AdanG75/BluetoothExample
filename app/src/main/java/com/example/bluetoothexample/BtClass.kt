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

    fun stop() {
        mConnectedThread?.cancel()
        mConnectedThread = null

        mConnectThread?.cancel()
        mConnectThread = null

        mState = STATE_NONE
        handler.obtainMessage(CONNECTING_STATUS, 2, -1).sendToTarget()
    }

    private inner class ConnectThread(btName: String, btAddress: String, btUUID: UUID) : Thread() {

        private lateinit var bluetoothAdapter: BluetoothAdapter
        private val device: BluetoothDevice
        private val btName: String = btName

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
                        Log.e(TAG, "Could not close the client socket", closeException)
                    }
                    return
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                ConnectedThread(socket)
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
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
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

        var bluetoothManager: BluetoothManager? = null
    }
}