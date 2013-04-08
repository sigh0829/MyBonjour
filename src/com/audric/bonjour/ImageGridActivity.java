package com.audric.bonjour;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.audric.bonjour.WebServiceClient.OnUrlsLoadingListener;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class ImageGridActivity extends BaseActivity implements OnUrlsLoadingListener {
	private static final String TAG = ImageGridActivity.class.getSimpleName();

	private ArrayList<String> imageUrls;
	private DisplayImageOptions options;

	private GridView gridView;
	private BmDatabaseAdapter mDb;


	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_image_grid);

		//getApplicationContext().deleteDatabase(BmDatabaseAdapter.DATABASE_NAME);

		WebServiceClient ws = WebServiceClient.getInstance();

		prepareDialog(ImageGridActivity.this);
		ws.resetSuffixes();
		ws.retrieveMadamesUrlsInThread(this);

		options = new DisplayImageOptions.Builder()
		.showStubImage(R.drawable.stub_image)
		.showImageForEmptyUri(R.drawable.image_for_empty_url)
		.cacheInMemory()
		.cacheOnDisc()
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();

		mDb = new BmDatabaseAdapter(getApplicationContext());
		mDb.open();

		ImageLoaderConfiguration configuration = ImageLoaderConfiguration.createDefault(getApplicationContext());
		imageLoader.init(configuration);

		gridView = (GridView) findViewById(R.id.gridview);
	}

	private void startImagePagerActivity(int position) {
		Intent intent = new Intent(this, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGES, imageUrls);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		startActivity(intent);
	}

	public class ImageAdapter extends BaseAdapter {
		ArrayList<String> urls;
		public ImageAdapter(ArrayList<String> urls) {
			super();
			this.urls = urls;

		}
		@Override
		public int getCount() {
			return urls.size();
		}

		@Override
		public Object getItem(int position) {
			return urls.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView;
			if (convertView == null) {
				imageView = (ImageView) getLayoutInflater().inflate(R.layout.item_grid_image, parent, false);
			} else {
				imageView = (ImageView) convertView;
			}

			imageLoader.displayImage(urls.get(position), imageView, options);
			return imageView;
		}
	}



	private void saveSuffixes() {
		if (WebServiceClient.getSuffixesHasChanged()) {
			new Thread( new Runnable() {
				@Override
				public void run() {
					ArrayList<String> suffixes = WebServiceClient.getSuffixes();
					String date;
					String desc = "";
					int start = 7, end;


					if( suffixes != null) {
						for (String item : suffixes) {
							end = item.length() - 4;
							date = item.substring(start, end);
							mDb.addEntry(date, desc, item);
						}
					}
					WebServiceClient.setSuffixesHasChanged(false);
				}
			}).run();


		}
		else
			Log.d(TAG, "Suffixes not changed. Nothing to do");
	}


	@Override
	protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_grid_activity, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.change_ip:

			AlertDialog.Builder builder = new AlertDialog.Builder(ImageGridActivity.this);
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_chge_ip, null);
			builder.setView(layout)
			.setTitle(R.string.new_ip)
			.setPositiveButton("OK", null)
			.setNegativeButton("Cancel", null)
			.show();
			break;

		case R.id.update_gridview:
			Log.d(TAG, "update");
			break;

		case R.id.delete_cache_gridview:
			new Thread(new Runnable() {

				@Override
				public void run() {
					imageLoader.clearDiscCache();
					imageLoader.clearMemoryCache();					
				}
			}
					).start();
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void urlsLoadingFinished(final boolean isOK, final ArrayList<String> urls) {
		dismissDialog();
		runOnUiThread(new Runnable() {


			@Override
			public void run() {
				if(!isOK) {
					Toast.makeText(getApplicationContext(), R.string.loadfrommemory, Toast.LENGTH_SHORT)
					.show();
					if(mDb!= null)
						imageUrls = mDb.fetchAllUrls(); 
				}
				else
					imageUrls = urls;


				if(imageUrls != null ) {
					gridView.setAdapter(new ImageAdapter(imageUrls));
					gridView.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							startImagePagerActivity(position);
						}
					});
					gridView.setOnScrollListener(new PauseOnScrollListener(true, true));
					saveSuffixes();
				}
				else
					Log.e(TAG, "can not retrieve urls");

			}

		});





	}
}