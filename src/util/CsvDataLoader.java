
package util;

import model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CsvDataLoader {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Data {
        public Map<String, Route> routes = new LinkedHashMap<>();
        public Map<String, Bus> buses = new LinkedHashMap<>();
        public Map<String, Trip> trips = new LinkedHashMap<>();
        public Map<String, Partner> partners = new LinkedHashMap<>();
    }

    private List<String[]> readCsv(String file) throws IOException {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader =
                     Files.newBufferedReader(Path.of(file))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                if (!line.isBlank()) {
                    rows.add(parseCsvLine(line));
                }
            }
        }

        return rows;
    }


    // Handles quoted commas, escaped double-quotes and empty cells in a CSV row.
    private String[] parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        value.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    value.append(c);
                }
            } else if (c == '"' && value.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                fields.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        if (quoted) throw new IOException("Malformed CSV: unclosed quoted field");
        fields.add(value.toString());
        return fields.toArray(new String[0]);
    }

    public Data loadAll(String folder) throws IOException {

        Data data = new Data();

        String stopsFile = folder + "/stops.csv";
        String routesFile = folder + "/route_stops.csv";
        String layoutsFile = folder + "/layout_seats.csv";
        String busesFile = folder + "/buses.csv";
        String tripsFile = folder + "/trips.csv";
        String partnersFile = folder + "/partners.csv";

        Map<String, Stop> stops = new HashMap<>();

        for (String[] r : readCsv(stopsFile)) {
            Stop stop = new Stop(
                    r[0].trim(),
                    r[1].trim(),
                    r[2].trim(),
                    0
            );
            stops.put(r[0].trim(), stop);
        }

        Map<String, List<String[]>> routeRows = new LinkedHashMap<>();

        for (String[] r : readCsv(routesFile)) {
            routeRows.computeIfAbsent(
                    r[0].trim(), k -> new ArrayList<>()
            ).add(r);
        }

        for (Map.Entry<String, List<String[]>> entry
                : routeRows.entrySet()) {

            List<String[]> rows = entry.getValue();

            rows.sort(Comparator.comparingInt(
                    r -> Integer.parseInt(r[2].trim())
            ));

            Route route = new Route(
                    entry.getKey(),
                    rows.get(0)[1].trim()
            );

            for (String[] r : rows) {
                Stop original = stops.get(r[3].trim());

                if (original == null) {
                    throw new IOException("Unknown stop: " + r[3]);
                }

                Stop routeStop = new Stop(
                        original.getStopId(),
                        original.getName(),
                        original.getCity(),
                        Integer.parseInt(r[4].trim())
                );

                route.addStop(
                        routeStop,
                        Double.parseDouble(r[5].trim())
                );
            }

            data.routes.put(route.getRouteId(), route);
        }

        Map<String, Layout> layouts = new LinkedHashMap<>();

        for (String[] r : readCsv(layoutsFile)) {

            String layoutId = r[0].trim();
            Layout layout = layouts.computeIfAbsent(
                    layoutId, Layout::new
            );

            Seat.SeatType type;

            switch (r[3].trim().toUpperCase()) {
                case "SEATER":
                    type = Seat.SeatType.SEATER;
                    break;
                case "SLEEPER_LOWER":
                    type = Seat.SeatType.LOWER_SLEEPER;
                    break;
                case "SLEEPER_UPPER":
                    type = Seat.SeatType.UPPER_SLEEPER;
                    break;
                case "SLEEPER_SINGLE":
                    type = Seat.SeatType.SINGLE_SLEEPER;
                    break;
                default:
                    throw new IOException("Unknown seat type: " + r[3]);
            }

            String pair = r[4].trim().isEmpty() ? null : r[4].trim();

            Seat seat = new Seat(
                    r[1].trim(),
                    type,
                    Boolean.parseBoolean(r[5].trim()),
                    pair,
                    r[2].trim()
            );

            layout.addSeat(seat);
        }

        for (String[] r : readCsv(busesFile)) {

            String busId = r[0].trim();
            String layoutId = r[1].trim();

            Layout layout = layouts.get(layoutId);

            if (layout == null) {
                throw new IOException("Unknown layout: " + layoutId);
            }

            Bus bus = new Bus(
                    busId,
                    layoutId,
                    r[2].trim(),
                    Boolean.parseBoolean(r[3].trim())
            );

            for (Seat seat : layout.getSeats()) {
                bus.addSeat(new Seat(
                        seat.getSeatId(),
                        seat.getSeatType(),
                        seat.isLadiesOnly(),
                        seat.getAdjacentSeatId(),
                        seat.getDeck()
                ));
            }

            data.buses.put(busId, bus);
        }

        for (String[] r : readCsv(tripsFile)) {

            Route route = data.routes.get(r[1].trim());
            Bus bus = data.buses.get(r[2].trim());

            if (route == null || bus == null) {
                throw new IOException("Invalid route or bus in trip " + r[0]);
            }

            LocalDateTime departure =
                    LocalDateTime.parse(r[4].trim(), DATE_TIME);

            Trip trip = new Trip(
                    r[0].trim(),
                    route,
                    bus,
                    departure
            );

            data.trips.put(trip.getTripId(), trip);
        }

        for (String[] r : readCsv(partnersFile)) {

            Partner partner = new Partner(
                    r[0].trim(),
                    r[1].trim(),
                    Double.parseDouble(r[2].trim()),
                    Double.parseDouble(r[3].trim())
            );

            data.partners.put(partner.getPartnerId(), partner);
        }

        return data;
    }
}
