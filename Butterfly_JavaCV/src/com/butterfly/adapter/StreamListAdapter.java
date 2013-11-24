package com.butterfly.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.butterfly.R;
import com.butterfly.StreamList.Stream;

public class StreamListAdapter extends ArrayAdapter<Stream> {
	private final Activity context;

	static class ViewHolder {
		public TextView textStreamName;
		public TextView textStreamViewerCount;
	}

	public StreamListAdapter(Activity context) {
		super(context, R.layout.stream_list_item);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.stream_list_item, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.textStreamName = (TextView) rowView
					.findViewById(R.id.stream_name);
			viewHolder.textStreamViewerCount = (TextView) rowView
					.findViewById(R.id.stream_viewer_count);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		Stream s = getItem(position);
		holder.textStreamName.setText(s.name);

		if (s.viewerCount > 0) {
			holder.textStreamViewerCount.setText(context.getResources()
					.getQuantityString(R.plurals.viewers_count, s.viewerCount,
							s.viewerCount));
			holder.textStreamViewerCount.setVisibility(View.VISIBLE);
		} else {
			holder.textStreamViewerCount.setVisibility(View.INVISIBLE);
		}

		return rowView;
	}
}