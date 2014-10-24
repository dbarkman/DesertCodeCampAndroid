package com.realsimpleapps.desertcodecamp;

import android.app.Activity;
import android.os.Bundle;

import android.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.realsimpleapps.desert.code.camp.R;

public class AboutActivity extends Activity {

	private static final String tag = "AboutActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

	@Override
	public void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}
}
