package com.msn.handleLabelPacket;

import java.io.File;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;  
import org.jivesoftware.openfire.container.PluginManager;  
 
public class HandleLabelPacketPlugin implements Plugin{  
	
	private XMPPServer server;
	
    @Override  
    public void initializePlugin(PluginManager manager, File pluginDirectory) {  
        System.out.println("HandleLabelPacket插件启动。");  
        server=XMPPServer.getInstance();
        server.getIQRouter().addHandler(new LabelIQHandler());
    }  
  
    @Override  
    public void destroyPlugin() {  
        System.out.println("HandleLabelPacketPlugin插件销毁。");  
    }
    
} 
