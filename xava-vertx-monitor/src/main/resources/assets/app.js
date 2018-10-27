var module_route = angular.module('route', [ 'ngRoute' ]);
module_route.config([ '$routeProvider', function($routeProvider) {
	$routeProvider.when('/', {
		templateUrl : './overview.html',
		controller : 'OverviewCtrl'
	}).when('/metrics', {
		templateUrl : './metrics.html',
		controller : 'MetricsCtrl'
	}).when('/discovery', {
		templateUrl : './discovery.html',
		controller : 'DiscoveryCtrl'
	}).otherwise({
		redirectTo : '/'
	});
} ]);

var app = angular.module('MetricsApp', [ 'ng', 'route' ]);

app.controller('OverviewCtrl', function($scope, $http) {
	$http.get('/monitor/overview').then(function(response) {
		$scope.verticles = response.data.payload;
	});
});

app.controller('DiscoveryCtrl', function($scope, $http) {
	$http.get('/monitor/discovery').then(function(response) {
		$scope.discoveryInfo = response.data;
	});
});

app.controller('MetricsCtrl', function($scope, $http) {
	$http.get('/monitor/metrics').then(function(response) {
		$scope.metrics = response.data.payload;
	});

	$scope.expandSelected = function(metric) {
		$scope.metrics.forEach(function(val) {
			val.expanded = false;
		})

		$scope.metric = metric;

		metric.expanded = true;
		$http.get('/monitor/metrics/' + metric.id).then(
				function(response) {
					$scope.metric.snapshot = response.data.payload.snapshot;
				});
	}
});
