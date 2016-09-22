package com.msn.hybrid;

import java.io.File;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

public class HybridPlugin implements Plugin {

	private XMPPServer server;
	
	public static final String HOST_IP="192.168.0.201";
	
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub

		System.out.println("Hybrid插件启动。");  
        server=XMPPServer.getInstance();
        server.getIQRouter().addHandler(new NetworkSpeedIQHandler());
        server.getIQRouter().addHandler(new AskOnlineIQHandler());
        server.getIQRouter().addHandler(new SCFIQHandler());
        server.getIQRouter().addHandler(new NoticeIQHandler());
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		 System.out.println("Hybrid插件销毁。");  

	}

}
