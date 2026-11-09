# T062-PS-045 – 38 Business Rule Traceability and Test Plan

The official register is `docs/business-rules.md` (BR-01 through BR-38). This replaces the earlier draft test plan that incorrectly referred to 12 rules. The 12 headings in the earlier design document were grouped topics, not the official rule count.

## Test execution

1. Compile the Java application using the commands in the root README.
2. Run `python tests/verify_project.py` from the project root.
3. The script reruns the official sample pack and creates isolated temporary command files for combination tests.
4. A test is PASS only when its actual output matches the stated expectation.
5. The latest run is documented in `docs/verification-report.md`.

## Rule-by-rule traceability

| Rule | Rule summary | Verification / evidence | Coverage type |
|---|---|---|---|
| BR-01 | Route stops have a fixed order. | Loader sorts `route_stops.csv` by `stop_seq`; route output checked in DataLoadTest. | Code + sample data |
| BR-02 | Trip has one route, bus and travel date. | `CsvDataLoader` resolves route and bus IDs while constructing each trip. | Code audit + sample load |
| BR-03 | Valid boarding and dropping points. | Booking validates both stop IDs; invalid stop paths return rejection. | Code audit |
| BR-04 | Drop follows board on route. | Booking requires `boardIndex < dropIndex`. | Code audit |
| BR-05 | Stage fare sums all intervening stages. | `TKT-0036` stage fare checked against route stage sum. | Automated sample assertion |
| BR-06 | Seater has no premium. | Seater passenger fare output checked. | Output audit |
| BR-07 | Lower sleeper premium ₹150. | Lower-sleeper fare breakdown checked in sample tickets. | Output audit |
| BR-08 | Upper sleeper premium ₹100. | Seat premium calculation uses `Seat.getPremium()`; upper-berth rows inspected. | Code + output audit |
| BR-09 | Single sleeper premium ₹250. | `TKT-0036` single upper berth premium checked. | Automated sample assertion |
| BR-10 | Same seat can be reused on non-overlapping stages. | EC-01 lines 36 and 38 both accepted. | Automated sample assertion |
| BR-11 | Overlapping same-seat journeys rejected. | Same-seat overlapping fixture rejected with `SEAT_OVERLAP`; seat release combination also tested. | Automated combination tests |
| BR-12 | Ladies-only seats restricted to female passengers. | TKT-0071 rejected with `LADIES_SEAT`. | Automated sample assertion |
| BR-13 | Male cannot take adjacent berth beside solo female on separate ticket. | EC-02 rejects male-after-female; reverse-order fixture rejects female-after-male, confirming symmetry. | Automated sample + combination tests |
| BR-14 | At least 70% occupancy adds 15%. | EC-03 TKT-0042 has `dynamic_pct=15.00`. | Automated sample assertion |
| BR-15 | At least 90% occupancy adds 30%. | EC-07 rescheduled TKT-0024 receives 30%; threshold code uses `>= 0.90`. | Automated sample assertion + code audit |
| BR-16 | Dynamic uplift applies to stage fare, not premium. | TKT-0042 dynamic amount and premium fields checked separately. | Automated sample assertion |
| BR-17 | Under-five child travels free without a seat, max one per adult. | TKT-0049 free child; TKT-0063 rejected for excess free children. | Automated sample assertion |
| BR-18 | Ages 5–12 pay 75% seater / 100% sleeper. | TKT-0049 age-five seater has 75%; isolated sleeper-child fixture has 100%. | Automated sample + combination test |
| BR-19 | AC GST is 5% on fare plus premium. | AC ticket taxable/GST output inspected; formula uses 5% of taxable fare. | Code + output audit |
| BR-20 | Non-AC GST is 0%. | Non-AC sample tickets have zero GST. | Output audit |
| BR-21 | Partner commission is percentage of base fare excluding GST. | Settlement totals and commission percentage calculation inspected; uses `commission_pct`. | Code + output audit |
| BR-22 | Refund timing uses passenger boarding time. | Cancellation after boarding-point change uses the updated boarding time. | Combination test |
| BR-23 | More than 24 hours gives 90% refund. | Exact 24h+1s and 24h+1m fixtures both return 90%. | Automated boundary test |
| BR-24 | 12 to 24 hours inclusive gives 75%. | EC-05 exact 24-hour boundary returns 75%. | Automated sample assertion |
| BR-25 | 4 to less than 12 hours gives 50%. | Exact 4-hour fixture returns 50%; under-four-hour fixture returns 0%. | Automated boundary test |
| BR-26 | Within 4 hours gives 0%. | TKT-0064 cancellation at 3h10 has zero refund. | Automated sample assertion |
| BR-27 | Partial cancellation recalculates remaining passenger fares. | Active passenger fare lines are recalculated after cancellation; partial-cancel/reschedule combination tested. | Code + combination test |
| BR-28 | Free child cannot remain without responsible adult. | EC-06 cancellation rejected with `CHILD_ALONE`. | Automated sample assertion |
| BR-29 | Booking can be rescheduled only once. | TKT-0035 second reschedule rejected with `RESCHEDULE_LIMIT`. | Automated sample assertion |
| BR-30 | Reschedule >=6h before boarding and within 30 days. | TKT-0073 at 5h59 rejected; exact six-hour fixture accepted; date-range condition inspected. | Automated sample + boundary test + code audit |
| BR-31 | ₹50 per passenger plus positive fare difference. | EC-07 and TKT-0035 reschedule fee/fare difference checked. | Automated sample assertion |
| BR-32 | Lower reschedule fare gives no fare refund. | TKT-0035 lower-fare move records zero fare-difference credit/refund. | Automated sample assertion |
| BR-33 | Earlier boarding change free up to 2h; route restriction applies. | TKT-0075 accepted; 1h59 rejected; exact two-hour fixture accepted; after-drop rejected. | Automated sample + boundary test |
| BR-34 | Operator cancellation refunds 100% and credits ₹100 per passenger. | TRP-09 sample operator cancellation refund and ₹800 credit checked; partial-cancel/operator-cancel combination tested. | Automated sample + combination test |
| BR-35 | Operator cancellation reverses partner commission. | `commission_reversed` settlement column populated from partner commission rate. | Code + output audit |
| BR-36 | Cancellation fees shared between operator and partner. | Partner fee-share percentage used; sample partner master has 50%. | Code + output audit |
| BR-37 | Ticket includes passenger, trip, seat, boarding/drop and fare details. | `tickets.csv` schema checked; PRINT_TICKET prints passenger-level fare breakup at end-of-file state. | Automated sample + combination test |
| BR-38 | Trip chart includes seat and boarding/drop details. | `trip_chart.csv` schema and 13 sample rows checked. | Automated sample assertion |

## Combination scenarios included

- Partial cancellation followed by rescheduling: cancelled passenger row is preserved on the original trip; active passenger moves to the new trip.
- PRINT_TICKET appears before a later cancellation: printed ticket reflects the final end-of-file state.
- Cancelled seat is booked by another passenger.
- Boarding point is changed and then cancellation refund tier is calculated from the changed boarding time.
- Partial cancellation is followed by operator cancellation: only remaining passengers receive operator-cancellation refund and credit.
- Refund boundaries at more than 24 hours, exactly 24 hours, exactly 12 hours, exactly 4 hours and just under 4 hours.
- Exact six-hour reschedule and exact two-hour boarding-change eligibility boundaries.
- Same-seat overlapping rejection and adjacent-female validation in both booking orders.
- Age 5–12 sleeper fare factor (100%).

## Scope and limits

The official sample pack contains no expected-output CSVs. The verifier checks the planted scenarios and regression combinations but cannot reproduce the provider's hidden evaluation input. Some configuration-dependent cases (notably route restriction interpretations and settlement policy) are supported by source/output review because the full assignment brief and hidden evaluation input were not supplied. The five policy clarifications in `docs/decision_log.md` remain subject to mentor confirmation.
