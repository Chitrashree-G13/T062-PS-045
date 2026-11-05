
package service;

import java.time.LocalDateTime;
import java.util.*;
import model.*;

public class BookingService {

    private final SeatService seatService = new SeatService();
    private final FareService fareService = new FareService();

    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    // Backward-compatible method for existing code
    public Booking createBooking(
            Trip trip,
            String bookingId,
            Booking.BookingChannel channel,
            List<Booking.PassengerJourney> journeys) {

        return createBooking(
                trip, bookingId, channel, journeys,
                LocalDateTime.now(), null
        );
    }

    // Method for official command data
    public Booking createBooking(
            Trip trip,
            String bookingId,
            Booking.BookingChannel channel,
            List<Booking.PassengerJourney> journeys,
            LocalDateTime bookedAt,
            String partnerId) {

        if (trip == null || trip.isCancelled()) {
            throw new IllegalArgumentException("Trip is invalid or cancelled.");
        }

        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID is required.");
        }

        if (bookings.containsKey(bookingId)) {
            throw new IllegalArgumentException("Duplicate booking ID.");
        }

        if (journeys == null || journeys.isEmpty()) {
            throw new IllegalArgumentException("No passengers provided.");
        }

        if (channel == null || bookedAt == null) {
            throw new IllegalArgumentException("Booking details are incomplete.");
        }

        if (channel == Booking.BookingChannel.PARTNER
                && (partnerId == null || partnerId.isBlank())) {
            throw new IllegalArgumentException("Partner ID is required.");
        }

        if (channel == Booking.BookingChannel.DIRECT
                && partnerId != null && !partnerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Counter booking cannot have a partner ID.");
        }

        Booking booking = new Booking(
                bookingId, trip.getTripId(), channel, bookedAt, partnerId
        );

        Set<String> passengerNumbers = new HashSet<>();

        Map<String, Integer> freeChildrenPerAdult = new HashMap<>();

        // Validate passenger numbers and child associations.
        for (Booking.PassengerJourney journey : journeys) {

            if (journey == null || journey.getPassenger() == null) {
                throw new IllegalArgumentException("Invalid passenger details.");
            }

            Passenger passenger = journey.getPassenger();

            if (journey.getPaxNo() == null
                    || !passengerNumbers.add(
                            journey.getPaxNo().toUpperCase())) {
                throw new IllegalArgumentException(
                        "Missing or duplicate passenger number.");
            }

            if (passenger.isFreeChild()) {

                String adultId = passenger.getAdultPassengerId();

                if (adultId == null || adultId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Free child must be linked to an adult.");
                }

                boolean adultFound = false;

                for (Booking.PassengerJourney other : journeys) {
                    Passenger otherPassenger = other.getPassenger();

                    if (otherPassenger.getPassengerId().equals(adultId)
                            && otherPassenger.isAdult()) {
                        adultFound = true;
                        break;
                    }
                }

                if (!adultFound) {
                    throw new IllegalArgumentException(
                            "Accompanying adult not found in this ticket.");
                }

                int count = freeChildrenPerAdult.getOrDefault(adultId, 0) + 1;
                freeChildrenPerAdult.put(adultId, count);

                if (count > 1) {
                    throw new IllegalArgumentException(
                            "Maximum one free child per adult.");
                }

                if (journey.getSeatId() != null
                        && !journey.getSeatId().isBlank()
                        && !journey.getSeatId().equalsIgnoreCase("NOSEAT")) {
                    throw new IllegalArgumentException(
                            "Free child must use NOSEAT.");
                }

                continue;
            }
        }

        // Validate all journeys and seats before changing trip inventory.
        for (Booking.PassengerJourney journey : journeys) {

            Passenger passenger = journey.getPassenger();

            int from = journey.getBoardingIndex();
            int to = journey.getDroppingIndex();

            if (from < 0 || to >= trip.getRoute().getStopCount()
                    || from >= to) {
                throw new IllegalArgumentException(
                        "Invalid boarding or dropping point.");
            }

            if (passenger.isFreeChild()) {
                continue;
            }

            String seatId = journey.getSeatId();

            if (seatId == null || seatId.isBlank()
                    || seatId.equalsIgnoreCase("NOSEAT")) {
                throw new IllegalArgumentException("Seat is required.");
            }

            Seat seat = trip.getBus().findSeat(seatId);

            if (seat == null) {
                throw new IllegalArgumentException(
                        "Seat does not exist: " + seatId);
            }

            if (!seatService.isAvailable(trip, seatId, from, to)) {
                throw new IllegalArgumentException(
                        "Seat unavailable: " + seatId);
            }

            if (seat.isLadiesOnly()
                    && passenger.getGender() != Passenger.Gender.FEMALE) {
                throw new IllegalArgumentException(
                        "Ladies-only seat cannot be booked by this passenger.");
            }

            // Check against existing bookings.
            seatService.validateGenderAndAdjacency(
                    trip, passenger, seat, from, to, Collections.emptyList()
            );

            // Check duplicate seat requests within this ticket.
            for (Booking.PassengerJourney other : journeys) {

                if (other == journey || other.getSeatId() == null
                        || other.getSeatId().isBlank()
                        || other.getSeatId().equalsIgnoreCase("NOSEAT")) {
                    continue;
                }

                if (other.getSeatId().equalsIgnoreCase(seatId)
                        && seatService.overlaps(
                                from, to,
                                other.getBoardingIndex(),
                                other.getDroppingIndex())) {
                    throw new IllegalArgumentException(
                            "Same seat requested for overlapping journeys.");
                }

                // Male passenger cannot be placed beside a female
                // passenger travelling alone.
                if (passenger.getGender() == Passenger.Gender.MALE
                        && other.getPassenger().getGender()
                        == Passenger.Gender.FEMALE) {

                    Seat otherSeat = trip.getBus().findSeat(other.getSeatId());

                    if (otherSeat != null
                            && seatId.equalsIgnoreCase(
                                    Objects.toString(
                                            otherSeat.getAdjacentSeatId(), "" ))
                            && seatService.overlaps(
                                    from, to,
                                    other.getBoardingIndex(),
                                    other.getDroppingIndex())) {

                        throw new IllegalArgumentException(
                                "Male passenger cannot sit beside a solo female passenger.");
                    }
                }
            }
        }

        // Calculate all fares before adding the booking to the trip.
        for (Booking.PassengerJourney journey : journeys) {
            fareService.calculate(trip, journey);
            booking.addPassenger(journey);
        }

        trip.addBooking(booking);
        bookings.put(bookingId, booking);

        return booking;
    }

    public Booking getBooking(String bookingId) {

        Booking booking = bookings.get(bookingId);

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found.");
        }

        return booking;
    }

    public boolean containsBooking(String bookingId) {
        return bookings.containsKey(bookingId);
    }

    public Collection<Booking> getAllBookings() {
        return Collections.unmodifiableCollection(bookings.values());
    }
}
