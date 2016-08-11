package com.example.axel.xmpp_application;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatMessageListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import javax.net.ssl.*;
import javax.net.SocketFactory;

/**
 * Created by axel on 29/07/2016.
 */
public class XMPP_Connection implements ConnectionListener, MessageListener, ChatMessageListener {
    private static final String TAG = "XMPP_Connection";
    private final Context mApplicationContext;
    private final String mUsername;
    private final String mPassword;
    private final String mServiceName;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;

    @Override
    public void processMessage(Chat chat, Message message) {
        Log.d(TAG, "message get body : "+message.getBody());
        Log.d(TAG, "message get form : "+message.getFrom());


        String from = message.getFrom();
        String contactPseudo="";
        if(from.contains("/")){
            contactPseudo = from.split("/")[0];
            Log.d(TAG, "Son Pseudo est "+contactPseudo);
        }
        else {
            contactPseudo = from;
            Log.d(TAG, "Recu message");
        }
        Intent intent = new Intent(XMPPConnectionService.NEW_MESSAGE);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(XMPPConnectionService.BUNDLE_FROM_PSEUDO, contactPseudo);
        intent.putExtra(XMPPConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());
        mApplicationContext.sendBroadcast(intent);
        Log.d(TAG, "Received message from : "+contactPseudo+" broadcast sent.");

    }

    @Override
    public void processMessage(Message message) {
        Log.d(TAG, "message get body : "+message.getBody());
        Log.d(TAG, "message get form : "+message.getFrom());

        String from = message.getFrom();
        String contactPseudo="";
        if(from.contains("/")){
            contactPseudo = from.split("/")[0];
            Log.d(TAG, "Son Pseudo est "+contactPseudo);
        }
        else
            contactPseudo=from;
        Intent intent = new Intent(XMPPConnectionService.NEW_MESSAGE);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(XMPPConnectionService.BUNDLE_FROM_PSEUDO, contactPseudo);
        intent.putExtra(XMPPConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());
        mApplicationContext.sendBroadcast(intent);
        Log.d(TAG, "Received message from : "+contactPseudo+" broadcast sent.");
    }


    public static enum ConnectionState{
        CONNECTED, AUTHENTICATED, CONNECTING, DISCONNECTING, DISCONNECTED;
    }

    public static enum LoggedInState{
        LOGGED_IN, LOGGED_OUT;
    }

    public XMPP_Connection(Context context){
        Log.d(TAG, "XMPP_Connection Constructor called!");
        mApplicationContext = context.getApplicationContext();
        String pseudo = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_pseudo", null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_password", null);

        if (pseudo != null){
            mUsername = pseudo.split("@")[0];
            mServiceName = pseudo.split("@")[1];
        }else{
            mUsername = "";
            mServiceName = "menzamad";
        }

    }

    public void connect() throws IOException, XMPPException, SmackException{
        Log.d(TAG, "Connecting to server " + mServiceName);
        XMPPTCPConnectionConfiguration.XMPPTCPConnectionConfigurationBuilder builder = XMPPTCPConnectionConfiguration.builder();
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        //builder.setSocketFactory(new DummySSLSocketFactory());
        builder.setServiceName(mServiceName);
        builder.setUsernameAndPassword(mUsername, mPassword);
        //builder.setHost("192.168.1.14");
        //builder.setPort(5222);
        builder.setRosterLoadedAtLogin(true);
        builder.setResource("XMPP_App");

        setupUiThreadBroadCastMessageReceiver();
        mConnection = new XMPPTCPConnection(builder.build());
        mConnection.addConnectionListener(this);
        mConnection.connect();
        mConnection.login();

        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();
    }

    public void disconnect()
    {
        Log.d(TAG, "Disconnecting from server " + mServiceName);
        try {
            if (mConnection != null)
                mConnection.disconnect();
        }catch (SmackException.NotConnectedException e){
            XMPPConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
            e.printStackTrace();
        }
        mConnection = null;
    }
    @Override
    public void connected(XMPPConnection connection) {
        XMPPConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG, "Connected Successfully");
    }

    @Override
    public void authenticated(XMPPConnection connection) {
        XMPPConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG, "Authenticated Successfully");
        showContactListActivityWhenAuthenticated();
    }

    @Override
    public void connectionClosed() {
        XMPPConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG, "Connection closed !");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        XMPPConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG, "Connection closed on error, error "+ e.toString());
    }

    @Override
    public void reconnectingIn(int seconds) {
        XMPPConnectionService.sConnectionState=ConnectionState.CONNECTING;
        Log.d(TAG, "Reconnecting in ...");
    }

    @Override
    public void reconnectionSuccessful() {
        XMPPConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG, "Reconnection Successful!");
    }

    @Override
    public void reconnectionFailed(Exception e) {
        XMPPConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG, "Reconnect failed !");
    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(XMPPConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG, "Sent the broadcast that we are authenticated");
    }

    private void setupUiThreadBroadCastMessageReceiver()
    {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(XMPPConnectionService.SEND_MESSAGE))
                {
                    sendMessage(intent.getStringExtra(XMPPConnectionService.BUNDLE_MESSAGE_BODY), intent.getStringExtra(XMPPConnectionService.BUNDLE_TO));
                }
            }
        };
        IntentFilter filter =  new IntentFilter();
        filter.addAction(XMPPConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter);
    }

    private void sendMessage(String body, String toPseudo){
        Log.d(TAG, "Sending message to "+ toPseudo);
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(toPseudo, (ChatMessageListener) this);
        try{
            chat.sendMessage(body);
        }catch (SmackException.NotConnectedException | XMPPException e){
            e.printStackTrace();
        }
    }


}
