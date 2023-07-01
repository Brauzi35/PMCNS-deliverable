package control;

import model.FasciaOraria;

import java.util.List;

public class FasciaOrariaController {

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
