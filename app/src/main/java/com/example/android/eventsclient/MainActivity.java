package com.example.android.eventsclient;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity{
    EventFragment eventFragment = new EventFragment();
    EventsListFragment eventsListFragment = new EventsListFragment();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            setContentView(R.layout.activity_main);
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
                eventFragment = new EventFragment();
                eventsListFragment.setTablet(true);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.list_frag, eventsListFragment);
                transaction.commit();
            } else {
                createEventListFragment();
            }
        }


    }

    public void createEventListFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment eventListFragment = fragmentManager.findFragmentByTag(EventsListFragment.EVENT_LIST_FRAGMENT_TAG);
        if(eventListFragment == null){
            fragmentManager.beginTransaction().add(R.id.list_frag, EventsListFragment.getInstance(),
                                                   EventsListFragment.EVENT_LIST_FRAGMENT_TAG).commit();
        }
    }

    public void OnItemSelected(Context context, JSONObject event){
        if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE){
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(eventFragment);
            eventFragment = new EventFragment();
            eventFragment.setEvent(event);
            eventFragment.setTablet(true);
            transaction.add(R.id.event_frag, eventFragment);
            transaction.commit();
        }
        else{
            Intent intent = new Intent(context, EventActivity.class);
            intent.putExtra("event", event.toString());
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }
}
