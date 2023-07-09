package control;

import model.FasciaOraria;
import utils.Estimate;
import utils.Rngs;
import utils.Timestamp;
import utils.WriteDoubleListToFile;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static model.SimulationValues.*;
import static model.SimulationValues.SERVERS;

public class BatchSimulation {

    static List<FasciaOraria> fasce = new ArrayList<>();
    static double PERC = 0.041; //perc è la percentuale di chiamate in quella fascia oraria--- 0.02 fascia idx 1 ---- 0.03 fascia idx 2 --- 0.041 fascia idx 20

    public void batchSim(double perc, int fascia) throws FileNotFoundException {

        List<Double> responseTimeCentralinoList = new ArrayList<>();
        List<Double> responseTimeDispList = new ArrayList<>();
        List<Double> responseTimeRemotoList = new ArrayList<>();
        List<Double> responseTimeFieldList = new ArrayList<>();

        List<Double> interarrivalCentralinoList = new ArrayList<>();
        List<Double> interarrivalDispList = new ArrayList<>();
        List<Double> interarrivalRemotoList = new ArrayList<>();
        List<Double> interarrivalFieldList = new ArrayList<>();

        List<Double> utilCentralinoList = new ArrayList<>();
        List<Double> utilDispList = new ArrayList<>();
        List<Double> utilRemotoList = new ArrayList<>();
        List<Double> utilFieldList = new ArrayList<>();

        List<Double> numberJobCentralinoList = new ArrayList<>();
        List<Double> numberJobDispList = new ArrayList<>();
        List<Double> numberJobRemotoList = new ArrayList<>();
        List<Double> numberJobFieldList = new ArrayList<>();


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
        m.initFasce();
        Rngs r = new Rngs();
        r.plantSeeds(123456789);

        List<Double> abandons = new ArrayList<Double>(); //lista abbandoni
        List<Double> abandonsRH = new ArrayList<Double>(); //lista abbandoni remoto high priority
        List<Double> abandonsRM = new ArrayList<Double>(); //lista abbandoni remoto medium priority
        List<Double> abandonsRL = new ArrayList<Double>(); //lista abbandoni remoto low priority
        List<Double> abandonsFH = new ArrayList<Double>(); //lista abbandoni on field high priority
        List<Double> abandonsFM = new ArrayList<Double>(); //lista abbandoni on field medium priority
        List<Double> abandonsFL = new ArrayList<Double>(); //lista abbandoni on field low priority


        for(int f = 0; f<31; f++){ //sono 31 fasce orarie da mezz'ora
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
        event[0].t   = m.getArrivalBatch(r, t.current, idx, fascia);
        event[0].x   = 1;

        for (s = 1; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + ALL_EVENTS_FIELD; s++) { //messo il + 2 perchè ho aggiunto il dispatcher e +14 per i due centri dei guasti?
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        Timestamp timestamp = new Timestamp();

        double stopBatch = (1/((STOP_BATCH*perc)/1800))*(Math.pow(2,17));
        System.out.println(stopBatch);

        double sumService = 0.0;
        double tFinalCentralino = 0.0;
        double realTimeCentralino = 0.0;

        double tCurrentBatch = 0.0;
        double eventT = 0.0;

        int count = 0;

        while ((event[0].x != 0)) {

            /*
                    Entro nell'if ogni 1024 iterazioni (dimensione batch 2^10),
                    salvo i valori nelle liste appropiate e poi azzero le statistiche
             */

            if(index != 0 && index % 1024 == 0){
                count++;

                /*
                ****** CENTRALINO
                 */
                responseTimeCentralinoList.add(area / index);

                sumService = 0.0;
                for(s = 2; s <= SERVERS+1; s++){

                    sumService += sum[s].service;
                    sum[s].service=0;

                }
                sumService = sumService/SERVERS;

                utilCentralinoList.add(sumService/(t.current - tCurrentBatch)) ;
                interarrivalCentralinoList.add((event[0].t - eventT) / index);
                numberJobCentralinoList.add(area / (t.current - tCurrentBatch));


                area = 0.0;
                index = 0;

                /*
                ***** DISPATCHER

                 */

                responseTimeDispList.add(areaDispatcher/dispatched);
                utilDispList.add(sum[SERVERS+3].service/(t.current - tCurrentBatch));
                sum[SERVERS+3].service = 0;
                interarrivalDispList.add((event[0].t - eventT)/dispatched);
                numberJobDispList.add(areaDispatcher/(t.current - tCurrentBatch));

                areaDispatcher=0;
                dispatched=0;

                /*
                *** REMOTO ***
                 */

                responseTimeRemotoList.add(areaRemoto/indexRemoto);

                double sumRemoto = 0.0;
                for(s = SERVERS + 7; s < SERVERS+7+SERVERS_REMOTI; s++){
                    sumRemoto += sum[s].service;
                    sum[s].service=0;

                }
                sumRemoto = sumRemoto/SERVERS_REMOTI;

                utilRemotoList.add(sumRemoto/(t.current - tCurrentBatch));
                interarrivalRemotoList.add((event[0].t - eventT)/indexRemoto);
                numberJobRemotoList.add(areaRemoto/(t.current - tCurrentBatch));

                areaRemoto = 0;
                indexRemoto = 0;

                /*
                *** FIELD ***
                 */
                responseTimeFieldList.add(areaField/indexField);

                double sumField = 0.0;
                for(s = ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD; s < ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + ALL_EVENTS_REMOTE + EVENTS_ARRIVE_PRIORITY_CLASS_FIELD + SERVERS_FIELD_SPECIAL + SERVERS_FIELD_STD; s++){
                    sumField += sum[s].service;
                    sum[s].service=0;
                }
                sumField = sumField/(SERVERS_FIELD_STD);

                utilFieldList.add(sumField/(t.current - tCurrentBatch));
                interarrivalFieldList.add((event[0].t - eventT)/indexField);
                numberJobFieldList.add(areaField/(t.current - tCurrentBatch));

                areaField = 0;
                indexField = 0;

                /*
                FINAL
                 */
                tCurrentBatch = t.current;
                eventT = event[0].t;
            }

            if(field < 0){
                break;
            }

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

            areaRemoto += (t.next - t.current) * remoto;
            if(remoto > SERVERS_REMOTI){
                areaRemotoQueue += (t.next - t.current) * (remoto - SERVERS_REMOTI);
            }
            areaField += (t.next - t.current) * field;
            if(field > SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL){
                areaFieldQueue += (t.next - t.current) * (field - (SERVERS_FIELD_STD + SERVERS_FIELD_SPECIAL));
            }
            t.current = t.next;                            /* advance the clock*/




            if (e == EVENT_ARRIVE_CENTRALINO-1) {                                  /* process a callcenter arrival*/
                number++;
                event[0].t        = m.getArrivalBatch(r, t.current, idx, fascia);

                if(count == 132){
                    event[0].x = 0; //close the door
                }
                if (number <= SERVERS) {
                    service         = m.getServiceCentralino(r, idx);
                    s               = m.findOne(event); //id server
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service; //tempo di completamento
                    event[s].x      = 1; //eleggibile per il next event
                }
                if (number > SERVERS){
                    //genero abbandono se un job sta in coda
                    double at = m.getAbandon(PATIENCE_CENTRALINO, r, idx) + t.current;
                    abandons.add(at);
                }
            }

            else if(e == EVENT_ABANDONMENT_CENTRALINO) { //processo abbandono callcenter
                number--;
                abandon++;
                abandons.remove(abandons.indexOf(Collections.min(abandons))); //tolgo job dalla lista
                if(abandons.isEmpty()){
                    event[1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO){ //arrivo dispatcher

                event[ALL_EVENTS_CENTRALINO].x = 0;     //non può esserci un altro arrivo al dispatcher senza che ci
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
                    timestamp.primoComplCentralino = t.current;
                }

                numberDispatcher--;
                dispatched++;
                r.selectStream(10 + idx);
                double rnd = r.random(); //mi dice se il job va on field oppure va remoto
                double priority = r.random();
                if(rnd<REMOTE_PROBABILITY){ //in remoto era 0.8
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

                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                                                                            //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI && abandonsRH.isEmpty() && abandonsRM.isEmpty()){
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
                    double at = m.getAbandon(PATIENCE_LOW_REMOTO, r, idx) + t.current;
                    abandonsRL.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+1){ //arrivo coda priorità media - remoto
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
                    double at = m.getAbandon(PATIENCE_MEDIUM_REMOTO, r, idx) + t.current;
                    abandonsRM.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+2){ //arrivo coda priorità alta - remoto
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+2].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                                                                            //sia un' altra partenza dal dispatcher

                if(remoto <= SERVERS_REMOTI){
                    //processiamo i servizi
                    service = m.getServiceRemote(r, idx);
                    s = m.findOneRemoto(event);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event

                }
                if(remoto > SERVERS_REMOTI){
                    double at = m.getAbandon(PATIENCE_HIGH_REMOTO, r, idx) + t.current;
                    abandonsRH.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI){ //abbandono coda alta priorità remoto
                remoto--;
                abandonRH++;
                abandonsRH.remove(abandonsRH.indexOf(Collections.min(abandonsRH))); //tolgo job dalla lista
                if(abandonsRH.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 1){ //abbandono coda media priorità remoto
                remoto--;
                abandonRM++;
                abandonsRM.remove(abandonsRM.indexOf(Collections.min(abandonsRM))); //tolgo job dalla lista
                if(abandonsRM.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 2){ //abbandono coda bassa priorità remoto
                remoto--;
                abandonRL++;
                abandonsRL.remove(abandonsRL.indexOf(Collections.min(abandonsRL))); //tolgo job dalla lista
                if(abandonsRL.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO + ALL_EVENTS_DISPATCHER + EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE + SERVERS_REMOTI + 2].x = 0;
                }
            }

            else if(e >= ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE && e < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+EVENTS_ARRIVE_PRIORITY_CLASS_REMOTE+SERVERS_REMOTI){//completamento server remoto

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
                if(!abandonsRH.isEmpty() && abandonsRH.get(0) < t.current) {
                    abandonsRH.remove(0);
                }
                if(!abandonsRM.isEmpty() && abandonsRM.get(0) < t.current) {
                    abandonsRM.remove(0);
                }
                if(!abandonsRL.isEmpty() && abandonsRL.get(0) < t.current) {
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
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                                                                                            //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty() && abandonsFM.isEmpty()){ //la coda di priorità bassa vede solo i server standard
                                                                                                //processiamo i servizi
                    service = m.getServiceField(r, idx);
                    s = m.findOneFieldStd(event);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    double at = m.getAbandon(PATIENCE_LOW_FIELD, r, idx) + t.current;
                    abandonsFL.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 1){ //arrivo coda priorità media - on field
                event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE + 1].x = 0; //non può esserci un altro arrivo in questa coda senza che ci
                                                                                                //sia un' altra partenza dal dispatcher

                if(field <= SERVERS_FIELD_STD && abandonsFH.isEmpty()){ //la coda di priorità media vede solo i server standard
                                                                        //processiamo i servizi
                    service = m.getServiceField(r, idx);
                    s = m.findOneFieldStd(event);
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
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
                    sum[s].service +=service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1; //elegibile come next event


                }
                else{
                    //genero abbandono se un job sta in coda
                    double at = m.getAbandon(PATIENCE_HIGH_FIELD, r, idx) + t.current;
                    abandonsFH.add(at);
                }

            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD){ //abbandono coda bassa priorità field
                field--;
                abandonFL++;
                abandonsFL.remove(abandonsFL.indexOf(Collections.min(abandonsFL))); //tolgo job dalla lista
                if(abandonsFL.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1){ //abbandono coda media priorità field
                field--;
                abandonFM++;
                abandonsFM.remove(abandonsFM.indexOf(Collections.min(abandonsFM))); //tolgo job dalla lista
                if(abandonsFM.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 1].x = 0;
                }
            }

            else if(e == ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2){ //abbandono coda alta priorità field
                field--;
                abandonFH++;
                abandonsFH.remove(abandonsFH.indexOf(Collections.min(abandonsFH))); //tolgo job dalla lista
                if(abandonsFH.isEmpty()){
                    event[ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_SPECIAL+SERVERS_FIELD_STD + 2].x = 0;
                }
            }

            else if(e >= ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD && e < ALL_EVENTS_CENTRALINO+ALL_EVENTS_DISPATCHER+ALL_EVENTS_REMOTE+EVENTS_ARRIVE_PRIORITY_CLASS_FIELD+SERVERS_FIELD_STD+SERVERS_FIELD_SPECIAL){//completamento server on field

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
                if(!abandonsFH.isEmpty() && abandonsFH.get(0) < t.current) {
                    abandonsFH.remove(0);
                }
                if(!abandonsFM.isEmpty() && abandonsFM.get(0) < t.current) {
                    abandonsFM.remove(0);
                }
                if(!abandonsFL.isEmpty() && abandonsFL.get(0) < t.current) {
                    abandonsFL.remove(0);
                }
                int size = abandonsFH.size() + abandonsFL.size() + abandonsFM.size();

                //servizio se c'è coda - comportamento server speciali
                if(s>= 2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 && s<  2 + SERVERS + 2 + 3 + 3 + SERVERS_REMOTI + 3 + SERVERS_FIELD_SPECIAL && !abandonsFH.isEmpty()){

                    abandonsFH.remove(0); //prendo un job dalla coda ad alta priorità
                    service         = m.getServiceField(r, idx);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
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

                if (timestamp.primoComplCentralino == 0){
                    timestamp.primoComplCentralino = t.current;
                }

                index++;                                     /* from server s       */
                number--;
                s                 = e;                        //indice next event = server id
                event[ALL_EVENTS_CENTRALINO].t = t.current;   //invio ticket al dispatcher
                event[ALL_EVENTS_CENTRALINO].x = 1;           //arrivo dispatcher elegibile per next event
                numberDispatcher++;                           //incremento contatore
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

            /* FINE ITERAZIONE */
        }

        WriteDoubleListToFile w = new WriteDoubleListToFile();
        //CENTRALINO
        w.scrivi(responseTimeCentralinoList, "batchResponseTimeCent");
        w.scrivi(utilCentralinoList, "batchUtilCent");
        w.scrivi(interarrivalCentralinoList, "batchInterCent");
        w.scrivi(numberJobCentralinoList, "batchNumberJobCent");

        //DISPATCHER
        w.scrivi(responseTimeDispList, "batchResponseTimeDisp");
        w.scrivi(utilDispList, "batchUtilDisp");
        w.scrivi(interarrivalDispList, "batchInterDisp");
        w.scrivi(numberJobDispList, "batchNumberJobDisp");

        //REMOTO
        w.scrivi(responseTimeRemotoList, "batchResponseTimeRemoto");
        w.scrivi(utilRemotoList, "batchUtilRemoto");
        w.scrivi(interarrivalRemotoList, "batchInterRemoto");
        w.scrivi(numberJobRemotoList, "batchNumberJobRemoto");

        //FIELD
        w.scrivi(responseTimeFieldList, "batchResponseTimeField");
        w.scrivi(utilFieldList, "batchUtilField");
        w.scrivi(interarrivalFieldList, "batchInterField");
        w.scrivi(numberJobFieldList, "batchNumberJobField");


        Estimate est = new Estimate();
        List<String> names = List.of("batchResponseTimeCent", "BatchResponseTimeDisp", "BatchResponseTimeRemoto"
                , "batchResponseTimeField", "batchInterCent", "batchInterDisp", "batchInterRemoto",
                "batchInterField", "batchUtilCent", "batchUtilDisp", "batchUtilRemoto", "batchUtilField", "batchNumberJobCent",
                "batchNumberJobDisp", "batchNumberJobRemoto", "batchNumberJobField" );

        for(String str : names){
            System.out.println(str);
            est.intervals(str); //calcolo intervalli di confidenza
            System.out.println("\n------------------------------\n");
        }


    }

    public static void main(String[] args) throws FileNotFoundException {

        BatchSimulation bs = new BatchSimulation();
        bs.batchSim(PERC, 1);
    }

    }


