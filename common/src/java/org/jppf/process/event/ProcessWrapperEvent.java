/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.process.event;

import java.util.EventObject;

/**
 * Instances of this class encapsulate an event occurring when a process wrties to
 * its output or error stream. 
 * @author Laurent Cohen
 */
public class ProcessWrapperEvent extends EventObject
{
	/**
	 * Initialize this event with the specified source.
	 * @param content the source of this event, in effect the content of the corresponding process stream.
	 */
	public ProcessWrapperEvent(String content)
	{
		super(content);
	}

	/**
	 * Get the content of this event.
	 * @return the content encapsualted in a <code>StringBuilder</code> instance.
	 */
	public String getContent()
	{
		return (String) getSource();
	}
}
