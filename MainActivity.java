package com.elo.peripheral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.View.OnClickListener;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.elotouch.library.EloPeripheralEventListener;
import com.elotouch.library.EloPeripheralManager;



public class MainActivity extends Activity {
	private static final String TAG = "TestPeripheral";
	private static final String TAG_TITLE = "[Elo] ";
	//Text
	private TextView mMsrDataTextView;
	private TextView mBcrDataTextView;
	private TextView mNfcDataTextView;
	private TextView mGpio81DataEditText;
	private TextView mGpio82DataEditText;
	//Button

	private ImageButton mBcrTriggerButton;
	private ImageButton mGpio80PullHighButton;
	private ImageButton mGpio80PullLowButton;

	private ImageView mNfcConnectionImageView;
	private ImageView mMsrConnectionImageView;
	private ImageView mBcrConnectionImageView;
	private EloSwitch mMsrAdvanceSwitch;
	private EloSwitch mNfcAdvanceSwitch;
	private Switch mNfcActivateSwitch;
	private Switch mMsrActivateSwitch;
	private Switch mBcrActivateSwitch;

	private static final String NFC_DEVICE_NAME = "UNIFORM UIC681SG HID Keyboard";
	private static final String MSR_DEVICE_NAME = "Mag-Tek USB Swipe Reader";
	private static final String BCR_DEVICE_NAME = "Barcode Decoder";
	private static final String POWER_HIGH = "1";
	private static final String CAT_GPIO80 = "cat /sys/class/gpio/gpio80/value";
	private static final String CAT_GPIO81 = "cat /sys/class/gpio/gpio81/value";
	private static final String CAT_GPIO82 = "cat /sys/class/gpio/gpio82/value";
	public static final String ACTION_USB_PERMISSION = "com.elo.peripheral.USB_PERMISSION";
	public static final int NFC_DEVICE = 0;
	public static final int MSR_DEVICE = 1;
	public static final int BCR_DEVICE = 2;
	private static final int NOT_SPECIAL_DEVICE = 3;
	private static final int BCR_DATA = 100;
	private static final int CHANGE_BCR_DEVICE_STATUS = 101;
	public static final int MSR_DATA = 200;
	public static final int NFC_DATA = 300;
	public static final int MSR_TRANSFORM_RECOVERY = 400;
	public static final int NFC_TRANSFORM_RECOVERY = 500;
	private static final int GPIO81 = 81;
	private static final int GPIO82 = 82;
	private static final int GPIO81_INPUT_CHANGE = GPIO81;
	private static final int GPIO82_INPUT_CHANGE = GPIO82;
	private static final int BCR_TRIGGER_TIME = 1 * 1000; // 1 sec
	public static final int MSR_VID = 2049;
	public static final int MSR_PID_KB = 1;
	public static final int MSR_PID_HID = 17;
	public static final int NFC_VID = 25426;
	public static final int NFC_PID_KB = 26651;
	public static final int NFC_PID_MSR = 26653;
	public static final int BCR_VID = 1155;
	
	private OnCheckedChangeListener mOnCheckedChangedListner = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			// TODO Auto-generated method stub
			switch(buttonView.getId()){
			case R.id.nfc_switch:
				changeDeviceStatus(NFC_DEVICE, isChecked);
				break;
			case R.id.msr_switch:
				changeDeviceStatus(MSR_DEVICE, isChecked);
				break;
			case R.id.bcr_switch:
				changeDeviceStatus(BCR_DEVICE, isChecked);
				break;
			default:
				break;
			}
		}
	};


	private ArrayList<DeviceInformation> mControlArrayList= new ArrayList<DeviceInformation>();
	//private ELOPeripheralManager mELOManager = null;
	private InputManager mInputManager = null;
	//private ELOPeripheralEventListener mListener = new PeripheralListener();
	private MSRAdvance mMsrAdvance;
	private NFCAdvance mNfcAdvance;

	private UsbManager mUsbManager;
	private EloPeripheralManager mEloManager;
	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			Log.d(TAG, TAG_TITLE + "onReceive device PID= " + device.getProductId());
			Log.d(TAG, TAG_TITLE + "onReceive device VID= " + device.getVendorId());
			String action = intent.getAction();
			setDeviceToggleOn(action, device.getVendorId());
			setDeviceUntransform(action, device.getVendorId(), mControlArrayList);
			//When getting USB attach and detach event, application should get USB information each time 
			mHandler.postDelayed(runnableGetDeviceConnection, 100); //delay 100ms wait for all the device is ready
		}
	};
	public static void setDeviceUntransform(int vid, ArrayList<DeviceInformation> arrayList, Handler handler) {
		if(vid == MSR_VID || vid == MSR_DEVICE){
			arrayList.get(MSR_DEVICE).setTrasnformStatus(false);
			handler.removeMessages(MSR_TRANSFORM_RECOVERY);
		} else if (vid == NFC_VID || vid == NFC_DEVICE) {
			arrayList.get(NFC_DEVICE).setTrasnformStatus(false);
			handler.removeMessages(NFC_TRANSFORM_RECOVERY);
		}
	}
	protected void setDeviceToggleOn(String action, int vendorId) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "setDeviceToggleOn");
		Log.d(TAG, TAG_TITLE + "C = " + UsbManager.ACTION_USB_ACCESSORY_ATTACHED + " A = " + action);
		if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			Log.d(TAG, TAG_TITLE + "attached");
			switch(vendorId) {
			case BCR_VID:
				Log.d(TAG, TAG_TITLE + "BCR");
				getDeviceConnectionInformation();
				setDeviceStatus(BCR_DEVICE, true);
				//arrayList.get(BCR_DEVICE).setStatus(true);
				break;
			case MSR_VID:
				getDeviceConnectionInformation();
				setDeviceStatus(MSR_DEVICE, true);
				//arrayList.get(MSR_DEVICE).setStatus(true);
				break;
			case NFC_VID:
				getDeviceConnectionInformation();
				setDeviceStatus(NFC_DEVICE, true);
				//arrayList.get(NFC_DEVICE).setStatus(true);
				break;
			}
		}
	}
	protected void setDeviceUntransform(String action, int vendorId,
			ArrayList<DeviceInformation> arrayList) {
		if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			setDeviceUntransform(vendorId, arrayList, mHandler);
		}
	}
	protected void setFullScreen() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG_TITLE + "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initGuiComponent();
		initControlArrayList();
		getServiceManager();
		setFullScreen();
		mMsrAdvance = new MSRAdvance(this, mUsbManager, mHandler, mControlArrayList);
		mNfcAdvance = new NFCAdvance(this, mUsbManager, mHandler, mControlArrayList);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "onPause");
		super.onPause();
		/*
		if(mELOManager != null) {
			Log.i(TAG, TAG_TITLE + "mELOManager.unregisterListener(mListener)");
			mELOManager.unregisterListener(mListener);
		}*/
		mEloManager.OnPause();
		mMsrAdvance.onPause();
		mNfcAdvance.onPause();
		unregisterReceiver(mUsbReceiver);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "onResume");
		setFullScreen();
		super.onResume();
		/*
		if(mELOManager != null) {
			Log.i(TAG, TAG_TITLE + "mELOManager.registerListener(mListener)");
			mELOManager.registerListener(mListener);
		}*/
		mEloManager.OnResume();
		initGpioInformation();
		mHandler.postDelayed(runnableGetDeviceConnection, 100);
		registerUSBReceiver();
	}

	private void registerUSBReceiver() {
		// TODO Auto-generated method stub
		IntentFilter filter=new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver,filter);		
	}

	private void initGuiComponent() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "initialGUIComponent");

		//EditText

		mNfcDataTextView = (TextView) findViewById(R.id.nfc_data_content);
		mMsrDataTextView = (TextView) findViewById(R.id.msr_data_content);
		mBcrDataTextView = (TextView) findViewById(R.id.bcr_data_content);

		mNfcConnectionImageView = (ImageView)findViewById(R.id.nfc_connection);
		mBcrConnectionImageView = (ImageView)findViewById(R.id.bcr_connection);
		mMsrConnectionImageView = (ImageView)findViewById(R.id.msr_connection);

		mGpio81DataEditText = (TextView) findViewById(R.id.gpio81_status);
		mGpio82DataEditText = (TextView) findViewById(R.id.gpio82_status);

		//Button


		mNfcActivateSwitch = (Switch) findViewById(R.id.nfc_switch);
		mMsrActivateSwitch = (Switch) findViewById(R.id.msr_switch);
		mBcrActivateSwitch = (Switch) findViewById(R.id.bcr_switch);
		mBcrActivateSwitch.setOnCheckedChangeListener(mOnCheckedChangedListner);
		mNfcActivateSwitch.setOnCheckedChangeListener(mOnCheckedChangedListner);
		mMsrActivateSwitch.setOnCheckedChangeListener(mOnCheckedChangedListner);
		
		mBcrTriggerButton = (ImageButton) findViewById(R.id.bcr_scan);
		mGpio80PullHighButton = (ImageButton) findViewById(R.id.gpio80_pull_high);
		mGpio80PullLowButton = (ImageButton) findViewById(R.id.gpio80_pull_low);

		mMsrAdvanceSwitch = (EloSwitch) findViewById(R.id.msr_type_switch);
		mNfcAdvanceSwitch = (EloSwitch) findViewById(R.id.nfc_type_switch);
		
		mMsrAdvanceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				setAdvanceSwitchStatus(MSR_DEVICE, isChecked, mControlArrayList);
			}
		});
		mNfcAdvanceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				setAdvanceSwitchStatus(NFC_DEVICE, isChecked, mControlArrayList);
			}
		});

		//Set onClickListener
		mBcrTriggerButton.setOnClickListener(buttonListener);
		mGpio80PullHighButton.setOnClickListener(buttonListener);
		mGpio80PullLowButton.setOnClickListener(buttonListener);


		//setMovementMethod
		mNfcDataTextView.setMovementMethod(new ScrollingMovementMethod());
		mMsrDataTextView.setMovementMethod(new ScrollingMovementMethod());
		mBcrDataTextView.setMovementMethod(new ScrollingMovementMethod());

	}
	protected void setAdvanceSwitchStatus(int DEVICE, boolean isChecked, ArrayList<DeviceInformation> arrayList) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "device =" + DEVICE + " is " + isChecked);
		DeviceInformation deviceInfor = arrayList.get(DEVICE);
		if(deviceInfor.getDeviceConnection() || deviceInfor.getAdvanceConnection()) {
			if((deviceInfor.getAdvanceConnection() && isChecked) || (deviceInfor.getDeviceConnection() && !isChecked)) {
			} else {
				if(DEVICE == MSR_DEVICE) {
					switchMsrType(isChecked);
				} else if(DEVICE == NFC_DEVICE) {
					switchNfcType(isChecked);
				}
			}
		} else {
			deviceInfor.getAdvanceButton().setSwitchStatus(false);
		}
	}
	private void initControlArrayList() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "initialControlArrayList");

		/*
		initialize a arraylist save all of the peripheral component information, it should follow the sequence of device sequence 
			private static final int NFC_DEVICE = 0;
			private static final int MSR_DEVICE = 1;
			private static final int BCR_DEVICE = 2;
		*/
		mControlArrayList.add(new DeviceInformation(NFC_DEVICE, mNfcActivateSwitch, mNfcDataTextView, false, false, mNfcAdvanceSwitch,  false, mNfcConnectionImageView, false));
		mControlArrayList.add(new DeviceInformation(MSR_DEVICE, mMsrActivateSwitch, mMsrDataTextView, false, false, mMsrAdvanceSwitch,  false, mMsrConnectionImageView, false));
		mControlArrayList.add(new DeviceInformation(BCR_DEVICE, mBcrActivateSwitch, mBcrDataTextView, false, false, null, false, mBcrConnectionImageView, false));
	}
	private void getServiceManager() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "getServiceManager");
		//mELOManager = (ELOPeripheralManager) this.getApplicationContext().getSystemService(Context.ELO_SERVICE);
		mInputManager = (InputManager)getSystemService(Context.INPUT_SERVICE);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mEloManager = new EloPeripheralManager(getApplicationContext(), new PeripheralEventListener());
	}
	public class PeripheralEventListener implements EloPeripheralEventListener {

		@Override
		public void onEvent(int state, String data) {
			// TODO Auto-generated method stub
			switch(state) {
			case EloPeripheralEventListener.BCR_STATE_DEVICE_CONNECTION:
			case EloPeripheralEventListener.BCR_STATE_DEVICE_DISCONNECTION:
			case EloPeripheralEventListener.BCR_STATE_PIN_AUTO_DISABLE:
				Log.d(TAG, TAG_TITLE + "state = " + state);
				break;
			case EloPeripheralEventListener.BCR_STATE_DATA_RECEIVIED:
				Log.d(TAG, TAG_TITLE + "state = " + state + " data = " + data);
				if(mControlArrayList.get(BCR_DEVICE).getStatus()){
					sendMessageToHandler(BCR_DATA, data);
				}
				break;
			}
		}

		@Override
		public void onEvent(int state) {
			// TODO Auto-generated method stub
			onEvent(state, null);
		}

		@Override
		public void onEvent(int pinNumber, int state) {
			// TODO Auto-generated method stub
			switch(pinNumber) {
				case EloPeripheralEventListener.GPIO81_INPUT_CHANGE:
				case EloPeripheralEventListener.GPIO82_INPUT_CHANGE:
					sendMessageToHandler(pinNumber, EloPeripheralEventListener.GPIO_STATE_LOW == state ? getResources().getString(R.string.low) : getResources().getString(R.string.high));
					break;
			}
		}
		
	}
	private void getDevicesAdvanceConnectionInformation()
	{
		Log.d(TAG, TAG_TITLE + "getUsbAdvanceUsageDevices");
		UsbDevice device;
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		boolean msrStatus = false;
		boolean nfcStatus = false;
		while (deviceIterator.hasNext()) {
			device = deviceIterator.next();
			if(device.getVendorId() == MSR_VID) {
				msrStatus = true;
				if(device.getProductId() == MSR_PID_HID) {
					Log.d(TAG, TAG_TITLE + "MSR_HID");
					mControlArrayList.get(MSR_DEVICE).setAdvanceDeviceConnection(true);
				} else if (device.getProductId() == MSR_PID_KB) {
					mControlArrayList.get(MSR_DEVICE).setAdvanceDeviceConnection(false);
				}
			} else if(device.getVendorId() == NFC_VID) {
				nfcStatus = true;
				if(device.getProductId() == NFC_PID_KB) {
					Log.d(TAG, TAG_TITLE + "NFC_KB");
					mControlArrayList.get(NFC_DEVICE).setAdvanceDeviceConnection(false);
				} else if(device.getProductId() == NFC_PID_MSR) {
					Log.d(TAG, TAG_TITLE + "NFC_MSR");
					mControlArrayList.get(NFC_DEVICE).setAdvanceDeviceConnection(true);
				}
			} else {
				if(!msrStatus) {
					mControlArrayList.get(MSR_DEVICE).setAdvanceDeviceConnection(false);
				}
				if(!nfcStatus) {
					mControlArrayList.get(NFC_DEVICE).setAdvanceDeviceConnection(false);
				}
			}
		}
	}
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// TODO Auto-generated method stub
		int specialDevice = NOT_SPECIAL_DEVICE;
		specialDevice = isSpecialDevice(event);
		switch(specialDevice)
		{
		case NFC_DEVICE:
		case MSR_DEVICE:
			fillContent(specialDevice, event);
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	private void fillContent(int specialDevice, KeyEvent event) {
		// TODO Auto-generated method stub
		if(event.getAction() == KeyEvent.ACTION_DOWN){
			DeviceInformation deviceNode = mControlArrayList.get(specialDevice);
			if(deviceNode.getStatus()){
				deviceNode.getTextView().setText(deviceNode.getTextView().getText() + String.valueOf((char)event.getUnicodeChar()));
				int scrollParameter = deviceNode.getTextView().getLineCount()* deviceNode.getTextView().getLineHeight() - deviceNode.getTextView().getHeight();
				if(scrollParameter > 0){
					deviceNode.getTextView().setScrollY(scrollParameter);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	private void printDeviceConnectionStatus() {
		// TODO Auto-generated method stub
		for(DeviceInformation info : mControlArrayList){
			Log.d(TAG, TAG_TITLE + "Device ID = " + info.getDevice() + " connection = " + info.getDeviceConnection());
		}
	}

	private void getDeviceConnectionInformation() {
		// TODO Auto-generated method stub
		clearDeviceConnectionStatus();
		int[] totalInputDeviceID = mInputManager.getInputDeviceIds();
		for(int i = 0; i < totalInputDeviceID.length; i++){
			checkingDeviceConnection(mInputManager.getInputDevice(totalInputDeviceID[i]));
		}
		printDeviceConnectionStatus();
	}

	private void clearDeviceConnectionStatus() {
		// TODO Auto-generated method stub
		for(DeviceInformation info : mControlArrayList){
			info.setDeviceConnection(false);
		}
	}

	private void checkingDeviceConnection(InputDevice inputDevice) {
		// TODO Auto-generated method stub
		if(inputDevice.getName().contains(NFC_DEVICE_NAME)){
			mControlArrayList.get(NFC_DEVICE).setDeviceConnection(true);
		} else if(inputDevice.getName().contains(MSR_DEVICE_NAME)){
			mControlArrayList.get(MSR_DEVICE).setDeviceConnection(true);
		} else if(inputDevice.getName().contains(BCR_DEVICE_NAME)){
			mControlArrayList.get(BCR_DEVICE).setDeviceConnection(true);
		}
	}

	private void initGpioInformation() {
		// TODO Auto-generated method stub
		if(execShellCommand(CAT_GPIO81).equals(POWER_HIGH)){
			mGpio81DataEditText.setText(getResources().getString(R.string.high));
		} else {
			mGpio81DataEditText.setText(getResources().getString(R.string.low));
		}
		if(execShellCommand(CAT_GPIO82).equals(POWER_HIGH)){
			mGpio82DataEditText.setText(getResources().getString(R.string.high));
		} else {
			mGpio82DataEditText.setText(getResources().getString(R.string.low));
		}
		if(execShellCommand(CAT_GPIO80).equals(POWER_HIGH)){
			setGpio80HighDisplay();
		} else {
			setGpio80LowDisplay();
		}
	}

	private void setGpio80LowDisplay() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "GPI080_LowIndicator");
		mGpio80PullHighButton.setImageResource(R.drawable.pull_high_button);
		mGpio80PullLowButton.setImageResource(R.drawable.pulllow_button_pressed);
	}

	private void setGpio80HighDisplay() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "GPI080_HighIndicator");
		mGpio80PullHighButton.setImageResource(R.drawable.pullhigh_button_pressed);
		mGpio80PullLowButton.setImageResource(R.drawable.pull_low_button);
	}


	private OnClickListener buttonListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			// TODO Auto-generated method stub
			switch(view.getId())
			{
			case R.id.nfc_switch:
				changeDeviceStatus(NFC_DEVICE);
				break;
			case R.id.msr_switch:
				changeDeviceStatus(MSR_DEVICE);
				break;
			case R.id.bcr_switch:
				changeDeviceStatus(BCR_DEVICE);
				break;
			case R.id.gpio80_pull_high:
				setGpio80High();
				break;
			case R.id.gpio80_pull_low:
				setGpio80Low();
				break;
			case R.id.bcr_scan:
				forceTriggerBcr();
				break;
			case R.id.msr_type_switch:
				switchMsrType();
				break;			
			case R.id.nfc_type_switch:
				switchNfcType();
				break;
			default:
				Log.d(TAG, TAG_TITLE + "activateButtonLisnter");
				break;
			}
		}
	};

	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			Log.d(TAG, TAG_TITLE + "msgid = " + msg.what + " content = " + msg.obj);
			switch(msg.what)
			{
			case BCR_DATA:
				setDataToTextView(mControlArrayList.get(BCR_DEVICE), (String) msg.obj);
				//mControlArrayList.get(BCR_DEVICE).getTextView().setText(mControlArrayList.get(BCR_DEVICE).getTextView().getText()+(String)msg.obj + "\n");
				break;
			case CHANGE_BCR_DEVICE_STATUS:
				setDeviceStatus(BCR_DEVICE, (Boolean)msg.obj);
				break;
			case GPIO81_INPUT_CHANGE:
				mGpio81DataEditText.setText((String)msg.obj);
				break;
			case GPIO82_INPUT_CHANGE:
				mGpio82DataEditText.setText((String)msg.obj);
				break;
			case MSR_DATA:
				if(mControlArrayList.get(MSR_DEVICE).getStatus() && mControlArrayList.get(MSR_DEVICE).getAdvanceConnection()) {
					setDataToTextView(mControlArrayList.get(MSR_DEVICE), mMsrAdvance.displayCardData());
					//mControlArrayList.get(MSR_DEVICE).getTextView().setText(mMsrAdvance.displayCardData());
				}
				break;
			case NFC_DATA:
				if(mControlArrayList.get(NFC_DEVICE).getStatus() && mControlArrayList.get(NFC_DEVICE).getAdvanceConnection()) {
					setDataToTextView(mControlArrayList.get(NFC_DEVICE), (String)msg.obj);
					//mControlArrayList.get(NFC_DEVICE).getTextView().setText(mControlArrayList.get(NFC_DEVICE).getTextView().getText() + (String)msg.obj + "\n");
				}
				break;
			case MSR_TRANSFORM_RECOVERY:
				mHandler.post(createRunnable(MSR_DEVICE));
				break;
			case NFC_TRANSFORM_RECOVERY:
				mHandler.post(createRunnable(NFC_DEVICE));
				break;
			default:
				break;
			}
		}
	};
/*
	private class PeripheralListener extends ELOPeripheralEventListener {
		public void onBCR_StateChange(int state, String data) {
			switch(state) {
				case ELOPeripheralManager.BCR_STATE_DEVICE_CONNECTION:
					Log.w(TAG, TAG_TITLE + "BCR_STATE_DEVICE_CONNECTION");
					break;
				case ELOPeripheralManager.BCR_STATE_DEVICE_DISCONNECTION:
					Log.w(TAG, TAG_TITLE + "BCR_STATE_DEVICE_DISCONNECTION");
					break;
				case ELOPeripheralManager.BCR_STATE_DATA_RECEIVIED:
					Log.w(TAG, TAG_TITLE + "BCR_STATE_DATA_RECEIVIED; data: "+data);
					if(mControlArrayList.get(BCR_DEVICE).getStatus()){
						sendMessageToHandler(BCR_DATA, data);
					}
					break;
				case ELOPeripheralManager.BCR_STATE_PIN_AUTO_DISABLE:
					Log.w(TAG, TAG_TITLE + "BCR_STATE_PIN_AUTO_DISABLE");
					break;
				default:
					Log.e(TAG, TAG_TITLE + "Error; state: "+state+", data: "+data);
					break;
			}
		}
		public void onGPIO_StateChange(int state, String data) {
			Log.w(TAG, TAG_TITLE + "onGPIO_StateChange; data: "+data+" state: "+ state);
			int pinNumber = Integer.parseInt(data.substring(4));
			switch(pinNumber)
			{
			case GPIO81_INPUT_CHANGE:
			case GPIO82_INPUT_CHANGE:
				sendMessageToHandler(pinNumber, ELOPeripheralManager.GPIO_STATE_LOW == state ? getResources().getString(R.string.low) : getResources().getString(R.string.high));
				break;
			default:
				break;
			}
		}
	}
*/
	private int isSpecialDevice(KeyEvent event) {
		// TODO Auto-generated method stub
		try {
			if(NFC_DEVICE_NAME.contains(event.getDevice().getName())) {
				return NFC_DEVICE;
			} else if(MSR_DEVICE_NAME.contains(event.getDevice().getName())) {
				return MSR_DEVICE;
			} else {
				return NOT_SPECIAL_DEVICE;
			} 
		} catch (NullPointerException e) {
			Log.d(TAG, TAG_TITLE + "nullPointer Exception");
			return NOT_SPECIAL_DEVICE;
		}
	}


	protected void setDataToTextView(DeviceInformation deviceInformation, String obj) {
		// TODO Auto-generated method stub
		TextView textview = deviceInformation.getTextView();
		textview.setText(textview.getText()+ obj + "\n");
		int scrollParameter = textview.getLineCount()* textview.getLineHeight() - textview.getHeight();
		if(scrollParameter > 0){
			textview.setScrollY(scrollParameter);
		}

	}
	protected void switchNfcType() {
		// TODO Auto-generated method stub
		if(mControlArrayList.get(NFC_DEVICE).isTransforming()) { return;}
		Log.d(TAG, TAG_TITLE + "switch NFC Type");
		setConnectionUiSearchingAndFreeze(mControlArrayList.get(NFC_DEVICE));
		mControlArrayList.get(NFC_DEVICE).setTrasnformStatus(true);
		setDeviceStatus(NFC_DEVICE, false);
		if(mControlArrayList.get(NFC_DEVICE).getDeviceConnection()){
			mNfcAdvance.getDevicePermission(NFC_VID, NFC_PID_KB, true);
		} else if(mControlArrayList.get(NFC_DEVICE).getAdvanceConnection()) {
			mNfcAdvance.gotoNormalMode();
		}
	}
	protected void switchNfcType(boolean status) {
		// TODO Auto-generated method stub
		prepareSwitchType(NFC_DEVICE, mControlArrayList);
		if(status) {
			mNfcAdvance.getDevicePermission(NFC_VID, NFC_PID_KB, true);
		} else {
			mNfcAdvance.gotoNormalMode();
		}
	}
	private void prepareSwitchType(int DEVICE, ArrayList<DeviceInformation> arrayList) {
		// TODO Auto-generated method stub
		DeviceInformation deviceNode = arrayList.get(DEVICE);
		if(deviceNode.isTransforming()) { return;}
		setConnectionUiSearchingAndFreeze(deviceNode);
		deviceNode.setTrasnformStatus(true);
		//mHandler.postDelayed(createRunnable(DEVICE), 30000);
		setDeviceStatus(DEVICE, false);
	}
	protected void switchMsrType() {
		// TODO Auto-generated method stub
		if(mControlArrayList.get(MSR_DEVICE).isTransforming()) { return;}
		Log.d(TAG, TAG_TITLE + "switchMsrType");
		setConnectionUiSearchingAndFreeze(mControlArrayList.get(MSR_DEVICE));
		mControlArrayList.get(MSR_DEVICE).setTrasnformStatus(true);
		setDeviceStatus(MSR_DEVICE, false);
		if(mControlArrayList.get(MSR_DEVICE).getDeviceConnection()){
			mMsrAdvance.getMsrKbPermission();
		} else if(mControlArrayList.get(MSR_DEVICE).getAdvanceConnection()) {
			mMsrAdvance.gotoNormalMode();
		}
	}
	protected void switchMsrType(boolean status) {
		// TODO Auto-generated method stub
		prepareSwitchType(MSR_DEVICE, mControlArrayList);
		if(status){
			mMsrAdvance.getMsrKbPermission();
		} else {
			mMsrAdvance.gotoNormalMode();
		}
	}
	private void setConnectionUiSearchingAndFreeze(DeviceInformation deviceInformation) {
		// TODO Auto-generated method stub
		deviceInformation.getConnectionImageView().setImageResource(R.drawable.searching);
		setDeviceTypeUiOpposite(deviceInformation);
	}
	private void setDeviceTypeUiOpposite(DeviceInformation device) {
		// TODO Auto-generated method stub
		if(device.getAdvanceButton() == null) {
			return;
		}
		if(device.getAdvanceConnection()) {
			device.getAdvanceButton().setSwitchStatus(false);
		} else if (device.getDeviceConnection()) {
			device.getAdvanceButton().setSwitchStatus(true);
		}
	}
	protected void gotoActivity(Class<?> className) {
		// TODO Auto-generated method stub
		Intent  intent = new Intent();
		intent.setClass(this, className);
		startActivity(intent);
	}


	protected void forceTriggerBcr() {
		// TODO Auto-generated method stub
		if(mControlArrayList.get(BCR_DEVICE).getStatus()) {
			//trigger than disable;
			Log.d(TAG, TAG_TITLE + "forceTriggerBCR");
			mEloManager.activeBcr();
			mHandler.postDelayed(runnableStopActiveBcr, BCR_TRIGGER_TIME);
		}
	}
	Runnable runnableStopActiveBcr = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, TAG_TITLE + "runnableStopActiveBCR");
			mEloManager.disactiveBcr();
		}
	};
	protected void setGpio80High() {
		// TODO Auto-generated method stub
		mEloManager.pullGpio80High();
		setGpio80HighDisplay();
		/*
		if(mELOManager != null) {
			Log.i(TAG, TAG_TITLE + "mELOManager.mGPIO_APIs.pullHighGPIO(), iFace = "+ELOPeripheralManager.GPIO_IFACE_80_OUT);
			try {
				mELOManager.mGPIO_APIs.pullHighGPIO(ELOPeripheralManager.GPIO_IFACE_80_OUT);
				setGpio80HighDisplay();
			}catch (RemoteException ex) {
				Log.e(TAG, TAG_TITLE + "RemoteException, ex: "+ex);
			}
		}
		*/
	}

	protected void setGpio80Low() {
		// TODO Auto-generated method stub
		mEloManager.pullGpio80Low();
		setGpio80LowDisplay();
		/*
		if(mELOManager != null) {
			Log.i(TAG, TAG_TITLE + "mELOManager.mGPIO_APIs.pullLowGPIO(), iFace = "+ELOPeripheralManager.GPIO_IFACE_80_OUT);
			try {
				mELOManager.mGPIO_APIs.pullLowGPIO(ELOPeripheralManager.GPIO_IFACE_80_OUT);
				setGpio80LowDisplay();
			}catch (RemoteException ex) {
				Log.e(TAG, TAG_TITLE + "RemoteException, ex: "+ex);
			}
		}
		*/
	}

	public void sendMessageToHandler(int msgID, Object information) {
		// TODO Auto-generated method stub
		sendMessageToHandler(msgID, information, mHandler);
	}
	public static void sendMessageToHandler(int msgID, Object information, Handler handler) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "msgID = " + msgID + " Information = " + information);
		Message message;
		message = handler.obtainMessage(msgID, information);
		handler.sendMessage(message);
	}

	protected void changeDeviceStatus(int device) {
		// TODO Auto-generated method stub
		if(mControlArrayList.get(device).getDeviceConnection() == true || mControlArrayList.get(device).getAdvanceConnection() == true){
			setDeviceStatus(device, !(mControlArrayList.get(device).getStatus()));
			setBcrTriggerButtonEnable(mControlArrayList.get(device));
		}
	}
	protected void changeDeviceStatus(int device, boolean status) {
		// TODO Auto-generated method stub
		if(mControlArrayList.get(device).getDeviceConnection() == true || mControlArrayList.get(device).getAdvanceConnection() == true){
			setDeviceStatus(device, status);
			setBcrTriggerButtonEnable(mControlArrayList.get(device));
		} else {
			//setActivatedSwitchOff(device);
		}
	}

	private void setActivatedSwitchOff(int device) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "device " + device +" setActivatedSwitchOff");
		mControlArrayList.get(device).getButton().setChecked(false);
	}
	protected void setDeviceStatus(int device, boolean status) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "setDeviceStatus device = " + device + " status = " + status);
		DeviceInformation deviceNode = mControlArrayList.get(device);
		if(deviceNode.getStatus() != status) {
			deviceNode.setStatus(status);
			if(deviceNode.getStatus()){
				deviceNode.getButton().setChecked(true);
			} else {
				deviceNode.getButton().setChecked(false);
				//Log.d(TAG, TAG_TITLE + "height = " + deviceNode.getTextView().getScrollY());
				deviceNode.getTextView().setText("");
				deviceNode.getTextView().setScrollY(0);
			}
		}
	}

	private void setBcrTriggerButtonEnable(DeviceInformation deviceNode) {
		// TODO Auto-generated method stub
		if(deviceNode.getDevice() == BCR_DEVICE){
			setButtonEnable(mBcrTriggerButton, deviceNode.getStatus());
		}
	}

	protected String execShellCommand(String command) {
		// TODO Auto-generated method stub
		String retValue = "";
		try {
			Process process = Runtime.getRuntime().exec(command);
			InputStreamReader reader = new InputStreamReader(process.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(reader);

			int numRead;
			char[] buffer = new char[5000];
			StringBuffer commandOutput = new StringBuffer();
			while ((numRead = bufferedReader.read(buffer)) > 0) {
				commandOutput.append(buffer, 0, numRead);
			}
			bufferedReader.close();
			process.waitFor();
			retValue = commandOutput.toString().trim();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		Log.d(TAG, TAG_TITLE + command + " = " + retValue);
		return retValue;
	}
	Runnable runnableGetDeviceConnection = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, TAG_TITLE + "runnableGetDeviceConnection");
			getDeviceConnectionInformation();
			getDevicesAdvanceConnectionInformation();
			setDisconnectDeviceToggleOff();
			setConnectionAndDeviceTypeUi();
			mNfcAdvance.getNfcMsrDevice();
			mMsrAdvance.getMsrHidDevice();
		}
	};
	private Runnable createRunnable(final int deviceId) {
		Runnable recoveryTranformStatus = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(mControlArrayList.get(deviceId).isTransforming()) {
					setDeviceUntransform(deviceId, mControlArrayList, mHandler);
					mHandler.post(runnableGetDeviceConnection);
				}
			}
		};
		return recoveryTranformStatus;
	}
	protected void setDisconnectDeviceToggleOff() {
		// TODO Auto-generated method stub
		for(DeviceInformation device : mControlArrayList)
		{
			if(device.getDeviceConnection() == false && device.getAdvanceConnection() == false) {
				setDeviceStatus(device.getDevice(), false);
				//device.getButton().setChecked(false);
			} else {
				//setDeviceStatus(device.getDevice(), true);
			}
		}
	}

	protected void setConnectionAndDeviceTypeUi() {
		// TODO Auto-generated method stub
		for(DeviceInformation device : mControlArrayList)
		{
			Log.d(TAG, TAG_TITLE + "Device " + device.getDevice() + " enable = " + device.getDeviceConnection());
			setConnectionUi(device);
			setDeviceTypeUi(device);
			setBcrTriggerButtonEnable(device);
		}
	}

	private void setDeviceTypeUi(DeviceInformation device) {
		// TODO Auto-generated method stub
		if(device.isTransforming()) {
			return;
		}
		if(device.getAdvanceButton() == null) {
			return;
		}
		if(device.getAdvanceConnection()) {
			device.getAdvanceButton().setSwitchStatus(true);
		} else if (device.getDeviceConnection()) {
			device.getAdvanceButton().setSwitchStatus(false);
		} else {
			device.getAdvanceButton().setSwitchStatus(false);
		}
	}

	private void setConnectionUi(DeviceInformation device) {
		// TODO Auto-generated method stub
		if(device.isTransforming()){
			return;
		}
		if(device.getDeviceConnection() || device.getAdvanceConnection()) {
			device.getConnectionImageView().setImageResource(R.drawable.connected);
		} else {
			device.getConnectionImageView().setImageResource(R.drawable.disconnected);
		}
	}
	private void setButtonEnable(ImageButton mButton, boolean enable) {
		// TODO Auto-generated method stub
		if(mButton != null) {
			mButton.setEnabled(enable);
			if(enable) {
				mButton.setVisibility(View.VISIBLE);
			} else {
				mButton.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mMsrAdvance.onDestory();
		mNfcAdvance.onDestory();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, TAG_TITLE + "onConfigurationChanged");
	}
}
