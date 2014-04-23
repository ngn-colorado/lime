package org.flowvisor;

import java.util.Hashtable;

import org.flowvisor.PortInfo.PortType;

public class LimeSwitch {

	private Hashtable<Integer, PortInfo> portTable;
	
    public LimeSwitch(){
    	this.portTable 	= new Hashtable<>();
    }
    
    public LimeSwitch(Hashtable<Integer, PortInfo> portTable){
    	this.portTable = portTable;
    }
    
    
    public Hashtable<Integer, PortInfo> getPortTable(){
    	return portTable;
    }
    
    public void insertPortTable(Hashtable<Integer, PortInfo> pTable){
    	this.portTable = pTable;
    }
    
    public int getNumberOfPorts(){
    	return portTable.size();
    }
    
    public void addPort(int portNumber, PortType pType, String attMAC, String attIP){
    	PortInfo pInfo = new PortInfo(pType, attMAC, attIP);
    	portTable.put(portNumber, pInfo);
    }
    
    public void removePort(int portNumber){
    	portTable.remove(portNumber);
    }
}

