package com.nexusray.sqlsync.provider;

import java.util.Random;

import com.nexusray.sqlsync.settings.SyncSettings;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

public abstract class SyncDatabaseHelper extends SQLiteOpenHelper
{	
	Context context = null;
	public static final String syncID = "_syncID";
	
	public SyncDatabaseHelper(Context context, String name, CursorFactory cf, int version) {
		super(context, name, cf, version);
		this.context = context;
	}
	
	/*
	 * Provides basic settings for synchronization.
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	public void onCreate(SQLiteDatabase db)
	{	
		addSyncManagementTables(db);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		int deviceID = preferences.getInt("_sync_deviceid", 0);
		
		
		// Setting default values for synchronization
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("_sync_lastid", 0);
		editor.putString("_sync_lastsync", "1960-01-01 00:00:00+01");
		if (deviceID == 0) {
			Random r = new Random();
			int newid = Integer.valueOf(Integer.toOctalString(r.nextInt(1000000)+1000000).replace('0', '8'));
			editor.putInt("_sync_deviceid", newid);
			editor.putInt("_sync_deviceid_status", 0);
			editor.putString("_sync_androidid", Secure.getString(context.getContentResolver(), Secure.ANDROID_ID) );
		}
		editor.commit();
		
	}
	
	/*
	 * Modifies CREATE TABLE sql query for synchronization and creates table
	 */
	protected void createSyncTable(SQLiteDatabase db, String createSQL) {
		
		int posA = createSQL.lastIndexOf(")");
		int posB = createSQL.indexOf("CREATE TABLE ") + 13;
		int posC = createSQL.indexOf(" (");
		
		String tableName = createSQL.substring(posB, posC);
		
		// if _syncID is NULL, mean its not yet in global database - global DB generates this id	
		// if _lastChange is NULL, means no change since last synchronization
		String modCreateSQL = createSQL.substring(0, posA) + 
				"," + SyncDatabaseHelper.syncID + " INTEGER DEFAULT NULL," +
				"_lastChange TIMESTAMP DEFAULT (datetime('now'))," +
				"_syncStatus INTEGER NOT NULL DEFAULT 0," +
				"UNIQUE(" + SyncDatabaseHelper.syncID + ")" +	
				")";
		
		
		
		db.execSQL(modCreateSQL);
		
		db.execSQL("CREATE TRIGGER _sync_update_" + tableName + " " +
			     "BEFORE UPDATE ON " + tableName + " " +
			     "FOR EACH ROW BEGIN " +
			     "UPDATE " + tableName + " SET _lastChange=datetime('now') " +
			     "WHERE _ID = NEW._ID; " +
			     "END;");
		
		db.execSQL("CREATE TRIGGER _sync_delete_" + tableName + " " +
			     "AFTER DELETE ON " + tableName + " " +
			     "FOR EACH ROW WHEN OLD."+ SyncDatabaseHelper.syncID + " IS NOT NULL BEGIN " +
			     "INSERT INTO __SQLsyncDeletedRows(tableName," + SyncDatabaseHelper.syncID + ") " +
			     "VALUES('" + tableName + "',OLD." + SyncDatabaseHelper.syncID + "); " +
			     "END;");		
		
	}
	
	/*
	 * Adds synchronization tables
	 */
	protected void addSyncManagementTables(SQLiteDatabase db) {
		
		db.execSQL("CREATE TABLE __SQLsyncDeletedRows( " +
				"tableName VARCHAR(100) NOT NULL," +
				SyncDatabaseHelper.syncID + " INTEGER NOT NULL," +
				"_timeDeleted TIMESTAMP NOT NULL DEFAULT (datetime('now'))," +
				"_syncStatus INTEGER NOT NULL DEFAULT 0" +				
				")");
		
			
	}	
	
	/*
	 * Drops all tables and triggers for synchronization
	 */
	protected void dropSyncManagementTables(SQLiteDatabase db) {
		
		for (String tableName: SyncSettings.tablesToSync) {
			db.execSQL("DROP TRIGGER IF EXISTS _sync_update_" + tableName);
			db.execSQL("DROP TRIGGER IF EXISTS _sync_delete_" + tableName);
		}		
		

		db.execSQL("DROP TABLE IF EXISTS __SQLsyncTime");
		db.execSQL("DROP TABLE IF EXISTS __SQLsyncDeletedRows");
			
	}	
	

}