package com.example.android.eventsclient;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import static android.content.Intent.ACTION_SEND;

/**
 * Created by storm2513 on 24.04.17.
 */

public class EventFragment extends Fragment implements Constants {
    private JSONObject event = null;
    private boolean tablet = false;


    String htmlData = "", cost = "", title = "", href = "", place = "", day = "", month = "";

    public void setEvent(JSONObject event){
        this.event = event;
    }

    public void setTablet(boolean bool){
        tablet = bool;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container, false);
        if(event != null) {
            try {
                htmlData = event.getString("content");
                cost = event.getString("cost");
                title = event.getString("title");
                href = event.getString("href");
                place = event.getString("place");
                day = event.getString("day");
                month = event.getString("month");
                Log.d("EVENT", title);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            WebView webview = (WebView) view.findViewById(R.id.webview);
            webview.loadDataWithBaseURL("", htmlData, "text/html", "UTF-8", "");

            TextView titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setText(title);
            TextView costTextView = (TextView) view.findViewById(R.id.cost);
            costTextView.setText("Стоимость: " + cost.toLowerCase());
            final Button placeButton = (Button) view.findViewById(R.id.place_button);
            placeButton.setOnClickListener(new View.OnClickListener() {
                long lastTimeClicked = 0;

                @Override
                public void onClick(View v) {
                    if (lastTimeClicked == 0 || lastTimeClicked < System.currentTimeMillis() - 3000) {
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", place);
                        clipboard.setPrimaryClip(clip);
                        lastTimeClicked = System.currentTimeMillis();
                        Toast.makeText(getContext(), place, Toast.LENGTH_LONG).show();
                        Toast.makeText(getContext(), "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            if (place.equals("null")) {
                placeButton.setVisibility(View.GONE);
            }
            Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
            toolbar.setTitle(getEventType());
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            if(!tablet) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

        }
        return view;
    }

    public String getEventType(){
        String mainTitle = "Мероприятие";
        if(title.toLowerCase().contains("хакатон"))
            mainTitle = "Хакатон";
        else if(title.toLowerCase().contains("тренинг"))
            mainTitle = "Тренинг";
        else if(title.toLowerCase().contains("meetup"))
            mainTitle = "Meetup";
        else if(title.toLowerCase().contains("конференция"))
            mainTitle = "Конференция";
        else if(title.toLowerCase().contains("мастер-класс"))
            mainTitle = "Мастер-класс";
        else if(title.toLowerCase().contains("курс"))
            mainTitle = "Курс";
        else if(title.toLowerCase().contains("школа"))
            mainTitle = "Школа";
        else if(htmlData.toLowerCase().contains("хакатон"))
            mainTitle = "Хакатон";
        else if(htmlData.toLowerCase().contains("тренинг"))
            mainTitle = "Тренинг";
        else if(htmlData.toLowerCase().contains("meetup"))
            mainTitle = "Meetup";
        else if(htmlData.toLowerCase().contains("конференция"))
            mainTitle = "Конференция";
        else if(htmlData.toLowerCase().contains("мастер-класс"))
            mainTitle = "Мастер-класс";
        else if(htmlData.toLowerCase().contains("курс"))
            mainTitle = "Курс";
        else if(htmlData.toLowerCase().contains("школа"))
            mainTitle = "Школа";
        return mainTitle;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.event_menu, menu);
        if(place.equals("null")){
            MenuItem item = menu.findItem(R.id.open_in_map_item);
            item.setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.open_in_browser_item:
                Uri uri = Uri.parse(EVENTS_DEV_BY_URL + href);
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            case R.id.open_in_map_item:
                try {
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri location = Uri.parse("geo:53.899910,27.559?q=" + place.replace(" ", "+"));
                    getActivity().getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
                    intent.setPackage("com.google.android.apps.maps");
                    intent.setData(location);
                    startActivity(intent);
                }
                catch(PackageManager.NameNotFoundException e){
                    Toast.makeText(getContext(), "Google Maps не установлены", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.add_to_calendar:
                intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra(CalendarContract.Events.TITLE, title);
                if(place != "null")
                    intent.putExtra(CalendarContract.Events.EVENT_LOCATION, place);
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                Calendar eventCalendar = Calendar.getInstance();
                eventCalendar.set(calendar.get(Calendar.YEAR), getNumberOfMonth(month), getNumberOfDay(day));
                if(currentMonth != getNumberOfMonth(month) && (eventCalendar.getTimeInMillis() < calendar.getTimeInMillis() - 86400000))
                    year += 1;
                calendar.set(year, getNumberOfMonth(month), getNumberOfDay(day));
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.getTimeInMillis());
                startActivity(intent);
                return true;
            case R.id.share_item:
                intent = new Intent(ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, title);
                intent.putExtra(Intent.EXTRA_TEXT, EVENTS_DEV_BY_URL + href);
                startActivity(Intent.createChooser(intent, "Поделиться ссылкой"));
                return true;
            default:
                break;
        }
        return true;
    }
    public int getNumberOfMonth(String strMonth){
        switch (strMonth){
            case "Январь":
                return 0;
            case "Февраль":
                return 1;
            case "Март":
                return 2;
            case "Апрель":
                return 3;
            case "Май":
                return 4;
            case "Июнь":
                return 5;
            case "Июль":
                return 6;
            case "Август":
                return 7;
            case "Сентябрь":
                return 8;
            case "Октябрь":
                return 9;
            case "Ноябрь":
                return 10;
            case "Декабрь":
                return 11;
            default:
                break;
        }
        return 0;
    }

    public int getNumberOfDay(String strDay){
        return Integer.parseInt(strDay);
    }
}
