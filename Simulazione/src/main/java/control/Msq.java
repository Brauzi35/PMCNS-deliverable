package control;

import model.FasciaOraria;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import utils.Rngs;
import utils.Rvms;
import utils.Timestamp;
import utils.WriteDoubleListToFile;

import static model.SimulationValues.*;

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

    double sarrival = START;
    static List<FasciaOraria> fasce = new ArrayList<>();

    static List<Double> visiteCentralino = new ArrayList<>();
    static List<Double> utilCentralino = new ArrayList<>();
    static List<Integer> visiteDispatcher = new ArrayList<>();
    static List<Integer> visiteRemoto = new ArrayList<>();
    static List<Integer> visiteOnField = new ArrayList<>();



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
        double areaDispatcher = 0.0;
        double areaRemoto = 0.0;
        double areaField = 0.0;
        double areaRemotoQueue = 0.0;
        double areaFieldQueue = 0.0;
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
        long contrattoRescissoField = 0;
        long contrattoRescissoRemoto = 0;
        long feedbackField = 0;
        long feedbackRemoto = 0;

        double previousDispTime = 0.0;

        int idx = 1;

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



        for(int f = 0; f<31; f++){ //sono 34 fasce orarie da mezz'ora
            FasciaOraria fo = new FasciaOraria(PERCENTUALI[f], 10958, 0 + 1800*f, 1800*(f+1)-1);
            fasce.add(fo); //popolo array fasce orarie dinamicamente
        }

        MsqEvent [] event = new MsqEvent [ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + ALL_EVENTS_FIELD];
        MsqSum [] sum = new MsqSum [ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + ALL_EVENTS_FIELD];
        for (s = 0; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + ALL_EVENTS_FIELD; s++) {
            event[s] = new MsqEvent();
            sum [s]  = new MsqSum();
        }

        MsqT t = new MsqT();

        t.current    = START;
        event[0].t   = m.getArrival(r, t.current, idx);
        event[0].x   = 1;

        for (s = 1; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + ALL_EVENTS_FIELD; s++) { //messo il + 2 perchè ho aggiunto il dispatcher e +14 per i due centri dei guasti?
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        Timestamp timestamp = new Timestamp(); //classe che tiene i valori del primo e ultimo completamento per ogni centro

//cambiata condizione while
        while ((event[0].x != 0) || (number + numberDispatcher + remoto + field != 0)) {



            visiteCentralino.add(area/index);
            //sarebbe meglio farlo dentro gli arrivi //todo
            if(!abandons.isEmpty()){

                event[1].t = abandons.get(abandons.indexOf(Collections.min(abandons)));
                event[1].x = 1;
            }
            else{
                event[1].x = 0;
            }

            if(!abandonsRH.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI].t = abandonsRH.get(abandonsRH.indexOf(Collections.min(abandonsRH)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI].x = 0;
            }
            if(!abandonsRM.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 1].t = abandonsRM.get(abandonsRM.indexOf(Collections.min(abandonsRM)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 1].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 1].x = 0;
            }
            if(!abandonsRL.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 2].t = abandonsRL.get(abandonsRL.indexOf(Collections.min(abandonsRL)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 2].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI + 2].x = 0;
            }

            if(!abandonsFH.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2].t = abandonsFH.get(abandonsFH.indexOf(Collections.min(abandonsFH)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2].x = 0;
            }
            if(!abandonsFM.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1].t = abandonsFM.get(abandonsFM.indexOf(Collections.min(abandonsFM)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1].x = 0;
            }
            if(!abandonsFL.isEmpty()){
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD].t = abandonsFL.get(abandonsFL.indexOf(Collections.min(abandonsFL)));
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD].x = 1;
            }
            else{
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD].x = 0;
            }



            e         = m.nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            area     += (t.next - t.current) * number;     /* update integral  */
            areaDispatcher += (t.next - t.current) * numberDispatcher;

            //System.out.println("Area dispatcher: " + areaDispatcher + " con tnext: " + t.next + " e tcurr: " + t.current);
            areaRemoto += (t.next - t.current) * remoto;
            if(remoto > SERVERS_REMOTI){
                areaRemotoQueue += (t.next - t.current) * (remoto - SERVERS_REMOTI);
            }
            areaField += (t.next - t.current) * field;
            if(field > SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL){
                areaFieldQueue += (t.next - t.current) * (field - (SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL));
            }
            t.current = t.next;                            /* advance the clock*/

            //System.out.println("t current is: "+t.current);



            if (e == EVENT_ARRIVE_CENTRALINO-1) {                                  /* process a callcenter arrival*/
                //System.out.println("entrato in arrivals callcenter");
                number++;
                event[0].t        = m.getArrival(r, t.current, idx);
                if (event[0].t > STOP)
                    event[0].x      = 0; //close the door
                if (number <= SERVERS) {
                    service         = m.getServiceCentralino(r, idx);
                    s               = m.findOne(event); //id server
                    //System.out.println("s is " + s);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service; //tempo di completamento
                    event[s].x      = 1; //eleggibile per il next event
                }
                if (number > SERVERS){
                    //genero abbandono se un job sta in coda
                    //System.out.println("arrivo di un job messo in coda e numero di job nel nodo = " + number);
                    double at = m.getAbandon(PATIENCE_CENTRALINO, r, idx) + t.current;
                    abandons.add(at);
                }
            }

            else if(e == EVENT_ABANDONMENT_CENTRALINO) { //processo abbandono callcenter
                //index++;
                //System.out.println("entrato in abandons");
                number--;
                abandon++;
                abandons.remove(abandons.indexOf(Collections.min(abandons))); //tolgo job dalla lista
                if(abandons.isEmpty()){
                    event[1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO){ //arrivo dispatcher
                //numberDispatcher++; //incremento contatore
                //System.out.println("entrato in arrivo dispatcher");
                event[ALL_EVENTS_CENTRALINO].x = 0; //non può esserci un altro arrivo al dispatcher senza che ci
                //sia un' altra partenza dal centralino
                //se number dispatcher è >= 1 dopo l'incremento, vuol dire che ho il server idle
                if (numberDispatcher == 1) {
                    //e quindi faccio il servizio = spawn evento completamento dispatcher
                    sum[ALL_EVENTS_CENTRALINO+EVENT_ARRIVE_DISPATCHER].served++;
                    sum[ALL_EVENTS_CENTRALINO+EVENT_ARRIVE_DISPATCHER].service += DISPATCHER_SERVICE_TIME; //il tempo di servizio è discreto a 5 secondi
                    event[ALL_EVENTS_CENTRALINO+EVENT_ARRIVE_DISPATCHER].t = t.current + DISPATCHER_SERVICE_TIME;
                    event[ALL_EVENTS_CENTRALINO+EVENT_ARRIVE_DISPATCHER].x = 1; //completamento dispatcher eleggibile per next event
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+EVENT_ARRIVE_DISPATCHER){//departure dispatcher

                if(timestamp.primoComplDisp == 0){
                    timestamp.primoComplDisp = t.current;
                }

                numberDispatcher--;
                dispatched++;
                //System.out.println("entrato in partenze dispatcher");
                r.selectStream(10 + idx);
                double rnd = r.random(); //mi dice se il job va on field oppure va remoto
                double priority = r.random();
                if(rnd<REMOTE_PROBABILITY){ //in remoto era 0.8
                    //System.out.println("entrato ramo remoto");
                    if(priority < HIGH_PRIORITY_PROBABILITY){ //alta priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + 2].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + 2].t = t.current;

                    }
                    else if(priority < MEDIUM_PRIORITY_PROBABILITY){ //media priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + 1].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + 1].t = t.current;
                    }
                    else{ //bassa priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER].t = t.current;
                    }
                    remoto++;
                }
                else{//on field
                    //System.out.println("entrato ramo on field");

                    if(timestamp.primoArrivoField == 0){
                        timestamp.primoArrivoField = t.current;
                    }

                    if(priority < HIGH_PRIORITY_PROBABILITY){ //alta priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + 2].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + 2].t = t.current;

                    }
                    else if(priority < MEDIUM_PRIORITY_PROBABILITY){ //media priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + 1].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + 1].t = t.current;
                    }
                    else{ //bassa priorità
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE].x = 1;
                        event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE].t = t.current;
                    }
                    field++;
                    //System.out.println("ho incrementato field a: " + field);
                }



                if (numberDispatcher >= 1) { //se ho coda
                    //riprocesso un servizio spawnando un nuovo evento di completamento
                    sum[ALL_EVENTS_CENTRALINO + EVENT_ARRIVE_DISPATCHER].served++;
                    sum[ALL_EVENTS_CENTRALINO + EVENT_ARRIVE_DISPATCHER].service += 5; //il tempo di servizio è discreto a 5 secondi
                    event[ALL_EVENTS_CENTRALINO + EVENT_ARRIVE_DISPATCHER].t = t.current + 5;


                }
                else{
                    event[ALL_EVENTS_CENTRALINO + EVENT_ARRIVE_DISPATCHER].x = 0; // se non c'è coda il prossimo evento non può certamente essere un completamento nel dispathcher
                }

                previousDispTime = t.current;

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER){ //arrivo coda priorità bassa - remoto
                //remoto++;
                //System.out.println("entrato in arrivo coda bassa priorità");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI && abandonsRH.isEmpty() && abandonsRM.isEmpty()){
                    //processiamo i servizi
                    service = m.getServiceRemote(r, idx); //cambiare!
                    s = m.findOneRemoto(event);
                    //System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                else{
                    //genero abbandono se un job sta in coda
                    //System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(PATIENCE_LOW_REMOTO, r, idx) + t.current;
                    abandonsRL.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+1){ //arrivo coda priorità media - remoto
                //remoto++;
                //System.out.println("entrato in arrivo coda media priorità");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+1].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI && abandonsRH.isEmpty()){
                    //processiamo i servizi
                    service = m.getServiceRemote(r, idx); //cambiare!
                    s = m.findOneRemoto(event);

                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                else{
                    //genero abbandono se un job sta in coda
                    //System.out.println("genero abbandono media");
                    double at = m.getAbandon(PATIENCE_MEDIUM_REMOTO, r, idx) + t.current;
                    abandonsRM.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+2){ //arrivo coda priorità alta - remoto
                //remoto++;
                //System.out.println("entrato in arrivo coda alta priorità");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+2].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI){
                    //processiamo i servizi
                    service = m.getServiceRemote(r, idx); //cambiare!
                    s = m.findOneRemoto(event);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                if(remoto > SERVERS_REMOTI){
                    //System.out.println("genero abbandono alta");
                    double at = m.getAbandon(PATIENCE_HIGH_REMOTO, r, idx) + t.current;
                    abandonsRH.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI){ //abbandono coda alta priorità remoto
                //System.out.println("entrato in abbandono coda alta priorità");
                remoto--;
                abandonRH++;
                abandonsRH.remove(abandonsRH.indexOf(Collections.min(abandonsRH))); //tolgo job dalla lista
                if(abandonsRH.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 1){ //abbandono coda media priorità remoto
                //System.out.println("entrato in abbandono coda media priorità");
                remoto--;
                abandonRM++;
                abandonsRM.remove(abandonsRM.indexOf(Collections.min(abandonsRM))); //tolgo job dalla lista
                if(abandonsRM.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 2){ //abbandono coda bassa priorità remoto
                //System.out.println("entrato in abbandono coda bassa priorità");
                remoto--;
                abandonRL++;
                abandonsRL.remove(abandonsRL.indexOf(Collections.min(abandonsRL))); //tolgo job dalla lista
                if(abandonsRL.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 2].x = 0;
                }
            }

            else if(e >= ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE && e < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI){//completamento server remoto
                //System.out.println("entrato in completamento server remoto");

                if(timestamp.primoComplRemoto == 0){
                    timestamp.primoComplRemoto = t.current;
                }

                indexRemoto++;
                remoto--;
                s = e;

                //feedback remote
                r.selectStream(18 + idx);
                double probability = r.random();
                if(probability<GOBACK_PROBABILITY){ //la riparazione non ha sortito l'effetto desiderato
                    double feedback = r.random();
                    if(feedback < LEAVE_PROBABILTY){ //l'utente in questo caso decide di lasciare l'operatore rescindendo il contratto
                        contrattoRescissoRemoto++;
                    }
                    else{
                        feedbackRemoto++; //non lascia la telecom ma perdiamo i soldi della riparazione
                    }
                }

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
                    service         = m.getServiceRemote(r, idx);
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

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE){ //arrivo coda priorità bassa - on field
                //System.out.println("entrato in arrivo coda bassa priorità on field");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty() && abandonsFM.isEmpty()){ //la coda di priorità bassa vede solo i server standard
                    //processiamo i servizi
                    service = m.getServiceField(r, idx); //cambiare!
                    s = m.findOneFieldStd(event);
                    //System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    //System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(PATIENCE_LOW_FIELD, r, idx) + t.current;
                    abandonsFL.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 1){ //arrivo coda priorità media - on field
                //System.out.println("entrato in arrivo coda bassa priorità on field");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 1].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty()){ //la coda di priorità media vede solo i server standard
                    //processiamo i servizi
                    service = m.getServiceField(r, idx); //cambiare!
                    s = m.findOneFieldStd(event);
                    //System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    //System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(PATIENCE_MEDIUM_FIELD, r, idx) + t.current;
                    abandonsFM.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 2){ //arrivo coda priorità alta - on field
                //System.out.println("entrato in arrivo coda bassa priorità on field");
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 2].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL){ //la coda di priorità alta vede i server standard + quelli dedicati
                    //processiamo i servizi
                    service = m.getServiceField(r, idx); //cambiare!
                    s = m.findOneFieldSpecial(event);
                    //System.out.println("s is: " + s);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    //System.out.println("genero abbandono bassa");
                    double at = m.getAbandon(PATIENCE_HIGH_FIELD, r, idx) + t.current;
                    abandonsFH.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD){ //abbandono coda bassa priorità field
                //System.out.println("entrato in abbandono coda bassa priorità field");
                field--;
                //System.out.println("ho decrementato field a: " + field);
                abandonFL++;
                abandonsFL.remove(abandonsFL.indexOf(Collections.min(abandonsFL))); //tolgo job dalla lista
                if(abandonsFL.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1){ //abbandono coda media priorità field
                //System.out.println("entrato in abbandono coda media priorità field");
                field--;
                //System.out.println("ho decrementato field a: " + field);
                abandonFM++;
                abandonsFM.remove(abandonsFM.indexOf(Collections.min(abandonsFM))); //tolgo job dalla lista
                if(abandonsFM.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2){ //abbandono coda alta priorità field
                //System.out.println("entrato in abbandono coda alta priorità field");
                field--;
                //System.out.println("ho decrementato field a: " + field);
                abandonFH++;
                abandonsFH.remove(abandonsFH.indexOf(Collections.min(abandonsFH))); //tolgo job dalla lista
                if(abandonsFH.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2].x = 0;
                }
            }

            else if(e >= ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD && e < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_STD+SERVERS_FIELD_SPECIAL){//completamento server on field
                //System.out.println("entrato in completamento server on field");


                if(timestamp.primoComplField == 0){
                    timestamp.primoComplField = t.current;
                }

                indexField++;
                field--;
                s = e;


                //feedback on field
                r.selectStream(17 + idx);
                double probability = r.random();
                if(probability<GOBACK_PROBABILITY){ //la riparazione non ha sortito l'effetto desiderato
                    double feedback = r.random();
                    if(feedback < LEAVE_PROBABILTY){ //l'utente in questo caso decide di lasciare l'operatore rescindendo il contratto
                        contrattoRescissoField++;
                    }
                    else{
                        feedbackField++; //non lascia la telecom ma perdiamo i soldi della riparazione
                    }
                }


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
                int size = abandonsFH.size() + abandonsFL.size() + abandonsFM.size();

                //servizio se c'è coda - comportamento server speciali
                if(s>= 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 && s<  2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL && !abandonsFH.isEmpty()){
                    //if(!abandonsFH.isEmpty()){

                    abandonsFH.remove(0); //prendo un job dalla coda ad alta priorità
                    service         = m.getServiceField(r, idx);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    // }
                }


                else if(s>= ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL && s <  ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1 && size!=0){
                    service         = m.getServiceField(r, idx);
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
                //System.out.println("entrato in departures");

                if (timestamp.primoComplCentralino == 0){
                    timestamp.primoComplCentralino = t.current;
                }
                System.out.println(t.current + " " + index);
                index++;                                     /* from server s       */
                number--;
                s                 = e; //indice next event = server id
                event[ALL_EVENTS_CENTRALINO].t = t.current; //invio ticket al dispatcher
                event[ALL_EVENTS_CENTRALINO].x = 1; //arrivo dispatcher elegibile per next event
                numberDispatcher++; //incremento contatore
                if(!abandons.isEmpty()) {
                    abandons.remove(0);
                }


                if (number >= SERVERS) { //se ho coda
                    service         = m.getServiceCentralino(r, idx);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;


                }
                else {
                    event[s].x = 0;
                }
            }


            //System.out.println("FINE ITERAZIONE\n\n");
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        double tFinalCentralino = 0.0;
        double mediaCentralino = 0.0;
        for(s = 2; s <= SERVERS+1; s++){
            mediaCentralino += event[s].t;
            if(event[s].t > tFinalCentralino){
                tFinalCentralino = event[s].t;
            }
        }
        mediaCentralino = mediaCentralino/SERVERS;

        System.out.println(mediaCentralino + " media centralino ");

        double realTimeCentralino = tFinalCentralino - timestamp.primoComplCentralino;

        System.out.println(realTimeCentralino + " real time centralino");

        System.out.println("\nfor " + index + " jobs the CENTRALINO statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[0].t / index));
        System.out.println("  avg wait (response time) ........... =   " + f.format(area / index));
        System.out.println("  avg # in centralino ...... =   " + f.format(area / realTimeCentralino)); // diviso ultimo - primo

        for (s = 2; s <= SERVERS+1; s++) {      /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */
        }

        double realTimeDispatcher = event[2+SERVERS+1].t-timestamp.primoComplDisp; //ultimo - primo completamento dispatcher
        System.out.println("Real time dispatcher: " + realTimeDispatcher);


        System.out.println("\nfor " + dispatched + " jobs the DISPATCHER statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(realTimeDispatcher / dispatched));
        System.out.println("  avg wait (response time)........... =   " + f.format(areaDispatcher / dispatched));
        System.out.println("  avg # in node ...... =   " + f.format(areaDispatcher / realTimeDispatcher )); //event[2+SERVERS+1].t


        areaDispatcher -= sum[ALL_EVENTS_CENTRALINO+1].service;


        double tFinalRemoto = 0.0;
        double mediaRemoto = 0.0;
        for(s = SERVERS + 7; s < SERVERS+7+SERVERS_REMOTI; s++){
            mediaRemoto += event[s].t;
            if(event[s].t > tFinalRemoto){
                tFinalRemoto = event[s].t;
            }
        }
        double lastArrivalRemoto = 0.0;
        for(s = 2 + SERVERS + 2; s<= 2 + SERVERS + 2 + 2; s++){
            if(event[s].t> lastArrivalRemoto){
                lastArrivalRemoto = event[s].t;
            }
        }
        mediaRemoto = mediaRemoto/SERVERS_REMOTI;

        System.out.println("\nfor " + indexRemoto + " jobs the REMOTO statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format((lastArrivalRemoto-timestamp.primoComplDisp) / indexRemoto));
        System.out.println("  avg wait (service time) ........... =   " + f.format(areaRemoto / indexRemoto));
        System.out.println("  avg # in node ...... =   " + f.format(areaRemoto / (tFinalRemoto-timestamp.primoComplRemoto)));
        System.out.println("area remoto queue= " + areaRemotoQueue + " acchittamento: " + f.format(areaRemotoQueue /  (tFinalRemoto-timestamp.primoComplRemoto)));


        double tFinalField = 0.0;
        double mediaField = 0.0;
        for(s = ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD; s++){
            mediaField += event[s].t;
            if(event[s].t > tFinalField){
                tFinalField = event[s].t;
            }
        }
        mediaField = mediaField/SERVERS_FIELD_STD+SERVERS_FIELD_SPECIAL;

        double lastArrivalField = 0.0;
        int i = 0;
        for(s = ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE; s< ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD; s++){
            i++;
            if(event[s].t> lastArrivalField){
                lastArrivalField = event[s].t;
            }
        }

        System.out.println("\nfor " + indexField + " jobs the FIELD statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format((lastArrivalField - timestamp.primoArrivoField) / indexField));
        System.out.println("  avg wait ........... =   " + f.format((areaField) / indexField));
        System.out.println("  avg # in node ...... =   " + f.format(areaField / (tFinalField - timestamp.primoComplField))); //9185 primo cmple

        System.out.println("area remoto queue= " + areaFieldQueue + " acchittamento: " + f.format(areaFieldQueue /  (tFinalField - 9185)));

        /*for (s = 2; s <= SERVERS+1; s++) {
            area -= sum[s].service;
        }*/
        for (s = ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD; s++) {
            areaField -= sum[s].service;
        }

        /*WriteDoubleListToFile writeDoubleListToFile = new WriteDoubleListToFile();
        writeDoubleListToFile.scrivi(visiteCentralino, "tempiRispostaPerFascia");
        writeDoubleListToFile.scrivi(utilCentralino, "utilCentralinoTotal");

        GraphControllerMsq.createGraph(("tempiRispostaPerFascia"));
        //GraphControllerMsq.createGraph("utilCentralinoTotal");
*/

        System.out.println("  \navg # queue dispatcher: " + sum[SERVERS + 3].service/realTimeDispatcher);
        System.out.println("  area disp: " + areaDispatcher);

        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(areaDispatcher / realTimeDispatcher));
        System.out.println("  abandons ........... =   " + abandon);
        System.out.println("  perc abandons centralino..=   " + (abandon/(dispatched+abandon)));
        System.out.println("  abandons in FH........... =   " + abandonFH);
        System.out.println("  abandons in FM........... =   " + abandonFM);
        System.out.println("  abandons in FL........... =   " + abandonFL);
        System.out.println("  abandons in RH........... =   " + abandonRH);
        System.out.println("  abandons in RM........... =   " + abandonRM);
        System.out.println("  abandons in RL........... =   " + abandonRL);
        System.out.println("  il dispathcer ha servito ticket =   " + dispatched + " per un tempo" +
                " totale di: " + sum[SERVERS + 3].service);
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        //il tempo dell'ultima uscita da un centralino

        /*for (s = 2; s <= SERVERS+1; s++) {
            //System.out.println(s +" "+sum[s].service + " " +t.current);
            System.out.print("       " + s + "          " + g.format(sum[s].service / tFinalCentralino) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }
        int disp = 2+SERVERS +1;
        double temp = event[disp].t-391.0; //perché devo togliere il tempo del primo arrivo al dispatcher
        System.out.print("       " + disp + "          " + g.format(sum[2+SERVERS +1].service / temp) + "            ");
        System.out.println(f.format(sum[2+SERVERS +1].service / sum[2+SERVERS +1].served) + "         " + g.format(sum[2+SERVERS +1].served / (double)dispatched));



        for (s = SERVERS + 7; s < SERVERS+7+SERVERS_REMOTI; s++) {
            //System.out.println(s +" "+sum[s].service + " " +t.current);
            System.out.print("       " + s + "          " + g.format(sum[s].service / tFinalRemoto) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)indexRemoto));
        }

        for (s =2 + SERVERS + 2 + 3 + SERVERS_REMOTI + 3 + 3; s< 2 + SERVERS + 2 + 3 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL; s++){
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)indexField));
        }

        for (s =2 + SERVERS + 2 + 3 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL; s< 2 + SERVERS + 2 + 3 + SERVERS_REMOTI + 3 + 3 + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD; s++){
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)indexField));
        }


        System.out.println("media poisson:  " + fasce.get(1).getMediaPoisson());

        System.out.println(""); */
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

    double getAbandon(double patience, Rngs r, int streamIndex){

        r.selectStream(1 + streamIndex);
        //patience = 999999999;
        return (-patience * Math.log(1.0 - r.random())); //ok abbandoni reali
        //return 999999999; no abbandoni
    }

    double getArrival(Rngs r, double currentTime, int streamIndex) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        r.selectStream(0 + streamIndex);
        int index = FasciaOrariaController.fasciaOrariaSwitch(fasce, currentTime);

        Rvms rvms = new Rvms();

        sarrival += rvms.idfPoisson(fasce.get(index).getMediaPoisson(), r.random()); //deve diventare poissoniana
        //System.out.println("media poisson:  " + fasce.get(index).getMediaPoisson());
        //sarrival += exponential(2.0, r);
        //sarrival += rvms.idfPoisson(3.26, r.random());
        return (sarrival);
    }


    double getServiceCentralino(Rngs r, int streamIndex) {
        /* ------------------------------
         * generate the next service time, with rate 1/6
         * ------------------------------
         */
        Rvms rvms = new Rvms();
        r.selectStream(1 + streamIndex);
        //return (uniform(2.0, 10.0, r));
        return rvms.idfLogNormal(CENTRALINO_MU_PARAM_LOGNORMAL, CENTRALINO_SIGMA_PARAM_LOGNORMAL, r.random());
    }

    double getServiceField(Rngs r, int streamIndex){
        // Esponenziale
        r.selectStream(20 + streamIndex);
        double m = SERVICE_TIME_FIELD;  // 3h = 10800s
        return (-m * Math.log(1.0 - r.random()));
    }

    double getServiceRemote(Rngs r, int streamIndex){
        Rvms rvms = new Rvms();
        r.selectStream(1 + streamIndex);
        //return (uniform(2.0, 10.0, r));
        return rvms.idfLogNormal(REMOTE_MU_PARAM_LOGNORMAL, REMOTE_SIGMA_PARAM_LOGNORMAL, r.random());
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
        while (i < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+ALL_EVENTS_FIELD-1) {      //messo +1, ora +3 /* now, check the others to find which  */
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
            //System.out.println(i+" "+event[i].x);
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
        int i = ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            //System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI-1) {         /* now, check the others to find which   */
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
        int i = ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            //System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD -1) {         /* now, check the others to find which   */
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
        int i = ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD;

        while (event[i].x == 1 ) {     /* find the index of the first available */
            //System.out.println(i+" "+event[i].x);
            i++;    /* (idle) server                         */

        }
        s = i;
        while (i < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD -1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    void initFasce(){
        for(int f = 0; f<31; f++){ //sono 34 fasce orarie da mezz'ora
            FasciaOraria fo = new FasciaOraria(PERCENTUALI[f], 10958, 0 + 1800*f, 1800*(f+1)-1);
            fasce.add(fo); //popolo array fasce orarie dinamicamente
        }
    }




}
