package com.realsimpleapps.desertcodecamp;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import android.view.MenuItem;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.fragments.MySessionsListFragment;
import com.realsimpleapps.desertcodecamp.fragments.MySessionsListFragment.OnSessionSelectedListener;
import com.realsimpleapps.desertcodecamp.fragments.SessionDetailFragment;

public class MySessionsListActivity extends Activity implements OnSessionSelectedListener {

	private static final String tag = "MySessionsListActivity";

    private boolean sessionDetailVisible = false;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.my_sessions_list_fragment);

        hideSessionDetail(false);

        if (findViewById(R.id.my_sessions_list_fragment_container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			MySessionsListFragment firstFragment = new MySessionsListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction().add(R.id.my_sessions_list_fragment_container, firstFragment).commit();
		}
	}

    private void hideSessionDetail(Boolean hide) {
        if ((sessionDetailVisible == false || hide == true) && findViewById(R.id.my_sessions_list_fragment_container) == null) {
            SessionDetailFragment sessionDetailFragment = (SessionDetailFragment) getFragmentManager().findFragmentById(R.id.session_detail_fragment);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(sessionDetailFragment);
            ft.commit();
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
            transaction.replace(R.id.my_sessions_list_fragment_container, newSessionDetailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
		}
	}
}
