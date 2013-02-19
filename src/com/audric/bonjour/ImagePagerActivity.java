package com.audric.bonjour;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.audric.bonjour.WebServiceClient.OnUrlsLoadingListener;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class ImagePagerActivity extends BaseActivity 
implements OnUrlsLoadingListener {
	private static final String TAG = ImagePagerActivity.class.getSimpleName();

	private int pagerPosition;
	private TextView date_tv;
	private TextView description_tv;
	private ViewPager pager;
	private BmDatabaseAdapter mDb;
	private ImagePagerAdapter imageAdapter = null;
	private Animation fadein;
	private Animation fadeout;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_image_pager);

		fadein = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadein);
		fadeout = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadeout);

		Bundle bundle = getIntent().getExtras();
		prepareDialog(ImagePagerActivity.this);

		WebServiceClient ws = WebServiceClient.getInstance();
		ws.retrieveMadamesUrlsInThread(this);

		mDb = new BmDatabaseAdapter(getApplicationContext());
		mDb.open();

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

		pager = (ViewPager) findViewById(R.id.pager);
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

			imageLoader.displayImage(images.get(position), ( ImageView) imageView, options, new SimpleImageLoadingListener() {
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
					imageView.setOnClickListener(new ShowDetailsClickListener());
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
					imageUrls = mDb.fetchAllUrls();
				}
				else
					imageUrls = urls; 

				if(imageUrls != null && !imageUrls.isEmpty()) {
					imageAdapter = new ImagePagerAdapter(imageUrls);
					pager.setAdapter(imageAdapter);
					pager.setCurrentItem(pagerPosition);
					pager.setOnPageChangeListener(new OnPageChangedListerner());
					date_tv = (TextView) findViewById(R.id.tv_date_pager);
					description_tv = (TextView) findViewById(R.id.tv_desc_pager);
				}
			}
		});


	}

	@Override
	protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	private class ShowDetailsClickListener implements OnClickListener  {

		@Override
		public void onClick(View v) {
			if(imageAdapter != null) {
				String date = getDateFromPage();

				if (date != null) {
					date_tv.setText(date);
					if (date_tv.getVisibility() == View.INVISIBLE) {
						date_tv.startAnimation(fadein);
						date_tv.setVisibility(View.VISIBLE);
					}
					else {
						date_tv.startAnimation(fadeout);
						date_tv.setVisibility(View.INVISIBLE);
					}
				}

				String desc = getDescriptionFromPage();
				//desc = "Proposé par Morback";
				if(desc != null) {
					description_tv.setText(desc);
					if (description_tv.getVisibility() == View.INVISIBLE) {
						description_tv.startAnimation(fadein);
						description_tv.setVisibility(View.VISIBLE);
					}
					else {
						description_tv.startAnimation(fadeout);
						description_tv.setVisibility(View.INVISIBLE);
					}
				}
				else
					description_tv.setVisibility(View.INVISIBLE);
			}
		}

	}


	private class OnPageChangedListerner extends ViewPager.SimpleOnPageChangeListener {
		@Override
		public void onPageScrollStateChanged(int state) {
			if (state == ViewPager.SCROLL_STATE_IDLE) {
				String date = getDateFromPage();
				if(date != null)
					date_tv.setText(date);
			}
		}
	}



	@SuppressLint("SimpleDateFormat") 
	private String getDateFromPage() {
		try {
			String image_url = imageAdapter.images.get(pager.getCurrentItem());

			long timestamp = mDb.fetchTimestamp(image_url);

			Date test = new Date(timestamp * 1000);
			SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy");
			String date = format.format(test);
			return date;
		}
		catch (NoSuchElementException e ) {

			return null;
		}

	}

	private String getDescriptionFromPage() {
		try {
			String image_url = imageAdapter.images.get(pager.getCurrentItem());

			String description = mDb.fetchDescription(image_url);
			Log.e(TAG, "description : " +description);
			if(description != null && description.equals(""))
				return null;
			return description;
		}
		catch (NoSuchElementException e ) {
			return null;
		}
	}
}
