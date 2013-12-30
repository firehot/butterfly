package net.butterflytv.server.feeder;
import java.util.ArrayList;


public class Main {

	private static ArrayList<LiveFeed> liveFeeds;

	public static void main(String[] args) {
		liveFeeds = new ArrayList<LiveFeed>();

		long time = System.currentTimeMillis();
		liveFeeds.add(new LiveFeed("Amsterdam (test broadcast)", "amsterdam" + time, "rtmp://192.87.30.3/webcam/webcam.stream"));

//		liveFeeds.add(new LiveFeed("Watch the Kooala(tst)", "kola", "rtmp://66.181.13.230/CamzoneStreams/zssd-koala"));
		
//		liveFeeds.add(new LiveFeed("Wrightsville Beach, NC, USA(test)", "villebeach"+ time, 
//				"rtmp://208.43.68.139/surfchex/wrightsvillebeach-super"));

//		liveFeeds.add(new LiveFeed("Watch the Panda(test)", "panda"+time, "rtmp://66.181.13.230/CamzoneStreams/zssd-panda"));
//		
//		liveFeeds.add(new LiveFeed("Watch the Elephants", "elephants", "rtmp://66.181.13.230/CamzoneStreams/elephants"));
		for (LiveFeed feed : liveFeeds) {
			feed.start();
		}

	}

	@Override
	protected void finalize() throws Throwable {
		if (liveFeeds != null) {
			for (LiveFeed feed : liveFeeds) {
				if (feed != null) {
					feed.stop();
				}
			}
		}
		super.finalize();
	}




}
