package com.audric.bonjour;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.audric.customprogressdialog.CustomProgressDialog;
import com.nostra13.universalimageloader.core.ImageLoader;

public abstract class BaseActivity extends Activity {
	/*private static final String TAG = BaseActivity.class.getSimpleName();*/
	
	public static final long COUNTDOWN_BEFORE_SHOWING = 500; 

	protected ImageLoader imageLoader = ImageLoader.getInstance();
	private boolean instanceStateSaved;




	private CustomProgressDialog dialog;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		instanceStateSaved = true;
	}

	@Override
	protected void onDestroy() {
		if (!instanceStateSaved) {
			imageLoader.stop(); 
		}
		super.onDestroy();
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	protected void dismissDialog() {
		dialog.cancelShowing();
	}



	protected void prepareDialog(Context context) {
		if(dialog==null) {
			dialog = new CustomProgressDialog(context);
			dialog.setCountdownBeforeShow(COUNTDOWN_BEFORE_SHOWING);
			dialog.setTitle(R.string.loading);
			dialog.setMessage(R.string.retrieving_latest_madames);
		}
	}






}
