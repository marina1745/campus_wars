package com.socgame.campuswars_app.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpHeader;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class ResultActivity extends AppCompatActivity
{
    /**
     *Here we display info about the players team
     *including a list of team members
     *
     * UI by Jonas, Calls by Daniel
     **/
    private Context ctx;

    private TextView loading = null;
    private LinearLayout resultWrapper = null;

    private ImageView image = null;
    private TextView resText = null;

    private TextView name = null;
    private TextView team = null;

    private enum WinState{WIN, TIE, LOSE};

    private String oppName, oppTeam, playerId, gameId, result;

    private BackendCom bCom;

    private boolean gotQuizState;
    private WinState state;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ctx = this.getApplicationContext();
        bCom = BackendCom.getInstance(ctx);

        gotQuizState = false;

        //get needed references
        image = (ImageView) findViewById(R.id.resultImg);
        loading = (TextView) findViewById(R.id.result_loading_text);
        resultWrapper = (LinearLayout) findViewById(R.id.resultWrapper);
        resText = (TextView) findViewById(R.id.resultTxt);
        name = (TextView) findViewById(R.id.enemyName);
        team = (TextView) findViewById(R.id.enemyTeam);


        //Set start visibility
        loading.setVisibility(View.VISIBLE);
        resultWrapper.setVisibility(View.INVISIBLE);

        //Continue Button
        Button next = (Button) findViewById(R.id.resultButton);
        next.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                Intent myIntent = new Intent(view.getContext(), MainScreenActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        //Getting Bundle Data
        Bundle b = getIntent().getExtras();
        if(b != null)
        {
            this.gameId = b.getString("gameId");
            this.playerId = b.getString("playerId");
            this.oppName = b.getString("opp-name");
            this.oppTeam = b.getString("opp-team");
            this.result = b.getString("result");
            name.setText(this.oppName);
            team.setText(this.oppTeam);
        }

        HttpHeader head = new HttpHeader(ctx);
        head.buildQuizAnswerHeader(playerId, gameId, result, result);
        bCom.quizAnswer("answer", quizAnswerListener(), httpErrorListener(), head);

        //Repeating our Calls every 5 Seconds
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                bCom.quizGet("state", quizStateListener(), httpErrorListener(), head);
                if(gotQuizState == false){
                    handler.postDelayed(this, 5000);
                } else {
                    setUIInfo(state);
                }
            }
        };

        handler.post(runnable);
    }


    private void setUIInfo(WinState result)
    {
        int imageResource = 0;
        String text = "";

        switch (result)
        {
            case WIN:
                imageResource = getResources().getIdentifier("@drawable/img_winning", null, this.getPackageName());
                text = "You Won!";
                break;
            case TIE:
                imageResource = getResources().getIdentifier("@drawable/img_tie", null, this.getPackageName());
                text = "You Tied";
                break;
            case LOSE:
                imageResource = getResources().getIdentifier("@drawable/img_lose", null, this.getPackageName());
                text = "You Lost..." + "\n" + "You will not be able to participate for 10 minutes";
                break;
        }

        image.setImageResource(imageResource);
        resText.setText(text);

        loading.setVisibility(View.INVISIBLE);
        resultWrapper.setVisibility(View.VISIBLE);
    }

    private Response.Listener<JSONObject> quizAnswerListener()
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //if we get key quiz answer it will always contain "ok"
                    response.getString("quiz-answer");
                } catch (JSONException e) {
                    Log.d("Quiz_Answer", "Something went wrong with sending the Quiz Answer: " + e.toString());
                }
            }
        };
    }

    private Response.Listener<JSONObject> quizStateListener()
    {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getString("quiz-state").contains("true")){
                        gotQuizState = true;
                        String result = response.getString("result");
                        if(result.contains("WON")){
                            state = WinState.WIN;
                        } else if (result.contains("LOST")){
                            state = WinState.LOSE;
                        } else{
                            state = WinState.TIE;
                        }
                    }
                } catch (JSONException e) {
                    Log.d("JSONException in Quiz State", e.toString());
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
}