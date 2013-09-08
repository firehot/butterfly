package com.butterfly;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.VideoView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ToggleButton;
import com.butterfly.R;


public class ClientActivity extends Activity {

	public static final String TAG = "TAG";

	//	private SurfaceView surfaceView;
	private VideoView videoView;

	protected Process ffmpegAudioProcess;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;

		String rtmpUrl = getString(R.string.rtmp_url);
		
		Intent intent = getIntent();
		String streamName = intent.getStringExtra(StreamList.STREAM_PUBLISHED_NAME);

		setContentView(R.layout.activity_client);
		


		videoView = (VideoView) findViewById(R.id.surface_view);
		
		if (streamName != null) {
			videoView.setVideoPath(rtmpUrl + streamName + " live=1");
			
			videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
			videoView.setBufferSize(1024*10);



			videoView.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
				int i = 0;
				@Override
				public void onBufferingUpdate(MediaPlayer arg0, int percent) {

					if (percent>=0 && videoView.isPlaying() == false) {
						videoView.start();
					}
					if (i > 20) {
						Log.i("Buffer update", "buffer " + percent);
						i = 0;
					}
					i++;

				}
			});

			videoView.setOnInfoListener(new OnInfoListener() {

				@Override
				public boolean onInfo(MediaPlayer arg0, int what, int extra) {
					if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
						videoView.start();
					}
					else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && videoView.isPlaying() == false) {
						videoView.start();
					}
					return false;
				}
			});


			videoView.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					System.out.println("Error on media player arg1:" + arg1 + " arg2:"+ arg2);
					return false;
				}
			});
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
		//	prepareInterface2();
		super.onResume();
	}



	public void stopStreaming() {
		if(videoView != null)
			videoView.stopPlayback();
	}

}
