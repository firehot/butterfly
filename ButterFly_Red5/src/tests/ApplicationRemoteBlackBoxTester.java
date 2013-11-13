package tests;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	private String serverURL = "http://localhost:5080/ButterFly_Red5/gateway";
	private AMFConnection amfConnection;

	@Before
	public void setUp() throws Exception {
		amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		amfConnection.connect(serverURL);
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
			double count = (Double)amfConnection.call("getUserCount", "ahmetmermerkaya@hotmail.com");
			assertEquals((int)count, 1);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testRegisterLiveStream() {
		
		try {
			boolean result = (Boolean)amfConnection.call("registerLiveStream", "streamName", "string_urhgjhgkjhgl" + Math.random() *100, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "tur");
			assertTrue(result);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testUpdateUser() {
		try {
			boolean result = (Boolean) amfConnection.call("updateUser", "ksdfjsalkfjsaşlkfjsaşlfj", "ahmetmermerkaya@hotmail.com", REG_ID);
			assertTrue(result);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		
	}

}
