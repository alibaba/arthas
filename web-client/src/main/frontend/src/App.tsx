import React from 'react';
import Header from './components/Header';
import Term from './components/Term';
import { Terminal } from 'xterm';
import { createTerminal } from './utils/ArthasUtils';

const wsPath = "ws://localhost:3658/ws";

interface ITermContext {
  terminal: Terminal;
  webSocket: WebSocket
}

const context = createTerminal(wsPath, false);

const TermContext = React.createContext(context);

function App() {
  return (
    <div>
      <TermContext.Provider value={context}>
        <Header />
        <Term />
      </TermContext.Provider>

    </div>
  );
}

export { App, TermContext };
export type { ITermContext }