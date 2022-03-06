package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import java.util.*

const val CONNECTING_STATUS: Int = 1
const val MESSAGE_READ: Int = 2
const val MESSAGE_WRITE: Int = 3

class ControlBluetoothFragment : Fragment(R.layout.fragment_control_bluetooth) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothAddress: String? = null
    private var bluetoothName: String? = null
    private var bluetoothUuid: UUID? = null

    private lateinit var btClass: BtClass
    private lateinit var mHandler: Handler
    private lateinit var outputText: TextView
    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothAdapter = BtClass.bluetoothManager?.adapter
        mHandler = handler
        btClass = BtClass(mHandler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectButton = view.findViewById(R.id.b_paired_device)
        outputText = view.findViewById(R.id.tv_output_bluetooth)
        sendButton = view.findViewById(R.id.b_send)
        inputText = view.findViewById(R.id.et_input_bluetooth)

        connectWithOtherDevice()

        connectButton.setOnClickListener {
            if (btClass.mState == BtClass.STATE_NONE) {
                showPairedDevices()
            } else {
                btClass.stop()
            }
        }
    }

    private fun showPairedDevices() {
        findNavController()
            .navigate(R.id.action_controlBluetoothFragment_to_pairedDevicesFragment)
    }

    private fun connectWithOtherDevice() {
        //Listener
        setFragmentResultListener("BluetoothInformation") { _, bundle ->
            bluetoothAddress = bundle.getString("btAddressKey")
            bluetoothName = bundle.getString("btNameKey")
            val uuidsParcel: Array<ParcelUuid>? = bundle.get("btUUIDsKey") as Array<ParcelUuid>?

            uuidsParcel?.let {
                val uuidString : String = it[0].toString()
                bluetoothUuid = UUID.fromString(uuidString)
            }

            btClass.connectOutgoingDevice(bluetoothName, bluetoothAddress, bluetoothUuid)


            // Toast.makeText(view?.context, "Connecting with $bluetoothName", Toast.LENGTH_LONG).show()
        }
    }

    fun setOutputText(msg: String) {
        outputText.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        btClass.stop()
    }

    private val handler: Handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            CONNECTING_STATUS -> {
                when(message.arg1) {
                    1 -> {

                        checkState(BtClass.STATE_CONNECTED, R.string.disconnect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "CONNECT", "Connect to $bluetoothName")
                        true
                    }
                    2 -> {
                        checkState(BtClass.STATE_NONE, R.string.connect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "DISCONNECT", "Disconnect to $bluetoothName")
                        true
                    }
                    -1 -> {
                        checkState(BtClass.STATE_NONE, R.string.connect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "CONNECT_ERROR", "Device fails to connect")
                        false
                    }
                    else -> {
                        checkState(BtClass.STATE_NONE, R.string.connect, lambdaChangeTextConnectButton)
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
            MESSAGE_WRITE -> {
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



    private val lambdaChangeTextConnectButton: (Int) -> Unit = {
        value -> connectButton.text = getString(value)
    }

    private fun checkState(state: Int, idString: Int, block: (Int) -> Unit) {
        if (btClass.mState == state) {
            block(idString)
        }
    }
}