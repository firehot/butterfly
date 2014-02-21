
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
import java.util.Map;

import javax.persistence.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.core.Application;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.RegIDs;
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
		Query query = JPAUtils.getEntityManager().createQuery("Delete FROM RegIDs");
		query.executeUpdate();
		query = JPAUtils.getEntityManager().createQuery("Delete FROM GcmUsers");
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
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, null, true, null);
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, null, true, null);
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName"+11, "publishUrl", null, null, true, null);
		//should return false because url is key
		assertEquals(registerLiveStream, false);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl" + 11, null, null, true, null);
		//should return true because url is changed
		assertEquals(registerLiveStream, true);
	}

	@Test
	public void testRemoveStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", null, null, true, null);
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

	@Test
	public void testUserUpdate()
	{
		int count = butterflyApp.getUserCount("mail1");
		int initialCount = count;

		int t = (int) (Math.random()*1000);
		boolean result = butterflyApp.registerUser(String.valueOf(t), "mail1");
		assertEquals(true, result);

		GcmUsers registerId = butterflyApp.getRegistrationIdList("mail1");
		assertEquals(registerId.fetchRegIDStrings().get(0), String.valueOf(t));

		count = butterflyApp.getUserCount("mail1");
		assertEquals(initialCount+1, count);

		t = (int) (Math.random()*1000);
		result = butterflyApp.registerUser(String.valueOf(t), "mail1");
		assertEquals(true, result);

		registerId = butterflyApp.getRegistrationIdList("mail1");
		assertEquals(registerId.fetchRegIDStrings().get(1), String.valueOf(t));

		count = butterflyApp.getUserCount("mail1");
		assertEquals(initialCount+1, count);

	}


	@Test
	public void testGetRegistrationId() {
		int t = (int) (Math.random()*1000);
		String mail = "mail@mailc.com" + t;
		boolean result = butterflyApp.registerUser(String.valueOf(t), mail);
		assertEquals(result, true);

		GcmUsers registerId = butterflyApp.getRegistrationIdList(mail);

		assertEquals(registerId.fetchRegIDStrings().get(0), String.valueOf(t));

		registerId = butterflyApp.getRegistrationIdList("slkdjflasjf" + t);

		assertEquals(registerId, null);
	}


	@Test
	public void testDeleteUser()
	{
		int t = (int) (Math.random()*1000);
		String mail = "murat@mailc.com" + t;
		boolean result = butterflyApp.registerUser(String.valueOf(t), mail);
		assertEquals(result, true);

		GcmUsers user = butterflyApp.getRegistrationIdList(mail);


		assertEquals(user.fetchRegIDStrings().get(0), String.valueOf(t));

		boolean opResult = butterflyApp.deleteUser(user);
		assertEquals(true, opResult);

		user = butterflyApp.getRegistrationIdList(mail);

		assertEquals(user, null);
	}

	@Test
	public void testStreamViewer() {
		Stream stream = new Stream("streamName", "streamUrl", Calendar.getInstance().getTime(), true);
		assertEquals(0, stream.getViewerCount());

		stream.addViewer("test");
		assertEquals(1, stream.getViewerCount());

		stream.removeViewer("test");
		assertEquals(0, stream.getViewerCount());

		stream.addViewer("test2");
		stream.addViewer("test3");
		stream.addViewer("test4");

		assertEquals(3, stream.getViewerCount());

		stream.removeViewer("test5");

		assertEquals(3, stream.getViewerCount());

		assertTrue(stream.containsViewer("test2"));

		assertTrue(!stream.containsViewer("test2121212"));

	}

	@Test
	public void testRegisterMultipleUser()
	{
		GcmUsers gcmUsers = new GcmUsers("mail@mail.com");
		RegIDs regid = new RegIDs("regid");
		gcmUsers.addRegID(regid);
		assertEquals(1,gcmUsers.getRegIDs().size());
		gcmUsers.addRegID(regid);
		assertEquals(1,gcmUsers.getRegIDs().size());
	}

	@Test
	public void testRegisterLocationForStream() {
		Map<String, Stream> registeredStreams = butterflyApp.getRegisteredStreams();
		assertEquals(0, registeredStreams.size());
		registeredStreams.put("video_url", new Stream("location_test", "video_url", Calendar.getInstance().getTime(), true));

		assertEquals(1, registeredStreams.size());

		butterflyApp.registerLocationForStream("video_url", 23.4566, 34.667, 100);

		registeredStreams = butterflyApp.getRegisteredStreams();
		assertEquals(1, registeredStreams.size());

		Stream stream = registeredStreams.get("video_url");
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
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", null, null, true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f2", null, null, true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", null, null, true, "tur");
			assertTrue(registered);
			
			assertEquals(3, butterflyApp.getRegisteredStreams().size());
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

		assertEquals(0, butterflyApp.getRegisteredStreams().size());
		
		try {
			f1.createNewFile();
			f2.createNewFile();
			f3.createNewFile();
			boolean registered = butterflyApp.registerLiveStream("streamName", "f1", null, null, true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f2", null, null, true, "tur");
			assertTrue(registered);
			registered = butterflyApp.registerLiveStream("streamName", "f3", null, null, true, "tur");
			assertTrue(registered);
			
			assertEquals(3, butterflyApp.getRegisteredStreams().size());

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
	public void testSaveStream()
	{
		Stream strm = new Stream("deneme", "denemeurl", Calendar.getInstance().getTime(), true);
		strm.broadcasterMail = "fdsf";
		butterflyApp.streamManager.saveStream(strm);
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

