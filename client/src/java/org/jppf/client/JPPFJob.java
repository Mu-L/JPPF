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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.event.TaskResultListener;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFUuid;

/**
 * Instances of this class represent a JPPF submission and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * <p>This class also provides the API for handling JPPF-annotated tasks and POJO tasks.
 * <p>All jobs have an id. It can be specified by calling {@link #setId(java.lang.String) setId(String)}.
 * If left unspecified, JPPF will automatically assign a uuid as its value.
 * @author Laurent Cohen
 */
public class JPPFJob implements Serializable
{
	/**
	 * The list of tasks to execute.
	 */
	private List<JPPFTask> tasks = null;
	/**
	 * The container for data shared between tasks.
	 */
	private DataProvider dataProvider = null;
	/**
	 * The tasks execution policy.
	 */
	private ExecutionPolicy executionPolicy = null;
	/**
	 * The listener that receives notifications of completed tasks.
	 */
	private transient TaskResultListener resultsListener = null;
	/**
	 * Determines whether the execution of this job is blocking on the client side.
	 */
	private boolean blocking = true;
	/**
	 * The universal unique id for this job.
	 */
	private String id = null;
	/**
	 * The list of tasks to execute.
	 */
	private List<JPPFTask> results = null;
	/**
	 * The priority of this job, used by the server to prioritize queued jobs.
	 */
	private int priority = 0;
	/**
	 * The maximum number of nodes this job can run on.
	 */
	private int  maxNodes = Integer.MAX_VALUE;

	/**
	 * Default constructor.
	 */
	public JPPFJob()
	{
		id = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString();
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param dataProvider - the container for data shared between tasks.
	 */
	public JPPFJob(DataProvider dataProvider)
	{
		this(dataProvider, null, true, null, 0);
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param dataProvider - the container for data shared between tasks.
	 * @param executionPolicy - the tasks execution policy.
	 */
	public JPPFJob(DataProvider dataProvider, ExecutionPolicy executionPolicy)
	{
		this(dataProvider, executionPolicy, true, null, 0);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param resultsListener - the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(TaskResultListener resultsListener)
	{
		this(null, null, false, resultsListener, 0);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param dataProvider - the container for data shared between tasks.
	 * @param resultsListener - the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, TaskResultListener resultsListener)
	{
		this(dataProvider, null, false, resultsListener, 0);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param dataProvider - the container for data shared between tasks.
	 * @param executionPolicy - the tasks execution policy.
	 * @param resultsListener - the listener that receives notifications of completed tasks.
	 */
	public JPPFJob(DataProvider dataProvider, ExecutionPolicy executionPolicy, TaskResultListener resultsListener)
	{
		this(dataProvider, null, false, resultsListener, 0);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param dataProvider - the container for data shared between tasks.
	 * @param executionPolicy - the tasks execution policy.
	 * @param blocking - determines whetehr this job is blocking.
	 * @param resultsListener - the listener that receives notifications of completed tasks.
	 * @param priority - the priority of this job.
	 */
	public JPPFJob(DataProvider dataProvider, ExecutionPolicy executionPolicy, boolean blocking, TaskResultListener resultsListener, int priority)
	{
		this();
		this.dataProvider = dataProvider;
		this.executionPolicy = executionPolicy;
		this.resultsListener = resultsListener;
		this.blocking = blocking;
		this.priority = priority;
	}

	/**
	 * Get the universal unique id for this job.
	 * @return the uuid as a string. 
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Set the universal unique id for this job.
	 * @param id - the id as a string. 
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Get the list of tasks to execute.
	 * @return a list of objects.
	 */
	public List<JPPFTask> getTasks()
	{
		return tasks;
	}

	/**
	 * Add a task to this job.
	 * @param taskObject - the task to add to this job.
	 * @param args - arguments to use with a JPPF-annotated class.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public void addTask(Object taskObject, Object...args) throws JPPFException
	{
		JPPFTask tmp = null;
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (taskObject instanceof JPPFTask) tmp = (JPPFTask) taskObject;
		else tmp = new JPPFAnnotatedTask(taskObject, args);
		if (tasks == null) tasks = new ArrayList<JPPFTask>();
		tasks.add(tmp);
	}

	/**
	 * Add a task to this job.
	 * @param taskObject the task to add to this job.
	 * @param method the name of the method to execute.
	 * @param args arguments to use with a JPPF-annotated class.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public void addTask(String method, Object taskObject, Object...args) throws JPPFException
	{
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (tasks == null) tasks = new ArrayList<JPPFTask>();
		tasks.add(new JPPFAnnotatedTask(taskObject, method, args));
	}

	/**
	 * Get the container for data shared between tasks.
	 * @return a <code>DataProvider</code> instance.
	 */
	public DataProvider getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Set the container for data shared between tasks.
	 * @param dataProvider - a <code>DataProvider</code> instance.
	 */
	public void setDataProvider(DataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	/**
	 * Get the tasks execution policy.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	public ExecutionPolicy getExecutionPolicy()
	{
		return executionPolicy;
	}

	/**
	 * Set the tasks execution policy.
	 * @param executionPolicy - an <code>ExecutionPolicy</code> instance.
	 */
	public void setExecutionPolicy(ExecutionPolicy executionPolicy)
	{
		this.executionPolicy = executionPolicy;
	}

	/**
	 * Get the listener that receives notifications of completed tasks.
	 * @return a <code>TaskCompletionListener</code> instance.
	 */
	public TaskResultListener getResultListener()
	{
		return resultsListener;
	}

	/**
	 * Set the listener that receives notifications of completed tasks.
	 * @param resultsListener - a <code>TaskCompletionListener</code> instance.
	 */
	public void setResultListener(TaskResultListener resultsListener)
	{
		this.resultsListener = resultsListener;
		blocking = false;
	}

	/**
	 * Determine whether the execution of this job is blocking on the client side.
	 * @return true if the execution is blocking, false otherwise.
	 */
	public boolean isBlocking()
	{
		return blocking;
	}

	/**
	 * Specify whether the execution of this job is blocking on the client side.
	 * @param blocking - true if the execution is blocking, false otherwise.
	 */
	public void setBlocking(boolean blocking)
	{
		this.blocking = blocking;
	}

	/**
	 * Get the priority of this job.
	 * @return the priority as an int.
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Set the priority of this job.
	 * @param priority the priority as an int.
	 */
	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	/**
	 * Get the maximum number of nodes this job can run on.
	 * @return the number of nodes as an int value.
	 */
	public int getMaxNodes()
	{
		return maxNodes;
	}

	/**
	 * Get the maximum number of nodes this job can run on.
	 * @param maxNodes - the number of nodes as an int value.
	 */
	public void setMaxNodes(int maxNodes)
	{
		this.maxNodes = maxNodes;
	}
}
