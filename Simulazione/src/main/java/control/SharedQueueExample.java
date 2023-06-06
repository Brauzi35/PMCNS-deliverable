package control;

import utils.Rngs;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SharedQueueExample {
    private static final int QUEUE_CAPACITY = Integer.MAX_VALUE/100;

    static double START   = 0.0;            /* initial (open the door)        */
    static double STOP    = 20000.0;        /* terminal (close the door) time */
    static int    SERVERS = 200;              /* number of servers              */

    //static double sarrival = START;

    public static void main(String[] args) {



        // Creazione delle code condivise tra i thread
        BlockingQueue<MsqEvent> queueAB = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<MsqEvent> queueBC = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<MsqEvent> queueBD = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Creazione dei thread
        Thread threadA = new Thread(new ProducerThread(queueAB), "Thread A");
        Thread threadB = new Thread(new DispatcherThread(queueAB, queueBC, queueBD), "Thread B");
        Thread threadC = new Thread(new ConsumerThread(queueBC), "Thread C");
        Thread threadD = new Thread(new ConsumerThread(queueBD), "Thread D");

        // Avvio dei thread
        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
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
            Rngs r = new Rngs();
            r.plantSeeds(0);

            //array di msqEvent e msqSum
            MsqEvent [] event = new MsqEvent [SERVERS + 1];
            MsqSum [] sum = new MsqSum [SERVERS + 1];
            for (s = 0; s < SERVERS + 1; s++) {
                event[s] = new MsqEvent();
                sum [s]  = new MsqSum();
            }

            MsqT t = new MsqT();

            t.current    = START;
            event[0].t   = m.getArrival(r);
            event[0].x   = 1;
            for (s = 1; s <= SERVERS; s++) {
                event[s].t     = START;          /* this value is arbitrary because */
                event[s].x     = 0;              /* all servers are initially idle  */
                sum[s].service = 0.0;
                sum[s].served  = 0;
            }
            System.out.println("thread 1 prima del while");

            while ((event[0].x != 0) || (number != 0)) {
                System.out.println("thread 1 dentro while");
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
                    System.out.println("thread 1 dentro else");
                    sharedQueue.add(event[e]);              //add in shared queue
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
                // Consumo degli oggetti MsqEvent
                while (true) {
                    MsqEvent event = inputQueue.take();
                    processEvent(event);
                    double rand = Math.random();
                    if (rand < 0.7) {
                        outputQueue1.put(event);
                    }
                    else {
                        outputQueue2.put(event);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void processEvent(MsqEvent event) {
            //todo


        }

    }

    // Thread consumatore (Thread C, D) riparazione
    static class ConsumerThread implements Runnable {
        private final BlockingQueue<MsqEvent> inputQueue;

        public ConsumerThread(BlockingQueue<MsqEvent> inputQueue) {
            this.inputQueue = inputQueue;

        }

        @Override
        public void run() {
            try {
                // Consumo degli oggetti MsqEvent
                while (true) {
                    MsqEvent event = inputQueue.take();
                    processEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Metodo di elaborazione degli oggetti MsqEvent
        private void processEvent(MsqEvent event) {
            System.out.println("entrato nel thread consumer" + event.t);
        }
    }
}
