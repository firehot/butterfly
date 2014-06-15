package com.butterfly;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragment {

	private static final String TWITTER_IS_ENABLED = "twitter_is_enabled";
	private static final String FACEBOOK_IS_ENABLED = "facebook_is_enabled";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		SettingsActivity activity = (SettingsActivity) getActivity();
		if (preference.getKey().equals(TWITTER_IS_ENABLED)) {
			CheckBoxPreference chkBoxPref = (CheckBoxPreference) preference;
			if (chkBoxPref.isChecked() == true) {

				loginTwitter(activity);
				logoutFacebook(activity);

			} else {
				logoutTwitter(activity);
			}
		} else if (preference.getKey().equals(FACEBOOK_IS_ENABLED)) {
			CheckBoxPreference chkBoxPref = (CheckBoxPreference) preference;
			if (chkBoxPref.isChecked() == true) {
				loginFacebook(activity);
				logoutTwitter(activity);
			} else {
				logoutFacebook(activity);
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	private void loginTwitter(SettingsActivity activity) {
		if (!activity.mSocialNetworkManager.getTwitterSocialNetwork()
				.isConnected())
		{
			activity.mSocialNetworkManager.getTwitterSocialNetwork()
					.requestLogin();
			CheckBoxPreference twitterCheck = (CheckBoxPreference) findPreference(TWITTER_IS_ENABLED);
			twitterCheck.setChecked(true);
		}
	}

	private void logoutTwitter(SettingsActivity activity) {
		if (activity.mSocialNetworkManager.getTwitterSocialNetwork()
				.isConnected())
		{
			activity.mSocialNetworkManager.getTwitterSocialNetwork().logout();
			CheckBoxPreference twitterCheck = (CheckBoxPreference) findPreference(TWITTER_IS_ENABLED);
			twitterCheck.setChecked(false);
		}
	}

	private void loginFacebook(SettingsActivity activity) {
		if (!activity.mSocialNetworkManager.getFacebookSocialNetwork()
				.isConnected())
		{
			activity.mSocialNetworkManager.getFacebookSocialNetwork()
					.requestLogin();
			CheckBoxPreference facebookCheck = (CheckBoxPreference) findPreference(FACEBOOK_IS_ENABLED);
			facebookCheck.setChecked(true);
		}
	}

	private void logoutFacebook(SettingsActivity activity) {
		if (activity.mSocialNetworkManager.getFacebookSocialNetwork()
				.isConnected()) {
			activity.mSocialNetworkManager.getFacebookSocialNetwork().logout();
			CheckBoxPreference facebookCheck = (CheckBoxPreference) findPreference(FACEBOOK_IS_ENABLED);
			facebookCheck.setChecked(false);
		}
	}

}