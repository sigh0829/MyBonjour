package com.audric.bonjour;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

public class MadamesImageViewTouch extends ImageViewTouch {

	private static final String TAG = MadamesImageViewTouch.class
			.getSimpleName();

	public MadamesImageViewTouch(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		Log.d(TAG, "test");
		super.setImageDrawable(drawable);
	}

}
