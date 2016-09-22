package com.msn.friendRecommend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.msn.friendRecommendApi.FriendRecommendInterface;
import org.msn.friendRecommendApi.UserInfo;
import org.msn.noSqlConf.NoSqlConf;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class FriendRecommendImpl implements FriendRecommendInterface {
	
	private String username;
	private UserInfo userInfo;
	private Jedis redis;
	
	public FriendRecommendImpl()
	{
		userInfo=new UserInfo();
		redis=NoSqlConf.createRedis();
	}
	
	public void setUsername(String name)
	{
		username=name;
		try {
			userInfo.setUsername(username);
		} catch (UserNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public List<String> recommendedFriends() {
		// TODO Auto-generated method stub
		List<String> recommendedFriends=new ArrayList<String>();
		
		HashSet<String> labelSet=userInfo.getLabels();//存储当前用户标签的集合
	    HashSet<String> friendSet=userInfo.getFriends();//存储当前用户的好友集合
	     
			try
			{
	            for(String label:labelSet)
	            	System.out.println("FriendRecommendPlugin::"+username+"的兴趣标签："+label);
	            for(String friend:friendSet)
	            	System.out.println("FriendRecommendPlugin::"+username+"的好友:"+friend);

				//获取其他每个用户的兴趣标签及好友列表，并计算相似度
				Set<String> users=redis.smembers("users");
				for(String tempUsername:users)
				{
					if(tempUsername.equals(username)||tempUsername.equals("admin"))//如果遍历到当前用户或管理员用户，则跳过
						continue;
					if(redis.sismember("friend:"+username, tempUsername))//如果某用户已经是当前用户的好友，则跳过
						continue;
					
					UserInfo tempUser=new UserInfo(tempUsername);
					HashSet<String> tempUserLabelSet=tempUser.getLabels();//存储某个用户的label的结果集合
					HashSet<String> tempUserFriendSet=tempUser.getFriends();//存储某个用户的friend的结果集合
	            	 
					FriendRecommendAlgorithm algorithm=
							new FriendRecommendAlgorithm(labelSet,tempUserLabelSet,friendSet,tempUserFriendSet);
	            	 boolean result=algorithm.isRecommended();
	            	 
	 		       if(result)
	 		    	   recommendedFriends.add(tempUsername);
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
			
			return recommendedFriends;
	}

}
