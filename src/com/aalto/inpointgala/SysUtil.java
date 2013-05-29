package com.aalto.inpointgala;
import android.content.Context;  
import android.content.Intent;

public class SysUtil {
	public static final int EXIT_APPLICATION = 0x0001;

	private Context mContext;

	public SysUtil(Context context) {
		this.mContext = context;
	}

	//exit app
	public void exit() {
		Intent mIntent = new Intent();
		mIntent.setClass(mContext, InPoint.class);
		// set flag
		mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// send exit instruction
		mIntent.putExtra("flag", EXIT_APPLICATION);
		mContext.startActivity(mIntent);
	}
}
