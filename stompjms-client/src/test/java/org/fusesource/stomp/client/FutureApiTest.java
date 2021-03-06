/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stomp.client;

import static org.fusesource.stomp.client.Constants.DESTINATION;
import static org.fusesource.stomp.client.Constants.ID;
import static org.fusesource.stomp.client.Constants.MESSAGE;
import static org.fusesource.stomp.client.Constants.MESSAGE_ID;
import static org.fusesource.stomp.client.Constants.SEND;
import static org.fusesource.stomp.client.Constants.SUBSCRIBE;

import java.util.concurrent.Future;

import org.fusesource.stomp.codec.StompFrame;

import junit.framework.TestCase;
/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class FutureApiTest extends TestCase {
    ApolloBroker broker = new ApolloBroker();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        broker.start();
    }

    @Override
    protected void tearDown() throws Exception {
        broker.stop();
        super.tearDown();
    }

    public void testFutureInterface() throws Exception {

        Stomp stomp = new Stomp("localhost", broker.port);
        Future<FutureConnection> future = stomp.connectFuture();
        FutureConnection connection = future.get();

        // Lets setup a receive.. this does not block until you await it..
        Future<StompFrame> receiveFuture = connection.receive();

        StompFrame frame = new StompFrame(SUBSCRIBE);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(ID, connection.nextId());
        Future<StompFrame> response = connection.request(frame);

        // This unblocks once the response frame is received.
        assertNotNull(response.get());

        frame = new StompFrame(SEND);
        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
        frame.addHeader(MESSAGE_ID, StompFrame.encodeHeader("test"));
        Future<Void> sendFuture = connection.send(frame);

        // This unblocks once the frame is accepted by the socket.  Use it
        // to avoid flow control issues.
        sendFuture.get();

        // Try to get the received message.
        StompFrame received = receiveFuture.get();
        assertTrue(received.action().equals(MESSAGE));
        assertTrue(received.getHeader(MESSAGE_ID).toString().equals("test"));
    }
}
