import React, { MouseEvent, useState, useEffect } from 'react';
import { openInNewTab, getUrlParam, createTerminal } from '../utils/ArthasUtils';
import { TermContext, ITermContext } from '../App';

interface HeaderItem {
    name: string;
    link: string;
}

interface InputItem {
    key: string;
    label: string;
    length: Number;
    defaultValue: () => string | null
}

interface ButtonItem {
    name: string;
}

interface Input {
    ip: string;
    port: string;
}

const headers: Array<HeaderItem> = [
    {
        name: "Documentation",
        link: "https://arthas.aliyun.com/doc"
    },
    {
        name: "Online Tutorials",
        link: "https://arthas.aliyun.com/doc/arthas-tutorials.html"
    },
    {
        name: "Github",
        link: "https://github.com/alibaba/arthas"
    }
];

const inputs: Array<InputItem> = [
    {
        key: "ip",
        label: "IP",
        length: 40,
        defaultValue: () => {
            return getUrlParam("ip") || window.location.hostname;
        }
    },
    {
        key: "port",
        label: "Port",
        length: 20,
        defaultValue: () => {
            return getUrlParam("port") || window.location.port;
        }
    }
];

const buttons: Array<ButtonItem> = [
    {
        name: "Connect"
    }, {
        name: "Disconnect",
    }, {
        name: "Output",
    }

];

function onClickOutput() {
    const url = window.location.href;
    openInNewTab(`${url}arthas-output`)
}

function Header() {
    const { terminal, webSocket } = React.useContext(TermContext);

    const [inputIp, setInputIp] = useState("");
    const [inputPort, setInputPort] = useState("");

    useEffect(
        () => {
            const inputValue = (document.getElementById("ip") as HTMLInputElement)?.value;
            const portValue = (document.getElementById("port") as HTMLInputElement)?.value;
            setInputIp(inputValue);
            setInputPort(portValue);
        }
    );

    return (
        <header className="flex h-14 bg-gray-100 font-sans">
            <div className="flex flex-1 justify-start">

                <ul className="flex">
                    <li className="flex items-center ml-6">
                        <a href="https://github.com/alibaba/arthas" target="_blank">
                            <img src="logo.png" alt="" className="h-6 w-auto" />
                        </a>
                    </li>

                    {headers.map((value, index) =>
                        <li key={index} className="flex items-center p-3">
                            <a href={value.link} target="_blank">
                                {
                                    index === 0
                                        ? <span className="font-normal">{value.name}</span>
                                        : <span className="font-thin text-gray-500">{value.name}</span>
                                }
                            </a>
                        </li>
                    )}
                </ul>
            </div>

            <div className="flex flex-1 h-full justify-end">
                {
                    inputs.map(
                        (value, index) =>
                            <div key={index} className="flex ml-4 items-center">
                                <span className="border-t border-l border-b rounded-l  bg-gray-200 border-gray-300 pl-3 pr-3 pt-1 pb-1">{value.label}</span>
                                <input type="text" id={value.key}
                                    className={`border pt-1 pb-1 rounded-r border-gray-300 w-${value.length} focus:outline-none focus:ring-2 pl-2`}
                                    defaultValue={value.defaultValue() || ""}
                                    onChange={
                                        event => {
                                            const key = value.key;
                                            switch (key) {
                                                case "ip": {
                                                    setInputIp(event.target.value);
                                                    break;
                                                }
                                                case "port": {
                                                    setInputPort(event.target.value);
                                                    break;
                                                }
                                                default: {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                />

                            </div>
                    )

                }
                <div className="flex items-center pr-3">
                    {
                        buttons.map(
                            (value, index) =>
                                <button
                                    key={index}
                                    className={`border rounded bg-blue-400 text-white pt-1 pb-1 pl-3 pr-3 ml-3 border-blue-500`}
                                    onClick={
                                        (event: MouseEvent) => {
                                            handle(event, { terminal, webSocket }, value, { ip: inputIp, port: inputPort })
                                        }
                                    }
                                >{value.name}</button>

                        )
                    }

                </div>
            </div>
        </header>
    );
}

function handle(event: MouseEvent, context: ITermContext, value: ButtonItem, input: Input) {
    const name = value.name;
    let { terminal, webSocket } = context;

    switch (name) {
        case "Disconnect": {
            try {
                webSocket.close();
                terminal.dispose();
                alert("Connection was closed successfully!");
            } catch (error) {
                console.log(error);
                alert('No connection, please start connect first.');
            }

            break;
        }
        case "Connect": {
            if (webSocket.readyState === WebSocket.OPEN) {
                alert("Already connected");
                return;
            }

            const wsPath = `ws://${input.ip}:${input.port}/ws`;
            createTerminal(wsPath);
            break;
        }
        case "Output": {
            onClickOutput();
            break;
        }
        default: {
            break;
        }
    }
}

export default Header;