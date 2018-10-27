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

import io.vertx.core.json.JsonObject;

/**
 * @author Steven Chen
 *
 */
public class Job {

	private JsonObject jobInfoLoaded;

	private JsonObject jobInfoUpdated;

	public static Job create(JsonObject jobInfo) {
		return new Job(jobInfo);
	}

	public static Job create(String catalog) {
		return new Job(catalog);
	}

	Job(String catalog) {
		this.jobInfoUpdated = this.jobInfoLoaded = new JsonObject();
		setCatalog(catalog);
	}

	Job(JsonObject jobInfo) {
		this.jobInfoLoaded = jobInfo;
		this.jobInfoUpdated = new JsonObject();
	}

	public Job setCatalog(String catalog) {
		jobInfoUpdated.put("catalog", catalog);
		return this;
	}

	public Job setPayload(JsonObject data) {
		jobInfoUpdated.put("payload", data);
		return this;
	}

	public Job setId(Long id) {
		jobInfoUpdated.put("id", id);
		return this;
	}

	public Job setScoreId(Long id) {
		String scoreId = createScoreId(id);
		jobInfoUpdated.put("score_id", scoreId);
		return this;
	}

	public Job setPriority(JobPriority priority) {
		jobInfoUpdated.put("priority", priority.intValue());
		return this;
	}

	public Job setRetry(Integer retry) {
		jobInfoUpdated.put("retry", retry);
		return this;
	}

	public Job setDelay(Long delay) {
		jobInfoUpdated.put("delay", delay);
		return this;
	}

	public Job setRetryFixedDelay(Long delay) {
		jobInfoUpdated.put("retryDelay", new JsonObject().put("type", "fixed").put("delay", delay));
		return this;
	}

	public Job setRetryExpnalDelay(Long delay) {
		jobInfoUpdated.put("retryDelay", new JsonObject().put("type", "Expnal").put("delay", delay));
		return this;
	}

	public Job setState(JobState jobState) {
		jobInfoUpdated.put("state", jobState);
		return this;
	}

	public Job setDateStarted(Long dateStarted) {
		jobInfoUpdated.put("date_started", dateStarted);
		return this;
	}

	public Job setDateCreated(Long dateCreated) {
		jobInfoUpdated.put("date_created", dateCreated);
		return this;
	}

	public Job setDatePromoted(Long datePromoted) {
		jobInfoUpdated.put("date_promoted", datePromoted);
		return this;
	}

	public Job setDateUpdated(Long dateUpdated) {
		jobInfoUpdated.put("date_updated", dateUpdated);
		return this;

	}

	public Long getId() {
		return jobInfoLoaded.getLong("id");
	}

	public JobState getState() {
		return JobState.valueOf(jobInfoLoaded.getString("state"));
	}

	public String getScroeId() {
		return jobInfoLoaded.getString("score_id");
	}

	public String getCatalog() {
		return jobInfoLoaded.getString("catalog");
	}

	public Integer getPriorityValue() {
		return JobPriority.valueOf(jobInfoLoaded.getString("priority")).intValue();
	}

	public Integer getActivePriorityValue() {
		int v = JobPriority.valueOf(jobInfoLoaded.getString("priority")).intValue();
		return v < 0 ? v : -v;
	}

	public Long getDatePromoted() {
		return jobInfoLoaded.getLong("date_promoted");
	}

	public Long getDateFailed() {
		return jobInfoLoaded.getLong("date_failed", 0L);
	}

	public Long getDateCreated() {
		return jobInfoLoaded.getLong("date_created", 0L);
	}

	public Long getDelay() {
		return jobInfoLoaded.getLong("delay", 0L);
	}

	public JsonObject getUpdatedInfo() {
		return jobInfoUpdated;
	}

	public String stringify() {
		return jobInfoLoaded.encode();
	}

	public String updatedStringify() {
		return jobInfoUpdated.encode();
	}

	public static String createScoreId(Long id) {
		return String.format("%02d|%d", String.valueOf(id).length(), id);
	}

	public static Long resolveScoreId(String scoreId) {
		String[] part = scoreId.split("|");
		return new Long(part[1]);
	}

}
