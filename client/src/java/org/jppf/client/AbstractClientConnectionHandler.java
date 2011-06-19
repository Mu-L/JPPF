/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.DISCONNECTED;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * Common abstract superclass for client connections to a server.
 * @author Laurent Cohen
 * @author Jeff Rosen
 */
public abstract class AbstractClientConnectionHandler implements ClientConnectionHandler
{
	/**
	 * The socket client uses to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	protected SocketInitializer socketInitializer = createSocketInitializer();
	/**
	 * The maximum time the underlying socket may be idle, before it is considered
	 * suspect and recycled.
	 */
	private long maxSocketIdleMillis;
	/**
	 * The name or IP address of the host the class server is running on.
	 */
	protected String host = null;
	/**
	 * The TCP port the class server is listening to.
	 */
	protected int port = -1;
	/**
	 * The client connection which owns this connection handler.
	 */
	protected JPPFClientConnection owner = null;
	/**
	 * The name given to this connection handler.
	 */
	protected String name = null;
	/**
	 * Status of the connection.
	 */
	protected AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<JPPFClientConnectionStatus>(DISCONNECTED);
	//protected JPPFClientConnectionStatus status = DISCONNECTED;
	/**
	 * Lock for synchronizing acces to the status.
	 */
	protected Object statusLock = new Object();
	/**
	 * List of status listeners for this connection.
	 */
	protected List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();
	/**
	 * Array of listeners on wich the iterations are performed.
	 */
	private ClientConnectionStatusListener[] listenerArray = new ClientConnectionStatusListener[0];

	/**
	 * Initialize this connection with the specified owner.
	 * @param owner the client connection which owns this connection handler.
	 */
	protected AbstractClientConnectionHandler(JPPFClientConnection owner)
	{
		this.owner = owner;
		if (owner != null) this.name = owner.getName();
		long configSocketIdle = JPPFConfiguration.getProperties().getLong("jppf.socket.max-idle", -1L);
		maxSocketIdleMillis = (configSocketIdle > 10L) ? configSocketIdle * 1000L : -1L; 
	}

	/**
	 * Get the status of this connection.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.ClientConnectionHandler#getStatus()
	 */
	public JPPFClientConnectionStatus getStatus()
	{
		synchronized(statusLock)
		{
			return status.get();
		}
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.ClientConnectionHandler#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	public void setStatus(JPPFClientConnectionStatus status)
	{
		synchronized(statusLock)
		{
			JPPFClientConnectionStatus oldStatus = getStatus();
			this.status.set(status);
			if (!status.equals(oldStatus)) fireStatusChanged(oldStatus);
		}
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.ClientConnectionHandler#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
			listenerArray = listeners.toArray(new ClientConnectionStatusListener[0]); 
		}
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.ClientConnectionHandler#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
			listenerArray = listeners.toArray(new ClientConnectionStatusListener[0]); 
		}
	}

	/**
	 * Create a socket initializer for this connection handler.
	 * @return a <code>SocketInitializer</code> instance.
	 */
	protected abstract SocketInitializer createSocketInitializer();

	/**
	 * Notify all listeners that the status of this connection has changed.
	 * @param oldStatus the connection status before the change.
	 */
	protected void fireStatusChanged(JPPFClientConnectionStatus oldStatus)
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
		ClientConnectionStatusListener[] temp = null;
		synchronized(listenerArray)
		{
			temp = listenerArray;
		}
		for (ClientConnectionStatusListener listener: temp) listener.statusChanged(event);
	}

	/**
	 * Get the socket client used to communicate over a socket connection.
	 * @return a <code>SocketWrapper</code> instance.
	 * @throws Exception if any error occurs.
	 */
	public SocketWrapper getSocketClient() throws Exception
	{
		// If the socket has been idle too long, recycle the connection.
		if ((maxSocketIdleMillis > 10000L) 
			&& (System.currentTimeMillis() - maxSocketIdleMillis > socketClient.getSocketTimestamp()))
		{
			close();
			init();
		}
		return socketClient;
	}
}
