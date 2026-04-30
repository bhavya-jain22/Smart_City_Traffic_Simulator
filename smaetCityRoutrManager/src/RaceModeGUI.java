import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RaceModeGUI extends JFrame {
    private static final String[] NAMES = { "A*", "Dijkstra", "Bellman-Ford" };
    private static final Color[] COLORS = { new Color(46, 204, 113), new Color(52, 152, 219), new Color(231, 76, 60) };

    // Index: 0=A*, 1=Dijkstra, 2=Bellman-Ford
    @SuppressWarnings("unchecked")
    private final List<Vehicle>[] vehicles = new ArrayList[3];
    @SuppressWarnings("unchecked")
    private final List<SpawnRecord>[] spawns = new ArrayList[3];
    private final Map<Integer, Intersection>[] graphs = new Map[3];
    private final SimulationPanel[] panels = new SimulationPanel[3];
    private final JProgressBar[] bars = new JProgressBar[3];
    private final JLabel[] barLabels = new JLabel[3];
    private final boolean[] done = new boolean[3];
    private final long[] finishMs = new long[3];

    private javax.swing.Timer simTimer;
    private javax.swing.Timer wallTimer;
    private int globalTick, totalCars = 200, stepsPerTick = 1;
    private long startTimeMs;
    private JLabel timerLabel;

    public RaceModeGUI() {
        super("Algorithm Race Mode – Live Showdown");
        for (int i = 0; i < 3; i++) {
            vehicles[i] = new ArrayList<>();
            spawns[i] = new ArrayList<>();
        }
        setSize(1700, 980);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildSimArea(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── TOP BAR ───────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(new Color(22, 22, 30));
        top.setBorder(new EmptyBorder(8, 10, 8, 10));

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

        JPanel pbRow = new JPanel(new GridLayout(1, 3, 10, 0));
        pbRow.setOpaque(false);
        pbRow.setBorder(new EmptyBorder(6, 0, 0, 0));
        for (int i = 0; i < 3; i++) {
            bars[i] = new JProgressBar(0, totalCars);
            bars[i].setBackground(new Color(40, 40, 50));
            bars[i].setForeground(COLORS[i]);
            bars[i].setPreferredSize(new Dimension(0, 22));
            barLabels[i] = new JLabel(NAMES[i] + "  0 / " + totalCars);
            barLabels[i].setForeground(COLORS[i]);
            barLabels[i].setFont(new Font("Segoe UI", Font.BOLD, 14));
            JPanel wrap = new JPanel(new BorderLayout(4, 0));
            wrap.setOpaque(false);
            wrap.add(barLabels[i], BorderLayout.WEST);
            wrap.add(bars[i], BorderLayout.CENTER);
            pbRow.add(wrap);
        }
        top.add(pbRow);
        return top;
    }

    // ── 3-PANEL SIMULATION AREA ───────────────────────────────────────
    private JPanel buildSimArea() {
        JPanel area = new JPanel(new GridLayout(1, 3, 4, 4));
        area.setBackground(Color.BLACK);
        for (int i = 0; i < 3; i++) {
            panels[i] = new SimulationPanel(null, vehicles[i], null);
            panels[i].setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(COLORS[i], 3),
                    NAMES[i] + " Routing", TitledBorder.CENTER, TitledBorder.TOP,
                    new Font("Segoe UI", Font.BOLD, 15), Color.WHITE));
            area.add(panels[i]);
        }
        return area;
    }

    // ── FOOTER CONTROLS ───────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        footer.setBackground(new Color(22, 22, 30));

        JLabel carLbl = styledLabel("Cars:");
        JComboBox<String> carCombo = new JComboBox<>(new String[] { "100 Cars", "200 Cars", "400 Cars" });
        carCombo.setSelectedIndex(1);

        JLabel speedLbl = styledLabel("Sim Speed:");
        JComboBox<String> speedCombo = new JComboBox<>(
                new String[] { "1x Normal", "2x Fast", "5x Turbo", "10x Ultra", "20x Max" });
        speedCombo.addActionListener(e -> {
            String s = (String) speedCombo.getSelectedItem();
            stepsPerTick = s == null ? 1
                    : s.startsWith("2") ? 2
                            : s.startsWith("5") ? 5 : s.startsWith("10") ? 10 : s.startsWith("20") ? 20 : 1;
        });

        JButton startBtn = fBtn("▶ START RACE", new Color(46, 204, 113));
        JButton resetBtn = fBtn("↺ Reset", new Color(200, 100, 0));

        startBtn.addActionListener(e -> {
            int cnt = carCombo.getSelectedIndex() == 0 ? 100 : carCombo.getSelectedIndex() == 2 ? 400 : 200;
            setupRace(cnt);
            startBtn.setEnabled(false);
            carCombo.setEnabled(false);
        });
        resetBtn.addActionListener(e -> {
            stopAll();
            dispose();
            new RaceModeGUI().setVisible(true);
        });

        footer.add(carLbl);
        footer.add(carCombo);
        footer.add(speedLbl);
        footer.add(speedCombo);
        footer.add(startBtn);
        footer.add(resetBtn);
        return footer;
    }

    // ── RACE LOGIC ────────────────────────────────────────────────────
    private void setupRace(int cars) {
        totalCars = cars;
        for (int i = 0; i < 3; i++) {
            bars[i].setMaximum(cars);
            barLabels[i].setText(NAMES[i] + "  0 / " + cars);
            done[i] = false;
            finishMs[i] = 0;
            graphs[i] = MapLoader.loadMap("complex_grid_map.txt");
            setGraph(panels[i], graphs[i]);
        }
        Random rand = new Random(42);
        List<Intersection> n0 = new ArrayList<>(graphs[0].values());
        List<Intersection> n1 = new ArrayList<>(graphs[1].values());
        List<Intersection> n2 = new ArrayList<>(graphs[2].values());
        for (int k = 0; k < 3; k++)
            spawns[k].clear();
        for (int i = 0; i < cars; i++) {
            int si = rand.nextInt(n0.size()), ei = rand.nextInt(n0.size());
            while (si == ei)
                ei = rand.nextInt(n0.size());
            int delay = rand.nextInt(1500);
            boolean amb = rand.nextInt(100) < 5;
            spawns[0].add(new SpawnRecord(n0.get(si), n0.get(ei), delay, amb));
            spawns[1].add(new SpawnRecord(n1.get(si), n1.get(ei), delay, amb));
            spawns[2].add(new SpawnRecord(n2.get(si), n2.get(ei), delay, amb));
        }
        for (int k = 0; k < 3; k++)
            vehicles[k].clear();
        globalTick = 0;
        startTimeMs = System.currentTimeMillis();
        wallTimer = new javax.swing.Timer(500, e -> {
            long el = System.currentTimeMillis() - startTimeMs;
            timerLabel.setText(String.format("⏱ %02d:%02d", el / 60000, (el / 1000) % 60));
        });
        wallTimer.start();
        simTimer = new javax.swing.Timer(33, e -> tick());
        simTimer.start();
    }

    private final RoutingStrategy[] STRATEGIES = { new AStarRouting(), new DijkstraRouting(), new BellmanFordRouting() };

    private void tick() {
        for (int s = 0; s < stepsPerTick; s++) {
            globalTick++;
            if (globalTick % 60 == 0)
                for (int k = 0; k < 3; k++)
                    for (Intersection n : graphs[k].values())
                        n.isHorizontalGreen = !n.isHorizontalGreen;
            for (int k = 0; k < 3; k++)
                for (int i = 0; i < totalCars; i++) {
                    if (spawns[k].get(i).spawnDelayTicks == globalTick)
                        vehicles[k].add(new Vehicle(spawns[k].get(i).start, spawns[k].get(i).target, STRATEGIES[k],
                                spawns[k].get(i).isAmbulance));
                }
            for (int k = 0; k < 3; k++)
                for (Vehicle v : vehicles[k])
                    if (!v.isFinished())
                        v.moveNextTick();
        }
        long now = System.currentTimeMillis();
        for (int k = 0; k < 3; k++) {
            int fin = countFinished(vehicles[k]);
            bars[k].setValue(fin);
            barLabels[k].setText(NAMES[k] + "  " + fin + " / " + totalCars);
            if (fin == totalCars && !done[k]) {
                done[k] = true;
                finishMs[k] = now - startTimeMs;
            }
        }
        for (SimulationPanel p : panels)
            p.repaint();
        if (done[0] && done[1] && done[2]) {
            stopAll();
            SwingUtilities.invokeLater(this::showAnalysis);
        }
    }

    private void stopAll() {
        if (simTimer != null)
            simTimer.stop();
        if (wallTimer != null)
            wallTimer.stop();
    }

    private int countFinished(List<Vehicle> list) {
        int c = 0;
        for (Vehicle v : list)
            if (v.isFinished())
                c++;
        return c;
    }

    @SuppressWarnings("unchecked")
    private void setGraph(SimulationPanel p, Map<Integer, Intersection> g) {
        try {
            var f = SimulationPanel.class.getDeclaredField("cityGraph");
            f.setAccessible(true);
            f.set(p, g);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── POST-RACE ANALYSIS ────────────────────────────────────────────
    private void showAnalysis() {
        String[] labels = { "Finish Time", "Avg Ticks/Car", "Avg Distance/Car" };
        Object[][] rows = new Object[3][4];
        for (int k = 0; k < 3; k++)
            rows[k] = new Object[] { NAMES[k], fmt(finishMs[k]),
                    String.format("%.1f", vehicles[k].stream().mapToLong(v -> v.ticksAlive).average().orElse(0)),
                    String.format("%.0f",
                            vehicles[k].stream().mapToDouble(v -> v.totalDistanceTraveled).average().orElse(0)) };

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(22, 22, 30));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));
        panel.add(hdr("🏆 Race Analysis Report", new Color(241, 196, 15)));
        panel.add(Box.createVerticalStrut(10));

        JPanel tbl = new JPanel(new GridLayout(4, 4, 2, 2));
        tbl.setBackground(new Color(22, 22, 30));
        String[] headers = { "Algorithm", "Finish Time", "Avg Ticks/Car", "Avg Dist/Car" };
        Color[] rowBg = { new Color(50, 50, 60), new Color(46, 204, 113, 80), new Color(52, 152, 219, 80),
                new Color(231, 76, 60, 80) };
        for (int c = 0; c < 4; c++)
            tbl.add(cell(headers[c], true, rowBg[0]));
        for (int k = 0; k < 3; k++)
            for (int c = 0; c < 4; c++)
                tbl.add(cell(rows[k][c].toString(), false, rowBg[k + 1]));
        panel.add(tbl);
        panel.add(Box.createVerticalStrut(14));

        int winner = finishMs[0] <= finishMs[1] && finishMs[0] <= finishMs[2] ? 0 : finishMs[1] <= finishMs[2] ? 1 : 2;
        panel.add(hdr("📋 Conclusion", Color.WHITE));
        panel.add(cline("🥇 Fastest: " + NAMES[winner] + " (" + fmt(finishMs[winner]) + ")", new Color(241, 196, 15)));
        panel.add(cline("A*: Best balance — heuristic guides search, avoids unnecessary exploration.",
                new Color(150, 255, 150)));
        panel.add(cline("Dijkstra: Optimal paths but explores more nodes — slower on large maps.",
                new Color(100, 180, 255)));
        panel.add(cline("Bellman-Ford: Guarantees shortest path via edge relaxation, handles complex weights.",
                new Color(255, 130, 130)));
        panel.add(cline("📊 Simulation used " + totalCars + " cars with identical routes across all algorithms.",
                Color.LIGHT_GRAY));

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(740, 420));
        scroll.getViewport().setBackground(new Color(22, 22, 30));
        scroll.setBorder(null);
        JOptionPane.showMessageDialog(this, scroll, "Race Complete – Analysis", JOptionPane.PLAIN_MESSAGE);
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private JLabel hdr(String t, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 17));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel cline(String t, Color c) {
        JLabel l = new JLabel("<html>" + t + "</html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    private JLabel styledLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return l;
    }

    private JLabel cell(String t, boolean bold, Color bg) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 14));
        l.setForeground(Color.WHITE);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 80), 1));
        l.setPreferredSize(new Dimension(170, 36));
        return l;
    }

    private JButton fBtn(String t, Color bg) {
        JButton b = new JButton(t);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bg.darker(), 2),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(bg.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(bg);
            }
        });
        return b;
    }

    private String fmt(long ms) {
        return String.format("%d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms / 100) % 10);
    }
}
