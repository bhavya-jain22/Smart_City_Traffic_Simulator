import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;

public class SimulationPanel extends JPanel {

    public SimulationPanel() {
        setBackground(Color.WHITE); // base background
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother visuals
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        // Road color (dark gray asphalt)
        g2d.setColor(Color.DARK_GRAY);

        // --- Horizontal Road ---
        int roadWidth = 80;
        g2d.fillRect(0, getHeight()/2 - roadWidth/2, getWidth(), roadWidth);

        // --- Vertical Road ---
        g2d.fillRect(getWidth()/2 - roadWidth/2, 0, roadWidth, getHeight());
    }
}