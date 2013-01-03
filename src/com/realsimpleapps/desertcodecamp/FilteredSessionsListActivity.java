package com.realsimpleapps.desertcodecamp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.fragments.FilteredSessionsListFragment;
import com.realsimpleapps.desertcodecamp.fragments.FilteredSessionsListFragment.OnSessionSelectedListener;
import com.realsimpleapps.desertcodecamp.fragments.SessionDetailFragment;

public class FilteredSessionsListActivity extends SherlockFragmentActivity implements OnSessionSelectedListener {

	private static final String tag = "FilteredSessionsListActivity";

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filtered_sessions_list);

		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			FilteredSessionsListFragment firstFragment = new FilteredSessionsListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		registerReceiver(refreshReceiver, new IntentFilter("com.realsimpleapps.desertcodecamp.allSessionRefresh"));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(refreshReceiver);
	}

	@Override
	public void onSessionSelected(String sessionString) {
		SessionDetailFragment sessionDetailFrag = (SessionDetailFragment) getSupportFragmentManager().findFragmentById(R.id.session_detail_fragment);

		if (sessionDetailFrag != null) {
			sessionDetailFrag.updateSessionView(sessionString);
		} else {
			SessionDetailFragment newFragment = new SessionDetailFragment();
			Bundle args = new Bundle();
			args.putBoolean("myScheduleIsParent", false);
			args.putString("sessionString", sessionString);
			newFragment.setArguments(args);

			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
			ft.replace(R.id.fragment_container, newFragment);
			ft.addToBackStack(null);
			ft.commit();
		}
	}
}
