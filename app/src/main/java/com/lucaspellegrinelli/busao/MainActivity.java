package com.lucaspellegrinelli.busao;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.maps.model.LatLng;
import com.lucaspellegrinelli.busao.buslightincidence.BusLightIncidence;
import com.lucaspellegrinelli.busao.buslightincidence.TravelPath;
import com.lucaspellegrinelli.busao.buslightincidence.geo.LatLngHelper;
import com.lucaspellegrinelli.busao.buslightincidence.geo.SPA;
import com.lucaspellegrinelli.busao.googleapi.PostLatLngRequest;
import com.lucaspellegrinelli.busao.googleapi.PostRouteRequest;
import com.lucaspellegrinelli.busao.googleapi.RequestLatLng;
import com.lucaspellegrinelli.busao.googleapi.RequestRoute;

import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity {

    int configurationStep = 0;
    MapsFragment mapsFragment;

    Date chosenTime;

    boolean isBackButtonEnabled = false;
    boolean isForwardButtonFinal = false;
    boolean isForwardButtonEnabled = false;

    AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openMapFragment();

        setupLoadingDialog();

        GregorianCalendar gc = new GregorianCalendar();
        Date date = new Date();
        gc.setTime(new Date((long)(date.getTime() + 3600000 * 7)));
        double azimuthAngle = SPA.calculateSolarPosition(gc,
                -15.379543,
                -50.449219,
                500.0, 68.0).getAzimuth();

        double angleBetweenPathPoints = LatLngHelper.angleBetweenCoordinates(new LatLng(-15.379543, -50.449219), new LatLng(-14.379543, -51.449219));

        double relativeAngleToSun = ((azimuthAngle % 90.0) - (angleBetweenPathPoints % 90.0)) * (Math.abs(angleBetweenPathPoints - azimuthAngle) >= 180 ? -1 : 1);

        Log.e("Azimuth", azimuthAngle + "°");
        Log.e("Angle", angleBetweenPathPoints + "°");
        Log.e("Relative test", relativeAngleToSun + "°");
    }

    private void openMapFragment(){
        mapsFragment = new MapsFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.mapHolder, mapsFragment).commit();

        changeRequest(0);
        setStripEnabled(0);

        LinearLayout forwardButton = (LinearLayout)findViewById(R.id.forwardStep);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isForwardButtonFinal){
                    callResultFragment();
                }else {
                    if(isForwardButtonEnabled) {
                        configurationStep++;
                        changeRequest(configurationStep);
                    }
                }
            }
        });

        LinearLayout backButton = (LinearLayout)findViewById(R.id.backStep);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBackButtonEnabled) {
                    configurationStep--;
                    changeRequest(configurationStep);
                }
            }
        });

        LinearLayout searchButton = (LinearLayout)findViewById(R.id.searchPlaceButton);
        final EditText placeInput = (EditText)findViewById(R.id.searchPlaceInput);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String placeName = placeInput.getText().toString();
                findLocationAndZoomThere(placeName);
            }
        });
    }

    private void resetEverything(){
        changeRequest(1);
        changeRequest(0);
        configurationStep = 0;

        mapsFragment.removeStartMarker();
        mapsFragment.resetStartEndLocation();
    }

    private void enableLoading(boolean enabled){
        if(enabled && !loadingDialog.isShowing()){
            loadingDialog.show();
        }else if(!enabled && loadingDialog.isShowing()){
            loadingDialog.dismiss();
        }
    }

    private void setupLoadingDialog(){
        final AlertDialog.Builder loadingAlertDialog = new AlertDialog.Builder(this);

        final View loadingView = getLayoutInflater().inflate(R.layout.alrt_dialg_loading, null);
        ProgressBar loading = (ProgressBar)loadingView.findViewById(R.id.loadingProgressBar);
        int loadingIconColor = getResources().getColor(R.color.loadingIconColor);
        loading.getIndeterminateDrawable().setColorFilter(loadingIconColor, PorterDuff.Mode.MULTIPLY);

        loadingAlertDialog.setView(loadingView);
        loadingAlertDialog.setCancelable(false);
        loadingDialog = loadingAlertDialog.create();
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    private void findLocationAndZoomThere(String locationName){
        if(locationName.isEmpty()){
            String title = getResources().getString(R.string.searchPlaceFailedTitle);
            String content = getResources().getString(R.string.searchPlaceEmptyContent);
            createSimpleAlertDialog(title, content);
            return;
        }

        enableLoading(true);
        RequestLatLng.requestLatLng(locationName, new PostLatLngRequest() {
            @Override
            public void postLocation(LatLng location) {
                if(location != null) {
                    mapsFragment.zoomToLocation(location);
                }else{
                    String title = getResources().getString(R.string.searchPlaceFailedTitle);
                    String content = getResources().getString(R.string.searchPlaceFailedContent);
                    createSimpleAlertDialog(title, content);
                }
                enableLoading(false);
            }
        });
    }

    private void changeRequest(int i){
        if(configurationStep > 3)
            configurationStep = 3;

        if(configurationStep < 0)
            configurationStep = 0;

        if(i == 0){
            mapsFragment.startLocationRequest();
            setStripEnabled(i);
            setBackButtonState(false);
            setForwardButtonFinalState(false);
            setForwardButtonEnabledState(mapsFragment.startLocationIsSet());
        }else if(i == 1){
            if(mapsFragment.startLocationIsSet()) {
                mapsFragment.endLocationRequest();
                setStripEnabled(i);
                setForwardButtonFinalState(false);
                setBackButtonState(true);
                setForwardButtonEnabledState(mapsFragment.endLocationIsSet());
            }else{
                configurationStep--;
            }
        }else if(i == 2){
            if(mapsFragment.endLocationIsSet()) {
                showChooseTime();
                mapsFragment.timeRequest();
                setStripEnabled(i);
                setForwardButtonFinalState(false);
                setBackButtonState(true);
            }else{
                configurationStep--;
            }
        }else if(i == 3){
            setStripEnabled(i);
            setBackButtonState(true);
            setForwardButtonFinalState(true);
        }
    }

    private void callResultFragment(){
        enableLoading(true);
        final Date finalDefinedDate = chosenTime;
        LatLng finalStartPosition = mapsFragment.getStartLocation();
        LatLng finalEndPosition = mapsFragment.getEndLocation();

        RequestRoute.requestRoute(finalStartPosition, finalEndPosition, new PostRouteRequest() {
            @Override
            public void postRoute(TravelPath route, long durationInSeconds) {
                if(durationInSeconds != -1) {
                    showResultDialog(route, durationInSeconds, finalDefinedDate);
                }else{
                    String title = getResources().getString(R.string.findRouteFailedTitle);
                    String content = getResources().getString(R.string.findRouteFailedContent);
                    createSimpleAlertDialog(title, content);
                }
                enableLoading(false);
            }
        });

        /*Bundle bundle = new Bundle();
        bundle.putDouble("startLatitude", finalStartPosition.latitude);
        bundle.putDouble("startLongitude", finalStartPosition.longitude);
        bundle.putDouble("endLatitude", finalEndPosition.latitude);
        bundle.putDouble("endLongitude", finalEndPosition.longitude);
        bundle.putLong("tripTime", finalDefinedDate.getTime());
        ShowResultsFragment showResultsFragment = new ShowResultsFragment();
        showResultsFragment.setArguments(bundle);*/


    }

    public void replaceFragmentWithAnimation(android.support.v4.app.Fragment fragment, String tag){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        transaction.replace(R.id.fullScreen, fragment);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    private void setBackButtonState(boolean enabled){
        isBackButtonEnabled = enabled;
        LinearLayout backButton = (LinearLayout)findViewById(R.id.backStep);
        int enabledColor = R.color.backStepTripConfigBackground;
        int disabledColor = R.color.backStepTripConfigBackgroundDisabled;

        backButton.setBackground(new ColorDrawable(getResources().getColor(enabled ? enabledColor : disabledColor)));

        int enabledTextColor = R.color.plainWhite;
        int disabledTextColor = R.color.stepTripConfigTextDisabled;
        TextView backText = (TextView)backButton.findViewById(R.id.chooseSettingsStepBackText);
        backText.setTextColor(getResources().getColor(enabled ? enabledTextColor : disabledTextColor));
    }

    public void setForwardButtonEnabledState(boolean enabled){
        isForwardButtonEnabled = enabled;
        LinearLayout forwardButton = (LinearLayout)findViewById(R.id.forwardStep);
        int enabledColor = R.color.forwardStepTripConfigBackground;
        int disabledColor = R.color.forwardStepTripConfigBackgroundDisabled;

        forwardButton.setBackground(new ColorDrawable(getResources().getColor(enabled ? enabledColor : disabledColor)));

        int enabledTextColor = R.color.plainWhite;
        int disabledTextColor = R.color.stepTripConfigTextDisabled;
        TextView forwardText = (TextView)forwardButton.findViewById(R.id.chooseSettingsStepForwardText);
        forwardText.setTextColor(getResources().getColor(enabled ? enabledTextColor : disabledTextColor));
    }

    private void setForwardButtonFinalState(boolean finalState){
        isForwardButtonFinal = finalState;
        LinearLayout forwardButton = (LinearLayout)findViewById(R.id.forwardStep);
        int normalColor = R.color.forwardStepTripConfigBackground;
        int confirmColor = R.color.forwardStepTripConfigBackgroundConfirmAction;

        String normalText = getResources().getString(R.string.chooseSettingsStepForward);
        String confirmText = getResources().getString(R.string.chooseSettingsStepForwardFinalState);

        if(finalState){
            forwardButton.setBackground(new ColorDrawable(getResources().getColor(confirmColor)));
            TextView text = (TextView)forwardButton.findViewById(R.id.chooseSettingsStepForwardText);
            text.setText(confirmText);
        }else{
            forwardButton.setBackground(new ColorDrawable(getResources().getColor(normalColor)));
            TextView text = (TextView)forwardButton.findViewById(R.id.chooseSettingsStepForwardText);
            text.setText(normalText);
        }
    }

    private void showResultDialog(TravelPath travelPath, long duration, Date date){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        final View view = getLayoutInflater().inflate(R.layout.alrt_dialg_result, null);

        Button closeButton  = (Button)view.findViewById(R.id.result_button_option_1);

        ImageView imageView = (ImageView)view.findViewById(R.id.resultImage);

        BusLightIncidence b = new BusLightIncidence(travelPath, duration, date);

        Bitmap result = getBusImage(b.getBusIncidence().getIncidencesPercentages());

        imageView.setImageDrawable(new BitmapDrawable(getResources(), result));

        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        final AlertDialog dialog = alertDialog.create();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetEverything();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void createSimpleAlertDialog(String title, String text){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.alrt_dialg_simple_message, null);
        TextView titleTV = (TextView)view.findViewById(R.id.title_to_show);
        TextView messageTV = (TextView)view.findViewById(R.id.message_to_show);

        titleTV.setText(title);
        messageTV.setText(text);

        alertDialog.setView(view);
        final AlertDialog dialog = alertDialog.create();

        Button close = (Button) view.findViewById(R.id.close_dialog);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showChooseTime(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        final View view = getLayoutInflater().inflate(R.layout.alrt_dialg_choose_time, null);

        Button okButton = (Button)view.findViewById(R.id.button_option_2);
        Button cancelButton  = (Button)view.findViewById(R.id.button_option_1);

        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        final AlertDialog dialog = alertDialog.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurationStep--;
                changeRequest(configurationStep);
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePicker timePicker = (TimePicker)view.findViewById(R.id.timePicker);

                Date date = new Date();
                date.setHours(timePicker.getHour());
                date.setMinutes(timePicker.getMinute());

                chosenTime = date;
                dialog.dismiss();

                configurationStep++;
                changeRequest(configurationStep);
            }
        });


        dialog.show();
    }

    private void setStripEnabled(int stripIndex){
        int[] strips = {R.id.chooseFromStrip, R.id.chooseDestStrip, R.id.chooseTimeStrip};

        int[] stripsEnabledColor = {R.color.chooseFromLocationStipBackgroundEnabled,
                R.color.chooseDestLocationStipBackgroundEnabled,
                R.color.chooseTimeStipBackgroundEnabled};

        int stripsDisabledColor = R.color.stipBackgroundDisabled;

        int[] imageViewStrips = {R.id.chooseFromStripImage, R.id.chooseDestStripImage, R.id.chooseTimedStripImage};
        int[] imageStrips = {R.drawable.ic_location_on_white_48dp, R.drawable.ic_near_me_white_48dp, R.drawable.ic_access_time_white_48dp};
        int doneImage = R.drawable.ic_done_white_48dp;
        int enabledColor = Color.parseColor("#ffffff");
        int disabledColor = Color.parseColor("#7f8c8d");

        for(int i = 0; i < strips.length; i++){
            LinearLayout strip = (LinearLayout)findViewById(strips[i]);
            int color = (i == stripIndex || stripIndex == strips.length) ? stripsEnabledColor[i] : stripsDisabledColor;
            strip.setBackground(new ColorDrawable(getResources().getColor(color)));

            ImageView image = (ImageView)findViewById(imageViewStrips[i]);

            if(i < stripIndex){
                Drawable mDrawable = getResources().getDrawable(doneImage);
                image.setImageDrawable(mDrawable);
                image.setColorFilter(enabledColor);
            }else if(i == stripIndex){
                Drawable mDrawable = getResources().getDrawable(imageStrips[i]);
                image.setImageDrawable(mDrawable);
                image.setColorFilter(enabledColor);
            }else{
                Drawable mDrawable = getResources().getDrawable(imageStrips[i]);
                image.setImageDrawable(mDrawable);
                image.setColorFilter(disabledColor);
            }
        }
    }

    private Bitmap getBusImage(double[][] incidences){
        Bitmap busBase = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.busao));

        Bitmap fullImage = Bitmap.createBitmap(busBase.getWidth() + 1350, busBase.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas fullImageCanvas = new Canvas(fullImage);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        fullImageCanvas.drawBitmap(busBase, fullImage.getWidth() / 2 - busBase.getWidth() / 2, 0, paint);

        int xOffset = (fullImage.getWidth() - busBase.getWidth()) / 2;
        int seatsWidth = 109 * 2;
        int[] seatsYPositions = {/*534, */1254/*, 1974*/};
        int[] seatsXPositions = {xOffset + 94, xOffset + 94 + seatsWidth, xOffset + 744, xOffset + 744 + seatsWidth};

        int textBoxHeight = 200;
        int spaceBetweenBorders = 50;
        int[] percentageYPositions = {/*534 + textBoxHeight, */1254 + textBoxHeight/*, 1974 + textBoxHeight*/};
        int[] percentageXPositions = {spaceBetweenBorders, 300 + spaceBetweenBorders, fullImage.getWidth() - 600 - spaceBetweenBorders, fullImage.getWidth() - 300 - spaceBetweenBorders};

        for(int i = 0; i < seatsXPositions.length; i++) {
            for(int j = 0; j < seatsYPositions.length; j++) {
                Bitmap image = getHueImage((double)Math.round(incidences[i][j] * 100.0) / 100.0);
                String incidenceText = Math.round(incidences[i][j] * 100.0) + "%";

                int textBackgroundColor = Color.HSVToColor(new float[]{90f - (float)incidences[i][j] * 90f, 0.74f, 0.80f});
                int textColor = Color.rgb(255, 255, 255);
                Bitmap textBitmap = drawTextBitmap(incidenceText, textBackgroundColor, textColor);
                fullImageCanvas.drawBitmap(image, seatsXPositions[i], seatsYPositions[j], paint);
                fullImageCanvas.drawBitmap(textBitmap, percentageXPositions[i], percentageYPositions[j], paint);
            }
        }

        return getResizedBitmap(fullImage, (int)(fullImage.getWidth() * 0.25), (int)(fullImage.getHeight() * 0.25));
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public Bitmap getHueImage (double percentage) {
        percentage = 1 - percentage;
        Bitmap seats = null;

        if(percentage >= 0.0 && percentage <= 0.1){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_0));
        }else if(percentage > 0.1 && percentage <= 0.2){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_10));
        }else if(percentage > 0.2 && percentage <= 0.3){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_20));
        }else if(percentage > 0.3 && percentage <= 0.4){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_30));
        }else if(percentage > 0.4 && percentage <= 0.5){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_40));
        }else if(percentage > 0.5 && percentage <= 0.6){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_50));
        }else if(percentage > 0.6 && percentage <= 0.7){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_60));
        }else if(percentage > 0.7 && percentage <= 0.8){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_70));
        }else if(percentage > 0.8 && percentage <= 0.9){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_80));
        }else if(percentage > 0.9 && percentage <= 1){
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_90));
        }else{
            seats = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.seats_hue_0));
        }

        return seats.copy(Bitmap.Config.ARGB_8888, true);
    }

    public Bitmap drawTextBitmap(String gText, int backgroundColor, int textColor) {
        /* CREATE BOX */
        final int BOX_WIDTH = 300;
        final int BOX_HEIGHT = 250;

        Bitmap boxBitmap = Bitmap.createBitmap(BOX_WIDTH, BOX_HEIGHT, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(boxBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(backgroundColor);
        canvas.drawRect(0F, 0F, (float) BOX_WIDTH, (float) BOX_HEIGHT, paint);

        float scale = getResources().getDisplayMetrics().density;

        /* CREATE TEXT */
        final int FONT_SIZE = 60;

        paint.setColor(textColor);
        paint.setTextSize((int) (FONT_SIZE * scale));

        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (boxBitmap.getWidth() - bounds.width())/2;
        int y = (boxBitmap.getHeight() + bounds.height())/2;

        canvas.drawText(gText, x, y, paint);

        return boxBitmap;
    }
}
