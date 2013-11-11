package com.butterfly;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.widget.VideoView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;

public class ClientActivity extends Activity implements
		io.vov.vitamio.MediaPlayer.OnCompletionListener {

	public static final String TAG = "TAG";

	// private SurfaceView surfaceView;
	private VideoView videoView;

	protected Process ffmpegAudioProcess;

	private TextView bufferStatusTextView;

	private String bufferText;

	private TextView loadingView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;

		String rtmpUrl = getString(R.string.rtmp_url);

		Intent intent = getIntent();
		String streamName = intent
				.getStringExtra(StreamList.STREAM_PUBLISHED_NAME);

		setContentView(R.layout.activity_client);

		bufferStatusTextView = (TextView) findViewById(R.id.bufferStatusText);
		loadingView = (TextView) findViewById(R.id.loading);

		videoView = (VideoView) findViewById(R.id.surface_view);
		bufferText = getString(R.string.buffer);

		if (streamName != null) {
			videoView.setVideoPath(rtmpUrl + streamName + " live=1");

			videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
			videoView.setBufferSize(1024 * 10);

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

			// videoView.setOnInfoListener(new OnInfoListener() {
			//
			// @Override
			// public boolean onInfo(MediaPlayer arg0, int what, int extra) {
			// if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
			// videoView.start();
			// } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
			// && videoView.isPlaying() == false) {
			// videoView.start();
			// }
			//
			// return false;
			// }
			// });

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_client, menu);
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
		// TODO Auto-generated method stub
		super.onDestroy();

		BugSenseHandler.closeSession(this);
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		videoView.stopPlayback();
		this.finish();
	}

}
