package model;

/**
 * Astrazione delle richieste di riparazione guasto linea fissa: inizialmente vengono istanziate
 * solo con tempo di arrivo in coda; in maniera iterativa saranno aggiunte le altre statistiche utili alla fine
 */
public class Report {

    private double arrivalTime;
    private double firstDepartureTime;
    private double dispatcherArrivalTime;
    private double dispatcherDepartureTime;
    private double repairArrivalTime;
    private double repairedTime;

    public Report(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getFirstDepartureTime() {
        return firstDepartureTime;
    }

    public void setFirstDepartureTime(double firstDepartureTime) {
        this.firstDepartureTime = firstDepartureTime;
    }

    public double getDispatcherArrivalTime() {
        return dispatcherArrivalTime;
    }

    public void setDispatcherArrivalTime(double dispatcherArrivalTime) {
        this.dispatcherArrivalTime = dispatcherArrivalTime;
    }

    public double getDispatcherDepartureTime() {
        return dispatcherDepartureTime;
    }

    public void setDispatcherDepartureTime(double dispatcherDepartureTime) {
        this.dispatcherDepartureTime = dispatcherDepartureTime;
    }

    public double getRepairArrivalTime() {
        return repairArrivalTime;
    }

    public void setRepairArrivalTime(double repairArrivalTime) {
        this.repairArrivalTime = repairArrivalTime;
    }

    public double getRepairedTime() {
        return repairedTime;
    }

    public void setRepairedTime(double repairedTime) {
        this.repairedTime = repairedTime;
    }
}
