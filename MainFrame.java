import javax.swing.JFrame;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Smart City Traffic Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center window

        // Add Simulation Panel
        SimulationPanel panel = new SimulationPanel();
        add(panel);

        setVisible(true);
    }

    public static void main(String[] args) {
        new MainFrame();
    }
}