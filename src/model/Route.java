
package model;

import java.util.ArrayList;
import java.util.List;

public class Route {

    private String routeId;
    private String routeName;
    private List<Stop> stops;
    private List<Double> stageFares;

    public Route(String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.stops = new ArrayList<>();
        this.stageFares = new ArrayList<>();
    }

    public void addStop(Stop stop, double fareFromPrevious) {

        if (stop == null) {
            throw new IllegalArgumentException("Stop cannot be null");
        }

        if (fareFromPrevious < 0) {
            throw new IllegalArgumentException("Fare cannot be negative");
        }

        if (!stops.isEmpty()) {
            stageFares.add(fareFromPrevious);
        }

        stops.add(stop);
    }

    public int findStopIndex(String stopName) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getName().equalsIgnoreCase(stopName)
                    || (stops.get(i).getStopId() != null
                    && stops.get(i).getStopId().equalsIgnoreCase(stopName))) {
                return i;
            }
        }
        return -1;
    }

    public double calculateStageFare(int boarding, int dropping) {

        if (boarding < 0 || dropping >= stops.size()
                || boarding >= dropping) {
            throw new IllegalArgumentException("Invalid journey stages");
        }

        double total = 0;

        for (int i = boarding; i < dropping; i++) {
            total += stageFares.get(i);
        }

        return total;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public List<Stop> getStops() {
        return new ArrayList<>(stops);
    }

    public List<Double> getStageFares() {
        return new ArrayList<>(stageFares);
    }

    public int getStopCount() {
        return stops.size();
    }

    public Stop getStop(int index) {
        return stops.get(index);
    }

    @Override
    public String toString() {
        return routeId + " - " + routeName + " - " + stops;
    }
}
