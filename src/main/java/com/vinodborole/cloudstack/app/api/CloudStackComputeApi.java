package com.vinodborole.cloudstack.app.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.cloudstack.domain.AsyncCreateResponse;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.features.OfferingApi;
import org.jclouds.cloudstack.features.VirtualMachineApi;
import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;
import org.jclouds.cloudstack.options.ListVirtualMachinesOptions;

import com.vinodborole.cloudstack.app.CloudStackAccountVO;
import com.vinodborole.cloudstack.app.CloudStackSession;
import com.vinodborole.cloudstack.app.api.jobutil.CloudStackAsyncJobUtil;

public class CloudStackComputeApi {

    public static Set<VirtualMachine> listVmWithDomain(CloudStackAccountVO account, String domainId) {
        ListVirtualMachinesOptions lstVmOptions = new ListVirtualMachinesOptions();
        lstVmOptions.domainId(domainId);
        Set<VirtualMachine> vms = CloudStackSession.getCloudStackDomainApi(account).getVirtualMachineApi().listVirtualMachines(lstVmOptions);
        if (vms != null && !vms.isEmpty()) {
            for (VirtualMachine virtualmachine : vms) {
                System.out.println(virtualmachine.getName());
            }
        }
        return vms;
    }

    public static VirtualMachine getVm(CloudStackAccountVO account, String vmId) {
        VirtualMachine vm = CloudStackSession.getCloudStackDomainApi(account).getVirtualMachineApi().getVirtualMachine(vmId);
        System.out.println(vm != null ? vm.getName() : "Not found");
        return vm;
    }

    public static Set<ServiceOffering> getServiceOfferings(CloudStackAccountVO account) {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        OfferingApi offeringApi = domainApi.getOfferingApi();
        Set<ServiceOffering> offerings = offeringApi.listServiceOfferings();
        if (offerings != null && !offerings.isEmpty()) {
            for (ServiceOffering offering : offerings) {
                System.out.println("CPU: " + offering.getCpuNumber() + ", RAM: " + offering.getMemory() + ", ID:" + offering.getId());
            }
        }
        return offerings;
    }

    public static boolean deleteVM(CloudStackAccountVO account, String vmId) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        VirtualMachineApi vmApi = domainApi.getVirtualMachineApi();
        boolean result = stopVM(account, vmId);
        if (result) {
            CloudStackNetworkApi.disassociatePublicIPAddress(account, vmId);
            String jobId = vmApi.destroyVirtualMachine(vmId);
            result = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, jobId);
            if (result) {
                return result;
            }
        }
        return false;
    }

    public static boolean stopVM(CloudStackAccountVO account, String vmId) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        VirtualMachineApi vmApi = domainApi.getVirtualMachineApi();
        String jobId = vmApi.stopVirtualMachine(vmId);
        boolean result = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, jobId);
        if (result) {
            return result;
        }
        return false;
    }

    public static boolean startVM(CloudStackAccountVO account, String vmId) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        VirtualMachineApi vmApi = domainApi.getVirtualMachineApi();
        String jobId = vmApi.startVirtualMachine(vmId);
        boolean result = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, jobId);
        if (result) {
            return result;
        }
        return false;
    }

    public static String createVM(CloudStackAccountVO account, String locationId, String templateId, String zoneId, int numCpu, int memory, String networkId,
            String vmName, boolean isPublicIPRequired) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        OfferingApi offeringApi = domainApi.getOfferingApi();
        Set<ServiceOffering> offerings = offeringApi.listServiceOfferings();
        List<ServiceOffering> supportedOfferingsLst = new ArrayList<ServiceOffering>();
        String spOfferings = "";
        for (ServiceOffering offer : offerings) {
            spOfferings += "[" + offer.getCpuNumber() + " core, " + offer.getMemory() + "MB],";
            if (offer.getCpuNumber() == numCpu && offer.getMemory() >= memory) {
                supportedOfferingsLst.add(offer);
            }
        }
        if (supportedOfferingsLst.size() < 1) {
            throw new Exception("Unable to match the cpu core and memory  required for server with the available service "
                    + " offerings at the service provider. The service offerings available are:" + spOfferings);
        }
        Comparator<ServiceOffering> OfferingComparator = new Comparator<ServiceOffering>() {
            @Override
            public int compare(ServiceOffering o1, ServiceOffering o2) {
                return Integer.valueOf(o1.getMemory()).compareTo(Integer.valueOf(o2.getMemory()));
            }
        };
        Collections.sort(supportedOfferingsLst, OfferingComparator);
        ServiceOffering supportedOfferings = supportedOfferingsLst.get(0);
        DeployVirtualMachineOptions options = new DeployVirtualMachineOptions();
        options.networkId(networkId);
        options.name(vmName);
        options.displayName(vmName);

        AsyncCreateResponse job = domainApi.getVirtualMachineApi().deployVirtualMachineInZone(zoneId, supportedOfferings.getId(), templateId, options);
        boolean result = CloudStackAsyncJobUtil.monitorCloudStackAsyncJob(domainApi, job.getJobId());
        if (result) {
            String vmId = job.getId();
            VirtualMachine vm = domainApi.getVirtualMachineApi().getVirtualMachine(vmId);
            if (vm.getState().equals(VirtualMachine.State.RUNNING)) {
                if (isPublicIPRequired) {
                    CloudStackNetworkApi.associatePublicIPAddress(account, locationId, networkId, vmId);
                }
            }
            return vmId;
        }
        return null;
    }

}
