(function() {
    const SUGGESTIONS_URL = 'http://localhost:8000/suggestions?';
    const CORRECTIONS_URL = "http://localhost:8000/corrections?";
    const AUTOCOMPLETE_URL = "http://localhost:8000/autocomplete?";
    const SEND_MSG_URL = "http://localhost:8000/sendMessage";
    const CONTACTS_URL = "http://localhost:8000/getContacts";
    const GET_MSG_URL = "http://localhost:8000/getMessages?";

    let currentChatID = "";
    let allChats = {};

    $(document).ready(function() {
        function talk(msg) {
            $('#chat-window').append(
                $('<table width="100%"><tr><td><div class="from-' + msg.who + '">' + msg.text + '</div></td></tr></table>')
            );
            $('#chat-window')[0].scrollTo(0, $('#chat-window')[0].scrollHeight);
        }

        // update the suggestions using a call to java backend
        function updateSuggestions(event) {
            let text = event.target.value;
            let words = text.split(' ');
            if (words.length == 0) {
                return;
            }
            let context = words.join(' ');
            if (words.length > 3) {
                context = words.slice(-3, words.length).join(' ');
            }

            let http = new XMLHttpRequest();
            let url = SUGGESTIONS_URL + context + '&3';
            http.open('GET', url);
            http.send();
            http.onreadystatechange = (e) => {
                updateSuggestionsButtons(http.responseText);
            }
        }

        // updates the suggestion boxes with the newly retrieved suggestions
        function updateSuggestionsButtons(arrayString) {
            let suggestions = arrayString.substring(1, arrayString.length - 1);
            let words = suggestions.split(',');
            for (i = 0; i < 3; i++) {
                let id = '#sug' + (i + 1);
                if (i < words.length) {
                    $(id).html(words[i]);
                }
                else {
                    $(id).html('');
                }
            }
        }

        function spellCheck(text, event) {
            // remove non-alphanumeric characters and split the text into an array of words
            let words = event.target.value.split(' ')
            if (words.length == 0) {
                return
            }
            misspelledWord = words.slice(-1)[0].replace(/[\W_]+/g,"")

            // assemble the context string
            let idx = words.length - 2
            let numWords = 0
            let context = ""
            while (idx >= 0 && numWords < 3) {
                context = words[idx] + " " + context
                numWords += 1
                idx -= 1
            }

            // make request to get corrections
            let reqURL = CORRECTIONS_URL + context + '&' + misspelledWord
            let http = new XMLHttpRequest();
            http.open('GET', reqURL);
            http.send();

            // update the text area if we got a correction
            http.onreadystatechange = (e) => {
                if (http.responseText.length > 0 &&
                    http.responseText.toLowerCase() != misspelledWord.toLowerCase()) {
                    event.target.value = words.slice(0, -1).join(' ')
                    event.target.value += ' ' + http.responseText
                }
            };
        }

        function autoComplete(text, event) {
            let words = text.split(' ');
            let partial = words[words.length - 1];
            let reqURL = AUTOCOMPLETE_URL + partial;

            let http = new XMLHttpRequest();
            http.open('GET', reqURL);
            http.send();

            // update the text area if we got a correction
            http.onreadystatechange = (e) => {
                if (http.responseText.length > 0) {
                    event.target.value = words.slice(0, -1).join(' ')
                    event.target.value += ' ' + http.responseText
                }
            };
        }

        // sends a message to the backend as json
        // backend will then forward to irc
        function sendMessage() {
            message = $('#chat-text').val()

            let http = new XMLHttpRequest();
            http.open('POST', SEND_MSG_URL);
            http.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
            http.send(currentChatID + "&" + message);
        }

        // retrieves the contact list from the backend
        function getContacts() {
            let http = new XMLHttpRequest();
            http.open('GET', CONTACTS_URL);
            http.send();

            // update the contacts list
            http.onreadystatechange = (e) => {
                users = [];
                if (http.responseText != "") {
                    users = http.responseText.split(',');
                }
                $("#contact-list").empty();
                parentDiv = document.getElementById('contact-list');
                for (var i = 0; i < users.length; i++) {
                    createContactDiv(parentDiv, users[i]);
                }
                $('.contact').on('click', contactHandler);

                if (currentChatID != "") {
                    let id = '#' + currentChatID;
                    $(id).addClass('selected');
                }
            }
        }

        // creates a div for the given contact inside the contact list
        function createContactDiv(parent, contactName) {
            var contactDiv = document.createElement('div');
            contactDiv.className = 'contact';
            contactDiv.id = contactName;
            var nameDiv = document.createElement('div');
            nameDiv.className = 'name';
            nameDiv.innerHTML = contactName;

            contactDiv.appendChild(nameDiv);
            parent.appendChild(contactDiv);
        }

        // retrieve all of the messages associated with the current chat id from the backend
        function getMessages() {
            // if there is no active chat, we don't need to get messages from anyone
            if (currentChatID == "") {
                return;
            }

            let http = new XMLHttpRequest();
            let reqURL = GET_MSG_URL + currentChatID;
            http.open('GET', reqURL);
            http.send();

            http.onreadystatechange = (e) => {
                if (http.readyState === 4 && http.responseText != "") {
                    $('#chat-window').empty();
                    allChats[currentChatID] = [];
                    let raw_msgs = http.responseText.split(":")
                    for (let raw_msg of raw_msgs) {
                        let elem = raw_msg.split("&");
                        let source = "them";
                        if (elem[0] !== currentChatID) {
                            source = "me";
                        }
                        let msg = {text: elem[1], who: source};
                        allChats[currentChatID].push(msg);
                        talk(msg);
                    }
                }
            }
        }

        // switches contacts when a new contact is clicked
        function contactHandler(event) {
            $('.contact').removeClass('selected');
            $(event.target).parent().addClass('selected');

            $('#chat-text').removeAttr('disabled');
            $('#chat-window').empty();

            currentChatID = event.target.innerHTML.trim().toLowerCase().replace(' ', '_');
            if (!(currentChatID in allChats)) {
                allChats[currentChatID] = [];
            }
            for (let msg of allChats[currentChatID]) {
                talk(msg);
            }
        }

        // attach handler to insert suggestions on click
        $('.suggestion').on('click', (event) => {
            let sug = event.target.innerText;
            $('#chat-text').val((i, text) => {
                return text + sug;
            });
            $('#chat-text').focus()
        });

        // handler for text area operations; handles submitting messages along with
        // updating corrections, suggestions, and autocompletions
        $('#chat-text').on('keypress', function(event) {
            if (event.key == 'Enter') {
                let text = event.target.value;
                if (text.length == 0) {
                    return false;
                }
                let msg = {text: text, who: 'me'};
                talk(msg);
                allChats[currentChatID].push(msg);
                sendMessage();

                event.target.value = '';
                return false;
            }
            else if (event.key == ' ') {
                let text = event.target.value;
                if (event.target.value.length == 0) {
                    return false;
                }
                updateSuggestions(event)
                spellCheck(text, event)
            }
            else {
                autoComplete(event.target.value + event.key, event)
            }
        });

        // register the contact handler
        // this is split out into a different function because we must reattach the contact handler after refreshing
        // contacts
        $('.contact').on('click', contactHandler);

        // retrieve messages from the backend every second
        setInterval(getMessages, 1000)
        // retrieve the contact list from the backend every second
        setInterval(getContacts, 1000);
    });
})();
