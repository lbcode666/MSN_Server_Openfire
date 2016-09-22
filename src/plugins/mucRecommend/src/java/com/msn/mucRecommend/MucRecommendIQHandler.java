package com.msn.mucRecommend;

import java.sql.*;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;

public class MucRecommendIQHandler extends IQHandler {
	
	private IQHandlerInfo info;
	private static final String MODULE_NAME="Muc Recommend IQ Handler";
	private static final String ELEMENT_NAME="query";
	private static final String NAMESPACE="com.msn.mucRecommend";

	public MucRecommendIQHandler() {
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		// TODO Auto-generated constructor stub
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO Auto-generated method stub
		
		IQ reply=IQ.createResultIQ(packet);
		
		if(IQ.Type.get.equals(packet.getType()))
		{
			System.out.println("MucRecommendPlugin::拦截一个社团推荐请求包。");
			dataRequire(packet);
		}
		else if(IQ.Type.set.equals(packet.getType()))
		{
			System.out.println("MucRecommendPlugin::拦截一个社团标签设置包。");
			
			Element childElement = packet.getChildElement();
			
			Element item = childElement.element("item");
			String mucname = item.attributeValue("mucname");
			String myLabel1 = item.attributeValue("label1");
			String myLabel2 = item.attributeValue("label2");
			String myLabel3 = item.attributeValue("label3");
			
			int check = dataCheck(mucname);
			
			if(check==0)
			{
				dataStore(mucname,myLabel1,myLabel2,myLabel3);	
			}
			else
			{
				dataUpdate(mucname,myLabel1,myLabel2,myLabel3);
			}
		}
		return null;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}
	
	public int dataCheck(String mucname){
    	
    	int check = 0;
    	
    	Connection con = null ;
    	Statement statement = null ;
     	ResultSet resultSet = null;
     	
    	String select = "select count(*) as RowCount from ofMucLabel where username='"+mucname+"'";  	
    	 
    	try {
	            con = DbConnectionManager.getConnection();
	            statement = con.createStatement();
	            resultSet = statement.executeQuery(select);
	            resultSet.next();
	            check = resultSet.getInt("RowCount");	         
	            System.out.println("check Success！");
	        } catch (SQLException sql) {
	            System.out.println("check Failed！");
	        } finally {
	            DbConnectionManager.closeConnection(statement, con);
	        }
    	 	
    	return check;
    } 

}
