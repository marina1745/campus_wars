package com.socgame.campuswars_app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.service.autofill.FieldClassification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.model.LatLng;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.Sensor.GpsObserver;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*

*/
public class TerritoryFragment extends Fragment  implements GpsObserver //implements View.OnClickListener
{
    /**
     * Shows the current Territory, with some data about who owns it
     * adds buttons for quiz and rally
     *
     * written by Daniel and Jonas
     */
    View fragmentView = null;
    private Context ctx;
    private BackendCom bCom;
    private String lectureId;
    private String lectureHall = "nothing";
    private LatLng lectureLoc = null;
    private String rallyResponse = "null";
    private boolean currentTimeOut = false;

    public TerritoryFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_territory, container, false);
        this.fragmentView = view;

        ctx = this.getContext();
        bCom = BackendCom.getInstance(ctx);

        Button challenge = (Button) view.findViewById(R.id.challengeButton);
        challenge.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                if(lectureHall.equals("nothing"))
                {
                    Toast.makeText(getActivity(), "Please enter a lecture hall to challenge an opponent", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Intent myIntent = new Intent(view.getContext(), MatchMakingActivity.class);
                    //We use bundles to give parameters to our QuizActivity
                    Bundle b = new Bundle();
                    b.putDouble("latitude", lectureLoc.latitude); //Question
                    b.putDouble("longitude", lectureLoc.longitude); //Question
                    b.putString("roomName", lectureHall); //Challenger name
                    b.putString("lid", lectureId); //Challenger name
                    myIntent.putExtras(b);
                    startActivityForResult(myIntent, 0);
                }
            }
        });


        Button rally = (Button) view.findViewById(R.id.raidButton);
        rally.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if(lectureHall.contains("nothing")){
                    Toast.makeText(getActivity(), "Please enter a lecture hall to start a raid", Toast.LENGTH_LONG).show();
                } else {
                    HttpHeader head = new HttpHeader(ctx);
                    head.buildRallyHeader(lectureHall);
                    bCom.rally(rallyPostListener(), httpErrorListener(), head, false);
                }
            }
        });


        register(getActivity());

        return view;
    }

    public void setHallInfo(String name, String owner, String lecture, LatLng loc)
    {
        lectureLoc = loc;

        TextView nameText = fragmentView.findViewById(R.id.lectureHall);
        nameText.setText(name);
        //nameText.setColor(color);

        TextView ownerText = fragmentView.findViewById(R.id.textCurrentOwner);
        ownerText.setText(owner);

        TextView lectureText = fragmentView.findViewById(R.id.textCurrentLecture);
        lectureText.setText(lecture);
        //lectureText.setText("Debug super long long long long long lecture hall lecture name");
    }

    private void setTimeOut(boolean timeOut)
    {
        currentTimeOut = false;
        TextView text = (TextView) getActivity().findViewById(R.id.timeoutID);
        text.setVisibility((timeOut ?  View.VISIBLE : View.INVISIBLE));
    }

    private Response.Listener<JSONObject> roomfinderPostListener(double latitude, double longitude)
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String currentLecture = response.getString("currentLecture");
                    if (currentLecture.equals(null)) {
                        currentLecture = "No active Lecture";
                    }
                    lectureId = response.getString("lid");
                    double multiplier = response.getDouble("multiplier");
                    JSONObject occupancy = response.getJSONObject("occupancy");
                    String occupier = response.getString("occupier");

                    lectureHall = response.getString("room_name");
                    setHallInfo(lectureHall, occupier, currentLecture, new LatLng(latitude, longitude));
                    if(currentTimeOut == true){
                        currentTimeOut = false;
                        setTimeOut(currentTimeOut);
                    }

                } catch(JSONException j){
                    //if JSONException we know we got a different answer -> eg.: time_out or nothing near me
                    //This isnt the prettiest way to do it, but i will always get an exception if i check for a key and it isnt there, so i cant do it another way
                    try{
                        //Checking if there is nothing near me
                        String message = response.getString("message");
                        if(message.contains("nothing near you")){
                            lectureHall = "nothing";
                            setHallInfo("Not in a lecture hall", "", "", new LatLng(latitude, longitude));
                        }
                    } catch(JSONException e){}

                    try{
                        //Checking if i am timed_out
                        String timeOut = response.getString("time_pretty");
                        //lectureHall = "nothing";
                        //setHallInfo("Timed Out", "", "", new LatLng(latitude, longitude));
                        currentTimeOut = true;
                        setTimeOut(currentTimeOut);
                        Toast.makeText(getActivity(), "You lost and are timed out until: "+ timeOut, Toast.LENGTH_LONG).show();
                    } catch(JSONException e)
                    {
                        // If an Exception happens here the server messed up
                    }

                    //We dont log our exceptions here because they will happen when we look for a key and arent sure if it exists
                    //Log.d("Exception in TimeOut", j.toString());
                }
            }
        };
    }

    private Response.Listener<JSONObject> rallyGetListener(){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //Need to compare as Strings because direct JSONObject compare doesnt work
                if(rallyResponse.equals(response.toString())){
                    //We just dont want the user to get the same rally several times
                    //Log.d("Rally", "Rally already gotten");
                } else {
                    try {
                        if(!response.getString("rally").contains("null")){
                            Log.d("Rally", response.toString());
                            JSONObject details = response.getJSONObject("rally");
                            SharedPreferences settings = ctx.getSharedPreferences("userdata", 0);
                            String myName = settings.getString("name", "empty");
                            String name = details.getString("name");
                            rallyResponse = response.toString();
                            if(!myName.contains(name)){
                                //Checking if user doesnt get their own rally
                                String room = details.getString("room");
                                Toast.makeText(getActivity(), name + " needs your help in " + room, Toast.LENGTH_LONG).show();
                            } else{
                                //Log.d("Rally", "This is my own rally");
                            }
                        }
                    } catch (Exception e) {
                        Log.d("Error in Rally", e.toString());
                    }
                }
            }
        };
    }

    private Response.Listener<JSONObject> rallyPostListener(){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String rallyPossible = response.getString("rally");
                    if(rallyPossible.contains("false")){
                        Toast.makeText(getActivity(), "Your Team already has an ongoing Rally!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "You sent for your troops!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.d("Error in Rally Post:", e.toString());
                }
            }
        };
    }

    private Response.ErrorListener httpErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                lectureHall = "nothing";
                //Error Handling
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        };
    }

    @Override
    public void OnLocationUpdate(LatLng loc)
    {
        HttpHeader head = new HttpHeader(ctx);
        head.buildRoomFinderHeader(loc.latitude, loc.longitude);
        bCom.roomDetectionPost(roomfinderPostListener(loc.latitude, loc.longitude), httpErrorListener(), head);
        //New Header for rally-check
        head = new HttpHeader(ctx);
        bCom.rally(rallyGetListener(), httpErrorListener(), head, true);
    }
}