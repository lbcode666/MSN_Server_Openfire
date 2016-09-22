package org.msn.friendRecommendApi;

import java.io.File;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;  
import org.jivesoftware.openfire.container.PluginManager;
  
public abstract class AbstractFriendRecommendPlugin implements Plugin{
	
	private XMPPServer server;
	private FriendRecommendInterface recommend;
	
	public AbstractFriendRecommendPlugin(FriendRecommendInterface recommend)
	{
		this.recommend=recommend;
	}
  
    @Override  
    public void initializePlugin(PluginManager manager, File pluginDirectory) {  
        System.out.println("FriendRecommend插件启动。");  
        server=XMPPServer.getInstance();
        server.getIQRouter().addHandler(new FriendRecommendIQHandler(recommend));
    }

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		 System.out.println("FriendRecommend插件销毁。");  
	}
	
}