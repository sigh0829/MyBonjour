package com.audric.bonjour;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;

public class CacheManager {

	private static final String TAG = "CacheManager";
	private static final String PNG_EXTENSION = ".png";
	private static final int MAX_CACHE_SIZE_MO = 50; //50 MO of cache is enough!
	private static final Long MAX_CACHE_SIZE_OCTETS = MAX_CACHE_SIZE_MO*1024*1024l;

	private static final String ThumbnailDirectory = "thumbnails";
	private static final int thumbnailHeight = 100;
	private static final int thumbnailWidth = 100;
	private static CacheManager singleton = null;
	private Context context;

	public static CacheManager getInstance(Context ctx) {
		if (singleton==null)
			singleton = new CacheManager(ctx);

		return singleton;
	}


	private CacheManager(Context ctx) {
		context = ctx;
	}
	

	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}





	/**
	 * The the bitmap to the cache directory of the application
	 * @param name the file's name to save the bitmap
	 * @param bitmap the bitmap to save
	 */
	public synchronized boolean saveImageToCache(final String date, final Bitmap bitmap) {
		/* if the external storage is available, get our cache dir */
		if(bitmap!=null && isExternalStorageWritable()) {
			if(isAllowedToSave()) {
				new Thread(new Runnable() {

					public void run() {	

						String newName = new String(date);
						if( !newName.endsWith(PNG_EXTENSION)) {
							newName += PNG_EXTENSION;
						}
						saveOriginal(bitmap,newName);
						/* save also the thumbnails to the directory of thumbnails */
						saveThumbnails(buildThumbnails(bitmap), newName);
						
					}
				}).start();
				return true;

			}
			else {
				Log.w(TAG, "Sorry, we estimate that the cache for our application is full");
				return false;
			}
		}
		else {
			Log.e(TAG,"External storage not available in write access.");
			return false;
		}
	}


	private void saveOriginal(Bitmap bitmap, String name) {
		if( ! name.endsWith(PNG_EXTENSION)) {
			name += PNG_EXTENSION;
		}
		File file = new File(context.getExternalCacheDir(), name);

		Log.d(TAG, "saving '"+ name+"' to cache");
		FileOutputStream os;
		try {
			os = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private boolean isAllowedToSave() {
		Long occupedOctets = getSizeOfCacheDirectory();
		Log.d(TAG,"occupedMOctects : "+ (occupedOctets/(1024*1024)) + " and max : "+ MAX_CACHE_SIZE_MO);
		return occupedOctets<=MAX_CACHE_SIZE_OCTETS;
	}




	/**
	 * Returns true if file like date + IMAGE_EXTENSION is found in the cache directory
	 * @param date : the strings which represents the date. Without extensions
	 * @return true if find, false otherwise
	 */
	public boolean isInCache(String name) {
		boolean imageFound = false;
		if(isExternalStorageWritable()) {
			if( ! name.endsWith(PNG_EXTENSION) )
				name+=  PNG_EXTENSION;

			File cacheDirectory = context.getExternalCacheDir();

			File[] fileList = cacheDirectory.listFiles();

			int indice=0;
			if(fileList!=null) {
				while (!imageFound && indice<fileList.length) {
					File temp = fileList[indice];
					if (temp.getName().equals(name))
						imageFound=true;
					indice++;
				}
			}
		}
		return imageFound;
	}




	public boolean isInCache(int page) {
		boolean imageFound = false;
		if(isExternalStorageWritable()) {
			String name = BmLoader.getDateOfImage(page-1);
			if (isInCache(name)) 
				imageFound = true;
		}
		return imageFound;
	}



	public Bitmap loadImage(String name) {
		/* if the external storage is available, get our cache dir */
		Bitmap bitmap = null;
		if(isExternalStorageWritable()) {

			
			if ( !name.endsWith( PNG_EXTENSION))
				name +=  PNG_EXTENSION;
			
			if(!isInCache(name))
				return null;
			
			File file = new File(context.getExternalCacheDir(), name);

			FileInputStream is; 
			try {
				is = new FileInputStream(file);

				bitmap = BitmapFactory.decodeStream(is);
				is.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		else {
			Log.e(TAG,"External storage not available in write access.");
		}

		return bitmap;
	}
	
	public Bitmap loadThumbnails(String name) {
		/* if the external storage is available, get our cache dir */
		Bitmap bitmap = null;
		if(isExternalStorageWritable()) {

			
			if ( !name.endsWith( PNG_EXTENSION))
				name +=  PNG_EXTENSION;
			Log.d(TAG, "thumbnails name to load : "+name);
			if(!isInCache(name))
				return null;
			File thumbnailsDir = new File(context.getExternalCacheDir(),ThumbnailDirectory);
			if(!thumbnailsDir.exists()) {
				return null;
			}
			File file = new File(thumbnailsDir,name);
			if(!file.exists())
				return null;
			
			FileInputStream is; 
			try {
				is = new FileInputStream(file);

				bitmap = BitmapFactory.decodeStream(is);
				is.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		else {
			Log.e(TAG,"External storage not available in write access.");
		}

		return bitmap;
	}






	/**
	 * get the size of all files in the cache directory
	 * @return size of all Files and files only (directories is bypassed here) in octets.
	 */
	private Long getSizeOfCacheDirectory() {
		Long result = 0l;
		if(isExternalStorageWritable()) {

			File cacheDirectory = context.getExternalCacheDir();

			File[] fileList = cacheDirectory.listFiles();
			for(File file : fileList) {
				if(file.isFile())
					result+=file.length();
			}
		}
		return result;
	}
















	/**
	 * Delete every file found in our externalCacheDirectory
	 */
	public void deleteAllCachedFiles() {
		if(isExternalStorageWritable()) {

			File cacheDirectory = context.getExternalCacheDir();
			Log.d(TAG,"Deleting every files in "+cacheDirectory.getAbsolutePath());
			File[] fileList = cacheDirectory.listFiles();
			int indice=0;
			File temp=null;
			while (indice<fileList.length) {
				temp = fileList[indice];
				if(!temp.isDirectory())
					temp.delete();
				indice++;
			}

			//TODO delete thumbnails also
		}
	}



	public void printCacheFiles() {
		if(isExternalStorageWritable()) {
			File cacheDirectory = context.getExternalCacheDir();
			File[] fileList = cacheDirectory.listFiles();
			for(File temp: fileList)
				Log.d(TAG,"cachedFile:"+temp.getName());
		}
	}


	public synchronized void cacheDelete(final String name) {
		Log.d(TAG, "deleting from cache: "+name);
		if(isExternalStorageWritable()) {
			new Thread(new Runnable() {

				public void run() {
					File cacheDirectory = context.getExternalCacheDir();

					String name_ext = new String(name);
					if ( !name.endsWith( PNG_EXTENSION))
						name_ext +=  PNG_EXTENSION;


					File temp = new File(cacheDirectory,name_ext);
					if (temp.exists()) {	
						temp.delete();
						Log.d(TAG, "File "+name_ext+" has been deleted!");
					}					

					/* and also the thumbnails */
					File thumbnailsDirectory = new File(cacheDirectory, ThumbnailDirectory);
					if(thumbnailsDirectory.exists()) {
						temp = new File(thumbnailsDirectory, name_ext);
						if (temp.exists()) {	
							temp.delete();
							Log.d(TAG, "Thumbnails "+name_ext+" has been deleted!");
						}
					}
				}
			}).start();

		} 
	}



	private Bitmap buildThumbnails(Bitmap source) {
		Bitmap thumbnails = ThumbnailUtils.extractThumbnail(source, thumbnailWidth, thumbnailHeight);
		return thumbnails;
	}




	/**
	 * save the thumbnails bitmap with the given name 
	 * @param thumbnails
	 * @param name
	 */
	private void saveThumbnails(Bitmap thumbnails, String name) {
		if(isExternalStorageWritable()) {
			if(isAllowedToSave()) {
				File cacheDir = context.getExternalCacheDir();
				File thumbnailsDir = new File(cacheDir,ThumbnailDirectory);
				if(!thumbnailsDir.exists()) {
					thumbnailsDir.mkdir();
				}
 
				
				if ( !name.endsWith( PNG_EXTENSION))
					name +=  PNG_EXTENSION;

				File file = new File(thumbnailsDir, name);

				Log.d(TAG, "saving '"+ name+"' to thumbnails dir:"+ThumbnailDirectory);
				FileOutputStream os;
				try {
					os = new FileOutputStream(file);
					thumbnails.compress(Bitmap.CompressFormat.PNG, 100, os);
					os.close();
				} catch (FileNotFoundException e) {
					Log.e(TAG,"Cannot find the file.");
					e.printStackTrace();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}








}
