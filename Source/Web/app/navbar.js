window.onload = function () {
	
};


firebase.auth().onAuthStateChanged(function (user) {
	if (user) {
		var user = firebase.auth().currentUser;
		var name;
		if (user != null) {
			name = user.email;
		}  
		document.getElementById("emailInNav").innerHTML = name + " <span class=\"glyphicon glyphicon-cog\"></span>"	
	}
});


