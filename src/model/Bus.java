
package model;

import java.util.ArrayList;
import java.util.List;

public class Bus {

    private String busId;
    private String busName;
    private String layoutId;
    private String registrationNo;
    private boolean ac;
    private List<Seat> seats;

    // Backward-compatible constructor
    public Bus(String busId, String busName, boolean ac) {
        this(busId, busName, null, null, ac);
    }

    public Bus(String busId, String layoutId,
               String registrationNo, boolean ac) {
        this(busId, busId, layoutId, registrationNo, ac);
    }
    
    public void loadLayout(Layout layout) {
        if (layout == null) {
            throw new IllegalArgumentException("Layout cannot be null");
        }

        for (Seat seat : layout.getSeats()) {
            addSeat(seat);
        }
    }
    private Bus(String busId, String busName,
                String layoutId, String registrationNo,
                boolean ac) {

        this.busId = busId;
        this.busName = busName;
        this.layoutId = layoutId;
        this.registrationNo = registrationNo;
        this.ac = ac;
        this.seats = new ArrayList<>();
    }

    public void addSeat(Seat seat) {

        if (seat == null) {
            throw new IllegalArgumentException("Seat cannot be null");
        }

        if (findSeat(seat.getSeatId()) != null) {
            throw new IllegalArgumentException("Duplicate seat ID");
        }

        seats.add(seat);
    }

    public Seat findSeat(String seatId) {
        for (Seat seat : seats) {
            if (seat.getSeatId().equalsIgnoreCase(seatId)) {
                return seat;
            }
        }
        return null;
    }

    public String getBusId() {
        return busId;
    }

    public String getBusName() {
        return busName;
    }

    public String getLayoutId() {
        return layoutId;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public boolean isAc() {
        return ac;
    }

    public double getGstRate() {
        return ac ? 0.05 : 0.0;
    }

    public List<Seat> getSeats() {
        return new ArrayList<>(seats);
    }

    public int getTotalSeats() {
        return seats.size();
    }

    @Override
    public String toString() {
        return busId + " - " + registrationNo
                + " - " + (ac ? "AC" : "Non-AC")
                + " - Seats: " + seats.size();
    }
}
