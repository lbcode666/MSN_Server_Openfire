package com.msn.handleLabelPacket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class LabelIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Handle Label Packet";
	private static final String ELEMENT_NAME="setlabel";
	private static final String NAMESPACE="com.msn.handleLabelPacket";
	
	private Jedis redis;
	
    private static final String REDIS_CONF=".."+File.separator+"conf"+File.separator+"redis";
    private String redis_ip;
	private int  redis_port;
	private int redis_select;

	public LabelIQHandler() {

		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
        
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
		
		redis=new Jedis(redis_ip,redis_port);
		redis.select(redis_select);
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO Auto-generated method stub
		
		//获取用户名
		JID from = packet.getFrom();
		String username = from.getNode();
		
		Element childElement = packet.getChildElement();
		
		if(IQ.Type.set.equals(packet.getType()))
		{
			System.out.println("HandleLabelPacketPlugin::拦截一个兴趣标签设置包");

			Element item = childElement.element("item");
			String myLabel1 = item.attributeValue("label1");
			String myLabel2 = item.attributeValue("label2");
			String myLabel3 = item.attributeValue("label3");
			String myLabel4 = item.attributeValue("label4");
			String myLabel5 = item.attributeValue("label5");
			
			int check = dataCheck(username);
			
			if(check==0)
			{
				dataStore(username,myLabel1,myLabel2,myLabel3,myLabel4,myLabel5);	
			}
			else
			{
				dataUpdate(username,myLabel1,myLabel2,myLabel3,myLabel4,myLabel5);
			}
		}
		
		return null;
	}

    public int dataCheck(String username){
    	int check = 0;
    	
    	Connection con = null ;
    	Statement statement = null ;
     	ResultSet resultSet = null;
     	
    	String select = "select count(*) as RowCount from ofLabel where username='"+username+"'";  	
    	 
    	try {
	            con = DbConnectionManager.getConnection();
	            statement = con.createStatement();
	            resultSet = statement.executeQuery(select);
	            resultSet.next();
	            check = resultSet.getInt("RowCount");	         
	            System.out.println("HandleLabelPacketPlugin::检测成功！");
	        } catch (SQLException sql) {
	            System.out.println("HandleLabelPacketPlugin::检测失败！");
	        } finally {
	            DbConnectionManager.closeConnection(statement, con);
	        }
    	 	
    	return check;
    } 
    
public void dataStore(String username,String myLabel1,String myLabel2,String myLabel3,String myLabel4,String myLabel5){
    	Connection con = null ;
     	PreparedStatement pstmt = null ;
    	String insert = "INSERT INTO ofLabel(username, label1, label2, label3, label4, label5) VALUES(?,?,?,?,?,?)";
    	
    	 try {
	            con = DbConnectionManager.getConnection();
	            pstmt = con.prepareStatement(insert);
	            pstmt.setString(1, username);
	            pstmt.setString(2, myLabel1); 
	            pstmt.setString(3, myLabel2);
	            pstmt.setString(4, myLabel3);
	            pstmt.setString(5, myLabel4);
	            pstmt.setString(6, myLabel5);
	            pstmt.execute();
	            
	            redis.sadd("label:"+username, myLabel1,myLabel2,myLabel3,myLabel4,myLabel5);
	            
	            System.out.println("HandleLabelPacketPlugin::成功将"+username+"的兴趣标签保存至MySQL并缓存到Redis.");
	        } 
    	 catch (SQLException sql) {
	            System.out.println("HandleLabelPacketPlugin::保存兴趣标签到MySQL数据库失败！");
	        } 
    	 catch(JedisConnectionException e)
    	 {
    		 System.err.println("HandleLabelPacketPlugin::无法连接Redis服务器！");
    	 }
    	 finally {
	            DbConnectionManager.closeConnection(pstmt, con);
	      }  	
    	
    }
    
    public void dataUpdate(String username,String myLabel1,String myLabel2,String myLabel3,String myLabel4,String myLabel5){
    	Connection con = null ;
     	PreparedStatement pstmt = null ;
     	String update="update ofLabel set label1=?,label2=?,label3=?,label4=?,label5=? where username=?";
    	
    	 try {
	            con = DbConnectionManager.getConnection();
	            pstmt = con.prepareStatement(update);
	            pstmt.setString(1, myLabel1);
	            pstmt.setString(2, myLabel2);
	            pstmt.setString(3, myLabel3);
	            pstmt.setString(4, myLabel4);
	            pstmt.setString(5, myLabel5);
	            pstmt.setString(6, username);
	            pstmt.execute();
	            
	            redis.del("label:"+username);
	            redis.sadd("label:"+username, myLabel1,myLabel2,myLabel3,myLabel4,myLabel5);
	            
	            System.out.println("HandleLabelPacketPlugin::成功将"+username+"的兴趣标签更新至MySQL并缓存到Redis.");
	        } catch (SQLException sql) {
	        	 System.out.println("HandleLabelPacketPlugin::更新兴趣标签到MySQL数据库失败！");
	        } 
    	 catch(JedisConnectionException e)
    	 {
    		 System.err.println("HandleLabelPacketPlugin::无法连接Redis服务器！");
    	 }
    	 finally {
	            DbConnectionManager.closeConnection(pstmt, con);
	        }  	
    	
    }    

} 

