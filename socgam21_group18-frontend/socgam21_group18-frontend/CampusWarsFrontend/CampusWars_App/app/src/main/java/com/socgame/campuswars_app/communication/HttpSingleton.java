package com.socgame.campuswars_app.communication;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpSingleton {

    /**
     * provides Structure for Http Calls
     *
     * follows Singleton Pattern like all of our Main Communication Classes
     *
     * we have 2 Urls we need and use, CampusOnline and our Server
     *
     * we have methods for standart Get and Post Requests to our Backend in which we get JSONArrays back
     *
     * we also have a more general getRequestString Method which can receive a String for further processing
     * we have 2 versions of this, one with a boolean which is set true if we want to call tum, which provides Data in XML format
     *
     * afterwards we also had to add different other HttpRequest Methods for JSONObjects with GET and POST and Headers, we had to have so many classes because of volley weirdness
     *
     * written by Daniel
     */


    //https://developer.android.com/training/volley/requestqueue
    private static HttpSingleton instance;
    private RequestQueue requestQueue;
    private static Context ctx;
    private static String url;
    private static String tumUrl;

    //For HTTP Requests
    private JSONArray response;
    private boolean success;

    private HttpSingleton(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
        url = "http://167.71.33.65/";
        tumUrl = "https://campus.tum.de/";
        response = null;
        //Refer to this for testing with localhost
        //https://developer.android.com/studio/run/emulator-networking.html
    }

    public static synchronized HttpSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new HttpSingleton(context);
        }
        return instance;
    }

    public String getUrl(){
        return this.url;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    //Standart getRequest is for JSONARRAY
    public void getRequest(String route, Response.Listener<JSONArray> listener, Response.ErrorListener errlsn){
        /**
         * create get request of string: url + route
         * @param route String for route to take ob HTTP Server
         * @param listener implement new Listener with overwrite OnResponse to get Data
         * @param errlsn implement new Error Listener with overwrite OnErrorResponse to get Data
         * getRequest(route, new Response.Listener<JSONArray>() {
         *      @Override
         *      public void onResponse(JSONArray Response) {
         *      //On Response
         *      //Handle Data
         *      Log.d("HTTP", "Success: " + Response.toString());
         *      }
         * }, new Response.ErrorListener() {
         *      @Override
         *      public void onErrorResponse(VolleyError error) {
         *      //Error Handling
         *      Log.d("HTTP", "Error: " + error.getMessage());
         *      }
         * });
         * @return void
         */

        /*
        StringRequest stringRequest = new StringRequest(Request.Method.GET, this.url + route,null, listener, errlsn);
        HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
        */
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, this.url + route,null, listener, errlsn);
        HttpSingleton.getInstance(ctx).addToRequestQueue(jsonArrayRequest);
    }

    //Standart postRequest is for JSONARRAY
    public void postRequest(String route, HashMap<String, String> params, Response.Listener<JSONArray> listener, Response.ErrorListener errlsn){
        /**
         * create get request of string: url + route
         * @param route String for route to take ob HTTP Server
         * @param params HashMap<String, String> params will be used as post Parameters; fill with params.put("token", "token_value");
         * @param listener implement new Listener with overwrite OnResponse to get Data
         * @param errlsn implement new Error Listener with overwrite OnErrorResponse to get Data
         * postRequest(route, new Response.Listener<JSONArray>() {
         *      @Override
         *      public void onResponse(JSONArray Response) {
         *      //On Response
         *      //Handle Data
         *      Log.d("HTTP", "Success: " + Response.toString());
         *      }
         * }, new Response.ErrorListener() {
         *      @Override
         *      public void onErrorResponse(VolleyError error) {
         *      //Error Handling
         *      Log.d("HTTP", "Error: " + error.getMessage());
         *      }
         * });
         * @return void
         */
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.POST, this.url + route, null, listener, errlsn) {
            //this is the part, that adds the header to the request
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> parameters = params;
                return parameters;
            }
        };
        HttpSingleton.getInstance(ctx).addToRequestQueue(jsonArrayRequest);
    }

    public void getRequestObject(String route, HashMap<String, String> params, Response.Listener<JSONObject> listener, Response.ErrorListener errlsn){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, this.url + route,null, listener, errlsn) {
            //this is the part, that adds the header to the request
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> parameters = params;
                return parameters;
            }
        };
        HttpSingleton.getInstance(ctx).addToRequestQueue(jsonObjectRequest);
    }

    public void postRequestObject(String route, HashMap<String, String> params, Response.Listener<JSONObject> listener, Response.ErrorListener errlsn){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, this.url + route, null, listener, errlsn) {
            //this is the part, that adds the header to the request
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> parameters = params;
                return parameters;
            }
        };
        HttpSingleton.getInstance(ctx).addToRequestQueue(jsonObjectRequest);
    }

    public void postRequestString(String route, HashMap<String, String> params, Response.Listener<String> listener, Response.ErrorListener errlsn){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, this.url + route, listener, errlsn) {
            //this is the part, that adds the header to the request
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> parameters = params;
                return parameters;
            }
        };
        HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
    }

    public void getRequestString(String route, Response.Listener<String> listener, Response.ErrorListener errlsn){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, this.url + route, listener, errlsn);
        HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
    }

    public void getRequestString(String route, Response.Listener<String> listener, Response.ErrorListener errlsn, boolean tum){
        if(tum){
            StringRequest stringRequest = new StringRequest(Request.Method.GET, this.tumUrl + route, listener, errlsn);
            HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
        } else {
            StringRequest stringRequest = new StringRequest(Request.Method.GET, this.url + route, listener, errlsn);
            HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
        }
    }

    public void getRequestStringParams(String route, HashMap<String, String> params, Response.Listener<String> listener, Response.ErrorListener errlsn){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, this.url + route, listener, errlsn) {
            //this is the part, that adds the header to the request
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> parameters = params;
                return parameters;
            }
        };
        HttpSingleton.getInstance(ctx).addToRequestQueue(stringRequest);
    }
}


