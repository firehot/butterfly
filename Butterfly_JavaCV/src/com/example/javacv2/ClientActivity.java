package com.example.javacv2;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ToggleButton;


public class ClientActivity extends Activity {

	private static final int DEFAULT_AUDIO_RECEIVER_PORT = 53008;

	public static final String TAG = "TAG";

	private static final String PREFS_NAME = "PREFS";

	private static final String IS_INITIALIZED = "IS_INITIALIZED";

	//	private SurfaceView surfaceView;
	private VideoView videoView;

	private ToggleButton startButton;

	private EditText serverAddressEditText;

	private DatagramSocket udpAudioSocket;

	protected AudioTrack audioTrack;

	protected Process ffmpegAudioProcess;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;


		initialize();

		setContentView(R.layout.activity_client);

		serverAddressEditText = (EditText) findViewById(R.id.portNumText);

		videoView = (VideoView) findViewById(R.id.surface_view);
		//surfaceView = (SurfaceView) findViewById(R.id.surface_view);

		startButton = (ToggleButton) findViewById(R.id.button_start);

		startButton.setOnClickListener(new OnClickListener() {
			private int audioSession;

			@Override
			public void onClick(View arg0) {

				if (startButton.isChecked()) {
					
					//videoView.setVideoPath("http://"+serverAddressEditText.getText().toString()+":24007/liveVideo");
					videoView.setVideoPath(Environment.getExternalStorageDirectory()+"/Video/small.mp4");
					
					videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
					videoView.setBufferSize(1024*10);

					receiveAudio();

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
				else {
					stopStreaming();
				}


			}
		});
	}

	private void initialize() {
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		boolean is_initialized = prefs.getBoolean(IS_INITIALIZED, false);

		if (is_initialized == false) {
			try {
				//copy from assets to files directory
				InputStream istream = getAssets().open("ffmpeg");
				FileOutputStream fos = openFileOutput("ffmpeg", Context.MODE_PRIVATE);
				int read;
				byte[] buffer = new byte[1024];
				while ((read = istream.read(buffer)) != -1) {
					fos.write(buffer, 0, read);
				}
				fos.close();

				istream = getAssets().open("libx264-ultrafast.ffpreset");
				fos = openFileOutput("libx264-ultrafast.ffpreset", Context.MODE_PRIVATE);
				while ((read = istream.read(buffer)) != -1) {
					fos.write(buffer, 0, read);
				}
				fos.close();

				// make ffmpeg executable
				File fileDir = getFilesDir();
				String ffmpegPath = fileDir.getAbsolutePath()+ "/ffmpeg";				
				String command = "chmod 755 "+ ffmpegPath;
				Runtime.getRuntime().exec(command);

				//save the initialization flag 
				Editor editor = prefs.edit();
				editor.putBoolean(IS_INITIALIZED, true);
				editor.commit();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}

	}


	public void playAudio(final InputStream istream){

		new Thread(){
			public void run() {
				byte[] buffer = new byte[2048];
				int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

				while (true) {
					try {
						int len = istream.read(buffer, 0, buffer.length);

						audioTrack.write(buffer, 0, len);
						audioTrack.flush();

						if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING ) {
							audioTrack.play();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};

		}.start();
	}


	public void receiveAudio(){

		new Thread(){
			public void run() {
				try {
					String ffmpegPath = getFilesDir().getAbsolutePath() + "/ffmpeg"; //  "data/ffmpeg/bin/ffmpeg"; 
					String ffmpegCommand = ffmpegPath + " -analyzeduration 0 -f aac -strict -2 -acodec aac -ac 1 -i - -f s16le -acodec pcm_s16le -ar 44100 -ac 1 -";

					ffmpegAudioProcess = Runtime.getRuntime().exec(ffmpegCommand);
					
					
					OutputStream ostream = ffmpegAudioProcess.getOutputStream();
					readErrorStream(ffmpegAudioProcess.getErrorStream());
					
					udpAudioSocket = new DatagramSocket(DEFAULT_AUDIO_RECEIVER_PORT);
					
					playAudio(ffmpegAudioProcess.getInputStream());


					
					byte[] buffer = new byte[10*1024];
					DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
					while (true) {
						udpAudioSocket.receive(datagramPacket);
						ostream.write(datagramPacket.getData(), 0, datagramPacket.getLength());
						ostream.flush();
					}

				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopStreaming();
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





	/** A basic Camera preview class */

	private void readErrorStream(final InputStream iserror)
	{
		new Thread(){
			public void run() {
				byte[] buffer = new byte[1024];
				int len;
				try {
					while ((len = iserror.read(buffer)) != -1) {

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}


	@Override
	protected void onResume() {
		//	prepareInterface2();
		super.onResume();
	}



	public void stopStreaming() {
		if (udpAudioSocket != null) {
			udpAudioSocket.close();
		}

		if (audioTrack != null) {
			audioTrack.stop();
		}
		
		if (ffmpegAudioProcess != null) {
			ffmpegAudioProcess.destroy();
		}
		
		videoView.stopPlayback();
	}

}
