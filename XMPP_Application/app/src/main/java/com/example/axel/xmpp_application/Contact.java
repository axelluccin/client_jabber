package com.example.axel.xmpp_application;

/**
 * Created by axel on 24/07/2016.
 */
public class Contact {
    private String pseudo;

    public Contact(String contactPseudo){
        pseudo = contactPseudo;
    }

    public String getPseudo(){
        return pseudo;
    }

    public void setPseudo(String pseudo){
        this.pseudo = pseudo;
    }
}
