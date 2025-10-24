package com.socgame.campuswars_app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseError;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.FirebaseCom;
import com.socgame.campuswars_app.communication.CampusCom;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpSingleton;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

import static com.socgame.campuswars_app.R.layout.activity_register;

public class RegisterActivity extends AppCompatActivity {

    /**
     * In this class we Register the User
     * First we get the Password and check if it matches confirm then we create a Firebase Account
     * We save all the data of our User we need in setUserProfile()
     *
     * Paralell to that we generate a Token in TumOnline for the User:
     * (we do these actions seperatly because otherwise a previously succesfull Firebase Create User could block out our generate Token if it failed on First Try)
     * We get the TumId and send a HTTP Request to generate a Token
     * if successful we generate a secret for user sensitive data and upload it -> we dont save or know this secret, because we dont need the user-sensitive data
     *
     * If everything was successful so far we switch to the TokenActivationActivity
     *
     * In future classes we seperate Methods for the HttpOnResponse and Error but here it would have made it even more complicated because we call http calls within http calls
     *
     * written by Daniel
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_register);

        Context ctx = this.getApplicationContext();
        FirebaseCom fCom = FirebaseCom.getInstance(ctx);
        CampusCom cCom = CampusCom.getInstance(ctx);
        BackendCom bCom = BackendCom.getInstance(ctx);
        HttpSingleton http = HttpSingleton.getInstance(ctx);

        EditText tumId = (EditText) findViewById(R.id.editTextTumId);
        EditText email = (EditText) findViewById(R.id.editTextEmailAddress);
        EditText password = (EditText) findViewById(R.id.editTextPassword);
        EditText confirmPassword = (EditText) findViewById(R.id.editTextConfirmPassword);

        Button next = (Button) findViewById(R.id.registerButton);

        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(email.getText().toString().length() == 0 || password.getText().toString().length() == 0 || confirmPassword.getText().toString().length() == 0 || tumId.getText().toString().length() == 0){
                    Toast.makeText(ctx, "Please enter values for all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(password.getText().toString().equals(confirmPassword.getText().toString())){
                    //DONE: check info here
                    fCom.createAccount(email.getText().toString(), password.getText().toString(), new OnCompleteListener<AuthResult>(){
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                FirebaseUser user = fCom.getMAuth().getCurrentUser();
                                fCom.setUserProfile();
                            } else {
                                Toast.makeText(ctx, "Register Failed: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


                    //Generating Token for TumOnline and Generating Secret for TumOnline
                    cCom.generateToken(tumId.getText().toString(), new Response.Listener<String>() {
                        @Override
                        public void onResponse(String Response) {
                            String token = "";
                            boolean Error = false;
                            try {

                                //Extracting Key from XML Response
                                XmlToJson xmlToJson = new XmlToJson.Builder(Response).build();
                                JSONObject jsonObject = xmlToJson.toJson();
                                //Converting
                                token = jsonObject.get("token").toString();
                                //Saving User Data
                                //Im unsure if this works correctly, but i hope it does
                                cCom.saveUserData(tumId.getText().toString(), token);

                                Log.d("HTTP", "Success: Token must be activated via TumOnline");
                            } catch (JSONException e) {
                                //When we dont have token in answer XML we get Nullpointer -> this means our TumId was wrong
                                Toast.makeText(ctx, "Your Token was not activated or doesnt work, please try again", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(ctx, "Failure: " + e.toString(), Toast.LENGTH_LONG).show();
                            }


                            //If no Error we upload Secret
                            if(!Error){
                                String key = cCom.generateSecret();
                                //Secret Upload
                                http.getRequestString("tumonline/wbservicesbasic.secretUpload?pToken=" + token + "&pSecret=" + key + "&pToken=" + token, new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String Response) {
                                        Log.d("HTTP", "Success: " + Response);
                                        try {
                                            XmlToJson xmlToJson = new XmlToJson.Builder(Response).build();
                                            JSONObject jsonObject = xmlToJson.toJson();

                                            //DONE Is this String Comparison okay?
                                            if(jsonObject.get("confirmed").toString().equals("true")){
                                                Log.d("HTTP", "Success: Token is valid and Secret was uploaded");
                                            }

                                        } catch (Exception e) {
                                            Log.d("Failure to Convert", e.toString());
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        //Error Handling
                                        Log.d("HTTP", "Error: " + error.getMessage());
                                    }
                                }, true);
                            }


                            if(!Error){
                                //Switches to Screen For Token Activation, we send our backend data in TokenActivationScreen
                                Intent myIntent = new Intent(view.getContext(), TokenActivationActivity.class);
                                startActivityForResult(myIntent, 0);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            //Error Handling
                            Toast.makeText(ctx, "TumToken failed: " + error.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(ctx, "Your Password do not match", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}