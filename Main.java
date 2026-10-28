
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== BUS RESERVATION SYSTEM ===");
        System.out.println("1. View available trip");
        System.out.println("2. Book a seat");
        System.out.print("Choose an option: ");

        int choice = sc.nextInt();

        if (choice == 1) {
            System.out.println("Trip: Bengaluru -> Mysuru");
            System.out.println("Departure: 09:00");
            System.out.println("Available seats: 30");
            System.out.println("Fare: Rs. 300");
        } else if (choice == 2) {
            System.out.print("Enter passenger name: ");
            sc.nextLine();
            String name = sc.nextLine();

            System.out.print("Enter passenger age: ");
            int age = sc.nextInt();

            if (age < 0) {
                System.out.println("Invalid age.");
            } else {
                double fare = age < 5 ? 0 :
                              age <= 12 ? 225 : 300;

                System.out.println("Booking request received!");
                System.out.println("Passenger: " + name);
                System.out.println("Fare: Rs. " + fare);
                System.out.println("Note: Demo only; booking is not saved.");
            }
        } else {
            System.out.println("Invalid option.");
        }

        sc.close();
    }
}
