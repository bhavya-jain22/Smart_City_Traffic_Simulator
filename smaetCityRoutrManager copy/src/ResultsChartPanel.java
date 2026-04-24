import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ResultsChartPanel extends JPanel {

    private List<BenchmarkResult> results;

    public ResultsChartPanel(List<BenchmarkResult> results) {
        this.results = results;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (results == null || results.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // Split into two charts (left = Distance, right = Ticks)
        int midX = width / 2;

        drawBarChart(g2d, "Average Distance (Lower is better)", 50, 50, midX - 80, height - 100, true);
        drawBarChart(g2d, "Average Ticks / Time (Lower is better)", midX + 50, 50, midX - 80, height - 100, false);
    }

    private void drawBarChart(Graphics2D g2d, String title, int x, int y, int chartWidth, int chartHeight, boolean isDistance) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString(title, x + 20, y - 20);

        // Draw axes
        g2d.drawLine(x, y, x, y + chartHeight); // Y axis
        g2d.drawLine(x, y + chartHeight, x + chartWidth, y + chartHeight); // X axis

        // Find max value for scaling
        double maxVal = 0;
        for (BenchmarkResult r : results) {
            double val = isDistance ? r.averageDistance : r.averageTicks;
            if (val > maxVal) maxVal = val;
        }

        // Add 10% padding to top
        maxVal *= 1.1;

        int barWidth = chartWidth / (results.size() * 2 + 1);
        int currentX = x + barWidth;

        Color[] colors = {new Color(70, 130, 180), new Color(220, 20, 60), new Color(34, 139, 34)};

        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            double val = isDistance ? r.averageDistance : r.averageTicks;

            int barHeight = (int) ((val / maxVal) * chartHeight);
            
            g2d.setColor(colors[i % colors.length]);
            g2d.fillRect(currentX, (y + chartHeight) - barHeight, barWidth, barHeight);

            // Value label
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            String valStr = String.format("%.1f", val);
            g2d.drawString(valStr, currentX, (y + chartHeight) - barHeight - 5);

            // Name label on X axis
            g2d.drawString(r.algorithmName, currentX - 10, y + chartHeight + 20);

            currentX += barWidth * 2;
        }
    }

    public static void showResultsDialog(List<BenchmarkResult> results) {
        JFrame frame = new JFrame("Benchmark Graphical Comparison");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new ResultsChartPanel(results));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
