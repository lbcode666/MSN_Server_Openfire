package com.msn.friendRecommend;

import java.util.HashSet;
import java.util.Set;

public class FriendRecommendAlgorithm {
	private Set<String> firstLabelSet;
	private Set<String> secondLabelSet;
	
	private Set<String> firstFriendSet;
	private Set<String> secondFriendSet;
	
	private double labelSimilarity;
	private double friendSimilarity;
	
	public FriendRecommendAlgorithm
	(Set<String> firstLabelSet,Set<String> secondLabelSet,Set<String> firstFriendSet,Set<String> secondFriendSet )
	{
		this.firstLabelSet=firstLabelSet;
		this.secondLabelSet=secondLabelSet;
		
		this.firstFriendSet=firstFriendSet;
		this.secondFriendSet=secondFriendSet;
		
		labelSimilarity=setSimilarity(this.firstLabelSet,this.secondLabelSet);
		friendSimilarity=setSimilarity(this.firstFriendSet,this.secondFriendSet);
	}

	public boolean isRecommended() {
		// TODO Auto-generated method stub
		
		double similarity=0.4*labelSimilarity+0.6*friendSimilarity;
		
		if(similarity>=0.3)
			return true;
		else
			return false;
	}
	
	private <T> double setSimilarity(Set<T> aCollection,Set<T> bCollection)
	{
		if(aCollection.size()==0||bCollection.size()==0||aCollection==null||bCollection==null)
			return 0;
		
		return (double)this.intersection(aCollection,bCollection).size()/(double)this.union(aCollection, bCollection).size();
	}
	
	private <T> Set<T> intersection(Set<T> a,Set<T> b)
	{
		Set<T> inter =new HashSet<T>();
		inter.addAll(a);
		inter.retainAll(b);
		
		return inter;
	}
	
	private <T> Set<T> union(Set<T> a,Set<T> b)
	{
		Set<T> un=new HashSet<T>();
		un.addAll(a);
		un.addAll(b);
		
		return un;
	}
	
	public Set<String> getFirstLabelSet() {
		return firstLabelSet;
	}
	public void setFirstLabelSet(Set<String> firstLabelSet) {
		this.firstLabelSet = firstLabelSet;
	}
	public Set<String> getSecondLabelSet() {
		return secondLabelSet;
	}
	public void setSecondLabelSet(Set<String> secondLabelSet) {
		this.secondLabelSet = secondLabelSet;
	}
	public Set<String> getFirstFriendSet() {
		return firstFriendSet;
	}
	public void setFirstFriendSet(Set<String> firstFriendSet) {
		this.firstFriendSet = firstFriendSet;
	}
	public Set<String> getSecondFriendSet() {
		return secondFriendSet;
	}
	public void setSecondFriendSet(Set<String> secondFriendSet) {
		this.secondFriendSet = secondFriendSet;
	}
}
