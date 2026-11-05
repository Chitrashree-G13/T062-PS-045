
package model;

public class Seat {

    public enum SeatType {
        SEATER,
        LOWER_SLEEPER,
        UPPER_SLEEPER,
        SINGLE_SLEEPER,
        DOUBLE_SLEEPER
    }

    private String seatId;
    private SeatType seatType;
    private boolean ladiesOnly;
    private String adjacentSeatId;
    private String deck;

    public Seat(String seatId, SeatType seatType,
                boolean ladiesOnly, String adjacentSeatId) {
        this(seatId, seatType, ladiesOnly, adjacentSeatId, null);
    }

    public Seat(String seatId, SeatType seatType,
                boolean ladiesOnly, String adjacentSeatId,
                String deck) {

        if (seatId == null || seatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat ID cannot be empty");
        }

        if (seatType == null) {
            throw new IllegalArgumentException("Seat type cannot be null");
        }

        this.seatId = seatId.trim();
        this.seatType = seatType;
        this.ladiesOnly = ladiesOnly;
        this.adjacentSeatId = adjacentSeatId;
        this.deck = deck;
    }

    public String getSeatId() {
        return seatId;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public boolean isLadiesOnly() {
        return ladiesOnly;
    }

    public String getAdjacentSeatId() {
        return adjacentSeatId;
    }

    public String getDeck() {
        return deck;
    }

    public double getPremium() {
        switch (seatType) {
            case LOWER_SLEEPER:
                return 150;
            case UPPER_SLEEPER:
                return 100;
            case SINGLE_SLEEPER:
                return 250;
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return seatId + " - " + seatType
                + (ladiesOnly ? " - Ladies Seat" : "");
    }
}
