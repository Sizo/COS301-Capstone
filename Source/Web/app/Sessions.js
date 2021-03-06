/* 
* 	File:	Sessions.js
*	Author:	Binary Ninjaz (Letanyan,Ojo)
*
*	Description:	This file contais functions for the data representation on 
*					"Sessions.html". It requests and recieves data from firebase
*					databse, and uses google graph APIs 
*/
var pageIndex = null; // track the last session loaded. Used for pagination
var pageSize = 21;
$(window).bind("load", () => {
	var divHide = document.getElementById('loader'); /* When the page loads, the error div should be hidden */
	divHide.style.visibility = "hidden"; /* When the page loads, the error div should be hidden, do not remove */
  let succ = () => {
    initPage();
    initMap();
    google.charts.load('current', {'packages':['corechart']});
  };
  let fail = () => {
    sessions = [];
  };
  retryUntilTimeout(succ, fail, 1000);
});

/* This function initiates the coordinates on the map*/
var map;
function initMap() {
  locationLookup((data, response) => {
    var latLng = new google.maps.LatLng(data.lat, data.lon);
    map.setCenter(latLng);
    map.setZoom(11);
  });
  map = new google.maps.Map(document.getElementById('map'), {
    center: {lat: -25, lng: 28 },
    zoom: 14,
    mapTypeId: 'satellite'
  });
}

/* Function returns a session, given a particular key */
function sessionForKey(key, sortedMap) {
  for (const groupIdx in sortedMap) {
    const group = sortedMap[groupIdx];
    for (const itemIdx in group.values) {
      const item = group.values[itemIdx];
      console.log(item.key, key);
      if (item.key === key) {
        return item;
      }
    }
  }
  return undefined;
}

/* Function loads a list of sessions on the side of the screen */
function sessionsListLoader(loading) {
  var sessionsListHolder = document.getElementById("sessionsListLoader");
  if (!loading) {
	var divHide = document.getElementById('loader');
	divHide.style.visibility = "hidden";
	updateSpiner(false);
    sessionsListHolder.innerHTML = "<button type='button' class='btn btn-secoundary' style='margin: 4px' onclick='newPage()'>Load More Sessions</button>";
  } else {
	var divHide = document.getElementById('loader');
	divHide.style.visibility = "visible";
	updateSpiner(true);
    /*sessionsListHolder.innerHTML = "<h2>Loading Sessions...</h2>";*/
  }
}

var farms = {};
var orchards = {};
var workers = {};
function initPage() {
  var sessionsList = document.getElementById("sessionsList");
  sessionsListLoader(true);
  farms = {};
  orchards = {};
  workers = {};
  setWorkers(workers, () => {
    newPage();
    setFarms(farms, () => {});
    setOrchards(orchards, () => {});
  });
}

// sorted map
var sessions = [];
var filteredSessions = [];
function insertSessionIntoSortedMap(session, key, checkEqualKey, sortedMap) {
  var belongsInGroup = undefined;
  for (const groupIdx in sortedMap) {
    const group = sortedMap[groupIdx];
    if (checkEqualKey(group.key, key)) {
      belongsInGroup = groupIdx;
      break;
    }
  }

  if (belongsInGroup !== undefined) {
    sortedMap[belongsInGroup].values.push(session);
    sortedMap[belongsInGroup].values = sortedMap[belongsInGroup].values.sort((a, b) => {
      return b.value.start_date - a.value.start_date;
    });
  } else {
    sortedMap.push({key: key, values: [session]});
  }

  sortedMap = sortedMap.sort((a, b) => {
    return b.key - a.key;
  });
}

function displaySessions(sortedMap, displayHeader, isFiltered) {
  var sessionsList = document.getElementById("sessionsList");
  sessionsList.innerHTML = "";
  for (const groupIdx in sortedMap) {
    const group = sortedMap[groupIdx];
    const key = group.key;
    sessionsList.innerHTML += "<h5>" + displayHeader(key) + "</h5>";
    for (const itemIdx in group.values) {
      const item = group.values[itemIdx];
      const foreman = workers[item.value.wid];
      const time = moment(new Date(item.value.start_date * 1000)).format(isFiltered ? "YYYY/MM/DD HH:mm" : "HH:mm");
      const text = foreman.name + " " + foreman.surname + " - " + time;
      sessionsList.innerHTML += "<button type='button' class='btn btn-primary' style='margin: 4px' onclick=loadSession('" + item.key + "') >" + text + "</button>";
      if (isFiltered) {
        sessionsList.innerHTML += "<p class='searchReason'>" + item.reason + "</p>";
      }
    }
  }
}

function newPage() {
  var ref;
  sessionsListLoader(true);
  var sessionsList = document.getElementById("sessionsList");
  if (pageIndex === null) {
    ref = firebase.database().ref('/' + userID() + '/sessions')
      .orderByKey()
      .limitToLast(pageSize);
  } else {
    ref = firebase.database().ref('/' + userID() + '/sessions')
      .orderByKey()
      .endAt(pageIndex)
      .limitToLast(pageSize);
  }
  var tempSessions = [];
  ref.once('value').then((snapshot) => {
    var lastSession = "";
    var resultHtml = [];
    var i = 0;
    snapshot.forEach((child) => {
      const obj = child.val();
      const foreman = workers[obj.wid];
      if (foreman !== undefined) {
        if (lastSession === "") {
          lastSession = child.key;
        }
        const session = {value: obj, key: child.key};

        const key = moment(new Date(session.value.start_date * 1000)).startOf('day');
        const equalDates = (a, b) => {
          return a.isSame(b);
        };

        insertSessionIntoSortedMap(session, key, equalDates, sessions);
      }
    });

    pageIndex = lastSession;
    sessionsListLoader(false);
    const formatHeader = (date) => {
      return date.format("dddd, DD MMMM YYYY");
    };
    filterSessions();
  });
}

var markers = []; /* An array of markers for the map */
var polypath; /* Variable for storing the path of the polygon */

/* This functions plots the graph of a choosen session by a particular foreman */
function loadSession(sessionID) {
  const ref = firebase.database().ref('/' + userID() + '/sessions/' + sessionID);

  var gdatai = 0;
  var graphData = {datasets: [{data: [], backgroundColor: []}], labels: []};


  const session = sessionForKey(sessionID, sessions);
  const val = session.value;

  const start = new Date(val.start_date * 1000);
  const end = new Date(val.end_date * 1000);
  const wid = val.wid;
  const foreman = workers[wid];
  const fname = foreman.name + " " + foreman.surname;

  var sessionDetails = document.getElementById("sessionDetails");

  sessionDetails.innerHTML = "<form class=form-horizontal'><div class='form-group'>"
  sessionDetails.innerHTML += "<div class='col-sm-12'><label>Foreman: </label> " + fname + "</div>"
  sessionDetails.innerHTML += "<div class='col-sm-6'><label>Time Started: </label><p> " + start.toLocaleString() + "</p></div>"
  sessionDetails.innerHTML += "<div class='col-sm-6'><label>Time Ended: </label><p> " + end.toLocaleString() + "</p></div>"
  sessionDetails.innerHTML += "</div></form>";

  var first = true;

  if (val.track !== undefined) {
    var track = [];
    for (const ckey in val.track) {
      const coord = val.track[ckey];
      const loc = new google.maps.LatLng(coord.lat, coord.lng)
      track.push(loc);
      if (first) {
        map.setCenter(loc);
        map.setZoom(15);
        first = false;
      }
    }
    if (polypath !== undefined) {
      polypath.setMap(null);
    }
    polypath = new google.maps.Polyline({
      path: track,
      geodesic: true,
      strokeColor: '#0000FF',
      strokeOpacity: 1.0,
      strokeWeight: 2,
      map: map
    });
  }

  for (const marker in markers) {
    markers[marker].setMap(null)
  }

  if (val.collections !== undefined) {
    for (const ckey in val.collections) {
      const collection = val.collections[ckey];
      const worker = workers[ckey];

      var wname = "";
      if (worker !== undefined) {
        wname = worker.name + " " + worker.surname;
      }

      graphData.datasets[0].data.push(collection.length);
      graphData.datasets[0].backgroundColor.push(harvestColorful[gdatai % 6]);
      graphData["labels"].push(wname);
      gdatai++;

      for (const pkey in collection) {
        const pickup = collection[pkey];
        const coord = new google.maps.LatLng(pickup.coord.lat, pickup.coord.lng);
        if (first) {
          map.setCenter(coord);
          first = false;
        }
        var marker = new google.maps.Marker({
          position: coord,
          map: map,
          title: wname
        });
        markers.push(marker);
      }
    }
  }
  initGraph(graphData);
}

/* This function (is a subfunction) simply displays the doughnut graph */
var chart;
function initGraph(collections) {
  if (chart !== undefined) {
    chart.destroy();
  }

  var options = {
    title: {
      display: true,
      text: "Worker Performance Summary"
    },
    legend: {
      position: 'right'
    }
  };
  var ctx = document.getElementById("doughnut").getContext('2d');
  chart = null;

  chart = new Chart(ctx,{
    type: 'doughnut',
    data: collections,
    options: options
  });
}

/* This function shows the spinner while still waiting for resources*/
var spinner;
function updateSpiner(shouldSpin) {
  var opts = {
	lines: 8, // The number of lines to draw
	length: 37, // The length of each line
	width: 10, // The line thickness
	radius: 20, // The radius of the inner circle
	scale: 1, // Scales overall size of the spinner
	corners: 1, // Corner roundness (0..1)
	color: '#4CAF50', // CSS color or array of colors
	fadeColor: 'transparent', // CSS color or array of colors
	speed: 1, // Rounds per second
	rotate: 0, // The rotation offset
	animation: 'spinner-line-fade-quick', // The CSS animation name for the lines
	direction: 1, // 1: clockwise, -1: counterclockwise
	zIndex: 2e9, // The z-index (defaults to 2000000000)
	className: 'spinner', // The CSS class to assign to the spinner
	top: '50%', // Top position relative to parent
	left: '50%', // Left position relative to parent
	shadow: '0 0 1px transparent', // Box-shadow for the lines
	position: 'absolute' // Element positioning
  };
  
  var target = document.getElementById("loader"); //This is where the spinner is gonna show
  if (shouldSpin) {
	  target.style.position = "absolute"; //This is for proper alignment
	  target.style.top = "100px"; //This is for proper alignment
	  target.style.left = "100px"; //This is for proper alignment
	  spinner = new Spinner(opts).spin(target); //The class and corresponding css are defined in spin.js and spin.css
  } else {
	  //target.style.top = "0px";
	  spinner.stop(); //This line stops the spinner. 
	  spinner = null;
  }
}

function filterSessions() {
  const searchField = document.getElementById("sessionSearchField");
  const searchText = searchField.value;

  if (searchText === "") {
    const formatHeader = (date) => {
      return date.format("dddd, DD MMMM YYYY");
    };
    displaySessions(sessions, formatHeader, false);
  } else {
    filteredSessions = []
    for (const groupKey in sessions) {
      const group = sessions[groupKey].values;

      for (const sessionId in group) {
        const session = group[sessionId];
        const sessionResults = searchSession(session.value, searchText, farms, orchards, workers);

        for (const key in sessionResults) {
          var newSession = session;
          newSession["reason"] = sessionResults[key];
          insertSessionIntoSortedMap(session, key, (a, b) => { return a === b; }, filteredSessions);
        }
      }
    }
    console.log(JSON.stringify(filteredSessions));

    const formatHeader = (title) => { return title };

    displaySessions(filteredSessions, formatHeader, true);
  }
}
