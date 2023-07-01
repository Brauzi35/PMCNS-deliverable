package model;

public class FasciaOraria {
    private double mediaPoisson;
    private double percentualeChiamate;
    private int chiamateGiornaliereTotali;
    private int lowerBound; //tempo in secondi
    private int upperBound; //tempo in secondi


    public FasciaOraria(double percentualeChiamate, int chiamateGiornaliereTotali, int lowerBound, int upperBound) {
        this.percentualeChiamate = percentualeChiamate;
        this.chiamateGiornaliereTotali = chiamateGiornaliereTotali;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.mediaPoisson = percentualeChiamate * chiamateGiornaliereTotali;
    }

    public double getMediaPoisson() {
        return mediaPoisson;
    }

    public void setMediaPoisson(double mediaPoisson) {
        this.mediaPoisson = mediaPoisson;
    }

    public double getPercentualeChiamate() {
        return percentualeChiamate;
    }

    public void setPercentualeChiamate(double percentualeChiamate) {
        this.percentualeChiamate = percentualeChiamate;
    }

    public int getChiamateGiornaliereTotali() {
        return chiamateGiornaliereTotali;
    }

    public void setChiamateGiornaliereTotali(int chiamateGiornaliereTotali) {
        this.chiamateGiornaliereTotali = chiamateGiornaliereTotali;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }
}
