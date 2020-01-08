package com.elo.peripheral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

public class NFCAdvance {
	private static final String TAG = "TestPeripheral";
	private static final String TAG_TITLE = "[Elo][NFC Adv] ";
	private static final String CMD_RESET =  "C200017F";
	private static final String CMD_TO_HID = "C20008090004555342024B";
	private static final String CMD_TO_KB =  "C200080900045553420049";
	private static final int NFC_ADVANCE_COMMAND_INIT_TIME = 500;
	private PendingIntent mPermissionIntent;
	private Context mContext;
	private UsbManager mUsbManager;
	private Handler mHandler;
	private UsbDevice mUsbDevice;
	private int packetSize;
	private Timer myTimer = new Timer();
	private String receiveMessageString = "";
	private ArrayList<DeviceInformation> mControlArrayList;
	private boolean mGetPermission = false;


	public NFCAdvance(Context context, UsbManager usbManager, Handler handler, ArrayList<DeviceInformation> arrayList) {
		mContext = context;
		mUsbManager = usbManager;
		mHandler = handler;
		setPermissionIntent();
		setupReceiver();
		mControlArrayList = arrayList;
	}
	private void setPermissionIntent() {
		// TODO Auto-generated method stub
		mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(
				MSRAdvance.ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(MSRAdvance.ACTION_USB_PERMISSION);
		mContext.registerReceiver(mUsbPermissionReceiver, filter);
	}
	private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, TAG_TITLE + "mUsbPermissionReceiver intent = " + action);
			if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if(device != null){
							Log.d(TAG, TAG_TITLE + "mUsbPermissionReceiver has device");
							if(device.getProductId()== MainActivity.NFC_PID_KB && device.getVendorId() == MainActivity.NFC_VID) {
								setDevice(device);
							} else if(device.getProductId()== MainActivity.NFC_PID_MSR && device.getVendorId() == MainActivity.NFC_VID) {
								setDevice(device);
							}
						}
						else {
							Log.d(TAG, TAG_TITLE + "mUsbPermissionReceiver no device");
						}
					}
					else {
						MainActivity.setDeviceUntransform(device.getVendorId(), mControlArrayList, mHandler);
						Log.d(TAG, TAG_TITLE + "permission denied for device " + device);
					}
				}
			}
		}
	};
	private UsbDeviceConnection mDeviceConnection;
	private UsbInterface mUsbInterface;
	private UsbDeviceConnection mDeviceConnectionWrite;
	private UsbDeviceConnection mDeviceConnectionRead;
	private UsbDevice mNfcKeyboardDevice;
	private UsbDevice mNfcMsrDevice = null;;
	private UsbEndpoint endPointRead;
	private UsbEndpoint endPointWrite;
	
	public UsbDevice getDevicePermission(int deviceVid, int devicePid, boolean getPermission) {
		// TODO Auto-generated method stub
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		UsbDevice device;
		while (deviceIterator.hasNext()) {
			device = deviceIterator.next();
			if(device.getVendorId() == deviceVid) {
				if(device.getProductId() == devicePid) {
					if(getPermission) {
						mUsbManager.requestPermission(device, mPermissionIntent);
					}
					return device;
				}
			}
		}
		return null;
	}
	protected Runnable runnableGotoAdvanceMode = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, TAG_TITLE + "gotoAdvanceMode" );
			sendCommandToMsrDevice(CMD_TO_HID, NFC_ADVANCE_COMMAND_INIT_TIME);
			softRestDeviceCommand();
		}
	};
/*
	protected void gotoAdvanceMode() {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "gotoAdvanceMode" );
		sendCommandToMsrDevice("C20008090004555342024B", NFC_ADVANCE_COMMAND_INIT_TIME);
		softRestDeviceCommand();
	}
*/
	protected void gotoNormalMode() {
		sendCommandToMsrDevice(CMD_TO_KB, NFC_ADVANCE_COMMAND_INIT_TIME);
		softRestDeviceCommand();
	}
	protected void softRestDeviceCommand() {
		// TODO Auto-generated method stub
		sendCommandToMsrDevice(CMD_RESET, NFC_ADVANCE_COMMAND_INIT_TIME);
		mHandler.sendEmptyMessageDelayed(MainActivity.NFC_TRANSFORM_RECOVERY, 10000);
	}
	private byte[] sendCommandToMsrDevice(String command, int commandTimeout) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "CommandToMsrDevice = " + command);
		if(mDeviceConnection == null) return null;
		byte[] out = new byte[1153];
		int requestType=0x21;
		int request = 0x09;
		int value = 0x0300;
		int index = 0x0;
		covertStringToCommandSet(command, out);

		int writeValue = mDeviceConnectionWrite.controlTransfer(requestType, request, value, index, out, out.length, commandTimeout);

		Log.d(TAG, TAG_TITLE + "[Command Return]writeValue = " + writeValue);
		byte[] response = getCommandResponse(out);
		Log.d(TAG, TAG_TITLE + "nfc Device response = " + bytesToHex(response));
		return response;
	}
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 4];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 4] = '<';
			hexChars[j * 4 + 1] = hexArray[v >>> 4];
			hexChars[j * 4 + 2] = hexArray[v & 0x0F];
			hexChars[j * 4 + 3] = '>';
		}
		return new String(hexChars);
	}
	private void covertStringToCommandSet(String command, byte[] out) {
		// TODO Auto-generated method stub
		for (int bnum = 0; bnum < command.length() / 2; bnum++) {
			String bstring = command.substring(bnum * 2, bnum * 2 + 2);
			int bval = Integer.parseInt(bstring, 16);
			out[bnum]=(byte)bval;
		}
	}
	private byte[] getCommandResponse(byte[] out) {
		byte[] in = new byte[1153];

		int requestType = 0xA1;
		int request = 0x01;
		int value = 0x0300;
		int index = 0x00;

		int readvalue = mDeviceConnectionRead.controlTransfer(requestType, request, value, index, in, in.length, 2000);

		Log.d(TAG, TAG_TITLE + "[Command Return]readvalue = "+readvalue);
		int sizeOfIn2;
		if(in[0] == (byte)0xC2) {
			sizeOfIn2 = (int)(in[1]* Math.pow(16, 2)) + in[2] + 3; //normal status
		} else {
			sizeOfIn2 = in[2] + 3; //abnormal
		}
		byte[] in2 = new byte[sizeOfIn2];
		for (int i = 0; i < sizeOfIn2; i++){
			in2[i] = in[i];
		}
		return in2;

		//return hexIn;
	}
	public void onPause() {
		// TODO Auto-generated method stub

	}
	public void onDestory() {
		// TODO Auto-generated method stub

	}
	public void onResume() {
		// TODO Auto-generated method stub
		//getNfcMsrDevice();

	}
	public void getNfcMsrDevice() {
		mNfcMsrDevice = getDevicePermission(MainActivity.NFC_VID, MainActivity.NFC_PID_MSR, true);
	}
	protected void setDevice(UsbDevice device) {
		// TODO Auto-generated method stub
		Log.d(TAG, TAG_TITLE + "Selected device VID:" + Integer.toHexString(device.getVendorId()) + " PID:" + Integer.toHexString(device.getProductId()));
		mDeviceConnection = mUsbManager.openDevice(device);
		mUsbInterface = device.getInterface(0);
		if (null == mDeviceConnection) {
			Log.d(TAG, TAG_TITLE + "(unable to establish connection)\n");
		} else {
			mDeviceConnection.claimInterface(mUsbInterface, true);
			Log.d(TAG, TAG_TITLE + "Claim Interface is true\n");
			mDeviceConnectionWrite = mDeviceConnection;
			mDeviceConnectionRead = mDeviceConnection;
		}
		try {
			if (UsbConstants.USB_DIR_IN == mUsbInterface.getEndpoint(0).getDirection()) {
				endPointRead = mUsbInterface.getEndpoint(0);
				packetSize = endPointRead.getMaxPacketSize();
			}
		} catch (Exception e) {
			Log.e("uic", "Device have no endPointRead", e);
		}
		if(device.getProductId()== MainActivity.NFC_PID_KB && device.getVendorId() == MainActivity.NFC_VID) {
			//gotoAdvanceMode();
			new Thread(runnableGotoAdvanceMode).start();
		} else {
			MainActivity.setDeviceUntransform(device.getVendorId(), mControlArrayList, mHandler);
		}
	}
	private void setupReceiver() {
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if (mDeviceConnection != null && endPointRead != null && mNfcMsrDevice != null) {
						final byte[] buffer = new byte[packetSize];
						final int status = mDeviceConnection.bulkTransfer(endPointRead, buffer, packetSize, 300);
						if (status >= 0) {
							//Process Receive Message
							for (int i = 0; i < packetSize; i++) {
								if(buffer[i] == 0) continue;
								////Log.i("uic","bytes[bytescount]="+bytes[bytescount]);
								char charbuffer=(char)buffer[i];
								receiveMessageString+=charbuffer;
							}
							if(receiveMessageString.isEmpty() || receiveMessageString == null || receiveMessageString.trim().isEmpty()) {
								
							} else {
								Log.d(TAG, TAG_TITLE + "receiveMessageString = " + receiveMessageString);
								MainActivity.sendMessageToHandler(MainActivity.NFC_DATA, receiveMessageString, mHandler);

								receiveMessageString = "";
							}
						}
					}
				} catch (Exception e) {
					Log.w("Exception: ", e.getLocalizedMessage());
					Log.w("setupReceiver", e);
				}
			};
		}, 0L, 1);
	}

}
