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

/*
 * START TABLE OF CONTENTS:
 * 
 * 1. OPDRACHTEN FUNCTIES
 * 2. OTHER JAVASCRIPT FUNCTIONS
 * 
 * END TABLE OF CONTENTS.
 */

/*
 * START NEW SECTION DECLARATION.
 * 
 * 
 * --> 1. OPDRACHTEN FUNCTIES <--
 * 
 * 
 * 
 * END NEW SECTION DECLARATION.
 */

/*
 * DEEL 1 - JavaScript, Ajax, Polling
 * -> Lijst van vrienden tonen
 * -> Zoeken in vriendenlijst om te kunnen chatten
 * -> Zoeken naar nieuwe vrienden
 * -> Toevoegen van nieuwe vrienden
 */

var xHRObject = new XMLHttpRequest();

/*
 * Lijst van vrienden tonen
 * Zoeken in vriendenlijst om te kunnen chatten
 */

function reloadUsersTable(){
	var filter = $("#searchFriendsField").val();
	xHRObject.open("GET", "ChatServlet?action=getFriends&email="+loggedInEmail+"&filter="+filter, true);
	xHRObject.onreadystatechange =
		function(){
			if(xHRObject.readyState==4){
				if(xHRObject.status==200){
					var serverResponse = xHRObject.responseXML;
					//If no server response, log out user
					if(serverResponse==null){
						logOut();
					}
					//Else (re-)load users table
					else {
						//clear table rows (apart from header)
						$("#users tr:gt(0)").remove();
						var users = serverResponse.getElementsByTagName("user");
						for(i=0; i<users.length; i++){
							var user = {
								nickname: serverResponse.getElementsByTagName("nickname")[i].textContent,
								email: serverResponse.getElementsByTagName("email")[i].textContent,
								status: serverResponse.getElementsByTagName("status")[i].textContent
							};
							var chatButton = "";
							//if user online or away, set up 'chat' button
							if(user.status!="OFFLINE"){
								var disabled = "";
								//if a chat window for this user is open, button should be disabled
								if(isChatWindowOpen(user.email)){
									disabled = " disabled";
								}
								chatButton = "<button onclick=\"showChatWindow(\'"+user.email+"\',\'"+user.email+"\')\""+disabled+">chat</button>";
							}
							//set up row
							var rowHTML = "<tr>" +
									  		"<td>"+user.nickname+"</td>" +
									  		"<td>"+user.status+"</td>" +
									  		"<td>"+chatButton+"</td>" +
									  		"<input class=\"hiddenEmail\" type=\"hidden\" value=\""+user.email+"\"/>"+
									  	  "</tr>";
							//add row to table
							$("#users tr:last").after(rowHTML);
							//create chat window if none exists (will be hidden until user presses on chat button)
							if(getChatWindow(user.email) == null){
								createChatWindow(user.nickname, user.email);
							}
						}
					}
				}
			}
		};
	xHRObject.overrideMimeType('text/xml');
	xHRObject.send(null);
}

/*
 * Zoeken naar nieuwe vrienden
 */

function searchUser(){
	var nickname = $("#searchNickname").val();
	var firstName = $("#searchFirstName").val();
	var name = $("#searchName").val();
	xHRObject.open("GET", "ChatServlet?action=searchUser&nickname="+nickname+"&firstName="+firstName+"&name="+name);
	xHRObject.onreadystatechange = displayFoundUsers;
	xHRObject.overrideMimeType('text/xml');
	xHRObject.send(null);
}

function displayFoundUsers(){
	if(xHRObject.readyState==4){
		if(xHRObject.status==200){
			var serverResponse = xHRObject.responseXML;
			if(serverResponse==null){
				//bug encountered
			}
			else {
				$("#foundUsersTable").find("tr:gt(0)").remove();
				var users = serverResponse.getElementsByTagName("user");
				if(users.length==0){
					$("#foundUsersTable tr:last").after($("<tr><td>No users found!</td></tr>"));
				}
				else {
					for(i=0; i<users.length; i++){
						var user = serverResponse.getElementsByTagName("nickname")[i].textContent;
						var email = serverResponse.getElementsByTagName("email")[i].textContent;
						var isFriend = serverResponse.getElementsByTagName("isFriend")[i].textContent;
						var secondColumnContent = "<button onclick=\"addUser('"+email+"')\">Add</button>";
						if(isFriend=="true"){
							secondColumnContent = "(already your friend)";
						}
						$("#foundUsersTable tr:last").after($("<tr><td>"+user+"</td><td>"+secondColumnContent+"</td></tr>"));
					}
				}

			}
		}
	}
	showAddNewFriendResults();
}

/*
 * Toevoegen van nieuwe vrienden
 */

function addUser(email){
	xHRObject.open("GET", "ChatServlet?action=addUser&email="+email);
	xHRObject.onreadystatechange = function(){confirmFriendAdded(email);};
	xHRObject.overrideMimeType('text/xml');
	xHRObject.send(null);
}

function confirmFriendAdded(email){
	if(xHRObject.readyState==4){
		if(xHRObject.status==200){
			alert("User with email "+email+" was added!");
			hideAddNewFriend();
		}
	}
}

/*
 * DEEL 2 - Ajax, Push, Websocket
 * 
 * -> Nieuwe functionaliteit in chat applicatie: one-to-one chatting tussen gebruikers
 *
 *
 */

var webSocket;

function openSocket(){
	if(webSocket === undefined || webSocket.readyState===WebSocket.CLOSED && email!=undefined){
		/*for online:*///webSocket = new WebSocket("ws://193.191.187.13:11998/ChatApplicatie/chat");
		/*for offline (testing)"*/webSocket = new WebSocket("ws://localhost:8080/ChatApplicatie/chat");
		webSocket.onmessage = function(event){
			var incomingMessage = event.data;
			processMessage(incomingMessage);
		}
	}
}

function closeSocket(){
	webSocket.close();
}

function processMessage(m){
	//retrieve items from message
	var data = m.split("\n");
	var sourceEmail = data[0];
	var destinationEmail = data[1];
	var message = data[2];
	for(i=3; i<data.length; i++){ //in case message spans over several lines or includes '\n'
		message+= "\n"+data[i];
	}
	//send items to appropriate chat window
	var email = destinationEmail;
	if(email==loggedInEmail){
		email = sourceEmail;
	}
	chatWindow = getChatWindow(email);
	sendMessageToChatWindow(chatWindow, message, sourceEmail);
	if(!isChatWindowOpen(email)) showChatWindow(email);
}

function sendMessage(input, destinationEmail){
	if(webSocket!==undefined && webSocket.readyState!==WebSocket.CLOSED){
		var message = input.value;
		input.value = "";
		var sourceEmail = loggedInEmail; //var declared on jsp page
		var outgoingMessage = sourceEmail+"\n"+destinationEmail+"\n"+message;
		//send message to friend
		sendToSocket(outgoingMessage);
		//send message to self so that it can be shown in chat window
		processMessage(outgoingMessage);
	}
}

function sendToSocket(message){
	webSocket.send(message);
}

//Zie 'OTHER JAVASCRIPT FUNCTIONS' hieronder voor gerelateerde functies

/*
 * DEEL 3 - jQuery, Ajax, Polling
 * 
 * -> Broadcast berichten versturen van 1 persoon naar al zijn online vrienden (toestemming hiervoor per mail gevraagd)
 * -> Status veranderen via $.ajax post
 * -> JQuery gebruiken om chatten er fancy uit te laten zien
 */

/*
 * Broadcast berichten sturen van 1 persoon naar al zijn online vrienden
 */

function sendBroadcast(){
	if($("#broadcastInput").val()!==""){
		$.post("?action=sendBroadcast",
	     	{
	     		message: $("#broadcastInput").val(),
	     		email: loggedInEmail
	     	},
	     	function(data, status){
	     		$("#broadcastInput").val("");
		     	}
	    );
	}
}

function refreshBroadcasts(){
	$.ajax({url: "?action=getBroadcasts&email="+loggedInEmail, success: function(result){
		if(result!='[object XMLDocument]'){
			logOut();
		}
		var broadcastsHtml = "";
		var messages = result.getElementsByTagName("message");
		for(i=0; i<messages.length; i++){
			var content = result.getElementsByTagName("content")[i].textContent;
			var sender = result.getElementsByTagName("sender")[i].textContent;
			broadcastsHtml+= "<p class=\"broadcastMessage\">"+content+" <i>-"+sender+"</i></p>\n";
		}
		if(broadcastsHtml==""){
			broadcastsHtml = "<p>Messages broadcasted by you and your friends will appear here.</p>"
		}
		$("#broadcastMessages").html(broadcastsHtml);
	}});
}

/*
 * Status veranderen via $.ajax post
 */

function changeStatus(){
	$.post("?action=changeStatus",
			{
				email: loggedInEmail,
				status: $("#statusSelect").val()
			},
			function(data, status){
				//no action needed on response
			});
}

/*
 * JQuery gebruiken om chatten er fancy uit te laten zien
 */

// Zie 'OTHER JAVASCRIPT FUNCTIONS' hieronder


/*
 * DEEL 4 - AngularJS, Ajax
 * 
 * -> Status tonen van alle gebruikers (online, away, offline) met AngularJS
 *
 *
 */

/*
 * Status tonen van alle gebruikers (online, away, offline) met AngularJS
 */

// Zie bestand login.js


/*
 * START NEW SECTION DECLARATION.
 * 
 * 
 * --> 2. OTHER JAVASCRIPT FUNCTIONS <--
 * 
 * 
 * 
 * END NEW SECTION DECLARATION.
 */


/*
 * Add new friend functions
 */

function showAddNewFriendResults(){
    $("#addNewFriendResultsSection").show();
}

function hideAddNewFriendOnLoad(){
	document.getElementById("addNewFriendSection").style.display="none";
    document.getElementById("addNewFriendResultsSection").style.display="none";
}

function hideAddNewFriend(){
    $("#addNewFriendButton").fadeIn(300);
    $("#addNewFriendSection").hide(300,"linear");
    $("#addNewFriendResultsSection").hide();
}
function showAddNewFriend(){
    $("#addNewFriendButton").fadeOut(300);
    $("#addNewFriendSection").show(200,"linear");
}

/*
 * Chat window functions
 */

function createChatWindow(nickname,email){
    //create chat div
	var div = document.createElement("div");
	div.setAttribute("class","chatWindow");
	div.style.display = "none";
	div.innerHTML = "<input class=\"emailParameter\" type=\"hidden\" value=\""+email+"\"/>" +
			"<h1>"+nickname+"</h1>" +
			"<button class=\"closeChatWindowButton\" onclick=\"hideChatWindow(\'"+email+"\')\">X</button>" +
			"<section class=\"messages\"></section>";
	/*
    var div = "<div class=\"chatWindow\" hidden>" +
    				 +
    			"</div>";
    */
    //append chat div to chats section
    $("#chats").append(div);
    //create input field (to type and send messages)
    var input = document.createElement("input");
    input.setAttribute("class","chatField");
    input.setAttribute("type","text");
    input.setAttribute("placeholder","Type your message here.");
    //execute function on 'enter' key-press
    input.onkeypress = function (e) {
    	if(!e) e = window.event;
    	var keyCode = e.keyCode || e.which;
    	if(keyCode == '13'){
    		sendMessage(this,email);
    	}
    };
    //append input field to chat div
    div.appendChild(input);
}

function getChatWindow(email){
	var chatWindow = null;
	$(".chatWindow").each(function(index, value){
		if(value.getElementsByClassName("emailParameter")[0].value==email){
			chatWindow = value;
			return false;
		}
	});
	return chatWindow;
}

function showChatWindow(email){
	//disable chat button
	$(getChatButton(email)).prop("disabled",true);
	//show window and focus on input field
	var chatWindow = getChatWindow(email);
	$(chatWindow).show(200);
	$(chatWindow.getElementsByClassName("chatField")[0]).focus();
}

function hideChatWindow(email){
	//close window
	$(getChatWindow(email)).hide(200);
	//enable chat button
	$(getChatButton(email)).prop("disabled",false);
}

function isChatWindowOpen(email){
	var chatWindow = getChatWindow(email);
	if(chatWindow==null){
		return false;
	}
	else {
		return chatWindow.style.display!="none";
	}
}

function sendMessageToChatWindow(chatWindow, message, sourceEmail){
	var messageSection = chatWindow.getElementsByClassName("messages")[0];
	var p = document.createElement("p");
	var meOrYou = "you";
	if(sourceEmail==loggedInEmail){ //set meOrYou to "me" if message was sent by logged in person
		meOrYou = "me";
	}
	p.setAttribute("class", meOrYou);
	p.innerHTML = message;
	messageSection.appendChild(p);
	//scroll to bottom of messages
	messageSection.scrollTop = messageSection.scrollHeight;
}

/*
 * Chat button functions
 */

function getChatButton(email){
	var chatButton = null;
	$("#users tr").not(":first").each(function(index, value){
		if(value.getElementsByClassName("hiddenEmail")[0].value==email){
			chatButton = value.getElementsByTagName("button")[0];
			return false;
		}
	});
	return chatButton;
}

/*
 * Miscellaneous functions
 */

//Logs out the user on server communication error (like when using Safari).
function logOut(){
	window.location.href = '/ChatApplicatie/ChatServlet?action=processLogOut';
	alert("You have been logged out. Either you logged out [from another location], the server restarted, or you are using Safari (websockets we learned are not supported due to headers in handshake).");
}