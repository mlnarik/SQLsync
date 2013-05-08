package com.example.samplesqlsync.provider;

import java.util.List;

import com.nexusray.sqlsync.provider.SyncProvider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class SampleProvider extends SyncProvider {
	
	 protected SQLiteOpenHelper getDatabaseHelper(Context context) {
		 
		 return new SampleDatabaseHelper(context);
	 }
	 
	
	protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		List<String> paths = uri.getPathSegments();
		if (paths.get(0) == null)
			return 0;
		int affectedRows = db.update(paths.get(0), values, selection,
				selectionArgs);

		return affectedRows;
	}

	protected Uri insertInTransaction(Uri uri, ContentValues values) {

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		String tableName = getTableName(uri);
		if (tableName == null) 
			return null;
		long id = db.insert(tableName, null, values);

		if (id > 0) {
			Uri resultUri = ContentUris.withAppendedId(uri, id);
			return resultUri;
		}

		return null;
	}

	protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) {
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		String tableName = getTableName(uri);
		if (tableName == null) 
			return 0;
		int affectedRows = db.delete(tableName, selection, selectionArgs);

		return affectedRows;
	}
	
	@Override
	protected Cursor customQuery(Uri uri, String[] projection,
		String selection, String[] selectionArgs, String sortOrder) {
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String tableName = getTableName(uri);
		if (tableName == null) 
			return null;
		Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);

		return c;		
	}	
	
	private String getTableName(Uri uri) {
		List<String> paths = uri.getPathSegments();
		return paths.get(0);
	}

	public void onBegin() {
		
	}

	public void onCommit() {
		
	}

	public void onRollback() {
		
	}




}
