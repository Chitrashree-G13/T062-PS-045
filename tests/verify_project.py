#!/usr/bin/env python3
"""Regression verification for T062-PS-045.
Run from the project root after compiling: python tests/verify_project.py
The script uses only Python's standard library and the supplied sample pack.
"""
from __future__ import annotations
import csv, pathlib, subprocess, tempfile, sys
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parents[1]
INPUT = ROOT / "input" / "T062-PS-045-sample-data"
OUT = ROOT / "out"
JAVA = ["java", "-cp", str(OUT), "Main"]
PASS = []
FAIL = []

def check(name, condition, detail=""):
    (PASS if condition else FAIL).append((name, detail))
    print(("PASS" if condition else "FAIL") + " | " + name + (" | " + detail if detail else ""))

def read_csv(path):
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))

def run_engine(commands: str, label: str):
    temp = pathlib.Path(tempfile.mkdtemp(prefix="ps045_"))
    command_file = temp / "commands.txt"
    output_dir = temp / "output"
    command_file.write_text(commands, encoding="utf-8")
    proc = subprocess.run(JAVA + [str(INPUT), str(command_file), str(output_dir)],
                          cwd=ROOT, text=True, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(f"{label} engine failed:\n{proc.stdout}\n{proc.stderr}")
    return output_dir, proc.stdout + proc.stderr

# 1) Official sample pack end-to-end.
sample_out = ROOT / "output"
proc = subprocess.run(JAVA + [str(INPUT), str(INPUT / "commands.txt"), str(sample_out)],
                      cwd=ROOT, text=True, capture_output=True)
check("official sample command completes", proc.returncode == 0, proc.stderr[-300:])
if proc.returncode == 0:
    commands = [x for x in (INPUT / "commands.txt").read_text(encoding="utf-8").splitlines()
                if x.strip() and not x.lstrip().startswith("#")]
    results = read_csv(sample_out / "command_results.csv")
    tickets = read_csv(sample_out / "tickets.csv")
    charts = read_csv(sample_out / "trip_chart.csv")
    settlements = read_csv(sample_out / "partner_settlement.csv")
    check("one result per non-comment command", len(results) == len(commands) == 127,
          f"commands={len(commands)}, results={len(results)}")
    check("four required output files exist", all((sample_out / n).is_file() for n in
          ["tickets.csv", "command_results.csv", "trip_chart.csv", "partner_settlement.csv"]))
    expected_headers = {
      "tickets.csv": "ticket_id,pax_no,passenger_name,gender,age,trip_id,seat_no,board_stop_id,drop_stop_id,stage_fare,dynamic_pct,dynamic_amount,seat_premium,child_factor_pct,taxable_fare,gst,passenger_total,status",
      "command_results.csv": "line_no,command,ref_id,status,reason_code,refund_amount,fee_amount,fare_difference,credit_amount",
      "trip_chart.csv": "trip_id,board_stop_id,board_time,seat_no,ticket_id,pax_no,passenger_name,gender,age,drop_stop_id",
      "partner_settlement.csv": "partner_id,from_date,to_date,tickets_sold,base_fare_total,commission,cancellation_fee_share,commission_reversed,net_payable"
    }
    for filename, header in expected_headers.items():
        actual = (sample_out / filename).read_text(encoding="utf-8").splitlines()[0]
        check(f"{filename} header matches README", actual == header)
    by_line = {r["line_no"]: r for r in results}
    by_id = {}
    for row in tickets: by_id.setdefault(row["ticket_id"], []).append(row)
    check("EC-01 same seat non-overlap accepted", by_line["36"]["status"] == "OK" and by_line["38"]["status"] == "OK")
    check("EC-02 adjacent male rejected", by_line["43"]["reason_code"] == "ADJACENT_FEMALE")
    check("EC-03 exact 70 percent gives 15 percent", by_id["TKT-0042"][0]["dynamic_pct"] == "15.00")
    check("EC-03 dynamic amount is stage-fare-only", by_id["TKT-0042"][0]["dynamic_amount"] == "19.50" and by_id["TKT-0042"][0]["seat_premium"] == "0.00")
    child_rows = {r["pax_no"]: r for r in by_id["TKT-0049"]}
    check("EC-04 age five pays 75 percent and under-five child is free",
          child_rows["2"]["child_factor_pct"] == "75.00" and child_rows["3"]["child_factor_pct"] == "0.00")
    check("seat premiums: lower 150, upper 100 and single 250",
          any(r["seat_no"] == "L12" and r["seat_premium"] == "150.00" for r in tickets)
          and any(r["seat_no"] == "U06" and r["seat_premium"] == "100.00" for r in tickets)
          and any(r["seat_no"] == "U04" and r["seat_premium"] == "250.00" for r in tickets))
    check("AC GST is 5 percent and non-AC GST is zero",
          by_id["TKT-0042"][0]["gst"] == "7.48" and all(r["gst"] == "0.00" for r in by_id["TKT-0049"]))
    check("EC-05 cancellation exactly 24 hours uses 75 percent refund",
          by_line["96"]["refund_amount"] == "448.88" and by_line["96"]["fee_amount"] == "149.62")
    check("EC-06 adult cancellation leaving free child rejected", by_line["67"]["reason_code"] == "CHILD_ALONE")
    check("EC-07 reschedule higher dynamic fare and charge recorded",
          by_line["58"]["status"] == "OK" and by_line["58"]["fee_amount"] == "50.00"
          and by_line["58"]["fare_difference"] == "146.00"
          and by_id["TKT-0024"][0]["dynamic_pct"] == "30.00")
    check("lower-fare reschedule gives no fare refund/difference",
          by_line["72"]["status"] == "OK" and by_line["72"]["fee_amount"] == "100.00"
          and by_line["72"]["fare_difference"] == "0.00")
    check("EC-08 boarding change after drop rejected", by_line["93"]["reason_code"] == "BOARDING_AFTER_DROPPING")
    check("duplicate ticket retry rejected", by_line["56"]["reason_code"] == "DUPLICATE_TICKET")
    check("male on ladies-only seat rejected", by_line["80"]["reason_code"] == "LADIES_SEAT")
    check("one adult with two free children rejected", by_line["68"]["status"] == "REJECTED")
    check("second reschedule rejected", by_line["79"]["reason_code"] == "RESCHEDULE_LIMIT")
    check("late reschedule rejected", by_line["100"]["reason_code"] == "LATE_RESCHEDULE")
    check("valid earlier boarding change accepted", by_line["111"]["status"] == "OK")
    check("late boarding change rejected", by_line["103"]["status"] == "REJECTED")
    check("operator cancellation refunds and credits passengers", by_line["112"]["refund_amount"] == "7014.00" and by_line["112"]["credit_amount"] == "800.00")
    check("trip chart contains passenger rows", len(charts) == 13)
    check("settlement generated for all three partners", len(settlements) == 3)
    check("partner commission and operator-cancellation reversal populated",
          settlements[0]["commission"] == "917.60" and float(settlements[1]["commission_reversed"]) > 0)
    check("tickets output sorted by ticket then numeric passenger number",
          [(r["ticket_id"], int(r["pax_no"])) for r in tickets] ==
          sorted((r["ticket_id"], int(r["pax_no"])) for r in tickets))
    check("print ticket contains fare breakup", "ACTIVE BOOKING TOTAL" in proc.stdout and "Passenger 1:" in proc.stdout)

# 2) Combination: partial cancellation followed by reschedule; cancelled passenger is retained.
combo, log = run_engine("\n".join([
 "BOOK|CMB-01|TRP-01|2026-10-08 09:00:00|COUNTER||STP-BLR|STP-MNG|1:Adult One:M:30:L01;2:Adult Two:F:28:L02",
 "CANCEL|CMB-01|2026-10-08 10:00:00|2",
 "RESCHEDULE|CMB-01|2026-10-08 11:00:00|TRP-03|1:L03",
 "PRINT_TICKET|CMB-01", ""]) , "partial-cancel-reschedule")
rows = read_csv(combo / "tickets.csv")
cm = {r["pax_no"]: r for r in rows}
check("combo partial cancel + reschedule keeps both passenger rows", len(rows) == 2)
check("combo cancelled passenger retains original trip and cancelled status",
      cm["2"]["trip_id"] == "TRP-01" and cm["2"]["status"] == "CANCELLED")
check("combo active passenger moves to new trip", cm["1"]["trip_id"] == "TRP-03" and cm["1"]["status"] == "RESCHEDULED_OUT")
check("combo final ticket print includes cancelled passenger", "Passenger 2:" in log and "Status: CANCELLED" in log)

# 3) Combination: PRINT_TICKET appears before a later cancellation but must show end-of-file state.
combo, log = run_engine("\n".join([
 "BOOK|CMB-02|TRP-01|2026-10-08 09:00:00|COUNTER||STP-BLR|STP-MNG|1:Print Test:M:30:L04",
 "PRINT_TICKET|CMB-02",
 "CANCEL|CMB-02|2026-10-08 10:00:00|1", ""]) , "deferred-ticket-print")
check("PRINT_TICKET reports final state even when command precedes later change", "Status: CANCELLED" in log)

# 4) Combination: cancelled seat is released for a later booking.
combo, _ = run_engine("\n".join([
 "BOOK|CMB-03|TRP-01|2026-10-08 09:00:00|COUNTER||STP-BLR|STP-MNG|1:First:M:30:L01",
 "CANCEL|CMB-03|2026-10-08 10:00:00|1",
 "BOOK|CMB-04|TRP-01|2026-10-08 10:05:00|COUNTER||STP-BLR|STP-MNG|1:Second:F:30:L01", ""]) , "cancel-seat-release")
r = read_csv(combo / "command_results.csv")
check("combo cancelled seat can be booked by another passenger", r[2]["status"] == "OK")

# 5) Combination: boarding-point change affects subsequent cancellation time calculation.
combo, _ = run_engine("\n".join([
 "BOOK|CMB-05|TRP-01|2026-10-08 08:00:00|COUNTER||STP-NLM|STP-MNG|1:Boarding Test:F:31:L06",
 "CHANGE_BOARDING|CMB-05|2026-10-08 10:00:00|STP-BLR",
 "CANCEL|CMB-05|2026-10-08 21:30:00|1", ""]) , "boarding-change-cancel")
r = read_csv(combo / "command_results.csv")
refund = float(r[2]["refund_amount"]); fee = float(r[2]["fee_amount"])
check("combo cancellation uses changed boarding point time (24h tier)", round(refund / (refund + fee) * 100, 2) == 75.00)

# 6) Combination: partial cancellation followed by operator cancellation.
combo, _ = run_engine("\n".join([
 "BOOK|CMB-06|TRP-09|2026-10-14 08:00:00|PARTNER|PRT-01|STP-BLR|STP-MNG|1:Cancel First:F:31:L01;2:Keep Second:M:32:L02",
 "CANCEL|CMB-06|2026-10-14 09:00:00|1",
 "OPERATOR_CANCEL|TRP-09|2026-10-15 18:00:00|TEST_REASON", ""]) , "partial-cancel-operator-cancel")
r = read_csv(combo / "command_results.csv")
check("combo operator cancellation refunds remaining passenger only and credits once",
      r[2]["refund_amount"] != "0.00" and r[2]["credit_amount"] == "100.00")

# 7) Refund tier boundary checks: >24h, exactly 12h, exactly 4h, and <4h.
refund_commands = [
 "BOOK|RF-90S|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Refund90PlusSecond:M:30:L13",
 "BOOK|RF-90|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Refund90:M:30:L01",
 "BOOK|RF-75|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Refund75:M:30:L04",
 "BOOK|RF-50|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Refund50:M:30:L07",
 "BOOK|RF-00|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Refund00:M:30:L10",
 "CANCEL|RF-90S|2026-10-08 21:29:59|1",
 "CANCEL|RF-90|2026-10-08 21:29:00|1",
 "CANCEL|RF-75|2026-10-09 09:30:00|1",
 "CANCEL|RF-50|2026-10-09 17:30:00|1",
 "CANCEL|RF-00|2026-10-09 17:31:00|1", ""]
combo, _ = run_engine("\n".join(refund_commands), "refund-boundaries")
r = read_csv(combo / "command_results.csv")
expected_tiers = [90.0, 90.0, 75.0, 50.0, 0.0]
for i, pct in enumerate(expected_tiers, start=5):
    refund = float(r[i]["refund_amount"]); fee = float(r[i]["fee_amount"])
    actual = round(refund / (refund + fee) * 100, 2) if refund + fee else 0.0
    check(f"refund boundary tier {pct:.0f}%", actual == pct, f"actual={actual:.2f}%")

# 8) Reverse-order adjacency, overlapping inventory and sleeper-child fare checks.
combo, _ = run_engine("\n".join([
 "BOOK|ADJ-01|TRP-03|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Male First:M:31:U09",
 "BOOK|ADJ-02|TRP-03|2026-10-08 08:01:00|COUNTER||STP-BLR|STP-MNG|1:Female Second:F:29:U08", ""]), "reverse-adjacency")
r = read_csv(combo / "command_results.csv")
check("adjacent solo-female rule is symmetric when male books first", r[1]["reason_code"] == "ADJACENT_FEMALE")
combo, _ = run_engine("\n".join([
 "BOOK|OV-01|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:First:M:31:L01",
 "BOOK|OV-02|TRP-01|2026-10-08 08:01:00|COUNTER||STP-BLR|STP-MNG|1:Second:F:29:L01", ""]), "overlap-seat")
r = read_csv(combo / "command_results.csv")
check("same-seat overlapping journey is rejected", r[1]["reason_code"] == "SEAT_OVERLAP")
combo, _ = run_engine("\n".join([
 "BOOK|CH-SL|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:Sleeper Child:F:6:L01", ""]), "sleeper-child-factor")
r = read_csv(combo / "tickets.csv")
check("age 5–12 sleeper child pays 100 percent", r[0]["child_factor_pct"] == "100.00")

# 8) Exact reschedule and boarding-change eligibility boundaries.
combo, _ = run_engine("\n".join([
 "BOOK|BOUND-RES|TRP-01|2026-10-08 08:00:00|COUNTER||STP-BLR|STP-MNG|1:SixHour:M:30:L03",
 "RESCHEDULE|BOUND-RES|2026-10-09 15:30:00|TRP-03|1:L04", ""]), "exact-six-hour-reschedule")
r = read_csv(combo / "command_results.csv")
check("reschedule exactly six hours before boarding is accepted", r[1]["status"] == "OK")
combo, _ = run_engine("\n".join([
 "BOOK|BOUND-BOARD|TRP-01|2026-10-08 08:00:00|COUNTER||STP-NLM|STP-MNG|1:TwoHour:F:30:L01",
 "CHANGE_BOARDING|BOUND-BOARD|2026-10-09 20:15:00|STP-BLR", ""]), "exact-two-hour-boarding-change")
r = read_csv(combo / "command_results.csv")
check("boarding change exactly two hours before old boarding is accepted", r[1]["status"] == "OK")

print("\nSUMMARY: %d passed, %d failed" % (len(PASS), len(FAIL)))
if FAIL:
    sys.exit(1)
