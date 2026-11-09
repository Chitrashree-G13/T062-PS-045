package service;

import model.*;
import util.Validation;

public class FareService {

    private final SeatService seatService = new SeatService();

    public void calculate(Trip trip,
                          Booking.PassengerJourney journey) {

        Passenger passenger = journey.getPassenger();

        // Children below 5 travel free without a seat.
        if (passenger.isFreeChild()) {
            journey.setFareDetails(0, 0, 0, 0, 0);
            return;
        }

        Seat seat = trip.getBus().findSeat(journey.getSeatId());

        if (seat == null) {
            throw new IllegalArgumentException("Invalid seat.");
        }

        double stageFare = trip.getRoute().calculateStageFare(
                journey.getBoardingIndex(),
                journey.getDroppingIndex()
        );

        double childFactor = 1.0;

        if (passenger.isChild()
                && seat.getSeatType() == Seat.SeatType.SEATER) {
            childFactor = 0.75;
        }

        double baseFare = stageFare * childFactor;

        double seatPremium = seat.getPremium();

        double multiplier = seatService.occupancyMultiplier(trip);

        // Occupancy increase applies only to stage fare.
        double occupancyCharge =
                stageFare * (multiplier - 1.0) * childFactor;

        double taxableAmount =
                baseFare + seatPremium + occupancyCharge;

        double gst = taxableAmount * trip.getBus().getGstRate();

        double total = baseFare + seatPremium
                + occupancyCharge + gst;

        journey.setFareDetails(
                Validation.money(baseFare),
                Validation.money(seatPremium),
                Validation.money(occupancyCharge),
                Validation.money(gst),
                Validation.money(total)
        );
    }
}