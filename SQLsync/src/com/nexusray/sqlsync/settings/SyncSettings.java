package com.nexusray.sqlsync.settings;

import android.net.Uri;

public final class SyncSettings {
	
	// ContentProvider settings
	public final static String[] tablesToSync = new String[] {"products", "customers"};	
	public final static int SLEEP_AFTER_YIELD_DELAY = 4000;
	public final static String AUTHORITY = "com.example.samplesqlsync";	
	public final static Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	
	// SERVICE settings
	// To use SSL use 'https' in the URL and use proper port if needed
	public final static String URL_WEBSERVICE = "http://10.0.0.32:9090/SyncService.php";
	//public final static String URL_WEBSERVICE = "http://nexusray.com/sqlsync/webservice/SyncService.php";
	public final static int HTTP_TIMEOUT = 30000;
		
}
