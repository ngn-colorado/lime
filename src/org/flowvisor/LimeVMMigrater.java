package org.flowvisor;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

//import org.libvirt.*;

public class LimeVMMigrater {
	public static boolean liveMigrateQemuVM(String sourceIP, String destIP, String vmDomain){
		if(!validIPAddress(sourceIP)){
			System.out.println("Source IP address is invalid.");
			return false;
		}
		if(!validIPAddress(destIP)){
			System.out.println("Destination IP address is invalid.");
			return false;
		}
		String src_uri = "qemu+ssh://"+sourceIP+"/system";
		String dest_uri = "qemu+ssh://"+destIP+"/system";
		Connect src = null;
		Connect dst = null;
		Domain domain_to_migrate = null;
		try {
			src = new Connect(src_uri);
		} catch (LibvirtException e) {
			System.out.println("Could not connect to: "+src_uri);
			e.printStackTrace();
//			System.exit(-1);
			return false;
		}
		try {
			dst = new Connect(dest_uri);
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
					System.out.println("Migration of "+domain_to_migrate.getName()+" successful.");
					return true;
				}
			} else{
				System.out.println("Please start "+domain_to_migrate);
				return false;
			}
		} catch (LibvirtException e) {
			e.printStackTrace();
			return false;
//			System.exit(-3);
		}
	}
	
	public static boolean validIPAddress(String ip){
		String[] tokens = ip.split("\\.");
		if(tokens.length != 4){
			return false;
		}
		for(String str : tokens){
			try{
				int i = Integer.parseInt(str);
				if(i < 0 || i > 255){
					return false;
				}
			} catch(NumberFormatException e){
				return false;
			}
		}
		return true;
	}
}
