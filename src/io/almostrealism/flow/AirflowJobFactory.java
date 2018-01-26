package io.almostrealism.flow;

import io.flowtree.job.Job;
import io.flowtree.job.JobFactory;

import org.almostrealism.flow.Client;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AirflowJobFactory extends AbstractHandler implements JobFactory {
	private static AirflowJobFactory defaultFactory;

	private double pri = 1.0;
	private long taskId;
	private int i;

	private long nextId;

	private List jobs;

	/**
	 * Constructs a new AirflowJobFactory object and starts the {@link org.eclipse.jetty.util.Jetty} server.
	 */
	public AirflowJobFactory() {
		initDefaultFactory(this);
		this.jobs = new ArrayList();
	}

	/**
	 * Constructs a new AirflowJobFactory object using the specified parameters.
	 */
	public AirflowJobFactory(long taskId) {
		this.taskId = taskId;
		this.jobs = new ArrayList();
	}

	private static synchronized void initDefaultFactory(AirflowJobFactory f) {
		if (defaultFactory != null) throw new RuntimeException("Cannot create more than one AirflowJobFactory per JVM");
		defaultFactory = f;

		Server server = new Server(7070);
		server.setHandler(defaultFactory);

		try {
			server.start();
//            server.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public long getTaskId() { return this.taskId; }

	/**
	 * @see io.flowtree.job.JobFactory#nextJob()
	 */
	public Job nextJob() {
		if (this.jobs.size() > 0) return (Job) this.jobs.remove(0);
		return null;
	}

	/**
	 * @see io.flowtree.job.JobFactory#createJob(java.lang.String)
	 */
	public Job createJob(String data) {
		Client c = Client.getCurrentClient();

		if (c != null && c.getServer() != null)
			return c.getServer().createJob(data);
		else
			return org.almostrealism.flow.Server.instantiateJobClass(data);
	}

	@Override
	public void handle(String target, Request baseRequest,
					   HttpServletRequest request,
					   HttpServletResponse response) throws IOException, ServletException {
		long id = nextId++;
		String cmd = request.getParameter("cmd");
		if (cmd == null) return;
		this.jobs.add(new AirflowJob(id, cmd));

		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(id);
		baseRequest.setHandled(true);
	}

	/**
	 * @return A String encoding of this {@link AirflowJobFactory} object.
	 */
	public String encode() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.getClass().getName());
		return buf.toString();
	}

	/**
	 * @see io.flowtree.job.JobFactory#set(java.lang.String, java.lang.String)
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

	public double getCompleteness() { return 0; }

	/**
	 * Always return false. Our work is never over.
	 */
	public boolean isComplete() { return false; }

	public void setPriority(double p) { this.pri = p; }

	public double getPriority() { return this.pri; }

	public String toString() {
		return "AirflowJobFactory: " + this.taskId;
	}
}
