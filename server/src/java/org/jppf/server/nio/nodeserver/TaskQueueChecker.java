/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.ChannelBundlePair;
import org.jppf.server.protocol.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
public class TaskQueueChecker implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(TaskQueueChecker.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Random number generator used to randomize the choice of idle channel.
	 */
	private Random random = new Random(System.currentTimeMillis());
	/**
	 * The owner of this queue checker.
	 */
	private NodeNioServer server = null;
	/**
	 * Determines whether this task is currently executing.
	 */
	private AtomicBoolean executing = new AtomicBoolean(false);
	
	/**
	 * Initialize this task queue checker with the specified node server. 
	 * @param server - the owner of this queue checker.
	 */
	public TaskQueueChecker(NodeNioServer server)
	{
		this.server = server;
	}

	/**
	 * Perform the assignment of tasks.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			setExecuting(true);
			List<SelectableChannel> idleChannels = server.getIdleChannels();
			synchronized(idleChannels)
			{
				if (idleChannels.isEmpty() || server.getQueue().isEmpty()) return;
				if (debugEnabled) log.debug(""+idleChannels.size()+" channels idle");
				List<SelectableChannel> channelList = new ArrayList<SelectableChannel>();
				channelList.addAll(idleChannels);
				boolean found = false;
				SelectableChannel channel = null;
				BundleWrapper selectedBundle = null;
				Iterator<BundleWrapper> it = server.getQueue().iterator();
				while (!found && it.hasNext() && !idleChannels.isEmpty())
				{
					BundleWrapper bundleWrapper = it.next();
					JPPFTaskBundle bundle = bundleWrapper.getBundle();
					if (!checkJobState(bundle)) continue;
					int n = findIdleChannelIndex(bundle);
					if (n >= 0)
					{
						channel = idleChannels.remove(n);
						selectedBundle = bundleWrapper;
						found = true;
					}
				}
				if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "found channel for bundle");
				if (channel != null)
				{
					SelectionKey key = channel.keyFor(server.getSelector());
					NodeContext context = (NodeContext) key.attachment();
					BundleWrapper bundleWrapper = server.getQueue().nextBundle(selectedBundle, context.getBundler().getBundleSize());
					context.setBundle(bundleWrapper);
					server.getTransitionManager().transitionChannel(key, NodeTransition.TO_SENDING);
					JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), channel);
				}
			}
		}
		finally
		{
			setExecuting(false);
		}
	}

	/**
	 * Find a channel that can send the specified task bundle for execution.
	 * @param bundle the bundle to execute.
	 * @return the index of an available and acceptable channel, or -1 if no channel could be found.
	 */
	private int findIdleChannelIndex(JPPFTaskBundle bundle)
	{
		List<SelectableChannel> idleChannels = server.getIdleChannels();
		int n = -1;
		ExecutionPolicy rule = bundle.getExecutionPolicy();
		if (debugEnabled && (rule != null)) log.debug("Bundle has an execution policy:\n" + rule);
		List<Integer> acceptableChannels = new ArrayList<Integer>();
		List<Integer> channelsToRemove =  new ArrayList<Integer>();
		List<String> uuidPath = bundle.getUuidPath().getList();
		for (int i=0; i<idleChannels.size(); i++)
		{
			SelectableChannel ch = idleChannels.get(i);
			if (!ch.isOpen())
			{
				channelsToRemove.add(i);
				continue;
			}
			NodeContext context = (NodeContext) ch.keyFor(server.getSelector()).attachment();
			if (uuidPath.contains(context.getNodeUuid())) continue;
			if (rule != null)
			{
				NodeManagementInfo mgtInfo = JPPFDriver.getInstance().getNodeInformation(ch);
				JPPFSystemInformation info = (mgtInfo == null) ? null : mgtInfo.getSystemInfo();
				if (!rule.accepts(info)) continue;
			}
			acceptableChannels.add(i);
		}
		for (Integer i: channelsToRemove) idleChannels.remove(i);
		if (debugEnabled) log.debug("found " + acceptableChannels.size() + " acceptable channels");
		if (!acceptableChannels.isEmpty())
		{
			int rnd = random.nextInt(acceptableChannels.size());
			n = acceptableChannels.remove(rnd);
		}
		return n;
	}

	/**
	 * Check if the job state allows it to be dispatched on another node.
	 * There are two cases when this method will return false: when the job is suspended and
	 * when the job is already executing on its maximum allowed number of nodes.
	 * @param bundle - the bundle from which to get the job information.
	 * @return true if the job can be dispatched to at least one more node, false otherwise.
	 */
	private boolean checkJobState(JPPFTaskBundle bundle)
	{
		if (bundle.isSuspended()) return false;
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		Integer maxNodes = (Integer) bundle.getParameter(BundleParameter.MAX_JOB_NODES);
		if (maxNodes == null) maxNodes = Integer.MAX_VALUE;
		List<ChannelBundlePair> list = server.getJobManager().getNodesForJob(jobId);
		int n = (list == null) ? 0 : list.size();
		if (debugEnabled) log.debug("current nodes = " + n + ", maxNodes = " + maxNodes);
		return n < maxNodes;
	}

	/**
	 * Determine whether this task is currently executing.
	 * @return true if this task is currently executing, false otherwise.
	 */
	public boolean isExecuting()
	{
		return executing.get();
	}

	/**
	 * Specify whether this task is currently executing.
	 * @param executing - true if this task is currently executing, false otherwise.
	 */
	public void setExecuting(boolean executing)
	{
		this.executing.set(executing);
	}
}