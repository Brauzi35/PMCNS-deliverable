package utils;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class WriteDoubleListToFile {
    public void scrivi(List<Double> doubleList, String fileName) {

        try {
            FileWriter fileWriter = new FileWriter(fileName);
            PrintWriter printWriter = new PrintWriter(fileWriter);

            for (double num : doubleList) {
                if(!Double.isNaN(num) && !(Double.isInfinite(num))) {
                    printWriter.println(num);

                }
                else{
                    System.out.println("NaN!");
                }
            }

            printWriter.close();
            System.out.println("Valori double scritti su file con successo.");

        } catch (IOException e) {
            System.out.println("Si è verificato un errore durante la scrittura del file.");
            e.printStackTrace();
        }
    }
}

