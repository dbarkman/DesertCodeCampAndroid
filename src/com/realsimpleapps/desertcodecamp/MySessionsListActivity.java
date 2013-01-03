package com.realsimpleapps.desertcodecamp;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.fragments.MySessionsListFragment;
import com.realsimpleapps.desertcodecamp.fragments.MySessionsListFragment.OnSessionSelectedListener;
import com.realsimpleapps.desertcodecamp.fragments.SessionDetailFragment;

public class MySessionsListActivity extends SherlockFragmentActivity implements OnSessionSelectedListener {

	private static final String tag = "MySessionsListActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.my_sessions_list);

		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			MySessionsListFragment firstFragment = new MySessionsListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();
		}
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
