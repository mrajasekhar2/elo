package com.elo.peripheral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.magtek.mobile.android.libDynamag.MagTeklibDynamag;

public class MSRAdvance {
	private static final String TAG = "TestPeripheral";
	private static final String TAG_TITLE = "[Elo][MSR Adv] ";
	public static final int STATUS_IDLE = 1;
	public static final int STATUS_PROCESSCARD = 2;
	public static final int DEVICE_MESSAGE_CARDDATA_CHANGE =3;	
	public static final int DEVICE_STATUS_CONNECTED = 4;
	public static final int DEVICE_STATUS_DISCONNECTED = 5;

	public static final int DEVICE_STATUS_CONNECTED_SUCCESS = 0;
	public static final int DEVICE_STATUS_CONNECTED_FAIL = 1;
	public static final int DEVICE_STATUS_CONNECTED_PERMISSION_DENIED = 2;
	static final String ACTION_USB_PERMISSION = "com.elo.peripheral.USB_PERMISSION";
	private static final String MSR_COMMAND_RESET = "02";
	private static final String MSR_COMMAND_KB_TO_HID = "01021001";

	private MagTeklibDynamag mMagTeklibDynamag;
	private Handler mReaderDataHandler = new Handler(new MtHandlerCallback());
	private UsbManager mUsbManager;
	private Context mContext;
	private UsbDevice mUsbDevice;
	private PendingIntent mPermissionIntent;
	private Handler mHandler;
	private ArrayList<DeviceInformation> mControlArrayList;
	public MSRAdvance(Context context, UsbManager usbManager, Handler handler, ArrayList<DeviceInformation> arrayList) {
		mContext = context;
		mUsbManager = usbManager;
		setPermissionIntent();
		mMagTeklibDynamag = new MagTeklibDynamag(mContext, mReaderDataHandler);
		mHandler = handler;
		mControlArrayList = arrayList;
	}
	protected void gotoNormalMode() {
		// TODO Auto-generated method stub
		if(mMagTeklibDynamag.isDeviceConnected()){
			mMagTeklibDynamag.sendCommandWithLength(MSR_COMMAND_KB_TO_HID);
			mMagTeklibDynamag.sendCommandWithLength(MSR_COMMAND_RESET);
			mHandler.sendEmptyMessageDelayed(MainActivity.MSR_TRANSFORM_RECOVERY, 3000);
		}
	}
	public void getMsrHidDevice() {
		if (! mMagTeklibDynamag.isDeviceConnected()) {
			mMagTeklibDynamag.openDevice();
		}
	}
	public void onDestory() {
		if (mMagTeklibDynamag.isDeviceConnected()) {
			mMagTeklibDynamag.closeDevice();
		}	
	}
	public void onPause() {
		if (mMagTeklibDynamag.isDeviceConnected()) {
			mMagTeklibDynamag.closeDevice();
		}
	}
	public String displayCardData()
	{
		String strDisplay="";
		strDisplay+="Track.Decode.Status\n" + mMagTeklibDynamag.getTrackDecodeStatus() + "\n\n";
		strDisplay+="SDK.Version:\n" + mMagTeklibDynamag.getSDKVersion() + "\n\n";
		strDisplay+="Encrypt.Status:\n" + mMagTeklibDynamag.getEncryptionStatus() + "\n\n";
		strDisplay+="Encrypted.Track1:\n" + mMagTeklibDynamag.getTrack1() + "\n\n";
		strDisplay+="Encrypted.Track2:\n" + mMagTeklibDynamag.getTrack2() + "\n\n";
		strDisplay+="Encrypted.Track3:\n" + mMagTeklibDynamag.getTrack3() + "\n\n";
		strDisplay+="Masked.Track1:\n" + mMagTeklibDynamag.getTrack1Masked() + "\n\n";
		strDisplay+="Masked.Track2:\n" + mMagTeklibDynamag.getTrack2Masked() + "\n\n";
		strDisplay+="Masked.Track3:\n" + mMagTeklibDynamag.getTrack3Masked() + "\n\n";
		strDisplay+="Device.Serial:\n" + mMagTeklibDynamag.getDeviceSerial() + "\n\n";
		strDisplay+="Key.Serial (KSN):\n" + mMagTeklibDynamag.getKSN() + "\n\n";
		strDisplay+="MagnePrint.Status:\n" + mMagTeklibDynamag.getMagnePrintStatus() + "\n\n";
		strDisplay+="MagnePrint.Data:\n" + mMagTeklibDynamag.getMagnePrint() + "\n\n";

		Log.d(TAG, TAG_TITLE + strDisplay);
		return strDisplay;
	}
	private void setPermissionIntent() {
		// TODO Auto-generated method stub
		// ------------------------------------------------------------------
		mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		mContext.registerReceiver(mUsbPermissionReceiver, filter);
		// -------------------------------------------------------------------		
	}
	public void getMsrKbPermission() {
		// TODO Auto-generated method stub
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		UsbDevice device;
		while (deviceIterator.hasNext()) {
			device = deviceIterator.next();
			if(device.getProductId()== MainActivity.MSR_PID_KB && device.getVendorId() == MainActivity.MSR_VID) {
				mUsbManager.requestPermission(device, mPermissionIntent);
			} else {
				mUsbDevice = null;
			}
		}
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
							if(device.getProductId()== MainActivity.MSR_PID_KB && device.getVendorId() == MainActivity.MSR_VID) {
								mUsbDevice = device;
								new Thread(runnableGotoAdvanceMode).start();
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
	private Runnable runnableGotoAdvanceMode = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(mUsbDevice == null) return;
			Log.d(TAG, TAG_TITLE + "GotoAdvanceMode device name = " + mUsbDevice.getDeviceName());
			UsbDeviceConnection usbC = mUsbManager.openDevice(mUsbDevice);
			if(usbC != null) {
				usbC.claimInterface(mUsbDevice.getInterface(0), true);
				//Fixed parameter, Don't Modify
				byte[] buffer;
				buffer = new byte[60];
				buffer[0] = 1;
				buffer[1] = 2;
				buffer[2] = 16;
				buffer[3] = 0;
				usbC.controlTransfer(33, 9, 768, 0, buffer, 0, 60, 0);
				usbC.controlTransfer(161, 1, 768, 0, buffer, 0, 60, 0);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				buffer = new byte[60];
				buffer[0] = 2;
				usbC.controlTransfer(33, 9, 768, 0, buffer, 0, 60, 0);
				usbC.controlTransfer(161, 1, 768, 0, buffer, 0, 60, 0);
			}	
			mUsbDevice = null;
			mHandler.sendEmptyMessageDelayed(MainActivity.MSR_TRANSFORM_RECOVERY, 3000);
		}
	};
		// TODO Auto-generated method stub
/*
	private void gotoAdvanceMode(UsbDevice device2) {
		// TODO Auto-generated method stub
		if(device2 == null) return;
		Log.d(TAG, TAG_TITLE + "docontrol device name = " + device2.getDeviceName());
		UsbDeviceConnection usbC = mUsbManager.openDevice(device2);
		if(usbC != null) {
			usbC.claimInterface(device2.getInterface(0), true);
			//Fixed parameter, Don't Modify
			byte[] buffer;
			buffer = new byte[60];
			buffer[0] = 1;
			buffer[1] = 2;
			buffer[2] = 16;
			buffer[3] = 0;
			usbC.controlTransfer(33, 9, 768, 0, buffer, 0, 60, 0);
			usbC.controlTransfer(161, 1, 768, 0, buffer, 0, 60, 0);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			buffer = new byte[60];
			buffer[0] = 2;
			usbC.controlTransfer(33, 9, 768, 0, buffer, 0, 60, 0);
			usbC.controlTransfer(161, 1, 768, 0, buffer, 0, 60, 0);
		}
	}
*/
	private class MtHandlerCallback implements Callback {
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch (msg.what) {
			case DEVICE_MESSAGE_CARDDATA_CHANGE: 
				mMagTeklibDynamag.setCardData((String) msg.obj);
				mHandler.sendEmptyMessage(MainActivity.MSR_DATA);
				ret = true;
				break;
			case DEVICE_STATUS_CONNECTED:
				Log.d(TAG, TAG_TITLE + "connect " + msg.obj.toString());
				if((Integer) msg.obj == 0) {
					MainActivity.setDeviceUntransform(MainActivity.MSR_VID, mControlArrayList, mHandler);
				}
				break;
			case DEVICE_STATUS_DISCONNECTED:
				Log.d(TAG, TAG_TITLE + "DEVICE_STATUS_DISCONNECTED");
				break;
			case DEVICE_STATUS_CONNECTED_PERMISSION_DENIED:
				Log.d(TAG, TAG_TITLE + "DEVICE_STATUS_CONNECTED_PERMISSION_DENIE");
				break;
			case DEVICE_STATUS_CONNECTED_SUCCESS:
				Log.d(TAG, TAG_TITLE + "DEVICE_STATUS_CONNECTED_SUCCESS");
				break;
			case DEVICE_STATUS_CONNECTED_FAIL:
				Log.d(TAG, TAG_TITLE + "DEVICE_STATUS_CONNECTED_FAIL");
				break;
			default:
				ret = false;
				break;
			}
			return ret; 
		}
	}
}
