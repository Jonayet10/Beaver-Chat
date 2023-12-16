$(document).ready(function() {
    $('#loginForm').submit(function(event) {
        let url = 'http://localhost:8000/login?';
        let username = $('.username').val();
        url += username;

        // send the request to login
        console.debug("Logging in user " + username)
        let http = new XMLHttpRequest();
        http.open('GET', url);
        http.send();

        // if the login failed, tell user the username was taken
        http.onreadystatechange = (e) => {
            if (http.responseText.status === 400) {
                console.debug("login failed");
                $('#errorMsg').css('visibility', 'visible');
            }
        }
    });
});
