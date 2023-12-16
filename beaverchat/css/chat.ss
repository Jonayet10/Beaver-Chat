body {
    font-family: "Georgia";
    font-size: 14px;
    overflow: hidden;
    margin: 0;
    height:100%;
    background: rgba(236, 236, 236, 1);
}

div {
    word-wrap: break-word;
}

.from-me {
    position: relative;
    padding: 2px 5px;
    margin-bottom: 5px;
    color: white;
    background: #0B93F6;
    border-radius: 5px;
    max-width: 80%;
    float: right;
    font-size: 14px;
}

.from-them {
    position: relative;
    padding: 2px 5px;
    margin-bottom: 5px;
    background: #E5E5EA;
    border-radius: 5px;
    color: black;
    float: left;
    max-width: 80%;
    font-size: 14px;
}

#chat-window {
    background: #FFFFFF;
    padding: 10px;
    position: fixed;
    top: 0;
    right: 0;
    width: 80%;
    height: 85%;
    overflow-y: scroll;
    padding-bottom: 30px;
    margin: 0px;
    margin-right: 10px;
}

#contact-list {
    height: 100%;
    margin: 0px;
    padding: 0px;
    background: rgba(236, 236, 236, 1);
    position: absolute;
    top: 0px;
    bottom: 0;
    left: 0;
    width: 20%;
    overflow-y: scroll;
}

.contact.selected {
    cursor: default;
    background: rgba(3, 72, 102, 0.37);
    color: #FFFFFF;
    width: 100%;
}

.contact {
    cursor: pointer;
    display: table;
    width: 100%;
    height: 50px;
    text-align: center;
}
.contact .name {
    display: table-cell;
    font-weight: bold;
    vertical-align: middle;
}

#chat-text-container {
    background: rgba(236, 236, 236, 1);
    position: absolute;
    bottom: 0px;
    margin: 0px;
    padding-bottom: 10px;
    right: 0;
    height: 10%;
    width: 80%;
    z-index: 0;
    min-height: 40px;
    margin-left: 10px;
    margin-right: 10px;
}
#chat-text, #chat-text:active {
    outline: none;
    display: inline-block;
    border-radius: 5px;
    border: 1px solid #EEE;
    width: 100%;
    height: 100%;
}

#word-suggestion-container {
    background: rgba(236, 236, 236, 1);
    position: absolute;
    bottom: 10%;
    margin: 0px;
    margin-right: 10px;
    padding-top: 8px;
    padding-bottom: 8px;
    right: 0;
    height: 20px;
    width: 80%;
    z-index: 0;
    min-height: 40px;
    display: flex;
    justify-content:space-between;
}

.suggestion {
    padding  : .35rem .4rem;
    font-size  : .875rem;
    line-height  : .5;
    border-radius : .2rem;
}
