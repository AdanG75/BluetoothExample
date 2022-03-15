package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import java.util.*

const val CONNECTING_STATUS: Int = 1
const val MESSAGE_READ: Int = 2
const val MESSAGE_WRITE: Int = 3

val setupRegex = """(?i).*nd image t.*""".toRegex()
val waitingRegex = """(?i).*ting for a.*""".toRegex()
val imageTakenRegex = """(?i).*ge tak.*""".toRegex()

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

    private var mOutStringBuffer: StringBuffer? = null
    private var mFingerprintBuffer: UByteArray = UByteArray(36864)

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

        inputText.setOnEditorActionListener(mWriteListener)

        connectWithOtherDevice()

        connectButton.setOnClickListener {
            if (btClass.mState == BtClass.STATE_NONE) {
                showPairedDevices()
            } else {
                btClass.stopBluetooth()
            }
        }

        sendButton.setOnClickListener {
            val message: String = inputText.text.toString()
            sendMessage(message)
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

    private fun setOutputText(msg: String) {
        val oldMessage: String? = outputText.text.toString()
        val newMessage: String = if (oldMessage == null) {
            msg
        } else {
            oldMessage + msg
        }
        outputText.text = newMessage
    }

    override fun onDestroy() {
        super.onDestroy()
        btClass.stopBluetooth()
    }

    @ExperimentalUnsignedTypes
    private val handler: Handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            CONNECTING_STATUS -> {
                when(message.arg1) {
                    1 -> {
                        checkState(BtClass.STATE_CONNECTED, R.string.disconnect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "CONNECT", "Connect to $bluetoothName")
                        Toast.makeText(
                            view?.context,
                            "$bluetoothName are connected",
                            Toast.LENGTH_LONG
                        ).show()
                        true
                    }
                    2 -> {
                        checkState(BtClass.STATE_NONE, R.string.connect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "DISCONNECT", "Disconnect to $bluetoothName")
                        Toast.makeText(
                            view?.context,
                            "$bluetoothName are disconnected",
                            Toast.LENGTH_LONG
                        ).show()
                        true
                    }
                    -1 -> {
                        checkState(BtClass.STATE_NONE, R.string.connect, lambdaChangeTextConnectButton)
                        Log.println(Log.INFO, "CONNECT_ERROR", "Device fails to connect")
                        Toast.makeText(
                            view?.context,
                            "Connection failed",
                            Toast.LENGTH_LONG
                        ).show()
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
                val mBuffer: ByteArray = message.obj as ByteArray
                val arduinoMessage: String? = String(mBuffer, 0, message.arg1)

                arduinoMessage?.let { message ->

                    if (!BtClass.possibleFingerprint) {
                        when {
                            setupRegex.containsMatchIn(message) -> {
                                setOutputText(message)
                            }
                            waitingRegex.containsMatchIn(message) -> {
                                setOutputText(message)
                                BtClass.possibleFingerprint = true
                            }
                            else -> {
                                setOutputText(message)
                            }
                        }
                    } else {
                        if (imageTakenRegex.containsMatchIn(message)) {
                            BtClass.firstMessage = true
                            BtClass.gettingFingerprintRawProcess = true
                        }
                    }

                    if (BtClass.gettingFingerprintRawProcess) {
                        getFingerprintRaw(BtClass.firstMessage, message)
                    }
                }
                true
            }
            MESSAGE_WRITE -> {
                when(message.arg1){
                    1 -> {
                        Log.println(Log.INFO, "SENT_MESSAGE", "Message was sent successfully")
                        true
                    }
                    -1 -> {
                        Log.println(Log.INFO, "CONNECTION_LOST", "Device connection was lost")
                        Toast.makeText(
                            view?.context,
                            "Connection lost",
                            Toast.LENGTH_LONG
                        ).show()
                        false
                    }
                    else -> {
                        Log.println(Log.INFO, "UNEXPECTED_SEND_ERROR", "Error when we trying to send message ")
                        false
                    }
                }

            }
            else -> {
                Log.println(Log.INFO, "UNEXPECTED_CASE", "Arduino Message")
                false
            }
        }
    }

    private val mWriteListener = OnEditorActionListener { view, actionId, event -> // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_UP) {
                val message = view.text.toString()
                sendMessage(message)
            }
            true
        }

    private fun sendMessage(message: String){
        if (btClass.mState == BtClass.STATE_CONNECTED) {
            if (message.isNotEmpty()) {
                val send: ByteArray = message.toByteArray()
                btClass.write(send)
            }

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer?.setLength(0)
            inputText.setText(mOutStringBuffer)
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

    @ExperimentalUnsignedTypes
    private fun getFingerprintRaw(firstMessage: Boolean, data: String) {

        if (firstMessage) {
            val firstPartString: String = data
                .substringBefore(BtClass.HORIZONTAL_TAB, "None")

            if (firstPartString == "None") {
                setOutputText(data)
                return
            }

            val secondPartString: String = data.substringAfter(BtClass.HORIZONTAL_TAB)

            if (secondPartString.isNotEmpty()) {
                getDataFingerprintRaw(secondPartString)
            }

            BtClass.firstMessage = false
            return
        }

        getDataFingerprintRaw(data)
    }

    @ExperimentalUnsignedTypes
    fun getDataFingerprintRaw(dataString: String) {
        var posSubString = 0
        while (true) {
            mFingerprintBuffer[BtClass.position] = dataString[posSubString]
                .code.toUByte()
            ++BtClass.position
            ++posSubString
            if (BtClass.position > 36863) {
                BtClass.position = 0
                BtClass.possibleFingerprint = false
                BtClass.gettingFingerprintRawProcess = false
                break
            }
            if (posSubString == dataString.length) {
                break
            }
        }

        if (posSubString < dataString.length - 1) {
            val finalPartString: String = dataString
                .substring(posSubString until dataString.length)
            if (finalPartString.isNotEmpty()) {
                setOutputText(finalPartString)
            }
        }

        return
    }
}