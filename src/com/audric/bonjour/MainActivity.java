package com.audric.bonjour;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager; 
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.Toast;

/* 		TODO, trouver comment demander au serveur le nombre de madames de dispo ;)
		TODO il faut checker au depart si la dame de today est deja sortie. Car 
		sinon on va dl celle d'hier comme si c etait celle de today.
		A voir ac le flux rss (contient les dates et aussi plus facile d'y recuperer les photos (tout sur la meme page)


		revoir architecture globale. IMG loader : 
			-	separé en cacheloader && networkloader
			-	d'abord call to cacheloader, if not found call networkloader
			-	save of image to cache ds un thread à part, ralentit affichage.


//date avec rss,
//description aussi...


 */


public class MainActivity extends Activity  implements SwitchListener , OnErrorLoadingListener, OnFinishLoadingListener{


	private static final String TAG = MainActivity.class.getSimpleName();
	public static final int X_SCROLL_THREASOLD = 400;
	private static final String LASTMADAME = "LastMadame;)";
	public static final String START_PAGE = "STARTING_PAGE";

	public static final int NBMAXPAGES = 100;
	private int currentPage = 1;


	/* views */
	private ProgressBar progressBar;

	private BmLoader loader;
	private mImageViewTouch	mImageView;
	private Animation fadeInAnimation;	
	BmDatabaseAdapter bmDb = null;



	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent i = getIntent();
		if(i!=null) {
			currentPage = i.getIntExtra(START_PAGE, -1);
			
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if(currentPage==-1) {
			currentPage = (savedInstanceState==null?1:savedInstanceState.getInt(LASTMADAME));
		}
		
		//getApplicationContext().deleteDatabase(BmDatabaseAdapter.DATABASE_NAME);
		bmDb = new BmDatabaseAdapter(getApplicationContext());
		bmDb.open();
		fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fadein/*android.R.anim.fade_in*/);

		
		
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		mImageView = (mImageViewTouch)findViewById( R.id.imageView1 );
		mImageView.setOnSwitchListener(this);
	}




	@Override 
	public void onStart() {
		super.onStart();
		if(!isInternetOn())
			Toast.makeText(this, "Not connected to Internet.", Toast.LENGTH_SHORT).show();

		updateImage();

	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(LASTMADAME, currentPage);
	}



	@Override
	public void onStop () {
		//mImageView.setVisibility(View.INVISIBLE);
		super.onStop();
	}


	@Override
	public void onDestroy () {
		loader.shouldStop();
		if(bmDb != null)
			bmDb.close();	
		super.onDestroy();
	}





	/* Setting the menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.previous_page) {
			getPreviousPage();
			return true;
		} else if (itemId == R.id.update) {
			Log.d(TAG,"Updating..");
			updateImage();
			return true;
		} else if (itemId == R.id.next_page) {
			getNextPage();
			return true;
		} else if (itemId == R.id.delete_from_cache_and_update) {
			deleteFromCache(currentPage);
			updateImage();
			return true;
		} else if (itemId == R.id.firstPage) {
			goToFirstPage();
			return true;
		} else if (itemId == R.id.all_views) {
			Intent i = new Intent(MainActivity.this, LoadImagesFromSDCardActivity.class);
			//todo, add current madame in view
			//i.putExtra()
			startActivity(i);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}


	
	private void goToFirstPage() {
		currentPage = 1;
		updateImage();
	}





	/**
	 * delete the page passed in arguments from cache
	 * @param page the page to delete
	 */
	private void deleteFromCache(int page) {
		String nameOfImage = BmLoader.getDateOfImage(page-1);
		CacheManager.getInstance(this).cacheDelete(nameOfImage);
	}





	/**
	 * Check if we have internet connection
	 * @return true if connection available, false otherwise
	 */
	public Boolean isInternetOn()
	{ 
		final ConnectivityManager connMgr = (ConnectivityManager)  getSystemService(Context.CONNECTIVITY_SERVICE);   
		final android.net.NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if( wifi.isConnected() || mobile.isConnected() ) {			
			return true;
		}		
		return false;
	}



	public void getNextPage() {
		if (currentPage<MainActivity.NBMAXPAGES-1) {
			currentPage++;
			updateImage();
		}
	}

	public void getPreviousPage() {
		if (currentPage>1) {
			currentPage--;	
			updateImage();
		}
	}


	public void updateImage() {
		if(loader!=null)
			loader.shouldStop();

		if(!CacheManager.getInstance(getApplicationContext()).isInCache(currentPage)) {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.bringToFront();
		}
		loader = new BmLoader(currentPage, getApplicationContext());
		loader.setOnErrorListener(this);
		loader.setOnLoadingFinished(this);
		loader.start();

	}


	public void onSwitch(Direction direction) {
		if(direction == Direction.LEFT) {
			getPreviousPage();
		}
		else if(direction == Direction.RIGHT) {
			getNextPage();
		}
	}




	public void onLoadingFinished(final Bitmap bitmap, boolean inCache, final int page) {
		mImageView.post(new Runnable() {

			public void run() {
				if(page == currentPage) {
					progressBar.setVisibility(View.INVISIBLE);
					mImageView.setVisibility(View.INVISIBLE);
					mImageView.setImageBitmapReset(bitmap,0, true);
					mImageView.startAnimation(fadeInAnimation);
					mImageView.setVisibility(View.VISIBLE);
				}

			}
		});

	}




	public void onError(int what) {
		Log.e(TAG, "Caught an error.");

		progressBar.post(new Runnable() {
			public void run() {
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);

				Toast.makeText(getApplicationContext(), "Error...", Toast.LENGTH_SHORT).show();
				progressBar.setVisibility(View.INVISIBLE);
				mImageView.setVisibility(View.VISIBLE);
				Bitmap cleaner = Bitmap.createBitmap(dm.widthPixels,dm.heightPixels, Config.ALPHA_8);
				mImageView.setImageBitmapReset(cleaner, true);
			}
		});


	}
}
