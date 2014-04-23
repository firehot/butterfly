package com.butterfly;

import com.butterfly.fragment.SettingsFragment;
import com.butterfly.social_media.TwitterProxy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends Activity {

	private TwitterProxy twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		 getFragmentManager().beginTransaction().replace(android.R.id.content,
	                new SettingsFragment()).commit();
		 
	}

}
