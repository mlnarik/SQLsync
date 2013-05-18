package com.example.samplesqlsync.provider;

import com.nexusray.sqlsync.provider.SyncDatabaseHelper;
import com.nexusray.sqlsync.settings.SyncSettings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class SampleDatabaseHelper extends SyncDatabaseHelper {
	
	private static final String logTag = "SampleSQLsync";
	public final static Uri tableProducts = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "products");
	public final static Uri tableCustomers = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "customers");
	public final static Uri tableDeletedRows = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "__SQLsyncDeletedRows");
	
	public SampleDatabaseHelper(Context context) {		
		
		super(context, "db_name", null, 37);
		
		Log.i(logTag, "Database");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		super.onCreate(db);
		
		Log.i(logTag, "Table created database");
		
		String createSQL = "CREATE TABLE products ( " +
				"_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name VARCHAR(100)," +
				"count INTEGER," +
				"price INTEGER," +
				"alt_name VARCHAR(100) DEFAULT NULL" +
				")";
		createSyncTable(db, createSQL);
		
		createSQL = "CREATE TABLE customers ( " +
				"_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name VARCHAR(100)," +
				"address VARCHAR(100)" +				
				")";
		createSyncTable(db, createSQL);
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		Log.i(logTag, "Upgrading database");
		
		for (String tableName: SyncSettings.tablesToSync) {
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
		}		
		
		dropSyncManagementTables(db);
		
		onCreate(db);
		
	}
}
