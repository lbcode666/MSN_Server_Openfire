package org.msn.noSqlConf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import redis.clients.jedis.Jedis;

public class NoSqlConf {
	
    private static final String REDIS_CONF=".."+File.separator+"conf"+File.separator+"redis";
	
	public static Jedis createRedis()
	{
		String redis_ip="127.0.0.1";
		int redis_port=6379;
		int redis_select=1;
		
		 File file=new File(REDIS_CONF);
	 	 try
	 	{
	 		BufferedReader reader=new BufferedReader(new FileReader(file));
	 		String line;
	 			
	 		if(file.exists())
	 		while((line=reader.readLine())!=null)
	 		{
	 			if(line.startsWith("IP"))
	 				redis_ip=line.substring("IP=".length()).trim();
	 			else if(line.startsWith("Port"))
	 				redis_port=Integer.parseInt(line.substring("Port=".length()).trim());
	 			else if(line.startsWith("Select"))
	 				redis_select=Integer.parseInt(line.substring("Select=".length()).trim());
	 		}
	 		else
	 		{
	 			redis_ip="127.0.0.1";
	 			redis_port=6379;
	 			redis_select=1;
	 		}
	 		reader.close();
	 	}
	 	catch(IOException e)
	 	{}
	 		
	 	 Jedis redis=new Jedis(redis_ip,redis_port);
	 	redis.select(redis_select);
	 	
	 	return redis;
	}

	public static MongoDatabase createMongo()
	{
		return new MongoClient().getDatabase("openfire");
	}
}
