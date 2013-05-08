package com.nexusray.sqlsync.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {

	private Object adapterLock = new Object();
	private SyncAdapter adapter;

	@Override
	public void onCreate() {
		super.onCreate();
		synchronized (adapterLock) {
			if (adapter == null)
				adapter = new SyncAdapter(getApplicationContext(), true);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return adapter.getSyncAdapterBinder();
	}
}
