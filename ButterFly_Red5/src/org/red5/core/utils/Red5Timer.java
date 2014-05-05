package org.red5.core.utils;

import java.io.File;
import java.util.TimerTask;

import org.red5.core.Application;

public class Red5Timer extends TimerTask {

	Application red5App;
	long deleteTime;
	
	public Red5Timer(Application red5App,final long deleteTime)
	{
		this.red5App = red5App;
		this.deleteTime = deleteTime;
	}
	@Override
	public void run() {
		File dir = new File("webapps/ButterFly_Red5/streams");
		String[] files = dir.list();
		if (files != null) {
			long timeMillis = System.currentTimeMillis();
			for (String fileName : files) {
				File f = new File(dir, fileName);
				if (f.isFile() == true && f.exists() == true) {

					String key = f.getName().substring(0,
							f.getName().indexOf(".flv"));
					if ((timeMillis - f.lastModified()) > deleteTime) {

						this.red5App.deleteStreamFiles(key);

						this.red5App.removeTimeUpStreams(key);
					}

				}
			}
		}

	}

}
