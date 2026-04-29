import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RaceModeGUI extends JFrame {

    // 3 independent graphs
    private Map<Integer, Intersection> graphAStar, graphDijkstra, graphGreedy;
    private List<Vehicle> vehiclesAStar   = new ArrayList<>();
    private List<Vehicle> vehiclesDijkstra = new ArrayList<>();
    private List<Vehicle> vehiclesGreedy   = new ArrayList<>();

    private List<SpawnRecord> spawnsAStar    = new ArrayList<>();
    private List<SpawnRecord> spawnsDijkstra = new ArrayList<>();
    private List<SpawnRecord> spawnsGreedy   = new ArrayList<>();

    private SimulationPanel panelAStar, panelDijkstra, panelGreedy;
    private Timer timer;
    private int globalTick = 0;
    private int stepsPerTick = 1;           // speed multiplier

    // Timing
    private long startTimeMs = 0;
    private long aStarFinishMs = 0, dijkstraFinishMs = 0, greedyFinishMs = 0;
    private boolean aStarDone = false, dijkstraDone = false, greedyDone = false;

    // Progress UI
    private JProgressBar progAStar, progDijkstra, progGreedy;
    private JLabel lblAStar, lblDijkstra, lblGreedy;
    private JLabel timerLabel;
    private javax.swing.Timer wallTimer;

    private int totalCars = 200;             // default, set by user

    // ---- constructor ----
    public RaceModeGUI() {
        super("Algorithm Race Mode – Live Showdown");
        setSize(1700, 980);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        buildTopBar();
        buildSimArea();
        buildFooter();
    }

    // ---- UI builders ----
    private void buildTopBar() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(new Color(22, 22, 30));
        top.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Row 1 – title + wall clock
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("⚡ ALGORITHM RACE MODE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(241, 196, 15));
        timerLabel = new JLabel("⏱ 00:00");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timerLabel.setForeground(Color.WHITE);
        titleRow.add(title);
        titleRow.add(timerLabel);
        top.add(titleRow);

        // Row 2 – progress bars + labels
        JPanel pbRow = new JPanel(new GridLayout(1, 3, 10, 0));
        pbRow.setOpaque(false);
        pbRow.setBorder(new EmptyBorder(6, 0, 0, 0));

        progAStar    = makeBar(new Color(46, 204, 113));
        progDijkstra = makeBar(new Color(52, 152, 219));
        progGreedy   = makeBar(new Color(231, 76, 60));

        lblAStar    = makeBarLabel("A*",       new Color(46, 204, 113));
        lblDijkstra = makeBarLabel("Dijkstra", new Color(52, 152, 219));
        lblGreedy   = makeBarLabel("Greedy",   new Color(231, 76, 60));

        pbRow.add(wrapBar(progAStar,    lblAStar));
        pbRow.add(wrapBar(progDijkstra, lblDijkstra));
        pbRow.add(wrapBar(progGreedy,   lblGreedy));
        top.add(pbRow);

        add(top, BorderLayout.NORTH);
    }

    private JProgressBar makeBar(Color c) {
        JProgressBar pb = new JProgressBar(0, totalCars);
        pb.setStringPainted(false);
        pb.setBackground(new Color(40, 40, 50));
        pb.setForeground(c);
        pb.setPreferredSize(new Dimension(0, 22));
        return pb;
    }

    private JLabel makeBarLabel(String name, Color c) {
        JLabel lbl = new JLabel(name + "  0 / " + totalCars);
        lbl.setForeground(c);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return lbl;
    }

    private JPanel wrapBar(JProgressBar pb, JLabel lbl) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);
        p.add(lbl, BorderLayout.WEST);
        p.add(pb,  BorderLayout.CENTER);
        return p;
    }

    private void buildSimArea() {
        JPanel simContainer = new JPanel(new GridLayout(1, 3, 4, 4));
        simContainer.setBackground(Color.BLACK);

        panelAStar    = new SimulationPanel(null, vehiclesAStar,    null);
        panelDijkstra = new SimulationPanel(null, vehiclesDijkstra, null);
        panelGreedy   = new SimulationPanel(null, vehiclesGreedy,   null);

        panelAStar   .setBorder(titledBorder("A* Routing",       new Color(46, 204, 113)));
        panelDijkstra.setBorder(titledBorder("Dijkstra Routing", new Color(52, 152, 219)));
        panelGreedy  .setBorder(titledBorder("Greedy Routing",   new Color(231, 76, 60)));

        simContainer.add(panelAStar);
        simContainer.add(panelDijkstra);
        simContainer.add(panelGreedy);
        add(simContainer, BorderLayout.CENTER);
    }

    private TitledBorder titledBorder(String text, Color c) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(c, 3), text,
            TitledBorder.CENTER, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 15), Color.WHITE);
    }

    private void buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        footer.setBackground(new Color(22, 22, 30));

        // Car count selector
        JLabel carLbl = new JLabel("Cars:");
        carLbl.setForeground(Color.WHITE);
        carLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JComboBox<String> carCombo = new JComboBox<>(new String[]{"100 Cars", "200 Cars", "400 Cars"});
        carCombo.setSelectedIndex(1);
        carCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Speed selector
        JLabel speedLbl = new JLabel("Sim Speed:");
        speedLbl.setForeground(Color.WHITE);
        speedLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JComboBox<String> speedCombo = new JComboBox<>(
            new String[]{"1x Normal", "2x Fast", "5x Turbo", "10x Ultra", "20x Max"});
        speedCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        speedCombo.addActionListener(e -> {
            String s = (String) speedCombo.getSelectedItem();
            if (s == null) return;
            switch (s) {
                case "2x Fast":   stepsPerTick = 2;  break;
                case "5x Turbo":  stepsPerTick = 5;  break;
                case "10x Ultra": stepsPerTick = 10; break;
                case "20x Max":   stepsPerTick = 20; break;
                default:          stepsPerTick = 1;  break;
            }
        });

        JButton startBtn = fancyBtn("▶ START RACE", new Color(46, 204, 113));
        JButton resetBtn = fancyBtn("↺ Reset",      new Color(200, 100, 0));

        startBtn.addActionListener(e -> {
            int cnt = carCombo.getSelectedIndex() == 0 ? 100
                    : carCombo.getSelectedIndex() == 2 ? 400 : 200;
            setupRace(cnt);
            startBtn.setEnabled(false);
            carCombo.setEnabled(false);
        });

        resetBtn.addActionListener(e -> {
            if (timer != null) timer.stop();
            if (wallTimer != null) wallTimer.stop();
            dispose();
            SwingUtilities.invokeLater(() -> new RaceModeGUI().setVisible(true));
        });

        footer.add(carLbl);   footer.add(carCombo);
        footer.add(speedLbl); footer.add(speedCombo);
        footer.add(startBtn);
        footer.add(resetBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private JButton fancyBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 2),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    // ---- race setup ----
    private void setupRace(int cars) {
        totalCars = cars;
        progAStar.setMaximum(cars);
        progDijkstra.setMaximum(cars);
        progGreedy.setMaximum(cars);
        lblAStar.setText("A*  0 / " + cars);
        lblDijkstra.setText("Dijkstra  0 / " + cars);
        lblGreedy.setText("Greedy  0 / " + cars);

        // Load 3 separate maps
        graphAStar    = MapLoader.loadMap("complex_grid_map.txt");
        graphDijkstra = MapLoader.loadMap("complex_grid_map.txt");
        graphGreedy   = MapLoader.loadMap("complex_grid_map.txt");

        // Inject maps into panels
        setField(panelAStar,    graphAStar);
        setField(panelDijkstra, graphDijkstra);
        setField(panelGreedy,   graphGreedy);

        // Identical seeded spawns
        Random rand = new Random(42);
        List<Intersection> na = new ArrayList<>(graphAStar.values());
        List<Intersection> nd = new ArrayList<>(graphDijkstra.values());
        List<Intersection> ng = new ArrayList<>(graphGreedy.values());

        spawnsAStar.clear(); spawnsDijkstra.clear(); spawnsGreedy.clear();
        for (int i = 0; i < cars; i++) {
            int si = rand.nextInt(na.size()), ei = rand.nextInt(na.size());
            while (si == ei) ei = rand.nextInt(na.size());
            int delay = rand.nextInt(1500);
            boolean amb = rand.nextInt(100) < 5;
            spawnsAStar.add   (new SpawnRecord(na.get(si), na.get(ei), delay, amb));
            spawnsDijkstra.add(new SpawnRecord(nd.get(si), nd.get(ei), delay, amb));
            spawnsGreedy.add  (new SpawnRecord(ng.get(si), ng.get(ei), delay, amb));
        }

        vehiclesAStar.clear(); vehiclesDijkstra.clear(); vehiclesGreedy.clear();
        globalTick = 0;
        aStarDone = dijkstraDone = greedyDone = false;

        startTimeMs = System.currentTimeMillis();

        // Wall clock
        wallTimer = new javax.swing.Timer(500, e -> {
            long elapsed = System.currentTimeMillis() - startTimeMs;
            timerLabel.setText(String.format("⏱ %02d:%02d", elapsed/60000, (elapsed/1000)%60));
        });
        wallTimer.start();

        // Sim timer
        timer = new Timer(33, e -> tick());
        timer.start();
    }

    /** Inject the graph into the SimulationPanel via reflection (field is private) */
    private void setField(SimulationPanel panel, Map<Integer, Intersection> graph) {
        try {
            java.lang.reflect.Field f = SimulationPanel.class.getDeclaredField("cityGraph");
            f.setAccessible(true);
            f.set(panel, graph);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ---- simulation tick ----
    private void tick() {
        for (int s = 0; s < stepsPerTick; s++) {
            globalTick++;
            if (globalTick % 60 == 0) {
                toggleLights(graphAStar);
                toggleLights(graphDijkstra);
                toggleLights(graphGreedy);
            }
            for (int i = 0; i < totalCars; i++) {
                if (spawnsAStar.get(i).spawnDelayTicks == globalTick) {
                    vehiclesAStar.add(new Vehicle(spawnsAStar.get(i).start, spawnsAStar.get(i).target, new AStarRouting(), spawnsAStar.get(i).isAmbulance));
                    vehiclesDijkstra.add(new Vehicle(spawnsDijkstra.get(i).start, spawnsDijkstra.get(i).target, new DijkstraRouting(), spawnsDijkstra.get(i).isAmbulance));
                    vehiclesGreedy.add(new Vehicle(spawnsGreedy.get(i).start, spawnsGreedy.get(i).target, new GreedyRouting(), spawnsGreedy.get(i).isAmbulance));
                }
            }
            moveAll(vehiclesAStar);
            moveAll(vehiclesDijkstra);
            moveAll(vehiclesGreedy);
        }

        int fa = countFinished(vehiclesAStar);
        int fd = countFinished(vehiclesDijkstra);
        int fg = countFinished(vehiclesGreedy);

        updateBar(progAStar,    lblAStar,    "A*",       fa);
        updateBar(progDijkstra, lblDijkstra, "Dijkstra", fd);
        updateBar(progGreedy,   lblGreedy,   "Greedy",   fg);

        long now = System.currentTimeMillis();
        if (fa == totalCars && !aStarDone)    { aStarDone    = true; aStarFinishMs    = now - startTimeMs; }
        if (fd == totalCars && !dijkstraDone) { dijkstraDone = true; dijkstraFinishMs = now - startTimeMs; }
        if (fg == totalCars && !greedyDone)   { greedyDone   = true; greedyFinishMs   = now - startTimeMs; }

        panelAStar.repaint();
        panelDijkstra.repaint();
        panelGreedy.repaint();

        if (fa == totalCars && fd == totalCars && fg == totalCars) {
            timer.stop();
            wallTimer.stop();
            SwingUtilities.invokeLater(this::showAnalysis);
        }
    }

    private void moveAll(List<Vehicle> list) {
        for (Vehicle v : list) if (!v.isFinished()) v.moveNextTick();
    }

    private int countFinished(List<Vehicle> list) {
        int c = 0; for (Vehicle v : list) if (v.isFinished()) c++; return c;
    }

    private void updateBar(JProgressBar pb, JLabel lbl, String name, int finished) {
        pb.setValue(finished);
        lbl.setText(name + "  " + finished + " / " + totalCars);
        if (finished == totalCars) pb.setForeground(new Color(46, 204, 113));
    }

    private void toggleLights(Map<Integer, Intersection> g) {
        for (Intersection n : g.values()) n.isHorizontalGreen = !n.isHorizontalGreen;
    }

    // ---- post-race analysis dialog ----
    private void showAnalysis() {
        // Compute stats
        double aStarAvgTicks    = avgTicks(vehiclesAStar);
        double dijkstraAvgTicks = avgTicks(vehiclesDijkstra);
        double greedyAvgTicks   = avgTicks(vehiclesGreedy);

        double aStarAvgDist    = avgDist(vehiclesAStar);
        double dijkstraAvgDist = avgDist(vehiclesDijkstra);
        double greedyAvgDist   = avgDist(vehiclesGreedy);

        String winner = determineWinner(aStarFinishMs, dijkstraFinishMs, greedyFinishMs);
        String distWinner = determineDistWinner(aStarAvgDist, dijkstraAvgDist, greedyAvgDist);

        // Build panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(22, 22, 30));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        panel.add(header("🏆 Race Analysis Report", new Color(241, 196, 15)));
        panel.add(Box.createVerticalStrut(12));

        // Results table
        String[][] data = {
            {"Algorithm", "Finish Time", "Avg Ticks/Car", "Avg Distance/Car"},
            {"A*",       fmt(aStarFinishMs),    String.format("%.1f", aStarAvgTicks),    String.format("%.0f", aStarAvgDist)},
            {"Dijkstra", fmt(dijkstraFinishMs), String.format("%.1f", dijkstraAvgTicks), String.format("%.0f", dijkstraAvgDist)},
            {"Greedy",   fmt(greedyFinishMs),   String.format("%.1f", greedyAvgTicks),   String.format("%.0f", greedyAvgDist)},
        };

        Color[] rowColors = {
            new Color(50, 50, 60),
            new Color(46, 204, 113, 80),
            new Color(52, 152, 219, 80),
            new Color(231, 76, 60, 80)
        };

        JPanel table = new JPanel(new GridLayout(4, 4, 2, 2));
        table.setBackground(new Color(22, 22, 30));
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                JLabel cell = new JLabel(data[r][c], SwingConstants.CENTER);
                cell.setFont(new Font("Segoe UI", r == 0 ? Font.BOLD : Font.PLAIN, 14));
                cell.setForeground(Color.WHITE);
                cell.setOpaque(true);
                cell.setBackground(rowColors[r]);
                cell.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 80), 1));
                cell.setPreferredSize(new Dimension(180, 36));
                table.add(cell);
            }
        }
        panel.add(table);
        panel.add(Box.createVerticalStrut(16));

        // Conclusion
        panel.add(header("📋 Conclusion", Color.WHITE));
        panel.add(Box.createVerticalStrut(6));
        panel.add(conclusionLine("🥇 Fastest completion:   " + winner,           new Color(241, 196, 15)));
        panel.add(conclusionLine("📏 Shortest avg path:   " + distWinner,         new Color(46, 204, 113)));
        panel.add(Box.createVerticalStrut(10));
        panel.add(conclusionLine("A*  : Uses a heuristic (Euclidean distance) to guide search. Typically the best balance of speed and optimality.", new Color(150, 255, 150)));
        panel.add(conclusionLine("Dijkstra: Explores all shortest paths from source. Finds optimal paths but does more work — slower on large maps.", new Color(100, 180, 255)));
        panel.add(conclusionLine("Greedy : Only looks at the estimated distance to goal. Very fast but can take suboptimal routes; may get stuck.", new Color(255, 130, 130)));
        panel.add(Box.createVerticalStrut(10));
        panel.add(conclusionLine("📊 Simulation used " + totalCars + " cars with identical start/end pairs across all three algorithms.", Color.LIGHT_GRAY));

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(780, 480));
        scroll.getViewport().setBackground(new Color(22, 22, 30));
        scroll.setBorder(null);

        JOptionPane.showMessageDialog(this, scroll,
            "Race Complete – Algorithm Analysis", JOptionPane.PLAIN_MESSAGE);
    }

    private JLabel header(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 17));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel conclusionLine(String text, Color c) {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    private String fmt(long ms) {
        return String.format("%d:%02d.%d", ms/60000, (ms/1000)%60, (ms/100)%10);
    }

    private double avgTicks(List<Vehicle> list) {
        return list.stream().mapToLong(v -> v.ticksAlive).average().orElse(0);
    }

    private double avgDist(List<Vehicle> list) {
        return list.stream().mapToDouble(v -> v.totalDistanceTraveled).average().orElse(0);
    }

    private String determineWinner(long a, long d, long g) {
        if (a <= d && a <= g) return "A* (" + fmt(a) + ")";
        if (d <= a && d <= g) return "Dijkstra (" + fmt(d) + ")";
        return "Greedy (" + fmt(g) + ")";
    }

    private String determineDistWinner(double a, double d, double g) {
        if (a <= d && a <= g) return "A* (" + String.format("%.0f", a) + ")";
        if (d <= a && d <= g) return "Dijkstra (" + String.format("%.0f", d) + ")";
        return "Greedy (" + String.format("%.0f", g) + ")";
    }
}
