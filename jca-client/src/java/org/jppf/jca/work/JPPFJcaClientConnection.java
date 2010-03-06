/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.jca.work;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;
import org.jppf.client.*;
import org.jppf.comm.socket.SocketInitializer;
import org.jppf.server.protocol.*;
import org.jppf.utils.Pair;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFJcaClientConnection extends AbstractJPPFClientConnection
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFJcaClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The JPPF client that manages connections to the JPPF drivers.
	 */
	private JPPFJcaClient client = null;

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 * @param client the JPPF client that owns this connection.
	 */
	public JPPFJcaClientConnection(String uuid, String name, String host, int driverPort,
		int classServerPort, int priority, JPPFJcaClient client)
	{
		super(uuid, name, host, driverPort, classServerPort, priority);
		status = DISCONNECTED;
		this.client = client;
	}

	/**
	 * 
	 * @see org.jppf.client.JPPFClientConnection#init()
	 */
	public void init()
	{
		try
		{
			setStatus(CONNECTING);
			initCredentials();
			taskServerConnection.init();
			setStatus(ACTIVE);
		}
		catch(Exception e)
		{
			log.debug(e);
			setStatus(DISCONNECTED);
		}
		catch(JPPFError e)
		{
			setStatus(FAILED);
			throw e;
		}
	}

	/**
	 * Send tasks to the server for execution.
	 * @param cl - classloader used for serialization.
	 * @param header - the task bundle to send to the driver.
	 * @param job - the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(ClassLoader cl, JPPFTaskBundle header, JPPFJob job) throws Exception
	{
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		try
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(cl);
			sendTasks(header, job);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		catch(Error e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	/**
	 * Submit a JPPFJob for execution.
	 * @param job the job to execute.
	 * @throws Exception if an error occurs while sending the job for execution.
	 * @see org.jppf.client.JPPFClientConnection#submit(org.jppf.client.JPPFJob)
	 */
	public void submit(JPPFJob job) throws Exception
	{
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index
	 * of the first result within the initial task execution request.
	 * @param cl the cintext classloader to use to deserialize the results.
	 * @throws Exception if an error is raised while reading the results from the server.
	 */
	public Pair<List<JPPFTask>, Integer> receiveResults(ClassLoader cl) throws Exception
	{
		ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
		if (cl != null) Thread.currentThread().setContextClassLoader(cl);
		Pair<List<JPPFTask>, Integer> results = null;
		try
		{
			results = super.receiveResults();
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(prevCl);
		}
		return results;
	}

	/**
	 * Get the name of the serialization helper implementation class name to use.
	 * @return the fully qualified class name of a <code>SerializationHelper</code> implementation.
	 * @see org.jppf.client.AbstractJPPFClientConnection#getSerializationHelperClassName()
	 */
	protected String getSerializationHelperClassName()
	{
		return "org.jppf.jca.serialization.JcaSerializationHelperImpl";
	}


	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>JPPFJob</code> instances to resubmit.
	 * @see org.jppf.client.JPPFClientConnection#close()
	 */
	public List<JPPFJob> close()
	{
		if (!isShutdown)
		{
			isShutdown = true;
			try
			{
				if (taskServerConnection != null) taskServerConnection.close();
				if (delegate != null) delegate.close();
			}
			catch(Exception e)
			{
				log.error("[" + name + "] "+ e.getMessage(), e);
			}
			List<JPPFJob> result = new ArrayList<JPPFJob>();
			if (job != null) result.add(job);
			return result;
		}
		return null;
	}

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 * @see org.jppf.client.JPPFClientConnection#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get a string representation of this client connection.
	 * @return a string representing this connection.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name + " : " + status;
	}

	/**
	 * Create a socket initializer.
	 * @return an instance of <code>SocketInitializerImpl</code>.
	 * @see org.jppf.client.AbstractJPPFClientConnection#createSocketInitializer()
	 */
	protected SocketInitializer createSocketInitializer()
	{
		return new JcaSocketInitializer();
	}

	/**
	 * Get the JPPF client that manages connections to the JPPF drivers.
	 * @return a <code>JPPFJcaClient</code> instance.
	 */
	public JPPFJcaClient getClient()
	{
		return client;
	}
}
