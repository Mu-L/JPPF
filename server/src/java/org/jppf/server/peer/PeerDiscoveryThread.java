/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.peer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.jppf.comm.discovery.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PeerDiscoveryThread extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PeerNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Contains the set of retrieved connection information objects.
	 */
	private Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
	/**
	 * Count of distinct retrieved connection informaiton objects.
	 */
	private AtomicInteger count = new AtomicInteger(0);
	/**
	 * Connection information for this JPPF driver.
	 */
	private JPPFConnectionInformation localInfo = null;

	/**
	 * Default constructor.
	 */
	public PeerDiscoveryThread()
	{
		localInfo = JPPFDriver.getInstance().createConnectionInformation();
	}

	/**
	 * Lookup server configurations from UDP multicasts.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			JPPFMulticastReceiver receiver = new JPPFMulticastReceiver();
			while (!isStopped())
			{
				JPPFConnectionInformation info = receiver.receive();
				if ((info != null) && !infoSet.contains(info) && !info.equals(localInfo))
				{
					if (debugEnabled) log.debug("Found peer connection information: " + info);
					infoSet.add(info);
					int n = count.get();
					TypedProperties props = JPPFConfiguration.getProperties();
					String peerNames = (n <= 0) ? "" : props.getString("jppf.peers", "");
					String name = "Peer-" + count.incrementAndGet();
					if (!"".equals(peerNames)) peerNames += " ";
					peerNames += name;
					props.setProperty("jppf.peers", peerNames);
					props.setProperty("jppf.peer." + name + ".server.host", info.host);
					props.setProperty("class.peer."+name+".server.port", "" + info.classServerPorts[0]);
					props.setProperty("node.peer."+name+".server.port", "" + info.nodeServerPorts[0]);
					new JPPFPeerInitializer(name).start();
				}
				//Thread.sleep(50L);
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
