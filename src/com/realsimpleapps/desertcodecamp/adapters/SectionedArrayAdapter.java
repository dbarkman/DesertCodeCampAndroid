package com.realsimpleapps.desertcodecamp.adapters;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.realsimpleapps.desert.code.camp.R;

public class SectionedArrayAdapter extends ArrayAdapter<String> {

	private static final String tag = "SectionedArrayAdapter";

	private Context context;
	private int layoutResourceId;
	private ArrayList<String> sessions;

	private static final int STATE_UNKNOWN = 0;
	private static final int STATE_SECTIONED_CELL = 1;
	private static final int STATE_REGULAR_CELL = 2;

	private int[] rowStates;

	public SectionedArrayAdapter(Context context, int layoutResourceId, ArrayList<String> sessions) {
		super(context, layoutResourceId, sessions);
		this.context = context;
		this.layoutResourceId = layoutResourceId;
		this.sessions = sessions;

		this.rowStates = new int[sessions.size()];
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		rowStates = new int[sessions.size()];
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		StringHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new StringHolder();
			holder.separator = (TextView)row.findViewById(R.id.separator);
			holder.txtTitle = (TextView)row.findViewById(R.id.txtTitle);

			if (position == 2) holder.separator.setVisibility(View.GONE);

			row.setTag(holder);
		} else {
			holder = (StringHolder)row.getTag();
		}

		String filter;
		String session;
		try {
			JSONObject sessionData = (new JSONObject(sessions.get(position)));
			filter = sessionData.getString("filter");
			session = sessionData.getString("session");
		} catch (JSONException e) {
			Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
			filter = "";
			session = "";
		}

		boolean needSeparator = false;

		switch (rowStates[position]) {
		case STATE_SECTIONED_CELL:
			needSeparator = true;
			break;

		case STATE_REGULAR_CELL:
			needSeparator = false;
			break;

		case STATE_UNKNOWN:
		default:
			if (position == 0) {
				needSeparator = true;
			} else {
				String lastFilter;
				try {
					JSONObject sessionData = (new JSONObject(sessions.get(position - 1)));
					lastFilter = sessionData.getString("filter");
				} catch (JSONException e) {
					Log.e(tag, "Couldn't parse JSON result: " + e.getMessage());
					lastFilter = "";
				}
				if (!filter.equals(lastFilter)) needSeparator = true;
			}

			rowStates[position] = (needSeparator) ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
			break;
		}

		if (needSeparator) {
			holder.separator.setVisibility(View.VISIBLE);
			holder.separator.setText(filter);
		} else {
			holder.separator.setVisibility(View.GONE);
		}

		holder.txtTitle.setText(session);

		return row;
	}

	static class StringHolder {
		private TextView separator;
		private TextView txtTitle;
	}
}
