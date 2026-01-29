# Real-Time Field Service Engineer Scheduling with Emergencies and Collaborations

## Overview

This repository contains the implementation of an event-driven simulation-optimization approach for dynamic Field Service Engineer (FSE) routing and scheduling, developed in 2016 as part of an industry-sponsored research project.

The system addresses the real-time management of field service engineers who must balance planned maintenance jobs with emergency repair requests that arrive stochastically throughout the day. The approach aims to minimize operational costs and the number of required engineers while maintaining workforce schedule stability.

**Historical Context**: This code was developed in collaboration with Hanna Grzybowska at UNSW's Research Centre for Integrated Transport Innovation. The work was presented at EURO 2016 (Poznan, Poland) and the Transportation Research Board Annual Meeting 2017 (Washington, DC).

**Important**: This is research code from 2016 shared for academic and educational purposes. It is not production-ready software and is not actively maintained.

## Quick Start

### Prerequisites

- Java JDK 1.8 or higher

### Compilation

From the repository root directory:

```bash
# Create output directory
mkdir -p bin

# Compile all Java sources
javac -d bin src/problem/*.java src/solver/*.java src/simulation/*.java src/run/*.java
```

### Running

```bash
# Run with default scenario (scenario1)
java -cp bin run.RunTest
```

To run a different scenario, modify the `instanceFile` variable in `src/run/RunTest.java`:

```java
private static String instanceFile = "instances/scenario3.txt";  // Change scenario here
```

Then recompile and run.

### Expected Output

```
Solution cost: [total_cost]
Number of routes: [num_routes]
...
CPU: [seconds]
[Assignment matrix showing resource-day-route mappings]
```

## Project Structure

```
field_service_engineers/
|
+-- src/                          # Java source code
|   +-- problem/                  # Problem domain models
|   |   +-- Instance.java         # Loads problem instance from files
|   |   +-- Resource.java         # Field service engineer entity
|   |   +-- Task.java             # Job/task entity
|   |
|   +-- solver/                   # Optimization solver
|   |   +-- Insertion.java        # Main solver: propagation-enhanced insertion heuristic
|   |   +-- Route.java            # Single engineer's daily schedule
|   |   +-- Solution.java         # Complete solution (all routes)
|   |
|   +-- simulation/               # Event-driven simulation
|   |   +-- Simulation.java       # Simulation engine for dynamic events
|   |   +-- Distributions.java    # Probability distributions for stochastic sampling
|   |   +-- Statistics.java       # Statistical utility functions
|   |
|   +-- run/                      # Entry point
|       +-- RunTest.java          # Main class to run experiments
|
+-- instances/                    # Test data and scenarios
|   +-- FSE_costmat.txt           # Distance/time matrix (251 locations)
|   +-- scenario1.txt             # Maintenance only (420 jobs)
|   +-- scenario2.txt             # Maintenance only (620 jobs)
|   +-- scenario3.txt             # Maintenance + repairs (420 + 200 jobs)
|   +-- scenario5.txt             # Maintenance + collaborations (420 jobs)
|   +-- scenario6.txt             # Maintenance + collaborations (620 jobs)
|   +-- FSE_Scenario_*_*.csv      # Extended data files (not used by current solver)
|
+-- FSE_COR.pdf                   # Research paper draft
+-- README.md                     # This file
```

## Architecture

### Component Overview

The system follows a two-phase approach: initial solution construction followed by event-driven re-planning:

```
                          +------------------+
                          |    Instance      |
                          |  (Problem Data)  |
                          +--------+---------+
                                   |
                                   v
+------------------+      +--------+---------+
|   RunTest.java   |----->|    Insertion     |
|   (Entry Point)  |      |    (Solver)      |
+--------+---------+      +--------+---------+
         |                         |
         |                         | solveByDistance()
         |                         v
         |                +--------+---------+
         |                |    Solution      |
         |                | (Initial State)  |
         |                +--------+---------+
         |                         |
         v                         v
+--------+-------------------------+---------+
|              Simulation Loop               |
|  +---------------------------------------+ |
|  | For each callback (emergency/repair): | |
|  |                                       | |
|  |   Insertion.scheduleCallback()        | |
|  |          |                            | |
|  |          v                            | |
|  |   Solution (modified in place)        | |
|  |          |                            | |
|  |          v                            | |
|  |   Reschedule displaced tasks          | |
|  +---------------------------------------+ |
+--------------------+-----------------------+
                     |
                     v
          +----------+----------+
          |      Solution       |
          |   (Final State)     |
          +---------------------+
```

### Class Descriptions

| Class | Package | Purpose |
|-------|---------|---------|
| `Instance` | problem | Loads and manages problem data from files (resources, tasks, callbacks, collaborations) |
| `Resource` | problem | Represents an FSE with multi-day shift availability |
| `Task` | problem | Represents a job with location, duration, priority, and optional arrival time |
| `Insertion` | solver | Main optimization algorithm using propagation-enhanced insertion heuristic |
| `Route` | solver | Represents one engineer's schedule for a single day |
| `Solution` | solver | Aggregates all routes with cost calculations |
| `Simulation` | simulation | Event-driven simulator that triggers re-planning on emergency arrivals |
| `Distributions` | simulation | Probability distributions (uniform, normal, gamma, beta, etc.) |
| `Statistics` | simulation | Basic statistical functions (mean, variance, std dev) |
| `RunTest` | run | Main entry point for running experiments |

### Key Algorithms

**1. Propagation-Enhanced Insertion Heuristic** (`Insertion.solveByDistance()`)
- Preprocesses nearest neighbors by distance and travel time
- Ranks locations by job demand (number of tasks)
- Greedily inserts tasks using nearest-neighbor logic
- Uses constraint propagation to filter infeasible insertions
- Handles collaborative jobs by building synchronized routes

**2. Event-Driven LNS Re-planning** (`Insertion.scheduleCallback()`)

The Large Neighborhood Search mechanism is event-driven rather than using random destruction:

```
When emergency callback arrives at time τ:

DESTROY PHASE (implicit):
├── Find route of closest FSE to emergency location
├── Identify task being executed at time τ
├── Clear the affected route
└── Displaced tasks (after insertion point) → tasksToSchedule

REPAIR PHASE (insertion heuristic):
├── Re-insert tasks up to current position
├── Insert emergency callback
└── Re-insert displaced tasks using propagation-enhanced insertion
    └── Only feasible tasks re-added; overflow may shift to next day
```

Key properties:
- Only ONE route is modified per emergency (schedule stability)
- Other engineers' schedules remain unchanged
- Displaced tasks are re-inserted using the same insertion heuristic

**3. Event-Driven Simulation** (`Simulation.run()`)
- Processes emergency callbacks in chronological order
- Each callback triggers the LNS destroy-repair cycle
- Maintains solution state throughout simulation horizon

## Problem Description

### Real-Time FSEEC Problem

The Real-Time Field Service Engineer Scheduling Problem with Emergencies and Collaborations involves routing and scheduling engineers to serve geographically distributed job requests.

**Key Characteristics**:
- **Dynamic arrivals**: Repair jobs arrive stochastically and require re-planning
- **Priority levels**: Low (maintenance), medium (repair), high (emergency)
- **Collaborative jobs**: Some jobs require two engineers working together
- **Open routes**: Routes start and end at job locations (not depots)
- **Hard time windows**: Jobs must be serviced within time constraints

### Problem Constraints

- Each job must be attended exactly once
- Each engineer can attend only one job at a time
- Jobs must be serviced within their time windows
- Collaborative jobs require two engineers finishing simultaneously

### Objectives (Lexicographic Priority)

1. Minimize number of required engineers
2. Minimize operational cost (travel time + waiting time)
3. Minimize schedule disruptions when emergencies occur

## Test Scenarios

The repository includes **5 working text-format scenarios**:

| Scenario | Maintenance Jobs | Repair Callbacks | Collaborations | Description |
|----------|-----------------|------------------|----------------|-------------|
| 1 | 420 | 0 | No | Baseline maintenance only |
| 2 | 620 | 0 | No | Higher volume maintenance |
| 3 | 420 | 200 | No | Maintenance + medium-priority repairs |
| 5 | 420 | 0 | Yes (~5%) | Maintenance with collaborative jobs |
| 6 | 620 | 0 | Yes (~5%) | Higher volume with collaborations |

**Note**: Scenario 4 (with high-priority emergencies) and scenarios 7-8 exist only as CSV data files and are not integrated with the current solver. The paper describes 8 scenarios, but only 5 are implemented in the working text format.

### Scenario File Format

```
# Resources
# ResourceID  ShiftStart(h)  ShiftEnd(h)  Availability(h)
1    0    8    8
1    0    8    8
...

# Tasks
# TaskID  Location  Priority  Duration(min)
1    150    1    120
2    42     1    85
...

# Callbacks
# TaskID  Location  TimeEvent(min)  Priority  Duration(min)
421    67    1250    2    45
...

# Collaborations
# TaskID1  TaskID2
55    56
73    74
...
```

### Scenario Parameters

- **10 FSEs** working 8-hour shifts
- **20 work-days** planning horizon
- **251 locations** in the service area
- Job durations: exponentially distributed (~110 min for maintenance, ~40 min for repairs)

## Instance Data

### Cost Matrix (`instances/FSE_costmat.txt`)

Tab-separated file with 4 columns:
- Origin location (1-251)
- Destination location (1-251)
- Distance (arbitrary units)
- Travel time (minutes)

## Performance

Tested on Intel i5 1.8 GHz, 4 GB RAM, OS X 10.11.4:

| Scenario | Jobs | Callbacks | CPU Time | Routes | FSEs Used |
|----------|------|-----------|----------|--------|-----------|
| 1 | 420 | 0 | < 1 sec | ~120 | 6 |
| 2 | 620 | 0 | < 1 sec | ~135 | 7 |
| 3 | 420 | 200 | < 1 sec | ~179 | 9 |
| 5 | 420 | 0 | < 1 sec | ~121 | 7 |
| 6 | 620 | 0 | < 1 sec | ~136 | 7 |

## Known Limitations

### Implementation Gaps

1. **Incomplete scenarios**: Scenarios 4, 7, 8 (high-priority emergencies + collaborations) are not fully implemented as text-format scenarios
2. **CSV data unused**: Extended scenario data (skills, equipment, preferences) exists but is not loaded by the solver
3. **Single-objective**: Objectives are handled lexicographically, not as true multi-objective optimization

### Code Quality

1. **Minimal documentation**: Limited inline comments and no JavaDoc
2. **Basic error handling**: Try-catch blocks with TODO comments
3. **Deprecated APIs**: Uses `Hashtable` instead of `HashMap`
4. **Hard-coded paths**: Instance file path is hard-coded in `RunTest.java`
5. **No unit tests**: No automated test suite

### Algorithmic

1. **No geographic clustering**: Jobs are not pre-clustered by area
2. **Greedy construction**: May not find globally optimal solutions
3. **Single-route re-planning**: By design, only the affected engineer's route is modified (for schedule stability); displaced tasks are rescheduled to subsequent days for the same engineer or placed in extra routes

## Extending the Code

### Adding a New Scenario

1. Create a new text file in `instances/` following the format above
2. Modify `RunTest.java` to point to your new file
3. Recompile and run

### Modifying the Solver

Key extension points in `Insertion.java`:
- `solveByDistance()` / `solveByDuration()`: Initial solution construction
- `scheduleCallback()`: Emergency insertion logic
- `completeRouteForward()` / `completeRouteBackwards()`: Route completion strategies

## References

### Conference Presentations

- **EURO 2016** (Poznan, Poland): "Real-Time Field Service Engineer Scheduling Problem with Emergencies and Collaborations: A Simulation-Optimization Approach"
- **TRB Annual Meeting 2017** (Washington, DC): "Real-Time Field Service Engineer Scheduling Problem with Emergencies and Collaborations: a Simulation-Optimization Approach" (Poster presentation)

## Citation

If you use this code in your research, please cite:

```bibtex
@inproceedings{grzybowska2016realtime,
  author    = {Grzybowska, Hanna and Guimarans, Daniel},
  title     = {Real-Time Field Service Engineer Scheduling Problem with
               Emergencies and Collaborations: A Simulation-Optimization Approach},
  booktitle = {Proceedings of EURO 2016},
  year      = {2016},
  address   = {Poznan, Poland}
}
```

## License

MIT License

Copyright (c) 2016 Hanna Grzybowska and Daniel Guimarans

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Acknowledgments

This work was developed in collaboration with **Hanna Grzybowska** from the Research Centre for Integrated Transport Innovation, School of Civil and Environmental Engineering, University of New South Wales, Sydney, Australia.

The research was supported by an industry partner through a commercial engagement. The synthetic benchmarks were designed to faithfully represent real-world operational characteristics while respecting confidentiality agreements.

## Contributing

This is archival research code and is not actively maintained. However, if you find bugs or have improvements:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/improvement`)
3. Commit your changes (`git commit -am 'Add improvement'`)
4. Push to the branch (`git push origin feature/improvement`)
5. Open a Pull Request

## Contact

For questions about this code or the underlying research, please open an issue in this repository.

---

**Disclaimer**: This repository contains historical research code from 2016. It is provided "as is" without warranty of any kind. The code is not maintained and may contain bugs or incomplete implementations. Use at your own risk.
