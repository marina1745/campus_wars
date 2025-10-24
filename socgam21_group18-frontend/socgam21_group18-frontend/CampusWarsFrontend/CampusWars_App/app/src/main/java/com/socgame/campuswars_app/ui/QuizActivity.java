package com.socgame.campuswars_app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.communication.BackendCom;
import com.socgame.campuswars_app.communication.HttpHeader;
//import com.socgame.campuswars_app.databinding.ActivityQuizBinding;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity //implements View.OnClickListener
{
    /**
     * Gets Data from calling Activity via Bundle
     * creates Quiz Duel between 2 Players
     *
     * sends Data back to Server
     *
     * written by Jonas and Daniel
     */
    private Context ctx;
    private BackendCom bCom;

    private String gameId;
    private int playerId; //0/1

    private String oppName;
    private String oppTeam;

    private String topic;
    private String question;
    private String[] wrongAnswers;//3 lang
    private String correctAnswer;

    private int indexRight = -1;

    //UI
    Button buttons[] = new Button[4];


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ctx = this.getApplicationContext();

        //Getting Data From Call
        Bundle b = getIntent().getExtras();
        if(b != null)
        {
            this.gameId = b.getString("gid");
            this.playerId = b.getInt("pid");
            this.topic = b.getString("topic");

            this.oppName = b.getString("opp-name");
            this.oppTeam = b.getString("opp-team");

            this.question = b.getString("question");
            this.wrongAnswers = b.getStringArray("wrongAnswers");
            this.correctAnswer = b.getString("correctAnswer");
        }
        setUI();
    }


    private void setUI()
    {
        //Set UI text
        TextView topicText = this.findViewById(R.id.topicText);
        topicText.setText(topic);


        //Question text
        TextView questionText = this.findViewById(R.id.questionText);
        questionText.setText(question);

        //Randomize answer order
        List<String> allAnswers = new ArrayList<String>(Arrays.asList(wrongAnswers));
        allAnswers.add(correctAnswer);
        Collections.shuffle(allAnswers);
        indexRight = allAnswers.indexOf(correctAnswer);


        //give answers to buttons
        buttons[0] = this.findViewById(R.id.answerButtonA);
        buttons[1] = this.findViewById(R.id.answerButtonB);
        buttons[2] = this.findViewById(R.id.answerButtonC);
        buttons[3] = this.findViewById(R.id.answerButtonD);

        for(int i = 0; i < buttons.length; i++)
        {
            buttons[i].setText(allAnswers.get(i));
        }


        //set responses
        buttons[0].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){ answer(0);}
        });

        buttons[1].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){ answer(1);}
        });

        buttons[2].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){ answer(2);}
        });

        buttons[3].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){ answer(3);}
        });
    }

    private void answer(int buttonIndex)
    {

        int result = 0;
        if(buttonIndex == indexRight) //Correct Answer
        {
            result = 1;
            Toast.makeText(this, "Correct!", Toast.LENGTH_LONG).show();
        }
        else //wrong answer
        {
            Toast.makeText(this, "False", Toast.LENGTH_LONG).show();
        }

        Bundle b = new Bundle();
        b.putString("opp-name", oppName); //Challenger name
        b.putString("opp-team", oppTeam); //Challenger team
        b.putString("playerId", Integer.toString(this.playerId)); //PlayerId
        b.putString("gameId", this.gameId); //gameId
        b.putString("result", Integer.toString(result)); //game result

        //DONE: send proper data to result screen
        Intent myIntent = new Intent(this, ResultActivity.class);
        myIntent.putExtras(b);
        startActivityForResult(myIntent, 0);
    }
}