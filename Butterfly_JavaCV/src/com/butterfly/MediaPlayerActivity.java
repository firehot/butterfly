package com.butterfly;

import com.butterfly.fragment.StreamListFragment;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.WindowManager;

/**
 * @author 
 *         
 */
public class MediaPlayerActivity extends Activity {

	private VideoView videoView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
            return;
		// Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_videoplayer);

		// Disable home button
		getActionBar().setDisplayShowHomeEnabled(false);

		Intent intent = getIntent();
		String videoUrl = intent
				.getStringExtra(StreamListFragment.STREAM_PUBLISHED_NAME);
		videoUrl = this.getString(R.string.image_url)+videoUrl+".flv";
		
		// initialize views
		this.videoView = (VideoView) this.findViewById(R.id.videoView);
		this.videoView
				.setVideoURI(Uri.parse(videoUrl));
		this.videoView.setMediaController(new MediaController(this));
		this.videoView.requestFocus();
		this.videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
		this.videoView.setBufferSize(1024 * 10);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}
}

