package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import java.util.*


class PairedDevicesFragment : Fragment(R.layout.fragment_paired_devices) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val mapUUID: MutableMap<String, Array<ParcelUuid>?> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val bluetoothManager: BluetoothManager =
//            Activity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//
//        if (bluetoothManager != null) {
//            bluetoothAdapter = bluetoothManager.adapter
//        }

        bluetoothAdapter = BtClass.bluetoothManager?.adapter

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pairedDevicesArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(view.context, R.layout.device_name)

        val pairedListView: ListView = view.findViewById(R.id.paired_devices)
        pairedListView.adapter = pairedDevicesArrayAdapter
        pairedListView.onItemClickListener = mDeviceClickListener



        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        if (pairedDevices == null || pairedDevices.isEmpty()){
//            Toast.makeText(
//                view.context,
//                "There aren't paired devices",
//                Toast.LENGTH_LONG
//            ).show()

            val noDevices: String = getString(R.string.no_devices_paired)
            pairedDevicesArrayAdapter.add(noDevices)
        }

        pairedDevices?.forEach { device ->
            pairedDevicesArrayAdapter.add("${device.name}\n${device.address}")

            val uuidsDevice: Array<ParcelUuid>? = device.uuids

            if (uuidsDevice != null) {
                if (uuidsDevice.isNotEmpty()) {
                    mapUUID[device.name.toString()] = uuidsDevice
//                    uuidsDevice.forEach {
//                        Log.println(Log.INFO, "UUID ${device.name}", it.toString())
//                    }
                }
            }
//            val deviceName: String = device.name
//            val deviceHardwareAddress: String = device.address // MAC address
        }
    }

    private val mDeviceClickListener =
        OnItemClickListener { _, v, _, _ -> // Cancel discovery because it's costly and we're about to connect

            // Get the device MAC address, which is the last 17 chars in the View
            val info: String = (v as TextView).text.toString()
            val address: String = info.substring(info.length - 17)
            val name: String = info.substring(0, info.length - 18)

            BtClass.btDeviceName = name
            BtClass.btAddress = address

            val uuidsDevice:Array<ParcelUuid>? = mapUUID[name]

//            if (uuidsDevice != null) {
//                if (uuidsDevice.isNotEmpty()) {
//                    uuidsDevice.forEach {
//                        Log.println(Log.INFO, "UUID ${name}", it.toString())
//                    }
//                }
//            }


//            Log.println(Log.INFO, "BtName", name)

//            Toast.makeText(view?.context, address, Toast.LENGTH_LONG).show()

            setFragmentResult(
                "BluetoothAddress",
                bundleOf(
                    "btAddressKey" to address,
                    "btUUIDsKey" to uuidsDevice
                )
            )

            activity?.onBackPressed() // To return to FirstFragment
        }

}