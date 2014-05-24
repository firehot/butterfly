package org.red5.core;

public interface IWebService {
	
	/**
	 * Return live streams in a json array
	 * @param mails - mail of the user
	 * @param start - offset
	 * @param batchSize - count of streams
	 *  
	 * @return Live streams in a json array
	 */
	public String getLiveStreams(String mails,String start,String batchSize);
	
	
	/**
	 * Same as getLiveStreams(String mails,String start,String batchSize)
	 * just gives offset 0 and batchSize 10 
	 * @param mails
	 */
	public String getLiveStreams(String mails);

	/**
	 * register live stream to server and gives other details
	 * @param streamName
	 * @param url
	 * @param mailsToBeNotified
	 * @param broadcasterMail
	 * @param isPublic
	 * @param deviceLanguage
	 * @return 
	 * boolean. if successful returns true else returns false
	 */
	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage);
	
	/**
	 * register location for a specified stream
	 * @param url
	 * @param longitude
	 * @param latitude
	 * @param altitude
	 * @return
	 * boolean. if successful returns true else returns false
	 */
	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude);
	
	/**
	 * register user with gcm id and mail adress.
	 * @param register_id
	 * @param mail 
	 * mail address can be comma separated
	 * @return
	 * boolean. if successful returns true else returns false
	 */
	public boolean registerUser(String register_id, String mail) ;
	
	
	/**
	 * use registerUser(String registerId, String mail, String deviceId)
	 * 
	 * update a users gcm id 
	 * @param register_id
	 * @param mail
	 * @param oldRegID
	 * @return
	 * boolean. if successful returns true else returns false
	 */
	public boolean updateUser(String register_id, String mail, String oldRegID);
	
	
	/**
	 * delete a stream 
	 * @param url
	 * @return
	 * boolean. if successful returns true else returns false
	 */
	public boolean deleteStream(String url);

	/**
	 * Saves a  preview of the stream
	 * @param data
	 * @param streamURL
	 */
	public void savePreview(byte[] data, String streamURL);
	
	/**
	 * checks that if the stream with specified url is still live
	 * @param url
	 * @return
	 * If that stream is still live, it returns true
	 * else it returns false
	 */
	public boolean isLiveStreamExist(String url);
}
