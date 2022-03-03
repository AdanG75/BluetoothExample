package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController

class ControlBluetoothFragment : Fragment(R.layout.fragment_control_bluetooth) {

    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothAddress: String? = null
    var handler: Handler? = null
    private val btClass: BtClass = BtClass()
    lateinit var outputText: TextView
    lateinit var connectButton: Button
    lateinit var sendButton: Button
    lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        arguments?.let {
//            bluetoothAdapter = it.get("bluetoothAdapter") as BluetoothAdapter
//        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectButton = view.findViewById(R.id.b_paired_device)
        outputText = view.findViewById(R.id.tv_output_bluetooth)
        sendButton = view.findViewById(R.id.b_send)
        inputText = view.findViewById(R.id.et_input_bluetooth)

        catchBluetoothAddress()

        connectButton.setOnClickListener {
            showPairedDevices()
        }
    }

    private fun showPairedDevices() {
        findNavController()
            .navigate(R.id.action_controlBluetoothFragment_to_pairedDevicesFragment)
    }

    private fun catchBluetoothAddress() {
        //Listener
        setFragmentResultListener("BluetoothAddress") { key, bundle ->
            bluetoothAddress = bundle.getString("btAddressKey")

            val btUUIDs: Array<ParcelUuid>? = bundle.get("btUUIDsKey") as Array<ParcelUuid>?

            if (btUUIDs != null) {
                if (btUUIDs.isNotEmpty()) {
                    BtClass.btUuid = btUUIDs[0]
//                    btUUIDs!!.forEach {
//                        Log.println(Log.INFO, "UUID", it.toString())
//                    }
                    btClass.connectOutgoingDevice()
                }
            }

            Toast.makeText(view?.context, bluetoothAddress, Toast.LENGTH_LONG).show()
        }
    }

    public fun setOutputText(msg: String) {
        outputText.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        btClass?.stop()
    }
}