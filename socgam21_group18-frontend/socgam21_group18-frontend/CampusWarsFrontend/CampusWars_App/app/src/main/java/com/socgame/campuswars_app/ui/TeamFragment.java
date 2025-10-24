package com.socgame.campuswars_app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TeamFragment extends Fragment
{
    /**
     *Here we display info about the players team
     *including a list of team members
     *
     * UI by Jonas, Calls by Daniel
     **/
    View fragmentView = null;

    private String color;
    private String teamName;
    private String[] members;

    public TeamFragment()
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
        Context ctx = this.getContext();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_team, container, false);
        this.fragmentView = view;

        ListView listView = view.findViewById(R.id.memberList);

        //Getting our Information from Backend and setting it in our GetResponseListener
        BackendCom bCom = BackendCom.getInstance(ctx);
        HttpHeader header = new HttpHeader(ctx);
        bCom.group(myGroupGet(), httpErrorListener(), header);
        bCom.roomDetectionGet(roomfinderGetListener(), httpErrorListener());

        return view;
    }

    public void setControl(double percentage)
    {
        TextView currentControl = (TextView) fragmentView.findViewById(R.id.textCurrentControl);
        currentControl.setText("" + Math.round(percentage) + "%");
    }

    public void setMembers(String[] names)
    {
        ListView listView = fragmentView.findViewById(R.id.memberList);

        //no custom Array Adapter (not needed, unless we show more info)
        //ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_activated_1, array);
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(getContext(), R.layout.teammember, names);
        listView.setAdapter(itemsAdapter);
    }

    public void setTeamInfo(String name, int memberCount, String color)
    {
        int colorIndex = Color.parseColor(color);

        TextView nameText = fragmentView.findViewById(R.id.teamName);
        nameText.setText(name);
        nameText.setTextColor(colorIndex);

        TextView memberText = fragmentView.findViewById(R.id.textCurrentMembers);
        memberText.setText(memberCount + " Members");
    }

    private Response.Listener<JSONObject> myGroupGet()
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    color = response.getString("colour");
                    teamName = response.getString("name");
                    JSONArray tMembers = response.getJSONArray("members");

                    members = new String[tMembers.length()];
                    for(int i = 0; i < tMembers.length(); i++){
                        members[i] = tMembers.getString(i);
                    }

                    setMembers(members);
                    setTeamInfo(teamName, members.length, color);

                } catch (JSONException e) {
                    Log.d("My Group:", e.toString());
                }

            }
        };
    }

    private Response.Listener<JSONArray> roomfinderGetListener() {
        return new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    int counter = 0;
                    int lectureHallCount = response.length();
                    for (int i = 0; i < response.length(); i++) {
                        //Getting JSONs
                        JSONObject lectureHall = response.getJSONObject(i);
                        JSONObject occupier = lectureHall.getJSONObject("occupier");
                        if(occupier.getString("name").contains(teamName)){
                            counter++;
                        }
                    }
                    setControl(((double) counter / (double) lectureHallCount)*100);
                } catch (Exception e) {
                    Log.d("roomFinderGetListener: ", e.toString());
                }
            }
        };
    }

    private Response.ErrorListener httpErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Error Handling
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        };
    }

}