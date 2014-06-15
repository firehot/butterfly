package tests.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.red5.core.utils.JPAUtils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

/**
 * This class tests the functions that android clients call.
 * @author mekya
 *
 */
public class ApplicationRemoteBlackBoxTester {

	private static final String REG_ID2 = "21118723424242423109823jshfsjafhsksdsagagf8374sfdasfasfasf2";
	private static final String REG_ID = "21118723109823jshfsjafhskf83742";
	private static String suffix;
	private static String serverPureURL = "http://localhost:5080/ButterFly_Red5/";

	private static String serverURL = serverPureURL + "gateway";
	private static AMFConnection amfConnection;


	@BeforeClass
	public static void beforeClass() {

		try {
			delete(new File("red5/webapps/ButterFly_Red5"));


			String gradlePath = "gradle";  //"/home/faraway/softwares/gradle-1.12/bin/gradle";
			String warCommand = gradlePath + " clean war uploadArchives";

			Process process = Runtime.getRuntime().exec(warCommand, null, new File("../ButterFly_Red5/"));
			System.out.println("building and deploying ButterFly_Red5...");
			
			process.waitFor();
			
			String path = "./red5.";
			String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

			if (osName.equals("linux")) {
				suffix = "sh"; 
			}
			else {
				suffix = "bat";
			}

			path += suffix;

			closeRed5(); // if it is running somehow.
			Runtime.getRuntime().exec(path, null, new File("red5"));
			int sleepTime = 30000;
			System.out.println("Waiting " + sleepTime + "ms for letting red5 start." );
			System.out.println("You can get exception if red5 is not started fully after this while. \n So arrange this time according to your red5 startup time in your machine");
			Thread.sleep(sleepTime);
		} catch (IOException e) {
			fail("the reason of this exception may gradle not be in system-wide OS path");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}



	@AfterClass
	public static void afterClass() {
		closeRed5();
	}

	public static void closeRed5() {
		String path = "./red5-shutdown." + suffix;

		try {
			Process exec = Runtime.getRuntime().exec(path, null, new File("red5")); //new String[] {"cd red5; "+ path});
			exec.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		prepareEnvironment();
		connectToServer();
	}

	public static boolean connectToServer()  {
		amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(serverURL);
		} catch (ClientStatusException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private void prepareEnvironment() {

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
	public void tearDown() throws Exception {
		amfConnection.close();
		amfConnection = null;
	}


	@Test
	public void testRegisterUser() {
		try {
			boolean result = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@hotmail.com");
			assertTrue(result);
			result = (Boolean) amfConnection.call("registerUser", REG_ID2, "ahmetmermerkaya@hotmail.com");
			assertTrue(result);
		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testRegisterLiveStream() {

		try {
			boolean resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");
			assertTrue(resultBool);
			boolean result = (Boolean)amfConnection.call("registerLiveStream", "streamName", "string_urhgjhgkjhgl" + Math.random() *100, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(result);
		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testGetLiveStreams() {
		try {

			boolean resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");
			assertTrue(resultBool);

			String result = (String) amfConnection.call("getLiveStreams", null);
			assertNotNull(result);
			resultBool = (Boolean)amfConnection.call("registerLiveStream", "streamName", "string_urhgjhgkjhgl" + Math.random() *100, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(resultBool);

			result = (String) amfConnection.call("getLiveStreams", null);
			assertNotNull(result);
			JSONArray jsOnArray = new JSONArray(result);

			assertEquals(1, jsOnArray.length());

			result = (String) amfConnection.call("getLiveStreams", null, "0", "10");
			assertNotNull(result);
			jsOnArray = new JSONArray(result);

			assertEquals(1, jsOnArray.length());



		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (JSONException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testUpdateUser() {
		try {
			boolean result = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@hotmail.com");
			assertTrue(result);

			result = (Boolean) amfConnection.call("updateUser", "ksdfjsalkjslkdjlkjslfjsj", "ahmetmermerkaya@hotmail.com", REG_ID);
			assertTrue(result);
		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}

	}

	@Test
	public void testRegisterLocationForStream() {
		boolean resultBool;
		try {
			resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");
			assertTrue(resultBool);

			String streamURL = "string_urhgjhgkjhgl" + Math.random() *100;
			resultBool = (Boolean)amfConnection.call("registerLiveStream", "streamName", streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(resultBool);

			double longitude = 12.232323;
			double altitude = 23.343434;
			double latitude = 87.453245;
			resultBool = (Boolean) amfConnection.call("registerLocationForStream", streamURL, longitude, latitude, altitude);
			assertTrue(resultBool);

			String result = (String) amfConnection.call("getLiveStreams", "ahmetmermerkaya@gmail.com");

			JSONArray jsOnArray = new JSONArray(result);
			assertEquals(1, jsOnArray.length());

			JSONObject jsonObject = (JSONObject) jsOnArray.get(0);

			double lat = jsonObject.getDouble("latitude"); // getString("latitude");
			assertEquals(latitude, lat, 0.0000001);

			double longitu = jsonObject.getDouble("longitude");
			assertEquals(longitu, longitude, 0.0000001);

			double alti = jsonObject.getDouble("altitude");
			assertEquals(altitude, alti, 0.0000001);

			assertEquals(jsonObject.getString("url"), streamURL);

		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (JSONException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testDeleteStream() {
		Boolean resultBool;
		try {
			resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");

			assertTrue(resultBool);

			String streamURL = "string_urhgjhgkjhgl" + (int)(Math.random() *1000);
			resultBool = (Boolean)amfConnection.call("registerLiveStream", "streamName", streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(resultBool);


			Process exec = Runtime.getRuntime().exec("ffmpeg -i src/resource/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/ButterFly_Red5/" + streamURL);

			Thread.sleep(2000);

			exec.destroy();

			String streams = (String)amfConnection.call("getLiveStreams", "ahmetmermerkaya@gmail.com");
			JSONArray jsonArray = new JSONArray(streams);

			assertEquals(jsonArray.length(), 1);

			resultBool = (Boolean)amfConnection.call("deleteStream", streamURL);
			assertTrue(resultBool);

			streams = (String)amfConnection.call("getLiveStreams", "ahmetmermerkaya@gmail.com");
			jsonArray = new JSONArray(streams);

			assertEquals(jsonArray.length(), 0);


		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (JSONException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testSavePreview() {

		try {
			Boolean resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");
			assertTrue(resultBool);

			String streamURL = "string_urhgjhgkjhgl" + Math.random() *100;
			resultBool = (Boolean)amfConnection.call("registerLiveStream", "streamName", streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(resultBool);

			RandomAccessFile f = new RandomAccessFile("src/resource/1.png", "r");
			byte[] b = new byte[(int)f.length()];
			f.read(b);
			amfConnection.call("savePreview", b, streamURL);

			byte[] array = getByteArray(serverPureURL + streamURL + ".png");

			assertNotNull(array);

			//just checking it is enough length
			assertTrue(array.length > 10000);

		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testIsLiveStreamExist() {
		Boolean resultBool;
		try {
			resultBool = (Boolean) amfConnection.call("registerUser", REG_ID, "ahmetmermerkaya@gmail.com");

			assertTrue(resultBool);

			String streamURL = "string_urhgjhgkjhgl" + (int)(Math.random() *1000);
			resultBool = (Boolean)amfConnection.call("registerLiveStream", "streamName", streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(resultBool);


			Process exec = Runtime.getRuntime().exec("ffmpeg -i src/resource/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/ButterFly_Red5/" + streamURL);

			Thread.sleep(2000);

			resultBool = (Boolean)amfConnection.call("isLiveStreamExist", streamURL);
			assertTrue(resultBool);

			exec.destroy();

		} catch (ClientStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}


	public byte[] getByteArray(String address){
		try {
			URL url = new URL(address);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(15000);
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);                 
			urlConnection.connect();

			InputStream in = urlConnection.getInputStream(); //getAssets().open("kralfmtop10.htm");

			byte[] byteArray = org.apache.commons.io.IOUtils.toByteArray(in);

			in.close();

			return byteArray;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void delete(File file)
			throws IOException{

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
