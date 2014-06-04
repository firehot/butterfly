package com.butterfly.adapter;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.utils.Utils;
import com.commonsware.cwac.endless.EndlessAdapter;

public class ButterfFlyEndlessAdapter extends EndlessAdapter {
	private RotateAnimation rotate = null;
	private View pendingView = null;
	public StreamListAdapter streamListAdapter = null;
	public int last_offset = 10;
	public static int batch_size = 10;
	public boolean reached_end = false;
	List<Stream> streamList = null;

	public ButterfFlyEndlessAdapter(StreamListAdapter streamListAdapter) {
		super(streamListAdapter);

		this.streamListAdapter = streamListAdapter;
		rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(600);
		rotate.setRepeatMode(Animation.RESTART);
		rotate.setRepeatCount(Animation.INFINITE);
	}

	@Override
	protected void appendCachedData() {
		//Log.d("butterfly", "appendCachedData");
		if (streamList != null)
			streamListAdapter.addAll(streamList);
	}

	public void init() {
		this.streamListAdapter.clear();
		this.last_offset = 10;
		this.reached_end = false;
		restartAppending();

	}

	public void addAll(ArrayList<Stream> streamList) {
		streamListAdapter.addAll(streamList);
	}

	@Override
	protected boolean cacheInBackground() throws Exception {

		if (!MainActivity.mainActivityCompleted) {
			streamList = null;
			return true;
		}
		
		if (reached_end) {
			streamList = null;
			return false;
		}

		MainActivity activity = (MainActivity) streamListAdapter.getContext();
		String urlAddress = activity.getString(R.string.http_gateway_url);

		String streams = Utils.getLiveStreams(activity, urlAddress,
				String.valueOf(last_offset), String.valueOf(batch_size));

		if (streamList != null)
			streamList.clear();
		else
			streamList = new ArrayList<Stream>();

		if (streams != null) {

			try {

				streamList.addAll(Utils.parseStreams(streams, activity));

				if (streamList.size() < batch_size) {
					reached_end = true;
				}

			} catch (JSONException e) {

			}

		}

		last_offset += batch_size;
		return true;
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		View row = LayoutInflater.from(parent.getContext()).inflate(
				R.layout.stream_list_item, null);

		pendingView = row.findViewById(R.id.streamListItemRelativeLayout);
		pendingView.setVisibility(View.GONE);
		pendingView = row.findViewById(R.id.throbber);
		pendingView.setVisibility(View.VISIBLE);
		startProgressAnimation();

		return (row);
	}

	void startProgressAnimation() {
		if (pendingView != null) {
			pendingView.startAnimation(rotate);
		}
	}

	public void remove(Stream stream) {
		this.streamListAdapter.remove(stream);
	}

}
