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
package test.localexecution;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;


/**
 * Runner class for testing the local execution toggle feature.
 * @author Laurent Cohen
 */
public class LocalExecutionRunner
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(LocalExecutionRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			TypedProperties props = JPPFConfiguration.getProperties();
			props.setProperty("jppf.local.execution.enabled", "false");
			int length = props.getInt("longtask.length");
			int nbTask = props.getInt("longtask.number");
			print("starting client ...");
			long start = System.nanoTime();
			jppfClient = new JPPFClient();
			long elapsed = System.nanoTime() - start;
			print("client started in "+StringUtils.toStringDuration(elapsed/1000000));
			/*
			print("run 1 with local execution off");
			perform(nbTask, length, 1);
			*/
			//print("run with local execution on"); 
			jppfClient.setLocalExecutionEnabled(true);
			perform2(100, 5, 20);
			//perform3();
			/*
			print("run 3 with local execution off"); 
			jppfClient.setLocalExecutionEnabled(false);
			perform(nbTask, length, 3);
			*/
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}
	
	/**
	 * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
	 * @param nbTasks the number of tasks to send at each iteration.
	 * @param length the executionlength of each task.
	 * @param iter the run number.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int nbTasks, int length, int iter) throws Exception
	{
		try
		{
			long start = System.currentTimeMillis();
			JPPFJob job = new JPPFJob();
			job.setId("Long task iteration " + iter);
			for (int i=0; i<nbTasks; i++)
			{
				LongTask task = new LongTask(length, false);
				task.setId("" + (iter+1) + ":" + (i+1));
				job.addTask(task);
			}
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			for (JPPFTask task: results)
			{
				Exception e = task.getException();
				if (e != null) throw e;
			}
			long elapsed = System.currentTimeMillis() - start;
			print("run " + iter + " time: "+StringUtils.toStringDuration(elapsed));
	
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
	 * @param nbTasks the number of tasks to send at each iteration.
	 * @param length the executionlength of each task.
	 * @param nbJobs the number of non-blocking jobs to submit.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform2(int nbTasks, int length, int nbJobs) throws Exception
	{
		try
		{
			print("creating the jobs");
			List<JPPFJob> jobs = new ArrayList<JPPFJob>(nbJobs);
			for (int i=0; i<nbJobs; i++)
			{
				JPPFJob job = new JPPFJob();
				job.setId("job " + i);
				job.setBlocking(false);
				job.setResultListener(new JPPFResultCollector(nbTasks));
				for (int j=0; j<nbTasks; j++)
				{
					JPPFTask task = new LongTask(length, false);
					task.setId("task " + i + ":" + j);
					job.addTask(task);
				}
				jobs.add(job);
			}
			long start = System.nanoTime();
			print("submitting the jobs");
			for (JPPFJob job: jobs) jppfClient.submit(job);
			print("getting the results");
			for (JPPFJob job: jobs)
			{
				JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
				collector.waitForResults();
				print("got results for " + job.getId());
			}
			long elapsed = System.nanoTime() - start;
			print("ran " + nbJobs + " in: "+StringUtils.toStringDuration(elapsed/1000000));
	
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Print a message tot he log and to the console.
	 * @param msg the message to print.
	 */
	private static void print(String msg)
	{
		log.info(msg);
		System.out.println(msg);
	}

	/**
	 * Get a proxy to the driver's job management MBean.
	 * @return an instance of {@link DriverJobManagementMBean}.
	 * @throws Exception if any error occurs.
	 */
	private static DriverJobManagementMBean getJobManagement() throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
		JMXDriverConnectionWrapper wrapper = c.getJmxConnection();
		return wrapper.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
	}

	/**
	 * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform3() throws Exception
	{
		try
		{
			long start = System.nanoTime();
			JPPFJob job = new JPPFJob();
			job.setId("test jaer download");
			job.addTask(new Task());
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			for (JPPFTask task: results)
			{
				Exception e = task.getException();
				if (e != null) throw e;
			}
			long elapsed = System.nanoTime() - start;
			print("run time: " + StringUtils.toStringDuration(elapsed/1000000));
	
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}
}
