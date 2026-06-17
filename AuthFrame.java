package eu.izadpanah.mcheck;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AuthFrame extends  JFrame{
    private VideoPanel videoPanel;
    private StreamInitializer producer = null;
    protected boolean running = true;
    protected static boolean initialized =false;

    private JLayeredPane rightLayeredPane;
    private JLabel bgLayer;
    protected StyledTextLayer textLayer;

    public AuthFrame() {
        setTitle("MCheck Authentication");
        setUndecorated(true);
        setVisible(false);
        setSize(1024, 768);
        setAlwaysOnTop(true);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Cursor invisibleCursor = toolkit.createCustomCursor(new BufferedImage(1, 1, 2), new Point(0, 0), (String)null);
        this.setCursor(invisibleCursor);

        // --- LEFT SIDE: Two Rows ---
        JPanel leftContainer = new JPanel(new GridBagLayout());
        leftContainer.setBackground(new Color(41, 52, 58));
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.fill = GridBagConstraints.BOTH;
        gbcLeft.gridx = 0;

        JPanel videoWrapper = new JPanel(new BorderLayout());
        videoWrapper.setOpaque(false);
        videoWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        videoPanel = new VideoPanel();
        videoWrapper.setPreferredSize(new Dimension(512, 660));
        videoWrapper.add(videoPanel, BorderLayout.CENTER);

        gbcLeft.gridy = 0;
        gbcLeft.weightx = 1.0;
        gbcLeft.weighty = 0.0;
        leftContainer.add(videoWrapper, gbcLeft);

// --- Row 2: Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        Font font = new Font(Main.font, Font.BOLD, 20);
        RoundedButton btnOk = new RoundedButton("Zustimmen");
        RoundedButton btnCancel = new RoundedButton("Ablehnen");
        btnOk.setFont(font);
        btnCancel.setFont(font);
        btnOk.setPreferredSize(new Dimension(170, 80));
        btnCancel.setPreferredSize(new Dimension(170, 80));

        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);

        gbcLeft.gridy = 1;
        gbcLeft.weighty = 1.0;
        leftContainer.add(buttonPanel, gbcLeft);

        // --- RIGHT SIDE: Layered Setup ---
        rightLayeredPane = new JLayeredPane();
        //rightLayeredPane.setLayout(new FlowLayout(FlowLayout.LEADING,5,0));
        rightLayeredPane.setPreferredSize(new Dimension(512, 768));
        rightLayeredPane.setBackground(new Color(41, 52, 58));
        rightLayeredPane.setOpaque(true);

        int panelWidth = 512;
        int panelHeight = 768;

        bgLayer = new JLabel();
        bgLayer.setBounds(0, 0, panelWidth, panelHeight);
        bgLayer.setOpaque(false);
        bgLayer.setBackground(new Color(41, 52, 58));
        bgLayer.setHorizontalAlignment(SwingConstants.CENTER);
        bgLayer.setVerticalAlignment(SwingConstants.CENTER);
        updateDefaultImage("config/pic001.jpg",panelWidth,panelHeight);


        textLayer = new StyledTextLayer("EDEKA IT");
        textLayer.setBounds(0, 638, panelWidth, 150);
        textLayer.setOpaque(false);
        textLayer.setForeground(Color.CYAN);
        textLayer.setFont(new Font(Main.font, Font.BOLD, 60));

        rightLayeredPane.add(bgLayer, Integer.valueOf(0));
        rightLayeredPane.add(textLayer, Integer.valueOf(1));

        // --- SPLIT PANE ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, rightLayeredPane);
        splitPane.setDividerLocation(512);
        splitPane.setBorder(null);
        splitPane.setDividerSize(0);
        add(splitPane);

        // --- LISTENERS ---
        btnCancel.addActionListener(e -> {
            LoggerConfig.log("AuthFrame.Reject",Level.INFO,"Customer has rejected the Verification");
            Main.checkCase.set(false);
            stopStream();
            // Testing the Design for Process
            //updateProcess();
            //countdown();
        });

        btnOk.addActionListener(e -> {
            LoggerConfig.log("AuthFrame.Agree",Level.INFO,"Customer agreed to Verification");
            if (!Main.checkCase.get())
                Main.checkCase.set(true);
            Main.myCheckerHandler.enableFaceprocess();
            if(!Main.approvePrivacy.get())
                Main.approvePrivacy.set(true);
            startConsumerLoop();
        });
        //updateOverlayFontSize(55);
    }

    public void updateDefaultImage(String path, int targetWidth, int targetHeight) {
        try {
            ImageIcon icon = new ImageIcon(path);
            // Scale the image to the target dimensions smoothly
            Image img = icon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            bgLayer.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            System.out.println("Image failed to load: " + path);
        }
    }

    public void updateOverlayText(String text) {
        if (textLayer != null) {
            textLayer.setText(text);
        }
    }

    protected void updateProcess(){
        SwingUtilities.invokeLater(() -> {
            if (Objects.equals(Main.procesState,"none")) {
                return;
            }
            if (Objects.equals(Main.procesState,"processing")){
                updateDefaultImage("config/noperson.png", 250, 250);
                updateOverlayFontSize(60);
                updateOverlayText("In Prüfung ... ");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"timeout")){
                updateDefaultImage("config/timeout.png", 250, 250);
                updateOverlayFontSize(30);
                textLayer.setColor(Color.RED);
                updateOverlayText("Altersprüfung fehlgeschlagen");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"success") && !Main.startCountDown.get()) {
                updateDefaultImage("config/sucess.png", 250, 250);
                textLayer.setColor(Color.GREEN);
                updateOverlayFontSize(38);
                updateOverlayText("Altersprüfung erfolgreich");
                textLayer.setBounds(0, 538, 512, 230);
                Main.verificationResult.set(true);
                Main.authFrame.countdown();
                return;
            }
            if (Objects.equals(Main.procesState,"empty")){
                updateDefaultImage("config/nodetect.png", 250, 250);
                updateOverlayFontSize(40);
                textLayer.setColor(Color.ORANGE);
                updateOverlayText("Keine Person erkannt");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"faceplus")){
                updateDefaultImage("config/faceplus.png", 250, 250);
                textLayer.setColor(Color.ORANGE);
                updateOverlayText("mehrere Gesichte ");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"spoof")){
                updateDefaultImage("config/noperson.png", 250, 250);
                updateOverlayFontSize(40);
                textLayer.setColor(Color.RED);
                updateOverlayText("Keine Person erkannt");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"personplus")){
                updateDefaultImage("config/faceplus.png", 250, 250);
                textLayer.setColor(Color.ORANGE);
                updateOverlayText("mehrere Personen");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
            if (Objects.equals(Main.procesState,"noperson")){
                updateDefaultImage("config/noperson.png", 250, 250);
                updateOverlayFontSize(40);
                textLayer.setColor(Color.ORANGE);
                updateOverlayText("Keine Person erkannt");
                textLayer.setBounds(0, 538, 512, 230);
                return;
            }
        });
    }

    protected void countdown(){
        if (!Main.startCountDown.compareAndSet(false, true)) {
            return; // a countdown is already in progress
        }
        new Thread(() -> {
            for (int i = 4; i > 1; i--) {
                if (i>3){
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                updateOverlayFontSize(100);
                String s = String.valueOf(i - 1);
                SwingUtilities.invokeLater(() -> {
                    updateOverlayText(s);
                });
                try {
                    Thread.sleep(900);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Main.countDownfinished.set(true);
            Main.checkCase.set(false);
            Main.authFrame.stopStream();
            updateOverlayFontSize(60);
        }).start();
    }

    /**
     * Brings the application back to a clean, idle verification state the moment
     * a countdown (SUCCESS or TIMEOUT) has finished. Called from FlowControl as
     * soon as the countdown completes, so the NEXT verification starts fresh and
     * does NOT depend on the "End session" message arriving.
     *
     * Notes:
     *  - The session itself (tranState / transSession) is left untouched, because
     *    after a successful age accept the customer continues in the same session.
     *  - sentRmoteAgeAccept is deliberately NOT reset here: on success it was just
     *    set true to record that the accept was sent, and clearing it could cause a
     *    duplicate accept. It is reset on the next "Start session" / "End session".
     */
    protected void resetAfterCountdown() {
        // Stream + window teardown (idempotent — safe even if already stopped)
        stopStream();

        // Stop face processing on the ICU
        Main.myCheckerHandler.disableFaceprocess();

        // Reset every per-verification / countdown flag back to idle
        Main.procesState = "none";
        Main.startCountDown.set(false);
        Main.countDownfinished.set(false);
        Main.verificationResult.set(false);
        Main.verificationTimeout.set(false);
        Main.checkCase.set(false);
        Main.approvePrivacy.set(false);
        Main.transHelp.set(false);
        Main.transVerify.set(false);

        // Restore the overlay colour on the EDT
        SwingUtilities.invokeLater(() -> textLayer.setColor(Color.WHITE));
    }

    public void updateOverlayFontSize(int fontSize) {
        if (textLayer != null) {
            Font currentFont = textLayer.getFont();
            // Create a new font keeping the current family and style, but with the new size
            SwingUtilities.invokeLater(() -> {
                textLayer.setCustomFont( fontSize);
            });
        }
    }

    protected void stopStream(){
        SwingUtilities.invokeLater(() -> {
            videoPanel.updateImage(null);
            Main.authFrame.setVisible(false);
            Main.authFrame.updateOverlayText("EDEKA IT");
            Main.authFrame.updateDefaultImage("config/pic001.jpg",512,768);
            textLayer.setBounds(0, 638, 512, 150);
            Main.transVerify.set(false);
        });
        running = false;
        Main.authFrame.initialized = false;

        if (producer != null) {
            producer.stop();
        }
    }
    protected void startInitialize(String url){
        videoPanel.updateImage(null);
        running = true;
        Main.authFrame.initialized = true;

        producer = new StreamInitializer(url);
        new Thread(producer).start();
        //startConsumerLoop();
    }
    private void startConsumerLoop() {
        new Thread(() -> {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            LinkedBlockingQueue<Frame> queue = producer.getFrameQueue();

            while (running) { // Uses the instance variable
                try {
                    Frame frame = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (frame != null) {
                        BufferedImage bi = converter.convert(frame);
                        if (bi != null) {
                            videoPanel.updateImage(bi);
                        }
                        frame.close();
                    }
                } catch (Exception e) {
                    LoggerConfig.log("AuthFrame.Consumer", Level.WARNING, "Loop error: " + e.getMessage());
                }
            }
            LoggerConfig.log("AuthFrame.Consumer", Level.INFO, "Stream Consumer safely stopped.");
        }).start();
    }

    class VideoPanel extends JPanel {
        private BufferedImage currentImage;

        public void updateImage(BufferedImage img) {
            this.currentImage = img;
            repaint(); // Trigger paintComponent
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentImage != null) {
                // Draw image and scale it to fit the panel while maintaining aspect ratio
                g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                Font font = new Font(Main.font,Font.BOLD,32);
                g.setFont(font);
                g.drawString("Warte auf Zustimmung ....", 10, 354);
            }
        }
    }

}


class StyledTextLayer extends JPanel {
    private String text;
    private Font customFont;
    private Color textColor = new Color(255, 255, 255);

    public StyledTextLayer(String text) {
        this.text = text;
        this.setOpaque(false); // Keep background transparent
        this.customFont = new Font(Main.font, Font.BOLD, 40);
    }

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public void setColor(Color color) {
        this.textColor = color;
        repaint();
    }


    public void setCustomFont(int size) {
        this.customFont = new Font(customFont.getName(), customFont.getStyle(), size);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // 1. Enable Anti-Aliasing for perfectly smooth, round text edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(customFont);
        //g2d.setColor(textColor);
        FontMetrics fm = g2d.getFontMetrics();

        // Center the text horizontally
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int x2 =x-20;
        int y = fm.getAscent() + 20; // 20px padding from the top of this layer
        int y2=y-10;

        // 2. Draw a smooth Drop Shadow
        g2d.setColor(new Color(0, 0, 0, 180)); // Semi-transparent black
        g2d.drawString(text, x + 4, y + 4);    // Offset by 4 pixels down and right

        // 3. Draw the Main Text with a subtle Gradient (White to Light Gray)
        GradientPaint gradient = new GradientPaint(
                x, y - fm.getAscent(), textColor,
                x2, y2, new Color(200, 200, 200)
        );
        g2d.setPaint(gradient);
        g2d.drawString(text, x, y);

        g2d.dispose();
    }
}
