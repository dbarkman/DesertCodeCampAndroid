package com.realsimpleapps.desertcodecamp.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.AboutActivity;
import com.realsimpleapps.desertcodecamp.MyScheduleListActivity;
import com.realsimpleapps.desertcodecamp.MySessionsListActivity;
import com.realsimpleapps.desertcodecamp.adapters.SectionedArrayAdapter;

public class FilteredSessionsListFragment extends SherlockListFragment {

	private static final String tag = "FilteredSessionListFragment";

	private Map<String, ArrayList<String>> allSessionsArray;
	private Map<String, String> allFiltersMap, sessionStrings;
	private ArrayList<String> filteredSessions, allFilters, sessions;
	private String filteredName;
	private int filterType;

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
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			filterType = extras.getInt("allSessionsFilterType");
			filteredName = extras.getString("filteredName");
			filteredSessions = extras.getStringArrayList("filteredSessions");
		}
		
		Map<String, String> flurryParams = new HashMap<String, String>();
		switch (filterType) {
		case 0:
			flurryParams.put("for", filteredName);
			FlurryAgent.logEvent("SessionsListForTrackByTime", flurryParams, false);
			break;
		case 1:
			flurryParams.put("for", filteredName);
			FlurryAgent.logEvent("SessionsListForTimeByTrack", flurryParams, false);
			break;
		}
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

		getActivity().setTitle(filteredName);

		allSessionsArray = new HashMap<String, ArrayList<String>>();
		allFiltersMap = new HashMap<String, String>();
		allFilters = new ArrayList<String>();
		sessions = new ArrayList<String>();

		sessionStrings = new HashMap<String, String>();

		int sessionsCount = filteredSessions.size();
		for (int i = 0; i < sessionsCount; i++) {

			try {
				JSONObject sessionData = (new JSONObject(filteredSessions.get(i)));

				sessionStrings.put(sessionData.getString("Name"), sessionData.toString());

				String filterKey;
				String filter;
				switch (filterType) {
				case 0:
					try {
						filter = sessionData.getJSONObject("Time").getString("Name");
						filterKey = sessionData.getJSONObject("Time").getString("StartDate").substring(6, 16);
					} catch (JSONException e) {
						filter = "Not Scheduled";
						filterKey = "Not Scheduled";
					}
					break;
				default:
					filter = sessionData.getJSONObject("Track").getString("Name");
					filterKey = sessionData.getJSONObject("Track").getString("Name");
					break;
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
			} catch (JSONException je) {
				Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
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
				} catch (JSONException e) {
					Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
				}
			}
		}

		SectionedArrayAdapter adapter = new SectionedArrayAdapter(getActivity(), R.layout.sectioned_list_item, sessions);
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
		lv.setDividerHeight(1);
	}

	@Override
	public void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(getActivity());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.filtered_sessions_list_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			FlurryAgent.logEvent("RefreshAllSessions");
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction("com.realsimpleapps.desertcodecamp.allSessionRefresh");
			getActivity().sendBroadcast(broadcastIntent);
			return true;
		case R.id.mySessions:
			startActivity(new Intent(getActivity(), MySessionsListActivity.class));
			return true;
		case R.id.mySchedule:
			startActivity(new Intent(getActivity(), MyScheduleListActivity.class));
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
}

