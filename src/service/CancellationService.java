
package service;

import java.time.Duration;
import java.time.LocalDateTime;
import model.*;

public class CancellationService {

    private double money(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    // Refund is calculated using the passenger's boarding time.
    public double calculateRefund(
            double fare,
            LocalDateTime cancelTime,
            LocalDateTime boardingTime) {

        if (fare < 0 || cancelTime == null || boardingTime == null) {
            throw new IllegalArgumentException("INVALID_CANCELLATION_DATA");
        }

        Duration remaining =
                Duration.between(cancelTime, boardingTime);

        if (remaining.isNegative()) {
            throw new IllegalArgumentException("BOARDING_TIME_PASSED");
        }

        double refundPercentage;

        // More than 24 hours: 90%
        if (remaining.compareTo(Duration.ofHours(24)) > 0) {
            refundPercentage = 90.0;
        }
        // Exactly 24 hours through 12 hours: 75%
        else if (remaining.compareTo(Duration.ofHours(12)) >= 0) {
            refundPercentage = 75.0;
        }
        // Less than 12 hours through 4 hours: 50%
        else if (remaining.compareTo(Duration.ofHours(4)) >= 0) {
            refundPercentage = 50.0;
        }
        // Less than 4 hours: no refund
        else {
            refundPercentage = 0.0;
        }

        return money(fare * refundPercentage / 100.0);
    }

    // Find a passenger using the official pax_no.
    private Booking.PassengerJourney findPassenger(
            Booking booking, String paxNo) {

        for (Booking.PassengerJourney journey : booking.getPassengers()) {
            if (journey.getPaxNo().equalsIgnoreCase(paxNo)) {
                return journey;
            }
        }

        return null;
    }

    // Prevent cancellation of an adult when their free child remains.
    private void validateChildDependency(
            Booking booking,
            Booking.PassengerJourney target) {

        Passenger passenger = target.getPassenger();

        if (!passenger.isAdult()) {
            return;
        }

        String adultId = passenger.getPassengerId();

        for (Booking.PassengerJourney journey : booking.getPassengers()) {

            Passenger child = journey.getPassenger();

            if (!journey.isCancelled()
                    && child.isFreeChild()
                    && adultId.equals(child.getAdultPassengerId())) {

                throw new IllegalArgumentException("CHILD_ALONE");
            }
        }
    }

    // Cancel one passenger.
    public double cancelPassenger(
            Booking booking,
            String paxNo,
            LocalDateTime cancelTime,
            LocalDateTime boardingTime) {

        if (booking == null) {
            throw new IllegalArgumentException("BOOKING_NOT_FOUND");
        }

        Booking.PassengerJourney target =
                findPassenger(booking, paxNo);

        if (target == null || target.isCancelled()) {
            throw new IllegalArgumentException("PASSENGER_NOT_FOUND");
        }

        validateChildDependency(booking, target);

        double refund = calculateRefund(
                target.getTotalFare(),
                cancelTime,
                boardingTime
        );

        target.cancel();

        boolean activePassengerFound = false;

        for (Booking.PassengerJourney journey : booking.getPassengers()) {
            if (!journey.isCancelled()) {
                activePassengerFound = true;
                break;
            }
        }

        if (activePassengerFound) {
            booking.setStatus(
                    Booking.BookingStatus.PARTIALLY_CANCELLED
            );
        } else {
            booking.setStatus(
                    Booking.BookingStatus.CANCELLED
            );
        }

        return refund;
    }

    // Cancel all passengers on a ticket.
    public double cancelAllPassengers(
            Booking booking,
            LocalDateTime cancelTime,
            Trip trip) {

        if (booking == null || trip == null) {
            throw new IllegalArgumentException("INVALID_CANCELLATION_DATA");
        }

        double totalRefund = 0.0;

        for (Booking.PassengerJourney journey : booking.getPassengers()) {

            if (journey.isCancelled()) {
                continue;
            }

            LocalDateTime boardingTime = trip.getBoardingTime(
                    journey.getBoardingIndex()
            );

            totalRefund += cancelPassenger(
                    booking,
                    journey.getPaxNo(),
                    cancelTime,
                    boardingTime
            );
        }

        return money(totalRefund);
    }

    // Partner's share of the cancellation fee.
    public double getPartnerCancellationShare(
            Booking booking,
            double originalFare,
            double refund,
            Partner partner) {

        if (booking == null
                || booking.getChannel()
                != Booking.BookingChannel.PARTNER
                || partner == null) {
            return 0.0;
        }

        double cancellationFee = originalFare - refund;

        double share = cancellationFee
                * partner.getCancellationFeeSharePct() / 100.0;

        return money(share);
    }

    // Operator cancellation: full refund and ₹100 credit per passenger.
    public double cancelEntireTrip(Trip trip) {

        if (trip == null) {
            throw new IllegalArgumentException("TRIP_NOT_FOUND");
        }

        trip.cancelTrip();

        double totalRefund = 0.0;

        for (Booking booking : trip.getBookings()) {

            for (Booking.PassengerJourney journey : booking.getPassengers()) {

                if (!journey.isCancelled()) {
                    totalRefund += journey.getTotalFare();
                    journey.cancel();
                }
            }

            booking.setStatus(Booking.BookingStatus.CANCELLED);
        }

        return money(totalRefund);
    }

    public double calculateOperatorCredit(Booking booking) {

        if (booking == null) {
            return 0.0;
        }

        int passengerCount = 0;

        for (Booking.PassengerJourney journey : booking.getPassengers()) {
            if (!journey.isCancelled()) {
                passengerCount++;
            }
        }

        return money(passengerCount * 100.0);
    }
}
