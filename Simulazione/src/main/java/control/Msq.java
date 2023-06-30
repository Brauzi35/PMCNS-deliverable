package control;


import utils.Rngs;
import utils.Rvms;

import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;


class MsqT {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class MsqSum {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long   served;                    /*   number served                    */
}

class MsqEvent{                     /* the next-event list    */
    double t;                         /*   next event time      */
    int    x;                         /*   event status, 0 or 1 */
}


class Msq {
    static double START   = 0.0;            /* initial (open the door)        */
    static double STOP    = 20000.0;        /* terminal (close the door) time */
    static int    SERVERS = 4;              /* number of servers              */

    static double sarrival = START;


    public static void main(String[] args) {

        long   number = 0;             /* number in the node                 */
        int    e;                      /* next event index                   */
        int    s;                      /* server index                       */
        long   index  = 0;             /* used to count processed jobs       */
        long   abandon = 0;
        double area   = 0.0;           /* time integrated number in the node */
        double service;

        Msq m = new Msq();
        Rngs r = new Rngs();
        r.plantSeeds(123456789);

        List<Double> abandons = new ArrayList<Double>(); //lista abbandoni


        MsqEvent [] event = new MsqEvent [SERVERS + 2];
        MsqSum [] sum = new MsqSum [SERVERS + 2];
        for (s = 0; s < SERVERS + 2; s++) { //prima era +1, ne ho messo un altro per gli abbandoni
            event[s] = new MsqEvent();
            sum [s]  = new MsqSum();
        }
        //l'evento messo in posizione 1 è l'evento di abbandono dalla coda

        MsqT t = new MsqT();

        t.current    = START;
        event[0].t   = m.getArrival(r);
        event[0].x   = 1;
        for (s = 1; s <= SERVERS; s++) { //servers + 1?
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        while ((event[0].x != 0) || (number != 0)) {

            //System.out.println("number is "+number);
            System.out.println("stato server con number a: " + number);
            System.out.println("abbandoni: "+abandons);
            for(int i = 2; i<SERVERS+2; i++) {

                System.out.println(i+ " - " +event[i].x + " time: " + event[i].t);
            }
            if(!abandons.isEmpty()){
                event[1].t = abandons.get(0);
                event[1].x = 1;
            }
            else{
                event[1].x = 0;
            }


            e         = m.nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            area     += (t.next - t.current) * number;     /* update integral  */
            t.current = t.next;                            /* advance the clock*/

            System.out.println("t current is: "+t.current);



            if (e == 0) {                                  /* process a callcenter arrival*/
                System.out.println("entrato in arivals");
                number++;
                event[0].t        = m.getArrival(r);
                if (event[0].t > STOP)
                    event[0].x      = 0; //close the door
                if (number <= SERVERS) {
                    service         = m.getService(r);
                    s               = m.findOne(event); //id server
                    System.out.println("s is " + s);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service; //tempo di completamento
                    event[s].x      = 1; //eleggibile per il next event
                }
                if (number > SERVERS){
                    //genero abbandono se un job sta in coda
                    System.out.println("arrivo di un job messo in coda e numero di job nel nodo = " + number);
                    double at = m.getAbandon(r) + t.current;
                    abandons.add(at);
                }
            }

            else if(e == 1) { //processo abbandono callcenter
                //index++;
                System.out.println("entrato in abandons");
                number--;
                abandon++;
                abandons.remove(0); //tolgo job dalla lista
                if(abandons.isEmpty()){
                    event[1].x = 0;
                }
            }

            else {                                         /* process a callcenter departure */
                System.out.println("entrato in departures");
                index++;                                     /* from server s       */
                number--;
                s                 = e; //indice next event = server id


                if(!abandons.isEmpty()) {
                    abandons.remove(0);
                }


                if (number >= SERVERS) { //se ho coda
                    service         = m.getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;


                }
                else {
                    event[s].x = 0;
                }
            }
            System.out.println("FINE ITERAZIONE\n\n");
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + index + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[0].t / index));
        System.out.println("  avg wait ........... =   " + f.format(area / index));
        System.out.println("  avg # in node ...... =   " + f.format(area / t.current));

        for (s = 2; s <= SERVERS+1; s++)          /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(area / t.current));
        System.out.println("  abandons ........... =   " + abandon);
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 2; s <= SERVERS+1; s++) {
            //System.out.println(s +" "+sum[s].service + " " +t.current);
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }

        System.out.println("");
    }


    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double uniform(double a, double b, Rngs r) {
        /* --------------------------------------------
         * generate a Uniform random variate, use a < b
         * --------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double getAbandon(Rngs r){
        r.selectStream(1);
        return (uniform(2.0, 10.5, r)); //deve diventare erlang a
    }

    double getArrival(Rngs r) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        r.selectStream(0);
        sarrival += exponential(2.0, r);
        return (sarrival);
    }


    double getService(Rngs r) {
        /* ------------------------------
         * generate the next service time, with rate 1/6
         * ------------------------------
         */
        Rvms rvms = new Rvms();
        r.selectStream(1);
        //return (uniform(2.0, 10.0, r));
        return rvms.idfLogNormal(5.97, 0.02761, r.random());
    }

    int nextEvent(MsqEvent [] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < SERVERS+1) {      //messo +1   /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }

    int findOne(MsqEvent [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 2; //prima era 1

        while (event[i].x == 1 ) {     /* find the index of the first available */
            System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < SERVERS+1) {     //aggiunto +1    /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }



}
