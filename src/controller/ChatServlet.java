package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import model.ChatSystem;
import model.Status;
import model.User;

/*
 * READ FIRST:
 * This application makes heavy use of sessions.
 * It is therefore recommended to test chatting from one user to the other using
 * two different browsers (or at least one browser in incognito mode - but this
 * is not 100% percent reliable).
 * IMPORTANT: This app does NOT work in Safari! Use Chrome and Firefox. Safari doesn't
 * accept the handshake used by the websockets. I couldn't fix this and saw that in the examples
 * that we were given, the websockets also didn't work (in Safari).
 * NOTE: If you accidentally log in as the same user on two different browsers, the server can get
 * confused by the sessions and things may not work as expected. If this happens, simply log out both users
 * and try again (this time using two different users!).
 */

@WebServlet("/ChatServlet")
@ServerEndpoint("/chat")
public class ChatServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	//chat service
	private ChatSystem system;
	//user automatically added as friend for every newly registered user (like Skype's echo)
	private User echo;
	//all users that log on are added to SESSIONS, this maps their email address to their session
	private final static Map<Session,String> SESSIONS = Collections.synchronizedMap(new HashMap<Session,String>());
	//users users that log on are added to BROADCASTS, this maps a list of broadcast messages (destined to them) to their email address
	private final static Map<String,List<String>> BROADCASTS = Collections.synchronizedMap(new HashMap<String,List<String>>());
	
	/*
	 * Constructor initialises some starting users (for test purposes). These users are:
	 * tom@tom.com (password: t)
	 * kaat@kaat.com (password: t)
	 * hannes@hannes.com (password: t)
	 */
    public ChatServlet() {
        super();
        this.system = new ChatSystem();
        //create echo
        echo = new User("echo","echo","Echo","echo@echo.com","password");
        echo.setStatus(Status.ONLINE);
        //create dummy users
        User tom = new User("Neckermann","Tom","ironlion","tom@tom.com","t");
        User hannes = new User("Neckermann","Hannes","hannekukkel","hannes@hannes.com","t");
        User kaat = new User("Neckermann","Kaat","kaatrocks","kaat@kaat.com","t");
        //add dummy users to system
        system.addUser(tom);
        system.addUser(hannes);
        system.addUser(kaat);
        //add friends to dummy users
        tom.addFriend(echo);
        hannes.addFriend(echo);
        kaat.addFriend(echo);
        tom.addFriend(hannes);
        tom.addFriend(kaat);
    }
    
    /*
	 * 
	 * 
	 * GENERAL NAVIGATION AND PROCESSING
	 * 
	 * 
	 */
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.processRequest(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.processRequest(request, response);
	}
	
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String destination = "";
		String action = request.getParameter("action");
		//angular-js call at log-in page
		if(action!=null && action.equals("showAllUsersOverview")){
			showAllUsersOverview(request, response);
		}
		//main actions
		else if(action==null || action.equals("showLogIn") || (this.isAuthenticationNeeded(action)==true && this.isUserAuthenticated(request, response)==false)){
			response.sendRedirect(this.showLogIn(request, response));
		}
		else if(action.equals("processLogIn")){
			destination = this.processLogIn(request, response);
			if(destination.equals("overview.jsp")){
				response.sendRedirect("ChatServlet?action=showOverview");
				destination = ""; //so view is not forwarded again
			}
		}
		else if(action.equals("showOverview")){
			destination = this.showOverview(request, response);
		}
		else if(action.equals("processLogOut")){
			response.sendRedirect(this.processLogOut(request, response));
		}
		else if(action.equals("showRegistration")){
			response.sendRedirect(this.showRegistration(request, response));
		}
		else if(action.equals("processRegistration")){
			destination = this.processRegistration(request, response);
		}
		//page actions
		else if(action.equals("getFriends")){
			getFriends(request, response);
		}
		else if(action.equals("getBroadcasts")){
			getBroadcasts(request, response);
		}
		else if(action.equals("changeStatus")){
			changeStatus(request,response);
		}
		else if(action.equals("searchUser")){
			searchUser(request, response);
		}
		else if(action.equals("addUser")){
			addUser(request, response);
		}
		else if(action.equals("sendBroadcast")){
			sendBroadcast(request, response);
		}
		if(!destination.equals("")){
			RequestDispatcher view = request.getRequestDispatcher(destination);
			view.forward(request, response);
		}
	}

	private boolean isAuthenticationNeeded(String action) {
		boolean authenticationNeeded = true;
		if(action==null || action.equals("showLogIn") || action.equals("processLogIn") || action.equals("showRegistration")||action.equals("processRegistration")){
			authenticationNeeded = false;
		}
		return authenticationNeeded;
	}

	private boolean isUserAuthenticated(HttpServletRequest request, HttpServletResponse response){
		return request.getSession().getAttribute("authenticatedUser")!=null;
	}
	
	private String showLogIn(HttpServletRequest request, HttpServletResponse response) {
		return "login.html";
	}

	private String processLogIn(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean success = false;
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		User user = system.getAuthenticatedUser(email, password);
		if(user!=null){
			success = true;
			request.getSession().setAttribute("authenticatedUser", user);
			user.setStatus(Status.ONLINE);
		}
		return success?"overview.jsp":showLogIn(request, response);
	}

	private String processLogOut(HttpServletRequest request, HttpServletResponse response) {
		((User)(request.getSession().getAttribute("authenticatedUser"))).setStatus(Status.OFFLINE);
		request.getSession().invalidate();
		return showLogIn(request, response);
	}
	
	private String showOverview(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setAttribute("email", ((User)(request.getSession().getAttribute("authenticatedUser"))).getEmail());
		return "overview.jsp";
	}
	
	private String showRegistration(HttpServletRequest request, HttpServletResponse response) {
		return "register.html";
	}
	
	private String processRegistration(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean success = true;
		//get data from view
		String name = request.getParameter("name");
		String firstName = request.getParameter("firstName");
		String nickname = request.getParameter("nickname");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		String repeatPassword = request.getParameter("repeatPassword");
		//send data to model
		try {
			User user = new User();
			user.setName(name);
			user.setFirstName(firstName);
			user.setNickname(nickname);
			user.setEmail(email);
			user.setPassword(password, repeatPassword);
			system.addUser(user);
			user.addFriend(echo);
		} catch (Exception e){
			success = false;
		}
		return success?showLogIn(request, response):showRegistration(request, response);
	}
	
	/*
	 * 
	 * 
	 * METHODS FOR ASYNCHRONOUS REQUESTS
	 * 
	 * 
	 */
	
	//Retrieve total amount of users online, offline and away
	private void showAllUsersOverview(HttpServletRequest request, HttpServletResponse response) throws IOException{
		int online = 0, away = 0, offline = 0;
		for(User user : system.getUsers()){
			if(user.getStatus().equals(Status.ONLINE)){
				online++;
			}
			else if(user.getStatus().equals(Status.AWAY)){
				away++;
			}
			else {
				offline++;
			}
		}
		String json = "{\n"
						+ "\"online\": "+online+",\n"
						+ "\"away\": "+away+",\n"
						+ "\"offline\": "+offline+"\n"
					+ "}";
		response.getWriter().write(json);
	}
	
	//Change status of user with given email
	private void changeStatus(HttpServletRequest request, HttpServletResponse response) {
		String email = request.getParameter("email");
		String status = request.getParameter("status");
		system.getUser(email).setStatus(Status.valueOf(status));
	}
	
	//Search user using given nickname, first name and/or last name.
	//IMPORTANT: This will return all users that match ANY of the strings in ANY of the given fields
	private void searchUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<User> foundUsers = new ArrayList<>();
		User requestingUser = (User)(request.getSession().getAttribute("authenticatedUser"));
		String nickname = request.getParameter("nickname");
		String firstName = request.getParameter("firstName");
		String name = request.getParameter("name");
		for(User user : system.getUsers()){
			if(!requestingUser.equals(user)
				&&
				(!nickname.isEmpty() && user.getNickname().toLowerCase().contains(nickname.toLowerCase()) 
					|| !firstName.isEmpty() && user.getFirstName().toLowerCase().contains(firstName.toLowerCase()) 
					|| !name.isEmpty() && user.getName().toLowerCase().contains(name.toLowerCase())))
				{
				foundUsers.add(user);
			}
		}
		String foundUsersXML = foundUsersToXml(foundUsers, requestingUser);
		response.setContentType("text/xml");
		response.getWriter().write(foundUsersXML);
	}
	
	//Return xml string of given found users
	private String foundUsersToXml(List<User> foundUsers, User requestingUser) {
		StringBuffer xml = new StringBuffer();
		xml.append("<users>\n");
		for(User user : foundUsers){
			xml.append("<user>\n");
			xml.append("<nickname>");
			xml.append(user.getNickname());
			xml.append("</nickname>\n");
			xml.append("<email>");
			xml.append(user.getEmail());
			xml.append("</email>\n");
			xml.append("<isFriend>");
			xml.append(requestingUser.getFriends().contains(user));
			xml.append("</isFriend>");
			xml.append("</user>\n");
		}
		xml.append("</users>");
		return xml.toString();
	}
	
	//Add user using given email
	private void addUser(HttpServletRequest request, HttpServletResponse response) {
		String toBeAddedUserEmail = request.getParameter("email");
		User requestingUser = (User)(request.getSession().getAttribute("authenticatedUser"));
		User toBeAddedUser = null;
		for(User user : system.getUsers()){
			if(user.getEmail().equalsIgnoreCase(toBeAddedUserEmail)){
				toBeAddedUser = user;
				break;
			}
		}
		if(toBeAddedUser!=null){
			requestingUser.addFriend(toBeAddedUser);
		}
	}
	
	//Send given broadcast message to sender and all of his online friends
	private void sendBroadcast(HttpServletRequest request, HttpServletResponse response) {
		//Get message
		String message = request.getParameter("message");
		if(message!=null && !message.isEmpty()){
			//Get sender
			String senderEmail = request.getParameter("sender");
			if(senderEmail==null) senderEmail = ((User)(request.getSession().getAttribute("authenticatedUser"))).getEmail(); //in case email was not found in parameters
			User sender = system.getUser(senderEmail);
			//Format broadcast message
			String broadcastMessage = sender.getNickname()+":"+message;
			//Send broadcast message to sender
			BROADCASTS.get(senderEmail).add(broadcastMessage);
			//Send broadcast message to online friends of sender
			for(String loggedInUserEmail : SESSIONS.values()){ //Iterate through logged in users (only online users should get broadcast messages)
				for(User friend : sender.getFriends()){ //Iterate through friends of sender (only friends of sender should get their broadcast messages)
					//If logged in user is friend of sender, add message to their broadcast messages
					if(friend.getEmail().equals(loggedInUserEmail)){
						BROADCASTS.get(loggedInUserEmail).add(broadcastMessage);
					}
				}
			}
		}
	}

	//Retrieve and send list of broadcast messages destined to user with given email
	private void getBroadcasts(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String requestingEmail = request.getParameter("email");
		if(requestingEmail==null || requestingEmail.equals("undefined") || requestingEmail.isEmpty()) //if email was not given in parameters
			requestingEmail = ((User)(request.getSession().getAttribute("authenticatedUser"))).getEmail();
		List<String> broadcasts = BROADCASTS.get(requestingEmail);
		String broadcastsXML = broadcastMessagesToXML(broadcasts);
		response.setContentType("text/xml");
		response.getWriter().write(broadcastsXML);
	}

	//Return xml string of given broadcast messages
	private String broadcastMessagesToXML(List<String> messages){
		StringBuffer xml = new StringBuffer();
		xml.append("<messages>\n");
		if(messages!=null){
			for(String message : messages){
				//Get sender and content of message
				String sender = message.substring(0,message.indexOf(":"));
				String content = message.substring(message.indexOf(":")+1,message.length());
				//Format content and sender to xml and add this to stringbuffer
				xml.append("<message>\n");
					xml.append("<content>");
					xml.append(content);
					xml.append("</content>\n");
					xml.append("<sender>");
					xml.append(sender);
					xml.append("</sender>\n");
				xml.append("</message>\n");
			}
		}
		xml.append("</messages>");
		return xml.toString();
	}
		
	//Retrieve and send list of friends of user with given email
	private void getFriends(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String requestingEmail = request.getParameter("email");
		if(requestingEmail==null || requestingEmail.equals("undefined") || requestingEmail.isEmpty()) //if email was not given in parameters
			requestingEmail = ((User)(request.getSession().getAttribute("authenticatedUser"))).getEmail();
		List<User> friends = system.getFriends(system.getUser(requestingEmail));
		String filter = request.getParameter("filter");
		//if there is a filter (user is searching in his friends list), show only the friends that contain filter string
		if(filter!=null && !filter.isEmpty()){
			List<User> filteredFriends = new ArrayList<>();
			for(User friend : friends){
				//check on first name, last name, nickname, email, ...
				if(friend.getNickname().toLowerCase().contains(filter.toLowerCase()) 
						|| friend.getFirstName().toLowerCase().contains(filter.toLowerCase()) 
						|| friend.getName().toLowerCase().contains(filter.toLowerCase())){
					filteredFriends.add(friend);
				}
			}
			friends = filteredFriends;
		}
		Collections.sort(friends); //sorts friends by status and nickname
		String friendsXML = usersToXML(friends);
		response.setContentType("text/xml");
		response.getWriter().write(friendsXML);
	}
		
	//Return xml string of given users
	private String usersToXML(List<User> users) {
		StringBuffer xml = new StringBuffer();
		xml.append("<users>\n");
		for(User user : users){
			xml.append("<user>\n");
				xml.append("<nickname>");
				xml.append(user.getNickname());
				xml.append("</nickname>\n");
				xml.append("<email>");
				xml.append(user.getEmail());
				xml.append("</email>\n");
				xml.append("<status>");
				xml.append(user.getStatus());
				xml.append("</status>\n");
			xml.append("</user>\n");
		}
		xml.append("</users>");
		return xml.toString();
	}

	/*
	 * PUSH
	 */
	
	@OnOpen
	public void onOpen(Session session){
		SESSIONS.put(session,null);
		System.out.println("Session with id "+session.getId()+" has joined.");
	}
	@OnClose
	public void onClose(Session session){
		BROADCASTS.remove(system.getUser(SESSIONS.get(session)));
		SESSIONS.remove(session);
		System.out.println("Session with id "+session.getId()+" has left.");
	}
	@OnMessage
	public void onMessage(String incomingMessage, Session session){
		if(!incomingMessage.isEmpty()){
			//If this is a message for registering user to active sessions, add user's email address to SESSIONS and BROADCASTS
			if(incomingMessage.split(":")[0].equals("registering")){
				String toBeRegisteredEmail = incomingMessage.split(":")[1];
				if(!toBeRegisteredEmail.isEmpty()){
					//if email already linked to another session, delete other session (so that a new one can be added that is linked to this email)
					for(Session s : SESSIONS.keySet()){
						if(SESSIONS.get(s) != null && SESSIONS.get(s).equals(toBeRegisteredEmail)){
							SESSIONS.remove(s);
							break;
						}
					}
					//add user email address to SESSIONS (so he can receive chat messages) and to BROADCASTS (so he can receive broadcast messages)
					SESSIONS.put(session, toBeRegisteredEmail);
					BROADCASTS.put(toBeRegisteredEmail, new ArrayList<String>());
				}
			}
			//Else if this is a chat message, extract source email, destination email and message so that the message can be sent to the appropriate user
			else {
				String[] data = incomingMessage.split("\\n");
				String sourceEmail = data[0];
				String destinationEmail = data[1];
				if(SESSIONS.get(session)==null){
					SESSIONS.put(session,sourceEmail);
				}
				Session destinationSession = null;
				//if chatting to Echo, set up message to self (echo just repeats what user says)
				if(destinationEmail.equals("echo@echo.com")){
					incomingMessage = destinationEmail+"\n"+sourceEmail+"\n"+data[2]; //invert message headers (so message gets sent to user that sent it
					destinationSession = session; //select itself
				}
				//if chatting to other user, set up message to appropriate user
				else {
					for(Session s : SESSIONS.keySet()){
						if(SESSIONS.get(s)!=null && SESSIONS.get(s).equals(destinationEmail)){
							destinationSession = s; //select appropriate user
							break;
						}
					}
				}
				//if a destination session was found, send chat message to this session
				if(destinationSession!=null){
					try {
						destinationSession.getBasicRemote().sendText(incomingMessage);
					} catch (IOException e) {
						//do nothing
					}
				}
			}
		}
	}
	
}
