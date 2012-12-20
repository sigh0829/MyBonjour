package com.audric.bonjour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

public class LoadImagesFromSDCardActivity extends Activity implements
OnItemClickListener {

	/**
	 * Grid view holding the images.
	 */
	private GridView sdcardImages;
	/**
	 * Image adapter for the grid view.
	 */
	private ThumbnailsAdapter imageAdapter;
	/**
	 * Display used for getting the width of the screen. 
	 */
	private Display display;

	private BmDatabaseAdapter bmAdapter;

	private static final String TAG = LoadImagesFromSDCardActivity.class
			.getSimpleName();

	/**
			.getSimpleName();
	 * Creates the content view, sets up the grid, the adapter, and the click listener.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);        
		// Request progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.thumbnails_activity);

		display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		bmAdapter = new BmDatabaseAdapter(getApplicationContext());
		bmAdapter.open();
		setupViews();
		setProgressBarIndeterminateVisibility(true); 
		loadImages();
	}

	/**
	 * Free up bitmap related resources.
	 */
	protected void onDestroy() {
		super.onDestroy();
		bmAdapter.close();
		final GridView grid = sdcardImages;
		final int count = grid.getChildCount();
		ImageView v = null;
		for (int i = 0; i < count; i++) {
			v = (ImageView) grid.getChildAt(i);
			((BitmapDrawable) v.getDrawable()).setCallback(null);
		}
	}
	/**
	 * Setup the grid view.
	 */
	private void setupViews() {
		sdcardImages = (GridView) findViewById(R.id.gridView);
		sdcardImages.setNumColumns(display.getWidth()/95);
		sdcardImages.setClipToPadding(false);
		sdcardImages.setOnItemClickListener(LoadImagesFromSDCardActivity.this);
		imageAdapter = new ThumbnailsAdapter(getApplicationContext()); 
		sdcardImages.setAdapter(imageAdapter);
	}
	/**
	 * Load images.
	 */
	private void loadImages() {
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			new LoadImagesFromSDCard().execute();
		} else {
			final LoadedImage[] photos = (LoadedImage[]) data;
			if (photos.length == 0) {
				new LoadImagesFromSDCard().execute();
			}
			for (LoadedImage photo : photos) {
				addImage(photo);
			}
		}
	}
	/**
	 * Add image(s) to the grid view adapter.
	 * 
	 * @param value Array of LoadedImages references
	 */
	private void addImage(LoadedImage... value) {
		for (LoadedImage image : value) {
			imageAdapter.addBitmap(image.getBitmap());
			imageAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * Save bitmap images into a list and return that list. 
	 * 
	 * @see android.app.Activity#onRetainNonConfigurationInstance()
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		final GridView grid = sdcardImages;
		final int count = grid.getChildCount();
		final LoadedImage[] list = new LoadedImage[count];

		for (int i = 0; i < count; i++) {
			final ImageView v = (ImageView) grid.getChildAt(i);
			list[i] = new LoadedImage(((BitmapDrawable) v.getDrawable()).getBitmap());
		}

		return list;
	}
	/**
	 * Async task for loading the images from the SD card. 
	 * 
	 * @author Mihai Fonoage
	 *
	 */
	class LoadImagesFromSDCard extends AsyncTask<Object, LoadedImage, Object> {

		Cursor cursor = null;
		/**
		 * Load images from SD Card in the background, and display each image on the screen. 
		 *  
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Object doInBackground(Object... params) {
			//setProgressBarIndeterminateVisibility(true); 
			Bitmap bitmap = null;
			cursor = bmAdapter.fetchAllThumbnails(); 
			if(cursor!=null) {
				int size = cursor.getCount();
				// If size is 0, there are no images on the SD Card.
				if (size == 0) {
					//No Images available, post some message to the user
					runOnUiThread(new Runnable() {
						
						public void run() {
							Toast.makeText(getApplicationContext(),"No images found...",Toast.LENGTH_SHORT).show();
							
						}
					});
				}
				else {
					Log.e(TAG, "Trying to print : "+size+ " bmThumbnails");
					for (int i = 0; i < size; i++) {
						try {
							cursor.moveToPosition(i);
							Log.e(TAG, "LOADING: "+cursor.getString(BmDatabaseAdapter.dateidx));
							bitmap = CacheManager.getInstance(getApplicationContext()).loadThumbnails(cursor.getString(BmDatabaseAdapter.dateidx));//BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
							if (bitmap != null) {
								publishProgress(new LoadedImage(bitmap));
							}
							else
								Log.e(TAG, "bitmap == NULL");
						} catch (SQLException s) {

							s.printStackTrace();
						}
					}
				}
				cursor.close();
			}
			return null;
		}
		/**
		 * Add a new LoadedImage in the images grid.
		 *
		 * @param value The image.
		 */
		@Override
		public void onProgressUpdate(LoadedImage... image) {
			addImage(image);
		}
		/**
		 * Set the visibility of the progress bar to false.
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Object result) {
			setProgressBarIndeterminateVisibility(false);

		}

		@Override
		protected void onCancelled() {
			Log.e(TAG, "ON CANCELLED");
			cursor.close();
			super.onCancelled();
		}
	}


	/**
	 * A LoadedImage contains the Bitmap loaded for the image.
	 */
	private static class LoadedImage {
		Bitmap mBitmap;

		LoadedImage(Bitmap bitmap) {
			mBitmap = bitmap;
		}

		public Bitmap getBitmap() {
			return mBitmap;
		}
	}
	/**
	 * When an image is clicked, load that image as a puzzle. 
	 */
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {        
		Intent i = new Intent(LoadImagesFromSDCardActivity.this, MainActivity.class);
		i.putExtra(MainActivity.START_PAGE, position+1);
		startActivity(i);
	
	}

}
