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

public class NoticeIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Notice IQ Handler";
	private static final String ELEMENT_NAME="notice";
	private static final String NAMESPACE="com.msn.hybrid.notice";
	
	public NoticeIQHandler() {
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		
		System.out.println("注册了Notice  IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		System.out.println("拦截到Hybrid插件Notice的IQ包   ");
		
		JID from=packet.getFrom();
		String fromJID=from.toString();
		String fromUsername=from.getNode();
		
		IQ reply=IQ.createResultIQ(packet);
		
		Element noticeElement=packet.getChildElement();
		String type=noticeElement.attributeValue("type");
		Element childElement=reply.setChildElement(ELEMENT_NAME, NAMESPACE);
		
		String throwid=noticeElement.elementText("throwid");
		String source=noticeElement.elementText("source");
		String arrivetime=noticeElement.elementText("arrivetime");
		
		List pass1Elements=noticeElement.elements("pass1");
		List<String> pass1List=new ArrayList<>();
		for(Object e:pass1Elements)
		{
			String pass1=((Element)e).getText();
			pass1List.add(pass1);
		}
		
		JSONObject json=new JSONObject();
		try {
			json.put("element", "notice");
			json.put("throwid", throwid);
			json.put("arrivetime", arrivetime);
			json.put("destination", fromUsername );
		} catch (JSONException e) {}
		
		JID toJID=new JID(source,HybridPlugin.HOST_IP,null);
		
		Message message=new Message();
		message.setFrom(HybridPlugin.HOST_IP);
		message.setTo(toJID);
		message.setBody(json.toString());
		
		 XMPPServer.getInstance().getRoutingTable().routePacket(toJID, message, true);
		 
		 for(String pass1:pass1List)
		 {
			 toJID=new JID(pass1,HybridPlugin.HOST_IP,null);
			 
			 message.setTo(toJID);
			 XMPPServer.getInstance().getRoutingTable().routePacket(toJID, message, true);
		 }
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

}
