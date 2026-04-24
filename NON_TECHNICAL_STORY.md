# How the Smart City Works: A Non-Technical Story

Imagine looking down at a bustling, miniature city from above. Cars are driving, traffic lights are changing, and a massive storm is rolling in. This document explains how our Java code brings this city to life, telling the story of how the different pieces of the code talk to each other behind the scenes.

---

## 1. Building the City (The Setup)
Before any cars can drive, the city needs to exist. 

When you first open the program, the very first thing that happens is **`SmartCityGUI`** (the main window) wakes up. But it has nothing to show yet. So, it makes a phone call to a helper named **`MapLoader`**. 

The `MapLoader` reads a text file (`complex_grid_map.txt`) and starts building the physical world. It lays down **`Intersection`** objects (the nodes, like Graphic Era University or the Clock Tower) and connects them with **`Road`** objects. Every `Road` has a length, and it keeps track of exactly how many cars are currently driving on it. 

Once `MapLoader` is done, it hands the fully built city back to the main window. The stage is set.

## 2. Hiring the Navigators (The Algorithms)
Now we have a city, but the drivers don't have maps. They need a GPS. We have three different GPS brains available:

1. **`AStarRouting` (The Smart Local):** This brain looks at the distance to the destination, but it also listens to the local radio. If a road has a lot of cars on it, A* says, *"Let's take a slight detour to save time."*
2. **`DijkstraRouting` (The Over-Planner):** This brain refuses to make a mistake. It exhaustively maps out every single possible route in the entire city before taking a turn to guarantee the mathematically perfect path. It is perfectly accurate, but takes a lot of time to think.
3. **`GreedyRouting` (The Stubborn Driver):** This brain only cares about one thing: moving physically closer to the destination. It ignores traffic jams and road closures. If the destination is North, it drives North, even if it drives straight into a traffic jam.

## 3. The Beating Heart (The Simulation Loop)
The city needs time to pass. Inside `SmartCityGUI`, there is a **`Timer`**. This is the beating heart of the city. Every 33 milliseconds, the Timer ticks. 

**When the Timer ticks, a massive chain reaction happens:**
1. **The Traffic Lights Change:** Every 60 ticks, the Timer taps the `Intersection`s on the shoulder and says, *"Switch your lights!"* The horizontal roads turn red, and the vertical roads turn green.
2. **New Cars Spawn:** The Timer checks a schedule (the `SpawnRecord`). If a car is scheduled to leave its house at this exact millisecond, the Timer creates a brand new **`Vehicle`** and drops it onto the map. (Sometimes, 5% of the time, it accidentally drops an `Ambulance` instead!).
3. **Cars Move:** The Timer yells to every car currently on the road: *"Move forward!"* This triggers the `moveNextTick()` action inside each car.

## 4. A Car's Journey (The Action)
What happens when a car is told to "Move forward"? The **`Vehicle`** has to make a series of rapid decisions:

* **Checking the Lights:** The car looks at the intersection it is standing at. Is the light red? If it is, the car turns itself **Red** and refuses to move. 
* *Wait, am I an Ambulance?* If the car is an ambulance, it ignores the red light entirely, turns on its flashing **Red and Blue** sirens, and hits the gas.
* **Checking the Road Ahead:** The car looks at the `Road` it is about to drive onto. If that road is completely jammed with cars, or if the user just clicked it to "Under Construction", the car panics!
* **The Detour:** The panicking car immediately calls its GPS Brain (`RoutingStrategy`) and says, *"I'm trapped! Calculate a new route from where I am right now!"* The GPS Brain hands back a new path. The car turns **Magenta** to let you know it just made a smart detour, and takes the new road instead.

## 5. The Weather Rolls In
While all this is happening, the user might change the dropdown to **"Snow Storm"**. 
When this happens, the main window creates a global rule: *"Everything moves slower."*

The next time the `Timer` tells a car to move, the car checks the weather. Instead of moving forward by 15 meters, the snow forces it to only move forward by 4 meters. Because every car is moving slower, cars spend more time on the roads. Because cars are stuck on the roads longer, the `Road` traffic density skyrockets! 

Because the density skyrockets, the `AStarRouting` GPS Brain starts heavily rerouting cars down back-alleys to avoid the snowy highways. The entire city ecosystem adapts dynamically.

## 6. The Finish Line
Eventually, a car reaches its final `Intersection`. When it does, it deletes itself from the road.
Once the `Timer` notices that every single car has reached its destination and the city is empty, the Timer stops beating. 

The simulation is over. The main window then gathers all the data from the trip, calls the **`CSVExporter`**, and hands it the paperwork to save to your computer so you can review how well the GPS Brains handled the chaos!
