package com.realsimpleapps.desertcodecamp.fragments;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.FilterListActivity;
import com.realsimpleapps.desertcodecamp.MyScheduleListActivity;
import com.realsimpleapps.desertcodecamp.MySessionsListActivity;

public class SessionDetailFragment extends SherlockFragment {

	private static final String tag = "SessionDetailFragment";

	private SharedPreferences displayPreferences;
	private SharedPreferences.Editor displayPreferencesEditor;

	private boolean myScheduleIsParent;

	private String sessionString;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			sessionString = savedInstanceState.getString("sessionString");
		}
		return inflater.inflate(R.layout.session_detail, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		displayPreferences = getActivity().getSharedPreferences("displayPreferences", Context.MODE_PRIVATE);
		displayPreferencesEditor = displayPreferences.edit();
	}

	@Override
	public void onStart() {
		super.onStart();

		Bundle args = getArguments();
		if (args != null) {
			myScheduleIsParent = args.getBoolean("myScheduleIsParent");
			sessionString = args.getString("sessionString");
			updateSessionView(sessionString);
		} else if (sessionString != null) {
			updateSessionView(sessionString);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.session_detail_menu, menu);

		MenuItem mi0 = menu.getItem(0);
		if (myScheduleIsParent == true) {
			mi0.setEnabled(false);
			mi0.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.addSession:
			addSessionToMySchedule();
			return true;
		case R.id.allSessions:
			startActivity(new Intent(getActivity(), FilterListActivity.class));
			return true;
		case R.id.mySessions:
			startActivity(new Intent(getActivity(), MySessionsListActivity.class));
			return true;
		case R.id.mySchedule:
			startActivity(new Intent(getActivity(), MyScheduleListActivity.class));
			return true;
		}
		return false;
	}

	private void addSessionToMySchedule() {
		String sessionName = "";
		try {
			JSONObject sessionObject = (new JSONObject(sessionString));
			sessionName = sessionObject.getString("Name");
		} catch (JSONException je) {
			Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
		}
		try {
			String mySchedule = displayPreferences.getString("mySchedule", "");
			String jsonArray = "";
			if (mySchedule.length() <= 0) {
				jsonArray = "[" + sessionString + "]";
				completeAddSession(jsonArray, sessionName);
			} else {
				if (!mySchedule.contains(sessionName)) {
					jsonArray = "[" + sessionString + "," + mySchedule.substring(1);
					completeAddSession(jsonArray, sessionName);
				}
			}
		} catch (Exception e) {
			Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
			Toast.makeText(getActivity(), "Session Could Not Added", Toast.LENGTH_LONG).show();
		}
	}

	private void completeAddSession(String jsonArray, String name) {
		displayPreferencesEditor.putString("mySchedule", jsonArray);
		displayPreferencesEditor.commit();
		Toast.makeText(getActivity(), "Session Added", Toast.LENGTH_SHORT).show();

		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("sessionName", name);
		FlurryAgent.logEvent("SessionAddedToSchedule", flurryParams, false);
	}

	public void updateSessionView(String sessionString) {

		this.sessionString = sessionString;
		String name = "";
		String sessionAbstract = "";
		String presenters = "";
		String time = "";
		String room = "";

		try {
			JSONObject sessionObject = (new JSONObject(sessionString));
			name = sessionObject.getString("Name");
			sessionAbstract = sessionObject.getString("Abstract");
			try {
				time = sessionObject.getJSONObject("Time").getString("Name");
			} catch (JSONException e) {
				time = getActivity().getString(R.string.tbd);
			}
			try {
				room = sessionObject.getJSONObject("Room").getString("Name");
			} catch (JSONException e) {
				room = getActivity().getString(R.string.tbd);
			}

			JSONArray presenterArray = sessionObject.getJSONArray("Presenters");
			int presentersArrayLength = presenterArray.length();
			for (int i = 0; i < presentersArrayLength; i++) {
				if (i > 0) presenters += "\n\n";
				JSONObject presenterObject = presenterArray.getJSONObject(i);

				String firstName = (presenterObject.getString("FirstName").equals("null")) ? "" : presenterObject.getString("FirstName");
				String lastName = (presenterObject.getString("LastName").equals("null")) ? "" : presenterObject.getString("LastName");
				String space = (firstName.length() > 0 && lastName.length() > 0) ? " " : ""; 
				String presenterName = firstName + space + lastName;

				String email = (presenterObject.getString("Email").equals("null")) ? "" : presenterObject.getString("Email");
				String twitter = (presenterObject.getString("TwitterHandle").equals("null") || presenterObject.getString("TwitterHandle").equals("@")) ? "" : "Twitter: " + presenterObject.getString("TwitterHandle");

				String presenter = "";
				presenter += (presenterName.length() > 0) ? presenterName : "";
				presenter += (presenter.length() > 0 && email.length() > 0) ? "\n" : "";
				presenter += (email.length() > 0) ? email : "";
				presenter += (presenter.length() > 0 && twitter.length() > 0) ? "\n" : "";
				presenter += (twitter.length() > 0) ? twitter : "";

				presenters += presenter;
			}
		} catch (JSONException je) {
			Log.e(tag, "Couldn't parse JSON result: " + je.getMessage());
		}

		TextView nameTextView = (TextView) getView().findViewById(R.id.name);
		nameTextView.setText(name);

		TextView abstractTextView = (TextView) getView().findViewById(R.id.sessionAbstract);
		abstractTextView.setText(sessionAbstract);

		TextView presentersLabelTextView = (TextView) getView().findViewById(R.id.presentersLabel);
		presentersLabelTextView.setText(R.string.sessionPresenters);

		TextView presentersTextView = (TextView) getView().findViewById(R.id.presenters);
		presentersTextView.setText(presenters);

		TextView roomLabelTextView = (TextView) getView().findViewById(R.id.roomLabel);
		roomLabelTextView.setText(R.string.sessionRoom);

		TextView roomTextView = (TextView) getView().findViewById(R.id.room);
		roomTextView.setText(room);

		TextView timeLabelTextView = (TextView) getView().findViewById(R.id.timeLabel);
		timeLabelTextView.setText(R.string.sessionTime);

		TextView timeTextView = (TextView) getView().findViewById(R.id.time);
		timeTextView.setText(time);

		Map<String, String> flurryParams = new HashMap<String, String>();
		flurryParams.put("sessionName", name);
		if (myScheduleIsParent == true) {
			FlurryAgent.logEvent("SessionDetailFromMySchedule", flurryParams, false);
		} else {
			FlurryAgent.logEvent("SessionDetailFromSessionList", flurryParams, false);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("sessionString", sessionString);
	}
}
