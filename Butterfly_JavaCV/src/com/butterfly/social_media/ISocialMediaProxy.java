package com.butterfly.social_media;

import android.content.Intent;

public interface ISocialMediaProxy {

	public void login();
	
	public void logout();
	
	public void updateStatus(String text, String videoId);
	
	public boolean isAuthorized();
	
	public void callbackIntent(Intent intent);
	
	public String getType();
}
