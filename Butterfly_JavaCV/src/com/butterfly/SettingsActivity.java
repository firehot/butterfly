package com.butterfly;

import android.os.Bundle;

import com.androidsocialnetworks.lib.SocialNetwork;

public class SettingsActivity extends BaseSocialMediaActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();

	}

	@Override
	public void onError(int socialNetworkID, String requestID,
			String errorMessage, Object data) {
		

	}

	@Override
	public void onLoginSuccess(int socialNetworkID) {
		

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