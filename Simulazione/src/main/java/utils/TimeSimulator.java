package utils;

public class TimeSimulator {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getNext() {
        return next;
    }

    public void setNext(double next) {
        this.next = next;
    }
}
