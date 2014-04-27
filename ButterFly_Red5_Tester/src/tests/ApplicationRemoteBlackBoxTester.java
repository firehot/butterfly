package tests;

import static org.junit.Assert.*;

import javax.persistence.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.core.utils.JPAUtils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import flex.messaging.util.concurrent.FailedExecutionHandler;

/**
 * This class tests the functions that android clients call.
 * @author mekya
 *
 */
public class ApplicationRemoteBlackBoxTester {

	private static final String REG_ID2 = "21118723424242423109823jshfsjafhsksdsagagf8374sfdasfasfasf2";
	private static final String REG_ID = "21118723109823jshfsjafhskf83742";
	private String serverURL = "http://localhost:5080/ButterFly_Red5/gateway";
	private AMFConnection amfConnection;

	@Before
	public void setUp() throws Exception {
		prepareEnvironment();
		amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		amfConnection.connect(serverURL);

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

}
