
import util.CsvDataLoader;

public class DataLoadTest {

    public static void main(String[] args) {

        try {
            CsvDataLoader loader = new CsvDataLoader();

            CsvDataLoader.Data data = loader.loadAll(
                "input/T062-PS-045-sample-data"
            );

            System.out.println("================================");
            System.out.println("   CSV DATA LOADING TEST");
            System.out.println("================================");

            System.out.println(
                "Routes loaded: " + data.routes.size()
            );

            System.out.println(
                "Buses loaded: " + data.buses.size()
            );

            System.out.println(
                "Trips loaded: " + data.trips.size()
            );

            System.out.println(
                "Partners loaded: " + data.partners.size()
            );

            System.out.println("\n--- ROUTES ---");

            data.routes.values().forEach(System.out::println);

            System.out.println("\n--- BUSES ---");

            data.buses.values().forEach(System.out::println);

            System.out.println("\n--- TRIPS ---");

            data.trips.values().forEach(System.out::println);

            System.out.println("\nCSV DATA LOADING COMPLETED.");

        } catch (Exception e) {

            System.out.println("DATA LOADING FAILED:");
            e.printStackTrace();
        }
    }
}
