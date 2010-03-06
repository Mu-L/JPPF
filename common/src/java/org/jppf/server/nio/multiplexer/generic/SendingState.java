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

package org.jppf.server.nio.multiplexer.generic;

import static org.jppf.server.nio.multiplexer.generic.MultiplexerTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * State of sending data on a channel.
 * @author Laurent Cohen
 */
public class SendingState extends MultiplexerServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingState(MultiplexerNioServer server)
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
	public MultiplexerTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		MultiplexerContext context = (MultiplexerContext) key.attachment();
		if (context.hasPendingMessage() && (context.getCurrentMessage() == null))
		{
			ByteBufferWrapper message = context.nextPendingMessage();
			context.setCurrentMessage(message.buffer);
			if (debugEnabled) log.debug(getRemoteHost(channel) + " about to send message #" + message.order +
				": " + (message.buffer.limit()+1) + " bytes");
		}
		if (context.getCurrentMessage() == null) return TO_SENDING_OR_RECEIVING;
		if (context.writeMultiplexerMessage((WritableByteChannel) channel))
		{
			if (debugEnabled) log.debug(getRemoteHost(channel) + " message sent");
			context.setCurrentMessage(null);
			return context.hasPendingMessage() ? TO_SENDING : TO_SENDING_OR_RECEIVING;
		}
		return TO_SENDING;
	}
}
