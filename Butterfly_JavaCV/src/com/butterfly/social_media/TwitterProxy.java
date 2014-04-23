package com.butterfly.social_media;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.butterfly.R;
import com.butterfly.listeners.IAuthorizationListener;

public class TwitterProxy implements ISocialMediaProxy {
	
	private static final String WEB_PLAY_URL = "http://www.butterflytv.net/player.html?videoId=";


	private static final String TWITTER_CONSUMER_KEY = "EQ5q7KJR6CGs1O47bqgxnaSZB";
	private static final String TWITTER_CONSUMER_SECRET = "SGiyaFQErnA7m2jjpEohsfPA7j7r3TE5SISWTus97Zw4s5KkS0";
	private static final String TWITTER_CALLBACK_URL = "oauth://butterfly_twitter_login";

	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	public static final String PREF_KEY_REQUEST_TOKEN = "request_token";
	public static final String PREF_KEY_REQUEST_SECRET = "request_secret";

	private static final String TYPE = "twitter";

	public Activity mActivity;
	private SharedPreferences mSharedPreferences;
	private TwitterFactory twitterFactory;
	private IAuthorizationListener mAuthorizationListener;

	public String getType() {
		return TYPE;
	}
	
	public TwitterProxy(Activity activity) {
		this.mActivity = activity;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		
		String secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, null);
		String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null);

		if (secret != null && token != null) {
			builder.setOAuthAccessToken(token);
			builder.setOAuthAccessTokenSecret(secret);
		}
		
		
		Configuration configuration = builder.build();

		twitterFactory = new TwitterFactory(configuration);
	}

	public TwitterProxy(Activity activity, IAuthorizationListener listener) {
		this(activity);
		this.mAuthorizationListener = listener;
	}

	private Twitter getTwitter() {
		return twitterFactory.getInstance();
	}

	public void login() {
		new GetRequestTokenTask().execute();
	}



	public void updateStatus(String text, String url) {
//		// Access Token 
//		String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
//		// Access Token Secret
//		String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
//
//		AccessToken accessToken = new AccessToken(access_token, access_token_secret);
//		getTwitter().setOAuthAccessToken(accessToken);

		url = WEB_PLAY_URL + url;
		// Update status
		try {
			twitter4j.Status response = getTwitter().updateStatus(text +" " + url);
		} catch (TwitterException e) {
			e.printStackTrace();
		}

	}

	public void callbackIntent(Intent intent) {
		new GetAccessTokenTask().execute(intent.getData());
	}

	public boolean isAuthorized() {
		// return twitter login status from Shared Preferences
		return PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}


	public class GetRequestTokenTask extends AsyncTask<String, Integer, String> {

		private ProgressDialog showDialog;

		@Override
		protected void onPreExecute() {
			showDialog = ProgressDialog.show(mActivity, null, null, true, false);
			showDialog.setContentView(R.layout.progress_bar);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			RequestToken requestToken;
			try {
				requestToken = getTwitter()
						.getOAuthRequestToken(TWITTER_CALLBACK_URL);

				Editor e = mSharedPreferences.edit();
				// After getting access token, access token secret
				// store them in application preferences
				e.putString(PREF_KEY_REQUEST_TOKEN, requestToken.getToken());
				e.putString(PREF_KEY_REQUEST_SECRET, requestToken.getTokenSecret());
				e.commit(); // save changes

			} catch (TwitterException e) {
				e.printStackTrace();
				return null;
			}
			return requestToken.getAuthenticationURL();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (showDialog != null && showDialog.isShowing() == true) {
				showDialog.dismiss();
			}

			if (result != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

				mActivity.startActivity(intent); 
				mActivity.finish();


			}

		}

	}

	public class GetAccessTokenTask extends AsyncTask<Uri, Integer, Boolean> {

		private ProgressDialog showDialog;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog = ProgressDialog.show(mActivity, null, null, true, false);
			showDialog.setContentView(R.layout.progress_bar);

		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (showDialog != null && showDialog.isShowing() == true) {
				showDialog.dismiss();
			}

			if (mAuthorizationListener != null) {
				mAuthorizationListener.authorizedResult(true, getType());
			}

		}
		@Override
		protected Boolean doInBackground(Uri... params) {
			Uri uri = params[0];
			if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
				// oAuth verifier
				String verifier = uri
						.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

				try {
					String secret = mSharedPreferences.getString(PREF_KEY_REQUEST_SECRET, "");
					String token = mSharedPreferences.getString(PREF_KEY_REQUEST_TOKEN, "");

					RequestToken reqToken = new RequestToken(token, secret);
					// Get the access token
					AccessToken accessToken = getTwitter().getOAuthAccessToken(
							reqToken, verifier);


					// Shared Preferences
					Editor e = mSharedPreferences.edit();

					// After getting access token, access token secret
					// store them in application preferences
					e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
					e.putString(PREF_KEY_OAUTH_SECRET,
							accessToken.getTokenSecret());
					// Store login status - true
					e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
					e.commit(); // save changes

					Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

					return true;
				} catch (Exception e) {
					// Check log for login errors
					Log.e("Twitter Login Error", "> " + e.getMessage());
				}
			}
			return false;
		}

	}

	@Override
	public void logout() {
		Editor e = mSharedPreferences.edit();
		e.remove(PREF_KEY_REQUEST_SECRET);
		e.remove(PREF_KEY_REQUEST_TOKEN);
		e.remove(PREF_KEY_OAUTH_TOKEN);
		e.remove(PREF_KEY_OAUTH_SECRET);
		e.remove(PREF_KEY_TWITTER_LOGIN);
		e.commit();

	}



}
