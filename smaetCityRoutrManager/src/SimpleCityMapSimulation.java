import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SimpleCityMapSimulation extends JPanel implements ActionListener {
    
    // Simple Car class representing our blue dots
    static class Car {
        int x, y;
        int targetX, targetY;
        int speed = 2; // Speed of the car
        
        public Car(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.targetX = startX;
            this.targetY = startY;
            setRandomTarget();
        }
        
        // Move the car towards its target
        public void move() {
            if (x < targetX) {
                x += Math.min(speed, targetX - x);
            } else if (x > targetX) {
                x -= Math.min(speed, x - targetX);
            } else if (y < targetY) {
                y += Math.min(speed, targetY - y);
            } else if (y > targetY) {
                y -= Math.min(speed, y - targetY);
            } else {
                // When target is reached, set a new random target
                setRandomTarget();
            }
        }
        
        // Pick a new random destination along the roads
        private void setRandomTarget() {
            // These are the coordinates of our roads in the grid
            int[] roadPositions = {100, 300, 500, 700};
            
            // 50% chance to move horizontally, 50% chance to move vertically
            if (Math.random() > 0.5) {
                // Move horizontally (change X, keep Y same)
                targetX = roadPositions[(int)(Math.random() * roadPositions.length)];
                targetY = y;
            } else {
                // Move vertically (change Y, keep X same)
                targetX = x;
                targetY = roadPositions[(int)(Math.random() * roadPositions.length)];
            }
        }
        
        // Draw the car as a blue dot
        public void draw(Graphics g) {
            g.setColor(Color.BLUE);
            // Draw an oval (circle) centered at (x, y)
            g.fillOval(x - 8, y - 8, 16, 16);
        }
    }
    
    private List<Car> cars;
    private Timer timer;

    public SimpleCityMapSimulation() {
        cars = new ArrayList<>();
        
        // Add some cars at initial intersections
        cars.add(new Car(100, 100));
        cars.add(new Car(300, 300));
        cars.add(new Car(500, 100));
        cars.add(new Car(100, 500));
        cars.add(new Car(300, 500));
        cars.add(new Car(700, 300));
        cars.add(new Car(500, 700));
        cars.add(new Car(700, 700));
        
        // Setup timer to call actionPerformed() every 30 milliseconds
        timer = new Timer(30, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 1. Draw background (Green land)
        g.setColor(new Color(34, 139, 34)); 
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // 2. Draw roads (Dark Gray)
        g.setColor(Color.DARK_GRAY);
        int[] roadPositions = {100, 300, 500, 700};
        int roadWidth = 30; // Width of the road
        
        for (int pos : roadPositions) {
            // Draw Vertical roads
            g.fillRect(pos - roadWidth / 2, 0, roadWidth, getHeight());
            // Draw Horizontal roads
            g.fillRect(0, pos - roadWidth / 2, getWidth(), roadWidth);
        }
        
        // 3. Draw dashed center lines on the roads
        g.setColor(Color.WHITE);
        Graphics2D g2d = (Graphics2D) g;
        // Create dashed stroke
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        
        for (int pos : roadPositions) {
            g2d.drawLine(pos, 0, pos, getHeight());
            g2d.drawLine(0, pos, getWidth(), pos);
        }
        
        // 4. Draw all cars
        for (Car car : cars) {
            car.draw(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Move all cars
        for (Car car : cars) {
            car.move();
        }
        // Redraw the screen to show updated positions
        repaint();
    }

    public static void main(String[] args) {
        // Create the main window (JFrame)
        JFrame frame = new JFrame("Simple City Traffic Simulation");
        SimpleCityMapSimulation simulationPanel = new SimpleCityMapSimulation();
        
        frame.add(simulationPanel);
        frame.setSize(850, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center window on screen
        frame.setVisible(true); // Show window
    }
}
