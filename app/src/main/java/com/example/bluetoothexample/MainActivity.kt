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
import androidx.lifecycle.MutableLiveData


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private val listen : MutableLiveData<BluetoothAdapter> =  MutableLiveData<BluetoothAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        BtClass.bluetoothManager = turnOnBluetoothIfIsAvailable()
        BtClass.bluetoothManager?.let {
            bluetoothManager = it
        }
        listen.value = BtClass.bluetoothManager?.adapter
    }

    override fun onResume() {
        super.onResume()

        listen.observe(this, {
            turnOnBluetooth(bluetoothManager = bluetoothManager)
        })
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

    private fun turnOnBluetoothIfIsAvailable(): BluetoothManager{
        val (isAvailable, bluetoothManager) = isAdapterAvailable()

        if (isAvailable) {

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

            finish()
        }

        return bluetoothManager
    }
}