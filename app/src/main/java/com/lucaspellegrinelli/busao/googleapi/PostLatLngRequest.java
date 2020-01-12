package com.lucaspellegrinelli.busao.googleapi;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by lucas on 15-Feb-17.
 */

public interface PostLatLngRequest {
    void postLocation(LatLng location);
}
