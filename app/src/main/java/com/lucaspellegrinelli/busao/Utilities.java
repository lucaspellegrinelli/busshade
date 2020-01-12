package com.lucaspellegrinelli.busao;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

/**
 * Created by lucas on 06-May-16.
 */
public class Utilities {

    public static double distance(LatLng loc1, LatLng loc2) {
        double lat1 = loc1.latitude;
        double lon1 = loc1.longitude;
        double lat2 = loc2.latitude;
        double lon2 = loc2.longitude;

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344; // convert to km

        return Double.parseDouble(new DecimalFormat("0.00").format(dist));
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    public static Bitmap makeTintedBitmap(Bitmap src, int color) {
        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas c = new Canvas(result);
        Paint paint = new Paint();
        paint.setColorFilter(new LightingColorFilter(color,0));
        c.drawBitmap(src, 0, 0, paint);
        return result;
    }

    public static Bitmap getColoredDrawable(Context context, int drawable, int color){
        Drawable icon = context.getResources().getDrawable(drawable);
        Bitmap bitmapIcon = ((BitmapDrawable)icon).getBitmap();
        return Utilities.makeTintedBitmap(bitmapIcon, context.getResources().getColor(color));
    }
}
