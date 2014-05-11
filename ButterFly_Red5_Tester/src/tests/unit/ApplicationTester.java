
package tests.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.core.Application;
import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIds;
import org.red5.core.dbModel.StreamProxy;
import org.red5.core.dbModel.Streams;
import org.red5.core.manager.StreamManager;
import org.red5.core.manager.StreamProxyManager;
import org.red5.core.utils.JPAUtils;

public class ApplicationTester {

	Application butterflyApp;

	@Before
	public void before() {
		butterflyApp = new Application();

		prepareEnvironment();

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void prepareEnvironment() {
		File webappsDir = new File("webapps");
		if (webappsDir.exists()) {
			delete(webappsDir);
		}
		JPAUtils.beginTransaction();
		Query query = JPAUtils.getEntityManager().createQuery("Delete FROM StreamViewers");
		query.executeUpdate();	
		query = JPAUtils.getEntityManager().createQuery("Delete FROM RegIds");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM GcmUserMails");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM Streams");
		query.executeUpdate();		
		query = JPAUtils.getEntityManager().createQuery("Delete FROM GcmUsers");
		query.executeUpdate();
		JPAUtils.commit();
		JPAUtils.closeEntityManager();

	}

	@After
	public void after() {
		butterflyApp = null;
	}

	@Test
	public void testRegisterStream() {

		boolean result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjj", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com");
		assertEquals(true, result);

		butterflyApp.registerUser("22", "mail@mail.com");
		
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		while(butterflyApp.isNotificationSent() == false) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		List<Streams> streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertTrue(streamList.get(0).getIsLive());

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName"+11, "publishUrl", null, "mail@mail.com", true, null);
		//should return false because url is key
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl" + 11, "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		//should return true because url is changed
		assertEquals(registerLiveStream, true);

		while(butterflyApp.isNotificationSent() == false) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


	}

	@Test
	public void testRemoveStream() {
		boolean result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjj", "ahmetmermerkaya@gmail.com");
		assertEquals(true, result);

		result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjasdfÅŸasdfjj", "ahmetmermerkaya@hotmail.com");
		assertEquals(true, result);

		butterflyApp.registerUser("22", "mail@mail.com");
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		Query query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers");
		assertEquals(2, query.getResultList().size());

		registerLiveStream = butterflyApp.removeStream("publishUrl");
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.removeStream("publishUrl" + 1234);
		assertEquals(registerLiveStream, false);

		query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers");
		assertEquals(2, query.getResultList().size());

	}

	@Test
	public void testRegisterUser() {
		int t = (int) (Math.random()*1000);
		boolean result = butterflyApp.registerUser(String.valueOf(t), "dgsdgs");
		assertEquals(true, result);

	}

	public int getMailRowCount(String mail) {

		int result = 0;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM GcmUserMails where mail= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			result = results.size();
			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	@Test
	public void testUserUpdate()
	{
		int count = getMailRowCount("mail1");
		int initialCount = count;

		int t = (int) (Math.random()*1000);
		boolean result = butterflyApp.registerUser(String.valueOf(t), "mail1");
		assertEquals(true, result);


		count = getMailRowCount("mail1");
		assertEquals(initialCount+1, count);

		int t2 = (int) (Math.random()*1000);
		result = butterflyApp.registerUser(String.valueOf(t2), "mail1");
		assertEquals(true, result);

		EntityManager entityManager = JPAUtils.getEntityManager();

		try {

			Query query = entityManager
					.createQuery("FROM GcmUserMails WHERE mail= :email");
			query.setParameter("email", "mail1");
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = ((GcmUserMails) results.get(0)).getGcmUsers();
				Set<RegIds> regIdSet = gcmUsers.getRegIdses();

			}

			JPAUtils.closeEntityManager();
		} catch (NoResultException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}


		count = getMailRowCount("mail1");
		assertEquals(initialCount+1, count);

	}



	@Test
	public void testGetRegistrationId() {
		int t = (int) (Math.random()*1000);
		String mail = "mail@mailc.com" + t;
		boolean result = butterflyApp.registerUser(String.valueOf(t), mail);
		assertEquals(result, true);

		GcmUsers gcmUser = butterflyApp.userManager.getGcmUserByMail(mail);

		Query query = JPAUtils.getEntityManager().createQuery("FROM RegIds WHERE gcmUsers.id = :id");
		query.setParameter("id", gcmUser.getId());

		List<RegIds> list = query.getResultList();

		assertEquals(list.get(0).getGcmRegId(), String.valueOf(t));

		gcmUser = butterflyApp.userManager.getGcmUserByMail("slkdjflasjf" + t);

		assertEquals(gcmUser, null);
	}

	@Test
	public void testUpdateCommaSeparatedMails() {
		String id = "123132131";
		boolean isRegistered = butterflyApp.registerUser(id, "hasan@hasan.com,salih@salih.com");
		assertEquals(isRegistered, true);

		Query query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "hasan@hasan.com");
		GcmUserMails userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);

		String newid = "35252828634";
		boolean result = butterflyApp.updateUser(newid, "hasan@hasan.com,salih@salih.com", id);
		assertEquals(result,true);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "hasan@hasan.com");
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), newid);

	}


	@Test
	public void testRegisterCommaSeparatedMails() {
		String id = "123132131";
		boolean isRegistered = butterflyApp.registerUser(id, "hasan@hasan.com,salih@salih.com,okan@okan.com");
		assertEquals(isRegistered, true);

		Query query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "hasan@hasan.com");
		GcmUserMails userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(1, query.getResultList().size());

		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "salih@salih.com");
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(1, query.getResultList().size());

		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "okan@okan.com");
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(1, query.getResultList().size());

		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);


		//differnt reg id and also adding an extra mail to array
		String id2="65565656";
		isRegistered = butterflyApp.registerUser(id2, "hasan@hasan.com,salih@salih.com,okan@okan.com,ferit@ferit.com");
		assertEquals(isRegistered, true);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "hasan@hasan.com");
		//because hasan@hasan.com exists, it doesnt add extra gcmuser. It only adds reg id and new email addres ferit@ferit.com
		assertEquals(query.getResultList().size(),1);
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().size(), 2);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "salih@salih.com");
		assertEquals(query.getResultList().size(),1);
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().size(), 2);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "okan@okan.com");
		assertEquals(query.getResultList().size(),1);
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().size(), 2);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "okan@okan.com");
		assertEquals(query.getResultList().size(),1);
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().size(), 2);

		isRegistered = butterflyApp.registerUser(id2, "hasan@hasan.com,salih@salih.com,okan@okan.com,ferit@ferit.com");
		assertEquals(isRegistered, false);


		isRegistered = butterflyApp.registerUser(id2, "hasan@hasan.com,ferit@ferit.com");
		assertEquals(isRegistered, false);

		isRegistered = butterflyApp.registerUser(id2, "dunya@dunya.com,ferit@ferit.com");
		assertEquals(isRegistered, false);

		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "dunya@dunya.com");
		assertEquals(query.getResultList().size(),1);
		userMail = (GcmUserMails) query.getSingleResult();

		assertEquals(userMail.getGcmUsers().getRegIdses().size(), 2);
	}


	@Test
	public void testDeleteUser()
	{
		int t = (int) (Math.random()*1000);
		String mail = "murat@mailc.com" + t;



		boolean result = butterflyApp.registerUser(String.valueOf(t), mail);
		assertEquals(result, true);

		butterflyApp.registerUser("22", "mail@mail.com");
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", mail, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		Query query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers");
		assertEquals(1, query.getResultList().size());

		query = JPAUtils.getEntityManager().createQuery("FROM Streams");
		assertEquals(1, query.getResultList().size());

		GcmUsers user = butterflyApp.userManager.getGcmUserByMail(mail);

		boolean opResult = butterflyApp.deleteRegId(String.valueOf(t));
		assertEquals(true, opResult);

		user = butterflyApp.userManager.getGcmUserByRegId(String.valueOf(t));

		assertEquals(user, null);
	}

	@Test
	public void testRegisterMultipleUser()
	{
		boolean registerUser = butterflyApp.registerUser("1", "deneme@deneme.com");
		assertTrue(registerUser);

		registerUser = butterflyApp.registerUser("1", "deneme@deneme.com");
		assertTrue(!registerUser);
	}

	@Test
	public void testRegisterLocationForStream() {
		List<Streams> streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(0, streamList.size());
		
		butterflyApp.registerUser("22", "mail@mail.com");
		GcmUsers user = butterflyApp.userManager.getGcmUserByMail("mail@mail.com");
		
		Streams strm =  new Streams(user, Calendar.getInstance().getTime(), "location_test", "video_url");
		strm.setIsPublic(true);
		butterflyApp.streamManager.saveStream(strm);

		streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(1, streamList.size());

		butterflyApp.registerLocationForStream("video_url", 23.4566, 34.667, 100);

		streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(1, streamList.size());

		Streams stream = butterflyApp.streamManager.getStream("video_url");
		assertNotNull(stream);
		assertEquals(23.4566, stream.getLongitude(), 1e-8);
		assertEquals(34.667, stream.getLatitude(), 1e-8);
		assertEquals(100, stream.getAltitude(), 1e-8);

	}
	

	@Test
	public void testSendMail() {
		ArrayList<String> mail = new ArrayList<String>();
		mail.add("ahmetmermerkaya@gmail.com");
		butterflyApp.sendMail(mail, "Test mail ", "Bu bir test mailidir.", "streamURL", "tur");
		String mailString = new String();
		for (int i = 0; i < mail.size(); i++) {
			mailString += mail.get(i) + ",";
		}
		while(butterflyApp.isNotificationSent() == false) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		fail("to correct this test check mail is received at " + mailString);
	}

	//	@Test
	//	public void testSendNotificationOrMail()
	//	{
	//		butterflyApp.sendNotificationsOrMail("ahmetmermerkaya@gmail.com","mail;videourl","en");
	//	}

	@Test
	public void testStreamDeleter() {
		butterflyApp.cancelStreamDeleteTimer();
		butterflyApp.cancelStreamDeleteTimer();
		File webApps = new File("webapps");
		if (webApps.exists() == false) {
			webApps.mkdir();
		}

		File butterFly = new File(webApps, "ButterFly_Red5");
		if (butterFly.exists() == false) {
			butterFly.mkdir();
		}

		File streamsFolder = new File(butterFly, "streams");
		if (streamsFolder.exists() == false) {
			streamsFolder.mkdir();
		}

		File f1 = new File(streamsFolder, "f1.flv");
		File f2 = new File(streamsFolder, "f2.flv");
		File f3 = new File(streamsFolder, "f3.flv");
		File f1p = new File(butterFly, "f1.png");
		File f2p = new File(butterFly, "f2.png");
		File f3p = new File(butterFly, "f3.png");
		try {
			f1.createNewFile();
			f2.createNewFile();
			f3.createNewFile();
			f1p.createNewFile();
			f2p.createNewFile();
			f3p.createNewFile();
			boolean result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjj", "ahmetmermerkaya@gmail.com");
			assertEquals(true, result);
			
			result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjasdfÅŸasdfjj", "ahmetmermerkaya@hotmail.com");
			assertEquals(true, result);
			butterflyApp.registerUser("22", "mail@mail.com");
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", "ahmetmermerkaya@hotmail.com,ahmetmermerkaya@gmail.com", "mail@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f2", "ahmetmermerkaya@hotmail.com,ahmetmermerkaya@gmail.com", "mail@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", "ahmetmermerkaya@hotmail.com,ahmetmermerkaya@gmail.com", "mail@mail.com", true, "tur");
			assertTrue(registered);
			assertEquals(3, butterflyApp.streamProxyManager.getLiveStreamProxies().size());

			Query query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers");
			assertEquals(6, query.getResultList().size());


		} catch (IOException e1) {
			e1.printStackTrace();
		}
		assertEquals(f1.exists(), true);
		assertEquals(f2.exists(), true);
		assertEquals(f3.exists(), true);
		assertEquals(f1p.exists(), true);
		assertEquals(f2p.exists(), true);
		assertEquals(f3p.exists(), true);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		butterflyApp.scheduleStreamDeleterTimer(1000, 1000);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(f1.exists(), false);
		assertEquals(f2.exists(), false);
		assertEquals(f3.exists(), false);
		assertEquals(f1p.exists(), false);
		assertEquals(f2p.exists(), false);
		assertEquals(f3p.exists(), false);
		butterflyApp.cancelStreamDeleteTimer();

		Query query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers");
		assertEquals(0, query.getResultList().size());

		assertEquals(0, butterflyApp.streamProxyManager.getLiveStreamProxies().size());

		try {
			f1.createNewFile();
			f2.createNewFile();
			f3.createNewFile();
			f1p.createNewFile();
			f2p.createNewFile();
			f3p.createNewFile();
			
			assertEquals(f1p.exists(), true);
			
			butterflyApp.registerUser("25", "mail1@mail.com");
			butterflyApp.registerUser("23", "mail2@mail.com");
			butterflyApp.registerUser("24", "mail3@mail.com");
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", null, "mail1@mail.com", true, "tur");
			assertTrue(registered);
			
			registered = butterflyApp.registerLiveStream("streamName", "f2", null, "mail2@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", null, "mail3@mail.com", true, "tur");
			assertTrue(registered);
			
			assertEquals(3, butterflyApp.streamProxyManager.getLiveStreamProxies().size());
			

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		assertEquals(f1.exists(), true);
		assertEquals(f2.exists(), true);
		assertEquals(f3.exists(), true);
		assertEquals(f1p.exists(), true);
		assertEquals(f2p.exists(), true);
		assertEquals(f3p.exists(), true);

		butterflyApp.scheduleStreamDeleterTimer(1000, 1000);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(f1.exists(), false);
		assertEquals(f2.exists(), false);
		assertEquals(f3.exists(), false);
		assertEquals(f1p.exists(), false);
		assertEquals(f2p.exists(), false);
		assertEquals(f3p.exists(), false);

		butterflyApp.cancelStreamDeleteTimer();
	}

	@Test
	public void testSaveUpdateDeleteStream()
	{
		butterflyApp.registerUser("22", "mail@mail.com");
		GcmUsers user = butterflyApp.userManager.getGcmUserByMail("mail@mail.com");
		
		Streams strm = new Streams(user, Calendar.getInstance().getTime(), "deneme", "denemeurl");
		strm.setIsPublic(true);
		butterflyApp.streamManager.saveStream(strm);

		Streams createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertNotNull(createdStream);

		strm.setAltitude((double) 1300);
		butterflyApp.streamManager.updateStream(strm);

		createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertNotNull(createdStream);
		assertEquals(createdStream.getAltitude(), 1300,1);

		butterflyApp.streamManager.deleteStream(strm);
		createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertEquals(createdStream,null);

	}

	@Test
	public void testGetAllStreamList()
	{
		List<Streams> streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		int streamCount = streamList.size();

		butterflyApp.registerUser("22", "mail@mail.com");
		GcmUsers user = butterflyApp.userManager.getGcmUserByMail("mail@mail.com");

		
		Streams strm1 = new Streams(user, Calendar.getInstance().getTime(), "stream1", "stream1url");
		strm1.setIsPublic(true);
		butterflyApp.streamManager.saveStream(strm1);	
		Streams createdStream = butterflyApp.streamManager.getStream("stream1url");
		assertNotNull(createdStream);

		Streams strm2 = new Streams(user, Calendar.getInstance().getTime(), "stream2", "stream2url");
		strm2.setIsPublic(true);
		butterflyApp.streamManager.saveStream(strm2);
		createdStream = butterflyApp.streamManager.getStream("stream2url");
		assertNotNull(createdStream);

		streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(streamCount+2, streamList.size());


	}

	@Test
	public void testStreamViewer() {
		butterflyApp.registerUser("22", "mail@mail.com");
		boolean result = butterflyApp.registerUser("ksdjfÅŸlask9934803248omjj", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com");
		assertEquals(true, result);


		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		while(butterflyApp.isNotificationSent() == false) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		List<Streams> streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(streamList.size(), 1);
		Integer id = streamList.get(0).getId();

		Query query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers WHERE streams.id =:id");
		query.setParameter("id", id);
		assertEquals(query.getResultList().size(),1);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl1", "ahmetmermerkaya@gmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		while(butterflyApp.isNotificationSent() == false) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(streamList.size(), 2);
		id = streamList.get(1).getId();

		query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers WHERE streams.id = :id");
		query.setParameter("id", id);
		assertEquals(query.getResultList().size(), 1);


		query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers AS viewer JOIN viewer.gcmUsers AS user JOIN user.gcmUserMailses AS userMail WHERE userMail.mail = :mail");
		query.setParameter("mail", "ahmetmermerkaya@gmail.com");
		assertEquals(query.getResultList().size(), 2);

		query = JPAUtils.getEntityManager().createQuery("FROM StreamViewers AS viewer JOIN viewer.gcmUsers AS user JOIN user.gcmUserMailses AS userMail WHERE userMail.mail = :mail");
		query.setParameter("mail", "ahmetmermerkaya@hotmail.com");
		assertEquals(query.getResultList().size(), 2);


	}

	@Test
	public void testStreamNameTurkishChar() {

		String tmpName = "iÅŸoÄ±oliliÅŸliÃ¼ÄŸÄ±Ä°Ã§Ã‡Ã¶Ã–Ä�ÃœÃ¼";
		butterflyApp.registerUser("22", "mail@mail.com");
		boolean registerLiveStream = butterflyApp.registerLiveStream(tmpName, "publishUrl", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		List<Streams> streamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		assertEquals(streamList.size(), 1);
		Integer id = streamList.get(0).getId();

		assertEquals(tmpName, streamList.get(0).getStreamName());
	}

	@Test 
	public void testStreamPrivacy() {
		
		butterflyApp.registerUser("22", "mail@mail.com");
		boolean registerUser = butterflyApp.registerUser("deneme", "ahmetmermerkaya@gmail.com");
		assertTrue(registerUser);
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl1", "ahmetmermerkaya@gmail.com", "mail@mail.com", false, null);
		assertEquals(registerLiveStream, true);

		//get public videos
		List<Streams> allStreamList = butterflyApp.streamManager.getAllStreamList(null,"0", "10");
		//it should be zero because there is no public video
		assertEquals(0, allStreamList.size());

		ArrayList<String> mailList = new ArrayList<String>(Arrays.asList(new String[] {"ahmetmermerkaya@gmail.com"}));
		allStreamList = butterflyApp.streamManager.getAllStreamList(mailList,"0", "10");
		//it should be one because video is shared with that email
		assertEquals(1, allStreamList.size());

		//register same user with other email address
		registerUser = butterflyApp.registerUser("deneme123131", "ahmetmermerkaya@hotmail.com,ahmetmermerkaya@gmail.com");
		assertTrue(registerUser);

		mailList = new ArrayList<String>(Arrays.asList(new String[] {"ahmetmermerkaya@hotmail.com"}));
		allStreamList = butterflyApp.streamManager.getAllStreamList(mailList,"0", "10");
		//check tthat registered second mail get the shared video
		assertEquals(1, allStreamList.size());

		//register public live stream with not shared with explicitly
		registerLiveStream = butterflyApp.registerLiveStream("publishedNamesdfdsf", "publishUrl1sdsdfs", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		mailList = new ArrayList<String>(Arrays.asList(new String[] {"ahmetmermerkaya@hotmail.com", "ahmetmermerkaya@gmail.com"}));
		allStreamList = butterflyApp.streamManager.getAllStreamList(mailList,"0", "10");
		//check that multiple email address gets the public and private videos
		assertEquals(2, allStreamList.size());

		//register a public live stream with shared some mail explicitly
		registerLiveStream = butterflyApp.registerLiveStream("publishedsfsfNamesdfdsf", "publishUrl1sdsdfs32424", "ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		mailList = new ArrayList<String>(Arrays.asList(new String[] {"ahmetmermerkaya@hotmail.com", "ahmetmermerkaya@gmail.com"}));
		allStreamList = butterflyApp.streamManager.getAllStreamList(mailList,"0", "10");
		assertEquals(3, allStreamList.size());

	}

	@Test
	public void testIsPublicWorkingAndRegisterTime() {
		try {
			butterflyApp.registerUser("22", "mail@mail.com");
			long currentTimeMillis = System.currentTimeMillis();
			boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", true, null);
			assertEquals(registerLiveStream, true);

			String liveStreams = butterflyApp.getLiveStreams(null, "0", "10");

			JSONArray jsonArray = new JSONArray(liveStreams);

			int length = jsonArray.length();
			assertEquals(1, length);

			JSONObject jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isPublic"));

			assertTrue(jsonObject.getBoolean("isPublic"));			
			assertTrue(jsonObject.has("isLive"));			
			assertTrue(jsonObject.getBoolean("isLive"));			
			assertTrue(jsonObject.has("registerTime"));
			
			System.out.println(jsonObject.getLong("registerTime"));
			assertTrue(jsonObject.getLong("registerTime") - currentTimeMillis < 1000 );

			currentTimeMillis = System.currentTimeMillis();
			boolean result = butterflyApp.registerUser("skjÅŸsalkdfj908098", "ahmetmermerkaya@gmail.com");
			assertTrue(result);
			registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl9898", "ahmetmermerkaya@gmail.com,ahmetmermerkaya@hotmail.com", "mail@mail.com", false, null);
			assertEquals(registerLiveStream, true);

			liveStreams = butterflyApp.getLiveStreams("ahmetmermerkaya@gmail.com","0", "10");
			
			jsonArray = new JSONArray(liveStreams);

			length = jsonArray.length();
			assertEquals(2, length);
			
			jsonObject = (JSONObject) jsonArray.get(1);
			if(!jsonObject.getString("url").equals("publishUrl9898"))
				jsonObject = (JSONObject) jsonArray.get(0);
			
			
			assertTrue(jsonObject.has("isPublic"));
			assertTrue(!jsonObject.getBoolean("isPublic"));			
			assertTrue(jsonObject.has("registerTime"));
			
			System.out.println(jsonObject.getLong("registerTime"));
			assertTrue(jsonObject.getLong("registerTime") - currentTimeMillis < 1000 );
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRemoveGhostStream() {
		
		butterflyApp.registerUser("22", "mail@mail.com");
		GcmUsers user = butterflyApp.userManager.getGcmUserByMail("mail@mail.com");
		
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);
		
		String liveStreams = butterflyApp.getLiveStreams("ahmetmermerkaya@gmail.com","0", "10");
		
		JSONArray jsonArray;
		try {
			jsonArray = new JSONArray(liveStreams);
			int length = jsonArray.length();
			assertEquals(1, length);

			JSONObject jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isLive"));
			assertTrue(jsonObject.getBoolean("isLive"));
			
			butterflyApp.streamManager.removeGhostStreams(butterflyApp.streamProxyManager.getLiveStreamProxies(), System.currentTimeMillis(),"0", "10");
			
			liveStreams = butterflyApp.getLiveStreams(null,"0", "10");
			
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(1, length);

			jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isLive"));
			assertTrue(jsonObject.getBoolean("isLive"));
			
			Thread.sleep(5000);
			
			butterflyApp.streamManager.removeGhostStreams(butterflyApp.streamProxyManager.getLiveStreamProxies(), System.currentTimeMillis(),"0", "10");
			
			liveStreams = butterflyApp.getLiveStreams(null,"0", "10");
			
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(1, length);

			jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isLive"));
			assertTrue(jsonObject.getBoolean("isLive"));
			
			Thread.sleep(StreamManager.MAX_TIME_INTERVAL_BETWEEN_PACKETS);
			
			butterflyApp.streamManager.removeGhostStreams(butterflyApp.streamProxyManager.getLiveStreamProxies(), System.currentTimeMillis(),"0", "10");
			
			liveStreams = butterflyApp.getLiveStreams(null,"0", "10");
			
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(1, length);

			jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isLive"));
			assertTrue(jsonObject.getBoolean("isLive") == false);
			
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSeeYourOwnStream() {
		boolean isregistered = butterflyApp.registerUser("22", "mail@mail.com");
		assertTrue(isregistered);
		
		isregistered = butterflyApp.registerLiveStream("publisname", "publishurl", null, "mail@mail.com", false, "tur");
		assertTrue(isregistered);
	
		String liveStreams = butterflyApp.getLiveStreams(null,"0", "10");
		
		JSONArray jsonArray;
		try {
			jsonArray = new JSONArray(liveStreams);
			int length = jsonArray.length();
			assertEquals(0, length);
			
			liveStreams = butterflyApp.getLiveStreams("mail@mail.com","0", "10");
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(1, length);
			
			//share video user ownself
			isregistered = butterflyApp.registerLiveStream("publisname2", "publishurl2", "mail@mail.com", "mail@mail.com", false, "tur");
			assertTrue(isregistered);
			
			//we have totally registered two streams one is shared with noone
			//and the above one is shared with users ownself
			
			//check that we should have 2 live streams registered
			liveStreams = butterflyApp.getLiveStreams("mail@mail.com","0", "10");
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(2, length);
			
			liveStreams = butterflyApp.getLiveStreams(null,"0", "10");
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(0, length);
			
			//test with unregistered mail adress
			liveStreams = butterflyApp.getLiveStreams("unregistered string","0", "10");
			jsonArray = new JSONArray(liveStreams);
			length = jsonArray.length();
			assertEquals(0, length);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
	
	}
	
	@Test
	public void testisDeleteThrowsException_LazyLoad() {
		boolean isregistered = butterflyApp.registerUser("22", "mail@mail.com");
		assertTrue(isregistered);
		
		isregistered = butterflyApp.registerLiveStream("publisname", "publishurl", null, "mail@mail.com", true, "tur");
		assertTrue(isregistered);
	
		String liveStreams = butterflyApp.getLiveStreams("mail@mail.com","0", "10");
		
		try {
			JSONArray jsonArray = new JSONArray(liveStreams);
			assertEquals(1, jsonArray.length());
			
			JSONObject jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isDeletable"));
			assertTrue(jsonObject.getBoolean("isDeletable"));
			
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static void delete(File file) {

		if(file.isDirectory()){

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();
				System.out.println("Directory is deleted : " 
						+ file.getAbsolutePath());

			}else{

				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
					System.out.println("Directory is deleted : " 
							+ file.getAbsolutePath());
				}
			}

		}else{
			//if file, then delete it
			file.delete();
			System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}








}

