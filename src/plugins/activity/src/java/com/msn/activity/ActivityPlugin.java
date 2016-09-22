package com.msn.activity;

import java.io.File;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

public class ActivityPlugin implements Plugin {

	private XMPPServer server;
	
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub
		System.out.println("Activity插件启动。");  
        server=XMPPServer.getInstance();
        server.getIQRouter().addHandler(new ActivityIQHandler());
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		 System.out.println("Activity插件销毁。");  
	}

}
