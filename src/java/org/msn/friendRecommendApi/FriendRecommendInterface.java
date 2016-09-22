package org.msn.friendRecommendApi;

import java.util.List;

public interface FriendRecommendInterface {
	public void setUsername(String name);
	public List<String> recommendedFriends();
}
