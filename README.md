# T062-PS-045 – Bus Reservation System

## Project Overview

A Java-based command-driven bus reservation engine supporting multiple boarding and dropping points, seat types, dynamic pricing, passenger restrictions, partial cancellations, rescheduling and partner settlement.

## Team

- Team ID: T062
- Problem Statement: PS-045
- Chitrashree G – 1AT24CG020
- David Jeremiah J – 1AT24CG021

## Technologies

- Java 17 or later
- CSV input and output files
- Command-file-based execution

## Main Features

- Route and stop management
- Stage-wise seat inventory
- Multiple passenger bookings
- Seater and sleeper seat pricing
- Dynamic occupancy pricing
- Ladies-seat restrictions
- Child fare rules
- GST calculation
- Partner commission
- Partial and full cancellation
- Ticket rescheduling
- Boarding-point changes
- Operator cancellation
- Trip charts and partner settlements

## Project Structure

```text
T062-PS-045/
├── input/
│   └── T062-PS-045-sample-data/
├── src/
│   ├── model/
│   ├── util/
│   ├── service/
│   ├── Main.java
│   └── DataLoadTest.java
├── output/
├── out/
└── README.md
```

## Compilation

Open Command Prompt in the project directory and execute:

```cmd
javac -d out src/model/*.java src/util/*.java src/service/*.java src/Main.java src/DataLoadTest.java
```

## Run the Data Loading Test

```cmd
java -cp out DataLoadTest
```

## Run the Reservation Engine

```cmd
java -cp out Main input/T062-PS-045-sample-data/commands.txt
```

## Generated Output

The program generates:

- tickets.csv
- command_results.csv
- trip_chart.csv
- partner_settlement.csv

These files are generated inside the `output` directory.

## Testing

The project should be tested against all 12 registered business rules and all eight specified edge cases. See the project's test documentation for the individual test scenarios.

## Project Domain

Travel – Inter-city Bus Reservation

## Problem Statement

PS-045 – Bus Reservation with Boarding Points, Seat Types and Partial Cancellation