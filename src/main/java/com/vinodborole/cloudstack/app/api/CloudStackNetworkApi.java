package com.vinodborole.cloudstack.app.api;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.cloudstack.domain.AsyncCreateResponse;
import org.jclouds.cloudstack.domain.IPForwardingRule;
import org.jclouds.cloudstack.domain.Network;
import org.jclouds.cloudstack.domain.NetworkOffering;
import org.jclouds.cloudstack.domain.PublicIPAddress;
import org.jclouds.cloudstack.features.NATApi;
import org.jclouds.cloudstack.features.NetworkApi;
import org.jclouds.cloudstack.options.AssociateIPAddressOptions;
import org.jclouds.cloudstack.options.CreateIPForwardingRuleOptions;
import org.jclouds.cloudstack.options.CreateNetworkOptions;
import org.jclouds.cloudstack.options.ListNetworkOfferingsOptions;
import org.jclouds.cloudstack.options.ListPublicIPAddressesOptions;

import com.vinodborole.cloudstack.app.CloudStackAccountVO;
import com.vinodborole.cloudstack.app.CloudStackSession;
import com.vinodborole.cloudstack.app.api.jobutil.CloudStackAsyncJobUtil;

public class CloudStackNetworkApi {

    public static void deleteNetwork(CloudStackAccountVO account, String networkId) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        NetworkApi networkApi = domainApi.getNetworkApi();
        String jobId = networkApi.deleteNetwork(networkId);
        CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, jobId);
    }

    public static void disassociatePublicIPAddress(CloudStackAccountVO account, String vmId) {
        PublicIPAddress publiIPAddress = CloudStackNetworkApi.getAssociatedPublicIP(account, vmId);
        if (publiIPAddress != null) {
            CloudStackSession.getCloudStackDomainApi(account).getAddressApi().disassociateIPAddress(publiIPAddress.getId());
        }
    }

    public static void associatePublicIPAddress(CloudStackAccountVO account, String locationId, String networkId, String vmId) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        PublicIPAddress assignedIpID = CloudStackNetworkApi.getAssociatedPublicIP(account, vmId);
        if (assignedIpID == null) {
            AssociateIPAddressOptions associateIPOptions = new AssociateIPAddressOptions();
            associateIPOptions.networkId(networkId);
            AsyncCreateResponse ipaddress = domainApi.getAddressApi().associateIPAddressInZone(locationId, associateIPOptions);
            boolean success = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, ipaddress.getJobId());
            if (success) {
                domainApi.getNATApi().enableStaticNATForVirtualMachine(vmId, ipaddress.getId());
                assignedIpID = domainApi.getAddressApi().getPublicIPAddress(ipaddress.getId());
                if (assignedIpID == null) {
                    throw new Exception("Unable to assign a public ip...", null);
                }
            }
        }
    }

    public static String createNetwork(CloudStackAccountVO account, String networkName, String cidrBlock, String locationId) throws Exception {
        SubnetUtils netUtils = new SubnetUtils(cidrBlock);
        ListNetworkOfferingsOptions networkOption = new ListNetworkOfferingsOptions();
        String networkOffering = "DefaultIsolatedNetworkOfferingWithSourceNatService";
        networkOption.name(networkOffering);
        networkOption.isShared(Boolean.FALSE);
        networkOption.zoneId(locationId);
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        Set<NetworkOffering> networkOfferings = domainApi.getOfferingApi().listNetworkOfferings(networkOption);
        if (networkOfferings.isEmpty() || networkOfferings == null) {
            String erroMsg = "Unable to find network offering:" + networkOffering + " at SP\n";
            throw new Exception(erroMsg);
        } else if (networkOfferings.size() > 1) {
            System.out.println("Found multiple network offerings with name:" + networkOffering + " at SP\n" + networkOfferings);
        }
        NetworkOffering offering = null;
        Iterator<NetworkOffering> iter = networkOfferings.iterator();
        while (iter.hasNext()) {
            offering = iter.next();
            if (offering.getName().equals(networkOffering)) {
                if (networkOfferings.size() > 1) {
                    System.out.println("Choosing service offering:" + offering + " from SP cloud");
                }
                break;
            }
            offering = null;
        }
        if (offering == null) {
            System.err.println("Unable to find network offering:" + networkOffering + " at SP\n");
            throw new Exception("Unable to find network offering:" + networkOffering + " at Service Provider Cloud", null);
        }
        CreateNetworkOptions options = new CreateNetworkOptions();
        options.isDefault(Boolean.TRUE);
        options.gateway(netUtils.getInfo().getLowAddress());
        options.endIP(netUtils.getInfo().getHighAddress());
        options.netmask(netUtils.getInfo().getNetmask());
        Network nwt = domainApi.getNetworkApi().createNetworkInZone(locationId, offering.getId(), networkName, "Network", options);
        System.out.println("Network created : " + nwt.getId());
        return nwt.getId();
    }

    public static Set<IPForwardingRule> getIPForwardingRulesForIPAddress(CloudStackAccountVO account, String ipAdressID) {
        Set<IPForwardingRule> rules = CloudStackSession.getCloudStackDomainApi(account).getNATApi().getIPForwardingRulesForIPAddress(ipAdressID);
        if (rules != null && !rules.isEmpty()) {
            for (IPForwardingRule rule : rules) {
                System.out.println(rule.getId());
            }
        }
        return rules;
    }

    public static void addIPForwardingRulesForIPAddress(CloudStackAccountVO account, String ipAddressID, String protocol, int statPort, int endPort)
            throws Exception {
        CreateIPForwardingRuleOptions option = new CreateIPForwardingRuleOptions().endPort(endPort);
        AsyncCreateResponse response = CloudStackSession.getCloudStackDomainApi(account).getNATApi()
                .createIPForwardingRule(ipAddressID, protocol, statPort, option);
        boolean status = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(CloudStackSession.getCloudStackDomainApi(account), response.getJobId());
        if (status) {
            System.out.println("Rule applied successfully");
        }
    }

    public static void removeIPForwardingRulesForIPAddress(CloudStackAccountVO account, String ipAddressID) {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        Set<IPForwardingRule> rules = getIPForwardingRulesForIPAddress(account, ipAddressID);
        if (rules != null && !rules.isEmpty()) {
            NATApi natApi = domainApi.getNATApi();
            for (IPForwardingRule rule : rules) {
                natApi.deleteIPForwardingRule(rule.getId());
            }
        }
    }

    public static PublicIPAddress getAssociatedPublicIP(CloudStackAccountVO account, String vmId) {
        ListPublicIPAddressesOptions options = new ListPublicIPAddressesOptions();
        options.allocatedOnly(Boolean.TRUE);
        Set<PublicIPAddress> ipList = CloudStackSession.getCloudStackDomainApi(account).getAddressApi().listPublicIPAddresses(options);
        PublicIPAddress assignedIpID = null;
        for (PublicIPAddress ip : ipList) {
            if (ip.isStaticNAT()) {
                if (ip.getVirtualMachineId() != null) {
                    if (ip.getVirtualMachineId().equals(vmId)) {
                        assignedIpID = ip;
                        break;
                    }
                }
            }
        }
        return assignedIpID;
    }
}
