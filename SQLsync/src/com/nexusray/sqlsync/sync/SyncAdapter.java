package com.nexusray.sqlsync.sync;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	public SyncAdapter(Context context, boolean autoinitialize) {
		super(context, autoinitialize);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		
		Context context = getContext();
		AccountManager accountManager = AccountManager.get(context);
		SyncExecutor.preformSync(account.name, accountManager.getPassword(account), context, syncResult);
		
	
	}

}
