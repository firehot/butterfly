package com.mekya.streamserver.servers;

import com.mekya.streamserver.IStreamListener;

public interface IVideoStreamPostman {

	public abstract void registerForVideo(IStreamListener streamListener);

	public abstract void removeVideoListener(IStreamListener streamListener);

}