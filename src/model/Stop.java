
package model;

import java.time.LocalTime;

public class Stop {

    private String stopId;
    private String name;
    private String city;
    private int offsetMinutes;
    private LocalTime arrivalTime;

    // Backward-compatible constructor
    public Stop(String name, LocalTime arrivalTime) {
        this(null, name, null, 0, arrivalTime);
    }

    public Stop(String stopId, String name, String city,
                int offsetMinutes) {
        this(stopId, name, city, offsetMinutes, null);
    }

    public Stop(String stopId, String name, String city,
                int offsetMinutes, LocalTime arrivalTime) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stop name cannot be empty");
        }

        if (offsetMinutes < 0) {
            throw new IllegalArgumentException("Invalid stop offset");
        }

        this.stopId = stopId;
        this.name = name.trim();
        this.city = city;
        this.offsetMinutes = offsetMinutes;
        this.arrivalTime = arrivalTime;
    }

    public String getStopId() {
        return stopId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return stopId + " - " + name + " (" + city + ")";
    }
}
