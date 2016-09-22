package com.msn.precache;

import java.io.File;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.spi.PresenceManagerImpl;
import org.jivesoftware.openfire.user.User;
import org.msn.noSqlConf.NoSqlConf;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

import org.jivesoftware.database.DbConnectionManager;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.*;

public class PrecachePlugin implements Plugin{
private Jedis redis;
	
	private static final String LOAD_USER="select * from ofUser";
	private static final String LOAD_LABEL="select * from ofLabel";
	private static final String LOAD_FRIEND="select username,sub,nick from ofRoster";
	
	private Connection con=null;
	private PreparedStatement statement=null;
	private ResultSet rs=null;

	
	public PrecachePlugin()
	{	
		redis=NoSqlConf.createRedis();
	}
	
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub			
        System.out.println("PreCache插件启动。");  
        
        
        redis.flushDB();
        loadUser();
        loadLabel();
        loadFriend();
        
	}
	
	private void loadUser()
	{
		con = null;
        statement = null;
        rs = null;
        
        try
        {
        	 con = DbConnectionManager.getConnection();
             statement = con.prepareStatement(LOAD_USER);
             rs=statement.executeQuery();
             
             while(rs.next())
             {
            	 String username=rs.getString("username");
            	 String plainPassword=rs.getString("plainPassword");
            	 String encryptedPassword=rs.getString("encryptedPassword");
            	 String name=rs.getString("name");
            	 String email=rs.getString("email");
            	 String creationDate=rs.getString("creationDate");
            	 String modificationDate=rs.getString("modificationDate");
            	 
            	 redis.sadd("users", username);
            	 
            	 if(plainPassword!=null)
            		 redis.hset("user:"+username, "plainPassword", plainPassword);
            	 if(encryptedPassword!=null)
            		 redis.hset("user:"+username, "encryptedPassword", encryptedPassword);
            	 if(name!=null)
            		 redis.hset("user:"+username, "name", name);
            	 if(email!=null)
            		 redis.hset("user:"+username, "email", email);
            	 if(creationDate!=null)
            		 redis.hset("user:"+username, "creationDate", creationDate);
            	 if(modificationDate!=null)
            		 redis.hset("user:"+username, "modificationDate", modificationDate);
            	 
//            	 //给所有预先缓存的用户设置为未登录状态
//            	 redis.hset("user:"+username,"online",Boolean.toString(false));
             }
             
             System.out.println("PreCachePlugin::在Redis中缓存所有用户信息！");
        }
        catch(JedisConnectionException e)
        {
        	System.err.println("PreCachePlugin::无法连接Redis服务器！");
        }
        catch(Exception e)
        {}
        finally
        {
        	DbConnectionManager.closeConnection(rs, statement, con);
        }
	}
	
	private void loadLabel()
	{
		con = null;
        statement = null;
        rs = null;
        
        try
        {
        	 con = DbConnectionManager.getConnection();
             statement = con.prepareStatement(LOAD_LABEL);
             rs=statement.executeQuery();
             
             while(rs.next())
             {
            	 String username=rs.getString("username");
            	 
            	 for(int i=2;i<=6;i++)
            		 redis.sadd("label:"+username, rs.getString(i).trim());
             }
             
             System.out.println("PreCachePlugin::在Redis中缓存所有用户兴趣标签！");
        }
        catch(JedisConnectionException e)
        {
        	System.err.println("PreCachePlugin::无法连接Redis服务器！");
        }
        catch(Exception e)
        {}
        finally
        {
        	DbConnectionManager.closeConnection(rs, statement, con);
        }
	}
	
	private void loadFriend()
	{
		con = null;
        statement = null;
        rs = null;
        
        try
        {
        	 con = DbConnectionManager.getConnection();
             statement = con.prepareStatement(LOAD_FRIEND);
             rs=statement.executeQuery();
             
             while(rs.next())
             {
            	 String username=rs.getString("username");
            	 
            	 int sub=rs.getInt("sub");
            	 if(sub==3)
            	 {
            		 String friendName=rs.getString("nick");
            		 redis.sadd("friend:"+username, friendName);
            	 }
             }
             
             System.out.println("PreCachePlugin::在Redis中缓存所有用户好友关系！");
        }
        catch(JedisConnectionException e)
        {
        	System.err.println("PreCachePlugin::无法连接Redis服务器！");
        }
        catch(Exception e)
        {}
        finally
        {
        	DbConnectionManager.closeConnection(rs, statement, con);
        }
	}
	
	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		System.out.println("PreCache插件销毁。");  
	}
}
