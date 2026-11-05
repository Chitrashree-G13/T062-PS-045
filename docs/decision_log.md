# T062 – PS-045 Decision Log

| No. | Decision | Reason |
|---|---|---|
| 1 | Selected Java as the implementation language | Java supports object-oriented design and structured business logic. |
| 2 | Used CSV files for master data | CSV files provide simple, readable input data. |
| 3 | Used a command-file-driven system | The assignment requires processing commands without a graphical interface. |
| 4 | Represented journeys using ordered stop indexes | Seat conflicts must be evaluated by route-stage intervals rather than clock time. |
| 5 | Used separate service modules | Booking, pricing, cancellation and reporting responsibilities can be maintained independently. |
| 6 | Generated separate output CSV files | Different reports can be inspected and evaluated independently. |
| 7 | Used passenger-level cancellation calculations | Partial cancellation requires the fare to be split per passenger. |
| 8 | Used validation before accepting bookings | Invalid passenger and seat combinations must be rejected. |

**Important:** These are suggested decision-log entries based on the project design. Confirm that each describes a decision actually reflected in your implementation, and add the real decisions made by your team.