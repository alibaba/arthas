let ws;
let xterm;

$(function () {
    const url = window.location.href;
    const ip = getUrlParam('ip');
    const port = getUrlParam('port');
    const agentId = getUrlParam('agentId');

    if (ip !== '' && ip != null) {
        $('#ip').val(ip);
    } else {
        $('#ip').val(window.location.hostname);
    }
    if (port !== '' && port != null) {
        $('#port').val(port);
    }
    if (agentId !== '' && agentId != null) {
        $('#selectAgent').val(agentId);
    }

    // startConnect(true);
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
    let tempDiv = $('<div />').attr({'role': 'listitem'});
    let tempSpan = $('<div />').html('qwertyuiopasdfghjklzxcvbnm');
    tempDiv.append(tempSpan);
    $("html body").append(tempDiv);
    const size = {
        width: tempSpan.outerWidth() / 26,
        height: tempSpan.outerHeight(),
        left: tempDiv.outerWidth() - tempSpan.outerWidth(),
        top: tempDiv.outerHeight() - tempSpan.outerHeight(),
    };
    tempDiv.remove();
    return size;
}

function getWindowSize() {
    let e = window;
    let a = 'inner';
    if (!('innerWidth' in window)) {
        a = 'client';
        e = document.documentElement || document.body;
    }
    const terminalDiv = document.getElementById("terminal-card");
    const terminalDivRect = terminalDiv.getBoundingClientRect();
    return {
        width: terminalDivRect.width,
        height: e[a + 'Height'] - terminalDivRect.top
    };
}

function getTerminalSize() {
    const charSize = getCharSize();
    const windowSize = getWindowSize();
    return {
        cols: Math.floor((windowSize.width - charSize.left) / 10),
        rows: Math.floor((windowSize.height - charSize.top) / 17)
    };
}

/** init websocket **/
function initWs(ip, port, agentId) {
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
    let path;
    const proxy = document.getElementById('connectType').checked;
    if (proxy) {
        path = protocol + location.hostname + ':' + location.port + '/arthas/' + agentId + '/ws?method=connectArthas&id=' + agentId;
    } else {
        path = protocol + ip + ':' + port + '/ws?method=connectArthas&id=' + agentId;
    }
    ws = new WebSocket(path);
}

function updateArthasOutputLink() {
    $('#arthasOutputA').prop("href", "proxy/" + $('#selectAgent').val() + "/arthas-output/")
}

/** init xterm **/
function initXterm(cols, rows) {
    xterm = new Terminal({
        cols: cols,
        rows: rows,
        screenReaderMode: true,
        rendererType: 'canvas',
        convertEol: true
    });
}


/** 有修改 begin connect **/
function connectServer(silent) {
    const ip = $('#ip').val();
    const port = $('#port').val();
    const agentId = $('#selectAgent').val();
    if (ip === '' || port === '') {
        alert('Ip or port can not be empty');
        return;
    }
    if (agentId === '') {
        if (silent) {
            return;
        }
        alert('AgentId can not be empty');
        return;
    }
    if (ws != null) {
        disconnectServer();
    }

    // init webSocket
    initWs(ip, port, agentId);
    ws.onerror = function () {
        ws.close();
        ws = null;
        !silent && alert('Connect error');
    };
    ws.onclose = function (message) {
        if (message.code === 2000) {
            alert(message.reason);
        }
    };
    ws.onopen = function () {
        $('#disconnect').show();
        $('#connect').hide();
        $('#fullSc').show();
        const terminalSize = getTerminalSize();
        // init xterm
        initXterm(terminalSize.cols, terminalSize.rows)
        ws.onmessage = function (event) {
            if (event.type === 'message') {
                const data = event.data;
                xterm.write(data);
            }
        };
        xterm.open(document.getElementById('terminal'));
        xterm.on('data', function (data) {
            ws.send(JSON.stringify({action: 'read', data: data}))
        });
        ws.send(JSON.stringify({action: 'resize', cols: terminalSize.cols, rows: terminalSize.rows}));
        window.setInterval(function () {
            if (ws != null && ws.readyState === 1) {
                ws.send(JSON.stringify({action: 'read', data: ""}));
            }
        }, 30000);
    }
}

function disconnectServer() {
    try {
        ws.close();
        ws.onmessage = null;
        ws.onclose = null;
        ws = null;
        xterm.destroy();
        $('#fullSc').hide();
        $('#disconnect').hide();
        $('#connect').show();
        // alert('Connection was closed successfully!');
    } catch (e) {
        alert('No connection, please start connect first.');
    }
}

/** full screen show **/
function xtermFullScreen() {
    const ele = document.getElementById('terminal-card');
    requestFullScreen(ele);
}

function requestFullScreen(element) {
    const requestMethod = element.requestFullScreen || element.webkitRequestFullScreen || element.mozRequestFullScreen || element.msRequestFullScreen;
    if (requestMethod) {
        requestMethod.call(element);
    } else if (typeof window.ActiveXObject !== "undefined") {
        const wscript = new ActiveXObject("WScript.Shell");
        if (wscript !== null) {
            wscript.SendKeys("{F11}");
        }
    }
}
