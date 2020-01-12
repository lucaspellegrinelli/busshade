package com.lucaspellegrinelli.busao;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class ShowResultsFragment extends Fragment {

    private Date tripDate;
    private LatLng fromLocation;
    private LatLng toLocation;

    public ShowResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fromLocation = new LatLng(getArguments().getDouble("startLatitude"), getArguments().getDouble("startLongitude"));
        toLocation = new LatLng(getArguments().getDouble("endLatitude"), getArguments().getDouble("endLongitude"));
        tripDate = new Date(getArguments().getLong("tripTime"));

        return inflater.inflate(R.layout.fragment_show_results, container, false);
    }

}
