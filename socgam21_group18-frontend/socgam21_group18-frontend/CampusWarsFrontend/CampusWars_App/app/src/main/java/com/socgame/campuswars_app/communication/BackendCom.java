package com.socgame.campuswars_app.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackendCom {
    /**
     * provides standard calls for Communication with Backend
     * we mostly have to create the listeners manually in the Activities because Android Studio Nonsense
     *
     * follows Singleton Pattern like all of our Main Communication Classes
     *
     * echo is a test Call to our Server
     *
     * register sends backend a User ID and adds that User to the Database
     *
     * lectures gets a JsonObject with our lectures from a Campus Call, then parses the data and sends it to Backend With UID
     *
     *
     * written by Daniel
     *
     */


    private static Context ctx;
    private static HttpSingleton http;
    private static BackendCom instance;

    private BackendCom(Context ctx){
        this.ctx = ctx;
        this.http = HttpSingleton.getInstance(ctx);
    }

    public static synchronized BackendCom getInstance(Context context) {
        if (instance == null) {
            instance = new BackendCom(context);
        }
        return instance;
    }

    public void echo(){
        http.getRequestString("v1/echo", new Response.Listener<String>() {
            @Override
            public void onResponse(String Response) {
                //On Response
                if(Response.toString().contains("Hallo Echo!")){
                    Log.d("HTTP", "Success Echo: " + Response.toString());
                } else {
                    Log.d("HTTP", "Fail Echo: " + Response.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Error Handling
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        });
    }

    public void register(){
        HttpHeader head = new HttpHeader(ctx);
        http.postRequest("v1/register",head.getHeaders(), new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray Response) {
                //On Response
                //Handle Data
                Log.d("HTTP", "Success: " + Response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Error Handling
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        });
    }

    public void lectures(JSONObject lectures){
        HttpHeader head = new HttpHeader(this.ctx);
        try {
            head.buildPersonalLecturesHeader(lectures);
            http.postRequest("v1/lectures",head.getHeaders(), new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray Response) {
                    //On Response
                    //Handle Data
                    Log.d("HTTP", "Success: " + Response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Error Handling
                    Log.d("HTTP", "Error: " + error.getMessage());
                }
            });
        } catch (JSONException e) {
            Log.d("Error in Register:", e.toString());
        }
    }

    public void roomDetectionGet(Response.Listener<JSONArray> listener, Response.ErrorListener error){
        http.getRequest("v1/roomfinder", listener, error);
    }

    public void roomDetectionPost(Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head){
        http.postRequestObject("v1/roomfinder", head.getHeaders(), listener, error);
    }

    public void quiz(String type, Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head){
        http.postRequestObject("v1/quiz-" + type, head.getHeaders(), listener, error);
    }

    public void quizGet(String type, Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head){
        http.getRequestObject("v1/quiz-" + type, head.getHeaders(), listener, error);
    }

    public void quizAnswer(String type, Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head){
        http.postRequestObject("v1/quiz-" + type, head.getHeaders(), listener, error);
    }

    public void group(Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head){
        http.getRequestObject("v1/mygroup", head.getHeaders(), listener, error);
    }

    public void rally(Response.Listener<JSONObject> listener, Response.ErrorListener error, HttpHeader head, boolean get){
        if(get){
            http.getRequestObject("v1/rally", head.getHeaders(), listener, error);
        } else {
            http.postRequestObject("v1/rally", head.getHeaders(), listener, error);
        }
    }

}
