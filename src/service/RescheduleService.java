package service;

import java.time.*;
import java.util.*;
import model.*;

public class RescheduleService {

    private final SeatService seatService = new SeatService();
    private final FareService fareService = new FareService();

    public double reschedule(
            Booking booking,
            Trip oldTrip,
            Trip newTrip,
            LocalDateTime now,
            LocalDateTime boardingTime) {

        if (booking.getRescheduleCount() >= 1) {
            throw new IllegalArgumentException(
                    "Only one reschedule is allowed.");
        }

        long hours = Duration.between(now, boardingTime).toHours();

        if (hours < 6) {
            throw new IllegalArgumentException(
                    "Rescheduling must be at least 6 hours before boarding.");
        }

        if (newTrip.getJourneyDate().isBefore(oldTrip.getJourneyDate())
                || newTrip.getJourneyDate().isAfter(
                        oldTrip.getJourneyDate().plusDays(30))) {
            throw new IllegalArgumentException(
                    "New trip must be within 30 days of the original trip.");
        }

        if (newTrip.isCancelled()) {
            throw new IllegalArgumentException("New trip is cancelled.");
        }

        Map<Booking.PassengerJourney, Double> oldFares =
                new HashMap<>();

        double oldTotal = 0;
        double newTotal = 0;
        int activeCount = 0;

        // Save old fares before calculating new fares.
        for (Booking.PassengerJourney journey : booking.getPassengers()) {

            if (journey.isCancelled()) {
                continue;
            }

            oldFares.put(journey, journey.getTotalFare());
            oldTotal += journey.getTotalFare();
            activeCount++;

            if (journey.getSeatId() != null
                    && !journey.getSeatId().isBlank()) {

                if (!seatService.isAvailable(
                        newTrip,
                        journey.getSeatId(),
                        journey.getBoardingIndex(),
                        journey.getDroppingIndex())) {

                    throw new IllegalArgumentException(
                            "Seat unavailable on the new trip: "
                                    + journey.getSeatId());
                }
            }
        }

        // Calculate the fare on the new trip.
        for (Booking.PassengerJourney journey : booking.getPassengers()) {

            if (journey.isCancelled()) {
                continue;
            }

            fareService.calculate(newTrip, journey);
            newTotal += journey.getTotalFare();
        }

        // Reschedule fee is Rs.50 per active passenger.
        double rescheduleFee = activeCount * 50.0;

        // Only a higher fare difference is collected.
        double fareDifference = Math.max(0, newTotal - oldTotal);

        double amountToCollect = rescheduleFee + fareDifference;

        oldTrip.removeBooking(booking);
        newTrip.addBooking(booking);

        booking.setTripId(newTrip.getTripId());
        booking.incrementRescheduleCount();

        return Math.round(amountToCollect * 100.0) / 100.0;
    }
}