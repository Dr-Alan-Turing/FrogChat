package model;

import java.util.ArrayList;
import java.util.List;

public class User implements Comparable<User> {
	
	private String name, firstName, nickname, email, password;
	private Status status;
	private List<User> friends;
	
	public User(){
		this.friends = new ArrayList<>();
		this.setStatus(Status.OFFLINE);
	}
	
	public User(String name, String firstName, String nickname, String email, String password){
		this();
		this.setName(name);
		this.setFirstName(firstName);
		this.setNickname(nickname);
		this.setEmail(email);
		this.setPassword(password, password);
	}
	
	public String getName() {
		return this.name;
	}
	public String getFirstName() {
		return this.firstName;
	}
	public String getNickname() {
		return this.nickname;
	}
	public String getEmail() {
		return this.email;
	}
	public String getPassword() {
		return this.password;
	}
	public Status getStatus(){
		return this.status;
	}
	public List<User> getFriends(){
		return this.friends;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public void setPassword(String password, String repeatPassword) {
		this.password = password;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public void addFriend(User user){
		if(user==null){
			throw new IllegalArgumentException("Invalid user cannot be added as friend");
		}
		if(this.getFriends().contains(user)){
			throw new IllegalArgumentException("User has already been added as friend");
		}
		this.friends.add(user);
		user.addedBy(this);
	}
	
	public void addedBy(User user){
		if(user==null){
			throw new IllegalArgumentException("Invalid user cannot be added as friend");
		}
		if(this.getFriends().contains(user)){
			throw new IllegalArgumentException("User has already been added as friend");
		}
		this.friends.add(user);
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof User && ((User)o).getEmail().equals(this.getEmail());
	}
	@Override
	public int hashCode(){
		return this.getEmail().hashCode();
	}

	@Override
	public int compareTo(User user) {
		if((this.getStatus()==Status.ONLINE && user.getStatus()!=Status.ONLINE)
				|| (this.getStatus()==Status.AWAY && user.getStatus()==Status.OFFLINE)){
			return -1;
		}
		if((user.getStatus()==Status.ONLINE && this.getStatus()!=Status.ONLINE)
				|| (user.getStatus()==Status.AWAY && this.getStatus()==Status.OFFLINE)){
			return +1;
		}
		return this.getNickname().compareTo(user.getNickname());
	}
	
}
