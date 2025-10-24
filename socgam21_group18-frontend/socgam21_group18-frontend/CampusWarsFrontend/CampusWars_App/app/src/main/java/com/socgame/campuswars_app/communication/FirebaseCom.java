package com.socgame.campuswars_app.communication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseCom{

    /**
     * from https://firebase.google.com/docs/auth/android/start but modified
     *
     * follows Singleton Pattern like all of our Main Communication Classes
     *
     * Communicates with our Firebase Console for User Creation, Email Login, and User Id Generation
     *
     * written by Daniel
     */


    private static FirebaseCom instance;

    private static Context ctx;

    private static FirebaseAuth mAuth;

    private static String name;
    private static String email;
    private static String UID;

    private FirebaseCom(Context ctx){
        this.ctx = ctx;
        this.mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        //Getting User Data
        this.getUserProfile();
    }

    public static synchronized FirebaseCom getInstance(Context context){
        if (instance == null) {
            instance = new FirebaseCom(context);
        }
        return instance;
    }

    public FirebaseAuth getMAuth(){
        return this.mAuth;
    }

    public void setUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            //writing User Data into fixed Storage
            SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("email", user.getEmail());
            String name = user.getEmail().split("@")[0];
            editor.putString("name", name);
            editor.putString("UID", user.getUid());
            editor.apply();

            //Getting Id Token For User and writing it into fixed Storage
            //Had to remove this because it could change if the user switches devices and i cant stop that
            //We need a fixed firebaseId for our database
            /*
            user.getIdToken(false).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                editor.putString("UID", idToken);
                editor.apply();
                this.UID = settings.getString("UID", "empty");
            });
            */

            //Collecting User Data from Fixed Storage and putting it into our live Singleton
            this.email = settings.getString("email", "empty");
            this.name = settings.getString("name", "empty");

            // we dont currently confirm users email
            // Check if user's email is verified
            // boolean emailVerified = user.isEmailVerified();
        }
    }

    public void getUserProfile() {
        //Gets Current User Profile Data, ie.: when this Singleton is created
        SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
        this.email = settings.getString("email", "empty");
        this.UID = settings.getString("UID", "empty");
    }

    public String getUID(){
        return this.UID;
    }

    public void createAccount(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    private void sendEmailVerification() {
        // Send verification email, currently unused
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Email sent
                    }
                });
    }
}
