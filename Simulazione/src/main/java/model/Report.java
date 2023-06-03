package model;

/**
 * Astrazione delle richieste di riparazione guasto linea fissa: inizialmente vengono istanziate
 * solo con tempo di arrivo in coda, classe di tariffa (privato-2, pubblico-1 e business-0)
 */
public class Report {

    private double arrivalTime;
    private int priority;

    public Report(double arrivalTime, int priority) {
        this.arrivalTime = arrivalTime;
        this.priority = priority;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
