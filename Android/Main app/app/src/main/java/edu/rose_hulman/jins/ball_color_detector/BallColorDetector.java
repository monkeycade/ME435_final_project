package edu.rose_hulman.jins.ball_color_detector;

import java.util.ArrayList;
import java.util.List;

public class BallColorDetector {
    /**
     * the learning rate
     */
    private double rate;

    /**
     * the number of iterations
     */
    private final static int MAX_ITERATIONS = 80000;
    private final static double TOLERANCE = 0.0001;

    //[ball,R,G,B,WHITE,OFF]
    private List<Instance> ballColorData;
    //[BALL FOLLOW SEQUENCE BY CONSTANT][R_coeff,G_coeff,B_coeff,WHITE_coeff,OFF_coeff]
    private double[][] calculationCoeff;

    private StringBuilder errorData;


    public BallColorDetector(List<Double> storedcoef, List<Instance> storedColorData) {
        errorData = new StringBuilder();
        calculationCoeff = new double[6][5];
        if(storedcoef == null || storedcoef.size() < 30){
            errorData.append("The input coefficient is " + storedcoef == null ? "null" : "not enough");
        }else {
            for (int ball = 0; ball < 6; ball++) {
                for (int i = 0; i < 5; i++) {
                    calculationCoeff[ball][i] = storedcoef.get(ball * 6 + i);
                }
            }
        }
        ballColorData = storedColorData;
        if (storedColorData == null){
            ballColorData =  new ArrayList<Instance>();
            errorData.append("The input ball Data is null");
        }

        rate = 0.0001;

        train();
    }

    public String getErrorData(){
        String toReturn = errorData.toString();
        errorData = new StringBuilder();
        return toReturn;
    }

    public List<Double> gettostoreCoefficient(){
        ArrayList<Double> coeftoStore = new ArrayList<>();
        for (int ball = 0; ball < 6; ball++) {
            for (int i = 0; i < 5; i++) {
                coeftoStore.add(calculationCoeff[ball][i]);
            }
        }
        return coeftoStore;
    }

    public List<Instance> gettostoreInstance(){
        return ballColorData;
    }

    public void train() {
        for (int ball = 0; ball < calculationCoeff.length; ball++) {
            //get the coefficient of the ball
            double[] weights = calculationCoeff[ball];
            for (int n = 0; n < MAX_ITERATIONS; n++) {
                //record how much offset the coefficient have against training data
                double total_cost = 0.0;
                for (int i = 0; i < ballColorData.size(); i++) {
                    //get certain set of train data R G B OFF WHITE
                    int[] x = ballColorData.get(i).x;
                    //calculate the percentage of current result
                    double predicted = classify(x, ball);
                    //get the expect result 1 for is that color or 0 is not that color
                    int label = ballColorData.get(i).label == ball ? 1 : 0;
                    //Change the coefficient based on the offset
                    for (int j = 0; j < weights.length; j++) {
                        weights[j] = weights[j] + rate * (label - predicted) * x[j];
                    }
                    // calculate the performance of that coefficient
                    total_cost += label * Math.log(classify(x, ball)) + (1 - label) * Math.log(1 - classify(x, ball));
                }
                //finish training if reach max iteration or the performance meet the tolerance
                if (Math.abs(total_cost) < TOLERANCE) {
                    break;
                }
            }
        }

    }

    private double classify(int[] x, int ball) {
        double logit = .0;
        double[] weights = calculationCoeff[ball];
        for (int i = 0; i < weights.length; i++) {
            logit += weights[i] * x[i];
        }
        return sigmoid(logit);
    }

    public static class Instance {
        public int label;
        public int[] x;

        public Instance(int label, int[] x) {
            this.label = label;
            this.x = x;
        }
    }

//    public List<Instance> readDataSet(ArrayList<Integer[]> storedColorData) {
//        List<Instance> dataset = new ArrayList<Instance>();
//        if (storedColorData != null) {
//            //iterate the ball colar data
//            for (int i = 0; i < storedColorData.size(); i++) {
//                Integer[] data = storedColorData.get(i);
//                //get R G B OFF WHITE info
//                int[] temp = new int[5];
//                for (i = 1; i < temp.length; i++) {
//                    temp[i - 1] = data[i];
//                }
//                //get ball color info
//                int label = data[0];
//                Instance instance = new Instance(label, temp);
//                dataset.add(instance);
//            }
//        }
//        return dataset;
//    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
