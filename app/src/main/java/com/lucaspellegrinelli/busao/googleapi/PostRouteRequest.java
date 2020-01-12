package com.lucaspellegrinelli.busao.googleapi;

import com.lucaspellegrinelli.busao.buslightincidence.TravelPath;

/**
 * Created by lucas on 10-Feb-17.
 */

public interface PostRouteRequest {
    void postRoute(TravelPath route, long durationInSeconds);
}
