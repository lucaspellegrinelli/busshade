package com.lucaspellegrinelli.busao.buslightincidence.geo;

import com.google.android.gms.maps.model.LatLng;

/**
 * 
 * @author Lucas Pellegrinelli
 *
 */
public class LatLngHelper {
	private static double bearingInRadians(LatLng coord1, LatLng coord2) {

		double phi1 = Math.toRadians(coord1.latitude);
		double phi2 = Math.toRadians(coord2.latitude);
		double lambda1 = Math.toRadians(coord1.longitude);
		double lambda2 = Math.toRadians(coord2.longitude);
		
		double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
		
		double x = Math.cos(phi1) * Math.sin(phi2) -
		        Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
		
		return Math.toDegrees(Math.atan2(y, x));
	}
	
	public static double angleBetweenCoordinates(LatLng src, LatLng dst) {
	    double bearing = bearingInRadians(src, dst);

		if((bearing <= 0 && bearing >= -180) || bearing == 180){
			bearing = Math.abs(bearing) + 90;
		}else if(bearing < 180 && bearing >= 90){
			bearing = 360.0 - (bearing - 90.0);
		}else{
			bearing = Math.abs(bearing - 90);
		}

		return bearing;
	}
	
	public static double getDistanceBetweenCoordinates(LatLng coord1, LatLng coord2) {
		final double R = 6371.0; // Radius of the earth in km
		  
		double dLat = Math.toRadians(coord2.latitude - coord1.latitude);  // deg2rad below
		double dLon = Math.toRadians(coord2.longitude - coord1.longitude);
		  
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
			Math.cos(Math.toRadians(coord1.latitude)) * Math.cos(Math.toRadians(coord2.latitude)) *
			Math.sin(dLon/2) * Math.sin(dLon/2); 
		  
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		  
		double d = R * c; // Distance in km
		
		return d;
	}
}
