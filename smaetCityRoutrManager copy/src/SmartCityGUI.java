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
    private JButton startVisualBtn;
    private JButton pauseBtn;
    private JButton runAllBenchmarksBtn;
    private JLabel activeLbl;
    private JLabel distLbl;
    private JLabel ticksLbl;
    
    // Educational Console
    private JTextArea infoConsole;

    // Staggered Spawning
    private List<SpawnRecord> spawnRecords;
    private int globalTick = 0;
    
    // Track if a visual simulation is currently running
    private boolean isVisualSimRunning = false;

    // Modern Styling Colors
    private final Color BG_DARK = new Color(30, 30, 36);
    private final Color TEXT_LIGHT = new Color(230, 230, 230);
    private final Color ACCENT_GREEN = new Color(46, 204, 113);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
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
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        controlPanel.setBackground(BG_DARK);
        controlPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel algoLabel = new JLabel("Routing Algorithm:");
        algoLabel.setForeground(TEXT_LIGHT);
        algoLabel.setFont(MODERN_FONT);

        algoSelector = new JComboBox<>(new String[]{"A* (A-Star)", "Dijkstra", "Greedy"});
        algoSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        startVisualBtn = createStyledButton("Start Visual Sim", ACCENT_BLUE);
        pauseBtn = createStyledButton("Pause", new Color(241, 196, 15));
        pauseBtn.setEnabled(false);
        runAllBenchmarksBtn = createStyledButton("Run All Benchmarks & Compare", ACCENT_GREEN);
        
        activeLbl = createStyledLabel("Active Cars: 0");
        distLbl = createStyledLabel("Avg Dist: 0.0");
        ticksLbl = createStyledLabel("Avg Ticks: 0");

        controlPanel.add(algoLabel);
        controlPanel.add(algoSelector);
        controlPanel.add(startVisualBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(runAllBenchmarksBtn);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(activeLbl);
        controlPanel.add(distLbl);
        controlPanel.add(ticksLbl);
        add(controlPanel, BorderLayout.NORTH);

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
        JLabel consoleTitle = new JLabel(" Live Telemetry & Events");
        consoleTitle.setForeground(ACCENT_GREEN);
        consoleTitle.setFont(MODERN_FONT);
        consoleTitle.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        consoleContainer.add(consoleTitle, BorderLayout.NORTH);
        consoleContainer.add(scrollPane, BorderLayout.CENTER);
        consoleContainer.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(50, 50, 60)));
        add(consoleContainer, BorderLayout.SOUTH);

        // Initial welcome message
        logMessage("Welcome to the Smart City Traffic Simulator.");
        logMessage("Concept: This sandbox demonstrates graph theory, pathfinding algorithms, and dynamic traffic management.");
        logMessage("Tip: Start the visual simulation, then click any road on the map to block it and watch the cars react!");

        // Button Actions
        startVisualBtn.addActionListener(e -> startVisualSimulation());
        runAllBenchmarksBtn.addActionListener(e -> runAllBenchmarksHeadless());
        
        pauseBtn.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                pauseBtn.setText("Resume");
                logMessage("USER ACTION: Simulation Paused. Take your time to analyze the traffic congestion.");
            } else {
                timer.start();
                pauseBtn.setText("Pause");
                logMessage("USER ACTION: Simulation Resumed.");
            }
        });

        // Timer Loop
        timer = new Timer(33, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalTick++;

                if (globalTick % 60 == 0) {
                    for (Intersection node : cityGraph.values()) {
                        node.isHorizontalGreen = !node.isHorizontalGreen;
                    }
                    if (globalTick == 60) {
                        logMessage("SYSTEM EVENT: Traffic lights are active! Notice vehicles pausing at red lights to avoid collisions.");
                    }
                }

                RoutingStrategy currentStrategy = getSelectedStrategy();
                for (SpawnRecord record : spawnRecords) {
                    if (record.spawnDelayTicks == globalTick) {
                        activeVehicles.add(new Vehicle(record.start, record.target, currentStrategy));
                    }
                }

                boolean anyMoving = false;
                int activeCount = 0;

                for (Vehicle v : activeVehicles) {
                    if (!v.isFinished()) {
                        v.moveNextTick();
                        anyMoving = true; 
                        activeCount++;
                    }
                }

                activeLbl.setText("Active Cars: " + activeCount);
                simulationPanel.repaint();

                if (!anyMoving && globalTick > getHighestSpawnTick()) {
                    timer.stop();
                    isVisualSimRunning = false;
                    calculateAndDisplayResults();
                    setControlsEnabled(true);
                    pauseBtn.setEnabled(false);
                    logMessage("SIMULATION COMPLETE: All vehicles have successfully reached their destinations.");
                }
            }
        });

        setSize(1200, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
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

    private JLabel createStyledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(MODERN_FONT);
        lbl.setForeground(new Color(200, 200, 200));
        lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
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
            spawnRecords.add(new SpawnRecord(start, end, delay));
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
        runAllBenchmarksBtn.setEnabled(enabled);
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
        
        String algo = (String) algoSelector.getSelectedItem();
        logMessage("STARTING SIMULATION: 400 cars will stagger their spawns over the next minute.");
        logMessage("CAR COLOR GUIDE: \n   - CYAN: Normal Traffic Flow.\n   - RED: Stopped at Red Light or Stuck in Heavy Traffic.\n   - MAGENTA: Actively rerouting/detouring to avoid traffic or blocked roads!");
        
        if (algo.contains("A-Star")) {
            logMessage("ALGORITHM CONCEPT: A* balances the physical distance (Heuristic) with dynamic traffic penalties. It is highly efficient and adaptive.");
        } else if (algo.contains("Dijkstra")) {
            logMessage("ALGORITHM CONCEPT: Dijkstra exhaustively checks all paths for the absolute lowest penalty. It guarantees the best path, but is slower.");
        } else {
            logMessage("ALGORITHM CONCEPT: Greedy ONLY cares about physical distance. It ignores traffic and potholes entirely, often driving right into jams!");
        }
        
        for (Intersection node : cityGraph.values()) {
            for (Road r : node.getConnectedRoads()) {
                while (r.getTrafficDensity() > 0) r.exitRoad(); 
            }
        }

        distLbl.setText("Avg Dist: Calculating...");
        ticksLbl.setText("Avg Ticks: Calculating...");
        
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
                    logMessage("BENCHMARK COMPLETE: Graphical charts generated. Compare the efficiency metrics!");
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

    private void calculateAndDisplayResults() {
        if (activeVehicles.isEmpty()) return;
        
        double totalDist = 0;
        int totalTicks = 0;
        for (Vehicle v : activeVehicles) {
            totalDist += v.totalDistanceTraveled;
            totalTicks += v.ticksAlive;
        }

        double avgDist = totalDist / activeVehicles.size();
        double avgTicks = (double) totalTicks / activeVehicles.size();

        distLbl.setText(String.format("Avg Dist: %.2f", avgDist));
        ticksLbl.setText(String.format("Avg Ticks: %.1f", avgTicks));
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


