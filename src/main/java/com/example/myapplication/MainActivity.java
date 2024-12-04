package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("a8098c1a-f86e-11da-bd1a-00112444be1e"); // UUID tùy chỉnh
    private TextView textView;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.message);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    1);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            1);
                    return;
                }
                bluetoothAdapter.enable();
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WAKE_LOCK},
                    1);
        }

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
        Intent ChatSer = new Intent(this, ServiceChat.class);
        startService(ChatSer);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("PER", "SUCCESS");
            } else {
                Log.e("PER", "FAIL");
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private byte[] mmBuffer;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            1);
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("IntegratedSever", MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    manageConnectedSocket(socket);
                }
            }
        }
        private void manageConnectedSocket(BluetoothSocket socket) {
            final InputStream inputStream;
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            try {
                inputStream = socket.getInputStream();
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = inputStream.read(mmBuffer);
                        String receivedMessage = new String(mmBuffer, 0, numBytes);
                        runOnUiThread(() -> textView.setText(receivedMessage));
                        String[] parts  = receivedMessage.split(":");

                        Intent intent = new Intent();
                        intent.setAction("com.salab.act.intent.START_ACT");
                        intent.setClassName("com.salab.act", "com.salab.act.SystemEventReceiver");
                        intent.putExtra("SCRIPT_FILE", parts[1]);

                        Log.e("MESS", parts[1]);


                        sendBroadcast(intent);

                        //getApplicationContext().sendBroadcast(intent);

                    } catch (IOException e) {
                        Log.e("THREAD", "FAIL");
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e("Input", "Error occurred when creating output stream", e);
            }
        }
    }

    private class  ServiceChat extends Service{
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            // Start the Bluetooth socket listening thread
            AcceptThread acceptThread = new AcceptThread();
            acceptThread.start();
            // Make the service persistent, so it won't be killed automatically
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service, so return null
            return null;
        }
    }
}