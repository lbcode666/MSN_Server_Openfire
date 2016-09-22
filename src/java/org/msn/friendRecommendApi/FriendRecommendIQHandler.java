package org.msn.friendRecommendApi;

import java.util.List;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class FriendRecommendIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Friend Recommend IQ Handler";
	private static final String ELEMENT_NAME="query";
	private static final String NAMESPACE="com.msn.friendRecommend";
	
	private FriendRecommendInterface recommend;
	
	public FriendRecommendIQHandler(FriendRecommendInterface recommend) {
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		this.recommend=recommend;
		
		System.out.println("注册了Friend IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO Auto-generated method stub
		IQ reply=IQ.createResultIQ(packet);
		
		if(IQ.Type.get.equals(packet.getType()))
		{
			System.out.println("FriendRecommendPlugin::拦截一个好友推荐请求包。"); 
			
			JID from = packet.getFrom();
			String username = from.getNode();//获取用户名

		     Element childElement = reply.setChildElement(ELEMENT_NAME,NAMESPACE);
			
			System.out.println("FriendRecommendPlugin::"+username+"请求推荐好友。");
			
			try
			{
				recommend.setUsername(username);
				List<String> recommendedFriends=recommend.recommendedFriends();
			
				for(String friend:recommendedFriends)
				{
					Element item=childElement.addElement("item");
					item.addAttribute("username", friend);
				}
			
				reply.setChildElement(childElement);
				System.out.println("FriendRecommendPlugin::"+reply.toString());		
			}
			catch(Exception e)
			{
				e.printStackTrace();
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
