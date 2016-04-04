package com.vinodborole.cloudstack.app;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackContext;
import org.jclouds.cloudstack.domain.PublicIPAddress;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Location;

import com.vinodborole.cloudstack.app.api.CloudStackComputeApi;
import com.vinodborole.cloudstack.app.api.CloudStackGeneralApi;
import com.vinodborole.cloudstack.app.api.CloudStackImageApi;
import com.vinodborole.cloudstack.app.api.CloudStackNetworkApi;

public class CloudStackApp {
    public static void main(String[] args) throws Exception {

        String cloudIp = "http://10.36.53.4:8080/client/api";
        String pikey = "g98tgzuvTNMxOkxyo2t1WVH9X_wtzxHbSs8hTVAsFHeb9xXQSqdkBjKqGIlD0LEMM4vkUCppPke5LLDoFwKAhA";
        String secretkey = "7i2OTgirZsyviq5PdFfOH66vMXeRvzVqlQ0zXVtRGt4c24Yo259k9mNRo0RxU8ocWQ5Skf3Sb2DvwEH6oZD89Q";

        CloudStackAccountVO account = new CloudStackAccountVO(cloudIp, pikey, secretkey);
        CloudStackContext csContext = CloudStackSession.getCloudStackContext(account);
        ComputeServiceContext csServiceContext = CloudStackSession.getComputeServiceContext(account);

        String vmId = "eadb3f94-c2c2-4524-9da4-3a3e382664e8";
        String domainId = "09c03906-fdb3-4ba5-9650-15f580d7e1bd";
        VirtualMachine vm = CloudStackComputeApi.getVm(account, vmId);

        System.out.println("==================List VM's in a Domain==================");
        CloudStackComputeApi.listVmWithDomain(account, domainId);
        if (vm != null) {
            PublicIPAddress publicIPAddress = CloudStackNetworkApi.getAssociatedPublicIP(account, vm.getId());
            if (publicIPAddress != null) {
                System.out.println(publicIPAddress.getIPAddress() + ", " + publicIPAddress.getId());
            } else {
                System.out.println("No Public IP associated");
            }
        }
        System.out.println("==================List Template==================");
        CloudStackImageApi.getTemplateList(account);
        System.out.println("==================List Locations==================");
        Set<? extends Location> locations = CloudStackGeneralApi.getLocations(account);
        System.out.println("==================List Offerings==================");
        Set<ServiceOffering> offerings = CloudStackComputeApi.getServiceOfferings(account);
        ServiceOffering soffering = null;
        if (offerings != null && !offerings.isEmpty()) {
            for (ServiceOffering offering : offerings) {
                soffering = offering;
                System.out.println("CPU: " + offering.getCpuNumber() + ", RAM: " + offering.getMemory() + ", ID:" + offering.getId());
                break;
            }
        }
        String locationId = null;
        String zoneId = null;
        if (locations != null && !locations.isEmpty()) {
            for (Location location : locations) {
                System.out.println("==================List Zones for location Id: " + location.getId() + "=================");
                Set<Zone> zones = CloudStackGeneralApi.getZones(account, location.getId());
                for (Zone zone : zones) {
                    System.out.println("location Id: " + location.getId());
                    System.out.println("Zone Id: " + zone.getId());
                    locationId = location.getId();
                    zoneId = zone.getId();
                    break;
                }
                break;
            }
            System.out.println("==================Create Network==================");
            String networkId = CloudStackNetworkApi.createNetwork(account, "myfirstNetwork", "10.0.0.0/24", locationId);
            System.out.println("Created network Id: " + networkId);
            // CloudStackNetworkApi.deleteNetwork(account, networkId);
            System.out.println("==================Create VM==================");
            String vmID = CloudStackComputeApi.createVM(account, locationId, "8acbe892-110a-11e4-bba3-00505699713c", zoneId, soffering.getCpuNumber(),
                    soffering.getMemory(), networkId, "myFirstVM", true);
            System.out.println("Vm Id: " + vmID);

            PublicIPAddress assignedPublicIP = CloudStackNetworkApi.getAssociatedPublicIP(account, vmID);
            System.out.println("==================Get Rules attached to Public IP==================");
            CloudStackNetworkApi.getIPForwardingRulesForIPAddress(account, assignedPublicIP.getId());
            System.out.println("==================Add Rules for the public IP: " + assignedPublicIP.getIPAddress() + "==================");
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 80, 80);
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 22, 22);
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 443, 443);
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 8888, 8888);

            System.out.println("==================Get Rules attached to Public IP==================");
            CloudStackNetworkApi.getIPForwardingRulesForIPAddress(account, assignedPublicIP.getId());

            System.out.println("==================Remove ALL Rules attached to Public IP==================");
            CloudStackNetworkApi.removeIPForwardingRulesForIPAddress(account, assignedPublicIP.getId());
            System.out.println("==================Get Rules attached to Public IP==================");
            CloudStackNetworkApi.getIPForwardingRulesForIPAddress(account, assignedPublicIP.getId());
            System.out.println("==================Add Rules for the public IP: " + assignedPublicIP.getIPAddress() + "==================");
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 443, 443);
            CloudStackNetworkApi.addIPForwardingRulesForIPAddress(account, assignedPublicIP.getId(), "TCP", 8888, 8888);

            System.out.println("==================Get Rules attached to Public IP==================");
            CloudStackNetworkApi.getIPForwardingRulesForIPAddress(account, assignedPublicIP.getId());

        }
    }

}
