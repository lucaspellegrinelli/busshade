package com.lucaspellegrinelli.busao.googleapi;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

/**
 * Created by lucas on 15-Feb-17.
 */

public class RequestLatLng {
    public static void requestLatLng(String placeName, final PostLatLngRequest postLatLngRequest){
        String url = makeLatLngURL(placeName);

        WebsiteRequest routeRequest = new WebsiteRequest(url, new PostExecute() {
            @Override
            public void postAction(JSONObject json) {
                try {
                    LatLng location = interpretJSONToGetLatLng(json);
                    postLatLngRequest.postLocation(location);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        routeRequest.execute();
    }

    private static LatLng interpretJSONToGetLatLng(JSONObject json){
        try {
            JSONObject results = json.getJSONArray("results").getJSONObject(0);
            JSONObject geometry = results.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");

            double lat = Double.parseDouble(location.getString("lat"));
            double lng = Double.parseDouble(location.getString("lng"));

            return new LatLng(lat, lng);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static String makeLatLngURL(String placeName){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.google.com/maps/api/geocode/json");
        urlString.append("?address=");
        urlString.append(placeName.replace(' ', '+'));
        return urlString.toString();
    }
}
