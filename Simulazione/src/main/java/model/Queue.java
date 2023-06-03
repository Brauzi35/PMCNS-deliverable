package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Astrazione dell'oggetto coda: viene istanziata conoscendone livello di priorità ed attribuendole un id univoco
 */
public class Queue {

    private int priority;
    private List<?> jobs;
    public Queue(int priority){
        this.priority = priority;
        this.jobs = new ArrayList<>();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<?> getJobs() {
        return jobs;
    }

    public void setJobs(List<?> jobs) {
        this.jobs = jobs;
    }
}
