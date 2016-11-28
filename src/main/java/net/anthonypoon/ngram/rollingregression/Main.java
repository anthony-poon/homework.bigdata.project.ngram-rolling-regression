/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.anthonypoon.ngram.rollingregression;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *
 * @author ypoon
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("a", "action", true, "Action");
        options.addOption("i", "input", true, "input");
        options.addOption("o", "output", true, "output");
        //options.addOption("f", "format", true, "Format");
        options.addOption("u", "upbound", true, "Year up bound");
        options.addOption("l", "lowbound", true, "Year low bound");
        options.addOption("r", "range", true, "Range");
        options.addOption("T", "threshold", true, "Threshold - min count for regression");
        options.addOption("p", "positive-only", false, "Write positive slope only"); // default faluse
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        Configuration conf = new Configuration();
        if (cmd.hasOption("range")) {
            conf.set("range", cmd.getOptionValue("range"));
        }
        if (cmd.hasOption("upbound")) {
            conf.set("upbound", cmd.getOptionValue("upbound"));
        } else {
            conf.set("upbound", "9999");
        }
        if (cmd.hasOption("lowbound")) {
            conf.set("lowbound", cmd.getOptionValue("lowbound"));
        } else {
            conf.set("lowbound", "0");
        }
        if (cmd.hasOption("threshold")) {
            conf.set("threshold", cmd.getOptionValue("threshold"));
        }
        if (cmd.hasOption("positive-only")) {
            conf.set("positive-only", "true");
        }
        Job job = Job.getInstance(conf);
        /**
        if (cmd.hasOption("format")) {
            switch (cmd.getOptionValue("format")) {
                case "compressed":
                    job.setInputFormatClass(SequenceFileAsTextInputFormat.class);
                    break;
                case "text":
                    job.setInputFormatClass(KeyValueTextInputFormat.class);
                    break;
            }
            
        }**/
        job.setJarByClass(Main.class);
        switch (cmd.getOptionValue("action")) {
            case "get-regression":
                job.setOutputKeyClass(Text.class);
                job.setOutputValueClass(Text.class);
                for (String inputPath : cmd.getOptionValue("input").split(",")) {
                    MultipleInputs.addInputPath(job,new Path(inputPath), KeyValueTextInputFormat.class, RollingRegressionMapper.class);
                }
                job.setReducerClass(RollingRegressionReducer.class);
                break;            
            default:
                throw new IllegalArgumentException("Missing action");
        }
        
        String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());       
        
        //FileInputFormat.setInputPaths(job, new Path(cmd.getOptionValue("input")));
        FileOutputFormat.setOutputPath(job, new Path(cmd.getOptionValue("output") + "/" + timestamp));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
        /**
        double[] nazismBaseLine = {3, 12, 12, 18, 233, 239, 386, 333, 593, 1244, 1925, 3013, 3120, 3215, 3002, 3355, 2130, 1828, 1406, 1751, 1433, 1033, 881, 1330, 1029, 760, 1288, 1013, 1014};
        InputStream inStream = Main.class.getResourceAsStream("/1g-matrix.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        String line = "";
        Map<String, Double> result = new HashMap();
        while ((line = br.readLine()) != null) {
            String[] strArray = line.split("\t");
            double[] compareArray = new double[nazismBaseLine.length];
            for (int i = 0; i < nazismBaseLine.length; i ++) {
                compareArray[i] = Double.valueOf(strArray[i + 24]);
            }
            result.put(strArray[0], new PearsonsCorrelation().correlation(nazismBaseLine, compareArray));
        }
        List<Map.Entry<String, Double>> toBeSorted = new ArrayList();
        for (Map.Entry pair : result.entrySet()) {
            toBeSorted.add(pair);
        }
        Collections.sort(toBeSorted, new Comparator<Map.Entry<String, Double>>(){
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Map.Entry<String, Double> pair : toBeSorted) {
            if (!Double.isNaN(pair.getValue())) {
                System.out.println(pair.getKey() + "\t" + pair.getValue());
            }
        }**/
    }
}
