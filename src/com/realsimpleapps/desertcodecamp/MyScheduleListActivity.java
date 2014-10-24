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
import com.realsimpleapps.desertcodecamp.fragments.MyScheduleListFragment;
import com.realsimpleapps.desertcodecamp.fragments.MyScheduleListFragment.OnSessionSelectedListener;
import com.realsimpleapps.desertcodecamp.fragments.SessionDetailFragment;

public class MyScheduleListActivity extends Activity implements OnSessionSelectedListener {

	private static final String tag = "MyScheduleListActivity";

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

        setContentView(R.layout.my_schedule_list_fragment);

        hideSessionDetail(false);

        if (findViewById(R.id.my_schedule_list_fragment_container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			MyScheduleListFragment firstFragment = new MyScheduleListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction().add(R.id.my_schedule_list_fragment_container, firstFragment).commit();
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

    private void hideSessionDetail(Boolean hide) {
        if ((sessionDetailVisible == false || hide == true) && findViewById(R.id.my_schedule_list_fragment_container) == null) {
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
            sessionDetailFragment.myScheduleIsParent = true;
            sessionDetailFragment.updateSessionView(sessionString);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.show(sessionDetailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            sessionDetailVisible = true;
		} else {
            SessionDetailFragment newSessionDetailFragment = new SessionDetailFragment();
            Bundle args = new Bundle();
            args.putBoolean("myScheduleIsParent", true);
            args.putString("sessionString", sessionString);
            newSessionDetailFragment.setArguments(args);

            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.my_schedule_list_fragment_container, newSessionDetailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
		}
	}
}
