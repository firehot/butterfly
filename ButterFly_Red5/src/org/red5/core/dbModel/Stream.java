package org.red5.core.dbModel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.stream.IStreamPacket;

@Entity
@Table(name = "streams")
public class Stream implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int id;
	public String streamName;
	public String streamUrl;
	public Date registerTime;
	public String broadcasterMail;
	public double altitude;
	public double longitude;
	public double latitude;
	public boolean isLive = true;
	public boolean isPublic;

	
	

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name = "id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "streamName",  nullable = false)
	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	@Column(name = "streamUrl",  nullable = false)
	public String getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}

	@Column(name = "registerTime",  nullable = false)
	public Date getRegisterTime() {
		return registerTime;
	}

	public void setRegisterTime(Date registerTime) {
		this.registerTime = registerTime;
	}

	@Column(name = "broadcasterMail",  nullable = false)
	public String getBroadcasterMail() {
		return broadcasterMail;
	}

	public void setBroadcasterMail(String broadcasterMail) {
		this.broadcasterMail = broadcasterMail;
	}

	@Column(name = "altitude")
	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	@Column(name = "longitude")
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Column(name = "latitude")
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@Column(name = "isLive")
	public boolean isLive() {
		return isLive;
	}

	public void setLive(boolean isLive) {
		this.isLive = isLive;
	}

	@Column(name = "isPublic")
	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Stream(String streamName, String streamUrl, Date registerTime, boolean isPublic) {
		super();
		this.streamName = streamName;
		this.streamUrl = streamUrl;
		this.registerTime = registerTime;
		this.isLive = true;
		this.isPublic = isPublic;

	}
	
	public Stream() {
		super();
	}

	
}