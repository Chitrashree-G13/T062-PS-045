package service;

import model.*;
import java.util.Locale;

public class ReportService {

    public void generateTicket(Booking booking, Trip trip) {

        System.out.println("\n========== BUS TICKET ==========");
        System.out.println("Booking ID : " + booking.getBookingId());
        System.out.println("Trip ID    : " + trip.getTripId());
        System.out.println("Route      : " + trip.getRoute().getRouteName());
        System.out.println("Travel Date: " + trip.getJourneyDate());
        System.out.println("Bus        : " + trip.getBus().getBusName());
        System.out.println("Booking    : " + booking.getStatus());
        System.out.println("--------------------------------");

        double total = 0;

        for (Booking.PassengerJourney journey : booking.getPassengers()) {

            Passenger passenger = journey.getPassenger();

            System.out.println("Passenger  : " + passenger.getName());
            System.out.println("Age        : " + passenger.getAge());
            System.out.println("Gender     : " + passenger.getGender());

            if (journey.isCancelled()) {
                System.out.println("Status     : CANCELLED");
                System.out.println("--------------------------------");
                continue;
            }

            String from = trip.getRoute().getStops()
                    .get(journey.getBoardingIndex()).getName();

            String to = trip.getRoute().getStops()
                    .get(journey.getDroppingIndex()).getName();

            System.out.println("Seat       : " + journey.getSeatId());
            System.out.println("From       : " + from);
            System.out.println("To         : " + to);
            System.out.printf(Locale.US, "Base Fare  : Rs. %.2f%n",
                    journey.getBaseFare());
            System.out.printf(Locale.US, "Seat Premium: Rs. %.2f%n",
                    journey.getSeatPremium());
            System.out.printf(Locale.US, "Occupancy  : Rs. %.2f%n",
                    journey.getOccupancyCharge());
            System.out.printf(Locale.US, "GST        : Rs. %.2f%n",
                    journey.getGst());
            System.out.printf(Locale.US, "Total Fare : Rs. %.2f%n",
                    journey.getTotalFare());

            total += journey.getTotalFare();

            System.out.println("--------------------------------");
        }

        System.out.printf(Locale.US, "BOOKING TOTAL: Rs. %.2f%n", total);
        System.out.println("================================\n");
    }

    public void generateTripChart(Trip trip, int boardingIndex) {

        System.out.println("\n========== TRIP CHART ==========");
        System.out.println("Trip: " + trip.getTripId());

        String stopName = trip.getRoute().getStops()
                .get(boardingIndex).getName();

        System.out.println("Boarding Point: " + stopName);
        System.out.println("--------------------------------");

        for (Booking booking : trip.getBookings()) {
            for (Booking.PassengerJourney journey : booking.getPassengers()) {

                if (!journey.isCancelled()
                        && journey.getBoardingIndex() == boardingIndex) {

                    System.out.println(
                            journey.getPassenger().getName()
                            + " | Seat: " + journey.getSeatId()
                    );
                }
            }
        }

        System.out.println("================================\n");
    }

    public double calculatePartnerCommission(Booking booking) {

        if (booking.getChannel() != Booking.BookingChannel.PARTNER) {
            return 0;
        }

        double commission = 0;

        for (Booking.PassengerJourney journey : booking.getPassengers()) {
            if (!journey.isCancelled()) {
                commission += journey.getBaseFare() * 0.08;
            }
        }

        return commission;
    }
}