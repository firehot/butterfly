package com.butterfly.adapter;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.view.VerticalProgressBar;
import com.loopj.android.image.SmartImageView;

public class StreamListAdapter extends ArrayAdapter<Stream> {

	private final StreamListFragment fragment;

	static class ViewHolder {
		public TextView textStreamName;
		public TextView textStreamViewerCount;
		public SmartImageView imageView;
		public TextView liveNowView;
		public Button overflowButton;
		public ImageView privacyImage;
		public TextView publishedTime;
		public VerticalProgressBar progressBar;
	}

	public StreamListAdapter(StreamListFragment fragment) {
		super(fragment.getActivity(), R.layout.stream_list_item);
		this.fragment = fragment;		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = this.fragment.getActivity().getLayoutInflater();
			rowView = inflater.inflate(R.layout.stream_list_item, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.textStreamName = (TextView) rowView
					.findViewById(R.id.stream_name);
			viewHolder.textStreamViewerCount = (TextView) rowView
					.findViewById(R.id.stream_viewer_count);
			viewHolder.imageView = (SmartImageView) rowView
					.findViewById(R.id.stream_image);
			viewHolder.progressBar = (VerticalProgressBar)rowView.findViewById(R.id.verticalProgressBar);
			viewHolder.liveNowView = (TextView) rowView.findViewById(R.id.live_now);
			viewHolder.privacyImage = (ImageView) rowView.findViewById(R.id.privacy_image);
			viewHolder.overflowButton = (Button) rowView.findViewById(R.id.streamOverflow);
			viewHolder.publishedTime = (TextView) rowView.findViewById(R.id.publish_time);
			viewHolder.overflowButton.setOnClickListener(this.fragment);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		Stream s = getItem(position);
		String streamname = s.name;
		
		holder.overflowButton.setTag(s.url);
		
		if(s.name.length() > 17)
			streamname = s.name.substring(0, 17)+"...";

		holder.textStreamName.setText(streamname);
		String image_url = this.fragment.getActivity().getString(R.string.http_url)+s.url+".png";
		holder.imageView.setImageUrl(image_url);
		
		if(!s.isDeletable)
			holder.overflowButton.setVisibility(View.GONE);
		else
			holder.overflowButton.setVisibility(View.VISIBLE);

		if(!s.isLive)
			holder.liveNowView.setText("");
		else
			holder.liveNowView.setText("Live Now");
		
		if (s.viewerCount > 0) {
			holder.textStreamViewerCount.setText(this.fragment.getActivity().getResources()
					.getQuantityString(R.plurals.viewers_count, s.viewerCount,
							s.viewerCount));
			holder.textStreamViewerCount.setVisibility(View.VISIBLE);
		} else {
			holder.textStreamViewerCount.setVisibility(View.INVISIBLE);
		}
		
		if (s.isPublic) {
			holder.privacyImage.setImageResource(R.drawable.ic_public_sign);
		}
		else {
			holder.privacyImage.setImageResource(R.drawable.ic_private);
		}
		
		int progress = 24-(int)getTimeDifference(s.registerTime);
		if(progress < 0)
			progress = 1;
		
		holder.progressBar.setProgress(progress);
		holder.publishedTime.setText(DateUtils.getRelativeTimeSpanString(s.registerTime));

		return rowView;
	}
	
	public long getTimeDifference(long registerTime)
	{
	    
	    Date currentTime = Calendar.getInstance().getTime();
	    currentTime.getTime();
	    long diff = currentTime.getTime()-registerTime;
	    long diff_sec = TimeUnit.MILLISECONDS.toSeconds(diff);
	    diff_sec/= 60;
	    diff_sec /= 60;
	    long hours = diff_sec % 24;
	    
	    return hours;
	}
}