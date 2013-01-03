package com.realsimpleapps.desertcodecamp;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;

public class AboutActivity extends SherlockActivity {

	private static final String tag = "AboutActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
	}

	@Override
	public void onStart() {
		super.onStart();

		FlurryAgent.setUseHttps(true);
		FlurryAgent.setLogEnabled(false);
		FlurryAgent.setContinueSessionMillis(60000);
		FlurryAgent.onStartSession(this, getString(R.string.flurryAPIKey));
	}

	@Override
	public void onResume() {
		super.onResume();

		FlurryAgent.logEvent("About");
	}

	@Override
	public void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}
}
