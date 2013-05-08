/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nexusray.sqlsync.account;

import com.nexusray.sqlsync.settings.SyncSettings;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;


/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain.
 */
public class Authenticator extends AbstractAccountAuthenticator {
    // Authentication Service context
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) {
    	
    	String name = options.getString(AccountManager.KEY_ACCOUNT_NAME);
    	String password = options.getString(AccountManager.KEY_PASSWORD);
    	
    	
    	
    	if (name != null && password != null) {
    		
    		Bundle result = null;
	    	Account account = new Account(name, SyncSettings.AUTHORITY);
			AccountManager am = AccountManager.get(mContext);
			if (am.addAccountExplicitly(account, password, null)) {
				result = new Bundle();
				
				result.putString(AccountManager.KEY_ACCOUNT_NAME, name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, SyncSettings.AUTHORITY);
			
				ContentResolver.setIsSyncable(account, SyncSettings.AUTHORITY, 1);
				//ContentResolver.setSyncAutomatically(account, SyncSettings.AUTHORITY, true);
				
				return result;
			}
    	}
    	
        return new Bundle();
    }
   

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) throws NetworkErrorException {

		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,
			String accountType) {

		return null;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {

		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,
			Account account, String[] features) throws NetworkErrorException {
		
		final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {

		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {

		return null;
	}


}
