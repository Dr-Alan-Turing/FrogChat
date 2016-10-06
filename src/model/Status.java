package model;

public enum Status {
	ONLINE, AWAY, OFFLINE;
	
	public String getStatus(){
		return this.name();
	}
}
