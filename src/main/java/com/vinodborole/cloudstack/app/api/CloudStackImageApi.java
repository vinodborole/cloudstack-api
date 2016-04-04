package com.vinodborole.cloudstack.app.api;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.cloudstack.domain.OSType;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.domain.TemplateMetadata;
import org.jclouds.cloudstack.features.TemplateApi;
import org.jclouds.cloudstack.options.ListOSTypesOptions;
import org.jclouds.cloudstack.options.RegisterTemplateOptions;

import com.google.common.collect.Iterables;
import com.vinodborole.cloudstack.app.CloudStackAccountVO;
import com.vinodborole.cloudstack.app.CloudStackSession;

public class CloudStackImageApi {

    public static Set<Template> getTemplateList(CloudStackAccountVO account) {
        TemplateApi templateApi = CloudStackSession.getCloudStackDomainApi(account).getTemplateApi();
        Set<Template> templateLst = templateApi.listTemplates();
        for (Template template : templateLst) {
            System.out.println(template.getId() + ", " + template.getName());
        }
        return templateLst;
    }

    public static Template registerTemplate(CloudStackAccountVO account, String name, String osFamily, String root_ova, String location, String server_type,
            String distro, String distro_ver, String os_arc) throws Exception {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        System.out.println("Registering template with SP: name:" + name + ", osFamily:" + osFamily + ", ova:" + root_ova + ", location:" + location
                + ", server_type:" + server_type);
        TemplateMetadata templateMeta = TemplateMetadata.builder().name(name).osTypeId(getOsType(account, osFamily, distro, distro_ver, os_arc).getId())
                .displayText(name).build();

        System.out.println("TemplateMetaData" + templateMeta);

        RegisterTemplateOptions templateOption = new RegisterTemplateOptions();
        if (!server_type.equals("APPLICATION")) {
            templateOption.buildQueryParameters().put("details[0].rootDiskController", "scsi");
        }
        templateOption.buildQueryParameters().put("isextractable", "true");
        Set<Template> returnTemplateList = null;
        try {
            returnTemplateList = domainApi.getTemplateApi().registerTemplate(templateMeta, "OVA", "vmware",
                    "http://" + getIpv4NonLoopbackAddress() + ":8080/" + root_ova.substring("/opt/images".length(), root_ova.length()), location,
                    templateOption);
        } catch (Exception e) {
            CloudStackSession.getCloudStackContext(account).close();
            throw e;
        }
        Template template = Iterables.getOnlyElement(returnTemplateList);
        System.out.println("Register template from SP returned:" + template);
        return template;

    }

    public static Template monitorRegisterTemplateJob(CloudStackAccountVO account, String templateId, String location) throws Exception {
        Template template;
        boolean runNext = true;
        int initial_wait = 0;
        System.out.println("monitoring register template operation for template with backend-id:" + templateId);
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        do {
            Thread.sleep(30 * 1000);
            template = domainApi.getTemplateApi().getTemplateInZone(templateId, location);
            if ((template.getStatus() == null) && initial_wait < 20) {
                initial_wait++;
                System.out.println("Unable to query status of template V1:" + template + ", will try for " + (20 - initial_wait) * 30 + " more secs");
                continue;
            } else if (template.getStatus() == null) {
                throw new Exception("Template registration with service" + " provider timed out" + template, null);
            }
            if (template.getStatus().equals(Template.Status.DOWNLOAD_ERROR) || template.getStatus().equals(Template.Status.ABANDONED)
                    || template.getStatus().equals(Template.Status.UPLOAD_ERROR) || template.getStatus().equals(Template.Status.UNRECOGNIZED)) {
                System.err.println("monitorRegisterTemplate: Template install failed with error : " + template.getStatus());
                runNext = false;
                String status = null;
                if (template.getStatus().equals(Template.Status.UNRECOGNIZED)) {
                    status = "UNRECOGNIZED";
                } else {
                    status = template.getStatus().toString();
                }
                throw new Exception("Template registration with service-provider failed with error:" + status, null);
            }
        } while (!template.isReady() && runNext);

        return template;
    }

    public static String getIpv4NonLoopbackAddress() throws SocketException {
        NetworkInterface i = NetworkInterface.getByName("eth0");
        for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
            InetAddress addr = en2.nextElement();
            if (!addr.isLoopbackAddress()) {
                if (addr instanceof Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        }
        return null;
    }

    public static OSType getOsType(CloudStackAccountVO account, String osFamily, String distro, String distro_ver, String os_arc) {
        CloudStackDomainApi domainApi = CloudStackSession.getCloudStackDomainApi(account);
        ListOSTypesOptions osTypeOptions = new ListOSTypesOptions();
        String osCategoriesId = null;
        String desc = null;
        OSType osType = null;
        int matched = 0;
        if (osFamily == null)
            osFamily = "";
        if (distro == null)
            distro = "";
        if (distro_ver == null)
            distro_ver = "";
        if (os_arc == null)
            os_arc = "";
        Map<String, String> osCategories = domainApi.getGuestOSApi().listOSCategories();
        osCategoriesId = getOsCategoriesId(osFamily, osCategories);
        osTypeOptions.OSCategoryId(osCategoriesId);
        Set<OSType> availOsList = domainApi.getGuestOSApi().listOSTypes(osTypeOptions);
        for (OSType os : availOsList) {
            desc = os.getDescription().toLowerCase();
            if (desc.contains(osFamily.toLowerCase()) && desc.contains(distro.toLowerCase()) && desc.contains(distro_ver.toLowerCase())
                    && desc.contains(os_arc.toLowerCase())) {
                osType = os;
                break;
            } else if (desc.contains(osFamily.toLowerCase()) && desc.contains(distro.toLowerCase()) && desc.contains(distro_ver.toLowerCase())) {
                osType = os;
                matched = 3;
            } else if (desc.contains(osFamily.toLowerCase()) && desc.contains(distro.toLowerCase()) && matched < 3) {
                osType = os;
                matched = 2;
            } else if (desc.contains(osFamily.toLowerCase()) && desc.contains(distro_ver.toLowerCase()) && matched < 3) {
                osType = os;
                matched = 2;
            } else if (desc.contains(osFamily.toLowerCase()) && matched < 2) {
                osType = os;
                matched = 1;
            } else {
                osType = os;
                matched = 0;
            }
        }
        return osType;
    }

    private static String getOsCategoriesId(String osFamily, Map<String, String> osCategories) {
        String osCategoriesId = null;
        for (String key : osCategories.keySet()) {
            if ("linux".equalsIgnoreCase(osFamily)) {
                if (("linux".equalsIgnoreCase(osCategories.get(key)) || "other".equalsIgnoreCase(osCategories.get(key)))) {
                    osCategoriesId = key;
                    break;
                }
            } else if (osCategories.get(key).equalsIgnoreCase(osFamily)) {
                osCategoriesId = key;
                break;
            }
        }
        if (osCategoriesId == null) {
            for (String key : osCategories.keySet()) {
                if ("Other".equalsIgnoreCase(osCategories.get(key))) {
                    osCategoriesId = key;
                    break;
                }
            }
        }

        return osCategoriesId;
    }
}
