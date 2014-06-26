package org.red5.core.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.StreamProxy;
import org.red5.core.dbModel.StreamViewers;
import org.red5.core.dbModel.Streams;
import org.red5.core.utils.JPAUtils;

public class StreamManager {

	public static final int MAX_TIME_INTERVAL_BETWEEN_PACKETS = 20000;

	public List<Streams> getLiveStreams(Map<String, StreamProxy> registeredLiveStreams,
			List<String> mailList, String start, String batchSize) {

		removeGhostStreams(registeredLiveStreams, System.currentTimeMillis(), start, batchSize);

		return getAllStreamList(mailList, start, batchSize);
	}

	public boolean isDeletable(Streams stream, List<String> mailList) 
	{
		if(mailList == null)
			return false;
		
		Set<GcmUserMails> mails = stream.getGcmUsers().getGcmUserMailses();
		for (GcmUserMails gcmUserMails : mails) {
			for (String mailItem : mailList) {
				if(gcmUserMails.getMail().equals(mailItem))
				{
					return true;
				}
			}
		}
		return false;
	}

	public void removeGhostStreams(Map<String, StreamProxy> registeredLiveStreams,
			long currentTime, String start, String batchSize) {
		List<Streams> streamList = getAllStreamList(null, start, batchSize);
		if (streamList != null) {
			for (Streams stream : streamList) {

				StreamProxy streamProxy = null;
				if (registeredLiveStreams.containsKey(stream.getStreamUrl())) {
					streamProxy = registeredLiveStreams.get(stream.getStreamUrl());

					if (registeredLiveStreams.containsKey(stream.getStreamUrl())) {
						streamProxy = registeredLiveStreams.get(stream.getStreamUrl());

						if ((currentTime - streamProxy.getLastPacketReceivedTime()) > MAX_TIME_INTERVAL_BETWEEN_PACKETS) {

							removeStream(stream.getStreamUrl(),
									registeredLiveStreams);
						}
					}
				}
			}
		}

	}

	public boolean isLiveStreamExist(String url, Set<String> streamNames) {

		boolean result = false;
		if (streamNames.contains(url)) {
			result = true;
		}
		return result;
	}

	public StreamProxy registerLiveStream(String url,
			String mailsToBeNotified,Streams stream) {

		JPAUtils.beginTransaction();

		JPAUtils.getEntityManager().persist(stream);

		// saveStream(stream);

		if (mailsToBeNotified != null) {
			String[] mails = mailsToBeNotified.split(",");
			List<String> mailList = new ArrayList<String>(Arrays.asList(mails));

			Query query = JPAUtils.getEntityManager().createQuery(
					"SELECT DISTINCT user FROM GcmUsers AS user "
							+ "JOIN user.gcmUserMailses AS userMails "
							+ "WHERE userMails.mail IN :email");
			query.setParameter("email", mailList);
			List<GcmUsers> results = query.getResultList();

			for (GcmUsers gcmUserMails : results) {
				JPAUtils.getEntityManager().persist(
						new StreamViewers(stream, gcmUserMails));
			}
		}

		JPAUtils.commit();

		StreamProxy proxy = new StreamProxy(url, stream.getId());


		return proxy;
	}

	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {

		Streams stream = getStream(url);

		boolean result = false;
		if (stream != null) {

			stream.setLatitude(latitude);
			stream.setLongitude(longitude);
			stream.setAltitude(altitude);

			updateStream(stream);
			result = true;
		}
		return result;
	}

	public boolean removeStream(String streamUrl,
			Map<String, StreamProxy> registeredLiveStreams) {

		boolean result = false;
		if (registeredLiveStreams.containsKey(streamUrl)) {
			StreamProxy stream = registeredLiveStreams.remove(streamUrl);
			stream.close();

			Streams strm = getStream(streamUrl);
			strm.setIsLive(false);
			updateStream(strm);

			if (stream != null) {
				result = true;
			}
			stream = null;
		}
		return result;
	}

	public boolean saveStream(Streams stream) {
		boolean result;
		try {

			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().persist(stream);
			JPAUtils.commit();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	public boolean updateStream(Streams stream) {
		boolean result;
		try {

			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().merge(stream);
			JPAUtils.commit();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	public boolean deleteStream(Streams stream) {
		boolean result;
		try {
			EntityManager em = JPAUtils.getEntityManager();
			JPAUtils.beginTransaction();
			em.remove(em.contains(stream) ? stream : em.merge(stream));
			JPAUtils.commit();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public Streams getStream(String streamUrl) {

		Streams resultStream = null;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM Streams where streamUrl= :streamUrl");
			query.setParameter("streamUrl", streamUrl);
			resultStream = (Streams) query.getSingleResult();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultStream;
	}

	/**
	 * Function to get public and private streams from database.
	 * 
	 * @param mailList
	 *            list of mails that stream is shared with
	 * 
	 * @return public streams and private streams shared with the mailList if
	 *         mailList is null then it returns only public streams
	 */
	public List<Streams> getAllStreamList(List<String> mailList, String start,
			String batchSize) {
		List<Streams> results = null;
		try {
			Query query = null;
			if (mailList != null) {
				// TODO: how to improve query below. same subquery executed
				// twice
				query = JPAUtils
						.getEntityManager()
						.createQuery(
								"SELECT str FROM Streams AS str "
										+ "LEFT JOIN str.streamViewerses AS viewer "
										+ "WHERE ( (str.isPublic = :isPublic) "
										+ " OR (viewer.gcmUsers.id IN "
										+ "		 (SELECT gcmUsers.id as gcmId FROM GcmUserMails userMails WHERE userMails.mail IN (:mails)))"
										+ " OR (str.gcmUsers.id IN "
										+ "		 (SELECT gcmUsers.id as gcmId FROM GcmUserMails userMails WHERE userMails.mail IN (:mails)))"
										+ ")"
										+ " ORDER BY str.registerTime DESC ");
				query.setParameter("mails", mailList);
			} else {
				query = JPAUtils.getEntityManager().createQuery(
						"SELECT str FROM Streams AS str "
								+ " WHERE (str.isPublic = :isPublic) ");
			}
			query.setParameter("isPublic", true);
			query.setFirstResult(Integer.parseInt(start));
			query.setMaxResults(Integer.parseInt(batchSize));

			results = query.getResultList();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

}
