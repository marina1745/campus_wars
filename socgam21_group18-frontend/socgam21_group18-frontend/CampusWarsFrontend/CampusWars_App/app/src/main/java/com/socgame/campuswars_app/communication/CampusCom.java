package com.socgame.campuswars_app.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

public class CampusCom {

    /**
     * provides standard calls to TumOnline
     *
     * follows Singleton Pattern like all of our Main Communication Classes
     *
     * we can generate a TumOnline Token, getLectures and test our Token with a getLectures Call
     *
     * we only save user data for tumId and Token for TumOnline locally
     * this is so we could for example refresh our lectures after a semester and get new ones
     *
     * written by Daniel
     */


    private static String pToken;
    private static String tumId;
    private static Context ctx;
    private static CampusCom instance;

    private static HttpSingleton http;

    private Map<String, String> params;
    private String[] lectures;

    private CampusCom(Context context) {
        this.ctx = context;
        // Get from the SharedPreferences
        SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
        this.pToken = settings.getString("pToken", "empty");
        this.tumId = settings.getString("tumId", "empty");
        this.http = HttpSingleton.getInstance(this.ctx);
    }

    public static synchronized CampusCom getInstance(Context context) {
        if (instance == null) {
            instance = new CampusCom(context);
        }
        return instance;
    }

    public void saveUserData(String tumId, String pToken){
        //This is not the safest way to store User Data, but it works in this case
        //Saving User Data:
        SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("tumId", tumId);
        editor.putString("pToken", pToken);
        // Apply the edits!
        editor.apply();

        //Overwriting our Variables so we can use it in other methods easily
        this.pToken = settings.getString("pToken", "empty");
        this.tumId = settings.getString("tumId", "empty");
    }

    public String generateSecret(){
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance("AES");
        } catch (Exception e) {
            Log.d("CampusCom", "Secret Key generation failed: " + e.toString());
        }
        kg.init(128);
        SecretKey key = kg.generateKey();
        return key.toString();
    }

    public HttpSingleton getHttp(){
        //We dont necessarily need this because we follow singelton pattern but sometimes it saves a line of code
        return http;
    }

    public void generateToken(String Id, Response.Listener<String> listener, Response.ErrorListener error){
        //You should only call this Method once
        //This Method is now integrated into Register Activity because of Android Studio NonSense
        http.getRequestString("tumonline/wbservicesbasic.requestToken?pUsername=" + Id + "&pTokenName=CampusWarsApp", listener, error, true);
    }

    public void getLectures(){
        //https://campus.tum.de/tumonline/wbservicesbasic.veranstaltungenEigene?pToken=pToken
        http.getRequestString("tumonline/wbservicesbasic.veranstaltungenEigene?pToken=" + pToken, new Response.Listener<String>() {
            @Override
            public void onResponse(String Response) {
                //Remove Log.d ?
                Log.d("HTTP", "Success: " + Response);

                try {
                    XmlToJson xmlToJson = new XmlToJson.Builder(Response).build();
                    JSONObject lectures = xmlToJson.toJson();

                    BackendCom bCom = BackendCom.getInstance(ctx);
                    bCom.lectures(lectures);

                    Log.d("CampusCom", lectures.toString());

                    Log.d("HTTP", "Success: " + "converted JSON Object and gave it to HTTP Header");
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

    private void getLectureTimeAndLocation(){
        //depricated -> data is partly unreadable/not helpful because of missing consistent formatting by lecturers and professors, also Online Semester
    }

    public void test(Response.Listener<String> listener, Response.ErrorListener error){
        //Here we test if our token works, this does mean we send 2 requests for our lectures but we want to make sure
        http.getRequestString("tumonline/wbservicesbasic.veranstaltungenEigene?pToken=" + pToken, listener, error, true);
    }

}
