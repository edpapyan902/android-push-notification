package com.notnoop.apns.internal;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;

import org.mockito.stubbing.OngoingStubbing;

public class MockingUtils {

    static SocketFactory mockSocketFactory(OutputStream stream) {
        try {
            Socket socket = mock(Socket.class);
            when(socket.getOutputStream()).thenReturn(stream);

            SocketFactory factory = mock(SocketFactory.class);
            when(factory.createSocket()).thenReturn(socket);
            when(factory.createSocket(anyString(), anyInt())).thenReturn(socket);

            return factory;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Cannot be here!");
        }
    }

    static SocketFactory mockClosedThenOpenSocket(OutputStream stream, boolean isClosed, int tries) {
        try {
            List<Socket> socketMocks = new ArrayList<Socket>(tries + 1);

            for (int i = 1; i < tries; ++i) {
                Socket socket = mock(Socket.class);
                if (isClosed) {
                    when(socket.isClosed()).thenReturn(true);
                    when(socket.isConnected()).thenReturn(false);
                    when(socket.getOutputStream()).thenThrow(
                            new AssertionError("Should have checked for closed connection"));
                } else {
                    when(socket.getOutputStream()).thenThrow(
                            new IOException("simulated IOException"));
                }
                socketMocks.add(socket);
            }

            Socket socket = mock(Socket.class);
            when(socket.getOutputStream()).thenReturn(stream);
            when(socket.isConnected()).thenReturn(true);
            socketMocks.add(socket);

            SocketFactory factory = mock(SocketFactory.class);
            OngoingStubbing<Socket> stubbing = when(factory.createSocket(anyString(), anyInt()));
            Socket first = socketMocks.remove(0);
            stubbing.thenReturn(first, socketMocks.toArray(new Socket[0]));

            return factory;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Cannot be here!");
        }
    }

}
