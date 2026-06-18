package eu.izadpanah.mcheck;

import javax.swing.*;
import java.util.Objects;

public class FlowControl implements Runnable {
    private String akt = "";

    @Override
    public void run() {
        while (true) {
            if (!Objects.equals(akt, Main.transSession)) {
                akt = Main.transSession;
                System.out.println("Status: " + akt);
            }

            if (Objects.equals(Main.tranState, "open")) {
                // Check using .get() for AtomicBoolean.
                // Only ever OPEN the overlay at the start of a verification. Once a
                // result/timeout/countdown exists, the flow is tearing down — re-opening
                // here is what left the window stuck visible ("frozen") after success.
                if (Main.transVerify.get() && !AuthFrame.initialized && Main.transHelp.get()
                        && !Main.verificationResult.get()
                        && !Main.verificationTimeout.get()
                        && !Main.countDownfinished.get()) {
                    Main.authFrame.initialized = true;
                    SwingUtilities.invokeLater(() -> {
                        Main.authFrame.startInitialize(Main.streamurl);
                        Main.authFrame.setVisible(true);
                    });
                }

                if (Main.verificationResult.get() && Main.countDownfinished.get()) {
                    Main.iTabHandler.remoteAgeAccept();
                    Main.authFrame.stopStream();
                    Main.transVerify.set(false);
                    Main.sentRmoteAgeAccept.set(true);
                    Main.myCheckerHandler.disableFaceprocess();
                    Main.verificationResult.set(false);
                    SwingUtilities.invokeLater(() -> {
                        Main.authFrame.setVisible(false);
                    });
                }

                if (Main.verificationTimeout.get()) {
                    Main.myCheckerHandler.disableFaceprocess();
                    /*if(!Main.rejectShow){
                        try {
                            Thread.sleep(1000);
                            return;
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }*/
                    if (!Main.startCountDown.get())
                        Main.authFrame.countdown();
                    //Main.verificationTimeout.set(true);
                }
            }

            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(Main.countDownfinished.get()){
                if (Main.authFrame.isVisible())
                    SwingUtilities.invokeLater(() -> {
                        Main.authFrame.setVisible(false);
                    });
            }
        }
    }
}
