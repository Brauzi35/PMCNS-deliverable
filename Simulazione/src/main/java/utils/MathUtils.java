package utils;

public class MathUtils {

    public static double findMinimum(double[] values) {
        double min = Double.MAX_VALUE; // Inizializzazione con un valore molto grande

        for (double value : values) {
            if (value < min) {
                min = value;
            }
        }

        return min;
    }


}
