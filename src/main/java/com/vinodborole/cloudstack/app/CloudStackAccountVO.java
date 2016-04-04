package com.vinodborole.cloudstack.app;

public class CloudStackAccountVO {
	private String accountAPIKey;
	private String accountSecretKey;
	private String cloudEndPoint;
	private String virtualAccountName;
	private String password;
	 
	public CloudStackAccountVO(String cloudIp, String pikey, String secretkey) {
		this.accountAPIKey=pikey;
		this.accountSecretKey=secretkey;
		this.cloudEndPoint=cloudIp;
	}
	public String getAccountAPIKey() {
		return accountAPIKey;
	}
	public void setAccountAPIKey(String accountAPIKey) {
		this.accountAPIKey = accountAPIKey;
	}
	public String getAccountSecretKey() {
		return accountSecretKey;
	}
	public void setAccountSecretKey(String accountSecretKey) {
		this.accountSecretKey = accountSecretKey;
	}
	public String getCloudEndPoint() {
		return cloudEndPoint;
	}
	public void setCloudEndPoint(String cloudEndPoint) {
		this.cloudEndPoint = cloudEndPoint;
	}
	public String getVirtualAccountName() {
		return virtualAccountName;
	}
	public void setVirtualAccountName(String virtualAccountName) {
		this.virtualAccountName = virtualAccountName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
