package service;

import model.*;
import java.util.*;

public class WaitlistService {

    private final List<WaitlistEntry> entries = new ArrayList<>();
    private int nextId = 1;

    public WaitlistEntry addToWaitlist(
            String tripId,
            List<Booking.PassengerJourney> passengers) {

        WaitlistEntry entry = new WaitlistEntry(
                "WL" + nextId++,
                tripId,
                passengers
        );

        entries.add(entry);

        return entry;
    }

    public List<WaitlistEntry> getWaitingEntries(String tripId) {

        List<WaitlistEntry> result = new ArrayList<>();

        for (WaitlistEntry entry : entries) {

            if (entry.getTripId().equals(tripId)
                    && entry.getStatus()
                    == WaitlistEntry.WaitlistStatus.WAITING) {

                result.add(entry);
            }
        }

        return result;
    }

    // Attempt to confirm waiting passengers in FIFO order.
    public List<Booking> promoteAvailable(
            Trip trip,
            BookingService bookingService) {

        List<Booking> promoted = new ArrayList<>();

        for (WaitlistEntry entry : getWaitingEntries(trip.getTripId())) {

            try {
                Booking booking = bookingService.createBooking(
                        trip,
                        "AUTO-" + entry.getWaitlistId(),
                        Booking.BookingChannel.DIRECT,
                        entry.getRequestedPassengers()
                );

                entry.setStatus(
                        WaitlistEntry.WaitlistStatus.CONFIRMED);

                promoted.add(booking);

            } catch (IllegalArgumentException exception) {
                // Keep waiting if all requested seats are not available.
            }
        }

        return promoted;
    }

    public List<WaitlistEntry> getAllEntries() {
        return Collections.unmodifiableList(entries);
    }
}