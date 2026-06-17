package eu.izadpanah.mcheck;

import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {
    public RoundedButton(String label) {
        super(label);
        setOpaque(false);  // Allow transparency
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(50, 50));
    }
    public RoundedButton(String label , int w) {
        super(label);
        setOpaque(true);  // Allow transparency
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(w, 50));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Semi-transparent fill
        g2.setColor(new Color(250, 250, 250)); // RGBA: black with alpha
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);  // Rounded corners

        // Draw the button text
        super.paintComponent(g);
        g2.dispose();
    }
}
