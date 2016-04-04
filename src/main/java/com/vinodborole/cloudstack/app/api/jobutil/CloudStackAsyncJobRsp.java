
package com.vinodborole.cloudstack.app.api.jobutil;


public class CloudStackAsyncJobRsp {
	String jobid;
	String jobresult;

	public String getJobresult() {
		return jobresult;
	}

	public void setJobresult(String jobresult) {
		this.jobresult = jobresult;
	}

	public CloudStackAsyncJobRsp(String jobid) {
		super();
		this.jobid = jobid;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

}
