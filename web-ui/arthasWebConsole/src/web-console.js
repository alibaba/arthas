<<<<<<< HEAD
// import "bootstrap";
=======
import "bootstrap";
>>>>>>> master
import 'bootstrap/dist/css/bootstrap.min.css';
import $ from "jquery"
import "xterm/css/xterm.css"
import { Terminal } from "xterm"
var ws;
var xterm =new Terminal()
const DEFAULT_SCROLL_BACK = 1000
const MAX_SCROLL_BACK = 9999999
const MIN_SCROLL_BACK = 1

$(function () {
    var url = window.location.href;
    var ip = getUrlParam('ip');
    var port = getUrlParam('port');

    if (ip != '' && ip != null) {
        $('#ip').val(ip);
    } else {
        $('#ip').val(window.location.hostname);
    }
    if (port != '' && port != null) {
        $('#port').val(port);
    }
    if (port == null && location.port == "8563") {
        $('#port').val(8563);
    }

    var iframe = getUrlParam('iframe');
    if (iframe != null && iframe != 'false') {
        $("nav").hide()
    }

    startConnect(true);
});

/** get params in url **/
function getUrlParam(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

function getCharSize() {
    var tempDiv = $('<div />').attr({ 'role': 'listitem' });
    var tempSpan = $('<div />').html('qwertyuiopasdfghjklzxcvbnm');
    tempDiv.append(tempSpan);
    $("html body").append(tempDiv);
    var size = {
        width: tempSpan.outerWidth() / 26,
        height: tempSpan.outerHeight(),
        left: tempDiv.outerWidth() - tempSpan.outerWidth(),
        top: tempDiv.outerHeight() - tempSpan.outerHeight(),
    };
    tempDiv.remove();
    return size;
}

function getWindowSize() {
    var e = window;
    var a = 'inner';
    if (!('innerWidth' in window)) {
        a = 'client';
        e = document.documentElement || document.body;
    }
    var terminalDiv = document.getElementById("terminal-card");
    var terminalDivRect = terminalDiv.getBoundingClientRect();
    return {
        width: terminalDivRect.width,
        height: e[a + 'Height'] - terminalDivRect.top
    };
}

function getTerminalSize() {
    var charSize = getCharSize();
    var windowSize = getWindowSize();
    console.log('charsize');
    console.log(charSize);
    console.log('windowSize');
    console.log(windowSize);
    return {
        cols: Math.floor((windowSize.width - charSize.left) / 10),
        rows: Math.floor((windowSize.height - charSize.top) / 17)
    };
}

/** init websocket **/
function initWs(ip, port) {
    var path = 'ws://' + ip + ':' + port + '/ws';
    ws = new WebSocket(path);
}

/** init xterm **/
function initXterm(cols, rows, scrollback) {
    let scrollNumber = parseInt(scrollback, 10)
    xterm = new Terminal({
        cols: cols,
        rows: rows,
        screenReaderMode: false,
        rendererType: 'canvas',
        convertEol: true,
        scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK
    });
}

function isValidNumber(scrollNumber) {
    return scrollNumber >= MIN_SCROLL_BACK &&
        scrollNumber <= MAX_SCROLL_BACK;
}

/** begin connect **/
window.startConnect =  function startConnect(silent) {
    var ip = $('#ip').val();
    var port = $('#port').val();
    if (ip == '' || port == '') {
        alert('Ip or port can not be empty');
        return;
    }
    if (ws != null) {
        alert('Already connected');
        return;
    }
    // init webSocket
    initWs(ip, port);
    ws.onerror = function () {
        ws.close();
        ws = null;
        !silent && alert('Connect error');
    };
    ws.onopen = function () {
        console.log('open');
        $('#fullSc').show();
        var terminalSize = getTerminalSize()
        let scrollback = getUrlParam('scrollback');
        console.log('terminalSize')
        console.log(terminalSize)
        // init xterm
        initXterm(terminalSize.cols, terminalSize.rows, scrollback)
        ws.onmessage = function (event) {
            if (event.type === 'message') {
                var data = event.data;
                xterm.write(data);
            }
        };
        xterm.open(document.getElementById('terminal'));
        console.log(xterm)
        // xterm = new Terminal()
        xterm.onData(function (data) {
            ws.send(JSON.stringify({ action: 'read', data: data }))
        });
        ws.send(JSON.stringify({ action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows }));
        window.setInterval(function () {
            if (ws != null && ws.readyState === 1) {
                ws.send(JSON.stringify({ action: 'read', data: "" }));
            }
        }, 30000);
    }
}

window.disconnect =  function disconnect() {
    try {
        ws.close();
        ws.onmessage = null;
        ws.onclose = null;
        ws = null;
        xterm.dispose();
        $('#fullSc').hide();
        alert('Connection was closed successfully!');
    } catch (e) {
        alert('No connection, please start connect first.');
    }
}

/** full screen show **/
window.xtermFullScreen = function xtermFullScreen() {
    var ele = document.getElementById('terminal-card');
    requestFullScreen(ele);
}

function requestFullScreen(element) {
    var requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
    if (requestMethod) {
        requestMethod.call(element);
    } else if (typeof window.ActiveXObject !== "undefined") {
        var wscript = new ActiveXObject("WScript.Shell");
        if (wscript !== null) {
            wscript.SendKeys("{F11}");
        }
    }
}

window.addEventListener('resize', function () {
    if (ws !== undefined && ws !== null) {
        let terminalSize = getTerminalSize();
        ws.send(JSON.stringify({ action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows }));
        xterm.resize(terminalSize.cols, terminalSize.rows);
    }
});
