package eu.izadpanah.mcheck;

import java.net.Socket;

@FunctionalInterface
public interface MessageListener {
    void onMessage(Socket client, String xmlPayload);
}

