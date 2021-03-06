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

import org.fusesource.stomp.codec.StompFrame;

import junit.framework.TestCase;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackApiTest extends TestCase {
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

    public void testCallbackInterface() throws Exception {
        final Promise<StompFrame> result = new Promise<StompFrame>();
        Stomp stomp = new Stomp("localhost", broker.port);
        stomp.connectCallback(new Callback<CallbackConnection>() {
            @Override
            public void onFailure(Throwable value) {
                result.onFailure(value);
            }

            @Override
            public void onSuccess(final CallbackConnection connection) {
                connection.receive(new Callback<StompFrame>() {
                    @Override
                    public void onFailure(Throwable value) {
                        result.onFailure(value);
                        connection.close(null);
                    }

                    @Override
                    public void onSuccess(StompFrame value) {
                        result.onSuccess(value);
                        connection.close(null);
                    }
                });

                connection.resume();

                StompFrame frame = new StompFrame(SUBSCRIBE);
                frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
                frame.addHeader(ID, connection.nextId());
                connection.request(frame, new Callback<StompFrame>() {
                    @Override
                    public void onFailure(Throwable value) {
                        result.onFailure(value);
                        connection.close(null);
                    }

                    @Override
                    public void onSuccess(StompFrame reply) {
                        StompFrame frame = new StompFrame(SEND);
                        frame.addHeader(DESTINATION, StompFrame.encodeHeader("/queue/test"));
                        frame.addHeader(MESSAGE_ID, StompFrame.encodeHeader("test"));
                        connection.send(frame, null);
                    }
                });

            }
        });
        StompFrame received = result.get();
        assertTrue(received.action().equals(MESSAGE));
        assertTrue(received.getHeader(MESSAGE_ID).toString().equals("test"));
    }
}
