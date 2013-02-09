package com.audric.bonjour;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.audric.bonjour.WebServiceClient.OnUrlsLoadingListener;
import com.nostra13.example.universalimageloader.Constants.Extra;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class ImagePagerActivity extends BaseActivity implements OnUrlsLoadingListener {
	//private static final String TAG = ImagePagerActivity.class.getSimpleName();

	private PreferencesManager prefManager;
	private int pagerPosition;

	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_image_pager);

		Bundle bundle = getIntent().getExtras();
		prepareDialog(ImagePagerActivity.this);
		
		WebServiceClient ws = WebServiceClient.getInstance();
		ws.retrieveMadamesUrlsInThread(this);
		prefManager = new PreferencesManager(getApplicationContext());


		pagerPosition = bundle.getInt(Extra.IMAGE_POSITION, 0);

		ImageLoaderConfiguration configuration = ImageLoaderConfiguration.createDefault(getApplicationContext());
		imageLoader.init(configuration);

		options = new DisplayImageOptions.Builder()
		.showImageForEmptyUri(R.drawable.image_for_empty_url)
		.resetViewBeforeLoading()
		.cacheOnDisc()
		.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
	}

	private class ImagePagerAdapter extends PagerAdapter {

		private ArrayList<String> images;
		private LayoutInflater inflater; 

		ImagePagerAdapter(ArrayList<String> images) {
			this.images = images;
			inflater = getLayoutInflater();
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public void finishUpdate(View container) {
		}

		@Override
		public int getCount() {
			return images.size();
		}

		@Override
		public Object instantiateItem(View view, int position) {
			final View imageLayout = inflater.inflate(R.layout.item_pager_image, null);
			final ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);

			imageLoader.displayImage(images.get(position), imageView, options, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted() {
					spinner.setVisibility(View.VISIBLE);
				}

				@Override
				public void onLoadingFailed(FailReason failReason) {
					String message = null;
					switch (failReason) {
					case IO_ERROR:
						message = "Input/Output error";
						break;
					case OUT_OF_MEMORY:
						message = "Out Of Memory error";
						break;
					case UNKNOWN:
						message = "Unknown error";
						break;
					}
					Toast.makeText(ImagePagerActivity.this, message, Toast.LENGTH_SHORT).show();

					spinner.setVisibility(View.GONE);
					imageView.setImageResource(android.R.drawable.ic_delete);
				}

				@Override
				public void onLoadingComplete(Bitmap loadedImage) {
					spinner.setVisibility(View.GONE);
				}
			});

			((ViewPager) view).addView(imageLayout, 0);
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View container) {
		}
	}


	


	@Override
	public void urlsLoadingFinished(final boolean isOK, final ArrayList<String> urls) {
		dismissDialog();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				ArrayList<String> imageUrls;
				if(!isOK) {
					imageUrls = prefManager.getUrlsFromPref();
				}
				else
					imageUrls = urls;

				ViewPager pager = (ViewPager) findViewById(R.id.pager);

				if(imageUrls != null && !imageUrls.isEmpty()) {
					pager.setAdapter(new ImagePagerAdapter(imageUrls));
					pager.setCurrentItem(pagerPosition);
					pager.setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							/*float y = event.getY();
							if(y > (v.getHeight()* 4)/5 ) {
							return false;
							}
							else
							return true;*/
							return false;
						}
					});
				}
			}
		});


	}
}
