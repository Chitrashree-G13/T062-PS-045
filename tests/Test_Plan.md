# T062-PS-045 – Business Rule Test Plan

## Test Cases

| Test ID | Test Scenario | Expected Result |
|---|---|---|
| TC-01 | Overlapping seat booking | Rejected |
| TC-02 | Same seat on A–C and C–E | Both accepted |
| TC-03 | Stage fare calculation | Correct fare |
| TC-04 | Lower sleeper premium | Rs 150 added |
| TC-05 | Male requests ladies seat | Rejected |
| TC-06 | Male requests adjacent seat to solo female | Rejected |
| TC-07 | Occupancy exactly 70% | 15% increase |
| TC-08 | Occupancy exactly 90% | 30% increase |
| TC-09 | Child under 5 with adult | Free without seat |
| TC-10 | Child exactly 5 | Paid child fare |
| TC-11 | AC bus booking | 5% GST |
| TC-12 | Non-AC bus booking | 0% GST |
| TC-13 | Partner booking | 8% commission |
| TC-14 | Partner cancellation | 50:50 fee sharing |
| TC-15 | Cancellation exactly 24 hours before boarding | 75% refund |
| TC-16 | Cancellation within 4 hours | 0% refund |
| TC-17 | Partial cancellation | Only selected passenger cancelled |
| TC-18 | Cancel adult with free child remaining | Rejected |
| TC-19 | Second reschedule attempt | Rejected |
| TC-20 | Reschedule less than 6 hours before boarding | Rejected |
| TC-21 | Reschedule to higher fare | Correct fare difference |
| TC-22 | Boarding change after original drop | Rejected |
| TC-23 | Operator trip cancellation | Full refund and Rs 100 credit per passenger |

## Test Execution Status

For each test, record:
- Actual result
- PASS or FAIL
- Date tested
- Remarks

Only mark PASS after the actual output matches the expected result.