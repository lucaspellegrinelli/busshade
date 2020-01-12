package com.lucaspellegrinelli.busao.buslightincidence;

/**
 * Created by lucas on 09-Feb-17.
 */

import android.util.Log;

public class Bus {
    public static final int SIDE_COUNT = 4;
    public static final int ROW_ABSTRACT_COUNT = 3;

    private double[][] lightIncidence;
    private double totalIncidence;

    public Bus(){
        this.lightIncidence = new double[SIDE_COUNT][ROW_ABSTRACT_COUNT];
    }

    public void addIncidence(int side, int row, double value){
        lightIncidence[side][row] += value;
        if(side == 0 || side == SIDE_COUNT - 1)
            totalIncidence += value;
    }

    public void addIncidence(double[][] incidences){
        for(int i = 0; i < lightIncidence.length; i++){
            for(int j = 0; j < lightIncidence[0].length; j++){
                addIncidence(i, j, incidences[i][j]);
            }
        }
    }

    public double[][] getIncidencesPercentages(){
        double[][] busIncidencePercentages = new double[SIDE_COUNT][ROW_ABSTRACT_COUNT];
        for(int i = 0; i < lightIncidence.length; i++){
            for(int j = 0; j < lightIncidence[0].length; j++){
                if(totalIncidence == 0){
                    busIncidencePercentages[i][j] = 0.0;
                }else {
                    busIncidencePercentages[i][j] = lightIncidence[i][j] / (totalIncidence / 3.0);
                }
            }
        }

        return busIncidencePercentages;
    }
}
