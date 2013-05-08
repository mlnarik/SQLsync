package com.nexusray.sqlsync.provider;

import com.nexusray.sqlsync.sync.SyncExecutor;

import android.database.ContentObserver;
import android.os.Handler;

public abstract class SyncObserver extends ContentObserver {

	private boolean inSync = false;
	//private final String logTag = "SyncObs";
	
	public SyncObserver(Handler handler) {
		super(handler);
	}
	/**
	 * Activates when synchronization completes
	 */
	abstract public void onCompletedSync();
	
	/**
	 * Activates when synchronization is active
	 */
	abstract public void onSyncActive();
	
	@Override
	public void onChange(boolean selfChange)
	{
		super.onChange(selfChange);
		
		if (SyncExecutor.isSyncActive()) {
			if (inSync == false) {
				inSync = true;
				onSyncActive();				
			}
		}
		else {
			if (inSync == true) {				
				inSync = false;
				onCompletedSync();
			}
		}
	}

}
