package org.msn.presence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.database.DbConnectionManager;
import org.msn.noSqlConf.NoSqlConf;

import redis.clients.jedis.Jedis;


/*
 * 用以获取用户的在线或者离线状态
 */
public class UserPresence {
	
	//判断username的用户是否在线
	public static boolean isOnline(String username)
	{
		String loadPresence="select * from ofPresence where username=?";
		
		try{	
			 Connection con = DbConnectionManager.getConnection();
             PreparedStatement statement = con.prepareStatement(loadPresence);
             statement.setString(1, username);
             ResultSet rs=statement.executeQuery();
             
             if(rs.next())
            	 return false;
             
             return true;
		}
		catch(SQLException e)
		{
			return false;
		}
	}
	
	//获取所有离线用户的集合
	public static Set<String> offlineUsers()
	{
		Set<String> offlineUsers=new HashSet<>();
		String loadOfflineUsers="select username from ofPresence";
		
		try{	
			 Connection con = DbConnectionManager.getConnection();
            PreparedStatement statement = con.prepareStatement(loadOfflineUsers);
            ResultSet rs=statement.executeQuery();
            
            while(rs.next())
            {
            	String name=rs.getString("username");
            	offlineUsers.add(name);
            }
            
            return offlineUsers;
		}
		catch(SQLException e)
		{
			return offlineUsers;
		}
	}
	
	//获取所有在线用户的集合
	public static Set<String> onlineUsers()
	{
		Jedis redis=NoSqlConf.createRedis();
		Set<String> allUsers=redis.smembers("users");
		
		allUsers.removeAll(offlineUsers());
		
		return allUsers;
	}
}
