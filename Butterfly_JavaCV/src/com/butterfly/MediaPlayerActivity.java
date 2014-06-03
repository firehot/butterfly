package com.butterfly;

import java.util.List;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.tasks.CheckStreamExistTask;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MediaPlayerActivity extends Activity implements
		io.vov.vitamio.MediaPlayer.OnCompletionListener {

	public static final String TAG = "TAG";

	// private SurfaceView surfaceView;
	private VideoView videoView;

	protected Process ffmpegAudioProcess;

	private TextView bufferStatusTextView;

	private String bufferText;

	private TextView loadingView;

	private String rtmpUrl;

	private String httpURL;
	
	private IAsyncTaskListener mAsyncTaskListener = new IAsyncTaskListener() {

		@Override
		public void onProgressUpdate(Object... progress) {}

		@Override
		public void onPreExecute() {}

		@Override
		public void onPostExecute(Object result) {
			Boolean boolResult = (Boolean)result;
			if (boolResult == false) {
				//loadingView.setText(R.string.missed_the_stream);
				videoView.stopPlayback();
				enableMediaController();
				videoView.setVideoPath(httpURL);
				videoView.start();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;

		rtmpUrl = getString(R.string.rtmp_url);
		String httpGatewayUrl = getString(R.string.http_gateway_url);
		httpURL = getString(R.string.http_url) + "streams/";

		Intent intent = getIntent();
		String streamName = intent
				.getStringExtra(StreamListFragment.STREAM_PUBLISHED_NAME);

		rtmpUrl += streamName + " live=1";
		httpURL += streamName + ".flv";
		setContentView(R.layout.activity_client);

		bufferStatusTextView = (TextView) findViewById(R.id.bufferStatusText);
		loadingView = (TextView) findViewById(R.id.loading);

		videoView = (VideoView) findViewById(R.id.surface_view);
		bufferText = getString(R.string.buffer);

		if (streamName != null) {
			boolean isLive = intent.getBooleanExtra(StreamListFragment.STREAM_IS_LIVE, true);
			String videoURL; // = rtmpUrl + streamName; 
			if (isLive == true) {
				new CheckStreamExistTask(mAsyncTaskListener, this).execute(httpGatewayUrl, streamName);
				videoURL = rtmpUrl;
			}
			else {
				enableMediaController();
				videoURL = httpURL;
			}
			
			videoView.setVideoPath(videoURL);

			videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
			//videoView.setBufferSize(1024 * 10);

			videoView.requestFocus();
			videoView
					.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
						int i = 0;

						@Override
						public void onBufferingUpdate(MediaPlayer arg0,
								int percent) {

							if (percent >= 5 && videoView.isPlaying() == false) {
								videoView.start();
							} else if (percent <= 1
									&& videoView.isPlaying() == true) {
								videoView.pause();
							}
							if (videoView.isPlaying() == true) {
								loadingView.setVisibility(View.INVISIBLE);
							} else {
								loadingView.setVisibility(View.VISIBLE);
							}

							i++;
							if (i > 100) {
								bufferStatusTextView.setText(bufferText + " %"
										+ percent);

								i = 0;
							}

						}
					});

			videoView.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					System.out.println("Error on media player arg1:" + arg1
							+ " arg2:" + arg2);
					loadingView.setVisibility(View.INVISIBLE);
					return false;
				}
			});

			videoView.setOnCompletionListener(this);

		}

	}

	private void enableMediaController() {
		MediaController mediaController = new MediaController(this);
		mediaController.setMediaPlayer(videoView);
		videoView.setMediaController(mediaController);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		stopStreaming();
		finish();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// prepareInterface2();
		super.onResume();
	}

	public void stopStreaming() {
		if (videoView != null)
			videoView.stopPlayback();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		BugSenseHandler.closeSession(this);
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		videoView.stopPlayback();
		System.out.println("MediaPlayerActivity.onCompletion()");
		this.finish();
	}

}
