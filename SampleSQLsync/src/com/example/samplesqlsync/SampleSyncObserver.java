package com.example.samplesqlsync;

import com.nexusray.sqlsync.provider.SyncObserver;

import android.os.Handler;
import android.util.Log;

public class SampleSyncObserver extends SyncObserver {

	private SampleActivity activity;
	
	public SampleSyncObserver(Handler handler, SampleActivity activity) {
		super(handler);

		this.activity = activity;
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
		if (activity.benchContinue) {
			activity.benchContinue = false;
			activity.runNextBenchmarkPhase(); 
		}
		else if (activity.testContinue) {
			activity.testContinue = false;
			activity.runNextTestPhase();			
		}
		
			
	}

}
