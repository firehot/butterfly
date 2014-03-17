package com.butterfly.message;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Data;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;

import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
	public static final String VIEWER_COUNT = "viewerCount";
	public static final String ACTION_VIEWER_COUNT_UPDATED = "VIEWER_COUNT_UPDATED";
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	
	private static String displayName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Data.DISPLAY_NAME_PRIMARY
			: Data.DISPLAY_NAME;


	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras.containsKey(VIEWER_COUNT)) {
			String viewerCount = extras.getString(VIEWER_COUNT);
			Intent i = new Intent();
			i.setAction(ACTION_VIEWER_COUNT_UPDATED);
			i.putExtra(VIEWER_COUNT, viewerCount);
			LocalBroadcastManager.getInstance(getApplicationContext())
					.sendBroadcast(i);
		} else {
			String streamURL = intent.getStringExtra("URL");
			String broadcaster = intent.getStringExtra("broadcaster");
			String streamName = intent.getStringExtra("name");
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			// The getMessageType() intent parameter must be the intent you
			// received
			// in your BroadcastReceiver.
			String messageType = gcm.getMessageType(intent);

			if (!extras.isEmpty()) { // has effect of unparcelling Bundle
				/*
				 * Filter messages based on message type. Since it is likely
				 * that GCM will be extended in the future with new message
				 * types, just ignore any message types you're not interested
				 * in, or that you don't recognize.
				 */
				if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
						.equals(messageType)) {
					sendNotification(null, null, null,
							"Send error: " + extras.toString());
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
						.equals(messageType)) {
					sendNotification(null, null, null, "Deleted messages on server: "
							+ extras.toString());
					// If it's a regular GCM message, do some work.
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
						.equals(messageType)) {

					// Post notification of received message.
					sendNotification(streamURL, broadcaster, streamName, null);

				}
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(String streamURL, String broadcaster, String streamName,
			String error) {
		mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		String displayName = getDisplayName(broadcaster);
		if (displayName == null) {
			String[] mail = broadcaster.split(",");
			displayName = mail[0]; 
		}

		String text = displayName + " "
				+ getString(R.string.stream_shared_with_you);
		if (error != null) {
			text = error;
		}

		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setAutoCancel(true)
				.setContentTitle(streamName + "(" +
						getApplicationContext().getString(
								R.string.live_stream) + ")")
				.setContentText(text)
				.setSound(soundUri);

		if (streamURL != null) {
			// Creates an explicit intent for an Activity in your app
			Intent resultIntent = new Intent(this, MediaPlayerActivity.class);
			resultIntent.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME, streamURL);

			// The stack builder object will contain an artificial back stack
			// for
			// the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out
			// of
			// your application to the Home screen.
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(MediaPlayerActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
					0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
		}

		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

	}
	
	private String getDisplayName(String emails) {
		
		String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
			Data._ID,
			// The primary display name
			displayName,
			// The contact's _ID, to construct a content URI
			Data.PHOTO_THUMBNAIL_URI,
		};

		String select = 
				"(" + Email.ADDRESS + " NOTNULL " + " AND " +
						Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" + " AND "
						+ displayName + " NOTNULL  ) ";

		String[] mail = emails.split(",");
		String mailQuery = new String();
		for (int i = 0; i < mail.length; i++) {
			if (i > 0) {
				mailQuery += " OR ";
			}
			mailQuery += " " + Email.ADDRESS + " == '"+ mail[i] +"' " ;
		}
		
		select += " AND (" + mailQuery +" )";

		Cursor cursor = getContentResolver().query(Data.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, select, null,
				null
				);
		 
		 cursor.moveToFirst();
		 if (cursor.getCount() > 0) {
			 return cursor.getString(cursor.getColumnIndex(displayName));
		 }
		 return null;
	}	
	
}
