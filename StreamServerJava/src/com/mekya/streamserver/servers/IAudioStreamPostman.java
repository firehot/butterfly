package com.mekya.streamserver.servers;

import com.mekya.streamserver.IStreamListener;

public interface IAudioStreamPostman {

	public abstract void registerForAudio(IStreamListener streamListener);

	public abstract void removeAudioListener(IStreamListener streamListener);

}