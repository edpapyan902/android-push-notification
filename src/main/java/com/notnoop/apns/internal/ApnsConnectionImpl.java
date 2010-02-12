/*
 * Copyright 2009, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.notnoop.apns.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.notnoop.apns.ApnsDelegate;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ReconnectPolicy;
import com.notnoop.exceptions.NetworkIOException;

public class ApnsConnectionImpl implements ApnsConnection {
    private static final Logger logger = LoggerFactory.getLogger(ApnsConnectionImpl.class);

    private final SocketFactory factory;
    private final String host;
    private final int port;
    private final Socket underlyingSocket;
    private final ReconnectPolicy reconnectPolicy;
    private final ApnsDelegate delegate;

    public ApnsConnectionImpl(SocketFactory factory, String host, int port) {
        this(factory, host, port, new ReconnectPolicies.Never(), ApnsDelegate.EMPTY);
    }

    public ApnsConnectionImpl(SocketFactory factory, String host,
            int port, ReconnectPolicy reconnectPolicy,
            ApnsDelegate delegate) {
        this(factory, host, port, null, reconnectPolicy, delegate);
    }

    public ApnsConnectionImpl(SocketFactory factory, String host,
            int port, Socket underlyingSocket,
            ReconnectPolicy reconnectPolicy, ApnsDelegate delegate) {
        this.factory = factory;
        this.host = host;
        this.port = port;
        this.reconnectPolicy = reconnectPolicy;
        this.delegate = delegate == null ? ApnsDelegate.EMPTY : delegate;
        this.underlyingSocket = underlyingSocket;
    }


    public synchronized void close() {
        try {
            if (socket != null)
                this.socket.close();
        } catch (IOException e) {
            logger.debug("Error while closing socket connection", e);
        }
    }


    // This method is only called from sendMessage.  sendMessage
    // has the required logic for retrying
    private Socket socket;
    private synchronized Socket socket() throws NetworkIOException {
        if (reconnectPolicy.shouldReconnect()) {
            Utilities.close(socket);
            socket = null;
        }

        if (socket == null || socket.isClosed()) {
            try {
                if (underlyingSocket == null) {
                    socket = factory.createSocket(host, port);
                } else {
                    underlyingSocket.connect(new InetSocketAddress(host, port));
                    socket = ((SSLSocketFactory)factory).createSocket(underlyingSocket, host, port, false);
                }
                reconnectPolicy.reconnected();
                logger.debug("Made a new connection to APNS");
            } catch (IOException e) {
                logger.error("Couldn't connect to APNS server", e);
                throw new NetworkIOException(e);
            }
        }
        return socket;
    }

    int DELAY_IN_MS = 1000;

    private static final int RETRIES = 3;
    public synchronized void sendMessage(ApnsNotification m) throws NetworkIOException {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                Socket socket = socket();
                socket.getOutputStream().write(m.marshall());
                socket.getOutputStream().flush();
                delegate.messageSent(m);

                logger.debug("Message \"{}\" sent", m);

                attempts = 0;
                break;
            } catch (Exception e) {
                if (attempts >= RETRIES) {
                    logger.error("Couldn't send message " + m, e);
                    delegate.messageSendFailed(m, e);
                    Utilities.wrapAndThrowAsRuntimeException(e);
                }
                logger.warn("Failed to send message " + m + "... trying again", e);
                // The first failure might be due to closed connection
                // don't delay quite yet
                if (attempts != 1)
                    Utilities.sleep(DELAY_IN_MS);
                Utilities.close(socket);
                socket = null;
            }
        }
    }

    public ApnsConnectionImpl copy() {
        return new ApnsConnectionImpl(factory, host, port, reconnectPolicy, delegate);
    }

    public void testConnection() throws NetworkIOException {
        ApnsConnectionImpl testConnection = new ApnsConnectionImpl(factory, host, port, reconnectPolicy, ApnsDelegate.EMPTY);
        testConnection.sendMessage(new ApnsNotification(new byte[] {0}, new byte[]{0}));
        testConnection.close();
    }
}
