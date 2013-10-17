
package tests;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.core.Application;

public class ApplicationTester {

	Application butterflyApp;
	@Before
	public void before() {
		butterflyApp = new Application();
	}

	@After
	public void after() {
		butterflyApp = null;
	}

	@Test
	public void testRegisterStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl");
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl");
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName"+11, "publishUrl");
		//should return false because url is key
		assertEquals(registerLiveStream, false);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl" + 11);
		//should return true because url is changed
		assertEquals(registerLiveStream, true);
	}

	@Test
	public void testRemoveStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl");
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.removeStream("publishUrl");
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.removeStream("publishUrl" + 1234);
		assertEquals(registerLiveStream, false);
	}

	@Test
	public void testRegisterUser() {
		int t = (int) (Math.random()*1000);
		boolean result = butterflyApp.registerUser(t, "dgsdgs");
		assertEquals(true, result);
	}


	@Test
	public void testGetRegistrationId() {
		int t = (int) (Math.random()*1000);
		String mail = "mail@mailc.com" + t;
		boolean result = butterflyApp.registerUser(t, mail);
		assertEquals(result, true);

		int registerId = butterflyApp.getRegistrationId(mail);

		assertEquals(registerId, t);

		registerId = butterflyApp.getRegistrationId("slkdjflasjf" + t);

		assertEquals(registerId, 0);
	}
	
//	@Test
//	public void testSendMail() {
//		ArrayList<String> mail = new ArrayList<String>();
//		mail.add("ahmetmermerkaya@gmail.com");
//		butterflyApp.sendMail(mail, "Test mail ", "Bu bir test mailidir.");
//		
//		String mailString = new String();
//		for (int i = 0; i < mail.size(); i++) {
//			mailString += mail.get(i) + ",";
//		}
//		fail("to correct this test check mail is received at " + mailString);
//	}
	
	@Test
	public void testSendNotificationOrMail()
	{
		butterflyApp.sendNotificationsOrMail("ahmetmermerkaya@gmail.com");
	}



	






}

