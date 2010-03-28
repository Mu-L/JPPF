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

package org.jppf.example.jmxlogger;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.apache.commons.logging.*;

/**
 * Implementation of a  simple logger that sends logged messages as JMX notifications.
 * @author Laurent Cohen
 */
public class JmxLogger extends NotificationBroadcasterSupport implements JmxLoggerMBean
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JmxLogger.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The mbrean object name sent with the notifications.
	 */
	private static final ObjectName OBJECT_NAME = makeObjectName();
	/**
	 * Sequence number generator.
	 */
	private static AtomicLong sequence = new AtomicLong(0);
	/**
	 * The log4j appender.
	 */
	private JmxAppender appender = null;

	/**
	 * Default constructor.
	 */
	public JmxLogger()
	{
		appender = new JmxAppender(this);
	}

	/**
	 * Log the specified message as a JMX notification.
	 * @param message the message to log.
	 * @see org.jppf.example.jmxlogger.JmxLoggerMBean#log(java.lang.Integer, java.lang.String, java.lang.Throwable)
	 */
	public void log(String message)
	{
		Notification notif = new Notification("JmxLogNotification", OBJECT_NAME, sequence.incrementAndGet(), message);
		sendNotification(notif);
	}

	/**
	 * Create the {@link ObjectName} used as source of the notifications.
	 * @return an {@link ObjectName} instance.
	 */
	private static ObjectName makeObjectName()
	{
		try
		{
			return new ObjectName(JmxLoggerMBean.JMX_LOGGER_MBEAN_NAME);
		}
		catch(Exception e)
		{
			log.error("failed to send JMX log notification", e);
		}
		return null;
	}
}
