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
                // Check using .get() for AtomicBoolean
                if (Main.transVerify.get() && !AuthFrame.initialized && Main.transHelp.get()) {
                    Main.authFrame.initialized = true;
                    SwingUtilities.invokeLater(() -> {
                        Main.authFrame.startInitialize(Main.streamurl);
                        Main.authFrame.setVisible(true);
                    });
                }

                // SUCCESS: the countdown has ended after a successful verification.
                // Send the accept to iTab FIRST (it relies on verificationResult /
                // countDownfinished still being true), then reset everything.
                if (Main.verificationResult.get() && Main.countDownfinished.get()) {
                    Main.iTabHandler.remoteAgeAccept();
                    Main.sentRmoteAgeAccept.set(true);
                    Main.authFrame.resetAfterCountdown();
                }

                // TIMEOUT: start the countdown over the timeout image, then reset
                // everything once that countdown has finished.
                if (Main.verificationTimeout.get()) {
                    Main.myCheckerHandler.disableFaceprocess();
                    if (!Main.startCountDown.get()) {
                        Main.authFrame.countdown();
                    } else if (Main.countDownfinished.get()) {
                        Main.authFrame.resetAfterCountdown();
                    }
                }
            }

            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
