package com.butterfly;

import com.butterfly.social_media.Twitter;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	private Twitter twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		twitter = new Twitter(this);
		
		Thread local = new Thread() {
			public void run() {
				twitter.login();
			};
		};
		local.start();
		
		
	}
}
