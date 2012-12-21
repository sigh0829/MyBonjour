package com.audric.bonjour;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;


public class BmDatabaseAdapter {
	// Database Key
	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_DATE = "date";
	public static final String KEY_DESCRIPTION = "description";



	private static final String TAG = BmDatabaseAdapter.class
			.getSimpleName();

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static final String DATABASE_NAME = "bonjourmadame";
	private static final String IMAGE_TABLE = "images";
	private static final String THUMBNAILS_TABLE = "thumbnails";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE_IMAGE = "create table "
			+ IMAGE_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_NAME + " text not null,"
			+ KEY_DATE + " text not null,"
			+ KEY_DESCRIPTION + "	text not null);";



	private static final String DATABASE_CREATE_THUMBNAILS = "create table "
			+ THUMBNAILS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_NAME + " text not null,"
			+ KEY_DATE + " text not null," 
			+ KEY_DESCRIPTION + "	text not null);";

	// idx in DB
	public static final int rowidx = 0;
	public static final int nameidx = 1;
	public static final int dateidx = 2;
	public static final int descidx = 3;



	private Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_IMAGE);
			db.execSQL(DATABASE_CREATE_THUMBNAILS);
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_CREATE_IMAGE);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_CREATE_THUMBNAILS);
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
		//printAllEntries();
		return this;
	}

	public void close() {
		mDbHelper.close();
		mCtx = null;
	}


	public void printAllEntries() {
		Cursor thumbnails = fetchAllThumbnails();

		if(thumbnails==null)
			Log.i(TAG, "PRINT ALL HUMBNAILSthumbnails entry NULL");
		else {
			while(!thumbnails.isAfterLast() && !thumbnails.isBeforeFirst() && !thumbnails.isClosed()) {
				Log.i(TAG, "thumbnails entry: name: '"+thumbnails.getString(1) + "';  date: '"
						+thumbnails.getString(2) +"';  description: '"+thumbnails.getString(3) + "'");
				thumbnails.moveToNext();
			}
			thumbnails.close();
		}
	}



	public void createImage(String name, String date, String desc, Bitmap bitmap) {
		if(CacheManager.getInstance(mCtx).saveImageToCache(name, bitmap)) {
			if(!isInDatabase(name)) {
				ContentValues initialValues = new ContentValues();
				initialValues.put(KEY_DATE, date);
				initialValues.put(KEY_DESCRIPTION, desc);
				initialValues.put(KEY_NAME, name);
				Log.d(TAG, "Adding new entry to "+DATABASE_NAME+ " name:"+ name + " date:"+date);


				mDb.insert(THUMBNAILS_TABLE, null, initialValues);
				mDb.insert(IMAGE_TABLE, null, initialValues);
			}
			else {
				ContentValues initialValues = new ContentValues();
				initialValues.put(KEY_DATE, date);
				initialValues.put(KEY_DESCRIPTION, desc);
				initialValues.put(KEY_NAME, name);
				Log.d(TAG, "updatig entry to "+DATABASE_NAME+ " name:"+ name + " date:"+date);

				mDb.update(IMAGE_TABLE, initialValues, KEY_NAME +"= '" + name + "'", null);
				mDb.update(THUMBNAILS_TABLE, initialValues, KEY_NAME +"= '" + name + "'", null);
			}
		}
		else {
			Log.d(TAG, "Failed to save new entry to "+DATABASE_NAME+ " name:"+ name + " date:"+date);
		}


	}

	public Cursor fetchAllThumbnails() {
		Cursor cursor = mDb.query(THUMBNAILS_TABLE,
				new String[] { KEY_ROWID, KEY_NAME, KEY_DATE, KEY_DESCRIPTION}, null,
				null, null, null, KEY_DATE + " DESC");
		if(cursor!=null)
			cursor.moveToFirst();

		//Log.e(TAG, "we have "+cursor.getCount()+ " thumbnails in DB");
		return cursor;
	}


	public boolean isInDatabase(String date) throws SQLException {
		Cursor mCursor =
				mDb.query(true, IMAGE_TABLE,
						new String[] { KEY_ROWID, KEY_NAME, KEY_DATE, KEY_DESCRIPTION},
						KEY_DATE + "= '" + date +"'", null, null, null, null, null);
		if (mCursor != null && mCursor.getCount()>0) {
			Log.d(TAG, "FOUND ONE MADAME IN DB FOR :"+date);
			mCursor.close();	
			return true;
		}
		else if(mCursor!=null)
			mCursor.close();
		

		return false;
	}


	public Cursor fetchImage(String date) throws SQLException {
		Cursor mCursor =
				mDb.query(true, IMAGE_TABLE,
						new String[] { KEY_ROWID, KEY_NAME, KEY_DATE, KEY_DESCRIPTION},
						KEY_DATE + "=" + date, null, null, null, null, null);
		if (mCursor != null) {
			Log.d(TAG, "FOUND ONE MADAME IN DB FOR :"+date);
			mCursor.moveToFirst();
		}

		return mCursor;
	}

	public void deleteImage(String date) throws SQLException {
		Log.d(TAG, "Deleting " + date + " from DB");
		mDb.delete(THUMBNAILS_TABLE, KEY_DATE + "='" + date + "'", null) ;
		mDb.delete(IMAGE_TABLE, KEY_NAME + "='" + date + "'", null) ;
		CacheManager.getInstance(mCtx).cacheDelete(date);

	}
}
