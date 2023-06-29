package control;


import org.apache.commons.math3.distribution.LogNormalDistribution;
import utils.MathUtils;
import utils.Rngs;

import java.io.*;
import java.lang.*;
import java.text.*;


class Ssq3Area {
    double node;                    /* time integrated number in the node  */
    double queue;                   /* time integrated number in the queue */
    double service;                 /* time integrated number in service   */

    void initAreaParas() {
        node = 0.0;
        queue = 0.0;
        service = 0.0;
    }
}

class Ssq3T {
    double arrival;                 /* next arrival time                   */
    double leaving;                 /* next queue abandon                   */
    double completion;              /* next completion time                */
    double current;                 /* current time                        */
    double next;                    /* next (most imminent) event time     */
    double last;                    /* last arrival time                   */


}


class Simulation {

    static double START = 0.0;              /* initial time                   */
    static double STOP  = 20000.0;          /* terminal (close the door) time */
    static double INFINITY = 100.0 * STOP;  /* must be much larger than STOP  */

    static double sarrival = START;

    public static void main(String[] args) {

        long index  = 0;                  /* used to count departed jobs         */
        long abnd   = 0;                  /* used to count abandons              */
        long number = 0;                  /* number in the node                  */

        Simulation s = new Simulation();

        Rngs r = new Rngs();
        r.plantSeeds(123456789);

        Ssq3T t      = new Ssq3T();
        t.current    = START;           /* set the clock                         */
        t.arrival    = s.getArrival(r); /* schedule the first arrival            */
        t.completion = INFINITY;        /* the first event can't be a completion */
        t.leaving    = INFINITY;        /* the first event can't be an abandon   */

        Ssq3Area area = new Ssq3Area();
        area.initAreaParas();

        while ((t.arrival < STOP) || (number > 0)) {
            //t.next          = Math.min(t.arrival, t.completion);  /* next event time   */
            double[] values = { t.arrival, t.leaving, t.completion};
            t.next = MathUtils.findMinimum(values);
            //System.out.println(" number is: " + number);
            if (number > 0)  {                               /* update integrals  */
                area.node    += (t.next - t.current) * number;
                area.queue   += (t.next - t.current) * (number - 1);
                area.service += (t.next - t.current);
            }
            t.current       = t.next;                    /* advance the clock */

            if (t.current == t.arrival)  {               /* process an arrival */
                //System.out.println(" tempo arrivo = "+  t.arrival);
                number++;

                t.arrival     = s.getArrival(r);
                if (t.arrival > STOP)  {
                    t.last      = t.current;
                    t.arrival   = INFINITY;
                }
                if (number == 1) {
                    t.completion = t.current + s.getService(r);
                    //t.leaving = t.current + s.getAbandon(r);
                }
                //System.out.println(" arr is: " + number +" leav is: " + number +" number is: " + number +);
            }

            else if (t.current == t.leaving){                 /* process an abandon */
                //se c'è un abbandono devo rigenerare tutti i tempi
                System.out.println(" tempo abbandono = "+  t.leaving);
                abnd++;
                index++;
                number--;
                if (number > 0) {
                    t.leaving = t.arrival + s.getAbandon(r);
                    //t.completion = t.arrival + s.getService(r);
                }
                else
                    t.leaving = INFINITY;
            }

            else {                                        /* process a completion */
                System.out.println(" tempo compl = "+  t.completion);

                index++;
                number--;
                if (number > 0)
                    t.completion = t.current + s.getService(r);
                    //generare anche il prossimo abbandono? forse no
                else
                    t.completion = INFINITY;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");

        System.out.println("\nfor " + index + " jobs with "+ abnd + " abandons");
        System.out.println("   average interarrival time =   " + f.format(t.last / index));
        System.out.println("   average wait ............ =   " + f.format(area.node / index));
        System.out.println("   average delay ........... =   " + f.format(area.queue / index));
        System.out.println("   average service time .... =   " + f.format(area.service / index));
        System.out.println("   average # in the node ... =   " + f.format(area.node / t.current));
        System.out.println("   average # in the queue .. =   " + f.format(area.queue / t.current));
        System.out.println("   utilization ............. =   " + f.format(area.service / t.current));
    }


    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double uniform(double a, double b, Rngs r) {
        /* ------------------------------------------------
         * generate an Uniform random variate, use a < b
         * ------------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double logNormal(double mu){

        double stdDev = mu * 0.3; // dev = mu * CV, nei call center possiamo assumere un valore costante di deviazione pari al 30%


        LogNormalDistribution logNormalDistribution = new LogNormalDistribution(mu, stdDev);

        System.out.println("STO IN LOGNORMAL");
        System.out.println(logNormalDistribution.sample());

        return logNormalDistribution.sample();
    }

    double getArrival(Rngs r) {
        /* ---------------------------------------------
         * generate the next arrival time, with rate 1/2
         * ---------------------------------------------
         */
        r. selectStream(0);
        sarrival += exponential(3.2, r);
        return (sarrival);
    }

    double getAbandon(Rngs r){
        r.selectStream(1);
        return (uniform(1.0, 1.5, r)); //deve diventare erlang a
    }


    double getService(Rngs r) {
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
        r. selectStream(2);
        //return (uniform(1.0, 2.0, r));
        return (logNormal(8)); //passo solo la media, poi calcolo la deviazione standard
    }

}