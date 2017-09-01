package io.almostrealism.flow;

import io.almostrealism.db.Client;
import org.almostrealism.flow.Job;
import org.almostrealism.flow.JobFactory;
import org.almostrealism.flow.Server;

import java.util.ArrayList;
import java.util.List;

public class AirflowJobFactory implements JobFactory {
    private static AirflowJobFactory defaultFactory;

    private double pri = 1.0;
    private long taskId;
    private int i;

    private List jobs;

    /**
     * Constructs a new AirflowJobFactory object.
     */
    public AirflowJobFactory() {
        if (defaultFactory != null) throw new RuntimeException("Cannot create more than one AirflowJobFactory per JVM");

        this.jobs = new ArrayList();
        this.defaultFactory = this;
    }

    /**
     * Constructs a new AirflowJobFactory object using the specified parameters.
     */
    public AirflowJobFactory(long taskId) {
        this.taskId = taskId;

        this.jobs = new ArrayList();
    }

    public long getTaskId() { return this.taskId; }

    /**
     * @see org.almostrealism.flow.JobFactory#nextJob()
     */
    public Job nextJob() {
        if (i >= this.totalJobs) {
            if (this.jobs.size() > 0) return (Job) this.jobs.remove(0);
            return null;
        }

        this.i++;
    }

    /**
     * @see org.almostrealism.flow.JobFactory#createJob(java.lang.String)
     */
    public Job createJob(String data) {
        Client c = Client.getCurrentClient();

        if (c != null && c.getServer() != null)
            return c.getServer().createJob(data);
        else
            return Server.instantiateJobClass(data);
    }

    /**
     * @return  A String encoding of this {@link AirflowJobFactory} object.
     */
    public String encode() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.getClass().getName());
        return buf.toString();
    }

    /**
     * @see org.almostrealism.flow.JobFactory#set(java.lang.String, java.lang.String)
     */
    public void set(String key, String value) {
        if (key.equals("id")) {
            this.taskId = Long.parseLong(value);
        }
    }

    public String getName() {
        StringBuffer b = new StringBuffer();
        b.append("Airflow Worker - ");
        b.append(this.taskId);
        return b.toString();
    }

    public double getCompleteness() { return ((double)this.i) / ((double)this.totalJobs); }

    /** Always return false. Our work is never over. */
    public boolean isComplete() { return false; }

    public void setPriority(double p) { this.pri = p; }

    public double getPriority() { return this.pri; }

    public String toString() {
        return "AirflowJobFactory: " + this.taskId;
    }
}
