/*
 * Copyright 2018 Michael Murray
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

import java.io.IOException;

import io.flowtree.job.Job;

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
