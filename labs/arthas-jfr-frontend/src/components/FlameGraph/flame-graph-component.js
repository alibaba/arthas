
// 将template暴露到全局作用域
window.flameGraphTemplate = document.createElement('template');
const template = window.flameGraphTemplate;

  template.innerHTML = `
        <style>
            .none-pinter-events {
                pointer-events: none;
            }
            
            #flame-graph {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                font-family: Menlo, NotoSans, 'Lucida Grande', 'Lucida Sans Unicode', sans-serif;
                line-height: normal;
                
                display: flex;
                flex-direction: row-reverse;
                overflow-y: visible;
            }
            
            #flame-graph-inner {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                
                flex-grow: 1;
            }
            
            #flame-graph-inner-wrapper {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
            }

            #flame-graph-canvas {
                display: block;
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
            }

            #pinned-frame-mask, #frame-mask {
                position: absolute;
                top: 0;
                left: 0;
                width: 0;
                height: 0;
                outline: 2px black solid ;
                outline-offset: -2px;
                visibility: hidden;
            }

            #frame-mask-text {
                position: absolute;
                left: 0;
                top: 0;
            }

            #frame-postcard-wrapper{
                position: fixed;
                top: 0;
                left: 0;
                z-index: 5
            }

            #frame-postcard {
                position: absolute;
                top: 0;
                left: 0;
                visibility: hidden;
            }

            #frame-postcard-starting-pointer {
                position: absolute;
                width: 6px;
                height: 6px;
                background-color: rgba(55, 59, 70, .8);
                border-radius: 100%;
                display: block;
                transform: translate3d(-3px, -3px, 0);
            }

            #frame-postcard-connecting-line {
                position: absolute;
                width: 40px;
                height: 1px;
                background-color: rgba(55, 59, 70, .6);
                background-size: 100% 100%;
                display: block;
                transform-origin: 0 0;
            }

            #frame-postcard-content {
                position: absolute;
            }

            #frame-postcard-content-main {
                position: relative;
                width: fit-content;
                max-width: 338px;
                padding: 8px 8px 8px 20px;
                box-shadow: 0 0 1px rgba(0, 0, 0, .1), 0 2px 5px;
                border-radius: 6px;
            }

            #frame-postcard-content-main-title {
                font-weight: 700;
                font-size: 14px;
                word-wrap: break-word;
            }

            #frame-postcard-content-foot {
                width: 350px;
                line-height: 23px;
                padding: 8px;
                font-size: 14px;
                margin-top: 2px;
                border-radius: 6px;
            }
            
            #frame-postcard-shadow {
                position: absolute;
                top: 0;
                left: 0;
            }
            
            .keyboard {
                background-color: rgb(243, 243, 243);
                color: rgb(33, 33, 33);
                padding: 1px 4px 1px 4px;
                border-radius: 3px;
                border: solid 1px #ccc;
                border-bottom-color: #bbb;
                box-shadow: inset 0 -1px 0 #bbb;
            }
            
            #help-button {
                position: absolute;
                top: 0;
                left: 0;
                color: rgba(0, 0, 0, .6);
                font-size: 24px;
                cursor: pointer;
                visibility: hidden;
                width: 15px;
                height:24px;
            }
            
            #help-button:hover {
                background: rgba(0, 0, 0, .1);
            }
            
            #color-bar-wrapper {
                background: linear-gradient(to top, rgba(255, 0, 0, .75), rgba(0, 0, 0, .75) 50%, rgba(0, 128, 0, .75));
                width: 40px;
                height: 95%;
                display: flex;
                justify-content: space-around;
                padding: 3px;
                
                position: relative;
            }
            
            #color-bar {
                background: linear-gradient(to top, green, grey 50%, red);
                opacity: 1;
                width: 100%;
                height: 100%;
                display: flex;
                flex-direction: column;
                align-items: center;
                
                position: relative;
            }
            
            .color-bar-percent {
                display: flex;
                flex-direction: row;
                font-size: 12px;
                color: rgba(255, 255, 255, 1);
                font-weight: bold;
            }
            
            .color-bar-percent-positive {
                height: 12%;
                align-items: start;
                padding-top: 8px;
            }
            
            .color-bar-percent-negative {
                height: 12%;
                align-items: end;
                padding-bottom: 8px;
            }
            
            .color-bar-percent-0 {
                height: 4%;
                align-items: center;
            }
            
            #color-arrow {
                position: absolute;
                top: 50%;
                left: 44px;
                height: 8px;
                width: 8px;
                display: inline-block;
                background: linear-gradient(to top left, rgba(255, 255, 255, 0) 50%, rgba(55, 59, 70, .9) 50%, rgba(55, 59, 70, .9));
                transform-origin: top left;
                transform: rotate(-45deg);
                visibility: hidden;
            }
        </style>
            
        <div id="flame-graph">
            <div id='color-bar-div' style="width: 58px; flex-shrink: 0; display: none; align-items: center;">
                <div id='color-bar-wrapper'> 
                    <div id='color-bar'> 
                        <div class="color-bar-percent color-bar-percent-positive">+100%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+75%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+50%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+25%</div>
                        <div class="color-bar-percent color-bar-percent-0">±0%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-25%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-50%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-75%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-100%</div>
                        <div id="color-arrow"></div>
                    </div>
                </div>
            </div>
            
            <div id="flame-graph-inner">
                <div id="flame-graph-inner-wrapper">
                    <canvas id="flame-graph-canvas"/>
                </div>

                <div id="pinned-frame-mask" class="none-pinter-events"></div>

                <div id="frame-mask" tabindex="-1">
                    <div id="frame-mask-text"></div>
                </div>
            </div>
            
            <div id="frame-postcard-wrapper">
                <div id="frame-postcard" class="none-pinter-events">
                    <div id="frame-postcard-starting-pointer" class="none-pinter-events"></div>

                    <div id="frame-postcard-connecting-line" class="none-pinter-events"></div>

                    <div id="frame-postcard-content" class="none-pinter-events">
                        <div id="frame-postcard-content-main" class="none-pinter-events">
                            <div id="frame-postcard-content-main-line"
                                 style="position: absolute; left: 9px; top: 10px; bottom: 10px; width: 3px;
                                        border-radius: 6px;"
                                 class="none-pinter-events">
                            </div>
                            <span id="frame-postcard-content-main-title"></span>
                        </div>
                        <div id="frame-postcard-content-foot" class="none-pinter-events"></div>
                    </div>
                </div>
            </div>
            
            <div id="frame-postcard-shadow"></div>
            
            <div id="help-button">
                <svg style="margin-left: -4.5px" focusable="false" width="1em" height="1em" fill="currentColor" aria-hidden="true" viewBox="64 64 896 896">
                    <path d="M456 231a56 56 0 10112 0 56 56 0 10-112 0zm0 280a56 56 0 10112 0 56 56 0 10-112 0zm0 280a56 56 0 10112 0 56 56 0 10-112 0z"></path>
                </svg>
            </div>
            
            <div id="flame-graph-help"
              style="position: absolute;
                     width: 450px;
                     top: 36px; left: 50%;
                     margin-left: -240px;
                     background-color: rgba(0,0,0,0.8);color: white;
                     border-radius: 6px;
                     padding: 15px;
                     z-index: 9999;
                     visibility: hidden;">
              <div style="display: flex; justify-content: space-between; padding: 0 2px; font-size: 14px">
                <span>Flame Graph Help</span>
                <span id="close-flame-graph-help" style="cursor: pointer">x</span>
              </div>
              
              <div style="display: block; height: 1px; width: 100%; background-color: #9a9a9a; margin: 15px 0"></div>
              
              <div style="font-size: 12px">
                <div style="display: flex;">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                    <span class="keyboard">^c</span>, <span class="keyboard">⌘c</span>, <span class="keyboard">ff</span>
                  </div>
                  <div style="width: 70%;">Copy the content of the touched frame</div>
                </div>
                
                <div style="display: flex; padding-top: 15px">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                    <span class="keyboard">fs</span>
                  </div>
                  <div style="width: 70%;">Copy the stack trace from the touched frame</div>
                </div>
              
                <div style="display: flex; padding-top: 15px">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                  Downward
                  </div>
                  <div style="width: 70%;">
                      <div id="downward-button" style="width: 28px; height: 16px; border-radius: 100px; cursor: pointer; display: flex; align-items: center">
                          <div style="width: 12px; height: 12px; border-radius: 100%; background: white; margin: 0 2px"></div>
                      </div>
                  </div>
                </div>
              <div/>
            </div>
        </div>
    `;

  // 导入Frame和FlameGraph类
  if (typeof Frame === 'undefined') {
    console.error('Frame class not found. Please include flame-graph-core.js first.');
  }
  if (typeof FlameGraph === 'undefined') {
    console.error('FlameGraph class not found. Please include flame-graph-class.js first.');
  }

  // 注册Web Component
  if (typeof FlameGraph !== 'undefined') {
    window.customElements.define('flame-graph', FlameGraph);
  }
