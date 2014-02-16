package org.red5.core.dbModel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.stream.IStreamPacket;

public class Stream implements Serializable {

	private static final long serialVersionUID = 1L;
	public String streamName;
	public String streamUrl;
	public Date registerTime;
	public ArrayList<String> viewerStreamNames = new ArrayList<String>();
	private GcmUsers gcmIdList;
	public Timestamp timeReceived;
	public double altitude;
	public double longitude;
	public double latitude;
	public FLVWriter flvWriter;
	public boolean isLive = true;
	public boolean isPublic;

	public Stream(String streamName, String streamUrl, Date registerTime, boolean isPublic) {
		super();
		this.streamName = streamName;
		this.streamUrl = streamUrl;
		this.registerTime = registerTime;
		this.isLive = true;
		this.isPublic = isPublic;

		File streamsFolder = new File("webapps/ButterFly_Red5/streams");
		if (streamsFolder.exists() == false) {
			streamsFolder.mkdir();
		}
		File file = new File(streamsFolder, streamUrl + ".flv");

		if (file.exists() == false) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		flvWriter = new FLVWriter(file, false);
	}

	public void write(IStreamPacket packet) {

		IoBuffer data = packet.getData().asReadOnlyBuffer().duplicate();

		if (data.limit() == 0) {
			System.out.println("data limit -> 0");
			return;
		}


		ITag tag = new Tag();
		tag.setDataType(packet.getDataType());
		tag.setBodySize(data.limit());
		tag.setTimestamp(packet.getTimestamp());
		tag.setBody(data);

		try {
			flvWriter.writeTag(tag);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		flvWriter.close();
	}

	public void addViewer(String streamName) {
		viewerStreamNames.add(streamName);
	}

	public boolean containsViewer(String streamName) {
		return viewerStreamNames.contains(streamName);
	}

	public void removeViewer(String streamName) {
		viewerStreamNames.remove(streamName);
	}

	public int getViewerCount() {
		return viewerStreamNames.size();
	}

	public void setGCMUser(GcmUsers registrationIdList) {
		this.gcmIdList = registrationIdList;
	}

	public GcmUsers getBroadcasterGCMUsers() {
		return gcmIdList;
	}

}