
package tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

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
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", true);
		assertEquals(registerLiveStream, true);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", true);
		assertEquals(registerLiveStream, false);


		registerLiveStream = butterflyApp.registerLiveStream("publishedName"+11, "publishUrl", true);
		//should return false because url is key
		assertEquals(registerLiveStream, false);

		registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl" + 11, true);
		//should return true because url is changed
		assertEquals(registerLiveStream, true);
	}

	@Test
	public void testRemoveStream() {
		boolean registerLiveStream = butterflyApp.registerLiveStream("publishedName", "publishUrl", true);
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
		
		String registerId = butterflyApp.getRegistrationId("mail1");
		assertEquals(registerId, String.valueOf(t));
		
		count = butterflyApp.getUserCount("mail1");
		assertEquals(initialCount+1, count);
		
		t = (int) (Math.random()*1000);
		result = butterflyApp.registerUser(String.valueOf(t), "mail1");
		assertEquals(true, result);
		
		registerId = butterflyApp.getRegistrationId("mail1");
		assertEquals(registerId, String.valueOf(t));
		
		count = butterflyApp.getUserCount("mail1");
		assertEquals(initialCount+1, count);

	}
	

	@Test
	public void testGetRegistrationId() {
		int t = (int) (Math.random()*1000);
		String mail = "mail@mailc.com" + t;
		boolean result = butterflyApp.registerUser(String.valueOf(t), mail);
		assertEquals(result, true);

		String registerId = butterflyApp.getRegistrationId(mail);

		assertEquals(registerId, String.valueOf(t));

		registerId = butterflyApp.getRegistrationId("slkdjflasjf" + t);

		assertEquals(registerId, null);
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
		butterflyApp.sendNotificationsOrMail("ahmetmermerkaya@gmail.com","mail;videourl","en");
	}



	






}

