package com.msn.catchPacket;

import java.io.File;
import java.util.Date;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

public class CatchPacketPlugin implements Plugin, PacketInterceptor {

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException {
		// TODO Auto-generated method stub
		String origin= incoming ? "客户端":"服务器";
		String result= processed? "已处理":"未经处理";
		
		if(packet instanceof IQ)
		{
			System.out.println();
			System.out.println("拦截一个来自"+origin+"的"+result+"的IQ包："+new Date().toString());
			System.out.println(packet.toString().trim());
			System.out.println();
		}
		else if(packet instanceof Message)
		{
			System.out.println();
			System.out.println("拦截一个来自"+origin+"的"+result+"的Message包："+new Date().toString());
			System.out.println(packet.toString().trim());
			System.out.println();
		}
		else if(packet instanceof Presence)
		{
			System.out.println();
			System.out.println("拦截一个来自"+origin+"的"+result+"的Presence包："+new Date().toString());
			System.out.println(packet.toString().trim());
			System.out.println();
		}
	}

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub
		System.out.println("CatchPacket插件启动。");
	    InterceptorManager.getInstance().addInterceptor(this);
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		InterceptorManager.getInstance().removeInterceptor(this);
		System.out.println("CatchPacket插件销毁。");
	}

}
