package com.audric.bonjour;


import java.util.ArrayList;
import java.util.NoSuchElementException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class BmDatabaseAdapter {
	// Database Keys
	public static final String KEY_ROWID = "_id";
	public static final String KEY_DATE = "date";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_FILENAME = "filename";




	private static final String TAG = BmDatabaseAdapter.class
			.getSimpleName();

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static final String DATABASE_NAME = "bonjourmadame";
	private static final String IMAGE_TABLE = "images";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE_IMAGE = "create table "
			+ IMAGE_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_DATE + " text not null,"
			+ KEY_FILENAME + " text not null,"
			+ KEY_DESCRIPTION + "	text );";



	public static final int rowidx = 0;
	public static final int dateidx = 1;
	public static final int filenameidx = 2;
	public static final int descidx = 3;



	private Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);

		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_IMAGE);
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_CREATE_IMAGE);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public BmDatabaseAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the InputDevices database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public BmDatabaseAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();	
		return this;
	}

	public void close() {
		mDbHelper.close();
		mCtx = null;
	}



	/**
	 * add the given entry, or edit the existing one
	 * note : you need to commit changes at the end.
	 * @param timestamp the timestamp as string 
	 * @param desc the description of this image
	 */
	public void addEntry(String timestamp, String desc, String filename) {

		ContentValues values = new ContentValues();
		values.put(KEY_DATE, timestamp);
		values.put(KEY_DESCRIPTION, desc);
		values.put(KEY_FILENAME, filename);
		if (isInDatabase(timestamp)) {
			Log.d(TAG, "updating entry : " + timestamp + " desc:'"+desc+ "' filename:" +filename);
			mDb.update(IMAGE_TABLE, values, KEY_DATE + " = " + timestamp, null);
		}
		else {
			Log.d(TAG, "inserting entry : " + timestamp + " desc:'"+desc+ "' filename:" +filename);
			mDb.insert(IMAGE_TABLE, null, values);
		}
	}




	/**
	 * Returns true if the timestamp is an existing one in database
	 * @param timestamp
	 * @return
	 * @throws SQLException
	 */
	public boolean isInDatabase(String timestamp) throws SQLException {
		Cursor mCursor = mDb.query
				(true, IMAGE_TABLE,
						new String[] {KEY_DATE},
						KEY_DATE + "= '" + timestamp +"'", null, null, null, null, null);
		if (mCursor != null && mCursor.getCount()>0) {
			mCursor.close();	
			return true;
		}
		else if(mCursor!=null) 
			mCursor.close();

		return false;
	}




	public String getDesc(String timestamp) throws NoSuchElementException {
		Cursor mCursor = mDb.query
				(true, IMAGE_TABLE,
						new String[] {KEY_DATE, KEY_DESCRIPTION},
						KEY_DATE + "= '" + timestamp +"'", null, null, null, null, null);

		String desc = null;
		if(mCursor != null) {
			desc = mCursor.getString(descidx);
			mCursor.close();
			return desc;
		}
		else
			throw new NoSuchElementException("No entry with timestamp " + timestamp + "found in db");
	}


	private ArrayList<String> fetchAllSuffixes() {
		Cursor mCursor = mDb.query
				(IMAGE_TABLE,
						new String[] {KEY_FILENAME},
						null, null, null, null, KEY_DATE + " DESC", null);

		if (mCursor!= null && mCursor.getCount() != 0) {
			mCursor.moveToFirst();
			ArrayList<String> suffixes = new ArrayList<String>();

			for (int index = 0; index < mCursor.getCount(); index++) {
				suffixes.add(mCursor.getString(0));
				mCursor.moveToNext();
			}

			mCursor.close();
			return suffixes;
		}
		else if (mCursor!=null)
			mCursor.close();
		return null;

	}

	public ArrayList<String> fetchAllUrls() {

		ArrayList<String> suffixes = fetchAllSuffixes();
		if( suffixes != null) {
			ArrayList<String> urls = new ArrayList<String>();
			for (String suffixe : suffixes) {
				urls.add(WebServiceClient.prefix + suffixe);
			}
			return urls;
		}
		else
			return null;

	}

	public long fetchTimestamp(String image_url) throws NoSuchElementException{
		if( image_url != null) {
			int start = image_url.indexOf("/image/");
			if(start == -1)
				throw new NoSuchElementException("Can't find a entry in db with : " + image_url);

			String suffixe = image_url.substring(start);
			Cursor mCursor = mDb.query
					(IMAGE_TABLE,
							new String[] {KEY_DATE},
							KEY_FILENAME + " LIKE \"" + suffixe + "\"", null, null, null, null);

			if (mCursor!= null && mCursor.getCount() != 0) {
				mCursor.moveToFirst();
				String timestamp;
				timestamp = mCursor.getString(0);

				mCursor.close();
				return Long.valueOf(timestamp);
			}
			else if (mCursor!=null)
				mCursor.close();


		}
		throw new NoSuchElementException("Can't find a entry in db with : " + image_url);
	}




	public String fetchDescription(String image_url) throws NoSuchElementException{

		if( image_url != null) {
			int start = image_url.indexOf("/image/");
			if(start == -1)
				throw new NoSuchElementException("Can't find a entry in db with : " + image_url);

			String suffixe = image_url.substring(start);
			Cursor mCursor = mDb.query
					(IMAGE_TABLE,
							new String[] {KEY_DESCRIPTION},
							KEY_FILENAME + " LIKE \"" + suffixe + "\"", null, null, null, null);

			if (mCursor!= null && mCursor.getCount() != 0) {
				mCursor.moveToFirst();
				String description = mCursor.getString(0);

				mCursor.close();
				return description;
			}
			else if (mCursor!=null)
				mCursor.close();
		}
		throw new NoSuchElementException("Can't find a entry in db with : " + image_url);
	}


}
