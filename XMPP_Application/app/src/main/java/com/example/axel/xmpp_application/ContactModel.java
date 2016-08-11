package com.example.axel.xmpp_application;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 24/07/2016.
 */
public class ContactModel {
    private static ContactModel sContactModel;
    private List<Contact> mContacts;

    public static ContactModel get(Context context){
        if(sContactModel == null){
            sContactModel = new ContactModel(context);
        }
        return sContactModel;
    }

    public ContactModel(Context context) {
        mContacts = new ArrayList<>();
        populateWithInitialContacts(context);
    }

    private void populateWithInitialContacts(Context context) {
        Contact contact1 = new Contact("dudu@menzamad");
        mContacts.add(contact1);
    }



    public List<Contact> getContacts(){
        return mContacts;
    }
}
