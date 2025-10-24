package com.socgame.campuswars_app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.model.LatLng;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MatchMakingActivity extends AppCompatActivity
{
    /**
     * Shows the User if their Quiz is available and if the server has answered to their requests
     *
     * written by Daniel and Jonas
     */
    //The state represents is "what is finished?"
    private enum State  {BEGIN, REQUEST,WAIT, READY};
    private State state = State.BEGIN;
    private Context ctx = this;//this.getApplicationContext();
    private double latitude;
    private double longitude;
    private String roomName;
    private String lid;
    private HttpHeader head;
    private BackendCom bCom;
    private Bundle quizBundle;
    private boolean switchedToQuiz = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_making);

        bCom = BackendCom.getInstance(ctx);

        Bundle b = getIntent().getExtras();
        if(b != null){
            this.latitude = b.getDouble("latitude");
            this.longitude = b.getDouble("longitude");
            this.roomName = b.getString("roomName");
            this.lid = b.getString("lid");
        }
        this.head = new HttpHeader(ctx);
        head.buildQuizHeader(latitude, longitude, lid, roomName);

        //Repeating our Calls every 5 Seconds
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(switchedToQuiz != true) {
                    doCommunication(state);
                    handler.postDelayed(this, 5000);
                }
            }
        };

        handler.post(runnable);
    }

    //this can be deleted
    private void debugChange()
    {
        changeUiState(State.BEGIN);

        Handler handler = new Handler();
        handler.postDelayed
        (
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        changeUiState(State.REQUEST);
                    }
                },
                1000
        );


        Handler handler2 = new Handler();
        handler2.postDelayed
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                changeUiState(State.WAIT);
                            }
                        },
                        2000
                );


        Handler handler3 = new Handler();
        handler3.postDelayed
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                changeUiState(State.READY);

                                Intent myIntent = new Intent(MatchMakingActivity.this, QuizActivity.class);
                                startActivityForResult(myIntent, 0);
                            }
                        },
                        3000
                );
    }

    //DONE?
    private void doCommunication(State s)
    {
        switch (s)
        {
            case BEGIN:
                bCom.quiz("request", quizRequestListener(), httpErrorListener(), head);
                break;
            case REQUEST:
                //Request done
                bCom.quiz("refresh", quizRefreshListener(), httpErrorListener(), head);
                break;
            case READY:
                //wait a sec and then change to quiz
                changeUiState(state.READY);
                Handler handler2 = new Handler();
                handler2.postDelayed
                (
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            switchedToQuiz = true;
                            Intent myIntent = new Intent(MatchMakingActivity.this, QuizActivity.class);
                            myIntent.putExtras(quizBundle);
                            startActivityForResult(myIntent, 0);
                        }
                    },
                    1000
                );
                break;
        }

    }

    private Response.Listener<JSONObject> quizRequestListener()
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    response.getString("quiz-request");
                    state = state.REQUEST;
                    changeUiState(state.REQUEST);
                    doCommunication(state);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.Listener<JSONObject> quizRefreshListener()
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    quizBundle = new Bundle();
                    quizBundle.putString("gid", response.getString("gid"));
                    quizBundle.putString("topic", response.getString("name"));
                    quizBundle.putString("opp-name", response.getString("opp-name"));
                    quizBundle.putString("opp-team", response.getString("opp-team"));
                    quizBundle.putInt("pid", response.getInt("pid"));
                    JSONObject quiz = response.getJSONObject("quiz");
                    quizBundle.putString("question", quiz.getString("question"));
                    quizBundle.putString("correctAnswer", quiz.getString("rightAnswer"));
                    JSONArray wAnswers = quiz.getJSONArray("wrongAnswers");
                    String[] wrongAnswers = new String[]{wAnswers.getString(0), wAnswers.getString(1), wAnswers.getString(2)};
                    quizBundle.putStringArray("wrongAnswers", wrongAnswers);

                    changeUiState(state.WAIT);
                    state = state.READY;

                    doCommunication(state);
                } catch (Exception e) {
                    Log.d("Error in Quiz Refresh Call", e.toString());
                }
            }
        };
    }

    private Response.ErrorListener httpErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        };
    }


    //The state represents is "what is finished?"
    private void changeUiState(State s)
    {
        TextView requestText = this.findViewById(R.id.match_request);
        TextView waitText = this.findViewById(R.id.match_waiting);
        TextView readyText = this.findViewById(R.id.match_ready);

        state = s;

        Drawable loading = getResources().getDrawable(R.drawable.ic_sand_clock);
        Drawable done = getResources().getDrawable(R.drawable.ic_check);

        int gray = ContextCompat.getColor(this, R.color.darkAccent);
        int text = ContextCompat.getColor(this, R.color.text);

        switch (s)
        {
            case REQUEST:
                requestText.setCompoundDrawablesWithIntrinsicBounds(done, null, null, null);
                requestText.setTextColor(text);
                break;

            case WAIT:
                waitText.setCompoundDrawablesWithIntrinsicBounds(done, null, null, null);
                waitText.setTextColor(text);
                break;

            case READY:
                readyText.setCompoundDrawablesWithIntrinsicBounds(done, null, null, null);
                readyText.setTextColor(text);
                break;

            default:
                requestText.setCompoundDrawablesWithIntrinsicBounds(loading, null, null, null);
                waitText.setCompoundDrawablesWithIntrinsicBounds(loading, null, null, null);
                readyText.setCompoundDrawablesWithIntrinsicBounds(loading, null, null, null);

                requestText.setTextColor(gray);
                waitText.setTextColor(gray);
                readyText.setTextColor(gray);
        }
    }
}