package tests;

import static org.junit.Assert.*;

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

}
