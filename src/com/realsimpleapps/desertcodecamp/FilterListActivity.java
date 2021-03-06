package com.realsimpleapps.desertcodecamp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.MenuItem;
import com.realsimpleapps.desert.code.camp.R;
import com.realsimpleapps.desertcodecamp.fragments.FilterListFragment;

public class FilterListActivity extends Activity {

	private static final String tag = "FilterListActivity";

	private SharedPreferences displayPreferences;
	private SharedPreferences.Editor displayPreferencesEditor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filter_list);

		displayPreferences = getSharedPreferences("displayPreferences", MODE_PRIVATE);
		displayPreferencesEditor = displayPreferences.edit();
		displayPreferencesEditor.putInt("allSessionsFilterType", 0);
		displayPreferencesEditor.commit();

		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) {
				return; 
			}
			FilterListFragment firstFragment = new FilterListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();
		}
	}
}
