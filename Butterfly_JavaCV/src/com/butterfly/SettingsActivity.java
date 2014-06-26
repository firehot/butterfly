package com.butterfly;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;

import com.androidsocialnetworks.lib.SocialNetwork;
import com.androidsocialnetworks.lib.impl.FacebookSocialNetwork;
import com.butterfly.fragment.SettingsFragment;

public class SettingsActivity extends BaseSocialMediaActivity {

	public static String FRAGMENT_TAG ="FRAGMENT_TAG";
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content,new SettingsFragment(),FRAGMENT_TAG).commit();

	}

	@Override
	public void onError(int socialNetworkID, String requestID,
			String errorMessage, Object data) {
		

	}

	@Override
	public void onLoginSuccess(int socialNetworkID) {
		
		PreferenceFragment prefFrag = (PreferenceFragment)this.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		
		if(FacebookSocialNetwork.ID == socialNetworkID)
		{
			//if event is from facebook
			CheckBoxPreference faceCheck = (CheckBoxPreference)prefFrag.findPreference(SettingsFragment.FACEBOOK_IS_ENABLED);
			faceCheck.setChecked(true);
		}
		else
		{
			//event is from twitter
			CheckBoxPreference twitterCheck = (CheckBoxPreference)prefFrag.findPreference(SettingsFragment.TWITTER_IS_ENABLED);
			twitterCheck.setChecked(true);
		}

	}

	@Override
	public void onSocialNetworkManagerInitialized() {

		for (SocialNetwork socialNetwork : mSocialNetworkManager
				.getInitializedSocialNetworks()) {
			socialNetwork.setOnLoginCompleteListener(this);
		}


	}

	@Override
	public void onPostSuccessfully(int socialNetworkID) {

	}

}