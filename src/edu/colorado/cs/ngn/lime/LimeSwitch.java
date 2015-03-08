package edu.colorado.cs.ngn.lime;

import java.util.HashMap;

import edu.colorado.cs.ngn.lime.util.PortInfo;
import edu.colorado.cs.ngn.lime.util.PortInfo.PortType;

public class LimeSwitch {

	private HashMap<Short, PortInfo> portTable;
	
    public LimeSwitch(){
    	this.portTable 	= new HashMap<>();
    }
    
    public LimeSwitch(HashMap<Short, PortInfo> portTable){
    	this.portTable = portTable;
    }
    
    
    public HashMap<Short, PortInfo> getPortTable(){
    	return portTable;
    }
    
    public void insertPortTable(HashMap<Short, PortInfo> pTable){
    	this.portTable = pTable;
    }
    
    public int getNumberOfPorts(){
    	return portTable.size();
    }
    
    public void addPort(short portNumber, PortType pType, String attMAC, String attIP){
    	PortInfo pInfo = new PortInfo(pType, attMAC, attIP);
    	portTable.put(portNumber, pInfo);
    }
    
    public void removePort(int portNumber){
    	portTable.remove(portNumber);
    }
}

