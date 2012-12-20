package com.audric.bonjour;

import sephiroth.android.library.imagezoom.RotateBitmap;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.audric.bonjour.SwitchListener.Direction;

public class mImageViewTouch  extends mImageViewTouchBase {

	static final float					MIN_ZOOM	= 0.6f; //1f

	private static final String TAG = mImageViewTouch.class.getSimpleName();

	protected float						mCurrentScaleFactor;
	protected float						mScaleFactor;
	protected int							mDoubleTapDirection;

	protected GestureListener gestureListerner;
	protected GestureDetector gestureDetector;

	protected SwitchListener switchListener = null;

	private boolean switchAlreadyStarted = false;



	protected OnLongClickListener longClickListener;
	protected ScaleListener				mScaleListener;
	protected ScaleGestureDetector	mScaleDetector;



	public mImageViewTouch( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}
 
	@Override
	protected void init()
	{
		super.init();
		/* about scroll & fling */
		gestureListerner = new GestureListener();
		setLongClickable(true);
		gestureDetector = new GestureDetector(getContext(), gestureListerner,null,true);

		mCurrentScaleFactor = 1f;
		mScaleListener = new ScaleListener();

		mScaleDetector = new ScaleGestureDetector( getContext(), mScaleListener );
		mCurrentScaleFactor = 1f;
		mDoubleTapDirection = 1;   
	}

	@Override
	public void setImageRotateBitmapReset( RotateBitmap bitmap, boolean reset )
	{
		super.setImageRotateBitmapReset( bitmap, reset );
		mScaleFactor = getMaxZoom() / 3;
		setSwitchAlreadyStarted(false);
	}
	
	
	public void setSwitchAlreadyStarted(Boolean bool) {
		switchAlreadyStarted = bool;
	}


	@Override
	public void setImageBitmapReset( final Bitmap bitmap, final int rotation, final boolean reset ) {
		super.setImageBitmapReset(bitmap, rotation, reset);
		setSwitchAlreadyStarted(false);
	}


	@Override
	protected void onZoom( float scale )
	{
		super.onZoom( scale );
		mCurrentScaleFactor = scale; 
	}



	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		mScaleDetector.onTouchEvent( event );
		if ( !mScaleDetector.isInProgress() ) gestureDetector.onTouchEvent( event );
		int action = event.getAction();
		switch ( action & MotionEvent.ACTION_MASK ) {
		case MotionEvent.ACTION_UP:
			if ( getScale() < 1f ) {
				zoomTo( 1f, 150 );
			}
			break;
		}
		return true;
	}



	protected float onDoubleTapPost( float scale, float maxZoom )
	{
		if ( mDoubleTapDirection == 1 ) {
			if ( ( scale + ( mScaleFactor * 2 ) ) <= maxZoom ) {
				return scale + mScaleFactor;
			} else {
				mDoubleTapDirection = -1;
				return maxZoom;
			}
		} else {
			mDoubleTapDirection = 1;
			return 1f;
		}
	}



	class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap( MotionEvent e )
		{
			float scale = getScale();
			float targetScale = scale;
			targetScale = onDoubleTapPost( scale, getMaxZoom() );
			targetScale = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
			mCurrentScaleFactor = targetScale;
			zoomTo( targetScale, e.getX(), e.getY(), 250 );
			invalidate();
			return super.onDoubleTap( e );
		}


		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			if ( e1 == null || e2 == null ) return false;
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;

			
			if(e1.getY()> (getHeight() * 4) /5 && e2.getY()>  (getHeight() * 4) /5) {
				float distance = e1.getX() - e2.getX();
				if ( distance> 0 && Math.abs(distance) > getWidth()/3) {
					Log.e(TAG, "switch : "+switchAlreadyStarted);
					if(!switchAlreadyStarted) {
						
						onSwitch(Direction.RIGHT);
						setSwitchAlreadyStarted(true);
					}
					
				}
				else if (distance <0 && Math.abs(distance)>getWidth()/3) {
					if(!switchAlreadyStarted) {
						onSwitch(Direction.LEFT);
						setSwitchAlreadyStarted(true);
					}
					
				}
				return true;
			}
			else {
				scrollBy( -distanceX, -distanceY );
				invalidate();
				return super.onScroll( e1, e2, distanceX, distanceY );}
		}


		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY )
		{
			if(!switchAlreadyStarted) {
				if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
				if ( mScaleDetector.isInProgress() ) return false;

				float diffX = e2.getX() - e1.getX();
				float diffY = e2.getY() - e1.getY();


				if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
					//scrollBy( diffX / 2, diffY / 2, 300 );
					scrollBy( diffX, diffY, 600);
					invalidate();
				}
			}
			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}



	class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@SuppressWarnings( "unused" )
		@Override
		public boolean onScale( ScaleGestureDetector detector )
		{
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = mCurrentScaleFactor * detector.getScaleFactor();
			if ( true ) {
				targetScale = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
				zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
				mCurrentScaleFactor = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
				mDoubleTapDirection = 1;
				invalidate();
				return true;
			}
			return false;
		}
	}



	public void setOnSwitchListener(SwitchListener listener) {
		this.switchListener = listener;
	}

	private void onSwitch(Direction direction) {
		if(switchListener!=null) {
			//Log.d(TAG, "onSwitch send");
			switchListener.onSwitch(direction);
		}
		else
			Log.d(TAG, "listener switch is null");
	}
}
