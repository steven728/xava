/**
 * The MIT License
 * 
 * Copyright (c) 2018-2020 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.jqm.internal;

import io.roxa.xava.jqm.JobState;

/**
 * @author Steven Chen
 *
 */
public interface StorageKeys {

	static final String statistics_job_duration = "jqm:stast:job_duration";

	static String keyForJobs() {
		return "jqm:jobs";
	}

	static String keyForState(JobState jobState) {
		return String.format("jqm:jobs:%s", jobState.name());
	}

	static String keyForState(String catalog, JobState jobState) {
		return String.format("jqm:jobs:%s:%s", catalog, jobState.name());
	}

	static String keyForJobInfo(Long id) {
		return String.format("jqm:job_info:%d", id);
	}

	static String keyForJobList(String catalog) {
		return String.format("jqm:job_list:%s:", catalog);
	}

	static String keyForJobCatalogs() {
		return "jqm:job_catalogs";
	}

	static String keyForJobLog(Long id) {
		return String.format("jqm:job_log:%d", id);
	}

	static String keyAutoIncrement() {
		return "jqm:incr:job_id";
	}

}
