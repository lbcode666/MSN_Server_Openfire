package com.msn.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.spi.PacketRouterImpl;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.msn.friendRecommendApi.UserInfo;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class ActivityIQHandler extends IQHandler {

	private IQHandlerInfo info;
	private static final String MODULE_NAME="Activity IQ Handler";
	private static final String ELEMENT_NAME="activity";
	private static final String NAMESPACE="com.msn.activity";
	
	MongoDatabase mongo;
	
	public ActivityIQHandler() {
		super(MODULE_NAME);
		info=new IQHandlerInfo(ELEMENT_NAME,NAMESPACE);
		mongo=new MongoClient().getDatabase("openfire");
		System.out.println("注册了Activity IQ Handler:"+ELEMENT_NAME+" "+NAMESPACE);
	
		// TODO Auto-generated constructor stub
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		System.out.println("拦截到Activity包");
		// TODO Auto-generated method stub
		IQ reply=IQ.createResultIQ(packet);
		
		JID from=packet.getFrom();
		String username=from.getNode();
		
		Element activityElement=packet.getChildElement();
		String type=activityElement.attributeValue("type");
		Element childElement=reply.setChildElement(ELEMENT_NAME, NAMESPACE);
		
		if(type.equals("createActivity"))
		{
			System.out.println("ActivityPlugin::"+username+"请求创建活动。");
			childElement=createActivity(activityElement, childElement,username);
		}
		else if(type.equals("askRecommendation"))
		{
			System.out.println("ActivityPlugin::"+username+"请求推荐活动。");
			try {
				childElement=recommendActivity(activityElement,childElement,username);
			} catch (UserNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(type.equals("post"))
		{
			System.out.println("ActivityPlugin::"+username+"发了一个帖子。");
			childElement=post(activityElement,childElement,username);
		}
		else if(type.equals("askComments"))
		{
			System.out.println("ActivityPlugin::"+username+"请求活动发帖。");
			childElement=returnPosts(activityElement,childElement);
		}
		else if(type.equals("join"))
		{
			System.out.println("ActivityPlugin::"+username+"请求加入活动。");
			childElement=joinActivity(activityElement,childElement,username);
		}
		else if(type.equals("cancel"))
		{
			System.out.println("ActivityPlugin::"+username+"请求退出活动。");
			childElement=cancel(activityElement,childElement,username);
		}
		else if(type.equals("myCreation"))
		{
			System.out.println("ActivityPlugin::"+username+"请求查看其创建的活动。");
			childElement=myCreation(activityElement,childElement,username);
		}
		else if(type.equals("myParticipation"))
		{
			System.out.println("ActivityPlugin::"+username+"请求查看其参与的活动。");
			childElement=myParticipation(activityElement,childElement,username);
		}
		else if(type.equals("delete"))
		{
			System.out.println("ActivityPlugin::"+username+"请求删除活动。");
			childElement=delete(activityElement,childElement,username);
		}
		else if(type.equals("joinedPeople"))
		{
			System.out.println("ActivityPlugin::"+username+"请求查看活动参与者。");
			childElement=joinedPeople(activityElement,childElement);
		}
		
		reply.setChildElement(childElement);
		
//		XMPPServer.getInstance().getRoutingTable().routePacket(jid, packet, fromServer);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		// TODO Auto-generated method stub
		return info;
	}
	
	
	/*
	 * the method of 'createActivity' is to create a new activity according to relevant
	 * information from the client and store it in the MongoDB.
	 * at last return the child element of Reply IQ packet.
	 */
	private Element createActivity(Element activityElement,Element childElement,String creator)
	{
		String title=activityElement.elementText("title");
		String content=activityElement.elementText("content");
		String time=activityElement.elementText("time");
		Date now=new Date();
		String imageString=activityElement.elementText("imageString");
		double longitude=Double.parseDouble(activityElement.element("location").attributeValue("longitude"));
		double latitude=Double.parseDouble(activityElement.element("location").attributeValue("latitude"));
		String address=activityElement.elementText("address");

		Set<String> labelSet=new HashSet<String>();
		Element labels=activityElement.element("labels");
		Iterator ite=labels.elements().iterator();
		while(ite.hasNext())
		{
			String label=((Element)ite.next()).getText();
			labelSet.add(label);
		}
		
		int limit=Integer.parseInt(activityElement.elementText("limit"));

		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		
		int activity_id;
		boolean activity_id_exist=false;
		Document max_id_doc=activityCollection.find(Filters.gte("max_id",0)).first();
		if(max_id_doc==null)
		{
			activity_id=1;
			activity_id_exist=false;
		}
		else
		{
			int max_id=max_id_doc.getInteger("max_id");
			activity_id=max_id+1;
			activity_id_exist=true;
		}
		
		Document document=new 
				Document("activity_id",activity_id)
				.append("creator", creator)
				.append("title", title)
				.append("content", content)
				.append("time", time)
				.append("creation_time", now)
				.append("imageString", imageString)
				.append("longitude", longitude)
				.append("latitude", latitude)
				.append("address", address)
				.append("labels", labelSet)
				.append("limit", limit)
				.append("members",new HashSet<String>())
				.append("posts", new ArrayList<Document>());

		activityCollection.insertOne(document);
		
		if(activity_id_exist)
			activityCollection.updateOne(
					Filters.gte("max_id",0),new Document("$set",new Document("max_id",activity_id)));
		else
			activityCollection.insertOne(new Document("max_id",activity_id));
		
		childElement.addAttribute("type", "createActivity");
		Element resultElement=childElement.addElement("result");
		resultElement.setText("1");
		Element idElement=childElement.addElement("activity_id");
		idElement.setText(Integer.toString(activity_id));
		return childElement;
	}
	
	/*
	 * the method of 'recommendActivity' is to recommend some activities to a certain user according to a certain algorithm.
	 * at last return at last return the child element of Reply IQ packet.
	 */
	@SuppressWarnings("unchecked")
	private Element recommendActivity(Element activityElement,Element childElement,String username) throws UserNotFoundException
	{
		double longitude=Double.parseDouble(activityElement.element("location").attributeValue("longitude"));
		double latitude=Double.parseDouble(activityElement.element("location").attributeValue("latitude"));
		int requestDistance=Integer.parseInt(activityElement.elementText("distance"));
		
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		MongoCursor<Document> cursor=activityCollection.find(Filters.gt("activity_id", 0)).iterator();
		UserInfo user=new UserInfo(username);
		Set<String> userLabels=user.getLabels();
		while(cursor.hasNext())
		{
			Document activity=cursor.next();
			double activityLongitude=activity.getDouble("longitude");
			double activityLatitude=activity.getDouble("latitude");
			List<String> activityLabels=(List<String>)activity.get("labels");
			List<String> members=(List<String>)activity.get("members");
			String creator=activity.getString("creator");
						
//			double distance=Math.sqrt(Math.pow(longitude-activityLongitude, 2)+Math.pow(latitude-activityLatitude,2));
			Set<String> res=new HashSet<String>();
			res.addAll(activityLabels);
			res.retainAll(userLabels);
			
			double distance=0.5;
			if(!creator.equals(username)&&distance<=requestDistance&&!res.isEmpty())
			{
				Element item=childElement.addElement("item");
				int activity_id=activity.getInteger("activity_id");
				String title=activity.getString("title");
				String content=activity.getString("content");
				String time=activity.getString("time");
				String imageString=activity.getString("imageString");
				String address=activity.getString("address");
				int limit=activity.getInteger("limit");
				int number=((List<String>)activity.get("members")).size();
				
				item.addElement("activity_id").setText(Integer.toString(activity_id));
				item.addElement("creator").setText(creator);
				item.addElement("title").setText(title);
				item.addElement("content").setText(content);
				item.addElement("imageString").setText(imageString);
				item.addElement("time").setText(time);
				item.addElement("address").setText(address);
				item.addElement("distance").setText(Double.toString(distance));
				
				Element labels=item.addElement("labels");
				for(String label:activityLabels)
					labels.addElement("label").setText(label);
				
				item.addElement("number").setText(Integer.toString(number));
				item.addElement("limit").setText(Integer.toString(limit));
				
				if(members.contains(username))
					item.addElement("isJoined").setText("1");
				else
					item.addElement("isJoined").setText("0");
			}		
		}
		childElement.addAttribute("type", "askRecommendation");
		return childElement;
	}

	/*
	 * the method of 'post' is for a user to post a comment to a certain activity.
	 * at last return at last return the child element of Reply IQ packet.
	 */
	private  Element post(Element activityElement,Element childElement,String username)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		String comment=activityElement.elementText("comment");
		Date now=new Date();
		
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		Document aPost=new Document("username",username)
				.append("comment", comment)
				.append("time", now);
		
		activityCollection.updateOne(Filters.eq("activity_id", activity_id), new Document("$push",new Document("posts",aPost)));
		
		childElement.addAttribute("type", "post");
		Element resultElement=childElement.addElement("result");
		resultElement.setText("1");
		
		return childElement;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private Element returnPosts(Element activityElement, Element childElement)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		List<Document> posts=(List<Document>)activityCollection.find(Filters.eq("activity_id", activity_id)).first().get("posts");
		
		Element activity_idElement=childElement.addElement("activity_id");
		activity_idElement.setText(Integer.toString(activity_id));
		
		for(Document aPost:posts)
		{
			String creator=aPost.getString("username");
			String content=aPost.getString("comment");
			Date date=aPost.getDate("time");
			String time=new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
			
			Element item=childElement.addElement("item");
			Element creatorElement=item.addElement("creator");
			Element timeElement=item.addElement("time");
			Element contentElement=item.addElement("content");
			
			creatorElement.setText(creator);
			timeElement.setText(time);
			contentElement.setText(content);
		}
		
		childElement.addAttribute("type", "askComments");
		return childElement;
	}
	
	/*
	 * the method of 'joinActivity' is for a user to join a certain activity.
	 * at last return at last return the child element of Reply IQ packet.
	 */
	@SuppressWarnings("unchecked")
	private Element joinActivity(Element activityElement, Element childElement,String username)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		Document theActivity=activityCollection.find(Filters.eq("activity_id", activity_id)).first();
		List<String> members=(List<String>)theActivity.get("members");
		int limit=theActivity.getInteger("limit"	, 50);
		
		childElement.addAttribute("type", "join");
		Element resultElement=childElement.addElement("result");
		
		if(members.size()>=limit)
		{
			resultElement.setText("0");
		}
		else
		{
			activityCollection.updateOne(Filters.eq("activity_id", activity_id), new Document("$push",new Document("members",username)));
			resultElement.setText("1");
		}
		
		return childElement;
	}
	
	/*
	 * the method of 'cancel' is for a user to cancel the participation to a certain activity or quit an activity
	 * at last return at last return the child element of Reply IQ packet.
	 */
	@SuppressWarnings("unchecked")
	private Element cancel(Element activityElement,Element childElement,String username)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		Document theActivity=activityCollection.find(Filters.eq("activity_id", activity_id)).first();
		List<String> members=(List<String>)theActivity.get("members");
		
		childElement.addAttribute("type", "cancel");
		Element resultElement=childElement.addElement("result");
		
		if(members.contains(username))
		{
			activityCollection.updateOne(Filters.eq("activity_id", activity_id), new Document("$pull",new Document("members",username)));
			resultElement.setText("1");
		}
		else
		{
			resultElement.setText("0");
		}
		
		return childElement;
	}
	
	@SuppressWarnings("unchecked")
	private Element myCreation(Element activityElement,Element childElement,String username)
	{
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		MongoCursor<Document> cursor=activityCollection.find(Filters.eq("creator", username)).iterator();
		
		while(cursor.hasNext())
		{
			Document activity=cursor.next();
			int activity_id=activity.getInteger("activity_id");
			String title=activity.getString("title");
			String content=activity.getString("content");
			String imageString=activity.getString("imageString");
			String time=activity.getString("time");
			String address=activity.getString("address");
			int limit=activity.getInteger("limit");
			List<String> activityLabels=(List<String>)activity.get("labels");
			int number=((List<String>)activity.get("members")).size();
			
			Element item=childElement.addElement("item");
			item.addElement("activity_id").setText(Integer.toString(activity_id));
			item.addElement("creator").setText(username);
			item.addElement("title").setText(title);
			item.addElement("content").setText(content);
			item.addElement("imageString").setText(imageString);
			item.addElement("time").setText(time);
			item.addElement("address").setText(address);
			
			item.addElement("distance").setText("0");
			
			
			Element labels=item.addElement("labels");
			for(String label:activityLabels)
				labels.addElement("label").setText(label);
			
			item.addElement("limit").setText(Integer.toString(limit));
			
			item.addElement("number").setText(Integer.toString(number));
			item.addElement("isJoined").setText("1");
		}
		
		childElement.addAttribute("type", "myCreation");
		return childElement;
	}
	
	@SuppressWarnings("unchecked")
	private Element myParticipation(Element activityElement,Element childElement,String username)
	{
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		MongoCursor<Document> cursor=activityCollection.find(Filters.exists("activity_id")).iterator();
		
		while(cursor.hasNext())
		{
			Document activity=cursor.next();
			
			List<String> members=(List<String>)activity.get("members");
			System.out.println(members.toString());
			
			if(members.contains(username))
			{
				int activity_id=activity.getInteger("activity_id");
				String title=activity.getString("title");
				String creator=activity.getString("creator");
				String content=activity.getString("content");
				String imageString=activity.getString("imageString");
				String time=activity.getString("time");
				String address=activity.getString("address");
				int limit=activity.getInteger("limit");
				List<String> activityLabels=(List<String>)activity.get("labels");
				int number=((List<String>)activity.get("members")).size();
				
				Element item=childElement.addElement("item");
				item.addElement("activity_id").setText(Integer.toString(activity_id));
				item.addElement("creator").setText(creator);
				item.addElement("title").setText(title);
				item.addElement("content").setText(content);
				item.addElement("imageString").setText(imageString);
				item.addElement("time").setText(time);
				item.addElement("address").setText(address);
				
				Element labels=item.addElement("labels");
				for(String label:activityLabels)
					labels.addElement("label").setText(label);
				
				item.addElement("number").setText(Integer.toString(number));
				item.addElement("limit").setText(Integer.toString(limit));
				item.addElement("isJoined").setText("1");
			}
		}
		childElement.addAttribute("type", "myParticipation");
		return childElement;
	}

	private Element delete(Element activityElement,Element childElement,String username)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		
		Document theActivity=activityCollection.find(Filters.eq("activity_id", activity_id)).first();
		String creator=theActivity.getString("creator");
		
		Element resultElement=childElement.addElement("result");
		
		if(username.equals(creator))
		{
			activityCollection.deleteOne(Filters.eq("activity_id", activity_id));
			resultElement.setText("1");
		}
		else
			resultElement.setText("0");
		
		childElement.addAttribute("type", "delete");
		return childElement;
	}
	
	@SuppressWarnings("unchecked")
	private Element joinedPeople(Element activityElement,Element childElement)
	{
		int activity_id=Integer.parseInt(activityElement.elementText("activity_id"));
		MongoCollection<Document> activityCollection=mongo.getCollection("activity");
		
		Document theActivity=activityCollection.find(Filters.eq("activity_id", activity_id)).first();
		List<String> members=(List<String>)theActivity.get("members");
		
		for(String member:members)
		{
			Element participatorElement=childElement.addElement("participator");
			participatorElement.addAttribute("name", member);
		}
		
		childElement.addAttribute("type", "joinedPeople");
		return childElement;
		
	}
}
