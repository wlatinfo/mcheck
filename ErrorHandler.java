package eu.izadpanah.mcheck;

import java.net.Socket;

@FunctionalInterface
public interface ErrorHandler {
    void onError(Socket client, Exception e);
}
