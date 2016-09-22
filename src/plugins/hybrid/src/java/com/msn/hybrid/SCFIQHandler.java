package com.msn.hybrid;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class SCFIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="SCF IQ Handler";
	private static final String ELEMENT_NAME="scf";
	private static final String NAMESPACE="com.msn.hybrid.scf";

	public SCFIQHandler() {
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		
		System.out.println("注册了SCF IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		System.out.println("拦截到Hybrid插件SCF的IQ包   ");
		
		JID from=packet.getFrom();
		String fromJID=from.toString();
		String fromUsername=from.getNode();
		
		IQ reply=IQ.createResultIQ(packet);
		
		Element scfElement=packet.getChildElement();
		String type=scfElement.attributeValue("type");
		Element childElement=reply.setChildElement(ELEMENT_NAME, NAMESPACE);
		
		if(type.equals("direct"))
		{
			String throwid=scfElement.elementText("throwid");
			String source=scfElement.elementText("source");
			String destination=scfElement.elementText("destination");
			String value=scfElement.elementText("value");
			String starttime=scfElement.elementText("starttime");
			String life=scfElement.elementText("life");
			
			Element passesElement=scfElement.element("passes");
			List passElements=passesElement.elements("pass");
			
			List<String> passList=new ArrayList<>();
			for(Object e:passElements)
			{
				String pass=((Element)e).getText();
				passList.add(pass);
			}
			
			JID toJID=new JID(destination,HybridPlugin.HOST_IP,null);
			
			JSONObject json=new JSONObject();
			try {
				json.put("element", "directSCF");
				json.put("throwid", throwid);
				json.put("source", source);
				json.put("value", value);
				json.put("starttime", starttime);
				json.put("passes", passList);
			} catch (JSONException e) {}
			
			Message message=new Message();
			message.setFrom(HybridPlugin.HOST_IP);
			message.setTo(toJID);
			message.setBody(json.toString());
			
			 XMPPServer.getInstance().getRoutingTable().routePacket(toJID, message, true);
		}
		else if(type.equals("forward"))
		{
			String throwid=scfElement.elementText("throwid");
			String source=scfElement.elementText("source");
			String destination=scfElement.elementText("destination");
			String forwarder=scfElement.elementText("forwarder");
			String value=scfElement.elementText("value");
			String starttime=scfElement.elementText("starttime");
			String life=scfElement.elementText("life");
			
			Element passesElement = scfElement.element("passes");
			
			List passElements=passesElement.elements("pass");
			
			List<String> passList=new ArrayList<>();
			for(Object e:passElements)
			{
				String pass=((Element)e).getText();
				passList.add(pass);
			}
			
			JID toJID=new JID(forwarder,HybridPlugin.HOST_IP,null);
			
			JSONObject json=new JSONObject();
			try {
				json.put("element", "forwardSCF");
				json.put("throwid", throwid);
				json.put("source", source);
				json.put("destination", destination);
				json.put("value", value);
				json.put("starttime", starttime);
				json.put("life",life);
				json.put("passes", passList);
			} catch (JSONException e) {}
			
			Message message=new Message();
			message.setFrom(HybridPlugin.HOST_IP);
			message.setTo(toJID);
			message.setBody(json.toString());
			
			 XMPPServer.getInstance().getRoutingTable().routePacket(toJID, message, true);
		}
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}

}
