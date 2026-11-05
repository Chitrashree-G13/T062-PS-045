
# Business Rule Register – Bus Reservation System

| Rule ID | Business Rule |
|---|---|
| BR-01 | Each route contains stops arranged in a fixed order. |
| BR-02 | A trip is associated with one route, one bus and a travel date. |
| BR-03 | A passenger must select valid boarding and dropping points. |
| BR-04 | The dropping point must occur after the boarding point on the route. |
| BR-05 | The fare is calculated by adding the fares of all stages between boarding and dropping points. |
| BR-06 | Seater seats have no additional seat premium. |
| BR-07 | Lower sleeper seats have a premium of ₹150. |
| BR-08 | Upper sleeper seats have a premium of ₹100. |
| BR-09 | Single sleeper seats have a premium of ₹250. |
| BR-10 | A seat can be booked by different passengers for non-overlapping travel stages. |
| BR-11 | Two bookings cannot reserve the same seat for overlapping travel stages on the same trip. |
| BR-12 | Seats marked as ladies seats can only be booked by female passengers. |
| BR-13 | A male passenger cannot book an adjacent seat on a separate ticket when that seat is reserved for a solo female passenger. |
| BR-14 | Dynamic pricing adds 15% when the highest stage occupancy is at least 70%. |
| BR-15 | Dynamic pricing adds 30% when the highest stage occupancy is at least 90%. |
| BR-16 | Dynamic pricing applies to stage fare, not seat premium. |
| BR-17 | Children under 5 travel free without a separate seat, subject to the one-child-per-adult limit. |
| BR-18 | Children aged 5–12 pay 75% of the seater fare or 100% of the sleeper fare. |
| BR-19 | AC trips charge 5% GST on fare plus seat premium. |
| BR-20 | Non-AC trips charge 0% GST. |
| BR-21 | Partner commission is 8% of the base fare excluding GST. |
| BR-22 | Cancellation refunds are calculated using the time remaining until the passenger's boarding time. |
| BR-23 | Cancellation more than 24 hours before boarding gives a 90% refund. |
| BR-24 | Cancellation from 12 hours up to and including 24 hours before boarding gives a 75% refund. |
| BR-25 | Cancellation from 4 hours up to less than 12 hours before boarding gives a 50% refund. |
| BR-26 | Cancellation within 4 hours of boarding gives no refund. |
| BR-27 | Partial cancellation must recalculate the fare for the remaining passengers. |
| BR-28 | A free child cannot remain on a booking if the adult responsible for that child is cancelled. |
| BR-29 | A booking may be rescheduled only once. |
| BR-30 | Rescheduling must occur at least 6 hours before boarding and within 30 days. |
| BR-31 | Rescheduling costs ₹50 per passenger plus any positive fare difference. |
| BR-32 | A lower fare after rescheduling does not generate a refund. |
| BR-33 | Boarding-point changes are free up to 2 hours before boarding, subject to the specified route restriction. |
| BR-34 | Operator cancellation gives a 100% refund and ₹100 inconvenience credit per passenger. |
| BR-35 | Operator cancellation reverses the partner commission. |
| BR-36 | Cancellation fees are shared equally between the operator and partner. |
| BR-37 | Ticket details must include passenger, trip, seat, boarding point, dropping point and fare information. |
| BR-38 | The system must generate a trip chart showing seat occupancy and passenger boarding/drop details. |

## Clarifications Required

1. Confirm the exact boundary rules for cancellations with the mentor.
2. Confirm fare rounding and how dynamic pricing interacts with child fares.
3. Confirm whether GST applies to the seat premium as well as the stage fare.
4. Clarify the route restriction for changing a boarding point.
5. Confirm how partner commission and cancellation fees are settled.
