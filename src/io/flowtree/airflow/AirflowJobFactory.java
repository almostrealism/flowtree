/*
 * Copyright 2019 Michael Murray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flowtree.airflow;

import io.flowtree.job.Job;
import io.flowtree.job.JobFactory;

import io.flowtree.node.Client;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AirflowJobFactory extends AbstractHandler implements JobFactory {
	private static AirflowJobFactory defaultFactory;

	private double pri = 1.0;
	private long taskId;
	private int i;

	private long nextId;

	private List jobs;

	private CompletableFuture<Void> future;

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
			return io.flowtree.node.Server.instantiateJobClass(data);
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

	@Override
	public CompletableFuture<Void> getCompletableFuture() { return future; }

	public String toString() {
		return "AirflowJobFactory: " + this.taskId;
	}
}
