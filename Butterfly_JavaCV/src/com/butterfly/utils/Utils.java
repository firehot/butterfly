package com.butterfly.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;
import android.widget.Toast;

import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class Utils {

	public static String getRegisteredMailList(Activity activity) {
		SharedPreferences sharedPreferences = activity.getSharedPreferences(MainActivity.APP_SHARED_PREFERENCES, MainActivity.MODE_PRIVATE);
		
		return sharedPreferences.getString(MainActivity.REGISTERED_MAIL_ADDRESS, null);
	
	}

	public static ArrayList<String> getFullMailArrayList(Activity activity) {
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(activity).getAccounts();
		//	String mails = new String();
		ArrayList<String> mails = new ArrayList<String>();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches() && 
					mails.contains(account.name) == false) {
				mails.add(account.name);
			}
		}
		return mails;
	}

	public static String getLiveStreams(Activity activity,String... params)
	{
		String streams = null;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {

			amfConnection.connect(params[0]);
			String mails = Utils.getRegisteredMailList(activity);
			streams = (String) amfConnection.call("getLiveStreams",mails,params[1],params[2]);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		amfConnection.close();

		return streams;
	}

	public static List<Stream> parseStreams(String streams,Context context) throws JSONException 
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
		} else {
			Toast.makeText(context,
					context.getString(R.string.noLiveStream),
					Toast.LENGTH_LONG).show();
		}

		return streamList;
	}

}
