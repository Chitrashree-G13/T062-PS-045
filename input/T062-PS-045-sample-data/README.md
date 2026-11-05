# PS-045 – Bus Reservation with Boarding Points, Seat Types and Partial Cancellation — Sample Data Pack

Prepared by 10 SECONDS for Team T062.

## Files

| File | What it holds | Row count |
|---|---|---|
| `stops.csv` | Stop master (boarding / dropping points) | 16 data rows |
| `route_stops.csv` | 3 routes with 6–9 ordered stops, scheduled offset from origin departure and stage fare | 23 data rows |
| `layout_seats.csv` | 2 bus layouts seat by seat: 40-seat 2+2 seater, 30-berth 1+2 sleeper (lower/upper deck) | 70 data rows |
| `buses.csv` | 4 buses with layout and AC flag | 4 data rows |
| `trips.csv` | 10 trips (route, bus, origin departure) | 10 data rows |
| `partners.csv` | 3 online partners with commission and cancellation-fee share | 3 data rows |
| `commands.txt` | Bookings, partial/full cancellations, reschedules, boarding changes, 1 operator cancellation, print requests | 127 command lines (excluding comments) |

Row numbers quoted below are 1-based data-row numbers (the header is row 0). Line numbers for `commands.txt` are physical line numbers including comment lines.

## Column reference

Conventions: dates `YYYY-MM-DD`; timestamps `YYYY-MM-DD HH:MM:SS` (24h, IST, no timezone suffix); money in Rs as plain decimals with 2 places (no commas, no symbol); percentages as plain numbers (12.5 means 12.5%); booleans `true`/`false`; IDs uppercase with a fixed prefix. An empty cell means "not applicable / not supplied" and only occurs in columns whose notes say so.

### `stops.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| stop_id | string | STP-XXX | — | primary key |
| stop_name | string | — | — | — |
| city | string | — | — | — |

### `route_stops.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| route_id | string | RT-nn | — | one row per stop |
| route_name | string | — | — | — |
| stop_seq | int | — | 1..n | stop index used for stage inventory is stop_seq - 1 |
| stop_id | string | — | stops.csv | — |
| offset_minutes | int | min | — | scheduled minutes after the trip's origin departure; boarding-point time = origin_departure_ts + offset |
| stage_fare_from_prev | decimal | Rs | — | seater fare of the stage from the previous stop; 0.00 on stop 1; fare A→B = sum over stops after A up to B |

### `layout_seats.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| layout_id | string | LY-xxxx | LY-ST40, LY-SL30 | — |
| seat_no | string | S01–S40 / L01–L15 / U01–U15 | — | unique within layout |
| deck | enum | — | LOWER, UPPER | seaters are LOWER |
| seat_type | enum | — | SEATER, SLEEPER_LOWER, SLEEPER_UPPER, SLEEPER_SINGLE | premium: SLEEPER_LOWER +150, SLEEPER_UPPER +100, SLEEPER_SINGLE +250 (single berths on either deck take the single premium only) |
| pair_seat_no | string | — | — | adjacent seat of a seater pair / double berth; empty for single berths |
| ladies_only | bool | — | — | true = seat marked L, female passengers only |

### `buses.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| bus_id | string | BUS-nn | — | — |
| layout_id | string | — | layout_seats.csv | — |
| registration_no | string | — | — | — |
| ac | bool | — | — | GST 5% if true, 0% if false |

### `trips.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| trip_id | string | TRP-nn | — | — |
| route_id | string | — | route_stops.csv | — |
| bus_id | string | — | buses.csv | — |
| trip_date | date | — | — | date of origin departure |
| origin_departure_ts | timestamp | — | — | — |

### `partners.csv`

| Column | Type | Units / format | Allowed values | Notes |
|---|---|---|---|---|
| partner_id | string | PRT-nn | — | — |
| partner_name | string | — | — | — |
| commission_pct | decimal | % | — | of base fare excluding GST |
| cancellation_fee_share_pct | decimal | % | — | partner's share of cancellation fees |

## Command file format

`commands.txt` holds one command per line in the form `COMMAND_NAME|arg1|arg2|...`. Commands must be processed strictly in file order. Lines starting with `#` are comments and blank lines are ignored. An empty argument (two adjacent `|`) means "not supplied". Every command used in this pack:

| Command | Arguments (in order) | Meaning |
|---|---|---|
| `BOOK` | `ticket_id|trip_id|booked_ts|channel|partner_id|board_stop_id|drop_stop_id|passengers` | channel COUNTER or PARTNER (partner_id empty for COUNTER). passengers = `;`-separated `pax_no:name:gender:age:seat_no`; gender M/F; age = completed years on the travel date; seat_no `NOSEAT` for a free child under 5. Occupancy for dynamic pricing is measured on the trip state immediately before this command |
| `CANCEL` | `ticket_id|cancel_ts|pax_nos` | pax_nos = comma-separated passenger numbers or `ALL` |
| `RESCHEDULE` | `ticket_id|request_ts|new_trip_id|seat_map` | same boarding/dropping stops on the new trip; seat_map = `;`-separated `pax_no:seat_no` (free children keep NOSEAT and are not listed) |
| `CHANGE_BOARDING` | `ticket_id|request_ts|new_board_stop_id` | change the boarding point of every passenger on the ticket |
| `OPERATOR_CANCEL` | `trip_id|cancel_ts|reason` | operator cancels the whole trip |
| `PRINT_TICKET` | `ticket_id` | print the ticket with fare breakup per passenger (state at end of file) |
| `PRINT_CHART` | `trip_id|board_stop_id` | print the chart of passengers boarding at that stop |
| `PRINT_SETTLEMENT` | `partner_id|from_date|to_date` | partner settlement for bookings with booked_ts in the date range |

## Planted scenarios

Every edge case in the brief, mapped to the exact record where it occurs, followed by extra traps.

- **EC-01 Seat booked A to C and another request for exactly C to E on the same seat**
  - TKT-0036 holds seat U04 on TRP-03 from Bengaluru (stop 1) to Kunigal (stop 3); TKT-0038 requests the same seat from Kunigal (stop 3) to Sakleshpur (stop 5) — intervals [0,2) and [2,4) do not overlap, so it must be ACCEPTED — `commands.txt line 36 and line 38`
- **EC-02 Solo female in a double berth with a male on a separate ticket requesting the adjacent berth**
  - TKT-0040: solo female in double berth U08 (TRP-03); TKT-0043 (separate ticket, male) requests the adjacent berth U09 — must be REJECTED — `commands.txt line 40 and line 43`
- **EC-03 Occupancy exactly 70% on the busiest stage only**
  - TKT-0042 on TRP-04 (40 seats) is booked when stage Chitradurga→Davangere holds exactly 28 seats (70.00%) and every other stage is below 70%; the booking itself is Haveri→Hubballi, which is not the busiest stage — occupancy is still 70% so +15% applies to the stage fare only — `commands.txt line 42`
- **EC-04 Child aged exactly 5 on travel date**
  - TKT-0049 pax 2 is aged exactly 5 on the travel date on a seater — not free; pays 75% of the seater fare. Pax 3 (age 3, NOSEAT) travels free with the adult — `commands.txt line 49`
- **EC-05 Cancellation at exactly 24 hours before the boarding-point time (not origin)**
  - TKT-0050 boards at Davangere (boarding time 2026-10-14 01:30:00, origin departs 2026-10-13 21:00:00); pax 1 cancelled at 2026-10-13 01:30:00 = exactly 24 h before the boarding-point time — tier '12 to 24 hours' (75%), not 90%; measured from origin it would wrongly look like 28.5 h — `commands.txt line 96`
- **EC-06 Partial cancellation of the only adult with a free child remaining**
  - TKT-0053: adult (pax 1) plus a free child aged 2 (pax 2, NOSEAT); CANCEL of pax 1 only would leave the free child alone — must be REJECTED — `commands.txt line 67`
- **EC-07 Reschedule to a trip with a higher dynamic multiplier**
  - TKT-0024 (TRP-01, booked with no dynamic uplift) is rescheduled on 2026-10-08 10:00 to TRP-02 where the busiest stage holds 27 of 30 berths (90%) — the new fare carries +30%; collect Rs 50 + the fare difference — `commands.txt line 58`
- **EC-08 Boarding-point change to a stop after the original drop**
  - TKT-0056 travels Tumakuru→Davangere on TRP-08; CHANGE_BOARDING to Haveri (after the original drop) must be REJECTED — `commands.txt line 93`
- **Extra traps (not in the brief, but realistic):**
  - TKT-0071: male passenger requests ladies-only seat S01 on TRP-06 — must be REJECTED — `commands.txt line 80`
  - TKT-0055 is BOOKed twice (duplicate partner retry 2 minutes later, same ticket id) — the second BOOK must be REJECTED as a duplicate — `commands.txt line 56`
  - TKT-0063: one adult with two children under 5 without seats (max 1 free child per adult) — must be REJECTED — `commands.txt line 68`
  - TKT-0064: partial cancellation 3 h 10 min before the boarding-point time — 0% refund tier — `commands.txt line 110`
  - TKT-0035: rescheduled TRP-05 → TRP-06 (lower occupancy, lower fare) — Rs 50 per passenger, no refund of the lower fare — `commands.txt line 72`
  - TKT-0035: second RESCHEDULE on the same ticket — must be REJECTED (allowed once) — `commands.txt line 79`
  - TKT-0073: RESCHEDULE requested 5 h 59 min before its boarding-point time — must be REJECTED (needs >= 6 h) — `commands.txt line 100`
  - TKT-0075: valid boarding-point change to Bengaluru Majestic (earlier on the route) well before boarding — free — `commands.txt line 111`
  - TKT-0074: boarding-point change requested 1 h 59 min before boarding — outside the free window (must be REJECTED) — `commands.txt line 103`
  - TRP-09 cancelled by the operator with 5 tickets (partner and counter) — 100% refund + Rs 100 per passenger, partner commission reversed — `commands.txt line 112`

## What is NOT in this pack

Expected outputs are not provided for this set. Validate your engine with your own test suite against the rules in your brief (BR-01 … BR-nn). A separate hidden input set with the same schema will be used at final evaluation; outputs are compared field-for-field, so follow the column formats above exactly in your own output.

## Output format expected at evaluation

Produce these CSV files (header row, same conventions as above, rows sorted as stated). Where the brief is silent on a format detail the choice below is binding for evaluation.

### `tickets.csv`

One row per passenger per ticket after all commands, sorted by ticket_id then pax_no.

Columns, in order: `ticket_id`, `pax_no`, `passenger_name`, `gender`, `age`, `trip_id`, `seat_no`, `board_stop_id`, `drop_stop_id`, `stage_fare`, `dynamic_pct`, `dynamic_amount`, `seat_premium`, `child_factor_pct`, `taxable_fare`, `gst`, `passenger_total`, `status`

### `command_results.csv`

One row per command line in file order (print commands included with status OK).

Columns, in order: `line_no`, `command`, `ref_id`, `status`, `reason_code`, `refund_amount`, `fee_amount`, `fare_difference`, `credit_amount`

### `trip_chart.csv`

One row per seated or NOSEAT passenger for each PRINT_CHART, sorted by board time then seat_no.

Columns, in order: `trip_id`, `board_stop_id`, `board_time`, `seat_no`, `ticket_id`, `pax_no`, `passenger_name`, `gender`, `age`, `drop_stop_id`

### `partner_settlement.csv`

One row per PRINT_SETTLEMENT.

Columns, in order: `partner_id`, `from_date`, `to_date`, `tickets_sold`, `base_fare_total`, `commission`, `cancellation_fee_share`, `commission_reversed`, `net_payable`

Binding choices: `status` in tickets.csv is BOOKED, CANCELLED or RESCHEDULED_OUT (old trip rows are replaced by the new trip's values, keep one row per passenger); command `status` is OK or REJECTED; `reason_code` is empty when OK, otherwise a short code of your choice that names the rule (e.g. SEAT_OVERLAP, LADIES_SEAT, ADJACENT_FEMALE, CHILD_ALONE, LATE_RESCHEDULE, DUPLICATE_TICKET). `dynamic_pct` is 0, 15 or 30. `child_factor_pct` is 100, 75 or 0. Amounts are rounded to paise per passenger line.
