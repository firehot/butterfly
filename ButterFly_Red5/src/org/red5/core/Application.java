package org.red5.core;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter {
	
	    private static final String url = "jdbc:mysql://localhost/test";
	    private static final String user = "root";
	    private static final String pass = "Deneme123";

	Map<String, Stream> registeredStreams = new HashMap<String, Stream>();
	
	public class Stream implements Serializable {
		public String streamName;
		public String streamUrl;
		public Long registerTime;
		public Stream(String streamName, String streamUrl, Long registerTime) {
			super();
			this.streamName = streamName;
			this.streamUrl = streamUrl;
			this.registerTime = registerTime;
		}
		
	}
	
	/** {@inheritDoc} */
    @Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		return true;
	}

	/** {@inheritDoc} */
    @Override
	public void disconnect(IConnection conn, IScope scope) {
		super.disconnect(conn, scope);
	}
    
	public HashMap<String, String> getLiveStreams() {
		HashMap<String, String> streams = new HashMap<String, String>();
		IScope target = null;

		target = Red5.getConnectionLocal().getScope();

		Set<String> streamNames =
				getBroadcastStreamNames(target);
		for (String name : streamNames) {
			if (registeredStreams.containsKey(name)) {
				Stream stream = registeredStreams.get(name);
				streams.put(stream.streamUrl, stream.streamName);
			}
		}
		return streams;
	}
	
	
	public boolean registerLiveStream(String streamName, String url) {
		boolean result = false;
		if (registeredStreams.containsKey(url) == false) {
			registeredStreams.put(url, new Stream(streamName,  url, System.currentTimeMillis()));
			result = true;
		}
		return result;
	}
	
	public boolean registerUser(String register_id, String mail )
	{
		
		boolean result = false;
		try 
		   {
   			Connection mySQLConn = DriverManager.getConnection( url , user , pass );
            Statement sttmnt = mySQLConn.createStatement( );
            String insert = "INSERT INTO  test.gcm_users(gcm_reg_id,email)  VALUES('"+register_id+"','"+mail+"')";
            sttmnt.executeUpdate( insert );
            result = true;
    	   }
			catch (Exception e) 
			{
			System.out.print("Hata");
			System.err.print(e.getMessage());
			}
		return result ;
	}
	
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamUrl = stream.getPublishedName();
		//getPublishedName means streamurl to us
		removeStream(streamUrl);
		super.streamBroadcastClose(stream);
	}

	public boolean removeStream(String streamUrl) {
		boolean result = false;
		if (registeredStreams.containsKey(streamUrl)) {
			Object object = registeredStreams.remove(streamUrl);
			if (object != null) {
				result = true;
			}
		}
		return result;
	}
	
	

}
