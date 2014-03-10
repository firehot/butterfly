
package tests.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.jruby.compiler.ir.operands.Array;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.core.Application;
import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIds;
import org.red5.core.dbModel.Stream;
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
		Query query = JPAUtils.getEntityManager().createQuery("Delete FROM RegIds");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM GcmUserMails");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM GcmUsers");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM Stream");
		query.executeUpdate();		
		JPAUtils.commit();
		JPAUtils.closeEntityManager();

	}

	@After
	public void after() {


		butterflyApp.getBandwidthServer().close();
		butterflyApp = null;
	}

	@Test
	public void testRegisterStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName"+11, "publishUrl", null, "mail@mail.com", true, null);
		//should return false because url is key
		assertEquals(registerLiveStream, false);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl" + 11, null, "mail@mail.com", true, null);
		//should return true because url is changed
		assertEquals(registerLiveStream, true);
	}

	@Test
	public void testRemoveStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, "mail@mail.com", true, null);
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.removeStream("publishUrl");
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.removeStream("publishUrl" + 1234);
		assertEquals(registerLiveStream, false);
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
	public void testRegisterCommaSeparatedMails() {
		String id = "123132131";
		boolean isRegistered = butterflyApp.registerUser(id, "hasan@hasan.com,salih@salih.com,okan@okan.com");
		assertEquals(isRegistered, true);
		
		Query query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "hasan@hasan.com");
		GcmUserMails userMail = (GcmUserMails) query.getSingleResult();
		
		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);
		
		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "salih@salih.com");
		userMail = (GcmUserMails) query.getSingleResult();
		
		assertEquals(userMail.getGcmUsers().getRegIdses().iterator().next().getGcmRegId(), id);
		
		query = JPAUtils.getEntityManager().createQuery("FROM GcmUserMails WHERE mail=:mail");
		query.setParameter("mail", "okan@okan.com");
		userMail = (GcmUserMails) query.getSingleResult();
		
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

		GcmUsers user = butterflyApp.userManager.getGcmUserByMail(mail);

		boolean opResult = butterflyApp.deleteRegId(String.valueOf(t));
		assertEquals(true, opResult);

		user = butterflyApp.userManager.getGcmUserByRegId(String.valueOf(t));
				

		assertEquals(user, null);
	}

	@Test
	public void testRegisterMultipleUser()
	{
		boolean registerUser = butterflyApp.registerUser("deneme", "deneme@deneme.com");
		assertTrue(registerUser);

		registerUser = butterflyApp.registerUser("deneme", "deneme@deneme.com");
		assertTrue(!registerUser);
	}

	@Test
	public void testRegisterLocationForStream() {
		List<Stream> streamList = butterflyApp.streamManager.getAllStreamList();
		assertEquals(0, streamList.size());
		
		Stream strm =  new Stream("location_test", "video_url", Calendar.getInstance().getTime(), true);
		strm.setBroadcasterMail("mail@mail.com");
		butterflyApp.streamManager.saveStream(strm);

		streamList = butterflyApp.streamManager.getAllStreamList();
		assertEquals(1, streamList.size());

		butterflyApp.registerLocationForStream("video_url", 23.4566, 34.667, 100);

		streamList = butterflyApp.streamManager.getAllStreamList();
		assertEquals(1, streamList.size());

		Stream stream = butterflyApp.streamManager.getStream("video_url");
		assertNotNull(stream);
		assertEquals(23.4566, stream.longitude, 1e-8);
		assertEquals(34.667, stream.latitude, 1e-8);
		assertEquals(100, stream.altitude, 1e-8);

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
		while(butterflyApp.isMailsSent() == false) {
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
	public void testcheckClientBandwidht() {
		byte[] data = new byte[20480];
		long currentTimeMillis = System.currentTimeMillis();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int checkClientBandwidth = butterflyApp.checkClientBandwidth(currentTimeMillis, data.length, data);

		System.out.println(" check client bandwidth -> " + checkClientBandwidth);

	}

	@Test
	public void testBandwidthServer() {
		try {
			Thread.sleep(100);
			Socket socket = new Socket("127.0.0.1", 53000);
			socket.setTcpNoDelay(true);

			OutputStream outputStream = socket.getOutputStream();
			byte[] data = new byte[2048000];
			String time = String.valueOf(System.currentTimeMillis()) + ";";
			outputStream.write(time.getBytes(), 0, time.getBytes().length);
			outputStream.write(data, 0, data.length);
			outputStream.write("\n".getBytes());			
			outputStream.flush();

			InputStream istr = socket.getInputStream();
			int length = 0;
			while ((length = istr.read(data, 0, data.length)) > 0) {
				String bandwidth = new String(data, 0, length);
				System.out.println("**************** bandwidth -> " + bandwidth);
				if (data[length-1] == '\n') {
					break;
				}
			}

			istr.close();
			outputStream.close();
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testStreamDeleter() {
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
		try {
			f1.createNewFile();
			f2.createNewFile();
			f3.createNewFile();
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", null, "mail@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f2", null, "mail@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", null, "mail@mail.com", true, "tur");
			assertTrue(registered);
			assertEquals(3, butterflyApp.getLiveStreamProxies().size());

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		assertEquals(f1.exists(), true);
		assertEquals(f2.exists(), true);
		assertEquals(f3.exists(), true);

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

		assertEquals(0, butterflyApp.getLiveStreamProxies().size());

		try {
			f1.createNewFile();
			f2.createNewFile();
			f3.createNewFile();
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", null, "mail1@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f2", null, "mail2@mail.com", true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", null, "mail3@mail.com", true, "tur");
			assertTrue(registered);
			
			assertEquals(3, butterflyApp.getLiveStreamProxies().size());

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		assertEquals(f1.exists(), true);
		assertEquals(f2.exists(), true);
		assertEquals(f3.exists(), true);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(f1.exists(), false);
		assertEquals(f2.exists(), false);
		assertEquals(f3.exists(), false);

	}

	@Test
	public void testSaveUpdateDeleteStream()
	{
		Stream strm = new Stream("deneme", "denemeurl", Calendar.getInstance().getTime(), true);
		strm.broadcasterMail = "fdsf";
		butterflyApp.streamManager.saveStream(strm);

		Stream createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertNotNull(createdStream);

		strm.altitude = 1300;
		butterflyApp.streamManager.updateStream(strm);

		createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertNotNull(createdStream);
		assertEquals(createdStream.altitude, 1300,1);

		butterflyApp.streamManager.deleteStream(strm);
		createdStream = butterflyApp.streamManager.getStream("denemeurl");
		assertEquals(createdStream,null);

	}

	@Test
	public void testGetAllStreamList()
	{
		List<Stream> streamList = butterflyApp.streamManager.getAllStreamList();
		int streamCount = streamList.size();
		
		Stream strm1 = new Stream("stream1", "stream1url", Calendar.getInstance().getTime(), true);
		strm1.broadcasterMail = "stream1mail";
		butterflyApp.streamManager.saveStream(strm1);	
		Stream createdStream = butterflyApp.streamManager.getStream("stream1url");
		assertNotNull(createdStream);
		
		Stream strm2 = new Stream("stream2", "stream2url", Calendar.getInstance().getTime(), true);
		strm2.broadcasterMail = "stream2mail";
		butterflyApp.streamManager.saveStream(strm2);
		createdStream = butterflyApp.streamManager.getStream("stream2url");
		assertNotNull(createdStream);
		
		streamList = butterflyApp.streamManager.getAllStreamList();
		assertEquals(streamCount+2, streamList.size());
		
		
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

