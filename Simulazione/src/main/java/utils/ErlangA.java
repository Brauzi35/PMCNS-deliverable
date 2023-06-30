package utils;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Functions for the Erlang-A model (M/M/n+M queue).
 * <p>
 * A number of metrics can be expressed by means of the incomplete Gamma
 * function. However, since that grows really quickly, causing overflows, an
 * algorithm which is numerically stable is employed.
 */
public class ErlangA {

    private static final double MAX_ITERATIONS = 10E+6;

    private static final double ERR = 10E-15;

    /** Number of servers. */
    private final int n;

    /** Load (&rho; = &lambda; / &mu;). */
    private final double rho;

    /** The arrival rate. */
    private final double lam;

    /** The service rate. */
    private final double mu;

    /** The abandonment rate. The average patience is defined as 1/&theta;. */
    private final double theta;

    /** Steady-state distribution of probabilities. */
    private double[] p;

    /** Steady-state distribution of jobs. */
    private double[] jobs;


    private final double axy;

    /**
     * Cached value of pn, i.e., probability that there are n jobs in the
     * system.
     */
    private final double pn;

    /**
     * Constructor.
     *
     * @param n The number of servers.
     * @param lam The arrival rate.
     * @param mu The service rate.
     * @param theta The abandonment rate.
     */
    public ErlangA(int n, double lam, double mu, double theta)
            throws IllegalArgumentException {
        if (n < 1) {
            throw new IllegalArgumentException("Need at least one server!");
        }
        if ((lam < 0.0) || (mu < 0.0)) {
            throw new IllegalArgumentException("Load parameters must be >= 0");
        }
        if (theta <= 0.0) {
            // if theta = 0 it becomes a M/M/n queue
            throw new IllegalArgumentException(
                    "The abandonment rate must be > 0");
        }
        this.n = n;
        this.lam = lam;
        this.mu = mu;
        this.rho = lam / mu;
        this.theta = theta;

        double x = this.n * this.mu / this.theta;
        double y = this.lam / this.theta;

        this.axy = axy(x, y);
        this.pn = pn();
        this.p = computeProbabilitiesP0();
        this.jobs = computeJobsDistribution();
    }

    /**
     * Computes the steady-state probability distribution starting from p0.
     */
    private final double[] computeProbabilitiesP0() {
        DoubleArrayList list = new DoubleArrayList(this.n);
        final double p0 = p0();
        list.add(p0);
        for (int i = 1; i <= n; i++) { // compute p1...pn
            double tmp = p0;
            for (int j = 1; j <= i; j++) {
                tmp *= rho / j; // compute rho^j / j!
            }
            // p0 * rho^j / j!
            list.add(tmp); // add p1...pn to the list
        }

        double pn = list.get(this.n); // pn is computed by itself, check
        assert (pn - this.pn <= 10E-6);

        // compute p_n+1... stop when the error is smaller than 10^-15
        double pj = 0.0;
        int j = n;

        double tmp = p0; // compute p0 * rho^n / n!
        for (int i = 1; i <= this.n; i++) {
            tmp *= rho / i;
        }
        do { // prod k=n+1...j (lam / (n*mu + (k-n)*theta) * tmp
            pj = tmp;
            j++;
            for (int k = n + 1; k <= j; k++) {
                pj *= this.lam / (this.n * this.mu + (k - this.n) * this.theta);
            }
            list.add(pj);
        } while (pj > ERR); // stop when prob. j is about 0

        return list.stream().mapToDouble(d -> d).toArray();
    }

    /**
     * Computes the steady-state jobs distribution.
     */
    private final double[] computeJobsDistribution() {
        double[] res = new double[this.p.length];

        for (int i = 1; i < res.length; i++) {
            res[i] = i * this.p[i];
        }
        return res;
    }



    /**
     * Computes the probability that the system is empty.
     */
    private final double p0() {
        double tmp = 1.0;
        for (int i = 1; i <= n; i++) {
            tmp *= i / this.rho;
        }
        return this.pn * tmp;
    }

    /**
     * Computes A(x, y) = (x e<sup>y</sup> / y<sup>x</sup>) * &gamma;(x,y) as 1
     * + sum j=1...inf y<sup>j</sup> / prod k = 1...k (x+k)
     */
    private static final double axy(double x, double y) {
        double res = 1.0;
        double tmp = 1.0;
        int j;
        for (j = 1; j <= MAX_ITERATIONS; j++) {
            tmp *= y / (x + j);
            res += tmp;
            if (tmp < ERR) {
                break;
            }
        }
        if (j > MAX_ITERATIONS) {
            System.err.printf("axy did not converge, found %.10f\n", res);
        }

        return res;
    }

    /**
     * Computes the probability that all servers are busy and no jobs are
     * waiting, i.e., the probability that there are exactly n jobs in the
     * system.
     */
    private final double pn() {
        final double erlangB = ErlangB.erlangB(this.n, this.rho);
        double tmp = 1.0 + erlangB * (this.axy - 1.0);
        return erlangB / tmp;
    }

    /**
     * Computes the probability that a job will have to wait, P(W>0).
     */
    public double waitingProbability() {
        return this.axy * this.pn;
    }

    /**
     * Computes the abandonment probability of delayed jobs, P(Ab|W>0).
     */
    public double abandonProbIfDelayed() {
        return (1.0 / (this.rho * this.axy)) + 1.0 - (1.0 / this.rho);
    }



    /**
     * Computes the probability that a job will abandon the system, P(Ab).
     */
    public double abandonmentProbability() {
        return abandonProbIfDelayed() * waitingProbability();
    }



    /**
     * /** Computes the probability that a job which, on arrival, finds all
     * servers busy and <i>i</i> jobs in the queue, i.e., <i>n+i</i> jobs in the
     * system, abandons the system.
     *
     * @param i Number of jobs in the queue.
     * @return The probability of abandonment.
     * @throws IllegalArgumentException If i < 0.
     */
    public double probAbandonment(int i) throws IllegalArgumentException {
        if (i < 0) {
            throw new IllegalArgumentException("");
        }
        return 1.0 - probAbandonment(i);
    }


    /**
     * Method to call from Ssq3 class for get the abandonment probability
     */
    public static void getAbandonmentProb(int n, double lam, double mu, double meanWaitinTime, double meanPatience){
        //        int n = 20;
        //        double lam = 19.0; arrival rate
        //        double mu = 1; service rate
        //        double theta = 0.000001;


        double theta = meanWaitinTime/meanPatience;
        ErlangA er = new ErlangA(n, lam, mu, theta);


        System.out.printf("P(Ab) %.10f\n", er.abandonmentProbability());
        System.out.printf("P(Ab | tq>0) " + er.abandonProbIfDelayed());
    }

    public static void main(String[] args) throws Exception {
        int n = 4;
        double lam = 2;//0.05194;
        double mu = 3.91;
        double theta = 0.000001; //tasso di abbandono
        ErlangA er = new ErlangA(n, lam, mu, theta);


        System.out.printf("P(Ab) %.10f\n", er.abandonmentProbability());
        System.out.printf("P(Ab | tq>0) " + er.abandonProbIfDelayed());
    }
}
