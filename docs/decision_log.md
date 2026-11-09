# T062 – PS-045 Decision Log

## Project: Bus Reservation with Boarding Points, Seat Types and Partial Cancellation

**Team:** T062  
**Project Code:** PS-045  
**Language:** Java 17+

## Implementation Decisions

| No. | Decision / Implementation Assumption | Reason / Evidence |
|---|---|---|
| 1 | Java 17+ is used for the command-line engine. | Matches the team's implementation and compilation workflow. |
| 2 | CSV files are used for master data and evaluation output. | Matches the supplied sample-data pack. |
| 3 | Commands are processed in physical file order; comments and blank lines are ignored. | Required by the sample-data README. |
| 4 | A journey occupies a half-open route-stage interval `[boardIndex, dropIndex)`. | Allows a seat to be reused when one passenger drops exactly where the next passenger boards. |
| 5 | Dynamic pricing uses the maximum occupied stage immediately before the booking command. | Explicitly required by the sample-data README. |
| 6 | Dynamic uplift applies to stage fare, not seat premium. | Based on BR-16. |
| 7 | Fare amounts are rounded to paise per passenger line. | Follows the output convention in the sample-data README. |
| 8 | Cancellation tier uses the time remaining until the passenger's boarding-point time. | Based on BR-22 through BR-26 and planted edge case EC-05. |
| 9 | Rescheduling is allowed once. Active passengers move to the new trip, while previously cancelled passenger rows remain represented against their original trip. | Preserves one passenger row per ticket and avoids losing cancelled passenger information during rescheduling. |
| 10 | `PRINT_TICKET` output is deferred until all commands are processed. | The sample-data README requires the ticket to show its state at the end of the command file. |
| 11 | Partner commission and cancellation-fee share use the percentages in `partners.csv`. | These percentages are provided as partner master data. |
| 12 | CSV input parsing supports quoted commas and escaped quotes. | Prevents commas inside quoted fields from being treated as column separators. |

## Clarifications to Confirm with 10 SECONDS

The sample README states that the detailed assignment brief contains the authoritative business rules. However, the full assignment brief and mentor clarifications were not included in the supplied ZIP.

The following implementation assumptions should therefore be confirmed with the mentor or on the team assignment thread.

### 1. Cancellation Refund Boundaries

Confirm the exact cancellation boundary wording, including exactly 24 hours, 12 hours and 4 hours before boarding.

The supplied planted scenario confirms that cancellation exactly 24 hours before boarding belongs to the 12–24 hour refund tier (75%).

**Current implementation:** Uses the cancellation time remaining until the passenger's boarding-point time and handles exact time boundaries.

### 2. Dynamic Pricing and Partner Commission

Confirm whether dynamic fare uplift is included in the partner commission calculation.

**Current implementation:** Uses stage fare after the child fare factor, excluding GST and dynamic uplift.

### 3. Reschedule Fare Difference and GST

Confirm whether the fare difference collected during rescheduling should include the GST difference.

**Current implementation:** Compares taxable fare before GST. GST is recalculated for the destination trip and displayed in the ticket output.

This approach preserves the supplied sample-pack lower-fare scenario, which expects no refund or negative fare difference. Confirm whether the hidden evaluation expects the GST change itself to be collected.

### 4. Boarding-Point Change Restrictions

Confirm whether a boarding-point change may only move the passenger to an earlier stop on the same route.

**Current implementation:** Allows an earlier boarding stop only and rejects a boarding stop after the original drop point.

### 5. Partner Cancellation-Fee Share and Commission Reversal

Confirm how the cancellation-fee share should be calculated for partners and whether commission should be reversed during operator cancellation.

**Current implementation:** Uses the percentages provided in the partner master file and reverses commission on operator cancellation.

## Verification Status

- Java compilation completed successfully.
- Master CSV data loading verified.
- Official sample command file processed successfully.
- All four required output CSV files generated.
- Regression verification completed with **50 passed and 0 failed**.
- Business-rule coverage is mapped against BR-01 through BR-38 in `tests/Test_Plan.md`.

## Important Note

The above five clarification topics are implementation assumptions that should not be silently changed. Any mentor-approved changes must be recorded in this decision log and reflected in the implementation and tests.