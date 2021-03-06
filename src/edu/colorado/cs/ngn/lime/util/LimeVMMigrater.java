package edu.colorado.cs.ngn.lime.util;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

/**
 * Class to interface with the libvirt Java api and perform live migrations
 * 
 * @author Michael Coughlin
 *
 */
public class LimeVMMigrater {
	private static final String URI_STRING = "qemu+ssh://";
	
	/**
	 * Use the libvirt Java api to live miration a VM. Polls every second
	 * until the migration is finished. Currently, only QEMU/KVM VMs are
	 * supported and only connections over ssh are supported.
	 * 
	 * Note: the linux user that is running this class must have an ssh key shared
	 * with the source and destination hypervisors and the keys of the hypervisors
	 * must have been accepted by this user. This is so that this program can log
	 * into the hypervisors without a password. 
	 * 
	 * @param sourceIP The source hypervisor ip address
	 * @param destIP The destination hypervisor ip address
	 * @param vmDomain The libvirt domain to migrate
	 * @return True if the vm was successfully migrated, False otherwise
	 */
	public static boolean liveMigrateQemuVM(String sourceIP, String destIP, String vmDomain){
		if(!LimeAPIUtils.validIPAddress(sourceIP)){
			System.out.println("Source IP address is invalid.");
			return false;
		}
		if(!LimeAPIUtils.validIPAddress(destIP)){
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
			return false;
		}
		try {
			System.out.println("Attempting to connect to "+dest_uri);
			dst = new Connect(dest_uri);
			System.out.println("Successfully connected to "+dest_uri);
		} catch (LibvirtException e) {
			System.out.println("Could not connect to: "+dest_uri);
			e.printStackTrace();
			return false;
		}
		if(src == null || dst == null){
			System.out.println("Cannot reach a state with two null connections");
			return false;
		}
		try {
			domain_to_migrate = src.domainLookupByName(vmDomain);
		} catch (LibvirtException e) {
			System.out.println("Could not find domain "+vmDomain+" in domain");
			e.printStackTrace();
			return false;
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
		}
	}
	
	/**
	 * Check if a given libvirt domain exists on given libvirt hypervisor
	 * 
	 * @param hostIp String ip address of the libvirt hypervisor
	 * @param domain String libvirt domain of the vm to be checked 
	 * @return boolean true if the vm exists, false if not or an error is encountered
	 */
	public static boolean checkDomain(String hostIp, String domain){
		if(!LimeAPIUtils.validIPAddress(hostIp)){
			System.out.println("IP address is invalid");
			return false;
		}
		try {
			Connect hypervisor = new Connect(URI_STRING+hostIp+"/system");
			Domain dom = hypervisor.domainLookupByName(domain);
			if(dom == null){
				System.out.println("Lookup failed");
				return false;
			}
			return true;
		} catch (LibvirtException e) {
			e.printStackTrace();
			return false;
		}
	}
}
