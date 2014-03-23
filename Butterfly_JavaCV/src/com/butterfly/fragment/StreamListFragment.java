package com.butterfly.fragment;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.butterfly.MainActivity;
import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.adapter.StreamListAdapter;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.message.CloudMessaging;
import com.butterfly.tasks.DeleteStreamTask;

public class StreamListFragment extends ListFragment implements IStreamListUpdateListener,
OnClickListener{

	public static final String STREAM_PUBLISHED_NAME = "stream-name";
	public static final String STREAM_IS_LIVE = "is-live";
	private StreamListAdapter adapter;
	public static CloudMessaging msg;

	public static class Stream {
		public String name;
		public String url;
		public int viewerCount;
		public double latitude;
		public double longitude;
		public double altitude;
		public boolean isLive;
		public boolean isDeletable;

		public Stream(String name, String url, int viewerCount, double latitude, double longitude, double altitude
				,boolean isLive,boolean isDeletable) {
			super();
			this.name = name;
			this.url = url;
			this.viewerCount = viewerCount;
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
			this.isLive = isLive;
			this.isDeletable = isDeletable;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			Toast.makeText(getActivity(),
					adapter.getItem(position).name, Toast.LENGTH_SHORT).show();

			Stream s = adapter.getItem(position);

			Intent intent = new Intent(getActivity().getApplicationContext(),
					MediaPlayerActivity.class);
			intent.putExtra(STREAM_PUBLISHED_NAME,
					adapter.getItem(position).url);
			intent.putExtra(STREAM_IS_LIVE, s.isLive);
			startActivity(intent);



		}
	};
	private ArrayList<Stream> streamList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.main, container, false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		streamList = ((MainActivity)getActivity()).getStreamList();
		adapter = new StreamListAdapter(this);

		setListAdapter(adapter);
		getListView().setOnItemClickListener(itemClickListener);
		
		streamListUpdated(streamList);
	}

	@Override
	public void onStart() {
		((MainActivity)getActivity()).registerStreamListListener(this);
		super.onStart();
	}

	@Override
	public void onStop() {
		((MainActivity)getActivity()).removeStreamListListener(this);
		super.onStop();
	}

	public void streamListUpdated(ArrayList<Stream> streamList)
	{
		if (adapter != null) {
			adapter.clear();
			if (streamList != null) {
				adapter.addAll(streamList);
			}
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		showStreamPopup(v);
		
	}

	// Display anchored popup menu based on view selected
	  private void showStreamPopup(final View v) {
	    PopupMenu popup = new PopupMenu(this.getActivity(), v);
	    // Inflate the menu from xml
	    popup.getMenuInflater().inflate(R.menu.stream_popup_menu, popup.getMenu());
	    // Setup menu item selection
	    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
	    	
	        public boolean onMenuItemClick(MenuItem item) {
	            switch (item.getItemId()) {
	            case R.id.menu_stream_popup_delete: 
	            	StreamListFragment fragment = StreamListFragment.this;
	            	DeleteStreamTask deleteTask = new DeleteStreamTask(fragment);
	            	deleteTask.execute(fragment.getActivity().getString(R.string.http_gateway_url),v.getTag().toString());
	             return true;
	            case R.id.menu_stream_popup_share:
	              Toast.makeText(StreamListFragment.this.getActivity(), "Share!", Toast.LENGTH_SHORT).show();
	              return true;
	            default:
	              return false;
	            }
	        }
	    });
	    // Handle dismissal with: popup.setOnDismissListener(...);
	    // Show the menu
	    popup.show();
	  }
	  
	  public void removeStream(String streamUrl)
	  {
		  int count = adapter.getCount();
		  Stream stream = null;
		  for(int i = 0; i < count;i++)
		  {
			  stream = adapter.getItem(i);
			  if(stream.url.equals(streamUrl))
			  {
				  break;
			  }
		  }
		  
		  if(stream != null)
		  {
			  adapter.remove(stream);
			  adapter.notifyDataSetChanged();
		  }
	  }
}
