package control;

import model.FasciaOraria;

import java.util.ArrayList;
import java.util.List;

public class FasciaOrariaController {
    //static double [] percentuali = {0.0,    0.0001,    0.02,    0.03,    0.04,    0.0470,    4.90,    4.90,    4.80,    4.60,    4.20,    3.80,    3.50,    3.50,    3.70,    3.60,    3.50,    3.60,    3.70,    3.90,    4.00,    4.10,    4.10,    3.90,    3.40,    2.70,    1.80,    1.70,    1.50,    1.10,    0.60,    0.30,    0.0,    0.0};
    static double [] percentuali = {0.001,    0.02,    0.03,    0.04,    0.0470,    0.049,    0.049,    0.048,    0.046,    0.042,    0.038,    0.0350,    0.0350,    0.037,    0.036,    0.035,    0.036,    0.037,    0.039,    0.04,    0.041,    0.041,    0.039,    0.034,    0.027,    0.018,    0.017,    0.015,    0.011,    0.006,    0.003};
    static List<FasciaOraria> fasce = new ArrayList<>();

    static double currentTimeSt= 2500.0;

    public static int fasciaOrariaSwitch(List<FasciaOraria> fol, double currentTime){
        int ret = 0;

        for(FasciaOraria f : fol){
            if(currentTime >= f.getLowerBound() && currentTime <= f.getUpperBound()){
                ret = fol.indexOf(f);
            }
        }

        return ret; //ritorno l'indice dell'array della fascia oraria in cui mi trovo
    }

    /*public static void main(String[] args){

        for(int f = 0; f<31; f++){ //sono 34 fasce orarie da mezz'ora
            FasciaOraria fo = new FasciaOraria(percentuali[f], 10958, 0 + 1800*f, 1800*(f+1)-1);
            fasce.add(fo); //popolo array fasce orarie dinamicamente
        }

        int index = FasciaOrariaController.fasciaOrariaSwitch(fasce, currentTimeSt);

        int i=0;
        for(double db : percentuali) {

            System.out.println(db + "    " + i );
            i++;
        }

    }*/
}
