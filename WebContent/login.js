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

var app = angular.module('allUsersOverview', []);
app.controller('allUsersOverviewController', function($scope, $http) {
	$http.get("ChatServlet?action=showAllUsersOverview")
	.success(function (response) {
		$scope.online = response.online;
		$scope.away = response.away;
		$scope.offline = response.offline;
	});
});