package control;


import model.FasciaOraria;
import utils.Rngs;
import utils.Rvms;

import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
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
    static double STOP    = 61200.0;        /* terminal (close the door) time */ //dalle 7 alle 24 in sec 61200.0;
    static int    SERVERS = 4;              /* number of servers              */
    static int    SERVERS_REMOTI = 4;
    static int    SERVERS_FIELD_STD = 4;
    static int    SERVERS_FIELD_SPECIAL = 1;

    static double sarrival = START;

    static double [] percentuali = {0.00,    0.10,    2.20,    3.30,    4.20,    4.70,    4.90,    4.90,    4.80,    4.60,    4.20,    3.80,    3.50,    3.50,    3.70,    3.60,    3.50,    3.60,    3.70,    3.90,    4.00,    4.10,    4.10,    3.90,    3.40,    2.70,    1.80,    1.70,    1.50,    1.10,    0.60,    0.30,    0.00,    0.00};
    static List<FasciaOraria> fasce = new ArrayList<>();



    public static void main(String[] args) {

        long   number = 0;             /* number in the node                 */
        long   numberDispatcher = 0;   /* number in dispathcer               */
        int    e;                      /* next event index                   */
        int    s;                      /* server index                       */
        long   index  = 0;             /* used to count processed jobs       */
        long   indexRemoto = 0;
        long   indexField=   0;        /* quanti job sono partiti da on field*/
        long   abandon = 0;
        double area   = 0.0;           /* time integrated number in the node */
        double service;
        double dispatched = 0;         /* number of dispatched tickets       */
        long remoto = 0; //index remoto
        long field = 0; //index on field
        long abandonRH = 0;
        long abandonRM = 0;
        long abandonRL = 0;
        long abandonFH = 0;
        long abandonFM = 0;
        long abandonFL = 0;

        Msq m = new Msq();
        Rngs r = new Rngs();
        r.plantSeeds(123456789);

        List<Double> abandons = new ArrayList<Double>(); //lista abbandoni
        List<Double> abandonsRH = new ArrayList<Double>(); //lista abbandoni remoto high priority
        List<Double> abandonsRM = new ArrayList<Double>(); //lista abbandoni remoto medium priority
        List<Double> abandonsRL = new ArrayList<Double>(); //lista abbandoni remoto low priority
        List<Double> abandonsFH = new ArrayList<Double>(); //lista abbandoni on field high priority
        List<Double> abandonsFM = new ArrayList<Double>(); //lista abbandoni on field medium priority
        List<Double> abandonsFL = new ArrayList<Double>(); //lista abbandoni on field low priority


        for(int f = 0; f<34; f++){ //sono 34 fasce orarie da mezz'ora
            FasciaOraria fo = new FasciaOraria(percentuali[f], 10958, 0 + 1800*f, 1800*(f+1)-1);
            fasce.add(fo); //popolo array fasce orarie dinamicamente
        }

        MsqEvent [] event = new MsqEvent [SERVERS + 2 + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3]; //il secondo + 2 indica gli eventi di arrivo e completamento per il dispatcher
        MsqSum [] sum = new MsqSum [SERVERS + 2 + 2 +3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3]; //14 perchè 3 arrivi + 3 arrivi + 3 abb + 3 abb+ 1 compl + 1 comp
        for (s = 0; s < SERVERS + 2 + 2 +3 + 3 + SERVERS_REMOTI+ 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3; s++) { //prima era +1, ne ho messo un altro per gli abbandoni
            event[s] = new MsqEvent();
            sum [s]  = new MsqSum();
        }
        //l'evento messo in posizione 1 è l'evento di abbandono dalla coda

        MsqT t = new MsqT();

        t.current    = START;
        event[0].t   = m.getArrival(r, t.current);
        event[0].x   = 1;
        for (s = 1; s < SERVERS + 2 + 2 +3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3; s++) { //messo il + 2 perchè ho aggiunto il dispatcher e +14 per i due centri dei guasti?
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }


//cambiata condizione while
        while ((event[0].x != 0) || (number + numberDispatcher + remoto + field != 0)) {

            //System.out.println("number is "+number);
            System.out.println("stato server con number a: " + number + " e dispatcher number a: " + numberDispatcher +
                    " remoto a: " + remoto + " on field a: " + field);
            System.out.println("abbandoni: "+abandons);
            System.out.println("abbandoni alta remote: "+abandonsRH);
            System.out.println("abbandoni media remote: "+abandonsRM);
            System.out.println("abbandoni bassa remote: "+abandonsRL);
            System.out.println("abbandoni alta field: "+abandonsFH);
            System.out.println("abbandoni media field: "+abandonsFM);
            System.out.println("abbandoni bassa field: "+abandonsFL);

            for(int i = 2; i<SERVERS+4; i++) {

                System.out.println(i+ " - " +event[i].x + " time: " + event[i].t);
            }
            for(int i = 8; i<11; i++) {

                System.out.println("arrivi alle code di priorità "+ " - " +event[i].x + " time: " + event[i].t);
            }

            for(int i = SERVERS+7; i<SERVERS+7+SERVERS_REMOTI; i++) {

                System.out.println(i+ " - " +event[i].x + " time: " + event[i].t);
            }

            for(int i = 2 + SERVERS + 2 +3 +3 +SERVERS_REMOTI + 3; i<2 + SERVERS + 2 +3 +3 +SERVERS_REMOTI + 3 + SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL +1; i++) {

                System.out.println(i+ " - " +event[i].x + " time: " + event[i].t);
            }

            if(field < 0){
                break;
            }
            //sarebbe meglio farlo dentro gli arrivi //todo
            if(!abandons.isEmpty()){

                event[1].t = abandons.get(0);
                event[1].x = 1;
            }
            else{
                event[1].x = 0;
            }

            if(!abandonsRH.isEmpty()){
                int minIndex = abandonsRH.indexOf(Collections.min(abandonsRH));
                event[SERVERS + 7 + SERVERS_REMOTI + 0].t = abandonsRH.get(0);
                event[SERVERS + 7 + SERVERS_REMOTI + 0].x = 1;
            }
            else{
                event[SERVERS + 7 + SERVERS_REMOTI + 0].x = 0;
            }
            if(!abandonsRM.isEmpty()){
                event[SERVERS + 7 + SERVERS_REMOTI + 1].t = abandonsRM.get(0);
                event[SERVERS + 7 + SERVERS_REMOTI + 1].x = 1;
            }
            else{
                event[SERVERS + 7 + SERVERS_REMOTI + 1].x = 0;
            }
            if(!abandonsRL.isEmpty()){
                event[SERVERS + 7 + SERVERS_REMOTI + 2].t = abandonsRL.get(0);
                event[SERVERS + 7 + SERVERS_REMOTI + 2].x = 1;
            }
            else{
                event[SERVERS + 7 + SERVERS_REMOTI + 2].x = 0;
            }

            if(!abandonsFH.isEmpty()){
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1].t = abandonsFH.get(0);
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1].x = 1;
            }
            else{
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1].x = 0;
            }
            if(!abandonsFM.isEmpty()){
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2].t = abandonsFM.get(0);
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2].x = 1;
            }
            else{
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2].x = 0;
            }
            if(!abandonsFL.isEmpty()){
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3].t = abandonsFL.get(0);
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3].x = 1;
            }
            else{
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3].x = 0;
            }



            e         = m.nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            area     += (t.next - t.current) * number;     /* update integral  */
            t.current = t.next;                            /* advance the clock*/

            System.out.println("t current is: "+t.current);



            if (e == 0) {                                  /* process a callcenter arrival*/
                System.out.println("entrato in arrivals callcenter");
                number++;
                event[0].t        = m.getArrival(r, t.current);
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
                    Collections.sort(abandons);
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

            else if(e == SERVERS + 2){ //arrivo dispatcher
                //numberDispatcher++; //incremento contatore
                System.out.println("entrato in arrivo dispatcher");
                event[SERVERS + 2].x = 0; //non può esserci un altro arrivo al dispatcher senza che ci
                //sia un' altra partenza dal centralino
                //se number dispatcher è >= 1 dopo l'incremento, vuol dire che ho il server idle
                if (numberDispatcher == 1) {
                    //e quindi faccio il servizio = spawn evento completamento dispatcher
                    sum[SERVERS + 3].served++;
                    sum[SERVERS + 3].service += 5; //il tempo di servizio è discreto a 5 secondi
                    event[SERVERS + 3].t = t.current + 5;
                    event[SERVERS + 3].x = 1; //completamento dispatcher eleggibile per next event
                }

            }

            else if(e == SERVERS + 3){//departure dispatcher
                numberDispatcher--;
                dispatched++;
                System.out.println("entrato in partenze dispatcher");
                double rnd = Math.random(); //mi dice se il job va on field oppure va remoto
                double priority = Math.random();
                if(rnd<-1.0){ //in remoto era 0.8
                    System.out.println("entrato ramo remoto");
                    if(priority < 0.0095){ //alta priorità
                        event[SERVERS + 6].x = 1;
                        event[SERVERS + 6].t = t.current;

                    }
                    else if(priority < 0.1075){ //media priorità
                        event[SERVERS + 5].x = 1;
                        event[SERVERS + 5].t = t.current;
                    }
                    else{ //bassa priorità
                        event[SERVERS + 4].x = 1;
                        event[SERVERS + 4].t = t.current;
                    }
                    remoto++;
                }
                else{//on field
                    System.out.println("entrato ramo on field");
                    if(priority < 0.0095){ //alta priorità
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3].x = 1;
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3].t = t.current;

                    }
                    else if(priority < 0.1075){ //media priorità
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 2].x = 1;
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 2].t = t.current;
                    }
                    else{ //bassa priorità
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 1].x = 1;
                        event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 1].t = t.current;
                    }
                    field++;
                    System.out.println("ho incrementato field a: " + field);
                }



                if (numberDispatcher >= 1) { //se ho coda
                    //riprocesso un servizio spawnando un nuovo evento di completamento
                    sum[SERVERS + 3].served++;
                    sum[SERVERS + 3].service += 5; //il tempo di servizio è discreto a 5 secondi
                    event[SERVERS + 3].t = t.current + 5;


                }
                else{
                    event[SERVERS + 3].x = 0; // se non c'è coda il prossimo evento non può certamente essere un completamento nel dispathcher
                }


            }

            else if(e == SERVERS + 4){ //arrivo coda priorità bassa - remoto
                //remoto++;
                System.out.println("entrato in arrivo coda bassa priorità");
                event[SERVERS + 4].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI && abandonsRH.isEmpty() && abandonsRM.isEmpty()){
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneRemoto(event);
                    System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(r) + t.current;
                    abandonsRL.add(at);
                    Collections.sort(abandonsRL);
                }

            }

            else if(e == SERVERS + 5){ //arrivo coda priorità media - remoto
                //remoto++;
                System.out.println("entrato in arrivo coda media priorità");
                event[SERVERS + 5].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI && abandonsRH.isEmpty()){
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneRemoto(event);
                    System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                else{
                    //genero abbandono se un job sta in coda
                    System.out.println("genero abbandono media");
                    double at = m.getAbandon(r) + t.current;
                    abandonsRM.add(at);
                    Collections.sort(abandonsRM);
                }

            }

            else if(e == SERVERS + 6){ //arrivo coda priorità alta - remoto
                //remoto++;
                System.out.println("entrato in arrivo coda alta priorità");
                event[SERVERS + 6].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI){
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneRemoto(event);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                if(remoto > SERVERS_REMOTI){
                    System.out.println("genero abbandono alta");
                    double at = m.getAbandon(r) + t.current;
                    abandonsRH.add(at);
                    Collections.sort(abandonsRH);
                }

            }

            else if(e == SERVERS + 7 + SERVERS_REMOTI){ //abbandono coda alta priorità remoto
                System.out.println("entrato in abbandono coda alta priorità");
                remoto--;
                abandonRH++;
                abandonsRH.remove(0); //tolgo job dalla lista
                if(abandonsRH.isEmpty()){
                    event[SERVERS + 7 + SERVERS_REMOTI].x = 0;
                }
            }

            else if(e == SERVERS + 7 + SERVERS_REMOTI + 1){ //abbandono coda media priorità remoto
                System.out.println("entrato in abbandono coda media priorità");
                remoto--;
                abandonRM++;
                abandonsRM.remove(0); //tolgo job dalla lista
                if(abandonsRM.isEmpty()){
                    event[SERVERS + 7 + SERVERS_REMOTI + 1].x = 0;
                }
            }

            else if(e == SERVERS + 7 + SERVERS_REMOTI + 2){ //abbandono coda bassa priorità remoto
                System.out.println("entrato in abbandono coda bassa priorità");
                remoto--;
                abandonRL++;
                abandonsRL.remove(0); //tolgo job dalla lista
                if(abandonsRL.isEmpty()){
                    event[SERVERS + 7 + SERVERS_REMOTI + 2].x = 0;
                }
            }

            else if(e >= SERVERS + 7 && e < SERVERS + 7 + SERVERS_REMOTI){//completamento server remoto
                System.out.println("entrato in completamento server remoto");
                indexRemoto++;
                remoto--;
                s = e;
                //cleanup abbandoni
                if(!abandonsRH.isEmpty() && abandonsRH.get(0) < t.current) { //minore o minore uguale?
                    abandonsRH.remove(0);
                }
                if(!abandonsRM.isEmpty() && abandonsRM.get(0) < t.current) { //minore o minore uguale?
                    abandonsRM.remove(0);
                }
                if(!abandonsRL.isEmpty() && abandonsRL.get(0) < t.current) { //minore o minore uguale?
                    abandonsRL.remove(0);
                }

                //servizio se c'è coda
                if(remoto >= SERVERS_REMOTI){
                    service         = m.getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    if(!abandonsRH.isEmpty()){
                        abandonsRH.remove(0);
                    }
                    else if(!abandonsRM.isEmpty()){
                        abandonsRM.remove(0);
                    }
                    else if(!abandonsRL.isEmpty()){
                        abandonsRL.remove(0);
                    }
                    //se sto schedulando un servizio, è giusto togliere un job dalle code di abbandono:
                    //viene fatto in quest'ordine per garantire la priorità
                }
                else {
                    event[s].x = 0;
                }


            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 1){ //arrivo coda priorità bassa - on field
                System.out.println("entrato in arrivo coda bassa priorità on field");
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 1].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty() && abandonsFM.isEmpty()){ //la coda di priorità bassa vede solo i server standard
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneFieldStd(event);
                    System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(r) + t.current;
                    abandonsFL.add(at);
                    Collections.sort(abandonsFL);
                }

            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 2){ //arrivo coda priorità media - on field
                System.out.println("entrato in arrivo coda bassa priorità on field");
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 2].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty()){ //la coda di priorità media vede solo i server standard
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneFieldStd(event);
                    System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(r) + t.current;
                    abandonsFM.add(at);
                    Collections.sort(abandonsFM);
                }

            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3){ //arrivo coda priorità alta - on field
                System.out.println("entrato in arrivo coda bassa priorità on field");
                event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL){ //la coda di priorità alta vede i server standard + quelli dedicati
                    //processiamo i servizi
                    service = m.getService(r); //cambiare!
                    s = m.findOneFieldSpecial(event);
                    System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(r) + t.current;
                    abandonsFH.add(at);
                    Collections.sort(abandonsFH);
                }

            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1){ //abbandono coda bassa priorità field
                System.out.println("entrato in abbandono coda bassa priorità field");
                field--;
                System.out.println("ho decrementato field a: " + field);
                abandonFL++;
                abandonsFL.remove(0); //tolgo job dalla lista
                if(abandonsFL.isEmpty()){
                    event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1].x = 0;
                }
            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2){ //abbandono coda media priorità field
                System.out.println("entrato in abbandono coda media priorità field");
                field--;
                System.out.println("ho decrementato field a: " + field);
                abandonFM++;
                abandonsFM.remove(0); //tolgo job dalla lista
                if(abandonsFM.isEmpty()){
                    event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2].x = 0;
                }
            }

            else if(e == SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3){ //abbandono coda alta priorità field
                System.out.println("entrato in abbandono coda alta priorità field");
                field--;
                System.out.println("ho decrementato field a: " + field);
                abandonFH++;
                abandonsFH.remove(0); //tolgo job dalla lista
                if(abandonsFH.isEmpty()){
                    event[SERVERS + 4 + 2 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 3].x = 0;
                }
            }

            else if(e >= 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 && e < 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL){//completamento server on field
                System.out.println("entrato in completamento server on field");
                indexField++;
                field--;
                System.out.println("ho decrementato field a: " + field);
                s = e;
                //cleanup abbandoni
                if(!abandonsFH.isEmpty() && abandonsFH.get(0) < t.current) { //minore o minore uguale?
                    abandonsFH.remove(0);
                }
                if(!abandonsFM.isEmpty() && abandonsFM.get(0) < t.current) { //minore o minore uguale?
                    abandonsFM.remove(0);
                }
                if(!abandonsFL.isEmpty() && abandonsFL.get(0) < t.current) { //minore o minore uguale?
                    abandonsFL.remove(0);
                }

                //servizio se c'è coda - comportamento server speciali
                if(s>= 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 && s<  2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL ){
                    if(!abandonsFH.isEmpty()){
                        abandonsFH.remove(0); //prendo un job dalla coda ad alta priorità
                        service         = m.getService(r);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t      = t.current + service;
                    }
                }
                else if(s>= 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL && s <  2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 1){

                    service         = m.getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    if(!abandonsFH.isEmpty()){
                        abandonsFH.remove(0);
                    }
                    else if(!abandonsFM.isEmpty()){
                        abandonsFM.remove(0);
                    }
                    else if(!abandonsFL.isEmpty()){
                        abandonsFL.remove(0);
                    }
                    //se sto schedulando un servizio, è giusto togliere un job dalle code di abbandono:
                    //viene fatto in quest'ordine per garantire la priorità
                }
                else {
                    event[s].x = 0;
                }


            }


            else {                                         /* process a callcenter departure */
                System.out.println("entrato in departures");
                index++;                                     /* from server s       */
                number--;
                s                 = e; //indice next event = server id
                event[SERVERS + 2].t = t.current; //invio ticket al dispatcher
                event[SERVERS + 2].x = 1; //arrivo dispatcher elegibile per next event
                numberDispatcher++; //incremento contatore
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

        for (s = 2; s <= SERVERS+1; s++) {      /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */
        }
        /*
        for (s = SERVERS + 7; s < SERVERS+7+SERVERS_REMOTI; s++) {
            area -= sum[s].service;
        }

         */


        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(area / t.current));
        System.out.println("  abandons ........... =   " + abandon);
        System.out.println("  il dispathcer ha servito ticket =   " + dispatched + " per un tempo" +
                " totale di: " + sum[SERVERS + 3].service);
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 2; s <= SERVERS+1; s++) {
            //System.out.println(s +" "+sum[s].service + " " +t.current);
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }

        for (s = SERVERS + 7; s <= SERVERS+7+SERVERS_REMOTI; s++) {
            //System.out.println(s +" "+sum[s].service + " " +t.current);
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)indexRemoto));
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

    double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        r.selectStream(0);
        int index = FasciaOrariaController.fasciaOrariaSwitch(fasce, currentTime);


        //sarrival += exponential(fasce.get(index).getMediaPoisson(), r); //deve diventare poissoniana
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
        while (i < SERVERS+3+3+3+SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD + 2) {      //messo +1, ora +3 /* now, check the others to find which  */
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

    int findOneRemoto(MsqEvent [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = SERVERS + 7;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < SERVERS + 7 + SERVERS_REMOTI-1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    int findOneFieldStd(MsqEvent [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD -1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    int findOneFieldSpecial(MsqEvent [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD -1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }




}
