
package com.vinodborole.cloudstack.app.api.jobutil;

import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.cloudstack.domain.AsyncJob;


public class CloudStackAsyncJobUtil {
	
	public static boolean monitorCloudstackAsyncJobForTime(CloudStackDomainApi domainApi, String jobId, Integer timeout_in_secs) throws Exception {
		
		if (timeout_in_secs == 0) {
			timeout_in_secs = 10; 
		}
		AsyncJob<Object> job = domainApi.getAsyncJobApi().getAsyncJob(jobId);
		try {
			while (!job.hasFailed() && !job.hasSucceed()) {
				Thread.sleep(timeout_in_secs * 1000);
				job = domainApi.getAsyncJobApi().getAsyncJob(jobId);
			}
		} catch (InterruptedException e) {
			throw e;
		}

		if (job.hasFailed()) {
			System.err.println("Async job failed : " + job);
			throw new Exception(job.getError().getErrorText());
		}
		if(job.hasSucceed()){
			return true;
		}
		return false;
	}
	
	public static boolean monitorCloudStackAsyncJob(CloudStackDomainApi domainApi, String jobId) throws Exception{
		AsyncJob<String> asyncJob = domainApi.getAsyncJobApi().getAsyncJob(jobId);
		
		while (asyncJob.getStatus().equals(AsyncJob.Status.IN_PROGRESS)) {
			asyncJob = domainApi.getAsyncJobApi().getAsyncJob(jobId);
			try {
				Thread.sleep(3 * 1000);
			} catch (InterruptedException e) {
				System.err.println("Failed "+e.getMessage());
				throw new Exception("Failure :", e);
			}
		}
	
		if (asyncJob.hasSucceed()) {
			return true;
		}
		if (asyncJob.hasFailed()) {
			System.err.println("Async job failed : " + asyncJob);
			throw new Exception(asyncJob.getError().getErrorText());
		}
		return false;
	}
	
	public static AsyncJob<Object> getCompletedCloudstackAsyncJob(CloudStackDomainApi domainApi,String jobId, Integer timeout_in_secs) throws Exception, 
	InterruptedException {

		if (timeout_in_secs == 0) {
			timeout_in_secs = 10; 
		}
		AsyncJob<Object> job = domainApi.getAsyncJobApi().getAsyncJob(jobId);

		try {
			while (!job.hasFailed() && !job.hasSucceed()) {
				Thread.sleep(timeout_in_secs * 1000);
				job = domainApi.getAsyncJobApi().getAsyncJob(jobId);
			}
		} catch (InterruptedException e) {
			throw e;
		}

		if (job.hasFailed()) {
			System.err.print("Async job failed : " + job);
			throw new Exception(job.getError().getErrorText());
		}
		return job;
	}

}
