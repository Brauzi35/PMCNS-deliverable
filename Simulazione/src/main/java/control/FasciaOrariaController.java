package control;

import model.FasciaOraria;

import java.util.ArrayList;
import java.util.List;

public class FasciaOrariaController {
    static double [] percentuali = {0.001,    0.02,    0.03,    0.04,    0.0470,    0.049,    0.049,    0.048,    0.046,    0.042,    0.038,    0.0350,    0.0350,    0.037,    0.036,    0.035,    0.036,    0.037,    0.039,    0.04,    0.041,    0.041,    0.039,    0.034,    0.027,    0.018,    0.017,    0.015,    0.011,    0.006,    0.003};
    static List<FasciaOraria> fasce = new ArrayList<>();


    public static int fasciaOrariaSwitch(List<FasciaOraria> fol, double currentTime){
        int ret = 0;

        for(FasciaOraria f : fol){
            if(currentTime >= f.getLowerBound() && currentTime <= f.getUpperBound()){
                ret = fol.indexOf(f);
            }
        }

        return ret; //ritorno l'indice dell'array della fascia oraria in cui mi trovo
    }

}
