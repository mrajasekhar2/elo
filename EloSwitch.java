package com.elo.peripheral;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

public class EloSwitch extends LinearLayout {
	private ImageView mKbImageView;
	private ImageView mHidImageView;
	private Switch mSwitch;
	//==
	//private Handler mEclipseHandler = null;
	//==
	public EloSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		setOrientation(LinearLayout.HORIZONTAL);
		//if(isInEditMode()){return;}
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.elo_switch, this, true);
		mKbImageView = (ImageView) getChildAt(0);
		mSwitch = (Switch) getChildAt(1);
		mHidImageView = (ImageView) getChildAt(2);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EloSwitchStyle, 0, 0);
		int eloSwitchMinWidth = a.getInteger(R.styleable.EloSwitchStyle_eloSwitchMinWidth, 36);
		int eloThumbTextPadding = a.getInteger(R.styleable.EloSwitchStyle_eloThumbTextPadding, 10);
		mSwitch.setSwitchMinWidth(eloSwitchMinWidth);
		mSwitch.setThumbTextPadding(eloThumbTextPadding);
	}
	public EloSwitch(Context context) {
		this(context, null);
	}
	public void setKBImage(int resId) {
		mKbImageView.setImageResource(resId);
	}
	public void setHidImage(int resId) {
		mHidImageView.setImageResource(resId);
	}
	public void setSwitchTrackThumb(int resId_track, int resId_thumb) {
		mSwitch.setTrackResource(resId_track);
		mSwitch.setThumbResource(resId_thumb);

	}
	public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
		mSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
	}
	public void setSwitchStatus(Boolean status) {
		mSwitch.setChecked(status);
	}
	public Boolean getSwithStatus() {
		return mSwitch.isChecked();
	}
	public void setEnabled(boolean status) {
		mSwitch.setEnabled(status);
	}

}
