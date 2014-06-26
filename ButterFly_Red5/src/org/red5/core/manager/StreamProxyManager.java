package org.red5.core.manager;

import java.util.HashMap;
import java.util.Map;

import org.red5.core.dbModel.StreamProxy;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;

public class StreamProxyManager implements IStreamListener{

	private Map<String, StreamProxy> proxyStreams = new HashMap<String, StreamProxy>();

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		
		String streamUrl = stream.getPublishedName();

		if (proxyStreams.containsKey(streamUrl)) {
			proxyStreams.get(streamUrl).write(packet);
		}

	}
	
	public Map<String, StreamProxy> getLiveStreamProxies() {
		return proxyStreams;
	}
	
	public void removeProxyStream(String key)
	{
		if (proxyStreams.containsKey(key))
			proxyStreams.remove(key);
	}
	
}
