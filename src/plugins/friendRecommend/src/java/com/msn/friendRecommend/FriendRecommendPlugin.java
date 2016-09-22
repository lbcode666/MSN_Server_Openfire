package com.msn.friendRecommend;

import org.msn.friendRecommendApi.AbstractFriendRecommendPlugin;

public class FriendRecommendPlugin extends AbstractFriendRecommendPlugin{
	
	public FriendRecommendPlugin() {
		super(new FriendRecommendImpl());
		// TODO Auto-generated constructor stub
	}
	
}
