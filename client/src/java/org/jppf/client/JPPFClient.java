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

import java.util.List;

import org.jppf.client.event.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractGenericClient
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFClient.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The submission manager.
	 */
	private SubmissionManager submissionManager;

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		super(JPPFConfiguration.getProperties());
	}

	/**
	 * Initialize this client with an automatically generated application UUID.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(ClientListener...listeners)
	{
		this();
		for (ClientListener listener: listeners) addClientListener(listener);
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		super(uuid, JPPFConfiguration.getProperties());
	}

	/**
	 * Initialize this client with the specified application UUID and new connection listeners.
	 * @param uuid the unique identifier for this local client.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(String uuid, ClientListener...listeners)
	{
		this(uuid);
		for (ClientListener listener: listeners) addClientListener(listener);
	}

	/**
	 * Initialize this client's configuration.
	 * @param configuration an object holding the JPPF configuration.
	 */
	protected void initConfig(Object configuration)
	{
		config = (TypedProperties) configuration;
	}

	/**
	 * Create a new driver connection based on the specified parameters.
	 * @param uuid the uuid of the JPPF client.
	 * @param name the name of the connection.
	 * @param info the driver connection information.
	 * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
	 */
	protected AbstractJPPFClientConnection createConnection(String uuid, String name, JPPFConnectionInformation info)
	{
		return new JPPFClientConnectionImpl(this, uuid, name, info);
	}

	/**
	 * Submit a job execution request.
	 * @param job the job to execute.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submit(org.jppf.client.JPPFJob)
	 */
	public List<JPPFTask> submit(JPPFJob job) throws Exception
	{
		if ((job.getResultListener() == null) || job.isBlocking()) job.setResultListener(new JPPFResultCollector(job.getTasks().size()));
		//else if (job.isBlocking()) job.setResultListener(new JPPFResultCollector(job.getTasks().size()));
		submissionManager.submitJob(job);
		if (job.isBlocking())
		{
			JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
			return collector.waitForResults();
		}
		return null;
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 * @deprecated this method does not allow to chose which driver to get the statistics from.
	 * Use <code>((JPPFClientConnectionImpl) getConnection(java.lang.String)).getJmxConnection().statistics()</code> instead.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(true);
		return (conn == null) ? null : conn.getJmxConnection().statistics();
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		if (loadBalancer != null) loadBalancer.stop();
		if (submissionManager != null)
		{
			submissionManager.setStopped(true);
			submissionManager.wakeUp();
		}
		super.close();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void initPools()
	{
		submissionManager = new SubmissionManager(this);
		new Thread(submissionManager, "SubmissionManager").start();
		super.initPools();
	}

	/**
	 * Invoked when the status of a client connection has changed.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void statusChanged(ClientConnectionStatusEvent event)
	{
		super.statusChanged(event);
		submissionManager.wakeUp();
	}
}
