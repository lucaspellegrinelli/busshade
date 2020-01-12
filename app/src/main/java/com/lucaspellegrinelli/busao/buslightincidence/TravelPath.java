package com.lucaspellegrinelli.busao.buslightincidence;

/**
 * Created by lucas on 09-Feb-17.
 */

import com.google.android.gms.maps.model.LatLng;
import com.lucaspellegrinelli.busao.buslightincidence.geo.LatLngHelper;

import java.util.List;

public class TravelPath {
    private List<LatLng> path;

    private double fullPathLength;

    public TravelPath(List<LatLng> path){
        this.path = path;

        for(int i = 1; i < path.size(); i++){
            LatLng previousCheckMark = path.get(i - 1);
            LatLng currentCheckMark = path.get(i);

            double distanceBetween = LatLngHelper.getDistanceBetweenCoordinates(previousCheckMark, currentCheckMark);
            fullPathLength += distanceBetween;
        }
    }

    public List<LatLng> getPath(){
        return path;
    }

    public LatLng getPointInPath(int index){
        return path.get(index);
    }

    public double getFullPathLength(){
        return fullPathLength;
    }

    public int getNumberOfSteps(){
        return path.size();
    }
}

