package com.audric.bonjour;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * {@link ViewPager} which can be deactivated (ignore touch events)
 * 
 * @author Sergey Tarasevich
 * @created 19.07.2012
 */
public class DeactivableViewPager extends ViewPager {

	private boolean activated = true;

	public DeactivableViewPager(Context context) {
		super(context);
	}

	public DeactivableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void activate() {
		activated = true;
	}

	public void deactivate() {
		activated = false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (activated) {
			try {
				return super.onInterceptTouchEvent(event);
			} catch (Exception e) { // sometimes happens
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		try {
			return super.onTouchEvent(event);
		} catch (Exception e) {
			return true;
		}
	}
}