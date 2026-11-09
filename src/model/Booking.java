
package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    public enum BookingStatus {
        CONFIRMED,
        PARTIALLY_CANCELLED,
        CANCELLED,
        RESCHEDULED_OUT
    }

    public enum BookingChannel {
        DIRECT,
        PARTNER
    }

    public static class PassengerJourney {

        private Passenger passenger;
        private String paxNo;
        private String seatId;
        private int boardingIndex;
        private int droppingIndex;

        private double baseFare;
        private double seatPremium;
        private double occupancyCharge;
        private double gst;
        private double totalFare;

        private boolean cancelled;

        public PassengerJourney(Passenger passenger, String seatId,
                                int boardingIndex, int droppingIndex) {
            this(passenger, passenger.getPassengerId(), seatId,
                    boardingIndex, droppingIndex);
        }

        public PassengerJourney(Passenger passenger, String paxNo,
                                String seatId, int boardingIndex,
                                int droppingIndex) {

            this.passenger = passenger;
            this.paxNo = paxNo;
            this.seatId = seatId;
            this.boardingIndex = boardingIndex;
            this.droppingIndex = droppingIndex;
            this.cancelled = false;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        public String getPaxNo() {
            return paxNo;
        }

        public String getSeatId() {
            return seatId;
        }

        public int getBoardingIndex() {
            return boardingIndex;
        }

        public int getDroppingIndex() {
            return droppingIndex;
        }

        public double getBaseFare() {
            return baseFare;
        }

        public double getSeatPremium() {
            return seatPremium;
        }

        public double getOccupancyCharge() {
            return occupancyCharge;
        }

        public double getGst() {
            return gst;
        }

        public double getTotalFare() {
            return totalFare;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setFareDetails(double baseFare,
                                   double seatPremium,
                                   double occupancyCharge,
                                   double gst,
                                   double totalFare) {
            this.baseFare = baseFare;
            this.seatPremium = seatPremium;
            this.occupancyCharge = occupancyCharge;
            this.gst = gst;
            this.totalFare = totalFare;
        }

        public void cancel() {
            cancelled = true;
        }

        public void changeJourney(int boardingIndex, int droppingIndex) {
            this.boardingIndex = boardingIndex;
            this.droppingIndex = droppingIndex;
        }

        public void changeSeat(String seatId) {
            this.seatId = seatId;
        }
    }

    private String bookingId;
    private String tripId;
    private List<PassengerJourney> passengers;
    private BookingChannel channel;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private int rescheduleCount;
    private String partnerId;

    // Existing constructor
    public Booking(String bookingId, String tripId,
                   BookingChannel channel) {
        this(bookingId, tripId, channel, LocalDateTime.now(), null);
    }

    // Constructor for official command data
    public Booking(String bookingId, String tripId,
                   BookingChannel channel, LocalDateTime bookedAt,
                   String partnerId) {

        this.bookingId = bookingId;
        this.tripId = tripId;
        this.channel = channel;
        this.bookedAt = bookedAt;
        this.partnerId = partnerId;
        this.passengers = new ArrayList<>();
        this.status = BookingStatus.CONFIRMED;
        this.rescheduleCount = 0;
    }

    public void addPassenger(PassengerJourney journey) {
        passengers.add(journey);
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public List<PassengerJourney> getPassengers() {
        return passengers;
    }

    public BookingChannel getChannel() {
        return channel;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public int getRescheduleCount() {
        return rescheduleCount;
    }

    public void incrementRescheduleCount() {
        rescheduleCount++;
    }
}
