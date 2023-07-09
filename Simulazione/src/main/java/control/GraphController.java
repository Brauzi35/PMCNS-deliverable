package control;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphController {


    public static void createGraph(String filePath) {

        List<Double> responseTimes = readDataFromFile(filePath);
        int displayInterval = 4;

        double[] xData = new double[responseTimes.size()];
        double[] yData = new double[responseTimes.size()];

        System.out.println("size: " + responseTimes.size());
        // Popola i dati X e Y del grafico
        for (int i = 0; i < responseTimes.size(); i++) {
            xData[i] = i; // Indice dell'elemento nella lista
            yData[i] = responseTimes.get(i); // Tempo di risposta
        }

        //  dataset per il grafico
        XYDataset dataset = createDataset(xData, yData);

        JFreeChart chart = createChart(dataset);
        XYPlot plot = (XYPlot) chart.getPlot();
        String[] xLabels = createXLabels(responseTimes.size(), displayInterval);
        SymbolAxis xAxis = new SymbolAxis("# job", xLabels);
        xAxis.setTickUnit(new NumberTickUnit(displayInterval)); // Imposta l'unità di tick sull'asse X
        plot.setDomainAxis(xAxis);
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setRange(370, 415); // Imposta il range dell'asse Y da val1 a val2



        ChartFrame frame = new ChartFrame("Tempo di risposta 8:00 - 8:30", chart);
        frame.pack();
        frame.setVisible(true);

        try {   //stampa su file .png
            File outputFile = new File("graficoBatchResponseTimeCent8:00_8:30_80Server.png");
            ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);
            System.out.println("Grafico esportato come immagine PNG");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static XYDataset createDataset(double[] xData, double[] yData) {
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("Tempi di risposta", new double[][]{xData, yData});
        return dataset;
    }


    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Tempo di risposta 8 - 8:30 ", // Titolo del grafico
                "# job", // Etichetta asse X
                "Tempo di risposta", // Etichetta asse Y
                dataset, // Dataset dei dati
                PlotOrientation.VERTICAL, // Orientamento del grafico
                true, // Mostra la legenda
                false, // Non mostra i tooltip
                false // Non mostra i link per l'URL
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        int red = 153;
        int green = 203;
        int blue = 255;

        // Crea il colore personalizzato utilizzando i valori RGB
        Color customColor = new Color(red, green, blue, 130); //130 = valore per trasparenza
        plot.getRenderer().setSeriesPaint(0, Color.RED);

        plot.setBackgroundPaint(customColor); //COLORE SFONDO CELESTE con TRASPARENZA

        return chart;
    }


    private static String[] createXLabels(int dataSize, int displayInterval) {
        int numLabels = dataSize / displayInterval;
        String[] labels = new String[numLabels];

        for (int i = 0; i < numLabels; i++) {
            int labelValue = i * 4096;//
            labels[i] = String.valueOf(labelValue);
        }

        return labels;
    }
    private static List<Double> readDataFromFile(String filePath) {
        List<Double> responseTimes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                double value = Double.parseDouble(line.trim());
                responseTimes.add(value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseTimes;
    }

    public static void main(String[] args) {
        // Esempio di utilizzo
        String filePath = "batchResponseTimeCent"; // Path del file contenente i dati
        createGraph(filePath);
    }

}
