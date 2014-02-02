package com.butterfly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.adapter.GridViewAdapter;

public class VideoGridFragment extends Fragment implements OnItemClickListener {

	private GridViewAdapter gridAdapter;
	private GridView gridView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.activity_video, container, false);
		gridView = (GridView) v.findViewById(R.id.gridview);
		gridView.setOnItemClickListener(this);

		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		gridAdapter = new GridViewAdapter(this.getActivity());
		
		gridView.setAdapter(gridAdapter);
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		
		Intent intent = new Intent(getActivity().getApplicationContext(),
				MediaPlayerActivity.class);
		intent.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME,
				gridAdapter.getItem(position));
		startActivity(intent);
	}



}
