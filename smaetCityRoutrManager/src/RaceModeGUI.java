import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RaceModeGUI extends JFrame {

    // 3 Independent Graphs and Vehicle Lists
    private Map<Integer, Intersection> graphAStar;
    private Map<Integer, Intersection> graphDijkstra;
    private Map<Integer, Intersection> graphGreedy;

    private List<Vehicle> vehiclesAStar = new ArrayList<>();
    private List<Vehicle> vehiclesDijkstra = new ArrayList<>();
    private List<Vehicle> vehiclesGreedy = new ArrayList<>();

    // Identical Spawns
    private List<SpawnRecord> spawnsAStar = new ArrayList<>();
    private List<SpawnRecord> spawnsDijkstra = new ArrayList<>();
    private List<SpawnRecord> spawnsGreedy = new ArrayList<>();

    private SimulationPanel panelAStar;
    private SimulationPanel panelDijkstra;
    private SimulationPanel panelGreedy;

    private Timer timer;
    private int globalTick = 0;

    // Progress UI
    private JProgressBar progAStar;
    private JProgressBar progDijkstra;
    private JProgressBar progGreedy;
    
    private final int TOTAL_CARS = 200;

    public RaceModeGUI() {
        super("Algorithm Race Mode - Live Showdown");
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Don't kill main app

        // Load 3 independent map copies
        graphAStar = MapLoader.loadMap("complex_grid_map.txt");
        graphDijkstra = MapLoader.loadMap("complex_grid_map.txt");
        graphGreedy = MapLoader.loadMap("complex_grid_map.txt");

        // Generate Identical Spawns (seeded random)
        Random rand = new Random(42); 
        List<Intersection> nodesAStar = new ArrayList<>(graphAStar.values());
        List<Intersection> nodesDijkstra = new ArrayList<>(graphDijkstra.values());
        List<Intersection> nodesGreedy = new ArrayList<>(graphGreedy.values());

        for (int i = 0; i < TOTAL_CARS; i++) {
            int startIdx = rand.nextInt(nodesAStar.size());
            int endIdx = rand.nextInt(nodesAStar.size());
            while (startIdx == endIdx) endIdx = rand.nextInt(nodesAStar.size());
            
            int delay = rand.nextInt(1500);
            boolean isAmbulance = rand.nextInt(100) < 5;

            spawnsAStar.add(new SpawnRecord(nodesAStar.get(startIdx), nodesAStar.get(endIdx), delay, isAmbulance));
            spawnsDijkstra.add(new SpawnRecord(nodesDijkstra.get(startIdx), nodesDijkstra.get(endIdx), delay, isAmbulance));
            spawnsGreedy.add(new SpawnRecord(nodesGreedy.get(startIdx), nodesGreedy.get(endIdx), delay, isAmbulance));
        }

        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        headerPanel.setBackground(new Color(30, 30, 36));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        progAStar = createProgressBar("A* Algorithm Progress");
        progDijkstra = createProgressBar("Dijkstra Algorithm Progress");
        progGreedy = createProgressBar("Greedy Algorithm Progress");

        headerPanel.add(progAStar);
        headerPanel.add(progDijkstra);
        headerPanel.add(progGreedy);
        add(headerPanel, BorderLayout.NORTH);

        // Simulation Panels
        JPanel simContainer = new JPanel(new GridLayout(1, 3, 5, 5));
        simContainer.setBackground(Color.BLACK);

        panelAStar = new SimulationPanel(graphAStar, vehiclesAStar, null);
        panelAStar.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(46, 204, 113), 3), "A* Routing", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 16), Color.WHITE));

        panelDijkstra = new SimulationPanel(graphDijkstra, vehiclesDijkstra, null);
        panelDijkstra.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 3), "Dijkstra Routing", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 16), Color.WHITE));

        panelGreedy = new SimulationPanel(graphGreedy, vehiclesGreedy, null);
        panelGreedy.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(231, 76, 60), 3), "Greedy Routing", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 16), Color.WHITE));

        simContainer.add(panelAStar);
        simContainer.add(panelDijkstra);
        simContainer.add(panelGreedy);
        add(simContainer, BorderLayout.CENTER);

        // Control Footer
        JPanel footer = new JPanel(new FlowLayout());
        footer.setBackground(new Color(30, 30, 36));
        JButton startBtn = new JButton("START RACE");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startBtn.setBackground(new Color(46, 204, 113));
        startBtn.setForeground(Color.WHITE);
        startBtn.addActionListener(e -> {
            startBtn.setEnabled(false);
            timer.start();
        });
        footer.add(startBtn);
        add(footer, BorderLayout.SOUTH);

        setupTimer();
    }

    private JProgressBar createProgressBar(String title) {
        JProgressBar pb = new JProgressBar(0, TOTAL_CARS);
        pb.setStringPainted(true);
        pb.setString(title + " (0/" + TOTAL_CARS + ")");
        pb.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pb.setBackground(new Color(40, 40, 48));
        pb.setForeground(new Color(241, 196, 15));
        return pb;
    }

    private void setupTimer() {
        timer = new Timer(33, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalTick++;

                // Toggle traffic lights for all graphs
                if (globalTick % 60 == 0) {
                    toggleLights(graphAStar);
                    toggleLights(graphDijkstra);
                    toggleLights(graphGreedy);
                }

                // Spawn logic
                for (int i = 0; i < TOTAL_CARS; i++) {
                    if (spawnsAStar.get(i).spawnDelayTicks == globalTick) {
                        vehiclesAStar.add(new Vehicle(spawnsAStar.get(i).start, spawnsAStar.get(i).target, new AStarRouting(), spawnsAStar.get(i).isAmbulance));
                        vehiclesDijkstra.add(new Vehicle(spawnsDijkstra.get(i).start, spawnsDijkstra.get(i).target, new DijkstraRouting(), spawnsDijkstra.get(i).isAmbulance));
                        vehiclesGreedy.add(new Vehicle(spawnsGreedy.get(i).start, spawnsGreedy.get(i).target, new GreedyRouting(), spawnsGreedy.get(i).isAmbulance));
                    }
                }

                // Move logic & Count finished
                int aStarFinished = moveVehicles(vehiclesAStar);
                int dijkstraFinished = moveVehicles(vehiclesDijkstra);
                int greedyFinished = moveVehicles(vehiclesGreedy);

                // Update UI
                updateProgress(progAStar, aStarFinished, "A* Algorithm");
                updateProgress(progDijkstra, dijkstraFinished, "Dijkstra Algorithm");
                updateProgress(progGreedy, greedyFinished, "Greedy Algorithm");

                panelAStar.repaint();
                panelDijkstra.repaint();
                panelGreedy.repaint();

                // Check end condition
                if (aStarFinished == TOTAL_CARS && dijkstraFinished == TOTAL_CARS && greedyFinished == TOTAL_CARS) {
                    timer.stop();
                    JOptionPane.showMessageDialog(RaceModeGUI.this, "The Race is Complete!\nCheck the progress bars to see who won!");
                }
            }
        });
    }

    private void updateProgress(JProgressBar pb, int finished, String name) {
        pb.setValue(finished);
        pb.setString(name + " Finished: " + finished + "/" + TOTAL_CARS);
        if (finished == TOTAL_CARS) {
            pb.setForeground(new Color(46, 204, 113)); // Green on win
        }
    }

    private void toggleLights(Map<Integer, Intersection> graph) {
        for (Intersection node : graph.values()) {
            node.isHorizontalGreen = !node.isHorizontalGreen;
        }
    }

    private int moveVehicles(List<Vehicle> list) {
        int finishedCount = 0;
        for (Vehicle v : list) {
            if (v.isFinished()) {
                finishedCount++;
            } else {
                v.moveNextTick();
            }
        }
        return finishedCount;
    }
}
