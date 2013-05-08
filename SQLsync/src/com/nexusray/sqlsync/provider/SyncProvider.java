package com.nexusray.sqlsync.provider;

import java.util.ArrayList;
import java.util.List;

import com.nexusray.sqlsync.settings.SyncSettings;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.preference.PreferenceManager;

/*
 * Provider adds batch operations and synchronizations with remote database to standart provider.
 * Extend this class to provide custom query executions.
 */
public abstract class SyncProvider extends ContentProvider implements
		SQLiteTransactionListener {

	protected SQLiteOpenHelper dbHelper;

	private ThreadLocal<Boolean> useBatch = new ThreadLocal<Boolean>();

	private boolean preformNetSync = false;
	
	private static final UriMatcher syncUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int UM_START_SYNC = 1;
	private static final int UM_END_SYNC = 2;
	
	static
    {
		syncUriMatcher.addURI(SyncSettings.AUTHORITY, "StartSync", UM_START_SYNC);
		syncUriMatcher.addURI(SyncSettings.AUTHORITY, "EndSync", UM_END_SYNC);
    }
	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		dbHelper = getDatabaseHelper(getContext());
		useBatch.set(false);

		return true;
	}

	abstract protected SQLiteOpenHelper getDatabaseHelper(Context context);
	
	abstract protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs);
	abstract protected Uri insertInTransaction(Uri uri, ContentValues values);
	abstract protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs);
	abstract protected Cursor customQuery(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder);

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int rowsAffected = 0;
		boolean isCallerSync = isCallerSyncAdapter(uri);

		if (!useBatch()) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();

			db.beginTransactionWithListener(this);
			try {
				rowsAffected = deleteInTransaction(uri, selection, selectionArgs);
				if (rowsAffected > 0)
					preformNetSync(!isCallerSync);
				db.setTransactionSuccessful();
				return rowsAffected;
			} finally {
				db.endTransaction();
				if (rowsAffected > 0)
					notifyChange();
			}
		} else {
			rowsAffected = deleteInTransaction(uri, selection, selectionArgs);
			if (rowsAffected > 0)
				preformNetSync(!isCallerSync);
			return rowsAffected;
		}
	}

	protected void preformNetSync(boolean sync) {
		preformNetSync |= sync;
	}
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO Delete this
		return null;
	}
	
	private synchronized int generateId() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		int lastId = preferences.getInt("_sync_lastid", 0) + 1;
		int deviceId = preferences.getInt("_sync_deviceid", 0);
		
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("_sync_lastid", lastId);
		editor.commit();
		
		int length = String.valueOf(deviceId).length();
		
		return lastId*(int) Math.pow(10, length+1) + deviceId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {

		Uri result = null;
		boolean isCallerSync = isCallerSyncAdapter(uri);

		if (!useBatch()) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();

			db.beginTransactionWithListener(this);
			try {
				values.put(SyncDatabaseHelper.syncID, generateId());
				result = insertInTransaction(uri, values);
				if (result != null)
					preformNetSync(!isCallerSync);
				db.setTransactionSuccessful();
				return result;
			} finally {
				db.endTransaction();
				if (result != null)
					notifyChange();
			}
		} 
		else {
			
			values.put(SyncDatabaseHelper.syncID, generateId());
			result = insertInTransaction(uri, values);
			if (result != null)
				preformNetSync(!isCallerSync);
			return result;
		}
	}

	/*
	 * Reads from Uri whether it is called from SyncAdapter. Uri ends with
	 * "?sync=1" if so.
	 */
	protected boolean isCallerSyncAdapter(Uri uri) { 
		
		if (uri == null)
			return false;
		
		String s = uri.getQueryParameter("sync");
		if (s == null)
			return false;

		if (s.equals("1"))
			return true;

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		boolean isCallerSync = isCallerSyncAdapter(uri);
		
		if (!isCallerSync) {
			return customQuery(uri, projection, selection,
					selectionArgs, sortOrder);
		}
		else {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			String tableName = getTableName(uri);
			if (tableName == null) 
				return null;
			
			db.beginTransactionWithListener(this);
			try {
				
				Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);	
				db.setTransactionSuccessful();
				
				return c;
			}
			finally {
				db.endTransaction();
			}
			
		}
			

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int rowsAffected = 0;
		boolean isCallerSync = isCallerSyncAdapter(uri);
		if (!isCallerSync)
		{
			if (!useBatch()) {
				SQLiteDatabase db = dbHelper.getWritableDatabase();
	
				db.beginTransactionWithListener(this);
				try {
					rowsAffected = updateInTransaction(uri, values, selection,
							selectionArgs);
					if (rowsAffected > 0)
						preformNetSync(!isCallerSync);
					db.setTransactionSuccessful();
					return rowsAffected;
				} finally {
					db.endTransaction();
					if (rowsAffected > 0)
						notifyChange();
				}
			} else {
				rowsAffected = deleteInTransaction(uri, selection, selectionArgs);
				if (rowsAffected > 0)
					preformNetSync(!isCallerSync);
				return rowsAffected;
			}
		}
		else
		{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			int match = syncUriMatcher.match(uri);
			ContentValues cv;
			
			switch (match)
			{
			case UM_START_SYNC:
				
				cv = new ContentValues(1);
				cv.put("_syncStatus", 1);
				
				db.beginTransactionWithListener(this);
				try {
					db.update("__SQLsyncDeletedRows", cv, null, null);
					
					for (String tableName : SyncSettings.tablesToSync) {
						db.execSQL("DROP TRIGGER IF EXISTS _sync_update_" + tableName);
						
						db.update(tableName, cv, "_lastChange IS NOT NULL", null);
					
						db.execSQL("CREATE TRIGGER _sync_update_" + tableName + " " +
						     "BEFORE UPDATE ON " + tableName + " " +
						     "FOR EACH ROW BEGIN " +
						     "UPDATE " + tableName + " SET _syncStatus=2, _lastChange=datetime('now') " +
						     "WHERE _ID = NEW._ID; " +
						     "END;");
					}
					
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
					
				}
				
				notifyChange();
				return 0;	
			case UM_END_SYNC:
				
				cv = new ContentValues(1);
				cv.put("_syncStatus", 0);
				//cv.putNull("_lastChange");
				
				db.beginTransactionWithListener(this);
				try {
					
					for (String tableName : SyncSettings.tablesToSync) {
						db.execSQL("DROP TRIGGER IF EXISTS _sync_update_" + tableName);
						
						db.update(tableName, cv, "_syncStatus!=0", null);
						
						db.execSQL("CREATE TRIGGER _sync_update_" + tableName + " " +
							     "BEFORE UPDATE ON " + tableName + " " +
							     "FOR EACH ROW BEGIN " +
							     "UPDATE " + tableName + " SET _lastChange=datetime('now') " +
							     "WHERE _ID = NEW._ID; " +
							     "END;");
						
					}
					
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}	
				
				notifyChange();					
				return 0;
			default:
				String tableName = getTableName(uri);
				if (tableName == null) 
					return 0;
				int no = db.update(tableName, values, selection, selectionArgs);
				//notifyChange();
				return no;

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#bulkInsert(android.net.Uri,
	 * android.content.ContentValues[])
	 */
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {

		int rowsAffected = 0;
		boolean isCallerSync = isCallerSyncAdapter(uri);
		SQLiteDatabase db = dbHelper.getWritableDatabase();			
		if (!isCallerSync)
		{
			db.beginTransactionWithListener(this);
			try {
				useBatch.set(true);
				int noValues = values.length;
				for (int i = 0; i < noValues; i++) {
					if (insert(uri, values[i]) != null) {
						rowsAffected += 1;
						preformNetSync(!isCallerSync);
					}
					db.yieldIfContendedSafely();
				}
				db.setTransactionSuccessful();
				return rowsAffected;
			} finally {
				db.endTransaction();
				useBatch.set(false);
				if (rowsAffected > 0) 
					notifyChange();
			}
		}
		else
		{
			db.beginTransactionWithListener(this);
			try {				
				int noValues = values.length;
				String tableName = getTableName(uri);
				for (int i = 0; i < noValues; i++) {
					String gid = values[i].getAsString(SyncDatabaseHelper.syncID);
					
					Cursor c = db.query(tableName, new String[] {"_syncStatus"}, SyncDatabaseHelper.syncID + "=" + gid, null, null, null, null);
					
					if (c.getCount() > 0) {
						c.moveToFirst();
						//_syncStatus must be 1 or it will delay sync to next time
						//System.out.println(c.getString(0));
						if (c.getInt(0) != 2 && db.update(tableName, values[i], SyncDatabaseHelper.syncID + "=" + gid, null) != 0) {
							rowsAffected += 1;
							preformNetSync(!isCallerSync);
						}
						c.close();
					}
					else
					{
						c.close();
						c = db.query("__SQLsyncDeletedRows", new String[] {SyncDatabaseHelper.syncID},
								"tableName='" + tableName + "' AND "+ SyncDatabaseHelper.syncID +"=" + gid,
								null, null, null, null);
						if (c.getCount() == 0 && db.insert(tableName, null, values[i]) != 0) {
							rowsAffected += 1;
							preformNetSync(!isCallerSync);
						}
						c.close();
					}
					db.yieldIfContendedSafely();
				}
				
				db.setTransactionSuccessful();
				return rowsAffected;
			} finally {
				db.endTransaction();
				if (rowsAffected > 0)
					notifyChange();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#applyBatch(java.util.ArrayList)
	 */
	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		boolean dbChanged = false;

		db.beginTransactionWithListener(this);

		try {
			int nOps = operations.size();
			ContentProviderResult[] results = new ContentProviderResult[nOps];
			useBatch.set(true);

			for (int i = 0; i < nOps; i++) {
				ContentProviderOperation operation = operations.get(i);
				if (i > 0 && operation.isYieldAllowed()) {
					db.yieldIfContendedSafely(SyncSettings.SLEEP_AFTER_YIELD_DELAY);
				}
				results[i] = operation.apply(this, results, i);
				if (results[i] != null)
					dbChanged = true;
			}

			db.setTransactionSuccessful();
			return results;
		} finally {
			db.endTransaction();
			useBatch.set(false);
			if (dbChanged)
				notifyChange();
		}

	}

	/*
	 * Notifies SyncManager to call Syncadapter if preformNetSync == true. Notifies
	 * CursorObservers and CursorAdapters.
	 */
	protected void notifyChange() {
		//if (preformNetSync) System.out.println("PreformingNetSync!");
		getContext().getContentResolver().notifyChange(
				SyncSettings.AUTHORITY_URI, null, preformNetSync);
		preformNetSync = false;
	}



	private boolean useBatch() {
		return (useBatch.get() != null) && useBatch.get();
	}
	
	private String getTableName(Uri uri) {
		List<String> paths = uri.getPathSegments();
		return paths.get(0);
	}

}