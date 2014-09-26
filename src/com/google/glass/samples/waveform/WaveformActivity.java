/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.glass.samples.waveform;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.google.glass.samples.waveform.Global;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

/**
 * An activity that receives EDA input from the Q-sensor and displays a
 * visualization of that data as a waveform on the screen.
 * 
 * @author Javier Hernandez Rivera Adapted functionalities by @author Adrian
 *         Jimenez-Galindo
 */

public class WaveformActivity extends Activity implements SensorEventListener{

	int SR = 4;// sampling rate
	int max_time_window = 10 * 60;// maximum time window size -> should be the
									// same as the maximum of list_modes
	boolean use_bluetooth = true;
	boolean showClock = false;

	private WaveformView mWaveformView;
	private TextView mDecibelView;
	private TextView mMessage;
	private RecordingThread mRecordingThread;
	private BluetoothSocket mSocket = null;
	private BufferedReader mBufferedReader = null;	
	private MediaPlayer mediaPlayer;
	private boolean fromScanner = Boolean.valueOf(false);
	
	String aString = "nothing";
	TextView textClock = null;
	long base_time = 0;
	@SuppressLint("SimpleDateFormat")
	SimpleDateFormat hour_format = new SimpleDateFormat("HH:mm:ss");
	Handler hand = null; // handler for clock
	Date base = null;
	long currentTime = 0;// variable to keep the current time
	double len = 0;
	private SensorManager mSensorManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.layout_waveform);
	
		mWaveformView = (WaveformView) findViewById(R.id.waveformView);
		mDecibelView = (TextView) findViewById(R.id.decibelView);
		mMessage = (TextView) findViewById(R.id.textMessage);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		if(Global.getConnected()) {
			toggleLoading(true, "Loading...");
		} else {
			toggleLoading(true, "Could not find a device. Please scan again");
		}

		try {
			openConnection("00:06:66:0A:50:B9");
			toggleLoading(false, " ");
		} catch (Exception e) {
			System.out.println("onCreat error");
			showToast("Please pair the device again");
		}
		showClockF(); 
	}

	void showClockF() {
		if (showClock == true) {
			textClock = (TextView) findViewById(R.id.textClock);
			hand = new Handler();
			hand.postDelayed(new Runnable() {
				public void run() {
					currentTime = (new Date()).getTime();
					textClock.setText(hour_format.format(currentTime));
					hand.postDelayed(this, 1000);
				}
			}, 1000);
		}
	}
	
	private void openConnection(String s) {
		connectDevice(s);
		BluetoothSocket mSocket1 = mSocket;
		InputStream aStream = null;
		try {
			aStream = mSocket1.getInputStream();
		} catch (IOException e) {
			System.out.println("openconnection error");
			e.printStackTrace();
		}
		InputStreamReader aReader = new InputStreamReader(aStream);
		mBufferedReader = new BufferedReader(aReader);
	}
	
	////////
	//QR Code scanner handler
	////////
	/**
	 * Handle result from QR code scanner.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				playSound();
				String contents = intent.getStringExtra("SCAN_RESULT");
				Global.setAddress(contents);
				Global.resetGlobals();
				mWaveformView.reset();
				openConnection(contents);				
			} else {
				System.out.println("onactivityresult error");
			}
		}
	}
	public void playSound() {
		mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.chime);
		mediaPlayer.start();
	}
	
	/* TODO: Preferences menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    //case R.id.refresh_option_item:
	     //   getActivity().startService(item.getIntent());
	    //    break;
	    case R.id.settings_option_item:
	        startActivity(item.getIntent());
	        break;
	    }
	    return true;
	}
	*/
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			String string = null;
			if (Global.getClicked()) {
				string = "Closing history...";
				mWaveformView.reset();
				mWaveformView.resetCurrentPosition();
			} else {
				string = "Opening history...";				
				mWaveformView.cloneEDA();
			}
			showToast(string);
			Global.setIndex(0);
			Global.onClick();
			return true;
		case KeyEvent.KEYCODE_BACK:
			showToast("Goodbye");
			Global.resetGlobals();
			mWaveformView.reset();
			finish();
			return true;
		case KeyEvent.KEYCODE_CAMERA:
				Intent intent = new Intent(
						"com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, 0);
				showToast("Initializing scanner...");
				fromScanner = true;
			return true;
		default:
			return false;
		}
	}
	
	private void showToast(String s) {
		Toast toast = Toast.makeText(getApplicationContext(), s,
				Toast.LENGTH_SHORT);
		toast.show();
	}

	private void toggleLoading(boolean b, String s) {
		
		if(b) {
			mMessage.setText(s);
			mMessage.setVisibility(View.VISIBLE);
		} else {
			mMessage.setText(" ");
			findViewById(R.id.textMessage).setVisibility(View.INVISIBLE);
		}
	}
	////////
	//Gyro sensor methods
	////////
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			SensorManager.getRotationMatrixFromVector(
					Global.getRotationMatrix(), event.values);
			SensorManager.remapCoordinateSystem(Global.getRotationMatrix(),
					SensorManager.AXIS_X, SensorManager.AXIS_Z,
					Global.getRotationMatrix());
			SensorManager.getOrientation(Global.getRotationMatrix(),
					Global.getOrientation());
			Global.setHeadingAndPitch(Global.getOrientation());
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	@SuppressLint("InlinedApi")
	private void startTracking() {
		mSensorManager.registerListener(this,  
				mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_NORMAL);		
	}
	
	private void stopTracking() { mSensorManager.unregisterListener(this); }
	
	//////
	//Bluetooth device methods
	/////
	private void openDeviceConnection(BluetoothDevice aDevice)
			
		throws IOException {
			Log.d("javierhr", "beginning openDeviceConnection");
			InputStream aStream = null;
			InputStreamReader aReader = null;
		try {
			UUID uuid = aDevice.getUuids()[0].getUuid();
			mSocket = aDevice.createRfcommSocketToServiceRecord(uuid);
			mSocket.connect();
			aStream = mSocket.getInputStream();
			aReader = new InputStreamReader(aStream);
			mBufferedReader = new BufferedReader(aReader);
			toggleLoading(false, " ");
			Global.Connected();
		} catch (IOException e) {
			Global.Disconnected();			
			toggleLoading(true, "Could not find a device. Please scan again.");
			close(mBufferedReader);
			close(aReader);
			close(aStream);
			close(mSocket);
			e.printStackTrace();
		}
	}
	
	/**
	 * Close the connection with @param aConnectedObject
	 */
	private void close(Closeable aConnectedObject) {
		if (aConnectedObject == null)
			return;
		try {
			aConnectedObject.close();
		} catch (IOException e) {
			System.out.println("close error");
		}
		aConnectedObject = null;
	}

	public boolean createBond(BluetoothDevice btDevice) throws Exception {
		Class<?> class1 = Class.forName("android.bluetooth.BluetoothDevice");
		Method createBondMethod = class1.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}
	
	/**
	 * Main method to connect to a bluetooth device based on the @param mac address of the device.
	 * @param mac : the MAC address of the device.
	 */
	private void connectDevice(String mac) {
		BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice btDevice = bluetooth.getRemoteDevice(mac);// tablet
		
		if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {
			try {
				Log.d("javierhr", "bonding...");
				boolean result = createBond(btDevice);
				Log.d("javierhr", "bonded..." + result + " " + btDevice.getBondState());
			} catch (Exception e1) {
				Log.d("javierhr", "Error in WaveformActivity.connectDevice");
				e1.printStackTrace();
			}
		}
		
		if (use_bluetooth) {
			while (btDevice.getBondState() != 12) {
				Log.d("javierhr", "waiting to add the pairing code " + btDevice.getBondState());
			}			
				Log.d("javierhr", "state device after pause " + btDevice.getBondState());
			try {
				Log.d("javierhr", "before connecting...");
				openDeviceConnection(btDevice);
				Log.d("javierhr", "after connecting...");
			} catch (IOException e) {
				System.out.println("connectdevice 2error");
				showToast("Can't communicate with device");
				e.printStackTrace();
			}
		}
	}
	
	//////
	//Super and thread methods
	/////
	@Override
	protected void onResume() {
		super.onResume();
		mRecordingThread = new RecordingThread();
		mRecordingThread.start();
		startTracking();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopTracking();
		if (mRecordingThread != null) {
			mRecordingThread.stopRunning();
			mRecordingThread = null;
		}
	}
	
	/**
	 * A background thread that receives data from the sensor and sends it
	 * to the waveform visualizing class.
	 */
	private class RecordingThread extends Thread {

		private boolean mShouldContinue = true;

		@Override
		public void run() {
			while (shouldContinue()) {
				try {
					if (use_bluetooth) {
						aString = mBufferedReader.readLine();
					} else {
						aString = "1,0.00,-0.03,-1.05,3.88,33.5,4.5";
					}
					
					// update EDA
					mWaveformView.updateSensorData(aString);
					updateDecibelLevel();
					Global.Connected();
			
				} catch (IOException e) {
					mShouldContinue = false;
					Global.Disconnected();
					if(fromScanner) {
						toggleLoading(true, "Could not connect to device. Make sure your device is powered on and on discovery mode");
					} else {
						toggleLoading(true, "Could not find a device. Please scan again.");
					}
					if (!use_bluetooth) {
						close(mBufferedReader);
						close(mSocket);
						toggleLoading(false, "  ");
					}
					e.printStackTrace();
				}
			}
		}

		private synchronized boolean shouldContinue() { return mShouldContinue; }
		public synchronized void stopRunning() { mShouldContinue = false; }
		//On screen information regarding normal mode (temperature, EDA, data displayed)
		private void updateDecibelLevel() {
			mDecibelView.post(new Runnable() {
				@Override
				public void run() {
					mDecibelView.setText(mWaveformView.getTimeMode()
							+ " seconds\n ["
							+ (String.format("%.2f", mWaveformView.minEDA))
							+ " - "
							+ (String.format("%.2f", mWaveformView.maxEDA))
							+ "] \u03BCS\n "
							+ (String.format("%.1f", mWaveformView.maxTEMP))
							+ " C\u00b0");
				}
			});
		}
	}
	
}

