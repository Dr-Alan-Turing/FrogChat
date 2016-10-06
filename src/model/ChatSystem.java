package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatSystem {

	private Map<String,User> users;
	
	public ChatSystem(){
		this.users = new HashMap<>();
	}
	
	public void addUser(User user){
		this.users.put(user.getEmail(),user);
	}
	
	public User getUser(String email){
		return users.get(email);
	}
	
	public List<User> getUsers(){
		return new ArrayList<>(this.users.values());
	}
	
	
	
	public User getAuthenticatedUser(String email, String password){
		User user = this.users.get(email);
		if(user==null || !user.getPassword().equals(password)){
			user = null;
		}
		return user;
	}

	public List<User> getFriends(User user) {
		return user.getFriends();
	}
	
}
