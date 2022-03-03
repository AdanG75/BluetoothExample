package com.example.bluetoothexample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController


class MainActivity : AppCompatActivity() {

//    lateinit var bluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        BtClass.bluetoothManager = turnOnBluetoothIfIsAvailable()

//        if (bluetoothManager != null) {
//            val bundle: Bundle = bundleOf("bluetoothAdapter" to bluetoothManager.adapter)
//
//            val myFragment: ControlBluetoothFragment = ControlBluetoothFragment()
//            myFragment.arguments = bundle
//        }


        //Bundle needs a parcelable object and Bluetooth Manager class isn't parcelable
//        val bundle: Bundle = bundleOf("bluetoothManager" to bluetoothManager)
//
//        val myFragment: ControlBluetoothFragment = ControlBluetoothFragment()
//        myFragment.arguments = bundle

        //Failed to pass bluetoothManager to controlBluetoothFragment
//        Navigation
//            .findNavController(this, R.id.controlBluetoothFragment)
//            .navigate(R.id.controlBluetoothFragment, bundle)
    }

    private fun isAdapterAvailable(): Pair<Boolean, BluetoothManager> {
        val bluetoothManager: BluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothManager.adapter ?: return Pair(false, bluetoothManager)

        return Pair(true, bluetoothManager)
    }

    private fun turnOnBluetooth(bluetoothManager: BluetoothManager): Boolean {
        if (!bluetoothManager.adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getBtResult.launch(enableBtIntent)
            return true
        }
        return false
    }

    private val getBtResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                this,
                R.string.bluetooth_turn_on,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                R.string.error_bluetooth_not_turn_on,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    public fun turnOnBluetoothIfIsAvailable(): BluetoothManager{
        val (isAvailable, bluetoothManager) = isAdapterAvailable()

        if (isAvailable) {

//            Toast.makeText(
//                this,
//                R.string.bluetooth_available,
//                Toast.LENGTH_LONG
//            ).show()

            if (!turnOnBluetooth(bluetoothManager)) {
                Toast.makeText(
                    this,
                    R.string.bluetooth_already_on,
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                R.string.error_bluetooth_not_supported,
                Toast.LENGTH_LONG
            ).show()
        }

        return bluetoothManager
    }
}