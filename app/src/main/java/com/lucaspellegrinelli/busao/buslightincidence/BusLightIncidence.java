package com.lucaspellegrinelli.busao.buslightincidence;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.lucaspellegrinelli.busao.buslightincidence.geo.LatLngHelper;
import com.lucaspellegrinelli.busao.buslightincidence.geo.SPA;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by lucas on 09-Feb-17.
 */

public class BusLightIncidence {

    private TravelPath path;
    private long durationInSeconds;
    private Date startTime;

    final double MIN_ZENITH_TO_AFFECT_ONLY_WINDOW_SEAT = 70;

    final double MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_ROW = 45;
    final double MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_AND_SEC_ROW = 22;

    public BusLightIncidence(TravelPath path, long durationInSeconds, Date startTime){
        this.path = path;
        this.durationInSeconds = durationInSeconds;
        this.startTime = startTime;
    }

    public Bus getBusIncidence(){
        Bus finalBus = new Bus();

        double distanceTraveled = 0;
        double secondsInEachKm = durationInSeconds / path.getFullPathLength();

        for(int i = 1; i < path.getNumberOfSteps(); i++){
            LatLng previousCheckMark = path.getPointInPath(i - 1);
            LatLng currentCheckMark = path.getPointInPath(i);

            double distanceBetweenInKm = LatLngHelper.getDistanceBetweenCoordinates(previousCheckMark, currentCheckMark);
            distanceTraveled += distanceBetweenInKm;

            GregorianCalendar g = getGregorianCalendarInTraveledPosition(distanceTraveled, path.getFullPathLength());

            GregorianCalendar[] sunriseAndSet = getSunriseAndSunsetDate(g, currentCheckMark);
            GregorianCalendar sunrise = sunriseAndSet[0];
            GregorianCalendar sunset = sunriseAndSet[1];

            double azimuthAngle = getAzimuthAngleAtDate(g, currentCheckMark);

            //double angleBetweenSouth = angleFromCoordinate(currentCheckMark, MAGNETIC_SOUTH_COORDINATES);
            double angleBetweenPathPoints = LatLngHelper.angleBetweenCoordinates(previousCheckMark, currentCheckMark);

            double relativeAngleToSun = angleBetweenPathPoints - azimuthAngle;
            if(relativeAngleToSun > 180)
                relativeAngleToSun %= 180;
            else if(relativeAngleToSun < -180)
                relativeAngleToSun = -(Math.abs(relativeAngleToSun) % 180);

            double elevation = 90.0 - getSunElevationAtDate(g, currentCheckMark);

            //Log.e("Azimuth", ""+azimuthAngle + "     " + g.getTime().toString());
			/*System.out.println("Real angle: " + angleBetweenPathPoints);
			System.out.println("Realative: " + relativeAngleToSun);
			System.out.println("Azimuth: " + azimuthAngle);
			System.out.println();*/

            long secondsInSubPath = (long)(secondsInEachKm * distanceBetweenInKm);
            boolean goingDown = previousCheckMark.latitude < currentCheckMark.latitude;

            double[][] incidence = getSectionsConditionsRelativeToTime(relativeAngleToSun, goingDown, secondsInSubPath, elevation, azimuthAngle, angleBetweenPathPoints);

            if(g.after(sunrise) && g.before(sunset)){
                finalBus.addIncidence(incidence);
            }
        }

        return finalBus;
    }

    private GregorianCalendar getGregorianCalendarInTraveledPosition(double distanceTraveled, double fullTravelDistanceInKm){
        double travelCompletedPercentage = distanceTraveled / fullTravelDistanceInKm;
        long timeSpentInMilisseconds = (long)(((double)durationInSeconds) * travelCompletedPercentage) * 1000;

        Date currentDateTime = new Date(startTime.getTime() + timeSpentInMilisseconds);

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(currentDateTime);

        return calendar;
    }

    private GregorianCalendar[] getSunriseAndSunsetDate(GregorianCalendar calendar, LatLng location){
        GregorianCalendar[] date = SPA.calculateSunriseTransitSet(calendar,
                location.latitude,
                location.longitude,
                68.0);

        return new GregorianCalendar[]{date[0], date[2]};
    }

    private double getAzimuthAngleAtDate(GregorianCalendar calendar, LatLng location){
        double azimuthAngle = SPA.calculateSolarPosition(calendar,
                location.latitude,
                location.longitude,
                500.0, 68.0).getAzimuth();

        return azimuthAngle;
    }

    private double getSunElevationAtDate(GregorianCalendar calendar, LatLng location){
        double elevation = SPA.calculateSolarPosition(calendar,
                location.latitude,
                location.longitude,
                500.0, 68.0).getZenithAngle();

        return elevation;
    }

    private double[][] getSectionsConditionsRelativeToTime(double relativeAngleToSun, boolean goingDown, long secondsInEachSubPath, double elevation, double azimuth, double busAngle){
        double[][] baseIncidence = getSectionsConditionAtAngle(relativeAngleToSun, goingDown, elevation, azimuth, busAngle);
        for(int j = 0; j < baseIncidence.length; j++){
            for(int k = 0; k < baseIncidence[0].length; k++){
                baseIncidence[j][k] *= secondsInEachSubPath;
            }
        }

        return baseIncidence;
    }

    private double[][] getSectionsConditionAtAngle(double angleRelativeToSun, boolean goingDown, double elevation, double azimuth, double busAngle){
        double[][] areas = new double[Bus.SIDE_COUNT][Bus.ROW_ABSTRACT_COUNT];

        double oppositeAzimuth = azimuth <= 180 ? azimuth + 180 : azimuth - 180;

        int side = 0;

        if(azimuth > 180) {
            if (busAngle > azimuth || busAngle < oppositeAzimuth) {
                side = 1;
            } else {
                side = -1;
            }
        }else {
            if (busAngle > azimuth && busAngle < oppositeAzimuth) {
                side = 1;
            } else {
                side = -1;
            }
        }

        //Log.e("Teste", "Azimuth: " + azimuth + "°   Bus Angle: " + busAngle + "°   Relative: " + angleRelativeToSun + "°     Right? " + (side == 1));

        side *= (goingDown ? 1 : -1);

        List<Integer> affectsColumns = new ArrayList<>();

        if(side == -1) {
            if(elevation < MIN_ZENITH_TO_AFFECT_ONLY_WINDOW_SEAT) {
                affectsColumns.add(0);
                affectsColumns.add(1);
            }else{
                affectsColumns.add(0);
            }
        }else{
            if(elevation < MIN_ZENITH_TO_AFFECT_ONLY_WINDOW_SEAT) {
                affectsColumns.add(3);
                affectsColumns.add(2);
            }else{
                affectsColumns.add(3);
            }
        }
        // side = 1 = direito
        for(int i = 0; i < areas[0].length; i++){
            for(int affectedColumn : affectsColumns) {
                /*if(elevation <= MIN_ZENITH_TO_AFFECT_OTHER_SIDE){
                    if((Math.abs(azimuth - busAngle) % 270) <= 90){ // Sol batendo de frente
                        if(Math.abs(angleRelativeToSun) <= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_AND_SEC_ROW
                                || Math.abs(angleRelativeToSun) >= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_AND_SEC_ROW){
                            if(i == 0 || i == 1) continue;
                        }else if(Math.abs(angleRelativeToSun) <= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_ROW
                                || Math.abs(angleRelativeToSun) >= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_ROW){
                            if(i == 0) continue;
                        }
                    }else { // Sol batendo de tras
                        if(Math.abs(angleRelativeToSun) <= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_AND_SEC_ROW
                                || Math.abs(angleRelativeToSun) >= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_AND_SEC_ROW){
                            if(i == 1 || i == 2) continue;
                        }else if(Math.abs(angleRelativeToSun) <= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_ROW
                                || Math.abs(angleRelativeToSun) >= MIN_REL_ANGLE_TO_NOT_AFFECT_FIRST_ROW){
                            if(i == 2) continue;
                        }
                    }
                }*/
                areas[affectedColumn][i] = 1.0;
            }
        }

        return areas;
    }
}
