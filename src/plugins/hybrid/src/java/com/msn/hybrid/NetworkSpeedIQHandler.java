package com.msn.hybrid;

import java.util.Arrays;

import org.bson.Document;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;


//获取并解析来自客户端的报告自己当前网速的报文，存储于mongoDB中
public class NetworkSpeedIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Network Speed IQ Handler";
	private static final String ELEMENT_NAME="networkspeed";
	private static final String NAMESPACE="com.msn.hybrid.networkspeed";
	
	MongoDatabase mongo;
	
	public NetworkSpeedIQHandler()
	{
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		
		mongo=new MongoClient().getDatabase("openfire");
		System.out.println("注册了Network Speed IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO Auto-generated method stub
		System.out.println("拦截到Hybrid插件报告用户网速的IQ包");
		
		String fromJID=packet.getFrom().toString();
		
		Element networkspeedElement=packet.getChildElement();
		String networkspeedStr=networkspeedElement.elementText("speed");
		int networkspeed=Integer.parseInt(networkspeedStr);
		MongoCollection<Document> networkspeedCollection=mongo.getCollection("hybrid");
		
		if(networkspeedCollection.find(Filters.eq("jid", fromJID)).first()!=null)
		{
			networkspeedCollection.updateOne(Filters.eq("jid", fromJID), new Document("$push",new Document("speed",networkspeed)));
		}
		else
		{
			Document doc=new Document("jid",fromJID).append("speed", Arrays.asList(networkspeed));
			networkspeedCollection.insertOne(doc);
		}
		
		return null;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}

}
