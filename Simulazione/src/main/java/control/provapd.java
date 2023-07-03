package control;

import utils.Rngs;

public class provapd {

    public static int PATIENCE = 480;

    public static void main(String[] args) {

        Rngs r = new Rngs();
        r.plantSeeds(123456789);
        r.selectStream(1);

        for(int i=0; i<20; i++){
            System.out.println(-PATIENCE * Math.log(1.0 - r.random()));
        }

    }



}
