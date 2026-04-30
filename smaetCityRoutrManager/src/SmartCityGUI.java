import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SmartCityGUI extends JFrame {

    private Map<Integer, Intersection> cityGraph;
    private List<Vehicle> activeVehicles;
    private SimulationPanel simulationPanel;
    private Timer timer;

    // Benchmark UI Components
    private JComboBox<String> algoSelector;
    private JComboBox<String> weatherSelector;
    private JComboBox<String> simSpeedCombo;  // 1x / 2x / 5x / 10x / 20x
    private JComboBox<String> carCountCombo; // 100 / 200 / 400 cars
    private JButton startVisualBtn;
    private JButton pauseBtn;
    private JButton runAllBenchmarksBtn;
    private JButton resetMapBtn;
    private JButton raceModeBtn;
    private JButton bottleneckBtn;
    private JSlider speedSlider;
    
    // Live Stats Sidebar
    private JLabel activeLbl;
    private JLabel stoppedLbl;
    private JLabel ambulanceLbl;
    private JLabel timeLbl;
    private JProgressBar congestionBar;
    
    // Educational Console
    private JTextArea infoConsole;

    // Staggered Spawning
    private List<SpawnRecord> spawnRecords;
    private int globalTick = 0;

    // Simulation steps computed per timer tick (for 10x / 20x speed)
    private int stepsPerTick = 1;

    // Track if a visual simulation is currently running
    private boolean isVisualSimRunning = false;

    // Weather & Speed
    private static double globalSpeedMultiplier = 1.0;
    public static double getGlobalSpeedMultiplier() { return globalSpeedMultiplier; }

    // Classic Modern Styling Colors (Student Friendly)
    private final Color BG_DARK = new Color(245, 245, 250); // Light classic bg
    private final Color PANEL_DARK = new Color(255, 255, 255); // White panel
    private final Color TEXT_LIGHT = new Color(30, 30, 30); // Dark text
    private final Color ACCENT_GREEN = new Color(46, 204, 113);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
    private final Color ACCENT_RED = new Color(231, 76, 60);
    private final Color ACCENT_PURPLE = new Color(155, 89, 182);
    private final Color ACCENT_ORANGE = new Color(230, 126, 34);
    private final Font MODERN_FONT = new Font("Segoe UI", Font.BOLD, 14);

    public SmartCityGUI() {
        super("Smart City Route Manager - Educational GPS Dashboard");

        System.out.println("Loading complex map...");
        cityGraph = MapLoader.loadMap("complex_grid_map.txt");
        System.out.println("Map Loaded Successfully. Total Nodes: " + cityGraph.size());

        activeVehicles = new ArrayList<>();
        spawnRecords = new ArrayList<>();
        generateSpawnRecords(400);  // default; regenerated when car count changes

        setLayout(new BorderLayout());
        
        // --- Top Control Panel: 2 clean rows ---
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(BG_DARK);
        controlPanel.setBorder(new EmptyBorder(6, 10, 6, 10));

        // ROW 1 — Selectors
        JPanel selectorRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        selectorRow.setBackground(BG_DARK);

        JLabel algoLabel = new JLabel("Routing:");
        algoLabel.setForeground(TEXT_LIGHT);
        algoLabel.setFont(MODERN_FONT);
        algoSelector = new JComboBox<>(new String[]{"A* (A-Star)", "Dijkstra", "Bellman-Ford"});
        algoSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel weatherLabel = new JLabel("Weather:");
        weatherLabel.setForeground(TEXT_LIGHT);
        weatherLabel.setFont(MODERN_FONT);
        weatherSelector = new JComboBox<>(new String[]{"Sunny", "Heavy Rain", "Snow Storm"});
        weatherSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        weatherSelector.addActionListener(e -> updateWeather());

        JLabel animSpeedLabel = new JLabel("Anim Speed:");
        animSpeedLabel.setForeground(TEXT_LIGHT);
        animSpeedLabel.setFont(MODERN_FONT);
        speedSlider = new JSlider(5, 100, 33);
        speedSlider.setBackground(BG_DARK);
        speedSlider.setPreferredSize(new Dimension(100, 28));
        speedSlider.setInverted(true);
        speedSlider.addChangeListener(e -> {
            if (timer != null) timer.setDelay(speedSlider.getValue());
        });

        JLabel simSpeedLabel = new JLabel("Sim Speed:");
        simSpeedLabel.setForeground(TEXT_LIGHT);
        simSpeedLabel.setFont(MODERN_FONT);
        simSpeedCombo = new JComboBox<>(new String[]{"1x Normal", "2x Fast", "5x Turbo", "10x Ultra", "20x Max"});
        simSpeedCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        simSpeedCombo.addActionListener(e -> {
            String sel = (String) simSpeedCombo.getSelectedItem();
            if (sel == null) return;
            switch (sel) {
                case "2x Fast":   stepsPerTick = 2;  break;
                case "5x Turbo":  stepsPerTick = 5;  break;
                case "10x Ultra": stepsPerTick = 10; break;
                case "20x Max":   stepsPerTick = 20; break;
                default:          stepsPerTick = 1;  break;
            }
            logMessage("SIM SPEED: Changed to " + sel + " (" + stepsPerTick + " steps/tick)");
        });

        // Car count selector
        JLabel carLbl = new JLabel("Cars:");
        carLbl.setForeground(TEXT_LIGHT);
        carLbl.setFont(MODERN_FONT);
        carCountCombo = new JComboBox<>(new String[]{"100 Cars", "200 Cars", "400 Cars"});
        carCountCombo.setSelectedIndex(2); // default 400
        carCountCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        carCountCombo.addActionListener(e -> {
            int cnt = getSelectedCarCount();
            spawnRecords.clear();
            generateSpawnRecords(cnt);
            logMessage("CAR COUNT: Spawn list updated to " + cnt + " cars.");
        });

        selectorRow.add(algoLabel);      selectorRow.add(algoSelector);
        selectorRow.add(weatherLabel);   selectorRow.add(weatherSelector);
        selectorRow.add(animSpeedLabel); selectorRow.add(speedSlider);
        selectorRow.add(simSpeedLabel);  selectorRow.add(simSpeedCombo);
        selectorRow.add(carLbl);         selectorRow.add(carCountCombo);

        // ROW 2 — Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        btnRow.setBackground(BG_DARK);

        startVisualBtn    = createStyledButton("▶ Start Simulation",  ACCENT_BLUE);
        pauseBtn          = createStyledButton("⏸ Pause",             new Color(200, 160, 0));
        pauseBtn.setEnabled(false);
        resetMapBtn       = createStyledButton("↺ Reset Roads",       ACCENT_RED);
        runAllBenchmarksBtn = createStyledButton("📊 Run Benchmark",  ACCENT_GREEN);
        raceModeBtn       = createStyledButton("🏆 Race Mode",        ACCENT_PURPLE);
        JButton spawnDeliveryBtn  = createStyledButton("🚚 Delivery Truck",  ACCENT_ORANGE);
        JButton stepVisualizerBtn = createStyledButton("🔍 Step Visualizer", ACCENT_BLUE.darker());
        bottleneckBtn             = createStyledButton("🚦 Force Bottleneck", new Color(150, 0, 120));

        btnRow.add(startVisualBtn);  btnRow.add(pauseBtn); btnRow.add(resetMapBtn);
        btnRow.add(runAllBenchmarksBtn); btnRow.add(raceModeBtn);
        btnRow.add(spawnDeliveryBtn); btnRow.add(stepVisualizerBtn);
        btnRow.add(bottleneckBtn);

        controlPanel.add(selectorRow);
        controlPanel.add(btnRow);
        add(controlPanel, BorderLayout.NORTH);

        // --- Live Stats Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL_DARK);
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel statsTitle = new JLabel("Live Telemetry");
        statsTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statsTitle.setForeground(ACCENT_BLUE);
        statsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        activeLbl = createStyledSidebarLabel("Moving Cars: 0");
        stoppedLbl = createStyledSidebarLabel("Stopped Cars: 0");
        stoppedLbl.setForeground(ACCENT_RED);
        ambulanceLbl = createStyledSidebarLabel("Ambulances: 0");
        ambulanceLbl.setForeground(ACCENT_PURPLE);
        timeLbl = createStyledSidebarLabel("Sim Time: 0s");
        
        JLabel congestionTitle = createStyledSidebarLabel("City Congestion:");
        congestionBar = new JProgressBar(0, 100);
        congestionBar.setStringPainted(true);
        congestionBar.setBackground(BG_DARK);
        congestionBar.setForeground(ACCENT_RED);
        
        sidebar.add(statsTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        sidebar.add(timeLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(activeLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(stoppedLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(ambulanceLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        sidebar.add(congestionTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        sidebar.add(congestionBar);

        // ── Vehicle colour legend ──────────────────────────────────────
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        JLabel legendTitle = new JLabel("Vehicle Legend");
        legendTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        legendTitle.setForeground(ACCENT_BLUE);
        legendTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(legendTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(legendRow("●", new Color(0, 140, 255),   "Normal car"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(240, 60, 60),   "Stuck / waiting"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(130, 70, 180),  "Already rerouted"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(0, 240, 220),   "Just rerouted (flash)"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(180, 0, 140),   "Bottleneck car"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(230, 126, 34),  "Delivery truck"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("●", new Color(255, 0, 0),     "Ambulance (flash)"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(legendRow("━", new Color(190, 195, 210), "Empty road"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("━", new Color(230, 160, 0),   "Moderate (40–70%)"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("━", new Color(220, 53, 69),   "Congested (>70%)"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("━", new Color(180, 0, 0),     "Jammed (100%+)"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(legendRow("┅", new Color(255, 140, 0),   "Blocked road"));

        add(sidebar, BorderLayout.EAST);

        // --- Main Simulation Panel ---
        simulationPanel = new SimulationPanel(cityGraph, activeVehicles, this::logMessage);
        // Register callback: when a road is blocked, log how many cars rerouted
        simulationPanel.setOnRoadBlocked(blockedRoad -> {
            // Callback already logged inside SimulationPanel; we can add extra GUI reactions here
        });
        add(simulationPanel, BorderLayout.CENTER);

        // --- Bottom Info Console ---
        infoConsole = new JTextArea(5, 50);
        infoConsole.setEditable(false);
        infoConsole.setFont(new Font("Consolas", Font.BOLD, 14));
        infoConsole.setBackground(new Color(250, 250, 250)); // Light
        infoConsole.setForeground(new Color(0, 100, 50)); // Dark green
        infoConsole.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(infoConsole);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        JPanel consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBackground(BG_DARK);
        JLabel consoleTitle = new JLabel(" Educational Console & Events");
        consoleTitle.setForeground(ACCENT_GREEN);
        consoleTitle.setFont(MODERN_FONT);
        consoleTitle.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        consoleContainer.add(consoleTitle, BorderLayout.NORTH);
        consoleContainer.add(scrollPane, BorderLayout.CENTER);
        consoleContainer.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(50, 50, 60)));
        add(consoleContainer, BorderLayout.SOUTH);

        logMessage("Welcome to the Smart City Traffic Simulator.");
        logMessage("Tip: Start the visual simulation, then click any road on the map to block it and watch the cars react!");

        // Button Actions
        startVisualBtn.addActionListener(e -> startVisualSimulation());
        runAllBenchmarksBtn.addActionListener(e -> runAllBenchmarksHeadless());
        raceModeBtn.addActionListener(e -> launchRaceMode());
        
        spawnDeliveryBtn.addActionListener(e -> spawnDeliveryTruck());
        stepVisualizerBtn.addActionListener(e -> launchAlgorithmVisualizer());
        bottleneckBtn.addActionListener(e -> forceBottleneckDemo());
        
        resetMapBtn.addActionListener(e -> {
            for (Intersection node : cityGraph.values()) {
                for (Road r : node.getConnectedRoads()) {
                    r.setUnderConstruction(false);
                }
            }
            simulationPanel.repaint();
            logMessage("USER ACTION: All roads reset. Construction cleared.");
        });

        pauseBtn.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                pauseBtn.setText("Resume");
                logMessage("USER ACTION: Simulation Paused.");
            } else {
                timer.start();
                pauseBtn.setText("Pause");
                logMessage("USER ACTION: Simulation Resumed.");
            }
        });

        // Timer Loop — runs stepsPerTick simulation steps per paint frame
        timer = new Timer(speedSlider.getValue(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RoutingStrategy currentStrategy = getSelectedStrategy();

                // Inner loop: advance simulation N steps, paint only once
                for (int step = 0; step < stepsPerTick; step++) {
                    globalTick++;
                    simulationPanel.advanceTick();  // ← advance flash animation

                    if (globalTick % 60 == 0) {
                        for (Intersection node : cityGraph.values()) {
                            node.isHorizontalGreen = !node.isHorizontalGreen;
                        }
                    }

                    for (SpawnRecord record : spawnRecords) {
                        if (record.spawnDelayTicks == globalTick) {
                            activeVehicles.add(new Vehicle(record.start, record.target, currentStrategy, record.isAmbulance));
                        }
                    }

                    for (Vehicle v : activeVehicles) {
                        if (!v.isFinished()) v.moveNextTick();
                    }
                }

                // Collect stats once after all steps
                boolean anyMoving = false;
                int movingCount = 0, stoppedCount = 0, ambulanceCount = 0;
                int totalCongestion = 0, totalRoads = 0;

                for (Vehicle v : activeVehicles) {
                    if (!v.isFinished()) {
                        anyMoving = true;
                        if (v.isAmbulance) ambulanceCount++;
                        if (v.isWaitingOrSlow) stoppedCount++;
                        else movingCount++;
                    }
                }

                for (Intersection node : cityGraph.values()) {
                    for (Road r : node.getConnectedRoads()) {
                        totalCongestion += (int)(r.getTrafficDensity() * 100);
                        totalRoads++;
                    }
                }

                // Update UI Sidebar
                timeLbl.setText(String.format("Sim Time: %.1fs", globalTick / 30.0));
                activeLbl.setText("Moving Cars: " + movingCount);
                stoppedLbl.setText("Stopped Cars: " + stoppedCount);
                ambulanceLbl.setText("Ambulances: " + ambulanceCount);

                int avgCongestion = totalRoads > 0 ? totalCongestion / totalRoads : 0;
                congestionBar.setValue(avgCongestion);
                if (avgCongestion > 50) congestionBar.setForeground(ACCENT_RED);
                else if (avgCongestion > 20) congestionBar.setForeground(new Color(241, 196, 15));
                else congestionBar.setForeground(ACCENT_GREEN);

                simulationPanel.repaint();

                if (!anyMoving && globalTick > getHighestSpawnTick()) {
                    timer.stop();
                    isVisualSimRunning = false;
                    setControlsEnabled(true);
                    pauseBtn.setEnabled(false);
                    CSVExporter.exportData("benchmark_results.csv", (String)algoSelector.getSelectedItem() + " (Visual)", activeVehicles);
                    logMessage("SIMULATION COMPLETE: All vehicles arrived. Analytics successfully exported to benchmark_results.csv!");
                }
            }
        });

        setSize(1400, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
    }

    private void updateWeather() {
        String weather = (String) weatherSelector.getSelectedItem();
        if ("Heavy Rain".equals(weather)) {
            globalSpeedMultiplier = 0.6;
            logMessage("WEATHER ALERT: Heavy Rain! Global traffic speed reduced by 40%.");
        } else if ("Snow Storm".equals(weather)) {
            globalSpeedMultiplier = 0.3;
            logMessage("WEATHER ALERT: Snow Storm! Global traffic speed reduced by 70%. Expect massive delays.");
        } else {
            globalSpeedMultiplier = 1.0;
            logMessage("WEATHER UPDATE: Sunny skies. Normal traffic speed restored.");
        }
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(MODERN_FONT);
        btn.setOpaque(true);                         // Required on Windows for custom bg
        btn.setContentAreaFilled(true);
        btn.setBackground(bgColor);
        btn.setForeground(Color.BLACK);              // Always-visible black text
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 2),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Subtle hover highlight
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        return btn;
    }

    private JLabel createStyledSidebarLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(TEXT_LIGHT);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JPanel legendRow(String symbol, Color c, String label) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setBackground(PANEL_DARK);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel sym = new JLabel(symbol);
        sym.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sym.setForeground(c);
        JLabel txt = new JLabel(label);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txt.setForeground(TEXT_LIGHT);
        row.add(sym); row.add(txt);
        return row;
    }

    public void logMessage(String msg) {
        infoConsole.append("> " + msg + "\n");
        infoConsole.setCaretPosition(infoConsole.getDocument().getLength());
    }

    private void generateSpawnRecords(int count) {
        List<Intersection> nodes = new ArrayList<>(cityGraph.values());
        Random rand = new Random(123); 
        for (int i = 0; i < count; i++) {
            Intersection start = nodes.get(rand.nextInt(nodes.size()));
            Intersection end = nodes.get(rand.nextInt(nodes.size()));
            while (start == end) {
                end = nodes.get(rand.nextInt(nodes.size()));
            }
            int delay = rand.nextInt(2000);
            boolean isAmbulance = rand.nextInt(100) < 5; // 5% chance of ambulance
            spawnRecords.add(new SpawnRecord(start, end, delay, isAmbulance));
        }
    }

    private int getHighestSpawnTick() {
        int max = 0;
        for (SpawnRecord r : spawnRecords) {
            if (r.spawnDelayTicks > max) max = r.spawnDelayTicks;
        }
        return max;
    }

    private void setControlsEnabled(boolean enabled) {
        startVisualBtn.setEnabled(enabled);
        algoSelector.setEnabled(enabled);
        weatherSelector.setEnabled(enabled);
        runAllBenchmarksBtn.setEnabled(enabled);
        raceModeBtn.setEnabled(enabled);
        carCountCombo.setEnabled(enabled);
        bottleneckBtn.setEnabled(enabled);
    }

    /** Stop any running operation so a new one can start cleanly */
    private void stopCurrentOperation() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
            logMessage("INFO: Previous simulation stopped — starting new operation.");
        }
        isVisualSimRunning = false;
        pauseBtn.setEnabled(false);
        pauseBtn.setText("⏸ Pause");
        setControlsEnabled(true);
    }

    private int getSelectedCarCount() {
        String sel = (String) carCountCombo.getSelectedItem();
        if (sel == null) return 400;
        if (sel.startsWith("100")) return 100;
        if (sel.startsWith("200")) return 200;
        return 400;
    }

    private RoutingStrategy getSelectedStrategy() {
        String selectedAlgo = (String) algoSelector.getSelectedItem();
        if (selectedAlgo.equals("Dijkstra")) return new DijkstraRouting();
        if (selectedAlgo.equals("Bellman-Ford")) return new BellmanFordRouting();
        return new AStarRouting();
    }

    private void startVisualSimulation() {
        stopCurrentOperation();        // ← cancel any running sim first
        setControlsEnabled(false);
        pauseBtn.setEnabled(true);
        pauseBtn.setText("⏸ Pause");
        activeVehicles.clear();
        globalTick = 0;
        isVisualSimRunning = true;
        int cnt = getSelectedCarCount();
        spawnRecords.clear();
        generateSpawnRecords(cnt);

        logMessage("STARTING SIMULATION: " + cnt + " cars will stagger their spawns.");

        for (Intersection node : cityGraph.values()) {
            for (Road r : node.getConnectedRoads()) {
                while (r.getTrafficDensity() > 0) r.exitRoad();
            }
        }

        timer.start();
    }

    private void runAllBenchmarksHeadless() {
        stopCurrentOperation();        // ← cancel any running sim first
        setControlsEnabled(false);
        logMessage("BENCHMARK STARTED: Running headless simulations for all 3 algorithms. Please wait...");

        SwingWorker<List<BenchmarkResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BenchmarkResult> doInBackground() throws Exception {
                List<BenchmarkResult> results = new ArrayList<>();
                results.add(BenchmarkEngine.runBenchmark("A*", new AStarRouting(), cityGraph, spawnRecords));
                results.add(BenchmarkEngine.runBenchmark("Dijkstra", new DijkstraRouting(), cityGraph, spawnRecords));
                results.add(BenchmarkEngine.runBenchmark("Bellman-Ford", new BellmanFordRouting(), cityGraph, spawnRecords));
                return results;
            }

            @Override
            protected void done() {
                try {
                    List<BenchmarkResult> results = get();
                    ResultsChartPanel.showResultsDialog(results);
                    logMessage("BENCHMARK COMPLETE: Graphical charts generated. Analytics automatically saved to benchmark_results.csv!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SmartCityGUI.this, "Error running benchmarks.", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setControlsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void launchRaceMode() {
        logMessage("LAUNCHING RACE MODE: Opening split-screen algorithm showdown...");
        SwingUtilities.invokeLater(() -> {
            RaceModeGUI raceGui = new RaceModeGUI();
            raceGui.setVisible(true);
        });
    }

    private void spawnDeliveryTruck() {
        List<Intersection> nodes = new ArrayList<>(cityGraph.values());
        Random rand = new Random();
        Intersection start = nodes.get(rand.nextInt(nodes.size()));
        List<Intersection> deliveries = new ArrayList<>();
        for (int i=0; i<5; i++) {
            deliveries.add(nodes.get(rand.nextInt(nodes.size())));
        }
        activeVehicles.add(new DeliveryTruck(start, deliveries, getSelectedStrategy()));
        logMessage("TSP EVENT: Spawned a Delivery Truck (Orange) at " + start.getName() + " with 5 stops. Watch it use Nearest Neighbor approximation!");
        if (!timer.isRunning() && isVisualSimRunning) {
            timer.start();
            pauseBtn.setText("Pause");
        }
    }

    private void launchAlgorithmVisualizer() {
        logMessage("EDUCATION MODE: Launching Step-by-Step Algorithm Visualizer...");
        SwingUtilities.invokeLater(() -> {
            AlgorithmVisualizerGUI visualizer = new AlgorithmVisualizerGUI(cityGraph);
            visualizer.setVisible(true);
        });
    }

    /**
     * Force Bottleneck Demo — guaranteed congestion strategy:
     *  1. Find the node nearest to the grid centre (a natural chokepoint).
     *  2. Set capacity of ALL roads touching that node to 1.
     *  3. Spawn 150 cars in a single burst, all routed left-half → right-half
     *     so every path converges on the choke node.
     *  4. Density = cars_on_road / 1 → hits 100 % immediately.
     */
    private void forceBottleneckDemo() {
        // ── Step 1: find centre node ──────────────────────────────────
        double sumX = 0, sumY = 0;
        for (Intersection n : cityGraph.values()) { sumX += n.getX(); sumY += n.getY(); }
        double cx = sumX / cityGraph.size();
        double cy = sumY / cityGraph.size();

        Intersection centreNode = null;
        double minD = Double.MAX_VALUE;
        for (Intersection n : cityGraph.values()) {
            double d = Math.hypot(n.getX()-cx, n.getY()-cy);
            if (d < minD) { minD = d; centreNode = n; }
        }
        if (centreNode == null) return;

        // ── Step 2: reduce capacity of centre roads to 1 ─────────────
        List<Road> chokeRoads = new ArrayList<>();
        for (Road r : centreNode.getConnectedRoads()) {
            r.setCapacity(1);
            chokeRoads.add(r);
        }
        // Also reduce capacity on roads LEADING TO the centre node
        for (Intersection n : cityGraph.values()) {
            for (Road r : n.getConnectedRoads()) {
                if (r.getEnd() == centreNode) {
                    r.setCapacity(1);
                    chokeRoads.add(r);
                }
            }
        }

        // ── Step 3: pick src (left half) and dst (right half) ─────────
        List<Intersection> leftNodes  = new ArrayList<>();
        List<Intersection> rightNodes = new ArrayList<>();
        for (Intersection n : cityGraph.values()) {
            if (n.getX() < cx - 1) leftNodes.add(n);
            if (n.getX() > cx + 1) rightNodes.add(n);
        }
        if (leftNodes.isEmpty() || rightNodes.isEmpty()) return;

        // Use the two corners for maximum path overlap
        Intersection src = leftNodes.get(0), dst = rightNodes.get(0);
        double maxSpan = 0;
        for (Intersection l : leftNodes) for (Intersection r2 : rightNodes) {
            double span = Math.hypot(l.getX()-r2.getX(), l.getY()-r2.getY());
            if (span > maxSpan) { maxSpan = span; src = l; dst = r2; }
        }

        // ── Step 4: burst spawn 150 cars ─────────────────────────────
        RoutingStrategy strategy = getSelectedStrategy();
        final Intersection fSrc = src, fDst = dst;
        int spawned = 0;
        for (int i = 0; i < 150; i++) {
            Vehicle v = new Vehicle(fSrc, fDst, strategy, false);
            v.isBottleneckCar = true;
            activeVehicles.add(v);
            spawned++;
        }
        // Reverse wave too
        for (int i = 0; i < 50; i++) {
            Vehicle v = new Vehicle(fDst, fSrc, strategy, false);
            v.isBottleneckCar = true;
            activeVehicles.add(v);
        }

        logMessage("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logMessage("🚦 BOTTLENECK DEMO ACTIVATED");
        logMessage("   Choke node: " + centreNode.getName() +
            " (" + chokeRoads.size() + " roads reduced to capacity=1)");
        logMessage("   " + spawned + " magenta cars: " + fSrc.getName() + " → " + fDst.getName());
        logMessage("   + 50 cars in reverse direction.");
        logMessage("   All paths converge on the centre node → INSTANT JAM.");
        logMessage("   Watch the CONGESTION ALERT banner appear on the map!");
        logMessage("   Cars will try to reroute — click roads to block escape routes.");
        logMessage("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Ensure timer is running
        if (!timer.isRunning()) {
            isVisualSimRunning = true;
            pauseBtn.setEnabled(true);
            pauseBtn.setText("⏸ Pause");
            timer.start();
        }
    }

    public static void main(String[] args) {
        try {
            // Cross-platform L&F ensures custom button colours always render correctly
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            SmartCityGUI gui = new SmartCityGUI();
            gui.setVisible(true);
        });
    }
}


