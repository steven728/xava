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
package io.roxa.xava.jqm.internal.cli;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import io.roxa.xava.jqm.JobStorage;

/**
 * @author Steven Chen
 *
 */
@Command(scope = "xava", name = "job-create", description = "The create a job")
@Service
public class JobCreate implements Action {

	@Argument(index = 0, name = "catalog", description = "The catalog of department", required = true, multiValued = false)
	String catalog = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.karaf.shell.api.action.Action#execute()
	 */
	@Override
	public Object execute() throws Exception {
		JobStorage jobStorage = JobStorage.instance();
		jobStorage.create(catalog).compose(jobStorage::save);
		return null;
	}

}
