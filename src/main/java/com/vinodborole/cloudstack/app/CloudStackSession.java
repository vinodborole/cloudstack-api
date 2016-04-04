package com.vinodborole.cloudstack.app;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.CloudStackApiMetadata;
import org.jclouds.cloudstack.CloudStackContext;
import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.compute.ComputeServiceContext;

public class CloudStackSession { 

	public static CloudStackContext getCloudStackContext(CloudStackAccountVO tenantAccountVO) {
		CloudStackContext csContext = ContextBuilder.newBuilder(new CloudStackApiMetadata()).credentials(tenantAccountVO.getAccountAPIKey(),tenantAccountVO.getAccountSecretKey()).endpoint(tenantAccountVO.getCloudEndPoint()).build(CloudStackContext.class);
		return csContext;
	}

	public static ComputeServiceContext getComputeServiceContext(CloudStackAccountVO tenantAccountVO) {
		ComputeServiceContext computeContext = ContextBuilder.newBuilder(new CloudStackApiMetadata()).credentials(tenantAccountVO.getAccountAPIKey(),tenantAccountVO.getAccountSecretKey()).endpoint(tenantAccountVO.getCloudEndPoint()).build(ComputeServiceContext.class);
		return computeContext;

	} 

	public static CloudStackDomainApi getCloudStackDomainApi(CloudStackAccountVO tenantAccountVO) {
		CloudStackContext csContext = ContextBuilder.newBuilder(new CloudStackApiMetadata()).credentials(tenantAccountVO.getAccountAPIKey(),tenantAccountVO.getAccountSecretKey()).endpoint(tenantAccountVO.getCloudEndPoint()).build(CloudStackContext.class);
		return csContext.getDomainApi();
	}
}
