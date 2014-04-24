package org.flowvisor;

import java.util.HashMap;

import org.flowvisor.PortInfo.PortType;

public class LimeSwitch {

	private HashMap<Integer, PortInfo> portTable;
	
    public LimeSwitch(){
    	this.portTable 	= new HashMap<>();
    }
    
    public LimeSwitch(HashMap<Integer, PortInfo> portTable){
    	this.portTable = portTable;
    }
    
    
    public HashMap<Integer, PortInfo> getPortTable(){
    	return portTable;
    }
    
    public void insertPortTable(HashMap<Integer, PortInfo> pTable){
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

