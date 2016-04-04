package com.vinodborole.cloudstack.app.api;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackDomainApi;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.domain.Location;

import com.vinodborole.cloudstack.app.CloudStackAccountVO;
import com.vinodborole.cloudstack.app.CloudStackSession;

public class CloudStackGeneralApi {

	public static Set<? extends Location> getLocations(CloudStackAccountVO account){
		Set<? extends Location> locations=CloudStackSession.getComputeServiceContext(account).getComputeService().listAssignableLocations();
		if(locations!=null && !locations.isEmpty() ){
			for(Location location : locations){
				System.out.println(location.getId() +", "+location.getDescription()+","+location.getScope().toString()+","+location.getMetadata().toString());
			}
		}
		return locations;
	}
	
	public static Set<Zone> getZones(CloudStackAccountVO account, String locationId){
		CloudStackDomainApi domainApi=CloudStackSession.getCloudStackDomainApi(account);
		Set<Zone> zones=domainApi.getZoneApi().listZones(new ListZonesOptions().id(locationId));
		if(zones!=null && !zones.isEmpty()){
			for(Zone zone : zones){
				System.out.println(zone.getDisplayText() +", "+zone.getId()+","+zone.getName()+","+zone.getDomain()+","+zone.getVLAN());
			}
		}
		return zones;
	}
}
