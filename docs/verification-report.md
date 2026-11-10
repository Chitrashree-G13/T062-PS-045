# PS-045 – T062 Verification Report

**Verification date:** 9 November 2026  
**Input:** Official `T062-PS-045-sample-data` supplied by 10 SECONDS  
**Build:** Java compilation with `javac -Xlint:all`  
**Automated verifier:** `tests/verify_project.py`

## Result

**Result: 50 automated checks passed; 0 failed.**

The project compiled successfully. `DataLoadTest` loaded 3 routes, 4 buses, 10 trips and 3 partners. The official 127-command file completed and generated all four evaluation CSVs.

| Output | Data rows |
|---|---:|
| `tickets.csv` | 152 |
| `command_results.csv` | 127 |
| `trip_chart.csv` | 13 |
| `partner_settlement.csv` | 3 |

The 127 official commands resulted in 117 `OK` and 10 `REJECTED` statuses. The rejected commands correspond to deliberately invalid planted scenarios and extra traps (such as adjacent-female protection, duplicate ticket, child-alone, ladies-only seat, late reschedule and invalid boarding change).

## Checks included

- All eight planted edge cases in the supplied README.
- Duplicate ticket retry, ladies-only seat, excess free children, second reschedule, late reschedule, late boarding change and lower-fare reschedule.
- Stage fare, lower/upper/single sleeper premiums, child fare factors, AC/non-AC GST and partner commission/reversal fields.
- CSV headers, required files, command-result count, ticket sort order and chart/settlement row generation.
- Partial cancellation followed by rescheduling, preserving cancelled passenger records.
- PRINT_TICKET requested before a later cancellation, confirming end-of-file state is printed.
- Rebooking a cancelled seat.
- Boarding-point change followed by cancellation, confirming the updated boarding time is used.
- Partial cancellation followed by operator cancellation.
- Refund boundaries: more than 24 hours, exactly 24 hours (official EC-05), exactly 12 hours, exactly 4 hours and just under 4 hours.
- Exact six-hour reschedule and exact two-hour boarding-change eligibility.
- Adjacent solo-female protection in both booking orders.
- Overlapping same-seat rejection and age 5–12 sleeper fare factor.

## Code and documentation corrections made

1. Fixed rescheduling so previously cancelled passengers are not silently dropped from `tickets.csv`; active passengers move to the new trip while cancelled passenger records retain their original trip context.
2. Added fare-breakup output for `PRINT_TICKET` and deferred it until all commands have run, as required by the README's end-of-file-state instruction.
3. Made adjacent solo-female berth validation symmetric, so the result does not depend on which booking is entered first.
4. Corrected partner commission calculations to use `commission_pct` from `partners.csv` rather than a hard-coded 8%.
5. Recalculate remaining active passenger fare lines after partial cancellation.
6. Compare cancellation durations at exact second precision, avoiding a 24-hours-plus-one-second boundary error.
7. Reject invalid gender codes and duplicate passenger numbers in a cancellation request.
8. Improved CSV parsing for quoted commas and escaped quotes.
9. Fixed numeric passenger-number sorting in `tickets.csv`.
10. Replaced the text file named `design_document.pdf` with a valid PDF design document; retained an editable Markdown source.
11. Corrected the README's business-rule count from 12 to the official 38 and cleaned `.gitignore`.

## Important limitations – not a hidden-test guarantee

- The supplied sample pack does not contain authoritative expected-output CSVs. The verifier checks the README's planted scenarios and the regression conditions described above.
- The hidden final-evaluation input is not present in the ZIP, so it cannot be run here.
- The full original assignment brief and mentor replies were not included. Five policy questions remain recorded in `docs/decision_log.md`, including whether reschedule fare difference should include a GST delta and how dynamic uplift enters the partner commission base.
- Rules involving policy interpretation or alternative hidden master-data configurations have been code-reviewed and mapped in `tests/Test_Plan.md`; no sample can prove every possible input combination.

**Recommendation:** The visible sample and regression checks are passing. Before final submission, confirm the open policy questions on the team's assignment email thread and upload the final Git changes to the repository shared with 10 SECONDS.
