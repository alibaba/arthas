// 火焰图主类实现
class FlameGraph extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' });
    let sr = this.shadowRoot;
    // 使用全局template变量
    const template = window.flameGraphTemplate;
    if (template) {
      sr.appendChild(template.content.cloneNode(true));
    } else {
      console.error('FlameGraph template not found. Please include flame-graph-component.js first.');
    }

    this.$canvas = sr.getElementById('flame-graph-canvas');
    this.$context = this.$canvas.getContext('2d');
    this.$context.save();

    this.$frameHeight = 24;
    this.$fgVGap = 0;
    this.$fgVEndGap = 5;
    this.$xGap = 0.2;
    this.$xGapThreashold = 0.01;
    this.$yGap = 0.5;
    this.$textGap = 6;
    this.$showTextWidthThreshold = 30;
    this.$fontFamily = 'Menlo,NotoSans,"Lucida Grande","Lucida Sans Unicode",sans-serif';
    this.$font = '400 12px ' + this.$fontFamily;
    this.$font_600 = '600 12px ' + this.$fontFamily;
    this.$rootFont = '600 14px ' + this.$fontFamily;
    this.$moreText = '...';

    this.$defaultColorScheme = {
      colorForZero: ['#c5c8d3', '#000000'],
      colors: [
        ['#761d96', '#ffffff'],
        ['#c12561', '#ffffff'],
        ['#fec91b', '#000000'],
        ['#3f7350', '#ffffff'],
        ['#408118', '#ffffff'],
        ['#3ea9da', '#000000'],
        ['#9fb036', '#ffffff'],
        ['#b671c1', '#ffffff'],
        ['#faa938', '#000000']
      ]
    };

    this.$maxY = 0;
    this.$flameGraph = sr.getElementById('flame-graph');
    this.$flameGraphInner = sr.getElementById('flame-graph-inner');
    this.$flameGraphInnerWrapper = sr.getElementById('flame-graph-inner-wrapper');
    this.$pinnedFrameMask = sr.getElementById('pinned-frame-mask');
    this.$frameMask = sr.getElementById('frame-mask');
    this.$flameGraphHelp = sr.getElementById('flame-graph-help');
    this.$closeFlameGraphHelp = sr.getElementById('close-flame-graph-help');
    this.$helpButton = sr.getElementById('help-button');

    this.$helpButton.addEventListener('click', () => {
      if (this.$flameGraphHelp.style.visibility !== 'visible') {
        this.$flameGraphHelp.style.visibility = 'visible';
      } else {
        this.$flameGraphHelp.style.visibility = 'hidden';
      }
    });

    this.$flameGraphInner.addEventListener('click', () => {
      if (this.$flameGraphHelp.style.visibility === 'visible') {
        this.$flameGraphHelp.style.visibility = 'hidden';
      }
    });

    this.$closeFlameGraphHelp.addEventListener('click', () => {
      this.$flameGraphHelp.style.visibility = 'hidden';
    });

    this.$commandMode = false;
    this.$frameMask.addEventListener('keydown', (e) => {
      if ((e.key === 'c' || e.key === 'C') && (e.metaKey || e.ctrlKey)) {
        this.copy(false);
        this.$commandMode = false;
      } else {
        if (this.$commandMode) {
          if (e.key === 'f' || e.key === 'F') {
            this.copy(false);
          } else if (e.key === 's' || e.key === 'S') {
            this.copy(true);
          }
          this.$commandMode = false;
        } else if (e.key === 'f' || e.key === 'F') {
          this.$commandMode = true;
        }
      }
    });

    this.$frameMaskText = sr.getElementById('frame-mask-text');
    this.$framePostcard = sr.getElementById('frame-postcard');
    this.$framePostcardShadow = sr.getElementById('frame-postcard-shadow');
    this.$framePostcardConnectingLine = sr.getElementById('frame-postcard-connecting-line');
    this.$framePostcardContent = sr.getElementById('frame-postcard-content');
    this.$framePostcardContentMain = sr.getElementById('frame-postcard-content-main');
    this.$framePostcardContentMainLine = sr.getElementById('frame-postcard-content-main-line');
    this.$framePostcardContentMainTitle = sr.getElementById('frame-postcard-content-main-title');
    this.$framePostcardContentFoot = sr.getElementById('frame-postcard-content-foot');

    this.$frameMask.style.font = this.$font_600;
    this.$root = null;
    this.$currentFrame = null;
    this.$pinned = false;
    this.$pinnedFrame = null;
    this.$pinnedFrameLeft = null;
    this.$pinnedFrameRight = null;
    this.$drawingChildrenOfPinnedFrame = false;

    this.$frameMask.addEventListener('mousemove', (e) => {
      this.handleFrameMaskMouseMoveEvent(e);
    });
    this.$frameMask.addEventListener('click', (e) => {
      this.handleFrameMaskClickEvent(e);
    });
    this.$frameMask.addEventListener('dblclick', (e) => {
      if (window.getSelection) {
        window.getSelection().removeAllRanges();
      }
      this.handleFrameMaskClickEvent(e);
    });

    this.$scrollEventListener = () => {
      this.handleScrollEvent();
    };

    this.$flameGraphInner.addEventListener('scroll', this.$scrollEventListener);
    window.addEventListener('scroll', this.$scrollEventListener);

    this.$downwardBunnton = sr.getElementById('downward-button');
    this.$downwardBunnton.addEventListener('click', () => (this.downward = !this.downward));

    this.$root = new Frame(this, null, 0, true);
    this.$touchedFrame = null;

    this.$canvas.addEventListener('mousemove', (e) => {
      this.handleCanvasMouseMoveEvent(e);
    });

    this.$flameGraph.addEventListener('mouseleave', () => {
      if (this.$touchedFrame) {
        let tf = this.$touchedFrame;
        this.$touchedFrame = null;
        tf.leave();
      }
    });

    this.$totalWeight = 0;

    let o = this;
    new ResizeObserver(function () {
      o.render(true, false);
    }).observe(this.$flameGraph);

    this.$colorBarDiv = sr.getElementById('color-bar-div');
    this.$colorArrow = sr.getElementById('color-arrow');
  }

  handleScrollEvent() {
    this.$currentFrame = null;
    this.$touchedFrame = null;
    this.$frameMask.style.cursor = 'default';
    this.$frameMask.style.visibility = 'hidden';
    this.$framePostcard.style.visibility = 'hidden';

    if (this.$stackTraceMaxDrawnDepth < this.$stackTraceMaxDepth) {
      if (this.downward) {
        if (this.$flameGraphInner.scrollTop > this.$currentScrollTopLimit) {
          this.$flameGraphInner.scrollTop = this.$currentScrollTopLimit;
        }
      } else {
        if (this.$flameGraphInner.scrollTop < this.$currentScrollTopLimit) {
          this.$flameGraphInner.scrollTop = this.$currentScrollTopLimit;
        }
      }
    }
  }

  handleFrameMaskMouseMoveEvent(e) {
    this.$framePostcardShadow.style.left = this.$frameMask.offsetLeft + e.offsetX + 'px';
    this.decideFramePostcardLayout();
    e.stopPropagation();
  }

  handleFrameMaskClickEvent(e) {
    if (window.getSelection().type === 'Range') {
      e.stopPropagation();
      return;
    }

    if (this.$currentFrame === this.$root) {
      return;
    }

    if (!this.$pinned) {
      this.$pinned = true;
      this.$pinnedFrame = this.$currentFrame;
      this.$pinnedFrame.setPinned();
      this.$pinnedFrame.findSide();
    } else {
      if (this.$pinnedFrame === this.$currentFrame) {
        this.$pinnedFrame.setUnpinned();
        this.$pinnedFrame.clearFindSide();
        this.$pinnedFrame = null;
        this.$pinnedFrameMask.style.visibility = 'hidden';
        this.$pinned = false;
      } else {
        this.$pinnedFrame.setUnpinned();
        this.$pinnedFrame.clearFindSide();
        this.$pinnedFrame = this.$currentFrame;
        this.$pinnedFrame.setPinned();
        this.$pinnedFrame.findSide();
      }
    }

    this.render(false, false);

    if (this.$pinned && this.$pinnedFrame) {
      this.$pinnedFrameMask.style.left = this.$pinnedFrame.x + 'px';
      this.$pinnedFrameMask.style.top = this.$pinnedFrame.y + 'px';
      this.$pinnedFrameMask.style.width = this.$pinnedFrame.width + 'px';
      this.$pinnedFrameMask.style.height = this.$pinnedFrame.height + 'px';
      this.$pinnedFrameMask.style.visibility = 'visible';
    }

    let ne = new Event('mousemove');
    ne.offsetX = this.$frameMask.offsetLeft + e.offsetX;
    ne.offsetY = this.$frameMask.offsetTop + e.offsetY;

    this.$framePostcard.style.visibility = 'hidden';
    this.$frameMask.style.cursor = 'default';
    this.$frameMask.style.visibility = 'hidden';

    this.$canvas.dispatchEvent(ne);
    e.stopPropagation();
  }

  render(reInitRenderContext, reGenFrames) {
    if (this.$dataSource) {
      this.$$isLineFormat = this.$dataSource.format.toLowerCase() === 'line';
      this.$$diff = !!this.$dataSource.diff;
      this.$$dataExtractor = this.dataExtractor;
      this.$$stackTracesCounter = this.stackTracesCounter;
      this.$$stackTraceExtractor = this.stackTraceExtractor;
      this.$$framesCounter = this.framesCounter;
      this.$$frameExtractor = this.frameExtractor;
      this.$$framesIndexer = this.framesIndexer;
      this.$$stackTraceFilter = this.stackTraceFilter;
      this.$$frameEquator = this.frameEquator;
      this.$$reverse = this.reverse;
      this.$$rootFramesCounter = this.rootFramesCounter;
      this.$$rootFrameExtractor = this.rootFrameExtractor;
      this.$$childFramesCounter = this.childFramesCounter;
      this.$$childFrameExtractor = this.childFrameExtractor;
      this.$$frameStepper = this.frameStepper;
      this.$$childFramesIndexer = this.childFramesIndexer;
      this.$$weightsExtractor = this.weightsExtractor;
      this.$$rootTextGenerator = this.rootTextGenerator;
      this.$$textGenerator = this.textGenerator;
      this.$$titleGenerator = this.titleGenerator;
      this.$$detailsGenerator = this.detailsGenerator;
      this.$$footTextGenerator = this.footTextGenerator;
      this.$$rootColorSelector = this.rootColorSelector;
      this.$$colorSelector = this.colorSelector;
      this.$$footColorSelector = this.footColorSelector;
      this.$$hashCodeGenerator = this.hashCodeGenerator;
      this.$$showHelpButton = this.showHelpButton;
    }

    if (reInitRenderContext) {
      this.clearState();
    }

    this.clearCanvas();

    if (reGenFrames) {
      this.genFrames();
    }

    if (reInitRenderContext) {
      this.initRenderContext();
    }

    if (this.$totalWeight === 0) {
      this.$helpButton.style.visibility = 'hidden';
      this.$sibling = null;
      return;
    }

    if (this.$$diff) {
      this.$colorBarDiv.style.display = 'flex';
    }

    if (this.$$showHelpButton) {
      this.$helpButton.style.visibility = 'visible';
      this.$fgHGap = 15;
    } else {
      this.$helpButton.style.visibility = 'hidden';
      this.$fgHGap = 0;
    }

    let rect = this.$canvas.getBoundingClientRect();
    this.$stackTraceMaxDrawnDepth = 0;
    this.$sibling = Array(this.$stackTraceMaxDepth + 1);
    for (let i = 0; i < this.$stackTraceMaxDepth + 1; i++) {
      this.$sibling[i] = [];
    }
    if (this.downward) {
      this.$root.draw(
        this.$fgHGap,
        this.$fgVGap,
        rect.width - this.$fgHGap * 2,
        this.$frameHeight
      );
    } else {
      this.$root.draw(
        this.$fgHGap,
        rect.height - this.$fgVGap - this.$frameHeight,
        rect.width - this.$fgHGap * 2,
        this.$frameHeight
      );
    }

    if (this.$stackTraceMaxDrawnDepth < this.$stackTraceMaxDepth) {
      let height = (this.$stackTraceMaxDrawnDepth + 1) * this.$frameHeight;
      if (this.$stackTraceMaxDrawnDepth > 0) {
        height += this.$stackTraceMaxDrawnDepth * this.$yGap;
      }
      height += this.$fgVGap + this.$fgVEndGap;

      if (this.downward) {
        this.$currentScrollTopLimit = Math.max(
          height - this.$flameGraphInner.getBoundingClientRect().height,
          this.$flameGraphInner.scrollTop
        );
      } else {
        this.$currentScrollTopLimit = Math.min(
          this.$flameGraphHeight - height,
          this.$flameGraphInner.scrollTop
        );
      }
    }
  }

  clearState() {
    this.$pinnedFrame = null;
    this.$currentFrame = null;
    this.$touchedFrame = null;
    this.$pinnedFrameMask.style.visibility = 'hidden';
    this.$frameMask.style.cursor = 'default';
    this.$frameMask.style.visibility = 'hidden';
    this.$framePostcard.style.visibility = 'hidden';
    this.$pinned = false;
  }

  clearCanvas() {
    this.$context.clearRect(0, 0, this.$canvas.width, this.$canvas.height);
  }

  genFramesFromLineData() {
    let dataSource = this.$dataSource;

    for (let i = 0; i < this.$$stackTracesCounter(dataSource); i++) {
      const stackTrace = this.$$stackTraceExtractor(dataSource, i);
      if (!this.$$stackTraceFilter(dataSource, stackTrace)) {
        continue;
      }

      const frameCount = this.$$framesCounter(dataSource, stackTrace);
      if (frameCount === 0) {
        return;
      }

      this.$stackTraceMaxDepth = Math.max(this.$stackTraceMaxDepth, frameCount);
      let weights = this.$$weightsExtractor(dataSource, stackTrace);
      let weight, weightOfBaseline1, weightOfBaseline2;
      if (this.$$diff) {
        [weightOfBaseline1, weightOfBaseline2] = weights;
        weight = weightOfBaseline1 + weightOfBaseline2;
        this.$totalWeightOfBaseline1 += weightOfBaseline1;
        this.$totalWeightOfBaseline2 += weightOfBaseline2;
      } else {
        weight = weights;
      }

      this.$totalWeight += weight;
      this.$root.addWeight(weight);

      let current = this.$root;
      let j = this.$$reverse ? frameCount - 1 : 0;
      let end = this.$$reverse ? -1 : frameCount;
      let step = this.$$reverse ? -1 : 1;
      for (; j !== end; j += step) {
        const frame = this.$$frameExtractor(dataSource, stackTrace, j);
        const child = current.findOrAddChild(frame);
        child.addWeight(weight);
        if (this.$$diff) {
          child.addWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);
        }
        current = child;
      }
      current.addSelfWeight(weight);
      if (this.$$diff) {
        current.addSelfWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);
      }
    }
  }

  genFramesFromTreeData() {
    const queue = [];
    let dataSource = this.$dataSource;

    const process = (parent, frame) => {
      let child = parent.addChild(frame);
      let weights = this.$$weightsExtractor(dataSource, frame);
      let selfWeight, weight, selfWeightOfBaseline1, weightOfBaseline1, selfWeightOfBaseline2, weightOfBaseline2;
      if (this.$$diff) {
        [selfWeightOfBaseline1, weightOfBaseline1, selfWeightOfBaseline2, weightOfBaseline2] = weights;
        child.addSelfWeightOfBaselines(selfWeightOfBaseline1, selfWeightOfBaseline2);
        child.addWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);
        selfWeight = selfWeightOfBaseline1 + selfWeightOfBaseline2;
        weight = weightOfBaseline1 + weightOfBaseline2;
      } else {
        [selfWeight, weight] = weights;
      }

      child.addSelfWeight(selfWeight);
      child.addWeight(weight);
      this.$stackTraceMaxDepth = Math.max(this.$stackTraceMaxDepth, child.depth);

      if (this.$$childFramesCounter(dataSource, frame) > 0) {
        queue.push(child);
      }
      return child;
    };

    const rootFramesCount = this.$$rootFramesCounter(dataSource);
    for (let i = 0; i < rootFramesCount; i++) {
      const rootFrame = process(this.$root, this.$$rootFrameExtractor(dataSource, i));
      this.$totalWeight += rootFrame.weight;
      if (this.$$diff) {
        this.$totalWeightOfBaseline1 += rootFrame.weightOfBaseline1;
        this.$totalWeightOfBaseline2 += rootFrame.weightOfBaseline2;
      }
    }

    this.$root.addWeight(this.$totalWeight);

    while (queue.length > 0) {
      const frame = queue.shift();
      const childrenCount = this.$$childFramesCounter(dataSource, frame.raw);
      for (let i = 0; i < childrenCount; i++) {
        process(frame, this.$$childFrameExtractor(dataSource, frame.raw, i));
      }
    }
  }

  genFrames() {
    this.$root.clear();
    this.$stackTraceMaxDepth = 0;
    this.$totalWeight = 0;
    this.$totalWeightOfBaseline1 = 0;
    this.$totalWeightOfBaseline2 = 0;

    if (this.$dataSource) {
      let format = this.$dataSource.format;
      if (format === 'line') {
        this.genFramesFromLineData();
      } else if (format === 'tree') {
        if (this.$$reverse) {
          console.warn("Tree format data doesn't support reverse");
        }
        this.genFramesFromTreeData();
      } else {
        throw new Error(`Unsupported dataSource format ${format}`);
      }
    }

    this.$root.sort();

    this.$information = this.$$diff
      ? {
          totalWeight: this.$totalWeight,
          totalWeightOfBaseline1: this.$totalWeightOfBaseline1,
          totalWeightOfBaseline2: this.$totalWeightOfBaseline2
        }
      : {
          totalWeight: this.$totalWeight
        };

    this.$root.text = this.$$rootTextGenerator(this.$dataSource, this.$information);
  }

  initRenderContext() {
    let w = this.width;
    if (w) {
      if (w.endsWith('%')) {
        this.$flameGraph.style.width = w;
      } else {
        this.$flameGraph.style.width = w + 'px';
      }
    }

    let h = this.height;
    if (h) {
      if (h.endsWith('%')) {
        this.$flameGraph.style.height = h;
      } else {
        this.$flameGraph.style.height = h + 'px';
      }
    }

    this.$context.restore();
    this.$context.save();

    let height = (this.$stackTraceMaxDepth + 1) * this.$frameHeight;
    if (this.$stackTraceMaxDepth > 0) {
      height += this.$stackTraceMaxDepth * this.$yGap;
    }
    height += this.$fgVGap + this.$fgVEndGap;

    this.$flameGraphInnerWrapper.style.height = height + 'px';
    this.$flameGraphHeight = height;

    let innerHeight = this.$flameGraphInner.getBoundingClientRect().height;
    this.$flameGraphInner.style.overflowY = null;
    if (innerHeight < height) {
      this.$flameGraphInner.style.overflowY = 'auto';
    } else if (!this.downward) {
      this.$flameGraphInnerWrapper.style.height = innerHeight + 'px';
    }

    if (!this.downward) {
      this.$flameGraphInner.scrollTop = this.$flameGraphInner.scrollHeight;
      this.$downwardBunnton.style.background = 'grey';
      this.$downwardBunnton.style.flexDirection = 'row';
    } else {
      this.$flameGraphInner.scrollTop = 0;
      this.$downwardBunnton.style.background = 'rgb(24, 144, 255)';
      this.$downwardBunnton.style.flexDirection = 'row-reverse';
    }

    const dpr = window.devicePixelRatio || 1;
    const rect = this.$canvas.getBoundingClientRect();
    this.$canvas.width = rect.width * dpr;
    this.$canvas.height = rect.height * dpr;
    this.$context.scale(dpr, dpr);

    if (this.$dataSource) {
      this.$root.color = this.$$rootColorSelector(this.$dataSource, this.$information);
    }
    this.$colorBarDiv.style.display = 'none';
  }

  connectedCallback() {
    this.addEventListener('re-render', () => {
      this.render(true, true);
    });
  }

  disconnectedCallback() {
    window.removeEventListener('scroll', this.$scrollEventListener);
  }

  get width() {
    return this.getAttribute('width');
  }

  set width(w) {
    this.setAttribute('width', w);
  }

  get height() {
    return this.getAttribute('height');
  }

  set height(h) {
    this.setAttribute('height', h);
  }

  get downward() {
    return this.hasAttribute('downward');
  }

  set downward(downward) {
    this.toggleAttribute('downward', !!downward);
  }

  static get observedAttributes() {
    return ['width', 'height', 'downward'];
  }

  attributeChangedCallback(name, oldVal, newVal) {
    if (!this.$dataSource || oldVal === newVal) {
      return;
    }
    this.render(true, false);
  }

  set dataSource(dataSource) {
    if (!dataSource.format) {
      throw new Error("Should specify the format of dataSource: 'line' or 'tree'");
    }
    if (typeof dataSource.format !== 'string') {
      throw new Error('Illegal dataSource format type, must be string');
    }
    let format = dataSource.format.toLowerCase();
    if ('line' !== format && 'tree' !== format) {
      throw new Error("Illegal dataSource format, must be 'line' or 'tree'");
    }
    this.$dataSource = dataSource;
    this.render(true, true);
  }

  get dataSource() {
    return this.$dataSource;
  }

  set configuration(configuration) {
    if (typeof configuration !== 'object') {
      throw new Error('Configuration should be an object');
    }
    this.$configuration = configuration;
  }

  get configuration() {
    return this.$configuration;
  }

  getConfigItemOrDefault(name, def) {
    if (this.$configuration && this.$configuration[name]) {
      return this.$configuration[name];
    }
    return def;
  }

  _(name, def) {
    return this.getConfigItemOrDefault(name, def);
  }

  // 配置方法
  get dataExtractor() {
    return this._('dataExtractor', (dataSource) => dataSource.data);
  }

  get stackTracesCounter() {
    return this._('stackTracesCounter', (dataSource) => this.$$dataExtractor(dataSource).length);
  }

  get stackTraceExtractor() {
    return this._(
      'stackTraceExtractor',
      (dataSource, index) => this.$$dataExtractor(dataSource)[index]
    );
  }

  get framesCounter() {
    return this._('framesCounter', (dataSource, stackTrace) => {
      return stackTrace[this.$$framesIndexer(dataSource, stackTrace)].length;
    });
  }

  get frameExtractor() {
    return this._('frameExtractor', (dataSource, stackTrace, index) => {
      return stackTrace[this.$$framesIndexer(dataSource, stackTrace)][index];
    });
  }

  get framesIndexer() {
    return this._('framesIndexer', (dataSource, stackTrace) => 0);
  }

  get stackTraceFilter() {
    return this._('stackTraceFilter', (dataSource, stackTrace) => true);
  }

  get frameEquator() {
    return this._('frameEquator', (dataSource, left, right) => {
      return left === right;
    });
  }

  get reverse() {
    return !!this._('reverse', false);
  }

  get rootFramesCounter() {
    return this._(
      'rootFramesCounter',
      (dataSource) =>
        this.$$dataExtractor(dataSource).length / this.$$frameStepper(dataSource, null)
    );
  }

  get rootFrameExtractor() {
    return this._('rootFrameExtractor', (dataSource, index) => {
      let steps = this.$$frameStepper(dataSource, null);
      const start = index * steps;
      return this.$$dataExtractor(dataSource).slice(start, start + steps);
    });
  }

  get childFramesCounter() {
    return this._('childFramesCounter', (dataSource, frame) => {
      return (
        frame[this.$$childFramesIndexer(dataSource, frame)].length /
        this.$$frameStepper(dataSource, frame)
      );
    });
  }

  get childFrameExtractor() {
    return this._('childFrameExtractor', (dataSource, frame, index) => {
      let steps = this.$$frameStepper(dataSource, frame);
      const start = index * steps;
      return frame[this.$$childFramesIndexer(dataSource, frame)].slice(start, start + steps);
    });
  }

  get frameStepper() {
    return this._(
      'frameStepper',
      this.$$diff ? (dataSource, frame) => 6 : (dataSource, frame) => 4
    );
  }

  get childFramesIndexer() {
    return this._(
      'childFramesIndexer',
      this.$$diff ? (dataSource, frame) => 5 : (dataSource, frame) => 3
    );
  }

  get weightsExtractor() {
    return this._(
      'weightsExtractor',
      this.$$isLineFormat
        ? this.$$diff
          ? (dataSource, input) => [input[1], input[2]]
          : (dataSource, input) => input[1]
        : this.$$diff
        ? (dataSource, input) => [input[1], input[2], input[3], input[4]]
        : (dataSource, input) => [input[1], input[2]]
    );
  }

  get rootTextGenerator() {
    return this._('rootTextGenerator', (dataSource, information) => {
      let totalWeight = information.totalWeight.toLocaleString();
      if (this.$$diff) {
        let totalWeightOfBaseline1 = information.totalWeightOfBaseline1.toLocaleString();
        let totalWeightOfBaseline2 = information.totalWeightOfBaseline2.toLocaleString();
        return `Total: ${totalWeight} (Baseline1: ${totalWeightOfBaseline1}, Baseline2: ${totalWeightOfBaseline2})`;
      }
      return `Total: ${totalWeight}`;
    });
  }

  get textGenerator() {
    return this._(
      'textGenerator',
      this.$$isLineFormat
        ? (dataSource, frame, information) => frame
        : (dataSource, frame, information) => frame[0]
    );
  }

  get titleGenerator() {
    return this._('titleGenerator', (dataSource, frame, information) => information.text);
  }

  get detailsGenerator() {
    return this._('detailsGenerator', (dataSource, frame, information) => null);
  }

  get footTextGenerator() {
    return this._('footTextGenerator', (dataSource, frame, information) => {
      let selfWeight = information.selfWeight;
      let weight = information.weight;
      let totalWeight = information.totalWeight;
      let value = Math.round((weight / totalWeight) * 100 * 100) / 100;
      return `${value.toLocaleString()}% - (${selfWeight.toLocaleString()}, ${weight.toLocaleString()}, ${totalWeight.toLocaleString()})`;
    });
  }

  get rootColorSelector() {
    return this._('rootColorSelector', (dataSource, information) => ['#537e8b', '#ffffff']);
  }

  get colorSelector() {
    return this._('colorSelector', (dataSource, frame, information) => {
      if (this.$$diff) {
        return [this.diffColor(information.diffPercent), '#ffffff'];
      }
      let hashCode = this.$$hashCodeGenerator(dataSource, frame, information);
      if (hashCode === 0) {
        return this.$defaultColorScheme.colorForZero;
      }
      let colorIndex = Math.abs(hashCode) % this.$defaultColorScheme.colors.length;
      if (!colorIndex && colorIndex !== 0) {
        colorIndex = 0;
      }
      return this.$defaultColorScheme.colors[colorIndex];
    });
  }

  get footColorSelector() {
    return this._('footColorSelector', (dataSource, frame, information) => {
      return ['#537e8bff', '#373b46e6', '#ffffff'];
    });
  }

  get hashCodeGenerator() {
    return this._('hashCodeGenerator', (dataSource, frame, information) => {
      let text = information.text;
      let hash = 0;
      for (let i = 0; i < text.length; i++) {
        hash = 31 * hash + (text.charCodeAt(i) & 0xff);
        hash &= 0xffffffff;
      }
      return hash;
    });
  }

  get showHelpButton() {
    return !!this._('showHelpButton', false);
  }

  hexColorToFloatColor(hex) {
    return [
      parseInt(hex.substring(1, 3), 16) / 255,
      parseInt(hex.substring(3, 5), 16) / 255,
      parseInt(hex.substring(5, 7), 16) / 255
    ];
  }

  floatToHex(f) {
    let v = Math.round(f);
    let r = v.toString(16);
    if (r.length === 1) {
      return '0' + r;
    }
    return r;
  }

  floatColorToHexColor(float) {
    return (
      '#' +
      this.floatToHex(float[0] * 255) +
      this.floatToHex(float[1] * 255) +
      this.floatToHex(float[2] * 255)
    );
  }

  linearColor(from, to, pct) {
    return [
      from[0] + (to[0] - from[0]) * pct,
      from[1] + (to[1] - from[1]) * pct,
      from[2] + (to[2] - from[2]) * pct
    ];
  }

  diffColor(diffPercent) {
    let from = '#808080';
    let to = '#FF0000';
    if (diffPercent < 0) {
      to = '#008000';
      if (diffPercent < -1) {
        diffPercent = -1;
      }
    } else if (diffPercent > 1) {
      diffPercent = 1;
    }

    from = this.hexColorToFloatColor(from);
    to = this.hexColorToFloatColor(to);
    return this.floatColorToHexColor(this.linearColor(from, to, Math.abs(diffPercent)));
  }

  findFrame(x, y) {
    if (!this.$sibling) {
      return null;
    }

    let index;
    if (this.downward) {
      if (y <= this.$root.y) {
        return null;
      }
      index = Math.floor((y - this.$root.y) / (this.$frameHeight + this.$yGap));
    } else {
      if (y >= this.$root.y + this.$frameHeight) {
        return null;
      }
      index = Math.floor(
        (this.$root.y + this.$frameHeight - y) / (this.$frameHeight + this.$yGap)
      );
    }

    if (index >= this.$sibling.length || this.$sibling[index].length === 0) {
      return null;
    }

    let frame = this.$sibling[index][0];
    if (y <= frame.y || y >= frame.y + frame.height) {
      return null;
    }

    let start = 0;
    let end = this.$sibling[index].length - 1;
    while (start <= end) {
      const mid = (start + end) >>> 1;
      frame = this.$sibling[index][mid];
      if (x <= frame.x) {
        end = mid - 1;
      } else if (x >= frame.x + frame.width) {
        start = mid + 1;
      } else {
        return frame;
      }
    }
    return null;
  }

  handleCanvasMouseMoveEvent(e) {
    let lastTouchedFrame = this.$touchedFrame;

    if (lastTouchedFrame) {
      if (lastTouchedFrame.contain(e.offsetX, e.offsetY)) {
        lastTouchedFrame.touch(e.offsetX, e.offsetY);
        return;
      }
    }

    this.$touchedFrame = this.findFrame(e.offsetX, e.offsetY);

    if (lastTouchedFrame !== null && lastTouchedFrame !== this.$touchedFrame) {
      lastTouchedFrame.leave();
    }

    if (this.$touchedFrame) {
      this.$touchedFrame.touch(e.offsetX, e.offsetY);
    }
    e.stopPropagation();
  }

  decideFramePostcardLayout() {
    let rect = this.$framePostcardShadow.getBoundingClientRect();

    this.$framePostcard.style.left = rect.left + 'px';
    this.$framePostcard.style.top = rect.top + 'px';

    let height = this.$framePostcardContent.getBoundingClientRect().height + 26;

    let showAtTop = rect.top - height < 0;
    if (showAtTop) {
      this.$framePostcardContent.style.top = '26px';
      this.$framePostcardContent.style.bottom = null;
    } else {
      this.$framePostcardContent.style.top = null;
      this.$framePostcardContent.style.bottom = '26px';
    }
    let showAtLeft =
      rect.left + 392 > (window.innerWidth || document.documentElement.clientWidth);
    if (showAtLeft) {
      this.$framePostcardContentMain.style.marginLeft =
        366 - this.$framePostcardContentMain.clientWidth + 'px';
      this.$framePostcardContent.style.left = '-392px';
      if (showAtTop) {
        this.$framePostcardConnectingLine.style.transform =
          'rotate(135deg) translate3d(0px, -.5px, 0)';
      } else {
        this.$framePostcardConnectingLine.style.transform =
          'rotate(-135deg) translate3d(0px, -.5px, 0)';
      }
    } else {
      this.$framePostcardContentMain.style.marginLeft = '0px';
      this.$framePostcardContent.style.left = '26px';
      if (showAtTop) {
        this.$framePostcardConnectingLine.style.transform =
          'rotate(45deg) translate3d(0px, -.5px, 0)';
      } else {
        this.$framePostcardConnectingLine.style.transform =
          'rotate(-45deg) translate3d(0px, -.5px, 0)';
      }
    }
  }

  copy(stackTrace) {
    let text = this.$touchedFrame.text;
    if (stackTrace) {
      let f = this.$touchedFrame.parent;
      while (f && f !== this.$root) {
        text += '\n' + f.text;
        f = f.parent;
      }
    }
    if (navigator.clipboard && window.isSecureContext && false) {
      navigator.clipboard.writeText(text).then(() => {
        this.dispatchEvent(
          new CustomEvent('copied', {
            detail: {
              text: text
            }
          })
        );
      });
    } else {
      let textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      let success = document.execCommand('copy');
      textArea.remove();
      this.$frameMask.focus();
      if (success) {
        this.dispatchEvent(
          new CustomEvent('copied', {
            detail: {
              text: text
            }
          })
        );
      }
    }
  }
}

// 导出FlameGraph类
if (typeof module !== 'undefined' && module.exports) {
  module.exports = FlameGraph;
} else if (typeof window !== 'undefined') {
  window.FlameGraph = FlameGraph;
}
