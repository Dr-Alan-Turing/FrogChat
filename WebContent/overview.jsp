<!DOCTYPE html>
<head>
    <meta charset="UTF-8">
    <title>FrogChat</title>
    <link rel="stylesheet" type="text/css" href="style.css">
    <link rel="icon" href="files/favicon.ico">
    <script type="text/javascript" src="jquery.js"></script>
    <script type="text/javascript" src="overview.js"></script>
</head>
<body id="chatPage">
    <div id="wrapper">
    <section id="header">
        <h1>FrogChat</h1>
    </section>
    <form method="POST" action="ChatServlet?action=processLogOut">
    	<input id="logoutButton" type="submit" value="Log-out" onclick="closeSocket()">
    </form>
    <div id="statusWrapper">
    <h2>Personal</h2>
    <section style="display: none" id="addNewFriendSection">
        <section id="searchNewFriendSection">
            <h4>Add new friend:</h4>
            <form id="addNewFriendForm">
            	<p>
                    <label>Nickname?</label>
                    <input id="searchNickname" type="text" name="nickname">
                </p>
                <p>
                    <label>First name?</label>
                    <input id="searchFirstName" type="text" name="firstName">
                </p>
                <p>
                    <label>Last Name?</label>
                    <input id="searchName" type="text" name="name">
                </p>
            </form>
            <div class="clear"></div>
            <button id="cancelSearchNewFriendButton" onclick="hideAddNewFriend()">Cancel</button>
            <button id="searchNewFriendButton" onclick="searchUser()">Search</button>
        </section>
        <div class="clear"></div>
        <section style="display: none" id="addNewFriendResultsSection">
            <table id="foundUsersTable">
                <tr>
                    <th>Results:</th>
                </tr>
            </table>
        </section>
    </section>
    <section id="statusSection">
        Choose status:
        <select id="statusSelect">
            <option value="ONLINE">Online</option>
            <option value="AWAY">Away</option>
            <option value="OFFLINE">Offline</option>
        </select>
        <button id="addNewFriendButton" onclick = "showAddNewFriend()">Add new friend</button>
    	<div id="sendBroadcast">
            <input id="broadcastInput" type="text" placeholder="What you are up to?">
            <button id="sendBroadcastButton">Broadcast!</button>
        </div>
    </section>
    
    <div id="personalClear"></div>
    </div>
    <div id="friendsWrapper">
    <h2>Friends</h2>
    <div id="broadcasts">
        <h3>Broadcasts</h3>
        <div id="broadcastMessages">
        	<p>Messages broadcasted by you and your friends will appear here.</p>
        </div>
    </div>
    <h3>Chat</h3>
    <p>
        <input id="searchFriendsField" type="search" placeholder="Search on first-/last-/nickname..."/>
    </p>
    <table id="users">
    	<tr>
    		<th>Nickname</th>
    		<th>Status</th>
    		<th></th>
    		<th></th>
    	</tr>
    	<!--code is auto-filled here with javascript-->
    </table>
    </div>
    </div>
    <section id="chatsSection">
        <div id="topClear"></div>
        <div id="chats">
<!--     	code is auto-filled here with javascript -->
        </div>
        <div id="bottomClear"></div>
    </section>
    <section id="chatsFooter">
        <p>Open conversations appear here.</p>
    </section>
    
    <script>
    	//global variable(s)
     	var loggedInEmail = "${email}";
    	
     	//scripts to be executed on page load or after delay
     	reloadUsersTable();
    	openSocket();
     	setTimeout(function() {
         	sendToSocket("registering:${email}");
     	}, 500); //wait half a second to make sure connection is open and ready
    	setTimeout(function() {
    		refreshBroadcasts();
    	}, 700); //wait till socket has opened
     	
    	//scripts to be executed periodically (at intervals)
    	window.setInterval("reloadUsersTable();", 2000);
    	window.setInterval("refreshBroadcasts();", 2000);

		//scripts to be executed on triggered events
     	$("#statusSelect").change(function(){ changeStatus(); });
		$("#sendBroadcastButton").click(function(){ sendBroadcast(); });
	</script>
</body>
</html>