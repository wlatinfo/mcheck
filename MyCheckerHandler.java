package eu.izadpanah.mcheck;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MyCheckerHandler implements Runnable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int attemptCounter = 0; // Added counter to track seconds

    public MyCheckerHandler() {
        // Schedule status check every 1 second
        scheduler.scheduleAtFixedRate(this::performVerificationCycle, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
    }

    private void performVerificationCycle() {
        // If checking is not active, reset counter and exit
        if (!Main.checkCase.get()) {
            attemptCounter = 0;
            return;
        }

        // Safety check: Don't run if AuthFrame isn't ready yet or privacy not approved
        if (Main.authFrame == null || !AuthFrame.initialized || !Main.approvePrivacy.get()) {
            return;
        }

        // Check if we reached the waiting limit (timeout)
        if (attemptCounter >= Main.waiting) {
            handleTimeout();
            return;
        }

        // Increment counter as we are still within the waiting time
        attemptCounter++;
        callStatus();
    }

    private void handleTimeout() {
        Main.checkCase.set(false);
        Main.verificationTimeout.set(true);
        Main.procesState = "timeout";
        attemptCounter = 0; // Reset counter

        // Update UI
        Main.authFrame.updateProcess();
        LoggerConfig.log("MyCheckerHandler.performVerificationCycle", Level.WARNING, "Verification timed out after " + Main.waiting + " seconds.");
    }

    private void callStatus() {
        // Note: Check is already performed in performVerificationCycle
        //Main.verificationCall.set(true);

        JSONObject res = getStatus();

        if (res != null && res.has("Detections")) {
            if (Main.icuLog)
                LoggerConfig.log("MyCheckerHandler.callStatus", Level.INFO, "ICU Response : " + res);

            JSONArray detect = res.getJSONArray("Detections");
            JSONArray faces = res.getJSONArray("FaceInFrame");
            JSONObject face = faces.getJSONObject(0);
            int faceCount = face.optInt("FaceCount", 0);
            int personCount = face.optInt("PersonCount", 0);
            boolean faceDetect = face.optBoolean("Face", false);
            boolean spoof = face.optBoolean("Spoof", false);

            if (faces.length() > 0 && !Main.verificationResult.get() && !Main.verificationTimeout.get() && faceCount>0 ) {
                if (personCount == 1) {
                    // Update state to processing if it wasn't already
                    if (!Main.procesState.equals("processing")) {
                        Main.procesState = "processing";
                        Main.authFrame.updateProcess();
                    }
                    if (faceCount == 1 && faceDetect) {
                        if (!spoof && !detect.isEmpty()) {
                            int age = detect.getJSONObject(0).optInt("Age", 0);
                            if (age >= Main.ageThreashold && !Main.verificationResult.get()) {
                                LoggerConfig.log("MycheckerHandler.callStatus",Level.INFO,"Age is verified with: "+age);
                                Main.checkCase.set(false);
                                Main.procesState = "success";
                                Main.verificationResult.set(true);
                                Main.authFrame.updateProcess();
                            }
                        } else { // The Spoof case detection
                            if (spoof){
                                if (!Main.procesState.equals("spoof")) {
                                    Main.procesState = "spoof";
                                    Main.authFrame.updateProcess();
                                }
                            } else {
                                // The empty case detection
                                if (!Main.procesState.equals("noperson")) {
                                    Main.procesState = "noperson";
                                    Main.authFrame.updateProcess();
                                }
                            }
                        }
//                    } else if (faceCount > 1) {
//                        if (!Main.procesState.equals("faceplus")) {
//                            Main.procesState = "faceplus";
//                            Main.authFrame.updateProcess();
//                        }
                    }
                } else if (personCount > 1) {
//                    if (!Main.procesState.equals("personplus")) {
//                        Main.procesState = "personplus";
//                        Main.authFrame.updateProcess();
//                    }
                }
            } else {
                if (!Main.procesState.equals("noperson")) {
                    Main.procesState = "noperson";
                    Main.authFrame.updateProcess();
                }
            }
        }
    }


    protected JSONObject getStatus() {
        try {
            String res = Main.icuService.fetchJsonResponseWithToken(Main.api + "/api/v1_0/status", "GET", Main.token, "");
            return new JSONObject(res);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String faceProcss(String jb) {
        try {
            return Main.icuService.fetchJsonResponseWithToken(Main.api + "/api/v1_0/faceprocess", "POST", Main.token, jb);
        } catch (Exception e) {
            return null;
        }
    }

    protected String enableFaceprocess() {
        StringJoiner faceProcess = new StringJoiner(",");
        faceProcess.add("{\"CameraIndex\":0");
        faceProcess.add("\"Enabled\":true}");
        return faceProcss(faceProcess.toString());
    }

    protected String disableFaceprocess() {
        StringJoiner faceProcess = new StringJoiner(",");
        faceProcess.add("{\"CameraIndex\":0");
        faceProcess.add("\"Enabled\":false}");
        return faceProcss(faceProcess.toString());
    }
}