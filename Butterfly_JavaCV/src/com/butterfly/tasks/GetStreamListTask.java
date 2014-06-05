package com.butterfly.tasks;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;

import com.butterfly.MainActivity;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.utils.Utils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class GetStreamListTask extends	AbstractAsyncTask<String, Void, List<Stream>> {

	public GetStreamListTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}

	@Override
	protected List<Stream> doInBackground(String... params) {
		
		String streams = null;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {

			amfConnection.connect(HTTP_GATEWAY_URL);
			String mails = Utils.getRegisteredMailList(this.context);
			streams = (String) amfConnection.call("getLiveStreams",mails,params[0],params[1]);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		amfConnection.close();
		
		if (streams != null) {
			try {
				return parseStreams((String)streams);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public List<Stream> parseStreams(String streams) throws JSONException 
	{
		List<StreamListFragment.Stream> streamList = new ArrayList<StreamListFragment.Stream>();

		JSONArray jsonArray = new JSONArray(streams);
		int length = jsonArray.length();
		if (length > 0) {

			JSONObject jsonObject;

			for (int i = 0; i < length; i++) {
				jsonObject = (JSONObject) jsonArray.get(i);

				String tmp = jsonObject.getString("latitude");
				if (tmp.equals("null")) { tmp = "0"; }
				Double latitude = Double.parseDouble(tmp);

				tmp = jsonObject.getString("longitude");
				if (tmp.equals("null")) { tmp = "0"; }
				Double longitude = Double.parseDouble(tmp);

				tmp = jsonObject.getString("altitude");
				if (tmp.equals("null")) { tmp = "0"; }
				Double altitude = Double.parseDouble(tmp);

				boolean isPublic = true;
				if (jsonObject.has("isPublic")) {
					isPublic = jsonObject.getBoolean("isPublic");
				}

				long registerTime = 0;
				if (jsonObject.has("registerTime")) {
					registerTime = jsonObject.getLong("registerTime");
				}

				streamList.add(new Stream(jsonObject.getString("name"), 
						jsonObject.getString("url"), 
						Integer.parseInt(jsonObject.getString("viewerCount")), 
						latitude, 
						longitude, 
						altitude, 
						Boolean.parseBoolean(jsonObject.getString("isLive")),
						Boolean.parseBoolean(jsonObject.getString("isDeletable")),
						isPublic,
						registerTime));


			}
		}

		return streamList;
	}
	
	



}
