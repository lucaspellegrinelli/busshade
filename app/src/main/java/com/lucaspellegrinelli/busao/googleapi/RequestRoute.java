package com.lucaspellegrinelli.busao.googleapi;

import com.google.android.gms.maps.model.LatLng;
import com.lucaspellegrinelli.busao.buslightincidence.TravelPath;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucas on 10-Feb-17.
 */

public class RequestRoute {
    public static void requestRoute(LatLng from, LatLng to, final PostRouteRequest postRouteRequest){
        String routeUrl = makeRouteURL(from, to);

        WebsiteRequest routeRequest = new WebsiteRequest(routeUrl, new PostExecute() {
            @Override
            public void postAction(JSONObject json) {
                try {
                    String encodedString = interpretJsonToGetEncodedPoly(json);
                    String duration = interpretJsonToGetDuration(json);

                    TravelPath travelPath = new TravelPath(decodePoly(encodedString));
                    long tripDuration = Long.parseLong(duration);

                    postRouteRequest.postRoute(travelPath, tripDuration);
                }catch(Exception e){
                    e.printStackTrace();
                    postRouteRequest.postRoute(null, -1);
                }
            }
        });
        routeRequest.execute();
    }

    private static String interpretJsonToGetEncodedPoly(JSONObject json){
        try {
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            return overviewPolylines.getString("points");
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static String interpretJsonToGetDuration(JSONObject json){
        try {
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject leg = routes.getJSONArray("legs").getJSONObject(0);
            JSONObject duration = leg.getJSONObject("duration");
            return duration.getString("value");
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    private static String makeRouteURL(LatLng from, LatLng to){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");
        urlString.append(from.latitude);
        urlString.append(",");
        urlString.append(from.longitude);
        urlString.append("&destination=");
        urlString.append(to.latitude);
        urlString.append(",");
        urlString.append(to.longitude);
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        return urlString.toString();
    }
}
