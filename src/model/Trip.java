
package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Trip {

    private String tripId;
    private Route route;
    private Bus bus;
    private LocalDate journeyDate;
    private LocalDateTime originDeparture;
    private List<Booking> bookings;
    private boolean cancelled;

    // Backward-compatible constructor
    public Trip(String tripId, Route route,
                Bus bus, LocalDate journeyDate) {
        this(tripId, route, bus,
                journeyDate.atStartOfDay());
    }

    public Trip(String tripId, Route route,
                Bus bus, LocalDateTime originDeparture) {

        if (route == null || bus == null || originDeparture == null) {
            throw new IllegalArgumentException(
                    "Route, bus and departure time are required");
        }

        this.tripId = tripId;
        this.route = route;
        this.bus = bus;
        this.originDeparture = originDeparture;
        this.journeyDate = originDeparture.toLocalDate();
        this.bookings = new ArrayList<>();
        this.cancelled = false;
    }

    public String getTripId() {
        return tripId;
    }

    public Route getRoute() {
        return route;
    }

    public Bus getBus() {
        return bus;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public LocalDateTime getOriginDeparture() {
        return originDeparture;
    }

    public LocalDateTime getBoardingTime(int stopIndex) {
        return originDeparture.plusMinutes(
                route.getStop(stopIndex).getOffsetMinutes());
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void addBooking(Booking booking) {
        if (cancelled) {
            throw new IllegalStateException("Trip is cancelled");
        }
        bookings.add(booking);
    }

    public void removeBooking(Booking booking) {
        bookings.remove(booking);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancelTrip() {
        cancelled = true;
    }

    @Override
    public String toString() {
        return tripId + " | " + route.getRouteName()
                + " | " + originDeparture
                + " | " + bus.getRegistrationNo();
    }
}
