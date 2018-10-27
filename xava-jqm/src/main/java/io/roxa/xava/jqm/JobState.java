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
 * <ul>
 * <li>INACTIVE: the job is inactive and waiting in queue for processing</li>
 * <li>ACTIVE: the processing procedure of the job is pending</li>
 * <li>COMPLETE: the job has been successfully processed</li>
 * <li>FAILED: the processing procedure of the job has failed</li>
 * <li>DELAYED: the job is delayed to be processed and waiting for promoting to
 * the queue</li>
 * </ul>
 * <p>
 * Inactive =(Start processing)=> Active =(Failure)=> Failed =(Retry)=> Delayed
 * =(Promotion)=> Inactive
 * </p>
 * <p>
 * Inactive =(Start processing)=> Active =(Complete)=> Completed =(Restart)=>
 * Inactive
 * </p>
 * 
 * 
 * @author Steven Chen
 *
 */
public enum JobState {

	INACTIVE, ACTIVE, COMPLETE, FAILED, DELAYED

}
