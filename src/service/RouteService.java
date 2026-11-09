package service;

import model.Route;
import java.util.*;

public class RouteService {

    private final Map<String, Route> routes = new LinkedHashMap<>();

    public void addRoute(Route route) {

        if (route == null) {
            throw new IllegalArgumentException("Route cannot be empty.");
        }

        if (routes.containsKey(route.getRouteId())) {
            throw new IllegalArgumentException("Route already exists.");
        }

        routes.put(route.getRouteId(), route);
    }

    public Route getRoute(String routeId) {

        Route route = routes.get(routeId);

        if (route == null) {
            throw new IllegalArgumentException("Route not found.");
        }

        return route;
    }

    public Collection<Route> getAllRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }
}