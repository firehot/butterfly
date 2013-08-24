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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter {

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
    
	public List<String> getLiveStreams() {
		List<String> streams = new	ArrayList<String>();
		IScope target = null;

		target = Red5.getConnectionLocal().getScope();

		Set<String> streamNames =
				getBroadcastStreamNames(target);
		for (String name : streamNames) {
			System.out.println("StreamNames -----*****----- " + name);
			streams.add(name);
		}
		return streams;
	}

}
