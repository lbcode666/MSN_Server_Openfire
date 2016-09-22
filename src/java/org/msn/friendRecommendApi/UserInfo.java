package org.msn.friendRecommendApi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashSet;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.user.RedisUserProvider;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.msn.noSqlConf.NoSqlConf;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class UserInfo {
	
	private String username;
    private Jedis redis;
    private User user;
    
	private static final String LOAD_LABEL="select * from ofLabel WHERE username=? ";
	private static final String LOAD_FRIEND="select sub,nick from ofRoster WHERE username=?";
	
	public UserInfo()
	{
		redis=NoSqlConf.createRedis();
	}
	
	public UserInfo(String name) throws UserNotFoundException
	{
		this.username=name;
		user=new RedisUserProvider().loadUser(username);
		redis=NoSqlConf.createRedis();
	}
	
	public void setUsername(String name) throws UserNotFoundException
	{
		if(!redis.sismember("users", name))
			throw new UserNotFoundException();
		
		username=name;
		user=new RedisUserProvider().loadUser(username);
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getEmail()
	{
		return user.getEmail();
	}
	
	public Date getCreationDate()
	{
		return user.getCreationDate();
	}
	
	public Date getModificationDate()
	{
		return user.getModificationDate();
	}
	
	public HashSet<String> getLabels()
	{
		HashSet<String> labelSet=new HashSet<String>();
		try
		{
			Connection con = DbConnectionManager.getConnection();
	        PreparedStatement pstmt = null;
	        ResultSet rs = null;
			
            if(redis.exists("label:"+username))//如果有数据，将label1--5添加入userLabelSet
            	labelSet.addAll(redis.smembers("label:"+username));
            else//没有数据则从后台MySQL取数据，并缓存之
            {
                pstmt = con.prepareStatement(LOAD_LABEL);
                pstmt.setString(1, username);
                rs = pstmt.executeQuery();
                
                if(rs.next())
                	for(int i=2;i<=6;i++)
                	{
                		String label=rs.getString(i).trim();
                		labelSet.add(label);
                		redis.sadd("label:"+username, label);
                	}
            }
		}
		catch(JedisConnectionException e)
		{
        	System.err.println("FriendRecommendPlugin::无法连接Redis服务器！");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return labelSet;
	}

	public HashSet<String> getFriends()
	{
		HashSet<String> friendSet=new HashSet<String>();
		
		try
		{
			Connection con = DbConnectionManager.getConnection();
	        PreparedStatement pstmt = null;
	        ResultSet rs = null;
	        
			if(redis.exists("friend:"+username))//将当前用户的好友加入friendSet
            	friendSet.addAll(redis.smembers("friend:"+username));
            else//没有数据则从后台MySQL取数据，并缓存之
            {
            	pstmt=con.prepareStatement(LOAD_FRIEND);
            	pstmt.setString(1, username);
            	rs=pstmt.executeQuery();
            	
            	while(rs.next())
            	{
            		int sub=rs.getInt("sub");
            		if(sub==3)
            		{
            			String friendName=rs.getString("nick").trim();
            			friendSet.add(friendName);
            			redis.sadd("friend:"+username, friendName);
            		}
            	}
            }
		}
		catch(JedisConnectionException e)
		{
        	System.err.println("FriendRecommendPlugin::无法连接Redis服务器！");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return friendSet;
	}
}
