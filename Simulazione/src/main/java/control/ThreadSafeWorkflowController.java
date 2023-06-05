package control;

import utils.Rngs;
import utils.TimeSimulator;

import java.sql.Time;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadSafeWorkflowController {

    private static final int QUEUE_CAPACITY = Integer.MAX_VALUE/100;

    static double START   = 0.0;            /* initial (open the door)        */
    static double STOP    = 20000.0;        /* terminal (close the door) time */
    static int    SERVERS = 200;              /* number of servers              */
    static int    DISPATCHERS = 1;

    static Rngs r = new Rngs();


    public static void main(String[] args) {



        Msq m = new Msq(); //call center utilities
        Ssq3 ssq3 = new Ssq3(); //dispatcher utilities
        r.plantSeeds(0);


        // Creazione delle code condivise tra i thread
        BlockingQueue<MsqEvent> queueAB = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<MsqEvent> queueBC = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<MsqEvent> queueBD = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Creazione dei thread
        Thread threadA = new Thread(new SharedQueueExample.ProducerThread(queueAB), "Thread A");
        Thread threadB = new Thread(new SharedQueueExample.DispatcherThread(queueAB, queueBC, queueBD), "Thread B");
        Thread threadC = new Thread(new SharedQueueExample.ConsumerThread(queueBC), "Thread C");
        Thread threadD = new Thread(new SharedQueueExample.ConsumerThread(queueBD), "Thread D");

        // Avvio dei thread
        threadA.start();
        threadB.start();
        //threadC.start();
        //threadD.start();
    }

    // Thread produttore (Thread A) call center
    static class ProducerThread implements Runnable {
        private final BlockingQueue<MsqEvent> sharedQueue;

        public ProducerThread(BlockingQueue<MsqEvent> sharedQueue) {
            this.sharedQueue = sharedQueue;
        }

        @Override
        public void run() {
            long   number = 0;             /* number in the node                 */
            int    e;                      /* next event index                   */
            int    s;                      /* server index                       */
            long   index  = 0;             /* used to count processed jobs       */
            double area   = 0.0;           /* time integrated number in the node */
            double service;

            Msq m = new Msq();


            //array di msqEvent e msqSum
            MsqEvent [] event = new MsqEvent [SERVERS + 1];
            MsqSum [] sum = new MsqSum [SERVERS + 1];
            for (s = 0; s < SERVERS + 1; s++) {
                event[s] = new MsqEvent();
                sum [s]  = new MsqSum();
            }

            TimeSimulator t = new TimeSimulator();

            t.setCurrent(START);
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
                t.setNext(event[e].t);                         /* next event time  */
                area     += (t.getNext() - t.getCurrent()) * number;     /* update integral  */
                t.setCurrent(t.getNext());                            /* advance the clock*/

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
                        event[s].t      = t.getCurrent() + service;
                        event[s].x      = 1;
                        event[s].report.setFirstDepartureTime(service);
                    }
                }
                else {                                         /* process a departure */
                    sharedQueue.add(event[e]);              //add in shared queue A
                    index++;                                     /* from server s       */
                    number--;
                    s                 = e;
                    if (number >= SERVERS) {
                        service         = m.getService(r);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t      = t.getCurrent() + service;
                    }
                    else
                        event[s].x      = 0;
                }
            }
        }


    }

    static class DispatcherThread implements Runnable{

        private final BlockingQueue<MsqEvent> inputQueue;
        private final BlockingQueue<MsqEvent> outputQueue1;
        private final BlockingQueue<MsqEvent> outputQueue2;
        public DispatcherThread(BlockingQueue<MsqEvent> inputQueue,
                                BlockingQueue<MsqEvent> outputQueue1,
                                BlockingQueue<MsqEvent> outputQueue2) {
            this.inputQueue = inputQueue;
            this.outputQueue1 = outputQueue1;
            this.outputQueue2 = outputQueue2;
        }

        @Override
        public void run() {
            try {
                long   number = 0;             /* number in the node                 */
                int    e;                      /* next event index                   */
                int    s;                      /* server index                       */
                long   index  = 0;             /* used to count processed jobs       */
                double area   = 0.0;           /* time integrated number in the node */
                double service;

                Msq m = new Msq();


                //array di msqEvent e msqSum
                MsqSum [] sum = new MsqSum [DISPATCHERS + 1];
                MsqEvent [] services = new MsqEvent [SERVERS + 1];
                for (s = 0; s < DISPATCHERS + 1; s++) {
                    sum [s]  = new MsqSum();
                    services [s] = new MsqEvent();
                }

                TimeSimulator t = new TimeSimulator();
                t.setCurrent(START);


                // Consumo degli oggetti MsqEvent
                while (t.getCurrent()<STOP) {
                    MsqEvent currentInput = inputQueue.take();

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }


}
