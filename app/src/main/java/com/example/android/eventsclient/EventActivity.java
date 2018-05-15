package com.example.android.eventsclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;


public class EventActivity extends AppCompatActivity implements Constants {

    public void createEventFragment(JSONObject event){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        EventFragment fragment = new EventFragment();
        fragment.setEvent(event);
        fragmentTransaction.add(R.id.content, fragment , "EVENT_FRAGMENT_TAG");
        fragmentTransaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        Intent intent = getIntent();
        JSONObject event = null;
        try {
            event = new JSONObject(intent.getStringExtra("event"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        createEventFragment(event);
    }
}
