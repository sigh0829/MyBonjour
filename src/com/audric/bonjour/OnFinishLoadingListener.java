package com.audric.bonjour;

import android.graphics.Bitmap;

public interface OnFinishLoadingListener {
	public void onLoadingFinished(Bitmap bitmap, boolean inCache, int page);
}
