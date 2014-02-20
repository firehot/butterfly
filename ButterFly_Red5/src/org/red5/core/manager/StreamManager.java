package org.red5.core.manager;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.red5.core.Application;
import org.red5.core.dbModel.Stream;

public class StreamManager {

	Application red5App;

	public StreamManager(Application red5App)
	{
		this.red5App = red5App;
	}

	public String getLiveStreams(Set<Entry<String, Stream>> entrySet) {

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;


		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, Stream> entry = (Entry<String, Stream>) iterator
					.next();
			Stream stream = entry.getValue();

			if (stream.isPublic) {
				jsonObject = new JSONObject();
				jsonObject.put("url", stream.streamUrl);
				jsonObject.put("name", stream.streamName);
				jsonObject.put("viewerCount", stream.getViewerCount());
				jsonObject.put("latitude", stream.latitude);
				jsonObject.put("longitude", stream.longitude);
				jsonObject.put("altitude", stream.altitude);
				jsonObject.put("isLive", stream.isLive);
				jsonArray.add(jsonObject);
			}
		}

		return jsonArray.toString();
	}

	public boolean isLiveStreamExist(String url,Set<String> streamNames) {

		boolean result = false;
		if (streamNames.contains(url)) {
			result = true;
		}
		return result;
	}

	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage) {

		Map<String, Stream> registeredStreams = this.red5App.getRegisteredStreams();

		boolean result = false;
		if (registeredStreams.containsKey(url) == false) {
			if (isPublic == true) {
				Stream stream = new Stream(streamName, url, Calendar.getInstance().getTime(), isPublic);
				stream.setGCMUser(this.red5App.getRegistrationIdList(broadcasterMail));

				registeredStreams.put(url, stream);
			}
			this.red5App.sendNotificationsOrMail(mailsToBeNotified, broadcasterMail, url,
					streamName, deviceLanguage);
			// return true even if stream is not public
			result = true;
		}
		return result;
	}

	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {

		Map<String, Stream> registeredStreams = this.red5App.getRegisteredStreams();
		boolean result = false;
		if (registeredStreams.containsKey(url) == true) {
			Stream stream = registeredStreams.get(url);
			stream.latitude = latitude;
			stream.longitude = longitude;
			stream.altitude = altitude;
			result = true;
		}
		return result;
	}

	public boolean removeStream(String streamUrl) {
		Map<String, Stream> registeredStreams = this.red5App.getRegisteredStreams();
		boolean result = false;
		if (registeredStreams.containsKey(streamUrl)) {
			Stream stream = registeredStreams.remove(streamUrl);
			stream.close();
			if (stream != null) {
				result = true;
			}
			stream = null;
			// File f = new File("webapps/ButterFly_Red5/"+streamUrl+".png");
			// f.delete();
		}
		return result;
	}
}
