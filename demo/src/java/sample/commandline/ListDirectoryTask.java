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

package sample.commandline;

import org.jppf.server.protocol.*;

/**
 * This task lists the files in a specified directory of the node's host.
 * @author Laurent Cohen
 */
public class ListDirectoryTask extends CommandLineTask
{
	/**
	 * Directory in which to list the files.
	 */
	private String dir = null;
	/**
	 * Determines whether this task should run on a linux or windows host.
	 */
	private boolean linux = true;

	/**
	 * Initialize the script's parameters.
	 * @param dir directory in which to list the files.
	 */
	public ListDirectoryTask(String dir)
	{
		this.dir = dir;
	}

	/**
	 * Execute the script.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			// get the name of the node's operating system
			String os = System.getProperty("os.name").toLowerCase();
			System.out.println("os: " + os);
			// the type of OS determines which command to execute
			if (os.indexOf("linux") >= 0) setCommandList("ls", "-a", dir, ">", "dirlist.txt");
			else setCommandList("cmd", "/C", "dir", dir, ">", "dirlist.txt");
			// set wehether the script output is captured
			setCaptureOutput(false);
			// execute the script/command
			launchProcess();
			// copy the resulting file in memory and set it as a result
			FileLocation fl = new FileLocation("dirlist.txt");
			byte[] buffer = new byte[(int) fl.size()];
			MemoryLocation ml = new MemoryLocation(buffer);
			fl.copyTo(ml);
			setResult(new String(buffer));
		}
		catch(Exception e)
		{
			setException(e);
		}
	}
}
