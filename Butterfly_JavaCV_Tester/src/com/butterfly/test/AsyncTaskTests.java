package com.butterfly.test;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.ViewAsserts;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.message.CloudMessaging;
import com.butterfly.tasks.AbstractAsyncTask;
import com.butterfly.tasks.CheckStreamExistTask;
import com.butterfly.tasks.DeleteStreamTask;
import com.butterfly.tasks.GetStreamListTask;
import com.butterfly.tasks.RegisterLocationForStreamTask;
import com.butterfly.tasks.RegisterStreamTask;
import com.butterfly.tasks.RegisterUserTask;
import com.butterfly.tasks.SendPreviewTask;
import com.butterfly.tasks.UpdateUserRegisterIdTask;

public class AsyncTaskTests extends ActivityInstrumentationTestCase2 {

	//Genymotion
	private static final String HTTP_GATEWAY_URL = "http://10.0.3.2:5080/ButterFly_Red5/gateway";
	
	//Android emulator
	//private static final String HTTP_GATEWAY_URL = "http://10.0.2.2:5080/ButterFly_Red5/gateway";
	
	private MainActivity mainActivity;
	protected boolean mainActivityOnPausedCalled = false;
	protected boolean recordActivityStarted = false;
	private boolean preExecuteCalled;
	private boolean postExecuteCalled;
	private UpdateUserRegisterIdTask updateRegisterIdTask;

	private AbstractAsyncTask asyncTask;


	public AsyncTaskTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mainActivity = (MainActivity) getActivity();
		mainActivityOnPausedCalled = false;
		recordActivityStarted = false;
		preExecuteCalled = false;
		postExecuteCalled = false;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		
		mainActivity = null;
	}

/*	
	public void testContactSearch() {
		ViewPager viewPager = (ViewPager)mainActivity.findViewById(R.id.pager);
		AppSectionsPagerAdapter adapter = (AppSectionsPagerAdapter) viewPager.getAdapter();
		int count = adapter.getCount();
		assertEquals(2, count);
		
		ContactsListFragment contactFragment = (ContactsListFragment) adapter.getItem(0);
		
		View contactView = contactFragment.getView();
		final EditText searchEditText = (EditText)contactView.findViewById(R.id.searchText);
		
		mainActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				searchEditText.setText("gmail.com");
			}
		});
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		final ListView lv = (ListView) contactView.findViewById(R.id.selectedContactList);
		
		int childCount = lv.getChildCount();
		assertTrue(childCount > 0);
		
		mainActivity.getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			
			@Override
			public void onActivityStopped(Activity arg0) {}
			
			@Override
			public void onActivityStarted(Activity activity) {
				if (activity.getClass().equals(RecordActivity.class)) {
					recordActivityStarted = true;
				}
			}
			
			@Override
			public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {}
			
			@Override
			public void onActivityResumed(Activity arg0) {}
			
			@Override
			public void onActivityPaused(Activity activity) {
				if (activity.getClass().equals(MainActivity.class)) {
					mainActivityOnPausedCalled  = true;
				}
			}
			
			@Override
			public void onActivityDestroyed(Activity arg0) {}
			
			@Override
			public void onActivityCreated(Activity arg0, Bundle arg1) {}
		});
		 
		
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lv.performItemClick(lv.getAdapter().getView(0, null, null), 0, lv.getAdapter().getItemId(0));
				searchEditText.setText("gmail.co");
			}
		});
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 assertTrue(mainActivityOnPausedCalled);
		 assertTrue(recordActivityStarted);
	}
*/	
	
	//Test: Giving null does not throw any exception, 
	public void testGetStreamListTaskExceptionCase() {
		
		GetStreamListTask  getStreamListTask = new GetStreamListTask(null, getActivity());
		getStreamListTask.execute("0", "10");
		waitForTask(getStreamListTask);
		
		preExecuteCalled = false;
		postExecuteCalled = false;
		getStreamListTask = new GetStreamListTask(new IAsyncTaskListener() {
			
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertNull(object);
				postExecuteCalled = true;
			}
		}, getActivity());

		GetStreamListTask.setHttpGatewayURL(null);
		getStreamListTask.execute("0", "10");
		
		waitForTask(getStreamListTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}
	
	public void testGetStreamListTaskNormalCase() {
		
		GetStreamListTask getStreamListTask = new GetStreamListTask(new IAsyncTaskListener() {
			
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertNotNull(object);
				assertTrue(object instanceof List);
				postExecuteCalled = true;
			}
		}, getActivity());

		GetStreamListTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		getStreamListTask.execute("0", "10");
		
		waitForTask(getStreamListTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}
	
	
	public void testRegisterStreamTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		RegisterStreamTask registerStreamTask = new RegisterStreamTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		RegisterStreamTask.setHttpGatewayURL(null);
		registerStreamTask.execute(null, null);
		
		waitForTask(registerStreamTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}
	
	public void testRegisterStreamTaskNormalCase() {
		
		preExecuteCalled = false;
		postExecuteCalled = false;
		asyncTask = new RegisterUserTask(new IAsyncTaskListener() {
			
			@Override
			public void onProgressUpdate(Object... progress) {
				
			}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertNotNull(object);
				assertTrue(object instanceof String);
				
				assertTrue(((RegisterUserTask)asyncTask).isRegistered());
				postExecuteCalled = true;
			}
		}, getActivity());
		
		asyncTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		((RegisterUserTask)asyncTask).setRegisterId("1231kjsldkjs343434");
		((RegisterUserTask)asyncTask).execute("SENDER_IDDD_Meaningless", "deneme@deneme.com,test@test.com");
		
		waitForTask(asyncTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);


		preExecuteCalled = false;
		postExecuteCalled = false;
		RegisterStreamTask registerStreamTask = new RegisterStreamTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertTrue(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		RegisterStreamTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		registerStreamTask.execute("streamName"+(int)(Math.random()*1000), "streamURL"+(int)(Math.random()*1000), "deneme@deneme.com", "deneme@deneme.com", "true");
		
		waitForTask(registerStreamTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}

	
	public void testCheckStreamTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		CheckStreamExistTask checkStreamTask = new CheckStreamExistTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		CheckStreamExistTask.setHttpGatewayURL(null);
		checkStreamTask.execute(null, null);
		waitForTask(checkStreamTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}
	
	public void testCheckStreamTaskNormalCase() {
		
		
		CheckStreamExistTask checkStreamTask = new CheckStreamExistTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		CheckStreamExistTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		checkStreamTask.execute("non existed url");
		waitForTask(checkStreamTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}
	
	public void testDeleteStreamTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		DeleteStreamTask deleteStreamTask = new DeleteStreamTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		DeleteStreamTask.setHttpGatewayURL(null);
		deleteStreamTask.execute(null, null);
		
		waitForTask(deleteStreamTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}
	
	public void testRegisterLocationForStreamTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		RegisterLocationForStreamTask task = new RegisterLocationForStreamTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		AbstractAsyncTask.setHttpGatewayURL(null);
		task.execute(null, null);
		waitForTask(task);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
		
	}
	
	public void testRegisterUserTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		RegisterUserTask task = new RegisterUserTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertNull(object);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		AbstractAsyncTask.setHttpGatewayURL(null);
		task.execute(null, null);
		waitForTask(task);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}
	
	public void tttestRegisterUserTaskScenario1() {

		SharedPreferences preferences = mainActivity.getApplicationPrefs();
		Editor editor = preferences.edit();
		editor.clear();
		editor.commit();
		
		preferences = mainActivity.getGcmMessenger().getGCMPreferences(mainActivity, mainActivity.getClass());
		editor = preferences.edit();
		editor.clear();
		editor.commit();

			
		mainActivity.finish();
		
		mainActivity = null;
		mainActivity = (MainActivity) getActivity();

		getInstrumentation().waitForIdleSync();
		mainActivity = (MainActivity) getActivity();
		
		final AlertDialog dialog = mainActivity.getTermsOfUseDialog();
		assertTrue(dialog.isShowing());
		
		final ListView mailLV = (ListView) dialog.findViewById(R.id.mail_addresses);
		
		assertTrue(mailLV.isShown());
		
		try {
			runTestOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mailLV.setItemChecked(0, true);
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

				}
			});
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		getInstrumentation().waitForIdleSync();
		
		assertTrue(mainActivity.getGcmMessenger().getAsyncTask() instanceof RegisterUserTask);
	
	}
	
	public void testSendPreviewTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		SendPreviewTask task = new SendPreviewTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				boolean result = (Boolean) object;
				assertFalse(result);
				postExecuteCalled = true;
			}
		}, getActivity());
		
		AbstractAsyncTask.setHttpGatewayURL(null);
		task.execute(null, null);
		
		waitForTask(task);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}
	
	public void testUpdateUserRegisteredIdTaskExceptionCase() {
		
		//Test: onPostExecute object value should be false if an exception occurs in operation
		updateRegisterIdTask = new UpdateUserRegisterIdTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				
				assertFalse(updateRegisterIdTask.isRegistered());
				postExecuteCalled = true;
			}
		}, getActivity());
		
		AbstractAsyncTask.setHttpGatewayURL(null);
		updateRegisterIdTask.execute(null, null);
		
		waitForTask(updateRegisterIdTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}

	public void testUpdateUserRegisteredIdTaskNormalCase() {
		
		preExecuteCalled = false;
		postExecuteCalled = false;
		asyncTask = new RegisterUserTask(new IAsyncTaskListener() {
			
			@Override
			public void onProgressUpdate(Object... progress) {
				
			}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertNotNull(object);
				assertTrue(object instanceof String);
				
				assertTrue(((RegisterUserTask)asyncTask).isRegistered());
				postExecuteCalled = true;
			}
		}, getActivity());
		
		asyncTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		String mailAddress = "deneme"+Math.random()*1000+"@deneme.com";
		String registerId = "1231kjsldkjs343434"+Math.random()*1000;
		((RegisterUserTask)asyncTask).setRegisterId(registerId);
		((RegisterUserTask)asyncTask).execute("SENDER_IDDD_Meaningless", mailAddress);
		
		waitForTask(asyncTask);
		
		
		updateRegisterIdTask = new UpdateUserRegisterIdTask(new IAsyncTaskListener() {
			@Override
			public void onProgressUpdate(Object... progress) {}
			
			@Override
			public void onPreExecute() {
				preExecuteCalled = true;
			}
			
			@Override
			public void onPostExecute(Object object) {
				assertTrue(updateRegisterIdTask.isRegistered());
				postExecuteCalled = true;
			}
		}, getActivity());
		
		AbstractAsyncTask.setHttpGatewayURL(HTTP_GATEWAY_URL);
		updateRegisterIdTask.setNewRegisterId("meaningless register id");
		updateRegisterIdTask.execute("meaningless register id", mailAddress, registerId);
		
		waitForTask(updateRegisterIdTask);
		
		assertTrue(preExecuteCalled);
		assertTrue(postExecuteCalled);
	}

	
	

	private void waitForTask(AsyncTask task) {
		while (task.getStatus() != AsyncTask.Status.FINISHED) {
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


}
