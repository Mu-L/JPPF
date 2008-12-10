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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.server.protocol.BundleParameter.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.impl.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * This class implements the state of receiving information from the node as a
 * response to sending the initial bundle.
 * @author Laurent Cohen
 */
public class WaitInitialBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(WaitInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public WaitInitialBundleState(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		NodeContext context = (NodeContext) key.attachment();
		if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		if (context.getNodeMessage().read((ReadableByteChannel) channel))
		{
			if (debugEnabled) log.debug("read bundle for " + getRemoteHost(channel) + " done");
			BundleWrapper bundleWrapper = context.deserializeBundle();
			JPPFTaskBundle bundle = bundleWrapper.getBundle();
			context.setUuid(bundle.getBundleUuid());
			boolean override = bundle.getParameter(BUNDLE_TUNING_TYPE_PARAM) != null;
			Bundler bundler = override ? BundlerFactory.createBundler(bundle.getParametersMap(), true) : server.getBundler().copy();
			bundler.setup();
			context.setBundler(bundler);
			Boolean b = (Boolean) bundle.getParameter(IS_PEER);
			boolean isPeer = (b != null) && b;
			context.setPeer(isPeer);
			if (!isPeer && JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true))
			{
				String id = (String) bundle.getParameter(NODE_MANAGEMENT_ID_PARAM);
				if (id != null)
				{
					String host = (String) bundle.getParameter(NODE_MANAGEMENT_HOST_PARAM);
					int port = (Integer) bundle.getParameter(NODE_MANAGEMENT_PORT_PARAM);
					NodeManagementInfo info = new NodeManagementInfo(host, port, id);
					JPPFSystemInformation systemInfo = (JPPFSystemInformation) bundle.getParameter(NODE_SYSTEM_INFO_PARAM);
					if (systemInfo != null) info.setSystemInfo(systemInfo);
					JPPFDriver.getInstance().addNodeInformation(channel, info);
				}
			}
			// make sure the context is reset so as not to resubmit the last bundle executed by the node.
			context.setNodeMessage(null);
			context.setBundle(null);
			server.addIdleChannel(channel);
			return TO_IDLE;
		}
		return TO_WAIT_INITIAL;
	}
}
