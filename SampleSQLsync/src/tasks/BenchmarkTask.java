package tasks;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.example.samplesqlsync.SampleActivity;
import com.example.samplesqlsync.provider.SampleDatabaseHelper;
import com.nexusray.sqlsync.settings.SyncSettings;
import com.nexusray.sqlsync.sync.SyncExecutor;

/**
 * Worker thread for benchmarking, it runs one phase only each run
 *
 */
public class BenchmarkTask extends AsyncTask<Void, Void, Void> {

	private static final String logTag = "BenchmarkTask";
	private String displayText = "";
	private SampleActivity callerActivity;
	private ContentResolver cr;
	
	private final int nValues = 50;
	private final Object textLock = new Object();
	
	private void addText(String text) {
		synchronized (textLock) {
			displayText += text + "\n";
		}
	}
	
	public BenchmarkTask(SampleActivity callerActivity, ContentResolver cr) {
		this.callerActivity = callerActivity;
		this.cr = cr;
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
		
		synchronized (textLock) {
			callerActivity.displayText(displayText);	
			displayText = "";
			
			super.onProgressUpdate(values);
		}
	}
	
	@Override
	protected void onPostExecute(Void result) {
		synchronized (textLock) {
			callerActivity.displayText(displayText);	
			displayText = "";
			
			super.onPostExecute(result);
		}
	}
	
    @Override
    protected Void doInBackground(Void... url) {
    	
    	try {
    		
    		addText("Inserting 50 rows for each database...");
        	insert();

        	awaitNextPhase();

    		addText("Updating 50 rows (0% collision) for each database...");        		
    		updateRows(0);

    		awaitNextPhase();
    		
    		addText("Updating 50 rows (50% collision) for each database...");
    		updateRows(25);

    		awaitNextPhase();
		
    		addText("Updating 50 rows (100% collision) for each database...");
    		updateRows(50);

    		awaitNextPhase();

    		addText("Deleting all rows in mobile database...");
        	cr.delete(SampleDatabaseHelper.tableProducts, null, null);
        	
        	awaitNextPhase();
    		 
    		addText("Zero changes in any database");
    		
    		awaitNextPhase(); 		
    		
    		addText("Benchmark DONE");
    	
    	}
    	catch (IndexOutOfBoundsException e) {
    		final String error = "No connection to remote comparing service, check if URL" +
    				" both in SampleActivity and SyncSettings are correct and service is running!";
    		addText(error);        		
    		Log.e(logTag, error);
    		
    	} catch (InterruptedException e) {
			Log.e(logTag, "Task is interrupted");
			e.printStackTrace();
		}
    	
    	callerActivity.resetOperationInProgress();
    	
        return null;
    }

    
    
    
    
	private void awaitNextPhase() throws InterruptedException {
		addText("Waiting for synchronization...");            	
		SyncExecutor.requestSync();	
		publishProgress();
		
		//Wait for next phase
		SampleActivity.benchPhase.acquire();
		
		addText("Synchronization time: " + SyncExecutor.elapsed() + " ms\n");
		
		
	}

	
    /**
     * Executes HTTP request, must be done in worker thread
     * @param url
     * @return
     */
    private String httpExe(String url) {
    	HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, SyncSettings.HTTP_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SyncSettings.HTTP_TIMEOUT);
		ConnManagerParams.setTimeout(params, SyncSettings.HTTP_TIMEOUT);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpClient httpclient = new DefaultHttpClient(params);
        HttpResponse response;
        
        try {
            response = httpclient.execute(new HttpGet(url));
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            	return EntityUtils.toString(response.getEntity());
            }
            else {
            	Log.e(logTag, "Http request has FAILED!");
            }
        } 
        catch (Exception e) {
        	Log.e(logTag, "Http request has FAILED!");
        	e.printStackTrace();
        } 
        
        addText("HTTP request failed, URL must be incorrect");
        return null;
    }
    
    
	
	private void insert() {
		if (httpExe(SampleActivity.SCRIPT_BENCH_URL + "?a=insert") == null) { 
			return;
		}
		
    	//Removes previously stored deletes just for purpose of benchmarking,
    	//it is discouraged to interact with this table
    	cr.delete(SampleDatabaseHelper.tableDeletedRows, null, null); 
    	
    	//Deletes all previous data
    	cr.delete(SampleDatabaseHelper.tableProducts, null, null);
    	
    	//Generates inserts 
    	
		ContentValues[] values = new ContentValues[nValues];
		
		for (int i = 1; i <= values.length; i++) {
			values[i-1] = new ContentValues();
		    values[i-1].put("name", "lamp " + i);
		    values[i-1].put("count", i*10);
		    values[i-1].put("price", i*100);            			
		}
		cr.bulkInsert(SampleDatabaseHelper.tableProducts, values);
	}

    
    private void updateRows(int collision) {
    	
		if (httpExe(SampleActivity.SCRIPT_BENCH_URL + "?a=update") == null) { 
			return;
		}
		
		Cursor c = cr.query(SampleDatabaseHelper.tableProducts, new String[] {"_syncID","count","price"},  null, null, "_syncID"); 
		c.move(50-collision);
        for (int i = 1; i <= nValues; i++) {
        	c.moveToNext();
        	int id = c.getInt(0);   	
        	ContentValues values = new ContentValues();
        	values.put("count", c.getInt(1)-1);
        	values.put("price", c.getInt(2)-2);
        	cr.update(SampleDatabaseHelper.tableProducts, values, "_syncID="+id, null);
        }
		c.close();
    }
}
