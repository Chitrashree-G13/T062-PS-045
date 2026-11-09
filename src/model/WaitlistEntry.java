package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WaitlistEntry {

    public enum WaitlistStatus {
        WAITING,
        CONFIRMED,
        CANCELLED
    }

    private String waitlistId;
    private String tripId;
    private List<Booking.PassengerJourney> requestedPassengers;
    private LocalDateTime createdAt;
    private WaitlistStatus status;

    public WaitlistEntry(String waitlistId, String tripId,
                         List<Booking.PassengerJourney> passengers) {

        this.waitlistId = waitlistId;
        this.tripId = tripId;
        this.requestedPassengers = new ArrayList<>(passengers);
        this.createdAt = LocalDateTime.now();
        this.status = WaitlistStatus.WAITING;
    }

    public String getWaitlistId() {
        return waitlistId;
    }

    public String getTripId() {
        return tripId;
    }

    public List<Booking.PassengerJourney> getRequestedPassengers() {
        return new ArrayList<>(requestedPassengers);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }
}