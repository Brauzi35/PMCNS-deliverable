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

public class GraphControllerMsq {


    public static void createGraph(String filePath, String filePath80) {

        List<Double> responseTimes = readDataFromFile(filePath);
        List<Double> responseTimes80 = readDataFromFile(filePath80);



        double[] xData = new double[responseTimes.size()];
        double[] yData = new double[responseTimes.size()];

        double[] xData80 = new double[responseTimes80.size()];
        double[] yData80 = new double[responseTimes80.size()];



        // Popola i dati X e Y del grafico
        for (int i = 0; i < responseTimes.size(); i++) {
            xData[i] = i; // Indice dell'elemento nella lista
            yData[i] = responseTimes.get(i); // Tempo di risposta


        }
        for (int i = 0; i < responseTimes80.size(); i++) {
            xData80[i] = i;
            yData80[i] = responseTimes80.get(i);
        }
        // Crea il dataset per il grafico
        XYDataset dataset = createDataset(xData, yData);
        XYDataset dataset2 = createDataset2(xData, yData, xData80, yData80);

        // Crea il grafico
        JFreeChart chart = createChart(dataset2);
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setRange(0,500); // Imposta il range dell'asse Y da 0 a 5

        // Crea una finestra per visualizzare il grafico
        ChartFrame frame = new ChartFrame("Grafico tempi di risposta dispatcher", chart);
        frame.pack();
        frame.setVisible(true);
    }

    // Metodo per creare il dataset per il grafico
    private static XYDataset createDataset(double[] xData, double[] yData) {
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("Tempi di risposta", new double[][]{xData, yData});
        return dataset;
    }

    private static XYDataset createDataset2(double[] xData, double[] yData, double[] xData80, double[] yData80 ) {
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("Tempi di risposta 80 dispatcher", new double[][]{xData, yData});
        dataset.addSeries("Tempi di risposta 70 dispatcher", new double[][]{xData80, yData80});
        return dataset;
    }



    // Metodo per creare il grafico
    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Grafico tempi di risposta dispatcher", // Titolo del grafico
                "Tempo", // Etichetta asse X
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
        Color customColor = new Color(red, green, blue, 130);
        plot.getRenderer().setSeriesPaint(0, Color.RED);
        plot.getRenderer().setSeriesPaint(1, Color.BLACK);

        plot.setBackgroundPaint(customColor);

        try {
            File outputFile = new File("graficoAvgRespDispatcherDoppio.png");
            ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);
            System.out.println("Grafico esportato come immagine PNG");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chart;
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

        String filePath = "averagesRespDispatcher"; // Path del file contenente i dati
        String filePath80 = "averagesRespDispatcher70";
        createGraph(filePath,filePath80);
        //createGraph(filePath, null); //se hai solo un file
    }

}
 