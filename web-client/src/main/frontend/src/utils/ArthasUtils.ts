import { ITermContext } from "../App";
import { FitAddon } from 'xterm-addon-fit';
import { AttachAddon } from 'xterm-addon-attach';
import { Terminal } from "xterm";

const openInNewTab = (url: string) => {
    const newWindow = window.open(url, '_blank', 'noopener,noreferrer')
    if (newWindow) {
        newWindow.opener = null;
    }
}

const getUrlParam = (name: string): string | null => {
    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    return urlParams.get(name);
}

const fullScreen = (ele: any) => {
    const rfs = ele.requestFullscreen || ele.webkitRequestFullscreen ||
        ele.mozRequestFullScreen || ele.msRequestFullScreen;
    if (typeof rfs != "undefined" && rfs) {
        rfs.call(ele);
    } else if (typeof window.ActiveXObject !== "undefined") {
        var wscript = new ActiveXObject("WScript.Shell");
        if (wscript !== null) {
            wscript.SendKeys("{F11}");
        }
    }
}


const createTerminal = (wsPath: string, init: boolean = true): ITermContext => {
    const webSocket = new WebSocket(wsPath);

    const terminal = new Terminal({
        rendererType: 'canvas', // 渲染类型
        convertEol: true,
        cursorBlink: true, // 光标闪烁
        cursorStyle: 'block', // 光标样式
        screenReaderMode: true,
        windowOptions: {
            fullscreenWin: true
        }
    });

    if (init) {
        initTerminal(terminal, webSocket);
    }
    return {
        terminal, webSocket
    }
}

const initTerminal = (terminal: Terminal, webSocket: WebSocket, div: string = "terminal") => {
    const fitAddon = new FitAddon();
    const attachAddon = new AttachAddon(webSocket);
    terminal.loadAddon(fitAddon);
    terminal.loadAddon(attachAddon);
    terminal.open(document.getElementById(div)!);
    fitAddon.fit();
    terminal.onData(
        data => {
            webSocket.send(JSON.stringify({ action: 'read', data: data }));
        }
    );
}

export { openInNewTab, getUrlParam, fullScreen, createTerminal, initTerminal }