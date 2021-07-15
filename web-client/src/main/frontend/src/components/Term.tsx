import React, { useEffect } from 'react';
import { TermContext } from '../App';
import 'xterm/css/xterm.css';
import { fullScreen, initTerminal } from '../utils/ArthasUtils';

function Term() {
    const { terminal, webSocket } = React.useContext(TermContext);

    useEffect(() => {
        initTerminal(terminal, webSocket);
    });
    return (
        <div>
            <button
                className="fixed top-40 z-50 right-20"
                onClick={
                    () => {
                        fullScreen(document.getElementById("terminal"))
                    }
                }>
                <img
                    src="fullsc.png" />
            </button>

            <div id="terminal" className="h-screen"></div>
        </div>
    );
}
export default Term;


