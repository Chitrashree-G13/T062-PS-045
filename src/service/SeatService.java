
package service;

import java.util.*;
import model.*;

public class SeatService {

    // Journey intervals are [boarding, dropping).
    public boolean overlaps(int a, int b, int c, int d) {
        return a < d && c < b;
    }

    // Check seat availability for the requested journey.
    public boolean isAvailable(
            Trip trip, String seatId, int boarding, int dropping) {

        if (trip == null || trip.getBus().findSeat(seatId) == null) {
            return false;
        }

        for (Booking booking : trip.getBookings()) {
            for (Booking.PassengerJourney journey : booking.getPassengers()) {

                if (journey.isCancelled()
                        || journey.getSeatId() == null
                        || journey.getSeatId().equalsIgnoreCase("NOSEAT")) {
                    continue;
                }

                if (journey.getSeatId().equalsIgnoreCase(seatId)
                        && overlaps(
                                boarding, dropping,
                                journey.getBoardingIndex(),
                                journey.getDroppingIndex())) {
                    return false;
                }
            }
        }

        return true;
    }

    // Find maximum occupied seats on any one route stage.
    public int busiestStageOccupancy(Trip trip) {

        int maximum = 0;

        for (int stage = 0;
             stage < trip.getRoute().getStopCount() - 1;
             stage++) {

            int occupied = 0;

            for (Booking booking : trip.getBookings()) {
                for (Booking.PassengerJourney journey : booking.getPassengers()) {

                    if (journey.isCancelled()
                            || journey.getSeatId() == null
                            || journey.getSeatId().equalsIgnoreCase("NOSEAT")) {
                        continue;
                    }

                    if (journey.getBoardingIndex() <= stage
                            && journey.getDroppingIndex() > stage) {
                        occupied++;
                    }
                }
            }

            maximum = Math.max(maximum, occupied);
        }

        return maximum;
    }

    public double occupancyMultiplier(Trip trip) {

        int capacity = trip.getBus().getTotalSeats();

        if (capacity <= 0) {
            return 1.0;
        }

        double occupancy =
                (double) busiestStageOccupancy(trip) / capacity;

        if (occupancy >= 0.90) {
            return 1.30;
        }

        if (occupancy >= 0.70) {
            return 1.15;
        }

        return 1.0;
    }

    // Validate ladies-only seats and adjacency to an existing solo female.
    public void validateGenderAndAdjacency(
            Trip trip,
            Passenger passenger,
            Seat seat,
            int boarding,
            int dropping,
            List<String> sameTicketSeatIds) {

        if (seat.isLadiesOnly()
                && passenger.getGender() != Passenger.Gender.FEMALE) {
            throw new IllegalArgumentException(
                    "LADIES_SEAT: This seat is reserved for female passengers.");
        }

        for (Booking booking : trip.getBookings()) {

            long activeFemaleCount = booking.getPassengers().stream()
                    .filter(j -> !j.isCancelled()
                            && j.getPassenger().getGender()
                            == Passenger.Gender.FEMALE)
                    .count();

            for (Booking.PassengerJourney existing
                    : booking.getPassengers()) {

                if (existing.isCancelled()
                        || existing.getSeatId() == null
                        || existing.getSeatId().equalsIgnoreCase("NOSEAT")) {
                    continue;
                }

                if (existing.getPassenger().getGender()
                        != Passenger.Gender.FEMALE
                        || activeFemaleCount != 1) {
                    continue;
                }

                if (!overlaps(
                        boarding, dropping,
                        existing.getBoardingIndex(),
                        existing.getDroppingIndex())) {
                    continue;
                }

                Seat existingSeat =
                        trip.getBus().findSeat(existing.getSeatId());

                if (existingSeat == null) {
                    continue;
                }

                boolean adjacent =
                        seat.getSeatId().equalsIgnoreCase(
                                Objects.toString(
                                        existingSeat.getAdjacentSeatId(), ""))
                        || existing.getSeatId().equalsIgnoreCase(
                                Objects.toString(
                                        seat.getAdjacentSeatId(), ""));

                if (adjacent
                        && passenger.getGender() == Passenger.Gender.MALE) {
                    throw new IllegalArgumentException(
                            "ADJACENT_FEMALE: Male passenger cannot sit beside a solo female passenger.");
                }
            }
        }
    }
}
