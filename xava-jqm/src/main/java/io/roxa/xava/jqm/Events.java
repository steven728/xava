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
package io.roxa.xava.jqm;

/**
 * @author Steven Chen
 *
 */
public abstract class Events {

	/* The job is now queued */
	public static final String job_enqueue = "jqm.events.job.enqueue";
	/* The job is now running */
	public static final String job_start = "jqm.events.job.start";
	/* The job is promoted from delayed state to queued */
	public static final String job_promotion = "jqm.events.job.promotion";
	/* The job's progress ranging from 0-100 */
	public static final String job_progress = "jqm.events.job.progress";
	/* The job has failed, but has remaining attempts yet */
	public static final String job_failed_attempt = "jqm.events.job.failed_attempt";
	/* The job has failed and has no remaining attempts */
	public static final String job_failed = "jqm.events.job.failed";
	/* The job has completed */
	public static final String job_complete = "jqm.events.job.complete";
	/* The job has been removed */
	public static final String job_remove = "jqm.events.job.remove";

	/* The queue level job is now queued */
	public static final String queue_job_enqueue = "jqm.events.queue.job_enqueue";
	/* The queue level job is now running */
	public static final String queue_job_start = "jqm.events.queue.job_start";
	/* The queue level job is promoted from delayed state to queued */
	public static final String queue_job_promotion = "jqm.events.queue.job_promotion";
	/* The queue level job's progress ranging from 0-100 */
	public static final String queue_job_progress = "jqm.events.queue.job_progress";
	/* The queue level job has failed, but has remaining attempts yet */
	public static final String queue_job_failed_attempt = "jqm.events.queue.job_failed_attempt";
	/* The queue level job has failed and has no remaining attempts */
	public static final String queue_job_failed = "jqm.events.queue.job_failed";
	/* The queue level job has completed */
	public static final String queue_job_complete = "jqm.events.queue.job_complete";
	/* The queue level job has been removed */
	public static final String queue_job_remove = "jqm.events.queue.job_remove";

}
