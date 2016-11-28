/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.anthonypoon.ngram.rollingregression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 *
 * @author ypoon
 */
public class RollingRegressionReducer extends Reducer<Text, Text, Text, Text>{

    private Integer lowbound = 0;
    private Integer upbound = 0;
    private Integer range = 0;
    private Integer threshold = 1000;
    private Boolean positiveOnly = false;
    private Map<String, TreeMap<String, Double>> corrTargetArray = new HashMap();
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        range = Integer.valueOf(context.getConfiguration().get("range", "3"));
        lowbound = Integer.valueOf(context.getConfiguration().get("lowbound"));
        upbound = Integer.valueOf(context.getConfiguration().get("upbound")); 
        threshold = Integer.valueOf(context.getConfiguration().get("threshold", "0"));
        positiveOnly = Boolean.valueOf(context.getConfiguration().get("positive-only", "false"));
    }
    
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        TreeMap<String, Double> currElement = new TreeMap();
        boolean pastThreshold = false;
        for (Text val : values) {
            String[] strArray = val.toString().split("\t");
            if (Double.valueOf(strArray[1]) > threshold) {
                pastThreshold = true;
            }
            currElement.put(strArray[0], Math.log(Double.valueOf(strArray[1])));
        }
        if (pastThreshold) {
            for (Integer i = 0; i <= upbound - lowbound; i ++) {
                if (!currElement.containsKey(String.valueOf(lowbound + i))) {
                    if (i != 0) {
                        currElement.put(String.valueOf(lowbound + i), currElement.get(String.valueOf(lowbound + i - 1)));
                    } else {
                        currElement.put(String.valueOf(lowbound + i), 0.0);
                    }
                } 

            }
            TreeMap<String, Double> result = new TreeMap();
            for (Integer i = 0 + range; i <= upbound - lowbound - range; i ++) {
                SimpleRegression regression = new SimpleRegression();
                for (Integer l = - range; l <= range; l ++) {
                    regression.addData(l.doubleValue(), currElement.get(String.valueOf(i+lowbound+l)));
                }
                if (!Double.isNaN(regression.getSlope())) {
                    if (!positiveOnly || regression.getSlope() > 0) {
                        result.put(String.valueOf(lowbound+i), regression.getSlope());
                    }
                }
            }
            for (Map.Entry<String, Double> pair : result.entrySet()) {
                context.write(key, new Text(pair.getKey() + "\t" + String.format("%.5f", pair.getValue())));
            }
        }
    }
    
}
