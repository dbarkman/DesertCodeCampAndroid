package com.realsimpleapps.desertcodecamp.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.AboutActivity;
import com.realsimpleapps.desertcodecamp.FilterListActivity;
import com.realsimpleapps.desertcodecamp.MySessionsListActivity;
import com.realsimpleapps.desertcodecamp.adapters.SectionedArrayAdapter;

public class MyScheduleListFragment extends ListFragment {

	private static final String tag = "MyScheduleListFragment";

	private SharedPreferences displayPreferences;
	private SharedPreferences.Editor displayPreferencesEditor;

	private SectionedArrayAdapter adapter;

	private Map<String, ArrayList<String>> allSessionsArray;
	private Map<String, String> allFiltersMap, sessionStrings;
	private ArrayList<String> allFilters, sessions, sessionNames;

	private String mySchedule;

	OnSessionSelectedListener sessionSelectedCallback;

	public interface OnSessionSelectedListener {
		public void onSessionSelected(String sessionString);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			sessionSelectedCallback = (OnSessionSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnSessionSelectedListener");
		}

		displayPreferences = getActivity().getSharedPreferences("displayPreferences", Context.MODE_PRIVATE);
		displayPreferencesEditor = displayPreferences.edit();
		mySchedule = displayPreferences.getString("mySchedule", "");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getActivity().getActionBar().setDisplayShowHomeEnabled(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		FlurryAgent.setUseHttps(true);
		FlurryAgent.setLogEnabled(false);
		FlurryAgent.setContinueSessionMillis(60000);
		FlurryAgent.onStartSession(getActivity(), getString(R.string.flurryAPIKey));
	}

	@Override
	public void onResume() {
		super.onResume();

		setHasOptionsMenu(true);

		getActivity().setTitle("My Schedule");

		mySchedule = displayPreferences.getString("mySchedule", "");

		sessionNames = new ArrayList<String>(); 

		if (mySchedule.length() <= 0) {
			sessions = new ArrayList<String>();
			String jsonString = "{\"filter\":\"\",\"session\":\"" + getActivity().getText(R.string.noScheduleMessage) + "\"}";
			sessions.add(jsonString);

			SectionedArrayAdapter adapter = new SectionedArrayAdapter(getActivity(), R.layout.sectioned_list_item, sessions);
			setListAdapter(adapter);

			ListView lv = getListView();
			lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
			lv.setDividerHeight(1);
			lv.setEnabled(false);
			
			FlurryAgent.logEvent("MyScheduleEmpty");
		} else {
			try {
				JSONArray sessionsArray = (new JSONArray(mySchedule));

				allSessionsArray = new HashMap<String, ArrayList<String>>();
				allFiltersMap = new HashMap<String, String>();
				allFilters = new ArrayList<String>();
				sessions = new ArrayList<String>();

				sessionStrings = new HashMap<String, String>();

				int sessionsCount = sessionsArray.length();
				for (int i = 0; i < sessionsCount; i++) {

					JSONObject sessionData = sessionsArray.getJSONObject(i);

					sessionStrings.put(sessionData.getString("Name"), sessionData.toString());

					String filterKey;
					String filter;
					try {
						filter = sessionsArray.getJSONObject(i).getJSONObject("Time").getString("Name");
						filterKey = sessionsArray.getJSONObject(i).getJSONObject("Time").getString("StartDate").substring(6, 16);
					} catch (JSONException e) {
						filter = "Not Scheduled";
						filterKey = "Not Scheduled";
					}
					if (!allSessionsArray.containsKey(filter)) {
						ArrayList<String> tempArrayList = new ArrayList<String>();
						tempArrayList.add(sessionData.toString());
						allSessionsArray.put(filter, tempArrayList);
						allFiltersMap.put(filterKey, filter);
						allFilters.add(filterKey);
					} else {
						ArrayList<String> tempArrayList = allSessionsArray.get(filter);
						allSessionsArray.remove(filter);
						tempArrayList.add(sessionData.toString());
						allSessionsArray.put(filter, tempArrayList);
					}
				}

				Collections.sort(allFilters);

				for (String filter : allFilters) {
					ArrayList<String> tempArrayList = allSessionsArray.get(allFiltersMap.get(filter));
					int arrayListCount = tempArrayList.size();
					for (int i = 0; i < arrayListCount; i++) {
						try {
							JSONObject sessionData = (new JSONObject(tempArrayList.get(i)));
							String jsonString = "{\"filter\":\"" + allFiltersMap.get(filter) + "\",\"session\":\"" + sessionData.getString("Name") + "\"}";
							sessions.add(jsonString);
							sessionNames.add(sessionData.getString("Name"));
						} catch (JSONException e) {
							Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
						}
					}
				}

				adapter = new SectionedArrayAdapter(getActivity(), R.layout.sectioned_list_item, sessions);
				setListAdapter(adapter);

				ListView lv = getListView();
				lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
				lv.setDividerHeight(1);
				lv.setEnabled(true);

				getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
						promptBeforeSessionDelete(position);
						return true;
					}
				});
			} catch (JSONException je) {
				Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
			}
			
			FlurryAgent.logEvent("MySchedule");
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		String jsonArray = "[";
		int sessionsCount = sessionNames.size();
		for (int i = 0; i < sessionsCount; i++) {
			if (i > 0) jsonArray += ",";
			jsonArray += sessionStrings.get(sessionNames.get(i));
		}
		jsonArray += "]";

		if (sessionsCount > 0) {
			displayPreferencesEditor.putString("mySchedule", jsonArray);
		} else {
			displayPreferencesEditor.putString("mySchedule", "");
		}
		displayPreferencesEditor.commit();
	}

	@Override
	public void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(getActivity());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.my_schedule_list_menu, menu);

		MenuItem mi1 = menu.getItem(0);
		if (mySchedule.length() <= 0) {
			mi1.setEnabled(false);
			mi1.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.removeSessions:
                howToRemoveSessions();
                return true;
            case R.id.allSessions:
                startActivity(new Intent(getActivity(), FilterListActivity.class));
                return true;
            case R.id.mySessions:
                startActivity(new Intent(getActivity(), MySessionsListActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
		}
		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		String session;
		try {
			JSONObject sessionData = (new JSONObject(sessions.get(position)));
			session = sessionData.getString("session");
		} catch (JSONException e) {
			Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
			session = "";
		}

		String sessionString = sessionStrings.get(session);
		sessionSelectedCallback.onSessionSelected(sessionString);
	}

	private void howToRemoveSessions() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder
		.setTitle(null)
		.setMessage(R.string.removeSessionsMessage)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}

	private void promptBeforeSessionDelete(int pos) {
		final int position = pos;
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder
		.setTitle(null)
		.setMessage(R.string.removeSessionsPrompt)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				adapter.remove(adapter.getItem(position));
				adapter.notifyDataSetChanged();
				sessionNames.remove(position);
				
				FlurryAgent.logEvent("SessionRemovedFromSchedule");
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}
}
