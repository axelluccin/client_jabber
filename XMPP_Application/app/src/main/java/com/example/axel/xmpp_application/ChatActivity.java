package com.example.axel.xmpp_application;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import co.devcenter.androiduilibrary.ChatView;
import co.devcenter.androiduilibrary.ChatViewEventListener;
import co.devcenter.androiduilibrary.SendButton;

public class ChatActivity extends AppCompatActivity {
    private ChatView mChatView;
    private String contactpseudo;
    private SendButton mSendButton;
    private BroadcastReceiver mBroadcastReceiver;
    private MyMessageListener messageListener;

    private static final String TAG = "ChatActivity ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageListener = new MyMessageListener();
        mChatView = (ChatView) findViewById(R.id.xmpp_chat_view);

        mChatView.setEventListener(new ChatViewEventListener() {
            @Override
            public void userIsTyping() {
            }

            @Override
            public void userHasStoppedTyping() {

            }
        });


        mSendButton = mChatView.getSendButton();
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (XMPPConnectionService.getState().equals(XMPP_Connection.ConnectionState.CONNECTED)){
                    Log.d(TAG, "The client is connected to the server, Sendint Message");

                    Intent intent = new Intent(XMPPConnectionService.SEND_MESSAGE);
                    intent.putExtra(XMPPConnectionService.BUNDLE_MESSAGE_BODY, mChatView.getTypedString());
                    intent.putExtra(XMPPConnectionService.BUNDLE_TO, contactpseudo);

                    sendBroadcast(intent);
                    mChatView.sendMessage();
                }else{
                    Toast.makeText(getApplicationContext(), "Client not connected to server, Message not sent", Toast.LENGTH_LONG).show();
                }
            }
        });
        Intent intent = getIntent();
        contactpseudo = intent.getStringExtra("EXTRA_CONTACT_PSEUDO");
        setTitle(contactpseudo);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action)
                {
                    case XMPPConnectionService.NEW_MESSAGE:
                        String from = intent.getStringExtra(XMPPConnectionService.BUNDLE_FROM_PSEUDO);
                        String body = intent.getStringExtra(XMPPConnectionService.BUNDLE_MESSAGE_BODY);

                        if(from.equals(contactpseudo)){
                            mChatView.receiveMessage(body);
                        }else{
                            Log.d(TAG, "Got a message from pseudo "+from);
                        }
                        return;
                }
            }
        };
        IntentFilter filter = new IntentFilter(XMPPConnectionService.NEW_MESSAGE);
        registerReceiver(mBroadcastReceiver, filter);
    }


    class MyMessageListener implements MessageListener {

        @Override
        public void processMessage(Message message) {
            String from = message.getFrom();
            String body = message.getBody();
            System.out.println(String.format("Received message '%1$s' from %2$s", body, from));
        }
    }
}
