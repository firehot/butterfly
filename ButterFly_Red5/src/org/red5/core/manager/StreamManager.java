package org.red5.core.manager;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.red5.core.Application;
import org.red5.core.dbModel.Stream;
import org.red5.core.utils.JPAUtils;

public class StreamManager {

	Application red5App;

	public StreamManager(Application red5App)
	{
		this.red5App = red5App;
	}

	public String getLiveStreams(Set<Entry<String, Stream>> entrySet) {

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;

		java.util.Date date = new java.util.Date();
		Timestamp currentTime = new Timestamp(date.getTime());

		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, Stream> entry = (Entry<String, Stream>) iterator
					.next();
			Stream stream = entry.getValue();

			if (stream.timeReceived != null) {
				
				long diff = currentTime.getTime() - stream.timeReceived.getTime();
				if ( diff > 5000) {
					stream.isLive = false;
					stream.close();
				}
			}
					
			if (stream.isPublic) {
				jsonObject = new JSONObject();
				jsonObject.put("url", stream.streamUrl);
				jsonObject.put("name", stream.streamName);
				jsonObject.put("viewerCount", this.red5App.getViewerCount(stream.streamUrl));
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
			
			Stream stream = new Stream(streamName, url, Calendar.getInstance().getTime(), isPublic);
			stream.setBroadcasterMail(broadcasterMail);

			registeredStreams.put(url, stream);
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
	
	public boolean saveStream(Stream stream) {
		boolean result;
		try {
			
			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().persist(stream);
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}
	
	public boolean updateStream(Stream stream) {
		boolean result;
		try {
			
			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().merge(stream);
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}
	
	public boolean deleteStream(Stream stream) {
		boolean result;
		try {
			EntityManager em = JPAUtils.getEntityManager();
			JPAUtils.beginTransaction();	
			em.remove(em.contains(stream) ? stream : em.merge(stream));	
			JPAUtils.commit();
			JPAUtils.closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}
	
	
	public Stream getStream(String streamUrl) {

		Stream resultStream = null;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM Stream where streamUrl= :streamUrl");
			query.setParameter("streamUrl", streamUrl);
			resultStream = (Stream)query.getSingleResult();
			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultStream;
	}
	
	public List<Stream> getAllStreamList() {
		List results = null;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM Stream");
			
			results = query.getResultList();
			
			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return (List<Stream>)((Object)results);
	}
}