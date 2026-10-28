import java.util.*;

public class Main {

    static String[] stops = {"A", "B", "C", "D"};
    static int farePerStage = 100;

    static class Booking {
        String passenger;
        String seat;
        int boarding;
        int dropping;

        Booking(String passenger, String seat, int boarding, int dropping) {
            this.passenger = passenger;
            this.seat = seat;
            this.boarding = boarding;
            this.dropping = dropping;
        }
    }

    static ArrayList<Booking> bookings = new ArrayList<>();

    static int findStop(String stop) {
        for (int i = 0; i < stops.length; i++) {
            if (stops[i].equalsIgnoreCase(stop)) {
                return i;
            }
        }
        return -1;
    }

    static boolean isSeatAvailable(String seat, int boarding, int dropping) {
        for (Booking b : bookings) {
            if (b.seat.equalsIgnoreCase(seat)) {
                boolean overlaps =
                    boarding < b.dropping && b.boarding < dropping;

                if (overlaps) {
                    return false;
                }
            }
        }
        return true;
    }

    static void bookSeat(Scanner sc) {
        System.out.print("Passenger name: ");
        String name = sc.nextLine();

        System.out.print("Seat number: ");
        String seat = sc.nextLine();

        System.out.print("Boarding stop (A/B/C/D): ");
        String from = sc.nextLine();

        System.out.print("Dropping stop (A/B/C/D): ");
        String to = sc.nextLine();

        int boarding = findStop(from);
        int dropping = findStop(to);

        if (boarding < 0 || dropping < 0 || boarding >= dropping) {
            System.out.println("Invalid boarding or dropping points.");
            return;
        }

        if (!isSeatAvailable(seat, boarding, dropping)) {
            System.out.println("Seat unavailable for these stages.");
            return;
        }

        int fare = (dropping - boarding) * farePerStage;

        bookings.add(new Booking(name, seat, boarding, dropping));

        System.out.println("\nBooking successful!");
        System.out.println("Passenger: " + name);
        System.out.println("Seat: " + seat);
        System.out.println("Route: " + from.toUpperCase() + " to " + to.toUpperCase());
        System.out.println("Fare: Rs. " + fare);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== BUS RESERVATION SYSTEM ===");
            System.out.println("1. View route");
            System.out.println("2. Book seat");
            System.out.println("3. View bookings");
            System.out.println("4. Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("Route: A -> B -> C -> D");
                    System.out.println("Fare per stage: Rs. " + farePerStage);
                    break;

                case "2":
                    bookSeat(sc);
                    break;

                case "3":
                    for (Booking b : bookings) {
                        System.out.println(
                            b.passenger + " | Seat " + b.seat + " | "
                            + stops[b.boarding] + " to " + stops[b.dropping]
                        );
                    }
                    if (bookings.isEmpty()) {
                        System.out.println("No bookings yet.");
                    }
                    break;

                case "4":
                    System.out.println("Exiting system.");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
