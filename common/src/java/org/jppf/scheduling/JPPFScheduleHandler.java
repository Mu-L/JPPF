/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.scheduling;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class handles a timer.
 * @author Laurent Cohen
 */
public class JPPFScheduleHandler
{
	/**
	 * Timer that will trigger an action when a schedule date is reached.
	 */
	private Timer timer = null;
	/**
	 * Count of the instances of this class, added as a suffix to the timer's name.
	 */
	private static AtomicInteger instanceCount = new AtomicInteger(0);
	/**
	 * Mapping of timer tasks to a key.
	 */
	private Map<Object, TimerTask> timerTaskMap = new Hashtable<Object, TimerTask>();

	/**
	 * Initialize this schedule handler with a default name.
	 */
	public JPPFScheduleHandler()
	{
		timer = new Timer("JPPFScheduleHandler timer - " + instanceCount.incrementAndGet());
	}

	/**
	 * Initialize this schedule handler with the specified name.
	 * @param name the name given to this schedule handler.
	 */
	public JPPFScheduleHandler(String name)
	{
		timer = new Timer(name);
	}

	/**
	 * Schedule an action.
	 * @param key key used to retrieve or cancel the action at a later time.
	 * @param config the schedule at which the action is triggered.
	 * @param action the action to perform when the schedule date is reached.
	 * @throws ParseException if the schedule date could not be parsed
	 */
	public void scheduleAction(Object key, JPPFSchedule config, Runnable action) throws ParseException
	{
		long date = -1L;
		if (config.getDuration() > 0)
		{
			date = System.currentTimeMillis() + config.getDuration();
		}
		else
		{
			Date d = config.getDateFormat().parse(config.getDate());
			date = d.getTime();
		}
		ScheduleHandlerTask task = new ScheduleHandlerTask(key, action);
		timerTaskMap.put(key, task);
		timer.schedule(task, new Date(date));
	}

	/**
	 * Cancel the scheduled action identified by the specified key.
	 * @param key the key associated with the action.
	 */
	public void cancelAction(Object key)
	{
		TimerTask task = null;
		task = timerTaskMap.remove(key);
		if (task != null) task.cancel();
	}

	/**
	 * Timer task that triggers an action when the corresponding schedule date is reached.
	 */
	public class ScheduleHandlerTask extends TimerTask
	{
		/**
		 * Runnable action to perform.
		 */
		private Runnable action = null;
		/**
		 * The key associated witht this action.
		 */
		private Object key = null;

		/**
		 * Timer task wrapping a scheduled action.
		 * @param key the key associated with the action.
		 * @param action the action to perform when the schedule date is reached.
		 */
		public ScheduleHandlerTask(Object key, Runnable action)
		{
			this.key = key;
			this.action = action;
		}

		/**
		 * Check if the scheduled date has been reached and execute the corresponding action 
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			timerTaskMap.remove(key);
			action.run();
		}
	}

	/**
	 * Shutdown this schedule handler.
	 */
	public void clear()
	{
		timer.cancel();
		timer.purge();
		timerTaskMap.clear();
	}
}
