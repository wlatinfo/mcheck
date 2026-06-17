package eu.izadpanah.mcheck;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ITabSocketServer implements Runnable {

    private final int port;
    private final ExecutorService threadPool;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    // Thread-safe list for external classes to register for messages
    private final List<MessageListener> inboundListeners = new CopyOnWriteArrayList<>();

    protected final Map<String, Socket> activeClients = new ConcurrentHashMap<>();

    public ITabSocketServer(int port, int maxThreads) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LoggerConfig.log("ITabSocketServer.Run", Level.INFO, "Server started on port " + port);

            while (isRunning.get()) {
                Socket clientSocket = serverSocket.accept();
                LoggerConfig.log("ITabSocketServer.Run", Level.INFO, "New connection: " + clientSocket.getRemoteSocketAddress());
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                LoggerConfig.log("ITabSocketServer.Run", Level.SEVERE, "Server Error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        activeClients.put(clientIp, socket);
        LoggerConfig.log("ITabSocketServer.Handle", Level.INFO, "Registered client: " + clientIp);
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while (isRunning.get() && (bytesRead = in.read(buffer)) != -1) {
                String rawMsg = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                if (rawMsg.isEmpty()) continue;

                if (Main.iTabLog)
                    System.out.println("ITabSocketServer.Handle received Message: " + rawMsg);

                try {
                    // Parse the XML once to get needed metadata
                    Document doc = parseXml(rawMsg);
                    String msgId = queryXml(doc, "//id");
                    String msgType = queryXml(doc, "//type");
                    String age= queryXml(doc,"//age");
                    //System.out.println(msgType);

                    //TODO:: update the Verification for Transaction
                    if (age!=null && !age.isEmpty()){
                        if (Boolean.parseBoolean(age))
                            if (!Main.transVerify.get())
                                Main.transVerify.set(true);
                        System.out.println("age Approve: "+ age );}
                    //TODO:: pass the iTab message type to handle the Transaction status
                    if (!Objects.equals(msgType, "System info"))
                        updateTransactionState(msgType);

                    // iTab automated Response Logic (ACK)
                    if (msgId != null && !msgId.isEmpty()) {
                        Main.mID=msgId;
                        sendResponse(out, msgId, "ack");
                    }

                    // Notify all external listeners/processes
                    // parse raw XML
                    if (age!=null && !age.isEmpty()) notifyInboundListeners(socket, rawMsg);

                } catch (Exception e) {
                    LoggerConfig.log("ITabSocketServer.Handle", Level.WARNING, "XML Processing Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LoggerConfig.log("ITabSocketServer.Handle", Level.INFO, "Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            activeClients.remove(clientIp);
            closeQuietly(socket);
        }
    }

    // Update the static variables for Logics
    private void updateTransactionState(String msgType) {
        System.out.println("Help for Customer : "+Main.transHelp);
        //System.out.println("Status will be updated");
        if (Objects.equals(msgType,"Start session")) {
            Main.tranState = "open";
            Main.transSession="start";
            if (Main.sentRmoteAgeAccept.get())
                Main.sentRmoteAgeAccept.set(false);
        }
        if (Objects.equals(msgType,"Help")){
            if (Main.transVerify.get())
                Main.transHelp.set(true);
        }
        if(Objects.equals(msgType,"Report article handled")) {
            Main.tranState="open";
            Main.transSession = "scanning";
        }
        if(Objects.equals(msgType,"Pay session"))
            Main.transSession="payment";
        if (Objects.equals(msgType,"End session")){
            Main.transSession="End session";
            Main.tranState="close";
            Main.procesState="none";
            Main.transVerify.set(false);
            Main.transHelp.set(false);
            Main.approvePrivacy.set(false);
            Main.verificationTimeout.set(false);
            Main.myCheckerHandler.disableFaceprocess();// Potential possibility to move it to FlowControl Class
            Main.sentRmoteAgeAccept.set(false);
            //Main.verificationCall.set(false);
            Main.verificationResult.set(false);
            Main.checkCase.set(false);
            Main.countDownfinished.set(false);
            Main.authFrame.textLayer.setColor(Color.WHITE);
            Main.startCountDown.set(false);
        }

    }

    /**
     * Sends a formatted XML response back to the client/iTab-API.
     * Without this automatic AK the iTab API will terminate the Session
     */
    private void sendResponse(OutputStream out, String id, String result) throws IOException {
        String response = ITabHandler.startItab +ITabHandler.interfaceVersion+"1.8"+ITabHandler.interfaceVersionEnd+
                        "<response>\n" +
                        "<id>" + id + "</id>\n" +
                        "<result>" + result + "</result>\n" +
                        "</response>\n" +
                        "</ITAB_SCO>\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
        if (!Main.itabOnline)
            Main.itabOnline=true;
        //System.out.println("ITabSocketServer.ResponseSent " + result + " for ID: " + id);
    }

    public void sendMessageToClient(String clientIp, String xml) {
        Socket socket = activeClients.get(clientIp);
        if (socket != null && !socket.isClosed()) {
            try {
                OutputStream out = socket.getOutputStream();
                out.write((xml + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                LoggerConfig.log("ITabSocketServer.Send", Level.INFO, "Message has been sent to iTab on :" + clientIp);
            } catch (IOException e) {
                LoggerConfig.log("ITabSocketServer.Send", Level.SEVERE, "Failed to send: " + e.getMessage());
            }
        } else {
            LoggerConfig.log("ITabSocketServer.Send", Level.WARNING, "Client " + clientIp + " not found or disconnected.");
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String queryXml(Document doc, String expression) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            return xPath.compile(expression).evaluate(doc);
        } catch (Exception e) {
            return "";
        }
    }

    //add function from other classes to the Socket initializer in order to register a Listener.
    // This function add new Listener to the Listeners List in Order to notify them
    public void addInboundListener(MessageListener listener) {
        this.inboundListeners.add(listener);
    }

    //send the inbound Message to all listeners
    private void notifyInboundListeners(Socket client, String xml) {
        for (MessageListener listener : inboundListeners) {
            try {
                listener.onMessage(client, xml);
            } catch (Exception e) {
                LoggerConfig.log("ITabSocketServer.Notify", Level.SEVERE, "Listener Error: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(Socket socket) {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}