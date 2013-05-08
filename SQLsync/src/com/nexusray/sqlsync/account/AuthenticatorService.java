package com.nexusray.sqlsync.account;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticatorService extends Service {
    private Authenticator mAuthenticator;

    public AuthenticatorService() {
		super();
	}
    
    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
        System.out.println("AuthenticatorService/onCreate");
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
