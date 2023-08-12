package com.example.pcvolume

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Deferred
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {

    lateinit var bluetoothAdapter: BluetoothAdapter

    var REQUEST_ENABLE_BT: Int = 1
    lateinit var textView: TextView
    lateinit var plusButton : Button
    lateinit var minusButton : Button
    lateinit var enableBluetoothIntent: Intent
    lateinit var m_socket: BluetoothSocket
    var MY_UUID: UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee")
    lateinit var m_outputStream: OutputStream
    private var kotlinChannel = Channel<Int>(kotlinx.coroutines.channels.Channel.CONFLATED)
    var VolumeSize: Int = 0

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            } else {
                //deny
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.text_view_id)
        plusButton = findViewById<Button>(R.id.buttonPlus)

        plusButton.setOnClickListener{
            PlusVolume();
        }
        minusButton = findViewById<Button>(R.id.buttonMinus)

        minusButton.setOnClickListener(){
            MinusVolume();
        }
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            textView.text = "not support bluetooth"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            textView.text = "Try to enable bluetooth"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            }
            enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
        }
        else
        {
            textView.text = "Bluetooth Enabled"
        }
        if (!FindComputer("GENADY-5530"))
        {
            textView.text = "Computer not found"
        }
        else
        {
            textView.text = "GENADY-5530" + " Found !!"

        }
    }

    private fun PlusVolume()
    {
        SendVolume(55)
    }

    private fun MinusVolume()
    {

        SendVolume(25)
    }

    fun  SendVolume(volume:Int) = runBlocking {    // Creates a blocking coroutine that executes in current thread (main)

        kotlinChannel.send(volume)
    }

    fun numberToByteArray (data: Number, size: Int = 4) : ByteArray =
        ByteArray (size) {i -> (data.toLong() shr (i*8)).toByte()}

    @SuppressLint("MissingPermission")
    private fun RunCorrutineBluetooth(mydevice: BluetoothDevice)
    {

        GlobalScope.launch {


            /*if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }*/

            try {
                m_socket = mydevice.createRfcommSocketToServiceRecord(MY_UUID)
                m_socket.connect()
                //runOnUiThread { textView.text = "Connected !!!!" }

                m_outputStream = m_socket.outputStream
            } catch (e: Exception) {
                runOnUiThread { textView.text = "ERROR: Create socket" }
            }
            //var counter: Int = 0;
            while (true) {
                // Updating Text View at current
                // iteration
                //runOnUiThread{ textView.text = "First msg" }

                // Thread sleep for 1 sec
                // Thread.sleep(1000)
                // Updating Text View at current
                // iteration
                //runOnUiThread{ textView.text = "Secomd msg" }
                val vol = kotlinChannel.receive()

                //var mymsg = "Gena message #d"
               ///mymsg = mymsg + vol.toString();
                val byteArray = numberToByteArray(vol)

                m_outputStream.write(byteArray);
                //counter++;


                // Thread sleep for 1 sec
                //Thread.sleep(2000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun FindComputer(computerName: String): Boolean {
        val pairedDevice: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        if (pairedDevice != null)
        {
            var listDeviceName = arrayListOf<String>()
            pairedDevice.forEachIndexed { index, device ->
                listDeviceName.add(index, device.name)

                val toast = Toast.makeText(this, device.name, Toast.LENGTH_SHORT)
                toast.show()
                TimeUnit.SECONDS.sleep(1)

                if (device.name == computerName)
                {
                    RunCorrutineBluetooth(device)
                    return true;
                }

            }
        }
        return false
    }

    /*private fun RunThread(mydevice: BluetoothDevice)
    {
        Thread(Runnable {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@Runnable
            }
            try {
                m_socket = mydevice.createRfcommSocketToServiceRecord(MY_UUID)
                m_socket.connect()
                runOnUiThread{ textView.text = "Connected !!!!" }

                m_outputStream = m_socket.outputStream
            } catch (e: Exception) {
                runOnUiThread{ textView.text = "ERROR: Create socket" }
            }
            var counter:Int = 0;
            while (true) {
                // Updating Text View at current
                // iteration
                //runOnUiThread{ textView.text = "First msg" }

                // Thread sleep for 1 sec
                // Thread.sleep(1000)
                // Updating Text View at current
                // iteration
                //runOnUiThread{ textView.text = "Secomd msg" }

                var mymsg = "Gena message #d"
                mymsg =  mymsg + counter.toString();
                val byteArray = mymsg.toByteArray()

                m_outputStream.write(byteArray);
                counter++;



                // Thread sleep for 1 sec
                Thread.sleep(2000)
            }
        }).start()
    }*/
}