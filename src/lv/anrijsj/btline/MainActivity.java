package lv.anrijsj.btline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	//String deviceMac1 = "FF:FF:FF:FF:FF:FF"; // Disable MAC
	String deviceMac1 = "20:13:08:08:21:71"; // ALL CAPS!
	String deviceName1 = "HC-06"; // real name not given
	int threadDelay = 200; // min msec between sending data

    TextView statusLabel, param1Label, param2Label, param3Label, param4Label;
    TextView param1Value, param2Value, param3Value, param4Value;
    SeekBar param1SeekBar, param2SeekBar, param3SeekBar, param4SeekBar;
    
    Button connectButton, startButton, stopButton, writeButton, readButton;
    
    Handler mHandler = new Handler();
    
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    boolean connected = false;
    boolean valuesChanged = false;
    volatile boolean stopWorker;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        connectButton = (Button)findViewById(R.id.button5);
        writeButton = (Button)findViewById(R.id.button2);
        readButton = (Button)findViewById(R.id.button1);
        startButton = (Button)findViewById(R.id.button4);
        stopButton = (Button)findViewById(R.id.button3);
   
        param1Label = (TextView)findViewById(R.id.param1name);
        param2Label = (TextView)findViewById(R.id.param2name);
        param3Label = (TextView)findViewById(R.id.param3name);
        param4Label = (TextView)findViewById(R.id.param4name);
        statusLabel = (TextView)findViewById(R.id.statusText);
        
        param1Value = (TextView)findViewById(R.id.param1Value);
        param2Value = (TextView)findViewById(R.id.param2Value);
        param3Value = (TextView)findViewById(R.id.param3Value);
        param4Value = (TextView)findViewById(R.id.param4Value);
        
        param1SeekBar = (SeekBar)findViewById(R.id.seekBar1);
        param2SeekBar = (SeekBar)findViewById(R.id.seekBar2);
        param3SeekBar = (SeekBar)findViewById(R.id.seekBar3);
        param4SeekBar = (SeekBar)findViewById(R.id.seekBar4);

        //Open Button
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try 
                {
                    if(findBT()) {
                      openBT();
                    }
                }
                catch (IOException ex) { 
                	Log.d("exp", ex.getMessage());
                }
            }
        });
        
        readButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	byte[] bytes = {0x72};
            	sendBytes(bytes);
            }
        });
        
        writeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	byte[] bytes = {0x77};
            	sendBytes(bytes);
            }
        });
        
        stopButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	byte[] bytes = {0x73};
            	sendBytes(bytes);
            }
        });
        
        startButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	byte[] bytes = {0x67};
            	sendBytes(bytes);
            }
        });
        
        
        /*** SeekBar Listeners ***/
        param1SeekBar.setOnSeekBarChangeListener(
        	    new OnSeekBarChangeListener() {
        	        int progress = 0;
        	        @Override
        	        public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
        	          param1Value.setText(""+progresValue);
        	          valuesChanged = true;
        	        }

        	        @Override
        	        public void onStartTrackingTouch(SeekBar seekBar) {}

        	        @Override
        	        public void onStopTrackingTouch(SeekBar seekBar) {}
        	});

        param2SeekBar.setOnSeekBarChangeListener(
        	    new OnSeekBarChangeListener() {
        	        int progress = 0;
        	        @Override
        	        public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
        	          param2Value.setText(""+progresValue);
        	          valuesChanged = true;
        	        }

        	        @Override
        	        public void onStartTrackingTouch(SeekBar seekBar) {}

        	        @Override
        	        public void onStopTrackingTouch(SeekBar seekBar) {}
        	});
        
        param3SeekBar.setOnSeekBarChangeListener(
        	    new OnSeekBarChangeListener() {
        	        int progress = 0;
        	        @Override
        	        public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
        	          param3Value.setText(""+progresValue);
        	          valuesChanged = true;
        	        }

        	        @Override
        	        public void onStartTrackingTouch(SeekBar seekBar) {}

        	        @Override
        	        public void onStopTrackingTouch(SeekBar seekBar) {}
        	});
        
        param4SeekBar.setOnSeekBarChangeListener(
        	    new OnSeekBarChangeListener() {
        	        int progress = 0;
        	        @Override
        	        public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
        	          param4Value.setText(""+progresValue);
        	          valuesChanged = true;
        	        }

        	        @Override
        	        public void onStartTrackingTouch(SeekBar seekBar) {}

        	        @Override
        	        public void onStopTrackingTouch(SeekBar seekBar) {}
        	});
        
        /*** Thread for automatic value sending ***/
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        Thread.sleep(threadDelay);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                            	if(connected && valuesChanged) {
                            		byte data1 = (byte) param1SeekBar.getProgress();
                            		byte data2 = (byte) param2SeekBar.getProgress();
                            		byte data3 = (byte) param3SeekBar.getProgress();
                            		byte data4 = (byte) param4SeekBar.getProgress();
                            		
                            	    byte[] bytes = {0x76, data1, data2, data3, data4};
                            	    sendBytes(bytes);
                            	    valuesChanged = false;
                            	    
                            	}
                            }
                        });
                    } catch (Exception e) { }
                }
            }
        }).start();
    }
    
    boolean findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            statusLabel.setText("No bluetooth adapter available");
        }
        
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
            	Log.d(">> Address <<", device.getAddress());
            	Log.d(">> Name <<", device.getName());

                if(device.getAddress().equals(deviceMac1) || device.getName().equals(deviceName1)) 
                {
                	Log.d(">> CON <<", "connecting to: "+device.getName());
                    mmDevice = device;
                    return true;
                }
            }
        }
        statusLabel.setText("Bluetooth Device Found");
        return false;
    }
    
    void openBT() throws IOException
    {
    	
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        
        beginListenForData();
        
        param1Label.setEnabled(true);
        param2Label.setEnabled(true);
        param3Label.setEnabled(true);
        param4Label.setEnabled(true);
        
        param1SeekBar.setEnabled(true);
        param2SeekBar.setEnabled(true);
        param3SeekBar.setEnabled(true);
        param4SeekBar.setEnabled(true);
        
        readButton.setEnabled(true);
        writeButton.setEnabled(true);
        startButton.setEnabled(true);
        stopButton.setEnabled(true);
        
        statusLabel.setText("Connected");
        connectButton.setVisibility(11);
        
        connected = true;
    }
    
    void beginListenForData()
    {
        final Handler handler = new Handler(); 
        final byte delimiter = 10; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
               while(!Thread.currentThread().isInterrupted() && !stopWorker)
               {
                    try 
                    {
                        int bytesAvailable = mmInputStream.available();                        
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            statusLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    }
        
    void sendBytes(byte[] b)
    {
    	try {
            mmOutputStream.write(b);
            statusLabel.setText("Data Sent");
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        statusLabel.setText("Bluetooth Closed");
    }
}
