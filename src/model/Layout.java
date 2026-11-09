
package model;

import java.util.ArrayList;
import java.util.List;

public class Layout {

    private String layoutId;
    private List<Seat> seats;

    public Layout(String layoutId) {
        this.layoutId = layoutId;
        this.seats = new ArrayList<>();
    }

    public String getLayoutId() {
        return layoutId;
    }

    public void addSeat(Seat seat) {
        if (seat == null) {
            throw new IllegalArgumentException("Seat cannot be null");
        }
        seats.add(seat);
    }

    public List<Seat> getSeats() {
        return new ArrayList<>(seats);
    }

    public Seat findSeat(String seatNo) {
        for (Seat seat : seats) {
            if (seat.getSeatId().equalsIgnoreCase(seatNo)) {
                return seat;
            }
        }
        return null;
    }
}
