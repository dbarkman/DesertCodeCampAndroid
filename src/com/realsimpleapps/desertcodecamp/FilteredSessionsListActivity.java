package com.realsimpleapps.desertcodecamp;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.view.MenuItem;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.fragments.FilteredSessionsListFragment;
import com.realsimpleapps.desertcodecamp.fragments.FilteredSessionsListFragment.OnSessionSelectedListener;
import com.realsimpleapps.desertcodecamp.fragments.SessionDetailFragment;

public class FilteredSessionsListActivity extends Activity implements OnSessionSelectedListener {

	private static final String tag = "FilteredSessionsListActivity";

    private boolean sessionDetailVisible = false;

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.filtered_sessions_list_fragment);

        hideSessionDetail(false);

		if (findViewById(R.id.filtered_sessions_list_fragment_container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			FilteredSessionsListFragment firstFragment = new FilteredSessionsListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction().add(R.id.filtered_sessions_list_fragment_container, firstFragment).commit();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		registerReceiver(refreshReceiver, new IntentFilter("com.realsimpleapps.desertcodecamp.allSessionRefresh"));
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
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(refreshReceiver);
	}

    private void hideSessionDetail(Boolean hide) {
        if ((sessionDetailVisible == false || hide == true) && findViewById(R.id.filtered_sessions_list_fragment_container) == null) {
            SessionDetailFragment sessionDetailFragment = (SessionDetailFragment) getFragmentManager().findFragmentById(R.id.session_detail_fragment);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.hide(sessionDetailFragment);
            transaction.commit();
        }
    }

    @Override
	public void onSessionSelected(String sessionString) {
		SessionDetailFragment sessionDetailFragment = (SessionDetailFragment) getFragmentManager().findFragmentById(R.id.session_detail_fragment);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (sessionDetailFragment != null) {
            sessionDetailFragment.myScheduleIsParent = false;
			sessionDetailFragment.updateSessionView(sessionString);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.show(sessionDetailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            sessionDetailVisible = true;
		} else {
			SessionDetailFragment newSessionDetailFragment = new SessionDetailFragment();
            Bundle args = new Bundle();
            args.putBoolean("myScheduleIsParent", false);
            args.putString("sessionString", sessionString);
            newSessionDetailFragment.setArguments(args);

            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.filtered_sessions_list_fragment_container, newSessionDetailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
		}
	}
}
