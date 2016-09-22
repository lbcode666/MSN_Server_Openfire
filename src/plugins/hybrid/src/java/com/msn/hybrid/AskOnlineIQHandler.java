package com.msn.hybrid;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.json.JSONObject;
import org.json.JSONException;
import org.msn.noSqlConf.NoSqlConf;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import redis.clients.jedis.Jedis;

public class AskOnlineIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Ask Online IQ Handler";
	private static final String ELEMENT_NAME="askonline";
	private static final String NAMESPACE="com.msn.hybrid.askonline";
	
	MongoDatabase mongo;
	Jedis redis;
	
	public AskOnlineIQHandler()
	{
		
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		
		redis=NoSqlConf.createRedis();
		mongo=new MongoClient().getDatabase("openfire");
		
		System.out.println("注册了Ask Online IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO Auto-generated method stub
		
		System.out.println("拦截到Hybrid插件ask online的IQ包   ");
		
		JID from=packet.getFrom();
		String fromJID=from.toString();
		String fromUsername=from.getNode();
		
		IQ reply=IQ.createResultIQ(packet);
		
		Element askonlineElement=packet.getChildElement();
		String type=askonlineElement.attributeValue("type");
		Element childElement=reply.setChildElement(ELEMENT_NAME, NAMESPACE);
		
		if(type.equals("begin"))//received IQ 1,thus to send IQ 3 to all online users
		{
			String throwid=askonlineElement.elementText("throwid");
			String destination=askonlineElement.elementText("destination");
			String size=askonlineElement.elementText("size");
			
			Set<String> onlineUsers=org.msn.presence.UserPresence.onlineUsers();
			onlineUsers.remove("admin");
			onlineUsers.remove(fromUsername);
			
			System.out.println("HybridPlugin::现在在线的用户:"+onlineUsers.toString());
			
			redis.hset("throwid:"+throwid, "count", Integer.toString(onlineUsers.size()));
			
			for(String username:onlineUsers)
			{
				JID jid=new JID(username,HybridPlugin.HOST_IP,null);
			     
			     JSONObject json=new JSONObject();
			     try {
					json.put("element","askonline");
					 json.put("form","half");
				     json.put("throwid", throwid);
				     json.put("fromwho",fromUsername);
				     json.put("destination",destination);
				     json.put("size", size);
				} catch (JSONException e) {	}

			     Message message=new Message();
			     message.setFrom(HybridPlugin.HOST_IP);
			     message.setTo(jid);
			     message.setBody(json.toString());
			     
			     redis.hset("throwid:"+throwid, username, Integer.toString(-1));
			     
			     XMPPServer.getInstance().getRoutingTable().routePacket(jid, message, true);
			}
		}
		else if(type.equals("half"))
		{
			System.out.println("Received a packet of Half");
			String throwid=askonlineElement.elementText("throwid");
			String towho=askonlineElement.elementText("towho");
			String value=askonlineElement.elementText("value");
			
			redis.hset("throwid:"+throwid, fromUsername, value);
			int cnt=Integer.parseInt(redis.hget("throwid:"+throwid, "count"));
			redis.hset("throwid:"+throwid, "count", Integer.toString(--cnt));
			System.out.println("cnt="+cnt);
			if(cnt==0)
			{
				redis.hdel("throwid:"+throwid, "count");
				Map<String,String> map=redis.hgetAll("throwid:"+throwid);
				redis.del("throwid:"+throwid);
			    
			    Set<Map.Entry<String, String>> usernameAndValue=
			    		map.entrySet();
			    
			    int k=Integer.MAX_VALUE;
			    String forwarder="";
			    for(Map.Entry<String, String> entry:usernameAndValue)
			    {
			    	int v=(int)(Double.parseDouble(entry.getValue()));
			    	if(v<=k)
			    	{
			    		k=v;
			    		forwarder=entry.getKey();
			    	}
			    }
			    JID jid=new JID(towho,HybridPlugin.HOST_IP,null);
			     JSONObject json=new JSONObject();
			     try {
					json.put("element","responseonline");
				     json.put("form","begin");
				     json.put("throwid",throwid);
				     json.put("forwarder", forwarder);
				     json.put("value",k);
				     json.put("type",2);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			     
			     Message message=new Message();
			     message.setFrom(HybridPlugin.HOST_IP);
			     message.setTo(jid);
			     message.setBody(json.toString());
			     
			     XMPPServer.getInstance().getRoutingTable().routePacket(jid, message, true);
			}
		}
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}

}
