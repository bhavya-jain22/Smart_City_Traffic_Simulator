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
    private JButton startVisualBtn;
    private JButton pauseBtn;
    private JButton runAllBenchmarksBtn;
    private JButton resetMapBtn;
    private JButton raceModeBtn;
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
    
    // Track if a visual simulation is currently running
    private boolean isVisualSimRunning = false;

    // Weather & Speed
    private static double globalSpeedMultiplier = 1.0;
    public static double getGlobalSpeedMultiplier() { return globalSpeedMultiplier; }

    // Modern Styling Colors
    private final Color BG_DARK = new Color(30, 30, 36);
    private final Color PANEL_DARK = new Color(40, 40, 48);
    private final Color TEXT_LIGHT = new Color(230, 230, 230);
    private final Color ACCENT_GREEN = new Color(46, 204, 113);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
    private final Color ACCENT_RED = new Color(231, 76, 60);
    private final Color ACCENT_PURPLE = new Color(155, 89, 182);
    private final Font MODERN_FONT = new Font("Segoe UI", Font.BOLD, 14);

    public SmartCityGUI() {
        super("Smart City Route Manager - Modern GPS Dashboard");

        System.out.println("Loading complex map...");
        cityGraph = MapLoader.loadMap("complex_grid_map.txt");
        System.out.println("Map Loaded Successfully. Total Nodes: " + cityGraph.size());

        activeVehicles = new ArrayList<>();
        spawnRecords = new ArrayList<>();
        generateSpawnRecords(400);

        setLayout(new BorderLayout());
        
        // --- Top Control Panel (Modern Dark Theme) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(BG_DARK);
        controlPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel algoLabel = new JLabel("Routing:");
        algoLabel.setForeground(TEXT_LIGHT);
        algoLabel.setFont(MODERN_FONT);

        algoSelector = new JComboBox<>(new String[]{"A* (A-Star)", "Dijkstra", "Greedy"});
        algoSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel weatherLabel = new JLabel("Weather:");
        weatherLabel.setForeground(TEXT_LIGHT);
        weatherLabel.setFont(MODERN_FONT);

        weatherSelector = new JComboBox<>(new String[]{"Sunny", "Heavy Rain", "Snow Storm"});
        weatherSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        weatherSelector.addActionListener(e -> updateWeather());

        startVisualBtn = createStyledButton("Start Visual Sim", ACCENT_BLUE);
        pauseBtn = createStyledButton("Pause", new Color(241, 196, 15));
        pauseBtn.setEnabled(false);
        resetMapBtn = createStyledButton("Reset Roads", ACCENT_RED);
        runAllBenchmarksBtn = createStyledButton("Run Data Benchmark", ACCENT_GREEN);
        raceModeBtn = createStyledButton("🏆 Launch Race Mode", ACCENT_PURPLE);

        JLabel speedLabel = new JLabel("Speed:");
        speedLabel.setForeground(TEXT_LIGHT);
        speedLabel.setFont(MODERN_FONT);
        
        speedSlider = new JSlider(5, 100, 33);
        speedSlider.setBackground(BG_DARK);
        speedSlider.setInverted(true); // 5ms is fast, 100ms is slow
        speedSlider.addChangeListener(e -> {
            if (timer != null) timer.setDelay(speedSlider.getValue());
        });

        controlPanel.add(algoLabel);
        controlPanel.add(algoSelector);
        controlPanel.add(weatherLabel);
        controlPanel.add(weatherSelector);
        controlPanel.add(speedLabel);
        controlPanel.add(speedSlider);
        controlPanel.add(startVisualBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(resetMapBtn);
        controlPanel.add(runAllBenchmarksBtn);
        controlPanel.add(raceModeBtn);
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

        add(sidebar, BorderLayout.EAST);

        // --- Main Simulation Panel ---
        simulationPanel = new SimulationPanel(cityGraph, activeVehicles, this::logMessage);
        add(simulationPanel, BorderLayout.CENTER);

        // --- Bottom Info Console ---
        infoConsole = new JTextArea(5, 50);
        infoConsole.setEditable(false);
        infoConsole.setFont(new Font("Consolas", Font.BOLD, 14));
        infoConsole.setBackground(new Color(20, 20, 24));
        infoConsole.setForeground(new Color(0, 255, 150));
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

        // Timer Loop
        timer = new Timer(speedSlider.getValue(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalTick++;

                if (globalTick % 60 == 0) {
                    for (Intersection node : cityGraph.values()) {
                        node.isHorizontalGreen = !node.isHorizontalGreen;
                    }
                }

                RoutingStrategy currentStrategy = getSelectedStrategy();
                for (SpawnRecord record : spawnRecords) {
                    if (record.spawnDelayTicks == globalTick) {
                        activeVehicles.add(new Vehicle(record.start, record.target, currentStrategy, record.isAmbulance));
                    }
                }

                boolean anyMoving = false;
                int movingCount = 0;
                int stoppedCount = 0;
                int ambulanceCount = 0;
                int totalCongestion = 0;
                int totalRoads = 0;

                for (Vehicle v : activeVehicles) {
                    if (!v.isFinished()) {
                        v.moveNextTick();
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
                else if (avgCongestion > 20) congestionBar.setForeground(new Color(241, 196, 15)); // Yellow
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
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createStyledSidebarLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(TEXT_LIGHT);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
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
    }

    private RoutingStrategy getSelectedStrategy() {
        String selectedAlgo = (String) algoSelector.getSelectedItem();
        if (selectedAlgo.equals("Dijkstra")) return new DijkstraRouting();
        if (selectedAlgo.equals("Greedy")) return new GreedyRouting();
        return new AStarRouting();
    }

    private void startVisualSimulation() {
        setControlsEnabled(false);
        pauseBtn.setEnabled(true);
        pauseBtn.setText("Pause");
        activeVehicles.clear();
        globalTick = 0;
        isVisualSimRunning = true;
        
        logMessage("STARTING SIMULATION: 400 cars will stagger their spawns over the next minute.");
        
        for (Intersection node : cityGraph.values()) {
            for (Road r : node.getConnectedRoads()) {
                while (r.getTrafficDensity() > 0) r.exitRoad(); 
            }
        }
        
        timer.start();
    }

    private void runAllBenchmarksHeadless() {
        setControlsEnabled(false);
        logMessage("BENCHMARK STARTED: Running headless fast-forward simulations for all 3 algorithms. Please wait...");

        SwingWorker<List<BenchmarkResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BenchmarkResult> doInBackground() throws Exception {
                List<BenchmarkResult> results = new ArrayList<>();
                results.add(BenchmarkEngine.runBenchmark("A*", new AStarRouting(), cityGraph, spawnRecords));
                results.add(BenchmarkEngine.runBenchmark("Dijkstra", new DijkstraRouting(), cityGraph, spawnRecords));
                results.add(BenchmarkEngine.runBenchmark("Greedy", new GreedyRouting(), cityGraph, spawnRecords));
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            SmartCityGUI gui = new SmartCityGUI();
            gui.setVisible(true);
        });
    }
}


