package control;

import utils.Rngs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class MsqT2 {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class MsqSum2 {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long   served;                    /*   number served                    */
}

class MsqEvent2{                     /* the next-event list    */
    double t;                         /*   next event time      */
    int    x;                         /*   event status, 0 or 1 */
}
public class CallCenterNodeController implements Runnable {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = 20000.0;        /* terminal (close the door) time */
    static int SERVERS = 200;/* number of servers              */



    static double sarrival = START;

    private Queue<Object> sharedQueue;

    public CallCenterNodeController(Queue<Object> sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    public List<MsqEvent2> runFirstNode(){
        long number = 0;             /* number in the node                 */
        int e;                      /* next event index                   */
        int s;                      /* server index                       */
        long index = 0;             /* used to count processed jobs       */
        double area = 0.0;           /* time integrated number in the node */
        double service;
        List<MsqEvent2> eventsToDispatch = new ArrayList<>();

        CallCenterNodeController m = new CallCenterNodeController(sharedQueue);
        Rngs r = new Rngs();
        r.plantSeeds(0);

        //array di msqEvent e msqSum
        MsqEvent2[] event = new MsqEvent2[SERVERS + 1];
        MsqSum2[] sum = new MsqSum2[SERVERS + 1];
        for (s = 0; s < SERVERS + 1; s++) {
            event[s] = new MsqEvent2();
            sum[s] = new MsqSum2();
        }

        MsqT2 t = new MsqT2();

        t.current    = START;
        event[0].t   = m.getArrival(r);
        event[0].x   = 1;
        for (s = 1; s <= SERVERS; s++) {
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        while ((event[0].x != 0) || (number != 0)) {
            e         = m.nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            area     += (t.next - t.current) * number;     /* update integral  */
            t.current = t.next;                            /* advance the clock*/

            if (e == 0) {                                  /* process an arrival*/
                number++;
                event[0].t        = m.getArrival(r);
                if (event[0].t > STOP)
                    event[0].x      = 0;
                if (number <= SERVERS) {
                    service         = m.getService(r);
                    s               = m.findOne(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    event[s].x      = 1;
                }
            }
            else {                                         /* process a departure */
                eventsToDispatch.add(event[e]);
                index++;                                     /* from server s       */
                number--;
                s                 = e;
                if (number >= SERVERS) {
                    service         = m.getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                }
                else
                    event[s].x      = 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + index + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[0].t / index));
        System.out.println("  avg wait ........... =   " + f.format(area / index));
        System.out.println("  avg # in node ...... =   " + f.format(area / t.current));

        for (s = 1; s <= SERVERS; s++)          /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(area / t.current));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= SERVERS; s++) {
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }

        System.out.println("");
        System.out.println(eventsToDispatch.size());
        return eventsToDispatch;
    }


    double uniform(double a, double b, Rngs r) {
        /* --------------------------------------------
         * generate a Uniform random variate, use a < b
         * --------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double poisson(double lambda, Rngs r) {
        return (-Math.log(1.0 - r.random()) / lambda);
    }



    double getArrival(Rngs r) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         * Per un centralino di un call center di grandi dimensioni, il tasso di arrivo potrebbe essere più elevato,
         * ad esempio intorno a 10-20 chiamate al minuto, corrispondente a λ = 0.167-0.333 chiamate al secondo.
         */
        r.selectStream(0);
        sarrival += poisson( 20, r);
        return (sarrival);
    }


    double getService(Rngs r) {
        /* ------------------------------
         * generate the next service time, with rate 1/6
         * ------------------------------
         */
        r.selectStream(1);
        return (uniform(2, 10.0, r));
    }

    int nextEvent(MsqEvent2 [] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < SERVERS) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }

    int findOne(MsqEvent2 [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < SERVERS) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    @Override
    public void run() {
        long number = 0;             /* number in the node                 */
        int e;                      /* next event index                   */
        int s;                      /* server index                       */
        long index = 0;             /* used to count processed jobs       */
        double area = 0.0;           /* time integrated number in the node */
        double service;
        //List<MsqEvent2> eventsToDispatch = new ArrayList<>();


        Rngs r = new Rngs();
        r.plantSeeds(0);

        //array di msqEvent e msqSum
        MsqEvent2[] event = new MsqEvent2[SERVERS + 1];
        MsqSum2[] sum = new MsqSum2[SERVERS + 1];
        for (s = 0; s < SERVERS + 1; s++) {
            event[s] = new MsqEvent2();
            sum[s] = new MsqSum2();
        }

        MsqT2 t = new MsqT2();

        t.current    = START;
        event[0].t   = getArrival(r);
        event[0].x   = 1;
        for (s = 1; s <= SERVERS; s++) {
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        while ((event[0].x != 0) || (number != 0)) {
            e         = nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            area     += (t.next - t.current) * number;     /* update integral  */
            t.current = t.next;                            /* advance the clock*/

            if (e == 0) {                                  /* process an arrival*/
                number++;
                event[0].t        = getArrival(r);
                if (event[0].t > STOP)
                    event[0].x      = 0;
                if (number <= SERVERS) {
                    service         = getService(r);
                    s               = findOne(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    event[s].x      = 1;
                }
            }
            else {                                         /* process a departure */
                sharedQueue.add(event[e]);
                index++;                                     /* from server s       */
                number--;
                s                 = e;
                if (number >= SERVERS) {
                    service         = getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                }
                else
                    event[s].x      = 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + index + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[0].t / index));
        System.out.println("  avg wait ........... =   " + f.format(area / index));
        System.out.println("  avg # in node ...... =   " + f.format(area / t.current));

        for (s = 1; s <= SERVERS; s++)          /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(area / t.current));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= SERVERS; s++) {
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }

        System.out.println("");
        System.out.println(sharedQueue.size());
    }
}
