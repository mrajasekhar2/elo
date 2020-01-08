package com.elo.peripheral;

import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceInformation {
	/** Activate Button */
	private Switch mButton;
	
	/** Data Output */
	private TextView mTextView;
	
	/** Device ID */
	private int mDevice;
	
	/** Device be activated or not */
	private Boolean mActive;
	
	private Boolean mIsModeTranforming;
	
	/** Device is connect or not */
	private boolean mDeviceConnection;
	
	/** Device advance usage button */
	private EloSwitch mAdvanceButton;
	
	
	/** Device advance usage is connect or not */
	private boolean mAdvanceDeviceConnection;
	
	private ImageView mConnectionImageView;
	
	public DeviceInformation(int deviceId, Switch deviceButton, TextView deviceTextView, Boolean active, boolean connection, EloSwitch advanceButton, boolean advanceDeviceConnection, ImageView connectionImageView, Boolean transform)
	{
		mDevice = deviceId;
		mButton = deviceButton;
		mTextView = deviceTextView;
		mActive = active;
		mDeviceConnection = connection;
		mAdvanceButton = advanceButton;
		mAdvanceDeviceConnection = advanceDeviceConnection;
		mConnectionImageView = connectionImageView;
		mIsModeTranforming = transform;
	}
	public int getDevice(){
		return mDevice;
	};
	public Switch getButton(){
		return mButton;
	}
	public TextView getTextView(){
		return mTextView;
	}
	public Boolean getStatus(){
		return mActive;
	}
	public void setStatus(Boolean status) {
		mActive = status;
	}
	public boolean getDeviceConnection(){
		return mDeviceConnection;
	}
	public void setDeviceConnection(boolean connection){
		mDeviceConnection = connection;
	}
	public EloSwitch getAdvanceButton(){
		return mAdvanceButton;
	}
	public void setAdvanceDeviceConnection(boolean status){
		mAdvanceDeviceConnection = status;
	}
	public boolean getAdvanceConnection(){
		return mAdvanceDeviceConnection;
	}
	public ImageView getConnectionImageView(){
		return mConnectionImageView;
	}
	public boolean isTransforming(){
		return mIsModeTranforming;
	}
	public void setTrasnformStatus(boolean status) {
		mIsModeTranforming = status;
		mAdvanceButton.setEnabled(!status);
	}
}
