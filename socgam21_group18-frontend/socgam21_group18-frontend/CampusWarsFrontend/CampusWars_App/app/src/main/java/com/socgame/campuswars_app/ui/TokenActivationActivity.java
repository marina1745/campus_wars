package com.socgame.campuswars_app.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.CampusCom;

import org.json.JSONException;
import org.json.JSONObject;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

public class TokenActivationActivity extends AppCompatActivity {

    /**
     *  In this Activity we show the User how to activate their Token vie Imageviews
     *  we provide a direct link to TumOnline and a Continue Button which checks if the getLectures Call we need works
     *
     *  written by Daniel
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = this.getApplicationContext();
        setContentView(R.layout.activity_token_activation);

        CampusCom cCom = CampusCom.getInstance(ctx);
        BackendCom bCom = BackendCom.getInstance(ctx);

        Button contin = (Button) findViewById(R.id.activation_continue);
        Button toweb = (Button) findViewById(R.id.activation_website);

        ImageView activate_token_1 = (ImageView) findViewById(R.id.imageView);
        ImageView activate_token_2 = (ImageView) findViewById(R.id.imageView2);
        ImageView activate_token_3 = (ImageView) findViewById(R.id.imageView3);
        ImageView activate_token_4 = (ImageView) findViewById(R.id.imageView4);

        int imageResource1 = getResources().getIdentifier("@drawable/activate_token_1", null, this.getPackageName());
        int imageResource2 = getResources().getIdentifier("@drawable/activate_token_2", null, this.getPackageName());
        int imageResource3 = getResources().getIdentifier("@drawable/activate_token_3", null, this.getPackageName());
        int imageResource4 = getResources().getIdentifier("@drawable/activate_token_4", null, this.getPackageName());

        activate_token_1.setImageResource(imageResource1);
        activate_token_2.setImageResource(imageResource2);
        activate_token_3.setImageResource(imageResource3);
        activate_token_4.setImageResource(imageResource4);


        contin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                cCom.test(new Response.Listener<String>() {
                    @Override
                    public void onResponse(String Response) {
                        try {
                            XmlToJson xmlToJson = new XmlToJson.Builder(Response).build();
                            JSONObject object = xmlToJson.toJson();

                            String error = object.get("rowset").toString();

                            Toast.makeText(ctx, "Success!", Toast.LENGTH_SHORT).show();

                            //Sending register to Backend
                            bCom.register();
                            //Now that we know Token works we get lectures and then also send those to backend
                            cCom.getLectures();

                            //Saving Logged in State
                            //We do this here and not earlier because the user needs to finish the registration for both TUM and Firebase
                            SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("loggedIn", true);
                            editor.apply();

                            //Continue
                            Intent myIntent = new Intent(view.getContext(), MainScreenActivity.class);
                            startActivityForResult(myIntent, 0);
                        } catch (JSONException e) {
                            //When we dont have rowset in answer XML we get Nullpointer -> this means our Token was wrong or not activated
                            Toast.makeText(ctx, "Please activate your Token before you continue!", Toast.LENGTH_SHORT).show();
                            //Toast.makeText(ctx, "Token is wrong or was not activated yet", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Failure: " + e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Error Handling
                        Log.d("HTTP", "Error: " + error.getMessage());
                        Toast.makeText(ctx, "HTTP Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        toweb.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.campus.tum.de/"));
                startActivity(browserIntent);
            }
        });
    }
}