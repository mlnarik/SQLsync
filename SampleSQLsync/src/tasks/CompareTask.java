package tasks;

import com.nexusray.sqlsync.sync.SyncExecutor;

import android.os.AsyncTask;

public abstract class CompareTask extends AsyncTask<Void, Void, Void> {

	public void CompareTask() {
		
	}
	
	@Override
    protected void onPostExecute(Void result) {
    	
		// Displays buffered text
		displayText(displayText);	
		displayText = "";
   	
    	if (connectionError == true) {
    		
    		resetBenchmark();
    		
        	operationInProgress = false;      
        	displayText("CONNECTION ERROR");
    	}
    	else if (operationInProgress) {
    		// Enables next benchmark phase after synchronization is done
    		benchPhase++;
        	benchContinue = true;
        	
        	
        	SyncExecutor.requestSync();
        
    	}
    	super.onPostExecute(result);
    }	
}
