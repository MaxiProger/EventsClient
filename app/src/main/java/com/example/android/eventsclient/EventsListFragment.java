package com.example.android.eventsclient;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.BoringLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.example.android.eventsclient.R.color.colorPrimaryDark;

/**
 * Created by storm2513 on 20.04.17.
 */

public class EventsListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Constants{
    public static final String EVENT_LIST_FRAGMENT_TAG = "EVENT_LIST_FRAGMENT_TAG";
    View previousClicked = null;
    boolean fragmentJustStarted = true;
    public boolean tablet = false;
    public JSONArray eventsArray = null;
    public EventsListAdapter adapter;
    public SharedPreferences preferences;
    public final String FREE_ONLY_ON_LOAD = "2";
    public int currentVersion;
    public SwipeRefreshLayout swipeRefresh;
    public boolean freeChecked = false;
    public String currentQuery = "";
    public boolean shouldRefresh = false;

    public void setTablet(boolean bool){
        tablet = bool;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        currentVersion = getActivity().getSharedPreferences("", Context.MODE_PRIVATE).getInt("version", 0);
        eventsArray = loadJson();
        if((savedInstanceState == null &&
                getActivity().getSharedPreferences("", Context.MODE_PRIVATE).getLong("lastTimeUpdated", 0) < System.currentTimeMillis() - 10800000) ||
                getActivity().getSharedPreferences("", Context.MODE_PRIVATE).getLong("lastTimeUpdated", 0) == 0) {
            new CheckNewVersion().execute();
            shouldRefresh = true;
        }
        if(eventsArray == null) {
            new UpdateEvents().execute();
            shouldRefresh = true;
        }
        View view = inflater.inflate(R.layout.event_list_fragment, container, false);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        final ListView eventsList = (ListView) view.findViewById(R.id.events_list);
        adapter = new EventsListAdapter();
        if(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("defaultOnLoad", "").equals(FREE_ONLY_ON_LOAD)){
            freeChecked = true;
            new AsyncFreeOnly().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        eventsList.setAdapter(adapter);
        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        swipeRefresh.setOnRefreshListener(this);
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        if(shouldRefresh){
            swipeRefresh.setRefreshing(true);
        }
        return view;
    }

    public static EventsListFragment getInstance(){
        return new EventsListFragment();
    }

    @Override
    public void onRefresh() {
        swipeRefresh.setRefreshing(true);
        new CheckNewVersion().execute();
    }

    private class EventsListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            if(eventsArray == null)
                return 0;
            else
                return eventsArray.length();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            JSONObject event = null;
            String parsedDate = "", parsedMonth = "", parsedDay = "",
                    parsedTitle = "", parsedDescription = "";
            try {
                event = (JSONObject)eventsArray.get(position);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                parsedDate = event.getString("day");
                parsedMonth = event.getString("month");
                parsedDay = event.getString("dayOfWeek");
                parsedTitle = event.getString("title");
                parsedDescription = event.getString("description");
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }

            final View view = LayoutInflater.from(getContext()).inflate(R.layout.event_list_item, parent, false);
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setText(parsedDate);
            TextView month = (TextView) view.findViewById(R.id.month);
            month.setText(parsedMonth);
            TextView day = (TextView) view.findViewById(R.id.day);
            day.setText(parsedDay);
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(parsedTitle);
            TextView description = (TextView) view.findViewById(R.id.description);
            description.setText(parsedDescription);
            final JSONObject finalEvent = event;
            view.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View v) {
                    if(tablet) {
                        view.setBackgroundResource(R.color.clickedItem);
                        TypedValue outValue = null;
                        if (previousClicked != null && previousClicked != view) {
                            outValue = new TypedValue();
                            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                            previousClicked.setBackgroundResource(outValue.resourceId);
                        }
                        previousClicked = view;
                    }
                    ((MainActivity)getActivity()).OnItemSelected(getContext(), finalEvent);
                }
            });
            return view;
        }

        public void setJsonArray(JSONArray array){
            eventsArray = array;
            notifyDataSetChanged();
        }

        public void updateJsonArray(){
            notifyDataSetChanged();
        }
    }

    private class UpdateEvents extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void ... v) {
            String json = null;
            try {
                URL url = new URL(EVENTS_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
                urlConnection.disconnect();
                json = buffer.toString();
                Log.d("JSON", "json "+json);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return json;
        }

        protected void onPreExecute() {}

        protected void onPostExecute(String json) {
            try {
                if(json != null) {
                    JSONObject jsonObject = new JSONObject(json);
                    saveJson(jsonObject);
                    JSONArray array = jsonObject.getJSONArray("data");
                    currentVersion = Integer.parseInt(jsonObject.getString("id"));
                    eventsArray = array;
                    adapter.setJsonArray(array);
                    Toast.makeText(getContext(), "События обновлены!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(), "Ошибка соединения с сервером", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e("JSONException", "Error: " + e.toString());
            }
        }
    }

    private class CheckNewVersion extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void ... v) {
            String json = null;
            try {
                URL url = new URL(EVENTS_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                urlConnection.disconnect();
                json = buffer.toString();
                Log.d("JSON", "json "+json);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return json;
        }

        protected void onPreExecute() {}

        protected void onPostExecute(String json) {
            try {
                if(json != null) {
                    JSONObject jsonObject = new JSONObject(json);
                    int newVersion = Integer.parseInt(jsonObject.getString("id"));
                    preferences = getActivity().getSharedPreferences("", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putLong("lastTimeUpdated", System.currentTimeMillis());
                    editor.apply();
                    if (newVersion > currentVersion) {
                        new UpdateEvents().execute();
                    } else {
                        Toast.makeText(getContext(), "У вас последняя версия событий", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e("JSONException", "Error: " + e.toString());
            }
            swipeRefresh.setRefreshing(false);
        }
    }

    private class AsyncSearch extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... params) {
            String query = params[0];
            if(eventsArray != null) {
                query = query.toLowerCase();
                eventsArray = loadJson();
                if(freeChecked){
                    JSONArray newEventsArray = new JSONArray();
                    JSONObject event;
                    for (int i = 0; i < eventsArray.length(); i++){
                        try {
                            event = (JSONObject) eventsArray.get(i);
                            if(event.getString("cost").equals("Бесплатно")){
                                newEventsArray.put(eventsArray.get(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    eventsArray = newEventsArray;
                }
                else{
                    eventsArray = loadJson();
                }
                if(!query.isEmpty()) {
                    JSONArray newEventsArray = new JSONArray();
                    JSONObject event;
                    String title, content, description;
                    for (int i = 0; i < eventsArray.length(); i++){
                        try {
                            event = (JSONObject) eventsArray.get(i);
                            title = event.getString("title").toLowerCase();
                            content = event.getString("content").toLowerCase();
                            description = event.getString("description").toLowerCase();
                            if(title.contains(query) || description.contains(query) || content.contains(query)){
                                newEventsArray.put(eventsArray.get(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    eventsArray = newEventsArray;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.updateJsonArray();
        }
    }

    private class AsyncFreeOnly extends AsyncTask<Void, Void, Void>{
        boolean executeSearch = false;

        @Override
        protected Void doInBackground(Void... params) {
            if(eventsArray != null) {
                if (freeChecked) {
                    JSONArray newEventsArray = new JSONArray();
                    JSONObject event;
                    for (int i = 0; i < eventsArray.length(); i++){
                        try {
                            event = (JSONObject) eventsArray.get(i);
                            if(event.getString("cost").equals("Бесплатно")){
                                newEventsArray.put(eventsArray.get(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    eventsArray = newEventsArray;
                }
                else{
                    if(currentQuery.isEmpty()) {
                        eventsArray = loadJson();
                    }
                    else{
                        eventsArray = loadJson();
                        executeSearch = true;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(executeSearch)
                new AsyncSearch().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentQuery);
            else
                adapter.updateJsonArray();
        }
    }

    public void saveJson(JSONObject json){
        preferences = getActivity().getSharedPreferences("", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("JSON", json.toString());
        try {
            editor.putInt("version", Integer.parseInt(json.getString("id")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.apply();
    }

    public JSONArray loadJson(){
        preferences = getActivity().getSharedPreferences("", Context.MODE_PRIVATE);
        JSONArray jsonArray = null;
        JSONObject jsonObject;
        try {
            jsonObject =  new JSONObject(preferences.getString("JSON", ""));
            jsonArray = new JSONArray(jsonObject.getString("data"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.list_menu, menu);
        if((PreferenceManager.getDefaultSharedPreferences(getContext()).getString("defaultOnLoad", "").equals(FREE_ONLY_ON_LOAD)) && fragmentJustStarted){
            menu.findItem(R.id.free_check_box_item).setChecked(true);
            fragmentJustStarted = false;
        }
        super.onCreateOptionsMenu(menu, inflater);
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setMaxWidth( Integer.MAX_VALUE );
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                callSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                callSearch(newText);
                return true;
            }

            void callSearch(String query) {
                currentQuery = query;
                new AsyncSearch().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query, "true");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.free_check_box_item:
                if(item.isChecked()) {
                    item.setChecked(false);
                    freeChecked = false;
                    new AsyncFreeOnly().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else {
                    item.setChecked(true);
                    freeChecked = true;
                    new AsyncFreeOnly().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                return true;
            case R.id.update_app:
                Uri uri = Uri.parse(APP_URL);
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            case R.id.settings:
                intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
            default:
                break;
        }
        return false;
    }
}
