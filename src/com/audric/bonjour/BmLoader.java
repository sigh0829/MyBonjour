package com.audric.bonjour;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BmLoader extends Thread {
	static final String DEFAULT_URL = "http://www.bonjourmadame.fr/";
	private String siteURL = DEFAULT_URL;

	private static final String TAG = BmLoader.class.getSimpleName();

	private static final String PHOTO_PANEL = "<div class=\"photo-panel\">";
	private static final String IMG_SRC = "<img src=\"";


	private int page;
	//private String date; 
	private Context context;

	BmDatabaseAdapter bmDb;

	private OnFinishLoadingListener finishListener = null;
	private OnErrorLoadingListener errorListener = null;

	public BmLoader(int page , Context context) {
		super();
		this.page = page;
		this.context = context;
		siteURL = buildUrl(page);
		//date = getDateOfImage(page-1);
		bmDb = new BmDatabaseAdapter(context);
		bmDb.open();
		
		//Log.i(TAG,"New loader with date:"+date+"  siteUrl:"+siteURL);
	}


	public void shouldStop() {
		context = null;
		finishListener = null;
		errorListener = null;
		bmDb.close();
	}


	public void run() {
		loadImage();
		shouldStop();
	}
	
	
	
	private void loadImage() {
		Bitmap bitmap = null;
		String dateOfImage = getDateOfImage(page-1);

		//first check if this page is in Database
		if(bmDb.isInDatabase(dateOfImage)) {
			Log.d(TAG, "The date '" + dateOfImage + "' is found in Database. load it from cache");	
			bitmap = loadFromCache(dateOfImage);
		}
		else
			Log.d(TAG, "The date '" + dateOfImage + "' is not found in Database");
			

		/* if valid image found in cache */
		if(bitmap!=null) {
			Log.d(TAG,"Image "+dateOfImage+ " found in cache ");
			onFinished(bitmap, true, page);
		} 
		else {
			Log.d(TAG,"Image not in cache, downloading it from "+siteURL);
			bitmap = loadFromNetwork(dateOfImage);
			
			
			if(bitmap==null) 
				onError(1);
			else {
				onFinished(bitmap, false, page);
				//.saveImageToCache(date, bitmap);
				bmDb.createImage(dateOfImage, dateOfImage, "", bitmap);
			}
		}
		
		
		
		
		

	}
	
	
	
	
	private Bitmap loadFromCache(String date) {
		Bitmap bitmapInCache = null;
		
		CacheManager manager = CacheManager.getInstance(context);
		if(manager.isInCache(date))
			bitmapInCache = manager.loadImage(date);
		
		return bitmapInCache;
	}
	
	
	private Bitmap loadFromNetwork(String date) {
		/* extract the image's url from the html page */
		String imageUrl = extractImageUrl(siteURL);
		Bitmap bitmap = null;
		if(imageUrl==null) 
			onError(2);
		else {
			/* and get it from the imgUrl */
			try {
				bitmap = loadImageFromUrl(new URL(imageUrl));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			
		}
		return bitmap;
	}

	
	public void setOnErrorListener(OnErrorLoadingListener listener) {
		this.errorListener = listener;
	}
	
	public void setOnLoadingFinished(OnFinishLoadingListener listener) {
		this.finishListener = listener;
	}

	private void onError(int what) {
		if(errorListener!=null) {
			errorListener.onError(what);
		}
	}

	private void onFinished(Bitmap bitmap, boolean inCache, int page) {
		if(finishListener!=null) {
			finishListener.onLoadingFinished(bitmap, inCache, page);
		}
	}




	private static String buildUrl(int page) {
		String url = null;
		if(page>0) {
			if(page == 1)
				url = DEFAULT_URL;
			else
				url = DEFAULT_URL + "page/" +page +"/";
		}
		return url;
	}



	/**
	 * get in string the date of there is daysOffset days
	 * @param daysOffset : number of days to go on back from today
	 * @return the string as year-month-day
	 */
	public static String getDateOfImage(int daysOffset) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -daysOffset);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		StringBuilder retb = new StringBuilder();
		retb.append(year);
		retb.append("-");
		if(month<10) 
			retb.append("0");
		retb.append(month);
		retb.append("-");
		if(day<10) 
			retb.append("0");
		retb.append(day);
		
		Log.d(TAG, "Date : retb"+retb.toString());
		//"" + year +"-"+ month +"-"+ day;
		return retb.toString();
	}



	/**
	 * load an image from the url passed.
	 * @param url
	 * @return
	 */
	private static Bitmap loadImageFromUrl(URL url) {
		Bitmap image = null;
		try {
			InputStream in = url.openStream();
			image = BitmapFactory.decodeStream(new PatchInputStream(in));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
	}





	/**
	 * extract image's url (read web page until it founds img panel)
	 * @param url
	 * @return
	 */
	private static String extractImageUrl(String url) {
		String imageUrl = null;
		HttpResponse response;
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		try {
			response =  client.execute(request);

			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in), 512);//new BufferedReader(new InputStreamReader(in));

			String line = null;
			String imgLine = null;

			boolean mustContinue = true;;
			while((line = reader.readLine()) != null && mustContinue) {

				if (line.contains(PHOTO_PANEL)) {
					while((line = reader.readLine()) != null && mustContinue) {
						if(line.contains(IMG_SRC)) {
							mustContinue = false;
							imgLine = line;
							Log.d(TAG,"img line found!");
							break;
						}
					}
				}

			}
			in.close();

			/* i.e. we find it */
			if (imgLine!=null && !mustContinue) {
				int start = imgLine.indexOf(IMG_SRC)+IMG_SRC.length();
				int end = imgLine.indexOf("\"", start);
				imageUrl = imgLine.substring(start, end);
			}

		}
		catch (IOException io) {
			io.printStackTrace();
		}

		return imageUrl;
	}





}
