/**
 * 
 */
package com.vinodborole.cloudstack.app.api.jobutil;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class CloudStackAsyncJobRspDeserializer  implements JsonDeserializer<CloudStackAsyncJobRsp> {
		@Override
		  public CloudStackAsyncJobRsp deserialize(JsonElement je, Type typeOfT, JsonDeserializationContext context) throws 
		  JsonParseException
		  {
			final String uploadVolumeRsp = "uploadvolumeresponse";
			final String extractVolumeRsp = "extractvolumeresponse";
			final String queryAsyncJobResultRsp = "queryasyncjobresultresponse";

			String rsp_string = "";
			
			if (je.getAsJsonObject().has(uploadVolumeRsp)) {
				rsp_string = uploadVolumeRsp;
			} else if (je.getAsJsonObject().has(extractVolumeRsp)) {
				rsp_string = extractVolumeRsp;
			} else if (je.getAsJsonObject().has(queryAsyncJobResultRsp)) {
				rsp_string = queryAsyncJobResultRsp;
				JsonObject asyncJobRsp = je.getAsJsonObject().get(rsp_string).getAsJsonObject();
				CloudStackAsyncJobRsp rsp = new  CloudStackAsyncJobRsp(asyncJobRsp.get("jobid").getAsString());
				//System.out.print("\n job result is:" + asyncJobRsp.get("jobresult"));
				rsp.setJobresult(asyncJobRsp.get("jobresult").getAsJsonObject().get("volume").toString());
				return rsp;
			}
			
			if ("".equals(rsp_string)) {
				return null;
			} 
			return new CloudStackAsyncJobRsp(je.getAsJsonObject().get(rsp_string).getAsJsonObject().get("jobid").getAsString());
	}
}
