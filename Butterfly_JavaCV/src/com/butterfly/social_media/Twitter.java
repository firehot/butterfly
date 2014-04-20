package com.butterfly.social_media;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Twitter {
	
	private static final String TWITTER_CONSUMER_KEY = "EQ5q7KJR6CGs1O47bqgxnaSZB";
	private static final String TWITTER_CONSUMER_SECRET = "SGiyaFQErnA7m2jjpEohsfPA7j7r3TE5SISWTus97Zw4s5KkS0";
	private static final String TWITTER_CALLBACK_URL = "oauth://butterfly_twitter_login";
	private twitter4j.Twitter twitter;
	private RequestToken requestToken;
	
	public Context context;
	
	public Twitter(Context ctx) {
		this.context = ctx;
	}

	public void login() {
		
		 ConfigurationBuilder builder = new ConfigurationBuilder();
         builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
         builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
         Configuration configuration = builder.build();
          
         TwitterFactory factory = new TwitterFactory(configuration);
         twitter = factory.getInstance();

         try {
             requestToken = twitter
                     .getOAuthRequestToken(TWITTER_CALLBACK_URL);
             context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                     .parse(requestToken.getAuthenticationURL())));
         } catch (TwitterException e) {
             e.printStackTrace();
         }
		
	}

}
