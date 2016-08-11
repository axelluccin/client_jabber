package com.example.axel.xmpp_application;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

/**
 * Created by axel on 27/07/2016.
 */
public class XMPPConnectionService extends Service {
    private static final String TAG = "XMPPService";
    public static final String UI_AUTHENTICATED = "com.example.axel.uiauthenticated";
    public static final String SEND_MESSAGE = "com.example.axel.sendmessage";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";
    public static final String NEW_MESSAGE  = "com.example.axel.newmessage";
    public static final String BUNDLE_FROM_PSEUDO = "b_from";

    public static XMPP_Connection.ConnectionState sConnectionState;
    public static XMPP_Connection.LoggedInState sLoggedInState;
    private boolean mActive;
    private Thread mThread;
    private Handler mTHandler;

    private XMPP_Connection mConnection;


    public XMPPConnectionService(){

    }

    public static XMPP_Connection.ConnectionState getState(){
        if (sConnectionState == null)
            return XMPP_Connection.ConnectionState.DISCONNECTED;
        return sConnectionState;
    }

    public static XMPP_Connection.LoggedInState getsLoggedInState(){
        if (sLoggedInState == null)
            return XMPP_Connection.LoggedInState.LOGGED_OUT;
        return sLoggedInState;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "OnCreate()");
    }

    public void start(){
        Log.d(TAG, "Service Start() function called.");
        if(!mActive){
            mActive = true;
            if(mThread == null || !mThread.isAlive()){
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        //the code here runs in a background thread
                        Looper.loop();
                    }
                });
                mThread.start();
            }
        }
    }

    public void stop(){
        Log.d(TAG, "stop");
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mConnection != null)
                    mConnection.disconnect();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand");
        start();
        return Service.START_STICKY;

    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stop();
    }

    private void initConnection()
    {
        Log.d(TAG, "initConnection !");
        if (mConnection == null)
            mConnection = new XMPP_Connection(this);
        try {
            mConnection.connect();
        }catch (IOException | SmackException | XMPPException e){
            Log.d(TAG, "Something went wrong while connecting, make sure the credentials are right and try again");
            e.printStackTrace();
            stopSelf();
        }
    }




}
