package com.butterfly;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.androidsocialnetworks.lib.SocialNetworkManager;
import com.androidsocialnetworks.lib.SocialNetworkManager.OnInitializationCompleteListener;
import com.androidsocialnetworks.lib.listener.OnLoginCompleteListener;
import com.androidsocialnetworks.lib.listener.OnPostingCompleteListener;

public class BaseSocialMediaActivity extends FragmentActivity implements OnLoginCompleteListener, OnInitializationCompleteListener,
OnPostingCompleteListener {

	public SocialNetworkManager mSocialNetworkManager;
	protected final String SOCIAL_NETWORK_TAG = "Butterfly.Twitter";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSocialNetworkManager = (SocialNetworkManager) getSupportFragmentManager()
				.findFragmentByTag(SOCIAL_NETWORK_TAG);

		if (mSocialNetworkManager == null) {
			mSocialNetworkManager = SocialNetworkManager.Builder
					.from(this)
					.twitter("20oKJfbxE643Y9qvrCDIsGGL4","HfTVma7WnTuju5SkeC28sOSagiywaFHn7yuyNxliwCaiP567Ee")
							.facebook()
					.build();
			getSupportFragmentManager().beginTransaction()
					.add(mSocialNetworkManager, SOCIAL_NETWORK_TAG).commit();
		}
		
		mSocialNetworkManager.setOnInitializationCompleteListener(this);
	}

	@Override
	public void onError(int socialNetworkID, String requestID,
			String errorMessage, Object data) {

		
	}

	@Override
	public void onPostSuccessfully(int socialNetworkID) {

		
	}

	@Override
	public void onSocialNetworkManagerInitialized() {

		
	}

	@Override
	public void onLoginSuccess(int socialNetworkID) {

		
	}
}
