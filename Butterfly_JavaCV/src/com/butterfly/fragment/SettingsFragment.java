package com.butterfly.fragment;

import twitter4j.User;
import twitter4j.auth.AccessToken;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.butterfly.R;
import com.butterfly.listeners.IAuthorizationListener;
import com.butterfly.social_media.ISocialMediaProxy;
import com.butterfly.social_media.TwitterProxy;

public class SettingsFragment extends PreferenceFragment implements IAuthorizationListener{
	
	private static final String TWITTER_IS_ENABLED = "twitter_is_enabled";
	ISocialMediaProxy twitter;
	


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		 addPreferencesFromResource(R.xml.settings);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		twitter = new TwitterProxy(getActivity(), this);
			
		Intent intent = getActivity().getIntent();
		 if (intent != null && intent.getData() != null) {
			 twitter.callbackIntent(intent);
			 
			
		 }
	}
	
	public void authorizedResult(boolean result, String type) {
		if (type.equals(twitter.getType())) {
			 CheckBoxPreference twitterIsEnabled = (CheckBoxPreference) findPreference(TWITTER_IS_ENABLED);
				
			 if (result == true) {
		 		twitterIsEnabled.setChecked(true);
			 }
			 
		}
	}
	

	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference.getKey().equals(TWITTER_IS_ENABLED)) {
			CheckBoxPreference chkBoxPref = (CheckBoxPreference) preference;
			if (chkBoxPref.isChecked() == true) {
				chkBoxPref.setChecked(false);
				twitter.login();
			}
			else {
				twitter.logout();
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}


	


	
	

}
