package com.realsimpleapps.desertcodecamp.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.AboutActivity;
import com.realsimpleapps.desertcodecamp.FilterListActivity;
import com.realsimpleapps.desertcodecamp.MyScheduleListActivity;
import com.realsimpleapps.desertcodecamp.adapters.SectionedArrayAdapter;
import com.realsimpleapps.desertcodecamp.tasks.RestTask;

public class MySessionsListFragment extends SherlockListFragment {

	private static final String tag = "MySessionsListFragment";

	private SharedPreferences displayPreferences;
	private SharedPreferences.Editor displayPreferencesEditor;

	private ProgressDialog progress;
	private Map<String, ArrayList<String>> allSessionsArray;
	private Map<String, String> allFiltersMap, sessionStrings;
	private ArrayList<String> allFilters, sessions;
	private String login;
	private int filterType;

	private final String getMySessionsApiAction = "com.realsimpleapps.desertcodecamp.getMySessions";

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

		getActivity().registerReceiver(getMySessionsReceiver, new IntentFilter(getMySessionsApiAction));

		displayPreferences = getActivity().getSharedPreferences("displayPreferences", Context.MODE_PRIVATE);
		displayPreferencesEditor = displayPreferences.edit();

		login = displayPreferences.getString("login", "");
		if (login.length() > 0) {
			filterType = displayPreferences.getInt("mySessionsFilterType", 0);
			fetchSessions();
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

		switch (filterType) {
		case 0:
			FlurryAgent.logEvent("MyInterestedSessions");
			break;
		case 1:
			FlurryAgent.logEvent("MyPresentingSessions");
			break;
		}

		setHasOptionsMenu(true);

		getActivity().setTitle("My Sessions");

		if (login.length() <= 0) {
			sessions = new ArrayList<String>();
			String jsonString = "{\"filter\":\"\",\"session\":\"" + getActivity().getText(R.string.noLoginMessage) + "\"}";
			sessions.add(jsonString);

			SectionedArrayAdapter adapter = new SectionedArrayAdapter(getActivity(), R.layout.sectioned_list_item, sessions);
			setListAdapter(adapter);

			ListView lv = getListView();
			lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
			lv.setDividerHeight(1);
			lv.setEnabled(false);
			
			FlurryAgent.logEvent("No Username Set");
		} else {
			FlurryAgent.logEvent("Username Set");
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

		getActivity().unregisterReceiver(getMySessionsReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.my_sessions_list_menu, menu);

		MenuItem mi1 = menu.getItem(1);
		MenuItem mi2 = menu.getItem(2);
		MenuItem mi3 = menu.getItem(3);
		if (login.length() <= 0) {
			mi1.setEnabled(false);
			mi1.setVisible(false);
			mi3.setEnabled(false);
			mi3.setVisible(false);
		} else {
			mi2.setEnabled(false);
			mi2.setVisible(false);
			switch (filterType) {
			case 0:
				mi1.setTitle(getActivity().getText(R.string.presenting));
				break;
			case 1:
				mi1.setTitle(getActivity().getText(R.string.interested));
				break;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			FlurryAgent.logEvent("RefreshMySessions");
			fetchSessions();
			return true;
		case R.id.changeFilter:
			switch (filterType) {
			case 0:
				filterType = 1;
				displayPreferencesEditor.putInt("mySessionsFilterType", 1);
				item.setTitle(getActivity().getText(R.string.interested));
				FlurryAgent.logEvent("MyPresentingSessions");
				break;
			case 1:
				filterType = 0;
				displayPreferencesEditor.putInt("mySessionsFilterType", 0);
				item.setTitle(getActivity().getText(R.string.presenting));
				FlurryAgent.logEvent("MyInterestedSessions");
				break;
			}
			displayPreferencesEditor.commit();
			fetchSessions();
			return true;
		case R.id.enterLogin:
			getUsername();
			return true;
		case R.id.updateLogin:
			getUsername();
			return true;
		case R.id.allSessions:
			startActivity(new Intent(getActivity(), FilterListActivity.class));
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

	private void getUsername() {
		FlurryAgent.logEvent("Entering Username");
		
		final EditText input = new EditText(getActivity());
		input.requestFocus();
		input.setSingleLine();
		input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		//		input.setImeActionLabel("Save", EditorInfo.IME_ACTION_DONE);
		input.setText(login);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder
		.setTitle(null)
		.setMessage(getActivity().getText(R.string.enterLoginMessage))
		.setView(input)
		.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				login = input.getText().toString();
				displayPreferencesEditor.putString("login", login);
				displayPreferencesEditor.commit();

				Intent refreshIntent = getActivity().getIntent();
				getActivity().finish();
				startActivity(refreshIntent);
				
				FlurryAgent.logEvent("Username Entry Saved With Save");
				
				if (login.length() > 0) {
					FlurryAgent.logEvent("Username Saved");
				} else {
					FlurryAgent.logEvent("Blank Username Saved");
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				FlurryAgent.logEvent("Username Entry Canceled");
			}
		});

		builder.show();
	}

	private void fetchSessions() {
		String shortName = getActivity().getString(R.string.shortName);
		String myURI = "";
		switch (filterType) {
		case 0:
			myURI = "Session.svc/GetMyInterestedInSessionsByLogin?login=" + login + "&shortName=" + shortName;
			FlurryAgent.logEvent("FetchingMyInterestedSessions", true);
			break;
		case 1:
			myURI = "Session.svc/GetMyPresentationsByLogin?login=" + login + "&shortName=" + shortName;
			FlurryAgent.logEvent("FetchingMyPresentingSessions", true);
			break;
		}
		progress = ProgressDialog.show(getActivity(), null, "Fetching Sessions", true); 
		RestTask task = new RestTask(getActivity(), getMySessionsApiAction, myURI); 
		task.execute();
	}

	private BroadcastReceiver getMySessionsReceiver = new BroadcastReceiver() { 
		@Override 
		public void onReceive(Context context, Intent intent) {

			if (progress != null) {
				progress.dismiss();
				progress = null;
			}

			switch (filterType) {
			case 0:
				FlurryAgent.endTimedEvent("FetchingMyInterestedSessions");
				break;
			case 1:
				FlurryAgent.endTimedEvent("FetchingMyPresentingSessions");
				break;
			}

			String apiResult = intent.getStringExtra(RestTask.httpResponse);

			if (apiResult.equalsIgnoreCase("connectionFailed")) {
				String result = displayPreferences.getString("getMySessions", "");
				if (result.length() > 0) {
					updateTracks(result);
				} else {
					Toast.makeText(getActivity(), R.string.offlineAlert, Toast.LENGTH_LONG).show();
				}
			} else {
				String result = displayPreferences.getString("getMySessions", "");
				if (result.length() > 2) {
					updateTracks(result);
				} else {
					displayNoDataMessage();
				}
			}
		}
	};

	private void displayNoDataMessage() {
		String noDataMessage = "";
		switch (filterType) {
		case 0:
			noDataMessage = (String) getActivity().getText(R.string.noInterestedMessage);
			FlurryAgent.logEvent("No Interested Session Data");
			break;
		case 1:
			noDataMessage = (String) getActivity().getText(R.string.noPresentingMessage);
			FlurryAgent.logEvent("No Presenting Session Data");
			break;
		}
		sessions = new ArrayList<String>();
		String jsonString = "{\"filter\":\"\",\"session\":\"" + noDataMessage + "\"}";
		sessions.add(jsonString);

		SectionedArrayAdapter adapter = new SectionedArrayAdapter(getActivity(), R.layout.sectioned_list_item, sessions);
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
		lv.setDividerHeight(1);
		lv.setEnabled(false);
	}

	private void updateTracks(String apiResult) {
		try {
			JSONArray sessionsArray = (new JSONArray(apiResult));

			allSessionsArray = new HashMap<String, ArrayList<String>>();
			allFiltersMap = new HashMap<String, String>();
			allFilters = new ArrayList<String>();
			sessions = new ArrayList<String>();

			sessionStrings = new HashMap<String, String>();

			int sessionsCount = sessionsArray.length();
			for (int i = 0; i < sessionsCount; i++) {

				if (sessionsArray.getJSONObject(i).getString("IsApproved").equalsIgnoreCase("false")) continue; 

				JSONObject sessionData = sessionsArray.getJSONObject(i);

				sessionStrings.put(sessionData.getString("Name"), sessionData.toString());

				Map<String, String> flurryParams = new HashMap<String, String>();

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
					
					flurryParams.put("count", Integer.toString(sessionsCount));
					FlurryAgent.logEvent("PresentingSessionsCount", flurryParams, false);
					break;
				default:
					filter = sessionsArray.getJSONObject(i).getJSONObject("Track").getString("Name");
					filterKey = sessionsArray.getJSONObject(i).getJSONObject("Track").getString("Name");

					flurryParams.put("count", Integer.toString(sessionsCount));
					FlurryAgent.logEvent("InterestedSessionsCount", flurryParams, false);
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

			try {
				ListView lv = getListView();
				lv.setDivider(new ColorDrawable(this.getResources().getColor(R.color.darkGray)));
				lv.setDividerHeight(1);
				lv.setEnabled(true);
			} catch (IllegalStateException ise) {
				FlurryAgent.logEvent("MySessions ListView Exception");
				Log.e(tag, "Couldn't edit listview: " + ise.getMessage());
				problemWithListViewAlert();
			}

		} catch (JSONException je) {
			Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
			
		}
	}

	private void problemWithListViewAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder
		.setTitle(null)
		.setMessage(R.string.problemWithListViewAlertMessage)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		builder.show();
	}
}

