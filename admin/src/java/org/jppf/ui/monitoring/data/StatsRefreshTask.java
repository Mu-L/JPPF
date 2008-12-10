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

package org.jppf.ui.monitoring.data;

import java.util.TimerTask;

import org.jppf.client.*;

/**
 * Instances of this class are tasks run periodically from a timer thread, requesting the latest
 * statistics form a JPPF driver connection each time they are run.
 * @author Laurent Cohen
 */
public class StatsRefreshTask extends TimerTask
{
	/**
	 * Client connection to request the data from.
	 */
	private JPPFClientConnectionImpl connection = null;

	/**
	 * Initialize this task with a specified client connection.
	 * @param connection the connection to use to request data.
	 */
	public StatsRefreshTask(JPPFClientConnectionImpl connection)
	{
		this.connection = connection;
	}

	/**
	 * Request an update from the JPPF driver.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		if (JPPFClientConnectionStatus.ACTIVE.equals(connection.getStatus()))
			StatsHandler.getInstance().requestUpdate(connection);
	}
}
