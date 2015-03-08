package edu.colorado.cs.ngn.lime.util;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

//import org.libvirt.*;

public class LimeVMMigrater {
	//qemu+ssh://
	private static final String URI_STRING = "qemu+ssh://";
	public static boolean liveMigrateQemuVM(String sourceIP, String destIP, String vmDomain){
		if(!LimeUtils.validIPAddress(sourceIP)){
			System.out.println("Source IP address is invalid.");
			return false;
		}
		if(!LimeUtils.validIPAddress(destIP)){
			System.out.println("Destination IP address is invalid.");
			return false;
		}
		System.out.println("Validated ip addresses");
		String src_uri = URI_STRING+sourceIP+"/system";
		String dest_uri = URI_STRING+destIP+"/system";
		Connect src = null;
		Connect dst = null;
		Domain domain_to_migrate = null;
		try {
			System.out.println("Attempting to connect to "+src_uri);
			src = new Connect(src_uri);
			System.out.println("Successfully connected to "+src_uri);
		} catch (LibvirtException e) {
			System.out.println("Could not connect to: "+src_uri);
			e.printStackTrace();
//			System.exit(-1);
			return false;
		}
		try {
			System.out.println("Attempting to connect to "+dest_uri);
			dst = new Connect(dest_uri);
			System.out.println("Successfully connected to "+dest_uri);
		} catch (LibvirtException e) {
			System.out.println("Could not connect to: "+dest_uri);
			e.printStackTrace();
//			System.exit(-1);
			return false;
		}
		if(src == null || dst == null){
//			throw new IllegalStateException("Cannot reach a state with two null connections");
			System.out.println("Cannot reach a state with two null connections");
			return false;
		}
		try {
			domain_to_migrate = src.domainLookupByName(vmDomain);
		} catch (LibvirtException e) {
			System.out.println("Could not find domain "+vmDomain+" in domain");
			e.printStackTrace();
			return false;
//			System.exit(-2);
		}
		try {
			if(domain_to_migrate.isActive() == 1){
				Domain migrated_domain = domain_to_migrate.migrate(dst, 1, null, null, 0);
				System.out.println("Migrating "+domain_to_migrate.getName()+" from "+src.getHostName()+" to "+dst.getHostName());
				if(migrated_domain == null){
					System.out.println("Migration of "+domain_to_migrate.getName()+" unsuccessful.");
					return false;
				}
				else{
					while(migrated_domain.isActive() != 1){
						System.out.println("Waiting for domain to come up");
						Thread.sleep(1000);
					}
					System.out.println("Migration of "+domain_to_migrate.getName()+" successful.");
					return true;
				}
			} else{
				System.out.println("Please start "+domain_to_migrate);
				return false;
			}
		} catch (LibvirtException | InterruptedException e) {
			e.printStackTrace();
			return false;
//			System.exit(-3);
		}
	}
	
	/**
	 * Check if a given libvirt domain exists on given libvirt hypervisor
	 * @param hostIp String ip address of the libvirt hypervisor
	 * @param domain String libvirt domain of the vm to be checked 
	 * @return boolean true if the vm exists, false if not or an error is encountered
	 */
	public static boolean checkDomain(String hostIp, String domain){
		if(!LimeUtils.validIPAddress(hostIp)){
			System.out.println("IP address is invalid");
			return false;
		}
		try {
			//TODO: this doesn't work- it throws an exception about missing certificate. try ssh?
			Connect hypervisor = new Connect(URI_STRING+hostIp+"/system");
			Domain dom = hypervisor.domainLookupByName(domain);
			if(dom == null){
				System.out.println("Lookup failed");
				return false;
			}
			return true;
		} catch (LibvirtException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
