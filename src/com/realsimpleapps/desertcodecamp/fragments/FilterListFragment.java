package com.realsimpleapps.desertcodecamp.fragments;

import android.app.ProgressDialog;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.AboutActivity;
import com.realsimpleapps.desertcodecamp.FilteredSessionsListActivity;
import com.realsimpleapps.desertcodecamp.MyScheduleListActivity;
import com.realsimpleapps.desertcodecamp.MySessionsListActivity;
import com.realsimpleapps.desertcodecamp.tasks.RestTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FilterListFragment extends SherlockListFragment {

	private static final String tag = "FilterListFragment";

	private SharedPreferences displayPreferences;
	private SharedPreferences.Editor displayPreferencesEditor;

	private ProgressDialog progress;
	private Map<String, ArrayList<String>> allSessionsArray;
	private Map<String, String> allFiltersMap;
	private ArrayList<String> allFilters;
	private String[] filters;
	private int filterType;

	private final String getAllSessionsApiAction = "com.realsimpleapps.desertcodecamp.getAllSessions";

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fetchSessions();
		}
	};

	private void fetchSessions() {
		progress = ProgressDialog.show(getActivity(), null, "Fetching Sessions", true); 
		String campId = getActivity().getString(R.string.campId);
		RestTask task = new RestTask(getActivity(), getAllSessionsApiAction, "Session.svc/GetSessionsByCampId?campId=" + campId); 
		task.execute();
		FlurryAgent.logEvent("FetchingAllSessions", true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActivity().registerReceiver(getAllSessionsReceiver, new IntentFilter(getAllSessionsApiAction));

		fetchSessions();

		displayPreferences = getActivity().getSharedPreferences("displayPreferences", Context.MODE_PRIVATE);
		displayPreferencesEditor = displayPreferences.edit();

		filterType = displayPreferences.getInt("allSessionsFilterType", 0);
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

		getActivity().setTitle("Desert Code Camp");

		getActivity().registerReceiver(refreshReceiver, new IntentFilter("com.realsimpleapps.desertcodecamp.allSessionRefresh"));
		
		switch (filterType) {
		case 0:
			FlurryAgent.logEvent("AllTracks");
			break;
		case 1:
			FlurryAgent.logEvent("AllTimes");
			break;
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(getActivity());
	}

	@Override 
	public void onDestroy() { 
		super.onDestroy();

		getActivity().unregisterReceiver(refreshReceiver);
		getActivity().unregisterReceiver(getAllSessionsReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.filter_list_menu, menu);

		MenuItem mi1 = menu.getItem(1);
		switch (filterType) {
		case 0:
			mi1.setTitle(getActivity().getText(R.string.times));
			break;
		case 1:
			mi1.setTitle(getActivity().getText(R.string.tracks));
			break;
		}
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
		case R.id.changeFilter:
			switch (filterType) {
			case 0:
				filterType = 1;
				displayPreferencesEditor.putInt("allSessionsFilterType", 1);
				item.setTitle(getActivity().getText(R.string.tracks));
				FlurryAgent.logEvent("AllTimes");
				break;
			case 1:
				filterType = 0;
				displayPreferencesEditor.putInt("allSessionsFilterType", 0);
				item.setTitle(getActivity().getText(R.string.times));
				FlurryAgent.logEvent("AllTracks");
				break;
			}
			displayPreferencesEditor.commit();
			Intent changeFilterIntent = new Intent();
			changeFilterIntent.setAction("com.realsimpleapps.desertcodecamp.allSessionRefresh");
			getActivity().sendBroadcast(changeFilterIntent);
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

		String filteredName = filters[position];
		ArrayList<String> filteredSessions = allSessionsArray.get(filteredName);
		int sessionCount = filteredSessions.size();

		Intent allSessionsListIntent = new Intent(getActivity(), FilteredSessionsListActivity.class);
		allSessionsListIntent.putExtra("allSessionsFilterType", filterType);
		allSessionsListIntent.putExtra("sessionCount", sessionCount);
		allSessionsListIntent.putExtra("filteredName", filteredName);
		allSessionsListIntent.putExtra("filteredSessions", filteredSessions);
		startActivity(allSessionsListIntent);
	}

	private BroadcastReceiver getAllSessionsReceiver = new BroadcastReceiver() { 
		@Override 
		public void onReceive(Context context, Intent intent) {

			if (progress != null) {
				progress.dismiss();
				progress = null;
			}

			FlurryAgent.endTimedEvent("FetchingAllSessions");

			String apiResult = intent.getStringExtra(RestTask.httpResponse);

			if (apiResult.equalsIgnoreCase("connectionFailed")) {
				String result = displayPreferences.getString("getAllSessions", "");
				if (result.length() > 0) {
					updateTracks(result);
				} else {
					Toast.makeText(getActivity(), R.string.offlineAlert, Toast.LENGTH_LONG).show();
				}
			} else {
				String result = displayPreferences.getString("getAllSessions", "");
				updateTracks(result);
			}
		}
	};

	private void updateTracks(String apiResult) {
		try {
			JSONArray sessionsArray = (new JSONArray(apiResult));

			allSessionsArray = new HashMap<String, ArrayList<String>>();
			allFiltersMap = new HashMap<String, String>();
			allFilters = new ArrayList<String>();

			int sessionsCount = sessionsArray.length();
			for (int i = 0; i < sessionsCount; i++) {

				if (sessionsArray.getJSONObject(i).getString("IsApproved").equalsIgnoreCase("false")) continue; 

				JSONObject sessionData = sessionsArray.getJSONObject(i);

				String filterKey;
				String filter;
				switch (filterType) {
				case 1:
					try {
						filter = sessionsArray.getJSONObject(i).getJSONObject("Time").getString("Name");
						filterKey = sessionsArray.getJSONObject(i).getJSONObject("Time").getString("StartDate").substring(6, 16);
					} catch (JSONException e) {
						filter = "Not Scheduled";
						filterKey = "Not Scheduled";
					}
					break;
				default:
					filter = sessionsArray.getJSONObject(i).getJSONObject("Track").getString("Name");
					filterKey = sessionsArray.getJSONObject(i).getJSONObject("Track").getString("Name");
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
			}

			Collections.sort(allFilters);

			int allFiltersArrayCount = allFilters.size();
			filters = new String[allFiltersArrayCount];

			int count = 0;
			for (String str : allFilters) {
				filters[count++] = allFiltersMap.get(str);
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.row_layout, R.id.label, filters);
			setListAdapter(adapter);

			ListView lv = getListView();
			lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
			lv.setDividerHeight(1);
		} catch (JSONException je) {
			Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
			Toast.makeText(getActivity(), R.string.offlineAlert, Toast.LENGTH_LONG).show();
		}
	}
}
