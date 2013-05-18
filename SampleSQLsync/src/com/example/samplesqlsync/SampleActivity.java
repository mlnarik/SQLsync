package com.example.samplesqlsync;


import java.util.Random;

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

import com.nexusray.sqlsync.settings.SyncSettings;
import com.nexusray.sqlsync.sync.SyncExecutor;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Test functionality of master-master synchronization library
 * and measures its speed after execution of certain operations
 * on custom database
 *
 */
public class SampleActivity extends Activity {
	
	public SampleSyncObserver viewObs = null;
	private String logTag = "sampleApp"; 
	public long timeStart = 0;
	
	// URL must be same as library uses otherwise it won't work correctly
	private final String WEBSERVICE_URL = "http://10.0.0.32:9090/";
	//private final String WEBSERVICE_URL = "http://nexusray.com/sqlsync/webservice";
	private final String SCRIPT_BENCH_URL = WEBSERVICE_URL + "/sample/SampleBench.php";
	TextView resultField = null; 
	ContentResolver cr = null;
	
	
	Uri table1Uri = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "products");
	Uri table2Uri = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "customers");
	Uri tableDeletedRows = Uri.withAppendedPath(SyncSettings.AUTHORITY_URI, "__SQLsyncDeletedRows");
	
	private final int nValues = 50;
	
	private volatile int benchPhase = 0;
	public volatile boolean benchContinue = false;
	
	private volatile int testPhase = 0;
	public volatile boolean testContinue = false;
	
	private boolean operationInProgress = false;
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(logTag, "Starting activity ..");
        setContentView(R.layout.sample_activity);
        
        // Creates one account, it will be overridden each run
        SyncExecutor.createAccount("samplename", "samplepass", getApplicationContext());
        SyncExecutor.enableSync(getApplicationContext());
        resultField = (TextView) findViewById(R.id.result); 
        cr = getContentResolver();
        
              
        
       
     
        viewObs = new SampleSyncObserver(new Handler(), this);
        
        ContentValues values = new ContentValues();
        values.put("name", "lampa");
        values.put("count", 15);
        values.put("price", 11122);
       
        //cr.delete(Uri.parse("content://com.example.samplesqlsync/products"), "_syncID=310301", null);
        //cr.insert(Uri.parse("content://com.example.samplesqlsync/products"), values);
        //cr.update(Uri.parse("content://com.example.samplesqlsync/products"), values, "_syncID=80800", null);

        
    }
    
    /**
     * Unregisters ContentObserver on pause
     */
    @Override
    public void onPause() {
    	super.onPause();    
    	
    	if(viewObs != null) {
    		cr.unregisterContentObserver(viewObs);
    	}    	
    }
    
    /**
     * Registers ContentObserver on unpause
     */
    @Override
    public void onResume() {
    	super.onResume();    
    	
    	if (viewObs != null) {
    		cr.registerContentObserver(SyncSettings.AUTHORITY_URI, true, viewObs);
    	}    	
    }

    /**
     * Displays another line of text
     */
    private void displayText(String text) {
    	resultField.append(text);
    	ScrollView scroll = (ScrollView) findViewById(R.id.scroll); 
    	scroll.fullScroll(View.FOCUS_DOWN);
    }
    
    /**
     * Clears all text from the screen
     */
    private void clearText() {
    	resultField.setText("");
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
        return null;
    }
    
    /**
     * Method for benchmark button
     * @param view ignored
     */
    public void benchmarkSync(View view) {
    	
    	if (operationInProgress) return;
    	operationInProgress = true;
  	
    	resetBenchmark();
    	
    	clearText();
    	displayText("Starting benchmark...\n");
    	displayText("Please don't interact with your device till it's done, it might affect results\n\n");
    	
    	runNextBenchmarkPhase();

    }    
    
    /**
     * Initiates one of the benchmark phases
     */
    public void runNextBenchmarkPhase() {
    	new BenchmarkTask().execute();
    }
    
    /**
     * Resets benchmark
     */
    public void resetBenchmark() {
    	benchPhase = 0;
    	benchContinue = false;    	
    }
    
    /**
     * Worker thread for benchmarking, it runs one phase only each run
     *
     */
    public class BenchmarkTask extends AsyncTask<Void, Void, Void> {
    
    	private boolean connectionError = false;
    	private String displayText = "";
    	
    	private void addText(String text) {
    		displayText += text + "\n";    		
    	}
  	


    	
        @Override
        protected Void doInBackground(Void... url) {
        	
        	try {
        	
        	if (benchPhase == 0) {
        		
        		connectionError = false;
        		
        		addText("Inserting 50 rows for each database...");
        		
        		if (httpExe(SCRIPT_BENCH_URL + "?a=insert") == null) { 
        			connectionError = true;
        			return null;
        		}
        		
            	//Removes previously stored deletes just for purpose of benchmarking,
            	//it is discouraged to interact with this table
            	cr.delete(tableDeletedRows, null, null); 
            	
            	//Deletes all previous data
            	cr.delete(table1Uri, null, null);
            	
            	//Generates inserts    	
            	ContentValues[] values = new ContentValues[nValues];
            	
            	for (int i = 1; i <= values.length; i++) {
                	values[i-1] = new ContentValues();
                    values[i-1].put("name", "lamp " + i);
                    values[i-1].put("count", i*10);
                    values[i-1].put("price", i*100);            			
        		}
            	cr.bulkInsert(table1Uri, values);
	
            	addText("Waiting for synchronization...");            	
        	} 
        	else if (benchPhase == 1) {    	
        		compareDatabases();

        		addText("Updating 50 rows (0% collision) for each database...");        		
        		updateRows(0);
        	}
        	else if (benchPhase == 2) {    		
        		compareDatabases();
        		
        		addText("Updating 50 rows (50% collision) for each database...");
        		updateRows(25);
        	}
        	else if (benchPhase == 3) {    		
        		compareDatabases();
  		
        		addText("Updating 50 rows (100% collision) for each database...");
        		updateRows(50);
        	}
        	else if (benchPhase == 4) {    		
        		compareDatabases();

        		addText("Deleting all rows in mobile database...");
            	cr.delete(table1Uri, null, null);
            	           	
            	
            	addText("Waiting for synchronization...");
        	}
        	else if (benchPhase == 5) {    		
        		compareDatabases();
        		 
        		addText("Zero changes in any database");
        		addText("Waiting for synchronization...");
        	}
        	else if (benchPhase == 6) {    		
        		compareDatabases();        		
        		
        		operationInProgress = false;
        		addText("Benchmark DONE");
        	}
        	}
        	catch (IndexOutOfBoundsException e) {
        		final String error = "No connection to remote comparing service, check if URL" +
        				" both in SampleActivity and SyncSettings are correct and service is running!";
        		addText(error);        		
        		Log.e(logTag, error);
        		
        		operationInProgress = false;
        	}
        	
            return null;
        }

        /**
         * Compares mobile and global database
         */
        private void compareDatabases() {
        	
	    	// Stores data to string
	    	String mobileEcho = "START\n";    	
	    	Cursor c = cr.query(table1Uri, 
	    			new String[] {"_syncID", "name", "count", "price"},  null, null, "_syncID");
	        while (c.moveToNext())
	        	mobileEcho += c.getString(0) + "," + c.getString(1) +
	        			"," + c.getString(2) + "," + c.getString(3) + "\n";
	        mobileEcho += "DONE\n\n";    		
			c.close();
	        
			String serverEcho = httpExe(SCRIPT_BENCH_URL + "?a=echo");
			if (serverEcho == null) {
				connectionError = true;
				return;
			}
    			
    	    boolean compareResult = mobileEcho.equals(serverEcho); 

    		
    		if (compareResult) addText("Databases match");
        	else {
        		addText("Databases DON'T match!");
        		Log.w(logTag, "DB MISMATCH\nCompare result M/S:\n" +
        				mobileEcho + serverEcho);
        	}
    		
    		long time = SyncExecutor.elapsed();
    		addText("Synchronization time: " + time + "ms\n");
    		
        }
        private void updateRows(int collision) {
        	
    		if (httpExe(SCRIPT_BENCH_URL + "?a=update") == null) { 
    			connectionError = true;
    			return;
    		}
    		
    		Cursor c = cr.query(table1Uri, new String[] {"_syncID","count","price"},  null, null, "_syncID"); 
    		c.move(50-collision);
            for (int i = 1; i <= nValues; i++) {
            	c.moveToNext();
            	int id = c.getInt(0);   	
            	ContentValues values = new ContentValues();
            	values.put("count", c.getInt(1)-1);
            	values.put("price", c.getInt(2)-2);
            	cr.update(table1Uri, values, "_syncID="+id, null);
            }
    		c.close();
    		         	
    		addText("Waiting for synchronization...");
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Method for test button
     * @param view ignored
     */
    public void testConflicts(View view) {
    	if (operationInProgress) return;
    	operationInProgress = true;
    	
    	resetTest();
    	runNextTestPhase();
    	
    	clearText();
    	displayText("Testing synchronization\nThis operation might take a minute...\n");
    }
    
    /**
     * Creates TestTask thread
     * Tests synchronization functionality
     */
    public void runNextTestPhase() {
    	new TestTask().execute();
    }
    
    /**
     * Resets tests of functionality
     */
    public void resetTest() {
    	testPhase = 0;
    	testContinue = false;
    }
    
    /**
     * Tests synchronization functionality
     *
     */
    public class TestTask extends AsyncTask<Void, Void, Void> {
    	
    	private boolean connectionError = false;
    	private String displayText = "";
    	
    	private void addText(String text) {
    		displayText += text + "\n";
    	}
    	
		@Override
		protected void onPostExecute(Void result) {
			
			
			displayText(displayText);
			displayText = "";
			
			if (connectionError) {
				resetTest();				
		    	operationInProgress = false;
				displayText("CONNECTION ERROR");				
			}
			
			super.onPostExecute(result);
		}    	
    	
		@Override
		protected Void doInBackground(Void... params) {
			
			if (testPhase == 0) {		
				connectionError = false; 
				
				if (httpExe(SCRIPT_BENCH_URL + "?a=generate_init") == null) {
					connectionError = true;
					return null;
				}
		    	
		    	Random rnd = new Random();
		    	ContentValues values;
		    	
		    	for (int i = 0; i < 10; i++) {					
				
			    	values = new ContentValues();
			        values.put("name", "lampa");
			        values.put("count", rnd.nextInt(99)+1);
			        values.put("price", rnd.nextInt(999)+1);
			        
			        cr.insert(table1Uri, values);
		    	}
		        
		    	for (int no = 0; no < 10; no++) {	
		    		
		    		SyncExecutor.requestSync();
		    		
			    	for (int i = 0; i < 10; i++) {	
				        Cursor c = cr.query(table1Uri, new String[] {"_syncID"}, null, null, "random()");
				        if (c.moveToFirst()) {
				    	
				        	// Update random row
				        	values = new ContentValues();
				            values.put("count", rnd.nextInt(99)+1);
				            values.put("price", rnd.nextInt(999)+1);
					        cr.update(table1Uri, values, "_ID=?", new String[] {c.getString(0)});
					        c.close();
					        
					        if (httpExe(SCRIPT_BENCH_URL + "?a=generate") == null) {
								connectionError = true;
								return null;
							}
					        
					        // Delete random row
					        c = cr.query(table1Uri, new String[] {"_syncID"}, null, null, "random()");
					        c.moveToFirst();
					        values = new ContentValues();
					    	cr.delete(table1Uri, "_syncID=?", new String[] {c.getString(0)});
					    	
					    	// Insert a row
					    	values = new ContentValues();
					        values.put("name", "lampa");
					        values.put("count", rnd.nextInt(99)+1);
					        values.put("price", rnd.nextInt(999)+1);
					        
					        cr.insert(table1Uri, values);
				        }
				    	c.close();	
			    	}
		    	}
		    	
		    	try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    	
		    	testPhase++;
		    	testContinue = true;
				SyncExecutor.requestSync();
				
			}
			else if (testPhase == 1) {
				
		    	// Compares databases
		    	String mobileEcho = "START\n";    	
		    	Cursor c = cr.query(table1Uri, 
		    			new String[] {"_syncID", "name", "count", "price"},  null, null, "_syncID");
		        while (c.moveToNext())
		        	mobileEcho += c.getString(0) + "," + c.getString(1) +
		        			"," + c.getString(2) + "," + c.getString(3) + "\n";
		        mobileEcho += "DONE\n\n";    		
				c.close();
		        
				String serverEcho = httpExe(SCRIPT_BENCH_URL + "?a=echo");
				if (serverEcho == null) {
					connectionError = true;
					return null;
				}
				
		        boolean compareResult = mobileEcho.equals(serverEcho); 		
			    
			    if (compareResult) addText("Test SUCCEEDED");
			    else {
			    	addText("Test FAILED");
			    	Log.w(logTag, "Compare result M/S:\n"+ mobileEcho + serverEcho);
			    }
			    
			    operationInProgress = false;
			
			}
			
			return null;
		}
    }
}

