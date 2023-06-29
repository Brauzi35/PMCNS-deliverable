package math;

/**
 * The bisection (or binary search) method in mathematics is a root-finding
 * method which repeatedly bisects an interval and then selects a subinterval in
 * which a root must lie for further processing.
 */
public class BisectionSearch {

    /**
     * Tolerance in determining convergence upon a root
     */
    private static final double TOLERANCE = 1e-9;

    private BisectionSearch() {
        // avoid instantiation
    }

    /**
     * Bisection (binary search) method.
     *
     * @param fun The function to evaluate.
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     * @return
     */
    public static double bisect(RootFunction fun, double lowerBound,
                                double upperBound) {
        double mid = (upperBound - lowerBound) / 2.0;

        int i = 0; // no of iterations
        final int maxIterations = 1000;
        double preMid = 0.0;
        do {
            i++;
            if (Math.abs(preMid - mid) < TOLERANCE) {
                break;
            }
            preMid = mid;

            double fl = fun.evaluate(lowerBound);
            double fm = fun.evaluate(mid);
            double fu = fun.evaluate(upperBound);

            if (fl >= 0 && fm <= 0) {
                upperBound = mid;
                mid = (upperBound + lowerBound) / 2.0;
                continue;
            }
            if (fm >= 0 && fu <= 0) {
                lowerBound = mid;
                mid = (upperBound + lowerBound) / 2.0;
                continue;
            }
            if (fu >= 0) {
                lowerBound = upperBound;
                upperBound *= 2.0; // double the upper bound
                mid = (upperBound + lowerBound) / 2.0;
                continue;
            }
            if (fl <= 0) {
                upperBound = lowerBound;
                lowerBound /= 2.0; // halve the lower bound
                mid = (upperBound + lowerBound) / 2.0;
                continue;
            }
        } while (i < maxIterations);

        System.out.println(i);
        return preMid;
    }
}
