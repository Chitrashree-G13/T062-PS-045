# Capstone Design Document – PS-045

**Project:** Bus Reservation with Boarding Points, Seat Types and Partial Cancellation  
**Team:** T062  
**Domain:** Travel – Inter-city bus reservation  
**Implementation:** Java 17+, CSV input/output, command-file execution

## 1. Introduction

The project is a command-driven bus reservation engine. It loads route, stop, seat-layout, bus, trip and partner master data from CSV files; processes reservation commands in physical file order; and creates ticket, command-result, trip-chart and partner-settlement outputs.

## 2. Objectives

1. Maintain ordered routes with multiple boarding and dropping points.
2. Allocate seats by route-stage intervals, allowing sequential use of the same seat.
3. Support seater, sleeper and single-berth seat types, ladies-only seats and adjacent-berth restrictions.
4. Calculate stage fare, seat premium, dynamic occupancy uplift, child fare and GST.
5. Process partner and counter bookings, passenger-level cancellations and refunds.
6. Support one-time rescheduling and eligible boarding-point changes.
7. Handle operator cancellation and partner settlement.
8. Generate the evaluation CSV formats specified in the supplied sample-data README.

## 3. System inputs and outputs

### Inputs

- `stops.csv`: stop identifiers, names and cities.
- `route_stops.csv`: ordered route stops, offset minutes and stage fares.
- `layout_seats.csv`: seat number, deck, seat type, pair and ladies-only flag.
- `buses.csv`: bus-to-layout mapping and AC flag.
- `trips.csv`: trip-to-route/bus mapping and origin departure.
- `partners.csv`: commission and cancellation-fee share percentages.
- `commands.txt`: ordered booking, cancellation, reschedule, boarding-change, operator-cancel and print commands.

### Outputs

- `tickets.csv`: one passenger row per ticket, fare breakup and final status.
- `command_results.csv`: one row per non-comment command, including print commands.
- `trip_chart.csv`: one passenger row for each requested chart.
- `partner_settlement.csv`: one row for each settlement request.

## 4. Architecture and modules

- **CSV data loading:** reads the six master files and constructs domain objects.
- **Route and trip model:** stores ordered stops, route stage fares, bus layout and departure time.
- **Booking and seat validation:** checks journey stops, passenger data, seat existence, stage overlap, ladies-only and adjacent-berth constraints.
- **Fare calculation:** calculates stage fare, occupancy uplift, seat premium, child factor, taxable fare and GST.
- **Cancellation:** calculates passenger-specific refund tiers from boarding-point time, marks passengers cancelled and recalculates remaining passenger fare lines.
- **Rescheduling:** validates one-time use, date range, six-hour cutoff, matching route stops and destination seat availability; preserves already-cancelled passenger rows.
- **Boarding change:** permits an earlier boarding stop only when at least two hours remain and the new stop is before the original drop.
- **Reporting:** prints ticket fare breakups at end-of-file state and writes chart and settlement rows.

`Main.java` is the active command-file execution path used by the evaluation run. Domain models and supporting service classes are maintained under `src/model` and `src/service`.

## 5. Seat inventory model

Stops are ordered by `stop_seq`. A journey from stop index `i` to stop index `j` occupies the half-open interval `[i, j)`. Two journeys on the same trip and seat overlap when:

`newStart < existingEnd && existingStart < newEnd`

Therefore, a passenger travelling A–C and another travelling C–E can use the same seat without overlap.

## 6. Fare calculation model

The engine calculates the stage fare by summing the fares of all route stages between the selected boarding and dropping stops. It then applies the dynamic uplift to stage fare only, adds the seat premium, applies the child fare factor where relevant, and calculates GST according to the bus AC flag. Amounts are rounded to paise per passenger line.

## 7. Cancellation and rescheduling

Cancellation uses the time remaining until each passenger's boarding-point departure. Refund tiers are 90% for more than 24 hours, 75% from 12 hours up to and including 24 hours, 50% from 4 hours to less than 12 hours, and 0% within 4 hours. Partial cancellation is passenger-specific; a free child cannot remain without an adult.

A ticket may be rescheduled once. The request must be at least six hours before the original boarding time and the new trip must fall within the permitted 30-day range. The engine charges Rs 50 per active passenger plus any positive fare difference; it does not issue a refund for a lower fare. Previously cancelled passenger rows remain in the final ticket output against their original trip.

## 8. Business-rule register

The authoritative detailed register is `docs/business-rules.md`, containing BR-01 through BR-38. Related rules are grouped into functional topics in this design document; those topics are not a replacement for the 38 individual rule IDs.

## 9. Design diagrams

The class and booking-state diagrams are included in the accompanying PDF as images and retained in editable Mermaid source form in `docs/class-diagram.md` and `docs/state-diagram.md`.

## 10. Testing and verification

The project includes `tests/verify_project.py`, which runs the official 127-command sample pack and additional interaction scenarios. The latest execution record is in `docs/verification-report.md`. The supplied pack does not contain expected output CSVs, and the final evaluation uses hidden input; passing the visible tests is evidence for those tested cases, not a guarantee of hidden-set correctness.

## 11. Open clarification items

The implementation assumptions and five mentor clarification topics are recorded in `docs/decision_log.md`. They should be updated if the assignment thread provides authoritative answers.
