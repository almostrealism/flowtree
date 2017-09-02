package io.almostrealism.flow;

import org.almostrealism.flow.Job;

import java.io.IOException;

public class AirflowJob implements Job {
	private long taskId;
	private String command;

	public AirflowJob(long taskId, String command) {
		this.taskId = taskId;
		this.command = command;
		System.out.println("Constructing " + getTaskString());
	}

	@Override
	public long getTaskId() { return taskId; }

	@Override
	public String getTaskString() { return command; }

	@Override
	public String encode() {
		StringBuffer b = new StringBuffer();

		b.append(this.getClass().getName());
		b.append(":id=");
		b.append(this.taskId);
		b.append(":cmd=");
		b.append(command);

		return b.toString();
	}

	@Override
	public void set(String key, String value) {
		if (key.equals("id")) {
			this.taskId = Long.parseLong(value);
		} else if (key.equals("cmd")) {
			this.command = value;
		}
	}

	@Override
	public void run() {
		Runtime r = Runtime.getRuntime();

		try {
			System.out.println("Running " + command);
			r.exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String toString() { return "'" + getTaskString() + "'"; }
}
