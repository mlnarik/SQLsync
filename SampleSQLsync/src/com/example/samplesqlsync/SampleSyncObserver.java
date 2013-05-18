package com.example.samplesqlsync;

import com.nexusray.sqlsync.provider.SyncObserver;

import android.os.Handler;

public class SampleSyncObserver extends SyncObserver {

	
	public SampleSyncObserver(Handler handler) {
		super(handler);
	}

	/**
	 * Not used in this example
	 */
	@Override
	public void onSyncActive() { }

	/**
	 * Executes action after completed synchronization.
	 * In this case, benchmark or test will continue.
	 */
	@Override
	public synchronized void onCompletedSync() {
		SampleActivity.benchPhase.release();
		SampleActivity.testPhase.release();
		
			
	}

}
