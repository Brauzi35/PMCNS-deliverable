package model;

public final class SimulationValues {


    SimulationValues(){}

    public static final double START   = 0.0;            /* initial (open the door)        */
    public static final double STOP    = 55800.0;        /* terminal (close the door) time */ //dalle 7 alle 24 in sec 61200.0; 55800 tolte 3 fasce //80000 orizzionte infinito
    public static final double STOP_BATCH    = 10958; /* close the door batch*/
    public static final int    SERVERS = 80;              /* number of servers              */
    public static final int    SERVERS_REMOTI = 56;
    public static final int    SERVERS_FIELD_STD = 250;
    public static final int    SERVERS_FIELD_SPECIAL = 5;
    public static final int    DISPATCHER_SERVICE_TIME = 5;
    public static final double [] PERCENTUALI = {0.001,    0.02,    0.03,    0.04,    0.047,    0.049,    0.049,    0.0485,    0.0465,    0.0425,    0.0385,    0.0355,    0.0355,    0.0375,    0.0365,    0.0355,    0.0365,    0.0375,    0.0395,    0.04,    0.041,    0.041,    0.0395,    0.0345,    0.0275,    0.0185,    0.017,    0.015,    0.011,    0.006,    0.003};

    public static final double REMOTE_PROBABILITY = 0.8;
    public static final double HIGH_PRIORITY_PROBABILITY = 0.0095;
    public static final double MEDIUM_PRIORITY_PROBABILITY = 0.1075;


    public static final double GOBACK_PROBABILITY = 0.16;
    public static final double LEAVE_PROBABILTY = 0.03;


    public static final int EVENT_ARRIVE_CENTRALINO = 1;
    public static final int EVENT_ABANDONMENT_CENTRALINO = 1;
    public static final int EVENT_ARRIVE_DISPATCHER = 1;
    public static final int EVENT_DEPARTURE_DISPATCHER = 1;
    public static final int EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE = 3;
    public static final int EVENTS_ABANDONMENT_PRIORITY_CLASS_REMOTE = 3;
    public static final int EVENTS_ARRIVE_PRIORITY_CLASS_FIELD = 3;
    public static final int EVENTS_ABANDONMENT_PRIORITY_CLASS_FIELD = 3;


    public static final int ALL_EVENTS_CENTRALINO = EVENT_ARRIVE_CENTRALINO + EVENT_ABANDONMENT_CENTRALINO + SERVERS;
    public static final int ALL_EVENTS_DISPATCHER = EVENT_ARRIVE_DISPATCHER + EVENT_DEPARTURE_DISPATCHER;
    public static final int ALL_EVENTS_REMOTE = EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + EVENTS_ABANDONMENT_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI;
    public static final int ALL_EVENTS_FIELD = EVENTS_ARRIVE_PRIORITY_CLASS_FIELD + EVENTS_ABANDONMENT_PRIORITY_CLASS_FIELD + SERVERS_FIELD_STD +SERVERS_FIELD_SPECIAL;


    public static final int SERVICE_TIME_FIELD = 7200;
    public static final double CENTRALINO_MU_PARAM_LOGNORMAL = 5.97;
    public static final double CENTRALINO_SIGMA_PARAM_LOGNORMAL = 0.02761;
    public static final double REMOTE_MU_PARAM_LOGNORMAL = 6.263;
    public static final double REMOTE_SIGMA_PARAM_LOGNORMAL = 0.025;


    public static final double PATIENCE_LOW_REMOTO = 300;
    public static final double PATIENCE_MEDIUM_REMOTO = 240;
    public static final double PATIENCE_HIGH_REMOTO = 180;
    public static final double PATIENCE_LOW_FIELD = 172800;
    public static final double PATIENCE_MEDIUM_FIELD = 86400;
    public static final double PATIENCE_HIGH_FIELD = 43200;
    public static final double PATIENCE_CENTRALINO = 480;

}
