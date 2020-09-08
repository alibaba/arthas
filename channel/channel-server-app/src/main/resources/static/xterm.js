(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.Terminal = f()}})(function(){var define,module,exports;return (function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Strings = require("./Strings");
var Platform_1 = require("./core/Platform");
var RenderDebouncer_1 = require("./ui/RenderDebouncer");
var Lifecycle_1 = require("./ui/Lifecycle");
var Lifecycle_2 = require("./common/Lifecycle");
var MAX_ROWS_TO_READ = 20;
var AccessibilityManager = (function (_super) {
    __extends(AccessibilityManager, _super);
    function AccessibilityManager(_terminal) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._liveRegionLineCount = 0;
        _this._charsToConsume = [];
        _this._accessibilityTreeRoot = document.createElement('div');
        _this._accessibilityTreeRoot.classList.add('xterm-accessibility');
        _this._rowContainer = document.createElement('div');
        _this._rowContainer.classList.add('xterm-accessibility-tree');
        _this._rowElements = [];
        for (var i = 0; i < _this._terminal.rows; i++) {
            _this._rowElements[i] = _this._createAccessibilityTreeNode();
            _this._rowContainer.appendChild(_this._rowElements[i]);
        }
        _this._topBoundaryFocusListener = function (e) { return _this._onBoundaryFocus(e, 0); };
        _this._bottomBoundaryFocusListener = function (e) { return _this._onBoundaryFocus(e, 1); };
        _this._rowElements[0].addEventListener('focus', _this._topBoundaryFocusListener);
        _this._rowElements[_this._rowElements.length - 1].addEventListener('focus', _this._bottomBoundaryFocusListener);
        _this._refreshRowsDimensions();
        _this._accessibilityTreeRoot.appendChild(_this._rowContainer);
        _this._renderRowsDebouncer = new RenderDebouncer_1.RenderDebouncer(_this._terminal, _this._renderRows.bind(_this));
        _this._refreshRows();
        _this._liveRegion = document.createElement('div');
        _this._liveRegion.classList.add('live-region');
        _this._liveRegion.setAttribute('aria-live', 'assertive');
        _this._accessibilityTreeRoot.appendChild(_this._liveRegion);
        _this._terminal.element.insertAdjacentElement('afterbegin', _this._accessibilityTreeRoot);
        _this.register(_this._renderRowsDebouncer);
        _this.register(_this._terminal.addDisposableListener('resize', function (data) { return _this._onResize(data.rows); }));
        _this.register(_this._terminal.addDisposableListener('refresh', function (data) { return _this._refreshRows(data.start, data.end); }));
        _this.register(_this._terminal.addDisposableListener('scroll', function (data) { return _this._refreshRows(); }));
        _this.register(_this._terminal.addDisposableListener('a11y.char', function (char) { return _this._onChar(char); }));
        _this.register(_this._terminal.addDisposableListener('linefeed', function () { return _this._onChar('\n'); }));
        _this.register(_this._terminal.addDisposableListener('a11y.tab', function (spaceCount) { return _this._onTab(spaceCount); }));
        _this.register(_this._terminal.addDisposableListener('key', function (keyChar) { return _this._onKey(keyChar); }));
        _this.register(_this._terminal.addDisposableListener('blur', function () { return _this._clearLiveRegion(); }));
        _this.register(_this._terminal.addDisposableListener('dprchange', function () { return _this._refreshRowsDimensions(); }));
        _this.register(_this._terminal.renderer.addDisposableListener('resize', function () { return _this._refreshRowsDimensions(); }));
        _this.register(Lifecycle_1.addDisposableDomListener(window, 'resize', function () { return _this._refreshRowsDimensions(); }));
        return _this;
    }
    AccessibilityManager.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._terminal.element.removeChild(this._accessibilityTreeRoot);
        this._rowElements.length = 0;
    };
    AccessibilityManager.prototype._onBoundaryFocus = function (e, position) {
        var boundaryElement = e.target;
        var beforeBoundaryElement = this._rowElements[position === 0 ? 1 : this._rowElements.length - 2];
        var posInSet = boundaryElement.getAttribute('aria-posinset');
        var lastRowPos = position === 0 ? '1' : "" + this._terminal.buffer.lines.length;
        if (posInSet === lastRowPos) {
            return;
        }
        if (e.relatedTarget !== beforeBoundaryElement) {
            return;
        }
        var topBoundaryElement;
        var bottomBoundaryElement;
        if (position === 0) {
            topBoundaryElement = boundaryElement;
            bottomBoundaryElement = this._rowElements.pop();
            this._rowContainer.removeChild(bottomBoundaryElement);
        }
        else {
            topBoundaryElement = this._rowElements.shift();
            bottomBoundaryElement = boundaryElement;
            this._rowContainer.removeChild(topBoundaryElement);
        }
        topBoundaryElement.removeEventListener('focus', this._topBoundaryFocusListener);
        bottomBoundaryElement.removeEventListener('focus', this._bottomBoundaryFocusListener);
        if (position === 0) {
            var newElement = this._createAccessibilityTreeNode();
            this._rowElements.unshift(newElement);
            this._rowContainer.insertAdjacentElement('afterbegin', newElement);
        }
        else {
            var newElement = this._createAccessibilityTreeNode();
            this._rowElements.push(newElement);
            this._rowContainer.appendChild(newElement);
        }
        this._rowElements[0].addEventListener('focus', this._topBoundaryFocusListener);
        this._rowElements[this._rowElements.length - 1].addEventListener('focus', this._bottomBoundaryFocusListener);
        this._terminal.scrollLines(position === 0 ? -1 : 1);
        this._rowElements[position === 0 ? 1 : this._rowElements.length - 2].focus();
        e.preventDefault();
        e.stopImmediatePropagation();
    };
    AccessibilityManager.prototype._onResize = function (rows) {
        this._rowElements[this._rowElements.length - 1].removeEventListener('focus', this._bottomBoundaryFocusListener);
        for (var i = this._rowContainer.children.length; i < this._terminal.rows; i++) {
            this._rowElements[i] = this._createAccessibilityTreeNode();
            this._rowContainer.appendChild(this._rowElements[i]);
        }
        while (this._rowElements.length > rows) {
            this._rowContainer.removeChild(this._rowElements.pop());
        }
        this._rowElements[this._rowElements.length - 1].addEventListener('focus', this._bottomBoundaryFocusListener);
        this._refreshRowsDimensions();
    };
    AccessibilityManager.prototype._createAccessibilityTreeNode = function () {
        var element = document.createElement('div');
        element.setAttribute('role', 'listitem');
        element.tabIndex = -1;
        this._refreshRowDimensions(element);
        return element;
    };
    AccessibilityManager.prototype._onTab = function (spaceCount) {
        for (var i = 0; i < spaceCount; i++) {
            this._onChar(' ');
        }
    };
    AccessibilityManager.prototype._onChar = function (char) {
        var _this = this;
        if (this._liveRegionLineCount < MAX_ROWS_TO_READ + 1) {
            if (this._charsToConsume.length > 0) {
                var shiftedChar = this._charsToConsume.shift();
                if (shiftedChar !== char) {
                    this._announceCharacter(char);
                }
            }
            else {
                this._announceCharacter(char);
            }
            if (char === '\n') {
                this._liveRegionLineCount++;
                if (this._liveRegionLineCount === MAX_ROWS_TO_READ + 1) {
                    this._liveRegion.textContent += Strings.tooMuchOutput;
                }
            }
            if (Platform_1.isMac) {
                if (this._liveRegion.textContent && this._liveRegion.textContent.length > 0 && !this._liveRegion.parentNode) {
                    setTimeout(function () {
                        _this._accessibilityTreeRoot.appendChild(_this._liveRegion);
                    }, 0);
                }
            }
        }
    };
    AccessibilityManager.prototype._clearLiveRegion = function () {
        this._liveRegion.textContent = '';
        this._liveRegionLineCount = 0;
        if (Platform_1.isMac) {
            if (this._liveRegion.parentNode) {
                this._accessibilityTreeRoot.removeChild(this._liveRegion);
            }
        }
    };
    AccessibilityManager.prototype._onKey = function (keyChar) {
        this._clearLiveRegion();
        this._charsToConsume.push(keyChar);
    };
    AccessibilityManager.prototype._refreshRows = function (start, end) {
        this._renderRowsDebouncer.refresh(start, end);
    };
    AccessibilityManager.prototype._renderRows = function (start, end) {
        var buffer = this._terminal.buffer;
        var setSize = buffer.lines.length.toString();
        for (var i = start; i <= end; i++) {
            var lineData = buffer.translateBufferLineToString(buffer.ydisp + i, true);
            var posInSet = (buffer.ydisp + i + 1).toString();
            var element = this._rowElements[i];
            element.textContent = lineData.length === 0 ? Strings.blankLine : lineData;
            element.setAttribute('aria-posinset', posInSet);
            element.setAttribute('aria-setsize', setSize);
        }
    };
    AccessibilityManager.prototype._refreshRowsDimensions = function () {
        if (!this._terminal.renderer.dimensions.actualCellHeight) {
            return;
        }
        if (this._rowElements.length !== this._terminal.rows) {
            this._onResize(this._terminal.rows);
        }
        for (var i = 0; i < this._terminal.rows; i++) {
            this._refreshRowDimensions(this._rowElements[i]);
        }
    };
    AccessibilityManager.prototype._refreshRowDimensions = function (element) {
        element.style.height = this._terminal.renderer.dimensions.actualCellHeight + "px";
    };
    AccessibilityManager.prototype._announceCharacter = function (char) {
        if (char === ' ') {
            this._liveRegion.innerHTML += '&nbsp;';
        }
        else {
            this._liveRegion.textContent += char;
        }
    };
    return AccessibilityManager;
}(Lifecycle_2.Disposable));
exports.AccessibilityManager = AccessibilityManager;

},{"./Strings":13,"./common/Lifecycle":18,"./core/Platform":21,"./ui/Lifecycle":48,"./ui/RenderDebouncer":50}],2:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var CircularList_1 = require("./common/CircularList");
var EventEmitter_1 = require("./common/EventEmitter");
var BufferLine_1 = require("./BufferLine");
var Types_1 = require("./renderer/atlas/Types");
exports.DEFAULT_ATTR = (0 << 18) | (Types_1.DEFAULT_COLOR << 9) | (256 << 0);
exports.CHAR_DATA_ATTR_INDEX = 0;
exports.CHAR_DATA_CHAR_INDEX = 1;
exports.CHAR_DATA_WIDTH_INDEX = 2;
exports.CHAR_DATA_CODE_INDEX = 3;
exports.MAX_BUFFER_SIZE = 4294967295;
exports.NULL_CELL_CHAR = ' ';
exports.NULL_CELL_WIDTH = 1;
exports.NULL_CELL_CODE = 32;
var Buffer = (function () {
    function Buffer(_terminal, _hasScrollback) {
        this._terminal = _terminal;
        this._hasScrollback = _hasScrollback;
        this.markers = [];
        this.clear();
    }
    Buffer.prototype.setBufferLineFactory = function (type) {
        if (type === 'JsArray') {
            if (this._bufferLineConstructor !== BufferLine_1.BufferLineJSArray) {
                this._bufferLineConstructor = BufferLine_1.BufferLineJSArray;
                this._recreateLines();
            }
        }
        else {
            if (this._bufferLineConstructor !== BufferLine_1.BufferLine) {
                this._bufferLineConstructor = BufferLine_1.BufferLine;
                this._recreateLines();
            }
        }
    };
    Buffer.prototype._recreateLines = function () {
        if (!this.lines)
            return;
        for (var i = 0; i < this.lines.length; ++i) {
            var oldLine = this.lines.get(i);
            var newLine = new this._bufferLineConstructor(oldLine.length);
            for (var j = 0; j < oldLine.length; ++j) {
                newLine.set(j, oldLine.get(j));
            }
            this.lines.set(i, newLine);
        }
    };
    Buffer.prototype.getBlankLine = function (attr, isWrapped) {
        var fillCharData = [attr, exports.NULL_CELL_CHAR, exports.NULL_CELL_WIDTH, exports.NULL_CELL_CODE];
        return new this._bufferLineConstructor(this._terminal.cols, fillCharData, isWrapped);
    };
    Object.defineProperty(Buffer.prototype, "hasScrollback", {
        get: function () {
            return this._hasScrollback && this.lines.maxLength > this._terminal.rows;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Buffer.prototype, "isCursorInViewport", {
        get: function () {
            var absoluteY = this.ybase + this.y;
            var relativeY = absoluteY - this.ydisp;
            return (relativeY >= 0 && relativeY < this._terminal.rows);
        },
        enumerable: true,
        configurable: true
    });
    Buffer.prototype._getCorrectBufferLength = function (rows) {
        if (!this._hasScrollback) {
            return rows;
        }
        var correctBufferLength = rows + this._terminal.options.scrollback;
        return correctBufferLength > exports.MAX_BUFFER_SIZE ? exports.MAX_BUFFER_SIZE : correctBufferLength;
    };
    Buffer.prototype.fillViewportRows = function (fillAttr) {
        if (this.lines.length === 0) {
            if (fillAttr === undefined) {
                fillAttr = exports.DEFAULT_ATTR;
            }
            var i = this._terminal.rows;
            while (i--) {
                this.lines.push(this.getBlankLine(fillAttr));
            }
        }
    };
    Buffer.prototype.clear = function () {
        this.setBufferLineFactory(this._terminal.options.experimentalBufferLineImpl);
        this.ydisp = 0;
        this.ybase = 0;
        this.y = 0;
        this.x = 0;
        this.lines = new CircularList_1.CircularList(this._getCorrectBufferLength(this._terminal.rows));
        this.scrollTop = 0;
        this.scrollBottom = this._terminal.rows - 1;
        this.setupTabStops();
    };
    Buffer.prototype.resize = function (newCols, newRows) {
        var newMaxLength = this._getCorrectBufferLength(newRows);
        if (newMaxLength > this.lines.maxLength) {
            this.lines.maxLength = newMaxLength;
        }
        if (this.lines.length > 0) {
            if (this._terminal.cols < newCols) {
                var ch = [exports.DEFAULT_ATTR, exports.NULL_CELL_CHAR, exports.NULL_CELL_WIDTH, exports.NULL_CELL_CODE];
                for (var i = 0; i < this.lines.length; i++) {
                    this.lines.get(i).resize(newCols, ch);
                }
            }
            var addToY = 0;
            if (this._terminal.rows < newRows) {
                for (var y = this._terminal.rows; y < newRows; y++) {
                    if (this.lines.length < newRows + this.ybase) {
                        if (this.ybase > 0 && this.lines.length <= this.ybase + this.y + addToY + 1) {
                            this.ybase--;
                            addToY++;
                            if (this.ydisp > 0) {
                                this.ydisp--;
                            }
                        }
                        else {
                            var fillCharData = [exports.DEFAULT_ATTR, exports.NULL_CELL_CHAR, exports.NULL_CELL_WIDTH, exports.NULL_CELL_CODE];
                            this.lines.push(new this._bufferLineConstructor(newCols, fillCharData));
                        }
                    }
                }
            }
            else {
                for (var y = this._terminal.rows; y > newRows; y--) {
                    if (this.lines.length > newRows + this.ybase) {
                        if (this.lines.length > this.ybase + this.y + 1) {
                            this.lines.pop();
                        }
                        else {
                            this.ybase++;
                            this.ydisp++;
                        }
                    }
                }
            }
            if (newMaxLength < this.lines.maxLength) {
                var amountToTrim = this.lines.length - newMaxLength;
                if (amountToTrim > 0) {
                    this.lines.trimStart(amountToTrim);
                    this.ybase = Math.max(this.ybase - amountToTrim, 0);
                    this.ydisp = Math.max(this.ydisp - amountToTrim, 0);
                }
                this.lines.maxLength = newMaxLength;
            }
            this.x = Math.min(this.x, newCols - 1);
            this.y = Math.min(this.y, newRows - 1);
            if (addToY) {
                this.y += addToY;
            }
            this.savedY = Math.min(this.savedY, newRows - 1);
            this.savedX = Math.min(this.savedX, newCols - 1);
            this.scrollTop = 0;
        }
        this.scrollBottom = newRows - 1;
    };
    Buffer.prototype.stringIndexToBufferIndex = function (lineIndex, stringIndex) {
        while (stringIndex) {
            var line = this.lines.get(lineIndex);
            if (!line) {
                [-1, -1];
            }
            for (var i = 0; i < line.length; ++i) {
                stringIndex -= line.get(i)[exports.CHAR_DATA_CHAR_INDEX].length;
                if (stringIndex < 0) {
                    return [lineIndex, i];
                }
            }
            lineIndex++;
        }
        return [lineIndex, 0];
    };
    Buffer.prototype.translateBufferLineToString = function (lineIndex, trimRight, startCol, endCol) {
        if (startCol === void 0) { startCol = 0; }
        if (endCol === void 0) { endCol = null; }
        var lineString = '';
        var line = this.lines.get(lineIndex);
        if (!line) {
            return '';
        }
        var startIndex = startCol;
        if (endCol === null) {
            endCol = line.length;
        }
        var endIndex = endCol;
        for (var i = 0; i < line.length; i++) {
            var char = line.get(i);
            lineString += char[exports.CHAR_DATA_CHAR_INDEX];
            if (char[exports.CHAR_DATA_WIDTH_INDEX] === 0) {
                if (startCol >= i) {
                    startIndex--;
                }
                if (endCol > i) {
                    endIndex--;
                }
            }
            else {
                if (char[exports.CHAR_DATA_CHAR_INDEX].length > 1) {
                    if (startCol > i) {
                        startIndex += char[exports.CHAR_DATA_CHAR_INDEX].length - 1;
                    }
                    if (endCol > i) {
                        endIndex += char[exports.CHAR_DATA_CHAR_INDEX].length - 1;
                    }
                }
            }
        }
        if (trimRight) {
            var rightWhitespaceIndex = lineString.search(/\s+$/);
            if (rightWhitespaceIndex !== -1) {
                endIndex = Math.min(endIndex, rightWhitespaceIndex);
            }
            if (endIndex <= startIndex) {
                return '';
            }
        }
        return lineString.substring(startIndex, endIndex);
    };
    Buffer.prototype.getWrappedRangeForLine = function (y) {
        var first = y;
        var last = y;
        while (first > 0 && this.lines.get(first).isWrapped) {
            first--;
        }
        while (last + 1 < this.lines.length && this.lines.get(last + 1).isWrapped) {
            last++;
        }
        return { first: first, last: last };
    };
    Buffer.prototype.setupTabStops = function (i) {
        if (i !== null && i !== undefined) {
            if (!this.tabs[i]) {
                i = this.prevStop(i);
            }
        }
        else {
            this.tabs = {};
            i = 0;
        }
        for (; i < this._terminal.cols; i += this._terminal.options.tabStopWidth) {
            this.tabs[i] = true;
        }
    };
    Buffer.prototype.prevStop = function (x) {
        if (x === null || x === undefined) {
            x = this.x;
        }
        while (!this.tabs[--x] && x > 0)
            ;
        return x >= this._terminal.cols ? this._terminal.cols - 1 : x < 0 ? 0 : x;
    };
    Buffer.prototype.nextStop = function (x) {
        if (x === null || x === undefined) {
            x = this.x;
        }
        while (!this.tabs[++x] && x < this._terminal.cols)
            ;
        return x >= this._terminal.cols ? this._terminal.cols - 1 : x < 0 ? 0 : x;
    };
    Buffer.prototype.addMarker = function (y) {
        var _this = this;
        var marker = new Marker(y);
        this.markers.push(marker);
        marker.register(this.lines.addDisposableListener('trim', function (amount) {
            marker.line -= amount;
            if (marker.line < 0) {
                marker.dispose();
            }
        }));
        marker.register(marker.addDisposableListener('dispose', function () { return _this._removeMarker(marker); }));
        return marker;
    };
    Buffer.prototype._removeMarker = function (marker) {
        this.markers.splice(this.markers.indexOf(marker), 1);
    };
    Buffer.prototype.iterator = function (trimRight, startIndex, endIndex, startOverscan, endOverscan) {
        return new BufferStringIterator(this, trimRight, startIndex, endIndex, startOverscan, endOverscan);
    };
    return Buffer;
}());
exports.Buffer = Buffer;
var Marker = (function (_super) {
    __extends(Marker, _super);
    function Marker(line) {
        var _this = _super.call(this) || this;
        _this.line = line;
        _this._id = Marker._nextId++;
        _this.isDisposed = false;
        return _this;
    }
    Object.defineProperty(Marker.prototype, "id", {
        get: function () { return this._id; },
        enumerable: true,
        configurable: true
    });
    Marker.prototype.dispose = function () {
        if (this.isDisposed) {
            return;
        }
        this.isDisposed = true;
        this.emit('dispose');
        _super.prototype.dispose.call(this);
    };
    Marker._nextId = 1;
    return Marker;
}(EventEmitter_1.EventEmitter));
exports.Marker = Marker;
var BufferStringIterator = (function () {
    function BufferStringIterator(_buffer, _trimRight, _startIndex, _endIndex, _startOverscan, _endOverscan) {
        if (_startIndex === void 0) { _startIndex = 0; }
        if (_endIndex === void 0) { _endIndex = _buffer.lines.length; }
        if (_startOverscan === void 0) { _startOverscan = 0; }
        if (_endOverscan === void 0) { _endOverscan = 0; }
        this._buffer = _buffer;
        this._trimRight = _trimRight;
        this._startIndex = _startIndex;
        this._endIndex = _endIndex;
        this._startOverscan = _startOverscan;
        this._endOverscan = _endOverscan;
        if (this._startIndex < 0) {
            this._startIndex = 0;
        }
        if (this._endIndex > this._buffer.lines.length) {
            this._endIndex = this._buffer.lines.length;
        }
        this._current = this._startIndex;
    }
    BufferStringIterator.prototype.hasNext = function () {
        return this._current < this._endIndex;
    };
    BufferStringIterator.prototype.next = function () {
        var range = this._buffer.getWrappedRangeForLine(this._current);
        if (range.first < this._startIndex - this._startOverscan) {
            range.first = this._startIndex - this._startOverscan;
        }
        if (range.last > this._endIndex + this._endOverscan) {
            range.last = this._endIndex + this._endOverscan;
        }
        range.first = Math.max(range.first, 0);
        range.last = Math.min(range.last, this._buffer.lines.length);
        var result = '';
        for (var i = range.first; i <= range.last; ++i) {
            result += this._buffer.translateBufferLineToString(i, (this._trimRight) ? i === range.last : false);
        }
        this._current = range.last + 1;
        return { range: range, content: result };
    };
    return BufferStringIterator;
}());
exports.BufferStringIterator = BufferStringIterator;

},{"./BufferLine":3,"./common/CircularList":16,"./common/EventEmitter":17,"./renderer/atlas/Types":43}],3:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("./Buffer");
var BufferLineJSArray = (function () {
    function BufferLineJSArray(cols, fillCharData, isWrapped) {
        this.isWrapped = false;
        this._data = [];
        if (!fillCharData) {
            fillCharData = [0, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE];
        }
        for (var i = 0; i < cols; i++) {
            this._push(fillCharData);
        }
        if (isWrapped) {
            this.isWrapped = true;
        }
        this.length = this._data.length;
    }
    BufferLineJSArray.prototype._pop = function () {
        var data = this._data.pop();
        this.length = this._data.length;
        return data;
    };
    BufferLineJSArray.prototype._push = function (data) {
        this._data.push(data);
        this.length = this._data.length;
    };
    BufferLineJSArray.prototype._splice = function (start, deleteCount) {
        var items = [];
        for (var _i = 2; _i < arguments.length; _i++) {
            items[_i - 2] = arguments[_i];
        }
        var _a;
        var removed = (_a = this._data).splice.apply(_a, [start, deleteCount].concat(items));
        this.length = this._data.length;
        return removed;
    };
    BufferLineJSArray.prototype.get = function (index) {
        return this._data[index];
    };
    BufferLineJSArray.prototype.set = function (index, data) {
        this._data[index] = data;
    };
    BufferLineJSArray.prototype.insertCells = function (pos, n, ch) {
        while (n--) {
            this._splice(pos, 0, ch);
            this._pop();
        }
    };
    BufferLineJSArray.prototype.deleteCells = function (pos, n, fillCharData) {
        while (n--) {
            this._splice(pos, 1);
            this._push(fillCharData);
        }
    };
    BufferLineJSArray.prototype.replaceCells = function (start, end, fillCharData) {
        while (start < end && start < this.length) {
            this.set(start++, fillCharData);
        }
    };
    BufferLineJSArray.prototype.resize = function (cols, fillCharData, shrink) {
        if (shrink === void 0) { shrink = false; }
        while (this._data.length < cols) {
            this._data.push(fillCharData);
        }
        if (shrink) {
            while (this._data.length > cols) {
                this._data.pop();
            }
        }
        this.length = this._data.length;
    };
    BufferLineJSArray.prototype.fill = function (fillCharData) {
        for (var i = 0; i < this.length; ++i) {
            this.set(i, fillCharData);
        }
    };
    BufferLineJSArray.prototype.copyFrom = function (line) {
        this._data = line._data.slice(0);
        this.length = line.length;
        this.isWrapped = line.isWrapped;
    };
    BufferLineJSArray.prototype.clone = function () {
        var newLine = new BufferLineJSArray(0);
        newLine.copyFrom(this);
        return newLine;
    };
    return BufferLineJSArray;
}());
exports.BufferLineJSArray = BufferLineJSArray;
var CELL_SIZE = 3;
var BufferLine = (function () {
    function BufferLine(cols, fillCharData, isWrapped) {
        if (isWrapped === void 0) { isWrapped = false; }
        this.isWrapped = isWrapped;
        this._data = null;
        this._combined = {};
        if (!fillCharData) {
            fillCharData = [0, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE];
        }
        if (cols) {
            this._data = new Uint32Array(cols * CELL_SIZE);
            for (var i = 0; i < cols; ++i) {
                this.set(i, fillCharData);
            }
        }
        this.length = cols;
    }
    BufferLine.prototype.get = function (index) {
        var stringData = this._data[index * CELL_SIZE + 1];
        return [
            this._data[index * CELL_SIZE + 0],
            (stringData & 0x80000000)
                ? this._combined[index]
                : (stringData) ? String.fromCharCode(stringData) : '',
            this._data[index * CELL_SIZE + 2],
            (stringData & 0x80000000)
                ? this._combined[index].charCodeAt(this._combined[index].length - 1)
                : stringData
        ];
    };
    BufferLine.prototype.set = function (index, value) {
        this._data[index * CELL_SIZE + 0] = value[0];
        if (value[1].length > 1) {
            this._combined[index] = value[1];
            this._data[index * CELL_SIZE + 1] = index | 0x80000000;
        }
        else {
            this._data[index * CELL_SIZE + 1] = value[1].charCodeAt(0);
        }
        this._data[index * CELL_SIZE + 2] = value[2];
    };
    BufferLine.prototype.insertCells = function (pos, n, fillCharData) {
        pos %= this.length;
        if (n < this.length - pos) {
            for (var i = this.length - pos - n - 1; i >= 0; --i) {
                this.set(pos + n + i, this.get(pos + i));
            }
            for (var i = 0; i < n; ++i) {
                this.set(pos + i, fillCharData);
            }
        }
        else {
            for (var i = pos; i < this.length; ++i) {
                this.set(i, fillCharData);
            }
        }
    };
    BufferLine.prototype.deleteCells = function (pos, n, fillCharData) {
        pos %= this.length;
        if (n < this.length - pos) {
            for (var i = 0; i < this.length - pos - n; ++i) {
                this.set(pos + i, this.get(pos + n + i));
            }
            for (var i = this.length - n; i < this.length; ++i) {
                this.set(i, fillCharData);
            }
        }
        else {
            for (var i = pos; i < this.length; ++i) {
                this.set(i, fillCharData);
            }
        }
    };
    BufferLine.prototype.replaceCells = function (start, end, fillCharData) {
        while (start < end && start < this.length) {
            this.set(start++, fillCharData);
        }
    };
    BufferLine.prototype.resize = function (cols, fillCharData, shrink) {
        if (shrink === void 0) { shrink = false; }
        if (cols === this.length || (!shrink && cols < this.length)) {
            return;
        }
        if (cols > this.length) {
            var data = new Uint32Array(cols * CELL_SIZE);
            if (this.length) {
                if (cols * CELL_SIZE < this._data.length) {
                    data.set(this._data.subarray(0, cols * CELL_SIZE));
                }
                else {
                    data.set(this._data);
                }
            }
            this._data = data;
            for (var i = this.length; i < cols; ++i) {
                this.set(i, fillCharData);
            }
        }
        else if (shrink) {
            if (cols) {
                var data = new Uint32Array(cols * CELL_SIZE);
                data.set(this._data.subarray(0, cols * CELL_SIZE));
                this._data = data;
            }
            else {
                this._data = null;
            }
        }
        this.length = cols;
    };
    BufferLine.prototype.fill = function (fillCharData) {
        this._combined = {};
        for (var i = 0; i < this.length; ++i) {
            this.set(i, fillCharData);
        }
    };
    BufferLine.prototype.copyFrom = function (line) {
        if (this.length !== line.length) {
            this._data = new Uint32Array(line._data);
        }
        else {
            this._data.set(line._data);
        }
        this.length = line.length;
        this._combined = {};
        for (var el in line._combined) {
            this._combined[el] = line._combined[el];
        }
        this.isWrapped = line.isWrapped;
    };
    BufferLine.prototype.clone = function () {
        var newLine = new BufferLine(0);
        newLine._data = new Uint32Array(this._data);
        newLine.length = this.length;
        for (var el in this._combined) {
            newLine._combined[el] = this._combined[el];
        }
        newLine.isWrapped = this.isWrapped;
        return newLine;
    };
    return BufferLine;
}());
exports.BufferLine = BufferLine;

},{"./Buffer":2}],4:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("./Buffer");
var EventEmitter_1 = require("./common/EventEmitter");
var BufferSet = (function (_super) {
    __extends(BufferSet, _super);
    function BufferSet(_terminal) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._normal = new Buffer_1.Buffer(_this._terminal, true);
        _this._normal.fillViewportRows();
        _this._alt = new Buffer_1.Buffer(_this._terminal, false);
        _this._activeBuffer = _this._normal;
        _this.setupTabStops();
        return _this;
    }
    Object.defineProperty(BufferSet.prototype, "alt", {
        get: function () {
            return this._alt;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BufferSet.prototype, "active", {
        get: function () {
            return this._activeBuffer;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BufferSet.prototype, "normal", {
        get: function () {
            return this._normal;
        },
        enumerable: true,
        configurable: true
    });
    BufferSet.prototype.activateNormalBuffer = function () {
        if (this._activeBuffer === this._normal) {
            return;
        }
        this._normal.x = this._alt.x;
        this._normal.y = this._alt.y;
        this._alt.clear();
        this._activeBuffer = this._normal;
        this.emit('activate', {
            activeBuffer: this._normal,
            inactiveBuffer: this._alt
        });
    };
    BufferSet.prototype.activateAltBuffer = function (fillAttr) {
        if (this._activeBuffer === this._alt) {
            return;
        }
        this._alt.fillViewportRows(fillAttr);
        this._alt.x = this._normal.x;
        this._alt.y = this._normal.y;
        this._activeBuffer = this._alt;
        this.emit('activate', {
            activeBuffer: this._alt,
            inactiveBuffer: this._normal
        });
    };
    BufferSet.prototype.resize = function (newCols, newRows) {
        this._normal.resize(newCols, newRows);
        this._alt.resize(newCols, newRows);
    };
    BufferSet.prototype.setupTabStops = function (i) {
        this._normal.setupTabStops(i);
        this._alt.setupTabStops(i);
    };
    return BufferSet;
}(EventEmitter_1.EventEmitter));
exports.BufferSet = BufferSet;

},{"./Buffer":2,"./common/EventEmitter":17}],5:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var TypedArrayUtils_1 = require("./common/TypedArrayUtils");
exports.wcwidth = (function (opts) {
    var COMBINING_BMP = [
        [0x0300, 0x036F], [0x0483, 0x0486], [0x0488, 0x0489],
        [0x0591, 0x05BD], [0x05BF, 0x05BF], [0x05C1, 0x05C2],
        [0x05C4, 0x05C5], [0x05C7, 0x05C7], [0x0600, 0x0603],
        [0x0610, 0x0615], [0x064B, 0x065E], [0x0670, 0x0670],
        [0x06D6, 0x06E4], [0x06E7, 0x06E8], [0x06EA, 0x06ED],
        [0x070F, 0x070F], [0x0711, 0x0711], [0x0730, 0x074A],
        [0x07A6, 0x07B0], [0x07EB, 0x07F3], [0x0901, 0x0902],
        [0x093C, 0x093C], [0x0941, 0x0948], [0x094D, 0x094D],
        [0x0951, 0x0954], [0x0962, 0x0963], [0x0981, 0x0981],
        [0x09BC, 0x09BC], [0x09C1, 0x09C4], [0x09CD, 0x09CD],
        [0x09E2, 0x09E3], [0x0A01, 0x0A02], [0x0A3C, 0x0A3C],
        [0x0A41, 0x0A42], [0x0A47, 0x0A48], [0x0A4B, 0x0A4D],
        [0x0A70, 0x0A71], [0x0A81, 0x0A82], [0x0ABC, 0x0ABC],
        [0x0AC1, 0x0AC5], [0x0AC7, 0x0AC8], [0x0ACD, 0x0ACD],
        [0x0AE2, 0x0AE3], [0x0B01, 0x0B01], [0x0B3C, 0x0B3C],
        [0x0B3F, 0x0B3F], [0x0B41, 0x0B43], [0x0B4D, 0x0B4D],
        [0x0B56, 0x0B56], [0x0B82, 0x0B82], [0x0BC0, 0x0BC0],
        [0x0BCD, 0x0BCD], [0x0C3E, 0x0C40], [0x0C46, 0x0C48],
        [0x0C4A, 0x0C4D], [0x0C55, 0x0C56], [0x0CBC, 0x0CBC],
        [0x0CBF, 0x0CBF], [0x0CC6, 0x0CC6], [0x0CCC, 0x0CCD],
        [0x0CE2, 0x0CE3], [0x0D41, 0x0D43], [0x0D4D, 0x0D4D],
        [0x0DCA, 0x0DCA], [0x0DD2, 0x0DD4], [0x0DD6, 0x0DD6],
        [0x0E31, 0x0E31], [0x0E34, 0x0E3A], [0x0E47, 0x0E4E],
        [0x0EB1, 0x0EB1], [0x0EB4, 0x0EB9], [0x0EBB, 0x0EBC],
        [0x0EC8, 0x0ECD], [0x0F18, 0x0F19], [0x0F35, 0x0F35],
        [0x0F37, 0x0F37], [0x0F39, 0x0F39], [0x0F71, 0x0F7E],
        [0x0F80, 0x0F84], [0x0F86, 0x0F87], [0x0F90, 0x0F97],
        [0x0F99, 0x0FBC], [0x0FC6, 0x0FC6], [0x102D, 0x1030],
        [0x1032, 0x1032], [0x1036, 0x1037], [0x1039, 0x1039],
        [0x1058, 0x1059], [0x1160, 0x11FF], [0x135F, 0x135F],
        [0x1712, 0x1714], [0x1732, 0x1734], [0x1752, 0x1753],
        [0x1772, 0x1773], [0x17B4, 0x17B5], [0x17B7, 0x17BD],
        [0x17C6, 0x17C6], [0x17C9, 0x17D3], [0x17DD, 0x17DD],
        [0x180B, 0x180D], [0x18A9, 0x18A9], [0x1920, 0x1922],
        [0x1927, 0x1928], [0x1932, 0x1932], [0x1939, 0x193B],
        [0x1A17, 0x1A18], [0x1B00, 0x1B03], [0x1B34, 0x1B34],
        [0x1B36, 0x1B3A], [0x1B3C, 0x1B3C], [0x1B42, 0x1B42],
        [0x1B6B, 0x1B73], [0x1DC0, 0x1DCA], [0x1DFE, 0x1DFF],
        [0x200B, 0x200F], [0x202A, 0x202E], [0x2060, 0x2063],
        [0x206A, 0x206F], [0x20D0, 0x20EF], [0x302A, 0x302F],
        [0x3099, 0x309A], [0xA806, 0xA806], [0xA80B, 0xA80B],
        [0xA825, 0xA826], [0xFB1E, 0xFB1E], [0xFE00, 0xFE0F],
        [0xFE20, 0xFE23], [0xFEFF, 0xFEFF], [0xFFF9, 0xFFFB]
    ];
    var COMBINING_HIGH = [
        [0x10A01, 0x10A03], [0x10A05, 0x10A06], [0x10A0C, 0x10A0F],
        [0x10A38, 0x10A3A], [0x10A3F, 0x10A3F], [0x1D167, 0x1D169],
        [0x1D173, 0x1D182], [0x1D185, 0x1D18B], [0x1D1AA, 0x1D1AD],
        [0x1D242, 0x1D244], [0xE0001, 0xE0001], [0xE0020, 0xE007F],
        [0xE0100, 0xE01EF]
    ];
    function bisearch(ucs, data) {
        var min = 0;
        var max = data.length - 1;
        var mid;
        if (ucs < data[0][0] || ucs > data[max][1]) {
            return false;
        }
        while (max >= min) {
            mid = (min + max) >> 1;
            if (ucs > data[mid][1]) {
                min = mid + 1;
            }
            else if (ucs < data[mid][0]) {
                max = mid - 1;
            }
            else {
                return true;
            }
        }
        return false;
    }
    function wcwidthHigh(ucs) {
        if (bisearch(ucs, COMBINING_HIGH)) {
            return 0;
        }
        if ((ucs >= 0x20000 && ucs <= 0x2fffd) || (ucs >= 0x30000 && ucs <= 0x3fffd)) {
            return 2;
        }
        return 1;
    }
    var control = opts.control | 0;
    var table = new Uint8Array(65536);
    TypedArrayUtils_1.fill(table, 1);
    table[0] = opts.nul;
    TypedArrayUtils_1.fill(table, opts.control, 1, 32);
    TypedArrayUtils_1.fill(table, opts.control, 0x7f, 0xa0);
    TypedArrayUtils_1.fill(table, 2, 0x1100, 0x1160);
    table[0x2329] = 2;
    table[0x232a] = 2;
    TypedArrayUtils_1.fill(table, 2, 0x2e80, 0xa4d0);
    table[0x303f] = 1;
    TypedArrayUtils_1.fill(table, 2, 0xac00, 0xd7a4);
    TypedArrayUtils_1.fill(table, 2, 0xf900, 0xfb00);
    TypedArrayUtils_1.fill(table, 2, 0xfe10, 0xfe1a);
    TypedArrayUtils_1.fill(table, 2, 0xfe30, 0xfe70);
    TypedArrayUtils_1.fill(table, 2, 0xff00, 0xff61);
    TypedArrayUtils_1.fill(table, 2, 0xffe0, 0xffe7);
    for (var r = 0; r < COMBINING_BMP.length; ++r) {
        TypedArrayUtils_1.fill(table, 0, COMBINING_BMP[r][0], COMBINING_BMP[r][1] + 1);
    }
    return function (num) {
        if (num < 32) {
            return control | 0;
        }
        if (num < 127) {
            return 1;
        }
        if (num < 65536) {
            return table[num];
        }
        return wcwidthHigh(num);
    };
})({ nul: 0, control: 0 });
function getStringCellWidth(s) {
    var result = 0;
    var length = s.length;
    for (var i = 0; i < length; ++i) {
        var code = s.charCodeAt(i);
        if (0xD800 <= code && code <= 0xDBFF) {
            if (++i >= length) {
                return result + exports.wcwidth(code);
            }
            var second = s.charCodeAt(i);
            if (0xDC00 <= second && second <= 0xDFFF) {
                code = (code - 0xD800) * 0x400 + second - 0xDC00 + 0x10000;
            }
            else {
                result += exports.wcwidth(second);
            }
        }
        result += exports.wcwidth(code);
    }
    return result;
}
exports.getStringCellWidth = getStringCellWidth;

},{"./common/TypedArrayUtils":19}],6:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var CompositionHelper = (function () {
    function CompositionHelper(_textarea, _compositionView, _terminal) {
        this._textarea = _textarea;
        this._compositionView = _compositionView;
        this._terminal = _terminal;
        this._isComposing = false;
        this._isSendingComposition = false;
        this._compositionPosition = { start: null, end: null };
    }
    CompositionHelper.prototype.compositionstart = function () {
        this._isComposing = true;
        this._compositionPosition.start = this._textarea.value.length;
        this._compositionView.textContent = '';
        this._compositionView.classList.add('active');
    };
    CompositionHelper.prototype.compositionupdate = function (ev) {
        var _this = this;
        this._compositionView.textContent = ev.data;
        this.updateCompositionElements();
        setTimeout(function () {
            _this._compositionPosition.end = _this._textarea.value.length;
        }, 0);
    };
    CompositionHelper.prototype.compositionend = function () {
        this._finalizeComposition(true);
    };
    CompositionHelper.prototype.keydown = function (ev) {
        if (this._isComposing || this._isSendingComposition) {
            if (ev.keyCode === 229) {
                return false;
            }
            else if (ev.keyCode === 16 || ev.keyCode === 17 || ev.keyCode === 18) {
                return false;
            }
            this._finalizeComposition(false);
        }
        if (ev.keyCode === 229) {
            this._handleAnyTextareaChanges();
            return false;
        }
        return true;
    };
    CompositionHelper.prototype._finalizeComposition = function (waitForPropogation) {
        var _this = this;
        this._compositionView.classList.remove('active');
        this._isComposing = false;
        this._clearTextareaPosition();
        if (!waitForPropogation) {
            this._isSendingComposition = false;
            var input = this._textarea.value.substring(this._compositionPosition.start, this._compositionPosition.end);
            this._terminal.handler(input);
        }
        else {
            var currentCompositionPosition_1 = {
                start: this._compositionPosition.start,
                end: this._compositionPosition.end
            };
            this._isSendingComposition = true;
            setTimeout(function () {
                if (_this._isSendingComposition) {
                    _this._isSendingComposition = false;
                    var input = void 0;
                    if (_this._isComposing) {
                        input = _this._textarea.value.substring(currentCompositionPosition_1.start, currentCompositionPosition_1.end);
                    }
                    else {
                        input = _this._textarea.value.substring(currentCompositionPosition_1.start);
                    }
                    _this._terminal.handler(input);
                }
            }, 0);
        }
    };
    CompositionHelper.prototype._handleAnyTextareaChanges = function () {
        var _this = this;
        var oldValue = this._textarea.value;
        setTimeout(function () {
            if (!_this._isComposing) {
                var newValue = _this._textarea.value;
                var diff = newValue.replace(oldValue, '');
                if (diff.length > 0) {
                    _this._terminal.handler(diff);
                }
            }
        }, 0);
    };
    CompositionHelper.prototype.updateCompositionElements = function (dontRecurse) {
        var _this = this;
        if (!this._isComposing) {
            return;
        }
        if (this._terminal.buffer.isCursorInViewport) {
            var cellHeight = Math.ceil(this._terminal.charMeasure.height * this._terminal.options.lineHeight);
            var cursorTop = this._terminal.buffer.y * cellHeight;
            var cursorLeft = this._terminal.buffer.x * this._terminal.charMeasure.width;
            this._compositionView.style.left = cursorLeft + 'px';
            this._compositionView.style.top = cursorTop + 'px';
            this._compositionView.style.height = cellHeight + 'px';
            this._compositionView.style.lineHeight = cellHeight + 'px';
            var compositionViewBounds = this._compositionView.getBoundingClientRect();
            this._textarea.style.left = cursorLeft + 'px';
            this._textarea.style.top = cursorTop + 'px';
            this._textarea.style.width = compositionViewBounds.width + 'px';
            this._textarea.style.height = compositionViewBounds.height + 'px';
            this._textarea.style.lineHeight = compositionViewBounds.height + 'px';
        }
        if (!dontRecurse) {
            setTimeout(function () { return _this.updateCompositionElements(true); }, 0);
        }
    };
    CompositionHelper.prototype._clearTextareaPosition = function () {
        this._textarea.style.left = '';
        this._textarea.style.top = '';
    };
    return CompositionHelper;
}());
exports.CompositionHelper = CompositionHelper;

},{}],7:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Lifecycle_1 = require("./common/Lifecycle");
function r(low, high) {
    var c = high - low;
    var arr = new Array(c);
    while (c--) {
        arr[c] = --high;
    }
    return arr;
}
var TransitionTable = (function () {
    function TransitionTable(length) {
        this.table = (typeof Uint8Array === 'undefined')
            ? new Array(length)
            : new Uint8Array(length);
    }
    TransitionTable.prototype.add = function (code, state, action, next) {
        this.table[state << 8 | code] = ((action | 0) << 4) | ((next === undefined) ? state : next);
    };
    TransitionTable.prototype.addMany = function (codes, state, action, next) {
        for (var i = 0; i < codes.length; i++) {
            this.add(codes[i], state, action, next);
        }
    };
    return TransitionTable;
}());
exports.TransitionTable = TransitionTable;
var PRINTABLES = r(0x20, 0x7f);
var EXECUTABLES = r(0x00, 0x18);
EXECUTABLES.push(0x19);
EXECUTABLES.push.apply(EXECUTABLES, r(0x1c, 0x20));
var DEFAULT_TRANSITION = 1 << 4 | 0;
exports.VT500_TRANSITION_TABLE = (function () {
    var table = new TransitionTable(4095);
    var states = r(0, 13 + 1);
    var state;
    for (state in states) {
        for (var code = 0; code < 160; ++code) {
            table.add(code, state, 1, 0);
        }
    }
    table.addMany(PRINTABLES, 0, 2, 0);
    for (state in states) {
        table.addMany([0x18, 0x1a, 0x99, 0x9a], state, 3, 0);
        table.addMany(r(0x80, 0x90), state, 3, 0);
        table.addMany(r(0x90, 0x98), state, 3, 0);
        table.add(0x9c, state, 0, 0);
        table.add(0x1b, state, 11, 1);
        table.add(0x9d, state, 4, 8);
        table.addMany([0x98, 0x9e, 0x9f], state, 0, 7);
        table.add(0x9b, state, 11, 3);
        table.add(0x90, state, 11, 9);
    }
    table.addMany(EXECUTABLES, 0, 3, 0);
    table.addMany(EXECUTABLES, 1, 3, 1);
    table.add(0x7f, 1, 0, 1);
    table.addMany(EXECUTABLES, 8, 0, 8);
    table.addMany(EXECUTABLES, 3, 3, 3);
    table.add(0x7f, 3, 0, 3);
    table.addMany(EXECUTABLES, 4, 3, 4);
    table.add(0x7f, 4, 0, 4);
    table.addMany(EXECUTABLES, 6, 3, 6);
    table.addMany(EXECUTABLES, 5, 3, 5);
    table.add(0x7f, 5, 0, 5);
    table.addMany(EXECUTABLES, 2, 3, 2);
    table.add(0x7f, 2, 0, 2);
    table.add(0x5d, 1, 4, 8);
    table.addMany(PRINTABLES, 8, 5, 8);
    table.add(0x7f, 8, 5, 8);
    table.addMany([0x9c, 0x1b, 0x18, 0x1a, 0x07], 8, 6, 0);
    table.addMany(r(0x1c, 0x20), 8, 0, 8);
    table.addMany([0x58, 0x5e, 0x5f], 1, 0, 7);
    table.addMany(PRINTABLES, 7, 0, 7);
    table.addMany(EXECUTABLES, 7, 0, 7);
    table.add(0x9c, 7, 0, 0);
    table.add(0x5b, 1, 11, 3);
    table.addMany(r(0x40, 0x7f), 3, 7, 0);
    table.addMany(r(0x30, 0x3a), 3, 8, 4);
    table.add(0x3b, 3, 8, 4);
    table.addMany([0x3c, 0x3d, 0x3e, 0x3f], 3, 9, 4);
    table.addMany(r(0x30, 0x3a), 4, 8, 4);
    table.add(0x3b, 4, 8, 4);
    table.addMany(r(0x40, 0x7f), 4, 7, 0);
    table.addMany([0x3a, 0x3c, 0x3d, 0x3e, 0x3f], 4, 0, 6);
    table.addMany(r(0x20, 0x40), 6, 0, 6);
    table.add(0x7f, 6, 0, 6);
    table.addMany(r(0x40, 0x7f), 6, 0, 0);
    table.add(0x3a, 3, 0, 6);
    table.addMany(r(0x20, 0x30), 3, 9, 5);
    table.addMany(r(0x20, 0x30), 5, 9, 5);
    table.addMany(r(0x30, 0x40), 5, 0, 6);
    table.addMany(r(0x40, 0x7f), 5, 7, 0);
    table.addMany(r(0x20, 0x30), 4, 9, 5);
    table.addMany(r(0x20, 0x30), 1, 9, 2);
    table.addMany(r(0x20, 0x30), 2, 9, 2);
    table.addMany(r(0x30, 0x7f), 2, 10, 0);
    table.addMany(r(0x30, 0x50), 1, 10, 0);
    table.addMany(r(0x51, 0x58), 1, 10, 0);
    table.addMany([0x59, 0x5a, 0x5c], 1, 10, 0);
    table.addMany(r(0x60, 0x7f), 1, 10, 0);
    table.add(0x50, 1, 11, 9);
    table.addMany(EXECUTABLES, 9, 0, 9);
    table.add(0x7f, 9, 0, 9);
    table.addMany(r(0x1c, 0x20), 9, 0, 9);
    table.addMany(r(0x20, 0x30), 9, 9, 12);
    table.add(0x3a, 9, 0, 11);
    table.addMany(r(0x30, 0x3a), 9, 8, 10);
    table.add(0x3b, 9, 8, 10);
    table.addMany([0x3c, 0x3d, 0x3e, 0x3f], 9, 9, 10);
    table.addMany(EXECUTABLES, 11, 0, 11);
    table.addMany(r(0x20, 0x80), 11, 0, 11);
    table.addMany(r(0x1c, 0x20), 11, 0, 11);
    table.addMany(EXECUTABLES, 10, 0, 10);
    table.add(0x7f, 10, 0, 10);
    table.addMany(r(0x1c, 0x20), 10, 0, 10);
    table.addMany(r(0x30, 0x3a), 10, 8, 10);
    table.add(0x3b, 10, 8, 10);
    table.addMany([0x3a, 0x3c, 0x3d, 0x3e, 0x3f], 10, 0, 11);
    table.addMany(r(0x20, 0x30), 10, 9, 12);
    table.addMany(EXECUTABLES, 12, 0, 12);
    table.add(0x7f, 12, 0, 12);
    table.addMany(r(0x1c, 0x20), 12, 0, 12);
    table.addMany(r(0x20, 0x30), 12, 9, 12);
    table.addMany(r(0x30, 0x40), 12, 0, 11);
    table.addMany(r(0x40, 0x7f), 12, 12, 13);
    table.addMany(r(0x40, 0x7f), 10, 12, 13);
    table.addMany(r(0x40, 0x7f), 9, 12, 13);
    table.addMany(EXECUTABLES, 13, 13, 13);
    table.addMany(PRINTABLES, 13, 13, 13);
    table.add(0x7f, 13, 0, 13);
    table.addMany([0x1b, 0x9c], 13, 14, 0);
    return table;
})();
var DcsDummy = (function () {
    function DcsDummy() {
    }
    DcsDummy.prototype.hook = function (collect, params, flag) { };
    DcsDummy.prototype.put = function (data, start, end) { };
    DcsDummy.prototype.unhook = function () { };
    return DcsDummy;
}());
var EscapeSequenceParser = (function (_super) {
    __extends(EscapeSequenceParser, _super);
    function EscapeSequenceParser(TRANSITIONS) {
        if (TRANSITIONS === void 0) { TRANSITIONS = exports.VT500_TRANSITION_TABLE; }
        var _this = _super.call(this) || this;
        _this.TRANSITIONS = TRANSITIONS;
        _this.initialState = 0;
        _this.currentState = _this.initialState;
        _this._osc = '';
        _this._params = [0];
        _this._collect = '';
        _this._printHandlerFb = function (data, start, end) { };
        _this._executeHandlerFb = function (code) { };
        _this._csiHandlerFb = function (collect, params, flag) { };
        _this._escHandlerFb = function (collect, flag) { };
        _this._oscHandlerFb = function (identifier, data) { };
        _this._dcsHandlerFb = new DcsDummy();
        _this._errorHandlerFb = function (state) { return state; };
        _this._printHandler = _this._printHandlerFb;
        _this._executeHandlers = Object.create(null);
        _this._csiHandlers = Object.create(null);
        _this._escHandlers = Object.create(null);
        _this._oscHandlers = Object.create(null);
        _this._dcsHandlers = Object.create(null);
        _this._activeDcsHandler = null;
        _this._errorHandler = _this._errorHandlerFb;
        _this.setEscHandler('\\', function () { });
        return _this;
    }
    EscapeSequenceParser.prototype.dispose = function () {
        this._printHandlerFb = null;
        this._executeHandlerFb = null;
        this._csiHandlerFb = null;
        this._escHandlerFb = null;
        this._oscHandlerFb = null;
        this._dcsHandlerFb = null;
        this._errorHandlerFb = null;
        this._printHandler = null;
        this._executeHandlers = null;
        this._csiHandlers = null;
        this._escHandlers = null;
        this._oscHandlers = null;
        this._dcsHandlers = null;
        this._activeDcsHandler = null;
        this._errorHandler = null;
    };
    EscapeSequenceParser.prototype.setPrintHandler = function (callback) {
        this._printHandler = callback;
    };
    EscapeSequenceParser.prototype.clearPrintHandler = function () {
        this._printHandler = this._printHandlerFb;
    };
    EscapeSequenceParser.prototype.setExecuteHandler = function (flag, callback) {
        this._executeHandlers[flag.charCodeAt(0)] = callback;
    };
    EscapeSequenceParser.prototype.clearExecuteHandler = function (flag) {
        if (this._executeHandlers[flag.charCodeAt(0)])
            delete this._executeHandlers[flag.charCodeAt(0)];
    };
    EscapeSequenceParser.prototype.setExecuteHandlerFallback = function (callback) {
        this._executeHandlerFb = callback;
    };
    EscapeSequenceParser.prototype.setCsiHandler = function (flag, callback) {
        this._csiHandlers[flag.charCodeAt(0)] = callback;
    };
    EscapeSequenceParser.prototype.clearCsiHandler = function (flag) {
        if (this._csiHandlers[flag.charCodeAt(0)])
            delete this._csiHandlers[flag.charCodeAt(0)];
    };
    EscapeSequenceParser.prototype.setCsiHandlerFallback = function (callback) {
        this._csiHandlerFb = callback;
    };
    EscapeSequenceParser.prototype.setEscHandler = function (collectAndFlag, callback) {
        this._escHandlers[collectAndFlag] = callback;
    };
    EscapeSequenceParser.prototype.clearEscHandler = function (collectAndFlag) {
        if (this._escHandlers[collectAndFlag])
            delete this._escHandlers[collectAndFlag];
    };
    EscapeSequenceParser.prototype.setEscHandlerFallback = function (callback) {
        this._escHandlerFb = callback;
    };
    EscapeSequenceParser.prototype.setOscHandler = function (ident, callback) {
        this._oscHandlers[ident] = callback;
    };
    EscapeSequenceParser.prototype.clearOscHandler = function (ident) {
        if (this._oscHandlers[ident])
            delete this._oscHandlers[ident];
    };
    EscapeSequenceParser.prototype.setOscHandlerFallback = function (callback) {
        this._oscHandlerFb = callback;
    };
    EscapeSequenceParser.prototype.setDcsHandler = function (collectAndFlag, handler) {
        this._dcsHandlers[collectAndFlag] = handler;
    };
    EscapeSequenceParser.prototype.clearDcsHandler = function (collectAndFlag) {
        if (this._dcsHandlers[collectAndFlag])
            delete this._dcsHandlers[collectAndFlag];
    };
    EscapeSequenceParser.prototype.setDcsHandlerFallback = function (handler) {
        this._dcsHandlerFb = handler;
    };
    EscapeSequenceParser.prototype.setErrorHandler = function (callback) {
        this._errorHandler = callback;
    };
    EscapeSequenceParser.prototype.clearErrorHandler = function () {
        this._errorHandler = this._errorHandlerFb;
    };
    EscapeSequenceParser.prototype.reset = function () {
        this.currentState = this.initialState;
        this._osc = '';
        this._params = [0];
        this._collect = '';
        this._activeDcsHandler = null;
    };
    EscapeSequenceParser.prototype.parse = function (data) {
        var code = 0;
        var transition = 0;
        var error = false;
        var currentState = this.currentState;
        var print = -1;
        var dcs = -1;
        var osc = this._osc;
        var collect = this._collect;
        var params = this._params;
        var table = this.TRANSITIONS.table;
        var dcsHandler = this._activeDcsHandler;
        var callback = null;
        var l = data.length;
        for (var i = 0; i < l; ++i) {
            code = data.charCodeAt(i);
            if (currentState === 0 && code > 0x1f && code < 0x80) {
                print = (~print) ? print : i;
                do
                    i++;
                while (i < l && data.charCodeAt(i) > 0x1f && data.charCodeAt(i) < 0x80);
                i--;
                continue;
            }
            if (currentState === 4 && (code > 0x2f && code < 0x39)) {
                params[params.length - 1] = params[params.length - 1] * 10 + code - 48;
                continue;
            }
            transition = (code < 0xa0) ? (table[currentState << 8 | code]) : DEFAULT_TRANSITION;
            switch (transition >> 4) {
                case 2:
                    print = (~print) ? print : i;
                    break;
                case 3:
                    if (~print) {
                        this._printHandler(data, print, i);
                        print = -1;
                    }
                    callback = this._executeHandlers[code];
                    if (callback)
                        callback();
                    else
                        this._executeHandlerFb(code);
                    break;
                case 0:
                    if (~print) {
                        this._printHandler(data, print, i);
                        print = -1;
                    }
                    else if (~dcs) {
                        dcsHandler.put(data, dcs, i);
                        dcs = -1;
                    }
                    break;
                case 1:
                    if (code > 0x9f) {
                        switch (currentState) {
                            case 0:
                                print = (~print) ? print : i;
                                break;
                            case 8:
                                osc += String.fromCharCode(code);
                                transition |= 8;
                                break;
                            case 6:
                                transition |= 6;
                                break;
                            case 11:
                                transition |= 11;
                                break;
                            case 13:
                                dcs = (~dcs) ? dcs : i;
                                transition |= 13;
                                break;
                            default:
                                error = true;
                        }
                    }
                    else {
                        error = true;
                    }
                    if (error) {
                        var inject = this._errorHandler({
                            position: i,
                            code: code,
                            currentState: currentState,
                            print: print,
                            dcs: dcs,
                            osc: osc,
                            collect: collect,
                            params: params,
                            abort: false
                        });
                        if (inject.abort)
                            return;
                        error = false;
                    }
                    break;
                case 7:
                    callback = this._csiHandlers[code];
                    if (callback)
                        callback(params, collect);
                    else
                        this._csiHandlerFb(collect, params, code);
                    break;
                case 8:
                    if (code === 0x3b)
                        params.push(0);
                    else
                        params[params.length - 1] = params[params.length - 1] * 10 + code - 48;
                    break;
                case 9:
                    collect += String.fromCharCode(code);
                    break;
                case 10:
                    callback = this._escHandlers[collect + String.fromCharCode(code)];
                    if (callback)
                        callback(collect, code);
                    else
                        this._escHandlerFb(collect, code);
                    break;
                case 11:
                    if (~print) {
                        this._printHandler(data, print, i);
                        print = -1;
                    }
                    osc = '';
                    params = [0];
                    collect = '';
                    dcs = -1;
                    break;
                case 12:
                    dcsHandler = this._dcsHandlers[collect + String.fromCharCode(code)];
                    if (!dcsHandler)
                        dcsHandler = this._dcsHandlerFb;
                    dcsHandler.hook(collect, params, code);
                    break;
                case 13:
                    dcs = (~dcs) ? dcs : i;
                    break;
                case 14:
                    if (dcsHandler) {
                        if (~dcs)
                            dcsHandler.put(data, dcs, i);
                        dcsHandler.unhook();
                        dcsHandler = null;
                    }
                    if (code === 0x1b)
                        transition |= 1;
                    osc = '';
                    params = [0];
                    collect = '';
                    dcs = -1;
                    break;
                case 4:
                    if (~print) {
                        this._printHandler(data, print, i);
                        print = -1;
                    }
                    osc = '';
                    break;
                case 5:
                    osc += data.charAt(i);
                    break;
                case 6:
                    if (osc && code !== 0x18 && code !== 0x1a) {
                        var idx = osc.indexOf(';');
                        if (idx === -1) {
                            this._oscHandlerFb(-1, osc);
                        }
                        else {
                            var identifier = parseInt(osc.substring(0, idx));
                            var content = osc.substring(idx + 1);
                            callback = this._oscHandlers[identifier];
                            if (callback)
                                callback(content);
                            else
                                this._oscHandlerFb(identifier, content);
                        }
                    }
                    if (code === 0x1b)
                        transition |= 1;
                    osc = '';
                    params = [0];
                    collect = '';
                    dcs = -1;
                    break;
            }
            currentState = transition & 15;
        }
        if (currentState === 0 && ~print) {
            this._printHandler(data, print, data.length);
        }
        else if (currentState === 13 && ~dcs && dcsHandler) {
            dcsHandler.put(data, dcs, data.length);
        }
        this._osc = osc;
        this._collect = collect;
        this._params = params;
        this._activeDcsHandler = dcsHandler;
        this.currentState = currentState;
    };
    return EscapeSequenceParser;
}(Lifecycle_1.Disposable));
exports.EscapeSequenceParser = EscapeSequenceParser;

},{"./common/Lifecycle":18}],8:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var EscapeSequences_1 = require("./common/data/EscapeSequences");
var Charsets_1 = require("./core/data/Charsets");
var Buffer_1 = require("./Buffer");
var CharWidth_1 = require("./CharWidth");
var EscapeSequenceParser_1 = require("./EscapeSequenceParser");
var Lifecycle_1 = require("./common/Lifecycle");
var GLEVEL = { '(': 0, ')': 1, '*': 2, '+': 3, '-': 1, '.': 2 };
var RequestTerminfo = (function () {
    function RequestTerminfo(_terminal) {
        this._terminal = _terminal;
    }
    RequestTerminfo.prototype.hook = function (collect, params, flag) {
        this._data = '';
    };
    RequestTerminfo.prototype.put = function (data, start, end) {
        this._data += data.substring(start, end);
    };
    RequestTerminfo.prototype.unhook = function () {
        this._terminal.handler(EscapeSequences_1.C0.ESC + "P0+r" + this._data + EscapeSequences_1.C0.ESC + "\\");
    };
    return RequestTerminfo;
}());
var DECRQSS = (function () {
    function DECRQSS(_terminal) {
        this._terminal = _terminal;
    }
    DECRQSS.prototype.hook = function (collect, params, flag) {
        this._data = '';
    };
    DECRQSS.prototype.put = function (data, start, end) {
        this._data += data.substring(start, end);
    };
    DECRQSS.prototype.unhook = function () {
        switch (this._data) {
            case '"q':
                return this._terminal.handler(EscapeSequences_1.C0.ESC + "P1$r0\"q" + EscapeSequences_1.C0.ESC + "\\");
            case '"p':
                return this._terminal.handler(EscapeSequences_1.C0.ESC + "P1$r61\"p" + EscapeSequences_1.C0.ESC + "\\");
            case 'r':
                var pt = '' + (this._terminal.buffer.scrollTop + 1) +
                    ';' + (this._terminal.buffer.scrollBottom + 1) + 'r';
                return this._terminal.handler(EscapeSequences_1.C0.ESC + "P1$r" + pt + EscapeSequences_1.C0.ESC + "\\");
            case 'm':
                return this._terminal.handler(EscapeSequences_1.C0.ESC + "P1$r0m" + EscapeSequences_1.C0.ESC + "\\");
            case ' q':
                var STYLES = { 'block': 2, 'underline': 4, 'bar': 6 };
                var style = STYLES[this._terminal.getOption('cursorStyle')];
                style -= this._terminal.getOption('cursorBlink');
                return this._terminal.handler(EscapeSequences_1.C0.ESC + "P1$r" + style + " q" + EscapeSequences_1.C0.ESC + "\\");
            default:
                this._terminal.error('Unknown DCS $q %s', this._data);
                this._terminal.handler(EscapeSequences_1.C0.ESC + "P0$r" + this._data + EscapeSequences_1.C0.ESC + "\\");
        }
    };
    return DECRQSS;
}());
var InputHandler = (function (_super) {
    __extends(InputHandler, _super);
    function InputHandler(_terminal, _parser) {
        if (_parser === void 0) { _parser = new EscapeSequenceParser_1.EscapeSequenceParser(); }
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._parser = _parser;
        _this.register(_this._parser);
        _this._surrogateFirst = '';
        _this._parser.setCsiHandlerFallback(function (collect, params, flag) {
            _this._terminal.error('Unknown CSI code: ', { collect: collect, params: params, flag: String.fromCharCode(flag) });
        });
        _this._parser.setEscHandlerFallback(function (collect, flag) {
            _this._terminal.error('Unknown ESC code: ', { collect: collect, flag: String.fromCharCode(flag) });
        });
        _this._parser.setExecuteHandlerFallback(function (code) {
            _this._terminal.error('Unknown EXECUTE code: ', { code: code });
        });
        _this._parser.setOscHandlerFallback(function (identifier, data) {
            _this._terminal.error('Unknown OSC code: ', { identifier: identifier, data: data });
        });
        _this._parser.setPrintHandler(function (data, start, end) { return _this.print(data, start, end); });
        _this._parser.setCsiHandler('@', function (params, collect) { return _this.insertChars(params); });
        _this._parser.setCsiHandler('A', function (params, collect) { return _this.cursorUp(params); });
        _this._parser.setCsiHandler('B', function (params, collect) { return _this.cursorDown(params); });
        _this._parser.setCsiHandler('C', function (params, collect) { return _this.cursorForward(params); });
        _this._parser.setCsiHandler('D', function (params, collect) { return _this.cursorBackward(params); });
        _this._parser.setCsiHandler('E', function (params, collect) { return _this.cursorNextLine(params); });
        _this._parser.setCsiHandler('F', function (params, collect) { return _this.cursorPrecedingLine(params); });
        _this._parser.setCsiHandler('G', function (params, collect) { return _this.cursorCharAbsolute(params); });
        _this._parser.setCsiHandler('H', function (params, collect) { return _this.cursorPosition(params); });
        _this._parser.setCsiHandler('I', function (params, collect) { return _this.cursorForwardTab(params); });
        _this._parser.setCsiHandler('J', function (params, collect) { return _this.eraseInDisplay(params); });
        _this._parser.setCsiHandler('K', function (params, collect) { return _this.eraseInLine(params); });
        _this._parser.setCsiHandler('L', function (params, collect) { return _this.insertLines(params); });
        _this._parser.setCsiHandler('M', function (params, collect) { return _this.deleteLines(params); });
        _this._parser.setCsiHandler('P', function (params, collect) { return _this.deleteChars(params); });
        _this._parser.setCsiHandler('S', function (params, collect) { return _this.scrollUp(params); });
        _this._parser.setCsiHandler('T', function (params, collect) { return _this.scrollDown(params, collect); });
        _this._parser.setCsiHandler('X', function (params, collect) { return _this.eraseChars(params); });
        _this._parser.setCsiHandler('Z', function (params, collect) { return _this.cursorBackwardTab(params); });
        _this._parser.setCsiHandler('`', function (params, collect) { return _this.charPosAbsolute(params); });
        _this._parser.setCsiHandler('a', function (params, collect) { return _this.hPositionRelative(params); });
        _this._parser.setCsiHandler('b', function (params, collect) { return _this.repeatPrecedingCharacter(params); });
        _this._parser.setCsiHandler('c', function (params, collect) { return _this.sendDeviceAttributes(params, collect); });
        _this._parser.setCsiHandler('d', function (params, collect) { return _this.linePosAbsolute(params); });
        _this._parser.setCsiHandler('e', function (params, collect) { return _this.vPositionRelative(params); });
        _this._parser.setCsiHandler('f', function (params, collect) { return _this.hVPosition(params); });
        _this._parser.setCsiHandler('g', function (params, collect) { return _this.tabClear(params); });
        _this._parser.setCsiHandler('h', function (params, collect) { return _this.setMode(params, collect); });
        _this._parser.setCsiHandler('l', function (params, collect) { return _this.resetMode(params, collect); });
        _this._parser.setCsiHandler('m', function (params, collect) { return _this.charAttributes(params); });
        _this._parser.setCsiHandler('n', function (params, collect) { return _this.deviceStatus(params, collect); });
        _this._parser.setCsiHandler('p', function (params, collect) { return _this.softReset(params, collect); });
        _this._parser.setCsiHandler('q', function (params, collect) { return _this.setCursorStyle(params, collect); });
        _this._parser.setCsiHandler('r', function (params, collect) { return _this.setScrollRegion(params, collect); });
        _this._parser.setCsiHandler('s', function (params, collect) { return _this.saveCursor(params); });
        _this._parser.setCsiHandler('u', function (params, collect) { return _this.restoreCursor(params); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.BEL, function () { return _this.bell(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.LF, function () { return _this.lineFeed(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.VT, function () { return _this.lineFeed(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.FF, function () { return _this.lineFeed(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.CR, function () { return _this.carriageReturn(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.BS, function () { return _this.backspace(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.HT, function () { return _this.tab(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.SO, function () { return _this.shiftOut(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C0.SI, function () { return _this.shiftIn(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C1.IND, function () { return _this.index(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C1.NEL, function () { return _this.nextLine(); });
        _this._parser.setExecuteHandler(EscapeSequences_1.C1.HTS, function () { return _this.tabSet(); });
        _this._parser.setOscHandler(0, function (data) { return _this.setTitle(data); });
        _this._parser.setOscHandler(2, function (data) { return _this.setTitle(data); });
        _this._parser.setEscHandler('7', function () { return _this.saveCursor([]); });
        _this._parser.setEscHandler('8', function () { return _this.restoreCursor([]); });
        _this._parser.setEscHandler('D', function () { return _this.index(); });
        _this._parser.setEscHandler('E', function () { return _this.nextLine(); });
        _this._parser.setEscHandler('H', function () { return _this.tabSet(); });
        _this._parser.setEscHandler('M', function () { return _this.reverseIndex(); });
        _this._parser.setEscHandler('=', function () { return _this.keypadApplicationMode(); });
        _this._parser.setEscHandler('>', function () { return _this.keypadNumericMode(); });
        _this._parser.setEscHandler('c', function () { return _this.reset(); });
        _this._parser.setEscHandler('n', function () { return _this.setgLevel(2); });
        _this._parser.setEscHandler('o', function () { return _this.setgLevel(3); });
        _this._parser.setEscHandler('|', function () { return _this.setgLevel(3); });
        _this._parser.setEscHandler('}', function () { return _this.setgLevel(2); });
        _this._parser.setEscHandler('~', function () { return _this.setgLevel(1); });
        _this._parser.setEscHandler('%@', function () { return _this.selectDefaultCharset(); });
        _this._parser.setEscHandler('%G', function () { return _this.selectDefaultCharset(); });
        var _loop_1 = function (flag) {
            this_1._parser.setEscHandler('(' + flag, function () { return _this.selectCharset('(' + flag); });
            this_1._parser.setEscHandler(')' + flag, function () { return _this.selectCharset(')' + flag); });
            this_1._parser.setEscHandler('*' + flag, function () { return _this.selectCharset('*' + flag); });
            this_1._parser.setEscHandler('+' + flag, function () { return _this.selectCharset('+' + flag); });
            this_1._parser.setEscHandler('-' + flag, function () { return _this.selectCharset('-' + flag); });
            this_1._parser.setEscHandler('.' + flag, function () { return _this.selectCharset('.' + flag); });
            this_1._parser.setEscHandler('/' + flag, function () { return _this.selectCharset('/' + flag); });
        };
        var this_1 = this;
        for (var flag in Charsets_1.CHARSETS) {
            _loop_1(flag);
        }
        _this._parser.setErrorHandler(function (state) {
            _this._terminal.error('Parsing error: ', state);
            return state;
        });
        _this._parser.setDcsHandler('$q', new DECRQSS(_this._terminal));
        _this._parser.setDcsHandler('+q', new RequestTerminfo(_this._terminal));
        return _this;
    }
    InputHandler.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._terminal = null;
    };
    InputHandler.prototype.parse = function (data) {
        if (!this._terminal) {
            return;
        }
        var buffer = this._terminal.buffer;
        var cursorStartX = buffer.x;
        var cursorStartY = buffer.y;
        if (this._terminal.debug) {
            this._terminal.log('data: ' + data);
        }
        if (this._surrogateFirst) {
            data = this._surrogateFirst + data;
            this._surrogateFirst = '';
        }
        this._parser.parse(data);
        buffer = this._terminal.buffer;
        if (buffer.x !== cursorStartX || buffer.y !== cursorStartY) {
            this._terminal.emit('cursormove');
        }
    };
    InputHandler.prototype.print = function (data, start, end) {
        var char;
        var code;
        var chWidth;
        var buffer = this._terminal.buffer;
        var charset = this._terminal.charset;
        var screenReaderMode = this._terminal.options.screenReaderMode;
        var cols = this._terminal.cols;
        var wraparoundMode = this._terminal.wraparoundMode;
        var insertMode = this._terminal.insertMode;
        var curAttr = this._terminal.curAttr;
        var bufferRow = buffer.lines.get(buffer.y + buffer.ybase);
        this._terminal.updateRange(buffer.y);
        for (var stringPosition = start; stringPosition < end; ++stringPosition) {
            char = data.charAt(stringPosition);
            code = data.charCodeAt(stringPosition);
            if (0xD800 <= code && code <= 0xDBFF) {
                if (++stringPosition >= end) {
                    this._surrogateFirst = char;
                    continue;
                }
                var second = data.charCodeAt(stringPosition);
                if (0xDC00 <= second && second <= 0xDFFF) {
                    code = (code - 0xD800) * 0x400 + second - 0xDC00 + 0x10000;
                    char += data.charAt(stringPosition);
                }
                else {
                    stringPosition--;
                }
            }
            chWidth = CharWidth_1.wcwidth(code);
            if (charset) {
                char = charset[char] || char;
                code = char.charCodeAt(0);
            }
            if (screenReaderMode) {
                this._terminal.emit('a11y.char', char);
            }
            if (!chWidth && buffer.x) {
                var chMinusOne = bufferRow.get(buffer.x - 1);
                if (chMinusOne) {
                    if (!chMinusOne[Buffer_1.CHAR_DATA_WIDTH_INDEX]) {
                        var chMinusTwo = bufferRow.get(buffer.x - 2);
                        if (chMinusTwo) {
                            chMinusTwo[Buffer_1.CHAR_DATA_CHAR_INDEX] += char;
                            chMinusTwo[Buffer_1.CHAR_DATA_CODE_INDEX] = code;
                            bufferRow.set(buffer.x - 2, chMinusTwo);
                        }
                    }
                    else {
                        chMinusOne[Buffer_1.CHAR_DATA_CHAR_INDEX] += char;
                        chMinusOne[Buffer_1.CHAR_DATA_CODE_INDEX] = code;
                        bufferRow.set(buffer.x - 1, chMinusOne);
                    }
                }
                continue;
            }
            if (buffer.x + chWidth - 1 >= cols) {
                if (wraparoundMode) {
                    buffer.x = 0;
                    buffer.y++;
                    if (buffer.y > buffer.scrollBottom) {
                        buffer.y--;
                        this._terminal.scroll(true);
                    }
                    else {
                        buffer.lines.get(buffer.y).isWrapped = true;
                    }
                    bufferRow = buffer.lines.get(buffer.y + buffer.ybase);
                }
                else {
                    if (chWidth === 2) {
                        continue;
                    }
                }
            }
            if (insertMode) {
                bufferRow.insertCells(buffer.x, chWidth, [curAttr, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
                var lastCell = bufferRow.get(cols - 1);
                if (lastCell[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 2) {
                    bufferRow.set(cols - 1, [curAttr, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
                }
            }
            bufferRow.set(buffer.x++, [curAttr, char, chWidth, code]);
            if (chWidth > 0) {
                while (--chWidth) {
                    bufferRow.set(buffer.x++, [curAttr, '', 0, undefined]);
                }
            }
        }
        this._terminal.updateRange(buffer.y);
    };
    InputHandler.prototype.bell = function () {
        this._terminal.bell();
    };
    InputHandler.prototype.lineFeed = function () {
        var buffer = this._terminal.buffer;
        if (this._terminal.options.convertEol) {
            buffer.x = 0;
        }
        buffer.y++;
        if (buffer.y > buffer.scrollBottom) {
            buffer.y--;
            this._terminal.scroll();
        }
        if (buffer.x >= this._terminal.cols) {
            buffer.x--;
        }
        this._terminal.emit('linefeed');
    };
    InputHandler.prototype.carriageReturn = function () {
        this._terminal.buffer.x = 0;
    };
    InputHandler.prototype.backspace = function () {
        if (this._terminal.buffer.x > 0) {
            this._terminal.buffer.x--;
        }
    };
    InputHandler.prototype.tab = function () {
        var originalX = this._terminal.buffer.x;
        this._terminal.buffer.x = this._terminal.buffer.nextStop();
        if (this._terminal.options.screenReaderMode) {
            this._terminal.emit('a11y.tab', this._terminal.buffer.x - originalX);
        }
    };
    InputHandler.prototype.shiftOut = function () {
        this._terminal.setgLevel(1);
    };
    InputHandler.prototype.shiftIn = function () {
        this._terminal.setgLevel(0);
    };
    InputHandler.prototype.insertChars = function (params) {
        this._terminal.buffer.lines.get(this._terminal.buffer.y + this._terminal.buffer.ybase).insertCells(this._terminal.buffer.x, params[0] || 1, [this._terminal.eraseAttr(), Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
        this._terminal.updateRange(this._terminal.buffer.y);
    };
    InputHandler.prototype.cursorUp = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y -= param;
        if (this._terminal.buffer.y < 0) {
            this._terminal.buffer.y = 0;
        }
    };
    InputHandler.prototype.cursorDown = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y += param;
        if (this._terminal.buffer.y >= this._terminal.rows) {
            this._terminal.buffer.y = this._terminal.rows - 1;
        }
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x--;
        }
    };
    InputHandler.prototype.cursorForward = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.x += param;
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x = this._terminal.cols - 1;
        }
    };
    InputHandler.prototype.cursorBackward = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x--;
        }
        this._terminal.buffer.x -= param;
        if (this._terminal.buffer.x < 0) {
            this._terminal.buffer.x = 0;
        }
    };
    InputHandler.prototype.cursorNextLine = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y += param;
        if (this._terminal.buffer.y >= this._terminal.rows) {
            this._terminal.buffer.y = this._terminal.rows - 1;
        }
        this._terminal.buffer.x = 0;
    };
    InputHandler.prototype.cursorPrecedingLine = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y -= param;
        if (this._terminal.buffer.y < 0) {
            this._terminal.buffer.y = 0;
        }
        this._terminal.buffer.x = 0;
    };
    InputHandler.prototype.cursorCharAbsolute = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.x = param - 1;
    };
    InputHandler.prototype.cursorPosition = function (params) {
        var col;
        var row = params[0] - 1;
        if (params.length >= 2) {
            col = params[1] - 1;
        }
        else {
            col = 0;
        }
        if (row < 0) {
            row = 0;
        }
        else if (row >= this._terminal.rows) {
            row = this._terminal.rows - 1;
        }
        if (col < 0) {
            col = 0;
        }
        else if (col >= this._terminal.cols) {
            col = this._terminal.cols - 1;
        }
        this._terminal.buffer.x = col;
        this._terminal.buffer.y = row;
    };
    InputHandler.prototype.cursorForwardTab = function (params) {
        var param = params[0] || 1;
        while (param--) {
            this._terminal.buffer.x = this._terminal.buffer.nextStop();
        }
    };
    InputHandler.prototype._eraseInBufferLine = function (y, start, end, clearWrap) {
        if (clearWrap === void 0) { clearWrap = false; }
        var line = this._terminal.buffer.lines.get(this._terminal.buffer.ybase + y);
        line.replaceCells(start, end, [this._terminal.eraseAttr(), Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
        if (clearWrap) {
            line.isWrapped = false;
        }
    };
    InputHandler.prototype._resetBufferLine = function (y) {
        this._eraseInBufferLine(y, 0, this._terminal.cols, true);
    };
    InputHandler.prototype.eraseInDisplay = function (params) {
        var j;
        switch (params[0]) {
            case 0:
                j = this._terminal.buffer.y;
                this._terminal.updateRange(j);
                this._eraseInBufferLine(j++, this._terminal.buffer.x, this._terminal.cols, this._terminal.buffer.x === 0);
                for (; j < this._terminal.rows; j++) {
                    this._resetBufferLine(j);
                }
                this._terminal.updateRange(j);
                break;
            case 1:
                j = this._terminal.buffer.y;
                this._terminal.updateRange(j);
                this._eraseInBufferLine(j, 0, this._terminal.buffer.x + 1, true);
                if (this._terminal.buffer.x + 1 >= this._terminal.cols) {
                    this._terminal.buffer.lines.get(j + 1).isWrapped = false;
                }
                while (j--) {
                    this._resetBufferLine(j);
                }
                this._terminal.updateRange(0);
                break;
            case 2:
                j = this._terminal.rows;
                this._terminal.updateRange(j - 1);
                while (j--) {
                    this._resetBufferLine(j);
                }
                this._terminal.updateRange(0);
                break;
            case 3:
                var scrollBackSize = this._terminal.buffer.lines.length - this._terminal.rows;
                if (scrollBackSize > 0) {
                    this._terminal.buffer.lines.trimStart(scrollBackSize);
                    this._terminal.buffer.ybase = Math.max(this._terminal.buffer.ybase - scrollBackSize, 0);
                    this._terminal.buffer.ydisp = Math.max(this._terminal.buffer.ydisp - scrollBackSize, 0);
                    this._terminal.emit('scroll', 0);
                }
                break;
        }
    };
    InputHandler.prototype.eraseInLine = function (params) {
        switch (params[0]) {
            case 0:
                this._eraseInBufferLine(this._terminal.buffer.y, this._terminal.buffer.x, this._terminal.cols);
                break;
            case 1:
                this._eraseInBufferLine(this._terminal.buffer.y, 0, this._terminal.buffer.x + 1);
                break;
            case 2:
                this._eraseInBufferLine(this._terminal.buffer.y, 0, this._terminal.cols);
                break;
        }
        this._terminal.updateRange(this._terminal.buffer.y);
    };
    InputHandler.prototype.insertLines = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        var buffer = this._terminal.buffer;
        var row = buffer.y + buffer.ybase;
        var scrollBottomRowsOffset = this._terminal.rows - 1 - buffer.scrollBottom;
        var scrollBottomAbsolute = this._terminal.rows - 1 + buffer.ybase - scrollBottomRowsOffset + 1;
        while (param--) {
            buffer.lines.splice(scrollBottomAbsolute - 1, 1);
            buffer.lines.splice(row, 0, buffer.getBlankLine(this._terminal.eraseAttr()));
        }
        this._terminal.updateRange(buffer.y);
        this._terminal.updateRange(buffer.scrollBottom);
    };
    InputHandler.prototype.deleteLines = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        var buffer = this._terminal.buffer;
        var row = buffer.y + buffer.ybase;
        var j;
        j = this._terminal.rows - 1 - buffer.scrollBottom;
        j = this._terminal.rows - 1 + buffer.ybase - j;
        while (param--) {
            buffer.lines.splice(row, 1);
            buffer.lines.splice(j, 0, buffer.getBlankLine(this._terminal.eraseAttr()));
        }
        this._terminal.updateRange(buffer.y);
        this._terminal.updateRange(buffer.scrollBottom);
    };
    InputHandler.prototype.deleteChars = function (params) {
        this._terminal.buffer.lines.get(this._terminal.buffer.y + this._terminal.buffer.ybase).deleteCells(this._terminal.buffer.x, params[0] || 1, [this._terminal.eraseAttr(), Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
        this._terminal.updateRange(this._terminal.buffer.y);
    };
    InputHandler.prototype.scrollUp = function (params) {
        var param = params[0] || 1;
        var buffer = this._terminal.buffer;
        while (param--) {
            buffer.lines.splice(buffer.ybase + buffer.scrollTop, 1);
            buffer.lines.splice(buffer.ybase + buffer.scrollBottom, 0, buffer.getBlankLine(Buffer_1.DEFAULT_ATTR));
        }
        this._terminal.updateRange(buffer.scrollTop);
        this._terminal.updateRange(buffer.scrollBottom);
    };
    InputHandler.prototype.scrollDown = function (params, collect) {
        if (params.length < 2 && !collect) {
            var param = params[0] || 1;
            var buffer = this._terminal.buffer;
            while (param--) {
                buffer.lines.splice(buffer.ybase + buffer.scrollBottom, 1);
                buffer.lines.splice(buffer.ybase + buffer.scrollBottom, 0, buffer.getBlankLine(Buffer_1.DEFAULT_ATTR));
            }
            this._terminal.updateRange(buffer.scrollTop);
            this._terminal.updateRange(buffer.scrollBottom);
        }
    };
    InputHandler.prototype.eraseChars = function (params) {
        this._terminal.buffer.lines.get(this._terminal.buffer.y + this._terminal.buffer.ybase).replaceCells(this._terminal.buffer.x, this._terminal.buffer.x + (params[0] || 1), [this._terminal.eraseAttr(), Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
    };
    InputHandler.prototype.cursorBackwardTab = function (params) {
        var param = params[0] || 1;
        var buffer = this._terminal.buffer;
        while (param--) {
            buffer.x = buffer.prevStop();
        }
    };
    InputHandler.prototype.charPosAbsolute = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.x = param - 1;
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x = this._terminal.cols - 1;
        }
    };
    InputHandler.prototype.hPositionRelative = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.x += param;
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x = this._terminal.cols - 1;
        }
    };
    InputHandler.prototype.repeatPrecedingCharacter = function (params) {
        var buffer = this._terminal.buffer;
        var line = buffer.lines.get(buffer.ybase + buffer.y);
        line.replaceCells(buffer.x, buffer.x + (params[0] || 1), line.get(buffer.x - 1) || [Buffer_1.DEFAULT_ATTR, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE]);
    };
    InputHandler.prototype.sendDeviceAttributes = function (params, collect) {
        if (params[0] > 0) {
            return;
        }
        if (!collect) {
            if (this._terminal.is('xterm') || this._terminal.is('rxvt-unicode') || this._terminal.is('screen')) {
                this._terminal.handler(EscapeSequences_1.C0.ESC + '[?1;2c');
            }
            else if (this._terminal.is('linux')) {
                this._terminal.handler(EscapeSequences_1.C0.ESC + '[?6c');
            }
        }
        else if (collect === '>') {
            if (this._terminal.is('xterm')) {
                this._terminal.handler(EscapeSequences_1.C0.ESC + '[>0;276;0c');
            }
            else if (this._terminal.is('rxvt-unicode')) {
                this._terminal.handler(EscapeSequences_1.C0.ESC + '[>85;95;0c');
            }
            else if (this._terminal.is('linux')) {
                this._terminal.handler(params[0] + 'c');
            }
            else if (this._terminal.is('screen')) {
                this._terminal.handler(EscapeSequences_1.C0.ESC + '[>83;40003;0c');
            }
        }
    };
    InputHandler.prototype.linePosAbsolute = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y = param - 1;
        if (this._terminal.buffer.y >= this._terminal.rows) {
            this._terminal.buffer.y = this._terminal.rows - 1;
        }
    };
    InputHandler.prototype.vPositionRelative = function (params) {
        var param = params[0];
        if (param < 1) {
            param = 1;
        }
        this._terminal.buffer.y += param;
        if (this._terminal.buffer.y >= this._terminal.rows) {
            this._terminal.buffer.y = this._terminal.rows - 1;
        }
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x--;
        }
    };
    InputHandler.prototype.hVPosition = function (params) {
        if (params[0] < 1)
            params[0] = 1;
        if (params[1] < 1)
            params[1] = 1;
        this._terminal.buffer.y = params[0] - 1;
        if (this._terminal.buffer.y >= this._terminal.rows) {
            this._terminal.buffer.y = this._terminal.rows - 1;
        }
        this._terminal.buffer.x = params[1] - 1;
        if (this._terminal.buffer.x >= this._terminal.cols) {
            this._terminal.buffer.x = this._terminal.cols - 1;
        }
    };
    InputHandler.prototype.tabClear = function (params) {
        var param = params[0];
        if (param <= 0) {
            delete this._terminal.buffer.tabs[this._terminal.buffer.x];
        }
        else if (param === 3) {
            this._terminal.buffer.tabs = {};
        }
    };
    InputHandler.prototype.setMode = function (params, collect) {
        if (params.length > 1) {
            for (var i = 0; i < params.length; i++) {
                this.setMode([params[i]]);
            }
            return;
        }
        if (!collect) {
            switch (params[0]) {
                case 4:
                    this._terminal.insertMode = true;
                    break;
                case 20:
                    break;
            }
        }
        else if (collect === '?') {
            switch (params[0]) {
                case 1:
                    this._terminal.applicationCursor = true;
                    break;
                case 2:
                    this._terminal.setgCharset(0, Charsets_1.DEFAULT_CHARSET);
                    this._terminal.setgCharset(1, Charsets_1.DEFAULT_CHARSET);
                    this._terminal.setgCharset(2, Charsets_1.DEFAULT_CHARSET);
                    this._terminal.setgCharset(3, Charsets_1.DEFAULT_CHARSET);
                    break;
                case 3:
                    this._terminal.savedCols = this._terminal.cols;
                    this._terminal.resize(132, this._terminal.rows);
                    break;
                case 6:
                    this._terminal.originMode = true;
                    break;
                case 7:
                    this._terminal.wraparoundMode = true;
                    break;
                case 12:
                    break;
                case 66:
                    this._terminal.log('Serial port requested application keypad.');
                    this._terminal.applicationKeypad = true;
                    if (this._terminal.viewport) {
                        this._terminal.viewport.syncScrollArea();
                    }
                    break;
                case 9:
                case 1000:
                case 1002:
                case 1003:
                    this._terminal.x10Mouse = params[0] === 9;
                    this._terminal.vt200Mouse = params[0] === 1000;
                    this._terminal.normalMouse = params[0] > 1000;
                    this._terminal.mouseEvents = true;
                    this._terminal.element.classList.add('enable-mouse-events');
                    this._terminal.selectionManager.disable();
                    this._terminal.log('Binding to mouse events.');
                    break;
                case 1004:
                    this._terminal.sendFocus = true;
                    break;
                case 1005:
                    this._terminal.utfMouse = true;
                    break;
                case 1006:
                    this._terminal.sgrMouse = true;
                    break;
                case 1015:
                    this._terminal.urxvtMouse = true;
                    break;
                case 25:
                    this._terminal.cursorHidden = false;
                    break;
                case 1048:
                    this.saveCursor(params);
                    break;
                case 1049:
                    this.saveCursor(params);
                case 47:
                case 1047:
                    this._terminal.buffers.activateAltBuffer(this._terminal.eraseAttr());
                    this._terminal.refresh(0, this._terminal.rows - 1);
                    if (this._terminal.viewport) {
                        this._terminal.viewport.syncScrollArea();
                    }
                    this._terminal.showCursor();
                    break;
                case 2004:
                    this._terminal.bracketedPasteMode = true;
                    break;
            }
        }
    };
    InputHandler.prototype.resetMode = function (params, collect) {
        if (params.length > 1) {
            for (var i = 0; i < params.length; i++) {
                this.resetMode([params[i]]);
            }
            return;
        }
        if (!collect) {
            switch (params[0]) {
                case 4:
                    this._terminal.insertMode = false;
                    break;
                case 20:
                    break;
            }
        }
        else if (collect === '?') {
            switch (params[0]) {
                case 1:
                    this._terminal.applicationCursor = false;
                    break;
                case 3:
                    if (this._terminal.cols === 132 && this._terminal.savedCols) {
                        this._terminal.resize(this._terminal.savedCols, this._terminal.rows);
                    }
                    delete this._terminal.savedCols;
                    break;
                case 6:
                    this._terminal.originMode = false;
                    break;
                case 7:
                    this._terminal.wraparoundMode = false;
                    break;
                case 12:
                    break;
                case 66:
                    this._terminal.log('Switching back to normal keypad.');
                    this._terminal.applicationKeypad = false;
                    if (this._terminal.viewport) {
                        this._terminal.viewport.syncScrollArea();
                    }
                    break;
                case 9:
                case 1000:
                case 1002:
                case 1003:
                    this._terminal.x10Mouse = false;
                    this._terminal.vt200Mouse = false;
                    this._terminal.normalMouse = false;
                    this._terminal.mouseEvents = false;
                    this._terminal.element.classList.remove('enable-mouse-events');
                    this._terminal.selectionManager.enable();
                    break;
                case 1004:
                    this._terminal.sendFocus = false;
                    break;
                case 1005:
                    this._terminal.utfMouse = false;
                    break;
                case 1006:
                    this._terminal.sgrMouse = false;
                    break;
                case 1015:
                    this._terminal.urxvtMouse = false;
                    break;
                case 25:
                    this._terminal.cursorHidden = true;
                    break;
                case 1048:
                    this.restoreCursor(params);
                    break;
                case 1049:
                case 47:
                case 1047:
                    this._terminal.buffers.activateNormalBuffer();
                    if (params[0] === 1049) {
                        this.restoreCursor(params);
                    }
                    this._terminal.refresh(0, this._terminal.rows - 1);
                    if (this._terminal.viewport) {
                        this._terminal.viewport.syncScrollArea();
                    }
                    this._terminal.showCursor();
                    break;
                case 2004:
                    this._terminal.bracketedPasteMode = false;
                    break;
            }
        }
    };
    InputHandler.prototype.charAttributes = function (params) {
        if (params.length === 1 && params[0] === 0) {
            this._terminal.curAttr = Buffer_1.DEFAULT_ATTR;
            return;
        }
        var l = params.length;
        var flags = this._terminal.curAttr >> 18;
        var fg = (this._terminal.curAttr >> 9) & 0x1ff;
        var bg = this._terminal.curAttr & 0x1ff;
        var p;
        for (var i = 0; i < l; i++) {
            p = params[i];
            if (p >= 30 && p <= 37) {
                fg = p - 30;
            }
            else if (p >= 40 && p <= 47) {
                bg = p - 40;
            }
            else if (p >= 90 && p <= 97) {
                p += 8;
                fg = p - 90;
            }
            else if (p >= 100 && p <= 107) {
                p += 8;
                bg = p - 100;
            }
            else if (p === 0) {
                flags = Buffer_1.DEFAULT_ATTR >> 18;
                fg = (Buffer_1.DEFAULT_ATTR >> 9) & 0x1ff;
                bg = Buffer_1.DEFAULT_ATTR & 0x1ff;
            }
            else if (p === 1) {
                flags |= 1;
            }
            else if (p === 3) {
                flags |= 64;
            }
            else if (p === 4) {
                flags |= 2;
            }
            else if (p === 5) {
                flags |= 4;
            }
            else if (p === 7) {
                flags |= 8;
            }
            else if (p === 8) {
                flags |= 16;
            }
            else if (p === 2) {
                flags |= 32;
            }
            else if (p === 22) {
                flags &= ~1;
                flags &= ~32;
            }
            else if (p === 23) {
                flags &= ~64;
            }
            else if (p === 24) {
                flags &= ~2;
            }
            else if (p === 25) {
                flags &= ~4;
            }
            else if (p === 27) {
                flags &= ~8;
            }
            else if (p === 28) {
                flags &= ~16;
            }
            else if (p === 39) {
                fg = (Buffer_1.DEFAULT_ATTR >> 9) & 0x1ff;
            }
            else if (p === 49) {
                bg = Buffer_1.DEFAULT_ATTR & 0x1ff;
            }
            else if (p === 38) {
                if (params[i + 1] === 2) {
                    i += 2;
                    fg = this._terminal.matchColor(params[i] & 0xff, params[i + 1] & 0xff, params[i + 2] & 0xff);
                    if (fg === -1)
                        fg = 0x1ff;
                    i += 2;
                }
                else if (params[i + 1] === 5) {
                    i += 2;
                    p = params[i] & 0xff;
                    fg = p;
                }
            }
            else if (p === 48) {
                if (params[i + 1] === 2) {
                    i += 2;
                    bg = this._terminal.matchColor(params[i] & 0xff, params[i + 1] & 0xff, params[i + 2] & 0xff);
                    if (bg === -1)
                        bg = 0x1ff;
                    i += 2;
                }
                else if (params[i + 1] === 5) {
                    i += 2;
                    p = params[i] & 0xff;
                    bg = p;
                }
            }
            else if (p === 100) {
                fg = (Buffer_1.DEFAULT_ATTR >> 9) & 0x1ff;
                bg = Buffer_1.DEFAULT_ATTR & 0x1ff;
            }
            else {
                this._terminal.error('Unknown SGR attribute: %d.', p);
            }
        }
        this._terminal.curAttr = (flags << 18) | (fg << 9) | bg;
    };
    InputHandler.prototype.deviceStatus = function (params, collect) {
        if (!collect) {
            switch (params[0]) {
                case 5:
                    this._terminal.emit('data', EscapeSequences_1.C0.ESC + "[0n");
                    break;
                case 6:
                    var y = this._terminal.buffer.y + 1;
                    var x = this._terminal.buffer.x + 1;
                    this._terminal.emit('data', EscapeSequences_1.C0.ESC + "[" + y + ";" + x + "R");
                    break;
            }
        }
        else if (collect === '?') {
            switch (params[0]) {
                case 6:
                    var y = this._terminal.buffer.y + 1;
                    var x = this._terminal.buffer.x + 1;
                    this._terminal.emit('data', EscapeSequences_1.C0.ESC + "[?" + y + ";" + x + "R");
                    break;
                case 15:
                    break;
                case 25:
                    break;
                case 26:
                    break;
                case 53:
                    break;
            }
        }
    };
    InputHandler.prototype.softReset = function (params, collect) {
        if (collect === '!') {
            this._terminal.cursorHidden = false;
            this._terminal.insertMode = false;
            this._terminal.originMode = false;
            this._terminal.wraparoundMode = true;
            this._terminal.applicationKeypad = false;
            if (this._terminal.viewport) {
                this._terminal.viewport.syncScrollArea();
            }
            this._terminal.applicationCursor = false;
            this._terminal.buffer.scrollTop = 0;
            this._terminal.buffer.scrollBottom = this._terminal.rows - 1;
            this._terminal.curAttr = Buffer_1.DEFAULT_ATTR;
            this._terminal.buffer.x = this._terminal.buffer.y = 0;
            this._terminal.charset = null;
            this._terminal.glevel = 0;
            this._terminal.charsets = [null];
        }
    };
    InputHandler.prototype.setCursorStyle = function (params, collect) {
        if (collect === ' ') {
            var param = params[0] < 1 ? 1 : params[0];
            switch (param) {
                case 1:
                case 2:
                    this._terminal.setOption('cursorStyle', 'block');
                    break;
                case 3:
                case 4:
                    this._terminal.setOption('cursorStyle', 'underline');
                    break;
                case 5:
                case 6:
                    this._terminal.setOption('cursorStyle', 'bar');
                    break;
            }
            var isBlinking = param % 2 === 1;
            this._terminal.setOption('cursorBlink', isBlinking);
        }
    };
    InputHandler.prototype.setScrollRegion = function (params, collect) {
        if (collect)
            return;
        this._terminal.buffer.scrollTop = (params[0] || 1) - 1;
        this._terminal.buffer.scrollBottom = (params[1] && params[1] <= this._terminal.rows ? params[1] : this._terminal.rows) - 1;
        this._terminal.buffer.x = 0;
        this._terminal.buffer.y = 0;
    };
    InputHandler.prototype.saveCursor = function (params) {
        this._terminal.buffer.savedX = this._terminal.buffer.x;
        this._terminal.buffer.savedY = this._terminal.buffer.y;
        this._terminal.buffer.savedCurAttr = this._terminal.curAttr;
    };
    InputHandler.prototype.restoreCursor = function (params) {
        this._terminal.buffer.x = this._terminal.buffer.savedX || 0;
        this._terminal.buffer.y = this._terminal.buffer.savedY || 0;
        this._terminal.curAttr = this._terminal.buffer.savedCurAttr || Buffer_1.DEFAULT_ATTR;
    };
    InputHandler.prototype.setTitle = function (data) {
        this._terminal.handleTitle(data);
    };
    InputHandler.prototype.nextLine = function () {
        this._terminal.buffer.x = 0;
        this.index();
    };
    InputHandler.prototype.keypadApplicationMode = function () {
        this._terminal.log('Serial port requested application keypad.');
        this._terminal.applicationKeypad = true;
        if (this._terminal.viewport) {
            this._terminal.viewport.syncScrollArea();
        }
    };
    InputHandler.prototype.keypadNumericMode = function () {
        this._terminal.log('Switching back to normal keypad.');
        this._terminal.applicationKeypad = false;
        if (this._terminal.viewport) {
            this._terminal.viewport.syncScrollArea();
        }
    };
    InputHandler.prototype.selectDefaultCharset = function () {
        this._terminal.setgLevel(0);
        this._terminal.setgCharset(0, Charsets_1.DEFAULT_CHARSET);
    };
    InputHandler.prototype.selectCharset = function (collectAndFlag) {
        if (collectAndFlag.length !== 2)
            return this.selectDefaultCharset();
        if (collectAndFlag[0] === '/')
            return;
        this._terminal.setgCharset(GLEVEL[collectAndFlag[0]], Charsets_1.CHARSETS[collectAndFlag[1]] || Charsets_1.DEFAULT_CHARSET);
    };
    InputHandler.prototype.index = function () {
        this._terminal.index();
    };
    InputHandler.prototype.tabSet = function () {
        this._terminal.tabSet();
    };
    InputHandler.prototype.reverseIndex = function () {
        this._terminal.reverseIndex();
    };
    InputHandler.prototype.reset = function () {
        this._parser.reset();
        this._terminal.reset();
    };
    InputHandler.prototype.setgLevel = function (level) {
        this._terminal.setgLevel(level);
    };
    return InputHandler;
}(Lifecycle_1.Disposable));
exports.InputHandler = InputHandler;

},{"./Buffer":2,"./CharWidth":5,"./EscapeSequenceParser":7,"./common/Lifecycle":18,"./common/data/EscapeSequences":20,"./core/data/Charsets":22}],9:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var MouseZoneManager_1 = require("./ui/MouseZoneManager");
var EventEmitter_1 = require("./common/EventEmitter");
var Buffer_1 = require("./Buffer");
var CharWidth_1 = require("./CharWidth");
var Linkifier = (function (_super) {
    __extends(Linkifier, _super);
    function Linkifier(_terminal) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._linkMatchers = [];
        _this._nextLinkMatcherId = 0;
        _this._rowsToLinkify = {
            start: null,
            end: null
        };
        return _this;
    }
    Linkifier.prototype.attachToDom = function (mouseZoneManager) {
        this._mouseZoneManager = mouseZoneManager;
    };
    Linkifier.prototype.linkifyRows = function (start, end) {
        var _this = this;
        if (!this._mouseZoneManager) {
            return;
        }
        if (this._rowsToLinkify.start === null) {
            this._rowsToLinkify.start = start;
            this._rowsToLinkify.end = end;
        }
        else {
            this._rowsToLinkify.start = Math.min(this._rowsToLinkify.start, start);
            this._rowsToLinkify.end = Math.max(this._rowsToLinkify.end, end);
        }
        this._mouseZoneManager.clearAll(start, end);
        if (this._rowsTimeoutId) {
            clearTimeout(this._rowsTimeoutId);
        }
        this._rowsTimeoutId = setTimeout(function () { return _this._linkifyRows(); }, Linkifier.TIME_BEFORE_LINKIFY);
    };
    Linkifier.prototype._linkifyRows = function () {
        this._rowsTimeoutId = null;
        var buffer = this._terminal.buffer;
        var absoluteRowIndexStart = buffer.ydisp + this._rowsToLinkify.start;
        if (absoluteRowIndexStart >= buffer.lines.length) {
            return;
        }
        var absoluteRowIndexEnd = buffer.ydisp + Math.min(this._rowsToLinkify.end, this._terminal.rows) + 1;
        var overscanLineLimit = Math.ceil(Linkifier.OVERSCAN_CHAR_LIMIT / this._terminal.cols);
        var iterator = this._terminal.buffer.iterator(false, absoluteRowIndexStart, absoluteRowIndexEnd, overscanLineLimit, overscanLineLimit);
        while (iterator.hasNext()) {
            var lineData = iterator.next();
            for (var i = 0; i < this._linkMatchers.length; i++) {
                this._doLinkifyRow(lineData.range.first, lineData.content, this._linkMatchers[i]);
            }
        }
        this._rowsToLinkify.start = null;
        this._rowsToLinkify.end = null;
    };
    Linkifier.prototype.registerLinkMatcher = function (regex, handler, options) {
        if (options === void 0) { options = {}; }
        if (!handler) {
            throw new Error('handler must be defined');
        }
        var matcher = {
            id: this._nextLinkMatcherId++,
            regex: regex,
            handler: handler,
            matchIndex: options.matchIndex,
            validationCallback: options.validationCallback,
            hoverTooltipCallback: options.tooltipCallback,
            hoverLeaveCallback: options.leaveCallback,
            willLinkActivate: options.willLinkActivate,
            priority: options.priority || 0
        };
        this._addLinkMatcherToList(matcher);
        return matcher.id;
    };
    Linkifier.prototype._addLinkMatcherToList = function (matcher) {
        if (this._linkMatchers.length === 0) {
            this._linkMatchers.push(matcher);
            return;
        }
        for (var i = this._linkMatchers.length - 1; i >= 0; i--) {
            if (matcher.priority <= this._linkMatchers[i].priority) {
                this._linkMatchers.splice(i + 1, 0, matcher);
                return;
            }
        }
        this._linkMatchers.splice(0, 0, matcher);
    };
    Linkifier.prototype.deregisterLinkMatcher = function (matcherId) {
        for (var i = 0; i < this._linkMatchers.length; i++) {
            if (this._linkMatchers[i].id === matcherId) {
                this._linkMatchers.splice(i, 1);
                return true;
            }
        }
        return false;
    };
    Linkifier.prototype._doLinkifyRow = function (rowIndex, text, matcher) {
        var _this = this;
        var rex = new RegExp(matcher.regex.source, matcher.regex.flags + 'g');
        var match;
        var stringIndex = -1;
        var _loop_1 = function () {
            var uri = match[typeof matcher.matchIndex !== 'number' ? 0 : matcher.matchIndex];
            if (!uri) {
                if (this_1._terminal.debug) {
                    console.log({ match: match, matcher: matcher });
                    throw new Error('match found without corresponding matchIndex');
                }
                return "break";
            }
            stringIndex = text.indexOf(uri, stringIndex + 1);
            rex.lastIndex = stringIndex + uri.length;
            var bufferIndex = this_1._terminal.buffer.stringIndexToBufferIndex(rowIndex, stringIndex);
            var line = this_1._terminal.buffer.lines.get(bufferIndex[0]);
            var char = line.get(bufferIndex[1]);
            var fg;
            if (char) {
                var attr = char[Buffer_1.CHAR_DATA_ATTR_INDEX];
                fg = (attr >> 9) & 0x1ff;
            }
            if (matcher.validationCallback) {
                matcher.validationCallback(uri, function (isValid) {
                    if (_this._rowsTimeoutId) {
                        return;
                    }
                    if (isValid) {
                        _this._addLink(bufferIndex[1], bufferIndex[0] - _this._terminal.buffer.ydisp, uri, matcher, fg);
                    }
                });
            }
            else {
                this_1._addLink(bufferIndex[1], bufferIndex[0] - this_1._terminal.buffer.ydisp, uri, matcher, fg);
            }
        };
        var this_1 = this;
        while ((match = rex.exec(text)) !== null) {
            var state_1 = _loop_1();
            if (state_1 === "break")
                break;
        }
    };
    Linkifier.prototype._addLink = function (x, y, uri, matcher, fg) {
        var _this = this;
        var width = CharWidth_1.getStringCellWidth(uri);
        var x1 = x % this._terminal.cols;
        var y1 = y + Math.floor(x / this._terminal.cols);
        var x2 = (x1 + width) % this._terminal.cols;
        var y2 = y1 + Math.floor((x1 + width) / this._terminal.cols);
        if (x2 === 0) {
            x2 = this._terminal.cols;
            y2--;
        }
        this._mouseZoneManager.add(new MouseZoneManager_1.MouseZone(x1 + 1, y1 + 1, x2 + 1, y2 + 1, function (e) {
            if (matcher.handler) {
                return matcher.handler(e, uri);
            }
            window.open(uri, '_blank');
        }, function (e) {
            _this.emit("linkhover", _this._createLinkHoverEvent(x1, y1, x2, y2, fg));
            _this._terminal.element.classList.add('xterm-cursor-pointer');
        }, function (e) {
            _this.emit("linktooltip", _this._createLinkHoverEvent(x1, y1, x2, y2, fg));
            if (matcher.hoverTooltipCallback) {
                matcher.hoverTooltipCallback(e, uri);
            }
        }, function () {
            _this.emit("linkleave", _this._createLinkHoverEvent(x1, y1, x2, y2, fg));
            _this._terminal.element.classList.remove('xterm-cursor-pointer');
            if (matcher.hoverLeaveCallback) {
                matcher.hoverLeaveCallback();
            }
        }, function (e) {
            if (matcher.willLinkActivate) {
                return matcher.willLinkActivate(e, uri);
            }
            return true;
        }));
    };
    Linkifier.prototype._createLinkHoverEvent = function (x1, y1, x2, y2, fg) {
        return { x1: x1, y1: y1, x2: x2, y2: y2, cols: this._terminal.cols, fg: fg };
    };
    Linkifier.TIME_BEFORE_LINKIFY = 200;
    Linkifier.OVERSCAN_CHAR_LIMIT = 2000;
    return Linkifier;
}(EventEmitter_1.EventEmitter));
exports.Linkifier = Linkifier;

},{"./Buffer":2,"./CharWidth":5,"./common/EventEmitter":17,"./ui/MouseZoneManager":49}],10:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var MouseHelper_1 = require("./utils/MouseHelper");
var Browser = require("./core/Platform");
var EventEmitter_1 = require("./common/EventEmitter");
var SelectionModel_1 = require("./SelectionModel");
var Buffer_1 = require("./Buffer");
var AltClickHandler_1 = require("./handlers/AltClickHandler");
var DRAG_SCROLL_MAX_THRESHOLD = 50;
var DRAG_SCROLL_MAX_SPEED = 15;
var DRAG_SCROLL_INTERVAL = 50;
var ALT_CLICK_MOVE_CURSOR_TIME = 500;
var WORD_SEPARATORS = ' ()[]{}\'"';
var NON_BREAKING_SPACE_CHAR = String.fromCharCode(160);
var ALL_NON_BREAKING_SPACE_REGEX = new RegExp(NON_BREAKING_SPACE_CHAR, 'g');
var SelectionManager = (function (_super) {
    __extends(SelectionManager, _super);
    function SelectionManager(_terminal, _charMeasure) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._charMeasure = _charMeasure;
        _this._enabled = true;
        _this._initListeners();
        _this.enable();
        _this._model = new SelectionModel_1.SelectionModel(_terminal);
        _this._activeSelectionMode = 0;
        return _this;
    }
    SelectionManager.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._removeMouseDownListeners();
    };
    Object.defineProperty(SelectionManager.prototype, "_buffer", {
        get: function () {
            return this._terminal.buffers.active;
        },
        enumerable: true,
        configurable: true
    });
    SelectionManager.prototype._initListeners = function () {
        var _this = this;
        this._mouseMoveListener = function (event) { return _this._onMouseMove(event); };
        this._mouseUpListener = function (event) { return _this._onMouseUp(event); };
        this._trimListener = function (amount) { return _this._onTrim(amount); };
        this.initBuffersListeners();
    };
    SelectionManager.prototype.initBuffersListeners = function () {
        var _this = this;
        this._terminal.buffer.lines.on('trim', this._trimListener);
        this._terminal.buffers.on('activate', function (e) { return _this._onBufferActivate(e); });
    };
    SelectionManager.prototype.disable = function () {
        this.clearSelection();
        this._enabled = false;
    };
    SelectionManager.prototype.enable = function () {
        this._enabled = true;
    };
    Object.defineProperty(SelectionManager.prototype, "selectionStart", {
        get: function () { return this._model.finalSelectionStart; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SelectionManager.prototype, "selectionEnd", {
        get: function () { return this._model.finalSelectionEnd; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SelectionManager.prototype, "hasSelection", {
        get: function () {
            var start = this._model.finalSelectionStart;
            var end = this._model.finalSelectionEnd;
            if (!start || !end) {
                return false;
            }
            return start[0] !== end[0] || start[1] !== end[1];
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SelectionManager.prototype, "selectionText", {
        get: function () {
            var start = this._model.finalSelectionStart;
            var end = this._model.finalSelectionEnd;
            if (!start || !end) {
                return '';
            }
            var result = [];
            if (this._activeSelectionMode === 3) {
                if (start[0] === end[0]) {
                    return '';
                }
                for (var i = start[1]; i <= end[1]; i++) {
                    var lineText = this._buffer.translateBufferLineToString(i, true, start[0], end[0]);
                    result.push(lineText);
                }
            }
            else {
                var startRowEndCol = start[1] === end[1] ? end[0] : null;
                result.push(this._buffer.translateBufferLineToString(start[1], true, start[0], startRowEndCol));
                for (var i = start[1] + 1; i <= end[1] - 1; i++) {
                    var bufferLine = this._buffer.lines.get(i);
                    var lineText = this._buffer.translateBufferLineToString(i, true);
                    if (bufferLine.isWrapped) {
                        result[result.length - 1] += lineText;
                    }
                    else {
                        result.push(lineText);
                    }
                }
                if (start[1] !== end[1]) {
                    var bufferLine = this._buffer.lines.get(end[1]);
                    var lineText = this._buffer.translateBufferLineToString(end[1], true, 0, end[0]);
                    if (bufferLine.isWrapped) {
                        result[result.length - 1] += lineText;
                    }
                    else {
                        result.push(lineText);
                    }
                }
            }
            var formattedResult = result.map(function (line) {
                return line.replace(ALL_NON_BREAKING_SPACE_REGEX, ' ');
            }).join(Browser.isMSWindows ? '\r\n' : '\n');
            return formattedResult;
        },
        enumerable: true,
        configurable: true
    });
    SelectionManager.prototype.clearSelection = function () {
        this._model.clearSelection();
        this._removeMouseDownListeners();
        this.refresh();
    };
    SelectionManager.prototype.refresh = function (isNewSelection) {
        var _this = this;
        if (!this._refreshAnimationFrame) {
            this._refreshAnimationFrame = window.requestAnimationFrame(function () { return _this._refresh(); });
        }
        if (Browser.isLinux && isNewSelection) {
            var selectionText = this.selectionText;
            if (selectionText.length) {
                this.emit('newselection', this.selectionText);
            }
        }
    };
    SelectionManager.prototype._refresh = function () {
        this._refreshAnimationFrame = null;
        this.emit('refresh', {
            start: this._model.finalSelectionStart,
            end: this._model.finalSelectionEnd,
            columnSelectMode: this._activeSelectionMode === 3
        });
    };
    SelectionManager.prototype.isClickInSelection = function (event) {
        var coords = this._getMouseBufferCoords(event);
        var start = this._model.finalSelectionStart;
        var end = this._model.finalSelectionEnd;
        if (!start || !end) {
            return false;
        }
        return this._areCoordsInSelection(coords, start, end);
    };
    SelectionManager.prototype._areCoordsInSelection = function (coords, start, end) {
        return (coords[1] > start[1] && coords[1] < end[1]) ||
            (start[1] === end[1] && coords[1] === start[1] && coords[0] >= start[0] && coords[0] < end[0]) ||
            (start[1] < end[1] && coords[1] === end[1] && coords[0] < end[0]) ||
            (start[1] < end[1] && coords[1] === start[1] && coords[0] >= start[0]);
    };
    SelectionManager.prototype.selectWordAtCursor = function (event) {
        var coords = this._getMouseBufferCoords(event);
        if (coords) {
            this._selectWordAt(coords, false);
            this._model.selectionEnd = null;
            this.refresh(true);
        }
    };
    SelectionManager.prototype.selectAll = function () {
        this._model.isSelectAllActive = true;
        this.refresh();
        this._terminal.emit('selection');
    };
    SelectionManager.prototype.selectLines = function (start, end) {
        this._model.clearSelection();
        start = Math.max(start, 0);
        end = Math.min(end, this._terminal.buffer.lines.length - 1);
        this._model.selectionStart = [0, start];
        this._model.selectionEnd = [this._terminal.cols, end];
        this.refresh();
        this._terminal.emit('selection');
    };
    SelectionManager.prototype._onTrim = function (amount) {
        var needsRefresh = this._model.onTrim(amount);
        if (needsRefresh) {
            this.refresh();
        }
    };
    SelectionManager.prototype._getMouseBufferCoords = function (event) {
        var coords = this._terminal.mouseHelper.getCoords(event, this._terminal.screenElement, this._charMeasure, this._terminal.cols, this._terminal.rows, true);
        if (!coords) {
            return null;
        }
        coords[0]--;
        coords[1]--;
        coords[1] += this._terminal.buffer.ydisp;
        return coords;
    };
    SelectionManager.prototype._getMouseEventScrollAmount = function (event) {
        var offset = MouseHelper_1.MouseHelper.getCoordsRelativeToElement(event, this._terminal.screenElement)[1];
        var terminalHeight = this._terminal.rows * Math.ceil(this._charMeasure.height * this._terminal.options.lineHeight);
        if (offset >= 0 && offset <= terminalHeight) {
            return 0;
        }
        if (offset > terminalHeight) {
            offset -= terminalHeight;
        }
        offset = Math.min(Math.max(offset, -DRAG_SCROLL_MAX_THRESHOLD), DRAG_SCROLL_MAX_THRESHOLD);
        offset /= DRAG_SCROLL_MAX_THRESHOLD;
        return (offset / Math.abs(offset)) + Math.round(offset * (DRAG_SCROLL_MAX_SPEED - 1));
    };
    SelectionManager.prototype.shouldForceSelection = function (event) {
        if (Browser.isMac) {
            return event.altKey && this._terminal.options.macOptionClickForcesSelection;
        }
        return event.shiftKey;
    };
    SelectionManager.prototype.onMouseDown = function (event) {
        this._mouseDownTimeStamp = event.timeStamp;
        if (event.button === 2 && this.hasSelection) {
            return;
        }
        if (event.button !== 0) {
            return;
        }
        if (!this._enabled) {
            if (!this.shouldForceSelection(event)) {
                return;
            }
            event.stopPropagation();
        }
        event.preventDefault();
        this._dragScrollAmount = 0;
        if (this._enabled && event.shiftKey) {
            this._onIncrementalClick(event);
        }
        else {
            if (event.detail === 1) {
                this._onSingleClick(event);
            }
            else if (event.detail === 2) {
                this._onDoubleClick(event);
            }
            else if (event.detail === 3) {
                this._onTripleClick(event);
            }
        }
        this._addMouseDownListeners();
        this.refresh(true);
    };
    SelectionManager.prototype._addMouseDownListeners = function () {
        var _this = this;
        this._terminal.element.ownerDocument.addEventListener('mousemove', this._mouseMoveListener);
        this._terminal.element.ownerDocument.addEventListener('mouseup', this._mouseUpListener);
        this._dragScrollIntervalTimer = setInterval(function () { return _this._dragScroll(); }, DRAG_SCROLL_INTERVAL);
    };
    SelectionManager.prototype._removeMouseDownListeners = function () {
        if (this._terminal.element.ownerDocument) {
            this._terminal.element.ownerDocument.removeEventListener('mousemove', this._mouseMoveListener);
            this._terminal.element.ownerDocument.removeEventListener('mouseup', this._mouseUpListener);
        }
        clearInterval(this._dragScrollIntervalTimer);
        this._dragScrollIntervalTimer = null;
    };
    SelectionManager.prototype._onIncrementalClick = function (event) {
        if (this._model.selectionStart) {
            this._model.selectionEnd = this._getMouseBufferCoords(event);
        }
    };
    SelectionManager.prototype._onSingleClick = function (event) {
        this._model.selectionStartLength = 0;
        this._model.isSelectAllActive = false;
        this._activeSelectionMode = this.shouldColumnSelect(event) ? 3 : 0;
        this._model.selectionStart = this._getMouseBufferCoords(event);
        if (!this._model.selectionStart) {
            return;
        }
        this._model.selectionEnd = null;
        var line = this._buffer.lines.get(this._model.selectionStart[1]);
        if (!line) {
            return;
        }
        if (line.length >= this._model.selectionStart[0]) {
            return;
        }
        var char = line.get(this._model.selectionStart[0]);
        if (char[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
            this._model.selectionStart[0]++;
        }
    };
    SelectionManager.prototype._onDoubleClick = function (event) {
        var coords = this._getMouseBufferCoords(event);
        if (coords) {
            this._activeSelectionMode = 1;
            this._selectWordAt(coords, true);
        }
    };
    SelectionManager.prototype._onTripleClick = function (event) {
        var coords = this._getMouseBufferCoords(event);
        if (coords) {
            this._activeSelectionMode = 2;
            this._selectLineAt(coords[1]);
        }
    };
    SelectionManager.prototype.shouldColumnSelect = function (event) {
        return event.altKey && !(Browser.isMac && this._terminal.options.macOptionClickForcesSelection);
    };
    SelectionManager.prototype._onMouseMove = function (event) {
        event.stopImmediatePropagation();
        var previousSelectionEnd = this._model.selectionEnd ? [this._model.selectionEnd[0], this._model.selectionEnd[1]] : null;
        this._model.selectionEnd = this._getMouseBufferCoords(event);
        if (!this._model.selectionEnd) {
            this.refresh(true);
            return;
        }
        if (this._activeSelectionMode === 2) {
            if (this._model.selectionEnd[1] < this._model.selectionStart[1]) {
                this._model.selectionEnd[0] = 0;
            }
            else {
                this._model.selectionEnd[0] = this._terminal.cols;
            }
        }
        else if (this._activeSelectionMode === 1) {
            this._selectToWordAt(this._model.selectionEnd);
        }
        this._dragScrollAmount = this._getMouseEventScrollAmount(event);
        if (this._activeSelectionMode !== 3) {
            if (this._dragScrollAmount > 0) {
                this._model.selectionEnd[0] = this._terminal.cols;
            }
            else if (this._dragScrollAmount < 0) {
                this._model.selectionEnd[0] = 0;
            }
        }
        if (this._model.selectionEnd[1] < this._buffer.lines.length) {
            var char = this._buffer.lines.get(this._model.selectionEnd[1]).get(this._model.selectionEnd[0]);
            if (char && char[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
                this._model.selectionEnd[0]++;
            }
        }
        if (!previousSelectionEnd ||
            previousSelectionEnd[0] !== this._model.selectionEnd[0] ||
            previousSelectionEnd[1] !== this._model.selectionEnd[1]) {
            this.refresh(true);
        }
    };
    SelectionManager.prototype._dragScroll = function () {
        if (this._dragScrollAmount) {
            this._terminal.scrollLines(this._dragScrollAmount, false);
            if (this._dragScrollAmount > 0) {
                if (this._activeSelectionMode !== 3) {
                    this._model.selectionEnd[0] = this._terminal.cols;
                }
                this._model.selectionEnd[1] = Math.min(this._terminal.buffer.ydisp + this._terminal.rows, this._terminal.buffer.lines.length - 1);
            }
            else {
                if (this._activeSelectionMode !== 3) {
                    this._model.selectionEnd[0] = 0;
                }
                this._model.selectionEnd[1] = this._terminal.buffer.ydisp;
            }
            this.refresh();
        }
    };
    SelectionManager.prototype._onMouseUp = function (event) {
        var timeElapsed = event.timeStamp - this._mouseDownTimeStamp;
        this._removeMouseDownListeners();
        if (this.selectionText.length <= 1 && timeElapsed < ALT_CLICK_MOVE_CURSOR_TIME) {
            (new AltClickHandler_1.AltClickHandler(event, this._terminal)).move();
        }
        else if (this.hasSelection) {
            this._terminal.emit('selection');
        }
    };
    SelectionManager.prototype._onBufferActivate = function (e) {
        this.clearSelection();
        e.inactiveBuffer.lines.off('trim', this._trimListener);
        e.activeBuffer.lines.on('trim', this._trimListener);
    };
    SelectionManager.prototype._convertViewportColToCharacterIndex = function (bufferLine, coords) {
        var charIndex = coords[0];
        for (var i = 0; coords[0] >= i; i++) {
            var char = bufferLine.get(i);
            if (char[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
                charIndex--;
            }
            else if (char[Buffer_1.CHAR_DATA_CHAR_INDEX].length > 1 && coords[0] !== i) {
                charIndex += char[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
            }
        }
        return charIndex;
    };
    SelectionManager.prototype.setSelection = function (col, row, length) {
        this._model.clearSelection();
        this._removeMouseDownListeners();
        this._model.selectionStart = [col, row];
        this._model.selectionStartLength = length;
        this.refresh();
    };
    SelectionManager.prototype._getWordAt = function (coords, allowWhitespaceOnlySelection, followWrappedLinesAbove, followWrappedLinesBelow) {
        if (followWrappedLinesAbove === void 0) { followWrappedLinesAbove = true; }
        if (followWrappedLinesBelow === void 0) { followWrappedLinesBelow = true; }
        if (coords[0] >= this._terminal.cols) {
            return null;
        }
        var bufferLine = this._buffer.lines.get(coords[1]);
        if (!bufferLine) {
            return null;
        }
        var line = this._buffer.translateBufferLineToString(coords[1], false);
        var startIndex = this._convertViewportColToCharacterIndex(bufferLine, coords);
        var endIndex = startIndex;
        var charOffset = coords[0] - startIndex;
        var leftWideCharCount = 0;
        var rightWideCharCount = 0;
        var leftLongCharOffset = 0;
        var rightLongCharOffset = 0;
        if (line.charAt(startIndex) === ' ') {
            while (startIndex > 0 && line.charAt(startIndex - 1) === ' ') {
                startIndex--;
            }
            while (endIndex < line.length && line.charAt(endIndex + 1) === ' ') {
                endIndex++;
            }
        }
        else {
            var startCol = coords[0];
            var endCol = coords[0];
            if (bufferLine.get(startCol)[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
                leftWideCharCount++;
                startCol--;
            }
            if (bufferLine.get(endCol)[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 2) {
                rightWideCharCount++;
                endCol++;
            }
            if (bufferLine.get(endCol)[Buffer_1.CHAR_DATA_CHAR_INDEX].length > 1) {
                rightLongCharOffset += bufferLine.get(endCol)[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
                endIndex += bufferLine.get(endCol)[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
            }
            while (startCol > 0 && startIndex > 0 && !this._isCharWordSeparator(bufferLine.get(startCol - 1))) {
                var char = bufferLine.get(startCol - 1);
                if (char[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
                    leftWideCharCount++;
                    startCol--;
                }
                else if (char[Buffer_1.CHAR_DATA_CHAR_INDEX].length > 1) {
                    leftLongCharOffset += char[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
                    startIndex -= char[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
                }
                startIndex--;
                startCol--;
            }
            while (endCol < bufferLine.length && endIndex + 1 < line.length && !this._isCharWordSeparator(bufferLine.get(endCol + 1))) {
                var char = bufferLine.get(endCol + 1);
                if (char[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 2) {
                    rightWideCharCount++;
                    endCol++;
                }
                else if (char[Buffer_1.CHAR_DATA_CHAR_INDEX].length > 1) {
                    rightLongCharOffset += char[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
                    endIndex += char[Buffer_1.CHAR_DATA_CHAR_INDEX].length - 1;
                }
                endIndex++;
                endCol++;
            }
        }
        endIndex++;
        var start = startIndex
            + charOffset
            - leftWideCharCount
            + leftLongCharOffset;
        var length = Math.min(this._terminal.cols, endIndex
            - startIndex
            + leftWideCharCount
            + rightWideCharCount
            - leftLongCharOffset
            - rightLongCharOffset);
        if (!allowWhitespaceOnlySelection && line.slice(startIndex, endIndex).trim() === '') {
            return null;
        }
        if (followWrappedLinesAbove) {
            if (start === 0 && bufferLine.get(0)[Buffer_1.CHAR_DATA_CODE_INDEX] !== 32) {
                var previousBufferLine = this._buffer.lines.get(coords[1] - 1);
                if (previousBufferLine && bufferLine.isWrapped && previousBufferLine.get(this._terminal.cols - 1)[Buffer_1.CHAR_DATA_CODE_INDEX] !== 32) {
                    var previousLineWordPosition = this._getWordAt([this._terminal.cols - 1, coords[1] - 1], false, true, false);
                    if (previousLineWordPosition) {
                        var offset = this._terminal.cols - previousLineWordPosition.start;
                        start -= offset;
                        length += offset;
                    }
                }
            }
        }
        if (followWrappedLinesBelow) {
            if (start + length === this._terminal.cols && bufferLine.get(this._terminal.cols - 1)[Buffer_1.CHAR_DATA_CODE_INDEX] !== 32) {
                var nextBufferLine = this._buffer.lines.get(coords[1] + 1);
                if (nextBufferLine && nextBufferLine.isWrapped && nextBufferLine.get(0)[Buffer_1.CHAR_DATA_CODE_INDEX] !== 32) {
                    var nextLineWordPosition = this._getWordAt([0, coords[1] + 1], false, false, true);
                    if (nextLineWordPosition) {
                        length += nextLineWordPosition.length;
                    }
                }
            }
        }
        return { start: start, length: length };
    };
    SelectionManager.prototype._selectWordAt = function (coords, allowWhitespaceOnlySelection) {
        var wordPosition = this._getWordAt(coords, allowWhitespaceOnlySelection);
        if (wordPosition) {
            while (wordPosition.start < 0) {
                wordPosition.start += this._terminal.cols;
                coords[1]--;
            }
            this._model.selectionStart = [wordPosition.start, coords[1]];
            this._model.selectionStartLength = wordPosition.length;
        }
    };
    SelectionManager.prototype._selectToWordAt = function (coords) {
        var wordPosition = this._getWordAt(coords, true);
        if (wordPosition) {
            var endRow = coords[1];
            while (wordPosition.start < 0) {
                wordPosition.start += this._terminal.cols;
                endRow--;
            }
            if (!this._model.areSelectionValuesReversed()) {
                while (wordPosition.start + wordPosition.length > this._terminal.cols) {
                    wordPosition.length -= this._terminal.cols;
                    endRow++;
                }
            }
            this._model.selectionEnd = [this._model.areSelectionValuesReversed() ? wordPosition.start : wordPosition.start + wordPosition.length, endRow];
        }
    };
    SelectionManager.prototype._isCharWordSeparator = function (charData) {
        if (charData[Buffer_1.CHAR_DATA_WIDTH_INDEX] === 0) {
            return false;
        }
        return WORD_SEPARATORS.indexOf(charData[Buffer_1.CHAR_DATA_CHAR_INDEX]) >= 0;
    };
    SelectionManager.prototype._selectLineAt = function (line) {
        var wrappedRange = this._buffer.getWrappedRangeForLine(line);
        this._model.selectionStart = [0, wrappedRange.first];
        this._model.selectionEnd = [this._terminal.cols, wrappedRange.last];
        this._model.selectionStartLength = 0;
    };
    return SelectionManager;
}(EventEmitter_1.EventEmitter));
exports.SelectionManager = SelectionManager;

},{"./Buffer":2,"./SelectionModel":11,"./common/EventEmitter":17,"./core/Platform":21,"./handlers/AltClickHandler":24,"./utils/MouseHelper":53}],11:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var SelectionModel = (function () {
    function SelectionModel(_terminal) {
        this._terminal = _terminal;
        this.clearSelection();
    }
    SelectionModel.prototype.clearSelection = function () {
        this.selectionStart = null;
        this.selectionEnd = null;
        this.isSelectAllActive = false;
        this.selectionStartLength = 0;
    };
    Object.defineProperty(SelectionModel.prototype, "finalSelectionStart", {
        get: function () {
            if (this.isSelectAllActive) {
                return [0, 0];
            }
            if (!this.selectionEnd || !this.selectionStart) {
                return this.selectionStart;
            }
            return this.areSelectionValuesReversed() ? this.selectionEnd : this.selectionStart;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SelectionModel.prototype, "finalSelectionEnd", {
        get: function () {
            if (this.isSelectAllActive) {
                return [this._terminal.cols, this._terminal.buffer.ybase + this._terminal.rows - 1];
            }
            if (!this.selectionStart) {
                return null;
            }
            if (!this.selectionEnd || this.areSelectionValuesReversed()) {
                var startPlusLength = this.selectionStart[0] + this.selectionStartLength;
                if (startPlusLength > this._terminal.cols) {
                    return [startPlusLength % this._terminal.cols, this.selectionStart[1] + Math.floor(startPlusLength / this._terminal.cols)];
                }
                return [startPlusLength, this.selectionStart[1]];
            }
            if (this.selectionStartLength) {
                if (this.selectionEnd[1] === this.selectionStart[1]) {
                    return [Math.max(this.selectionStart[0] + this.selectionStartLength, this.selectionEnd[0]), this.selectionEnd[1]];
                }
            }
            return this.selectionEnd;
        },
        enumerable: true,
        configurable: true
    });
    SelectionModel.prototype.areSelectionValuesReversed = function () {
        var start = this.selectionStart;
        var end = this.selectionEnd;
        if (!start || !end) {
            return false;
        }
        return start[1] > end[1] || (start[1] === end[1] && start[0] > end[0]);
    };
    SelectionModel.prototype.onTrim = function (amount) {
        if (this.selectionStart) {
            this.selectionStart[1] -= amount;
        }
        if (this.selectionEnd) {
            this.selectionEnd[1] -= amount;
        }
        if (this.selectionEnd && this.selectionEnd[1] < 0) {
            this.clearSelection();
            return true;
        }
        if (this.selectionStart && this.selectionStart[1] < 0) {
            this.selectionStart[1] = 0;
        }
        return false;
    };
    return SelectionModel;
}());
exports.SelectionModel = SelectionModel;

},{}],12:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_BELL_SOUND = 'data:audio/wav;base64,UklGRigBAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQQBAADpAFgCwAMlBZoG/wdmCcoKRAypDQ8PbRDBEQQTOxRtFYcWlBePGIUZXhoiG88bcBz7HHIdzh0WHlMeZx51HmkeUx4WHs8dah0AHXwc3hs9G4saxRnyGBIYGBcQFv8U4RPAEoYRQBACD70NWwwHC6gJOwjWBloF7gOBAhABkf8b/qv8R/ve+Xf4Ife79W/0JfPZ8Z/wde9N7ijtE+wU6xvqM+lb6H7nw+YX5mrlxuQz5Mzje+Ma49fioeKD4nXiYeJy4pHitOL04j/jn+MN5IPkFOWs5U3mDefM55/ogOl36m7rdOyE7abuyu8D8Unyj/Pg9D/2qfcb+Yn6/vuK/Qj/lAAlAg==';
var SoundManager = (function () {
    function SoundManager(_terminal) {
        this._terminal = _terminal;
    }
    Object.defineProperty(SoundManager, "audioContext", {
        get: function () {
            if (!SoundManager._audioContext) {
                var audioContextCtor = window.AudioContext || window.webkitAudioContext;
                if (!audioContextCtor) {
                    console.warn('Web Audio API is not supported by this browser. Consider upgrading to the latest version');
                    return null;
                }
                SoundManager._audioContext = new audioContextCtor();
            }
            return SoundManager._audioContext;
        },
        enumerable: true,
        configurable: true
    });
    SoundManager.prototype.playBellSound = function () {
        var ctx = SoundManager.audioContext;
        if (!ctx) {
            return;
        }
        var bellAudioSource = ctx.createBufferSource();
        ctx.decodeAudioData(this._base64ToArrayBuffer(this._removeMimeType(this._terminal.options.bellSound)), function (buffer) {
            bellAudioSource.buffer = buffer;
            bellAudioSource.connect(ctx.destination);
            bellAudioSource.start(0);
        });
    };
    SoundManager.prototype._base64ToArrayBuffer = function (base64) {
        var binaryString = window.atob(base64);
        var len = binaryString.length;
        var bytes = new Uint8Array(len);
        for (var i = 0; i < len; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes.buffer;
    };
    SoundManager.prototype._removeMimeType = function (dataURI) {
        var splitUri = dataURI.split(',');
        return splitUri[1];
    };
    return SoundManager;
}());
exports.SoundManager = SoundManager;

},{}],13:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.blankLine = 'Blank line';
exports.promptLabel = 'Terminal input';
exports.tooMuchOutput = 'Too much output to announce, navigate to rows manually to read';

},{}],14:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var BufferSet_1 = require("./BufferSet");
var Buffer_1 = require("./Buffer");
var CompositionHelper_1 = require("./CompositionHelper");
var EventEmitter_1 = require("./common/EventEmitter");
var Viewport_1 = require("./Viewport");
var Clipboard_1 = require("./ui/Clipboard");
var EscapeSequences_1 = require("./common/data/EscapeSequences");
var InputHandler_1 = require("./InputHandler");
var Renderer_1 = require("./renderer/Renderer");
var Linkifier_1 = require("./Linkifier");
var SelectionManager_1 = require("./SelectionManager");
var CharMeasure_1 = require("./ui/CharMeasure");
var Browser = require("./core/Platform");
var Lifecycle_1 = require("./ui/Lifecycle");
var Strings = require("./Strings");
var MouseHelper_1 = require("./utils/MouseHelper");
var Clone_1 = require("./utils/Clone");
var SoundManager_1 = require("./SoundManager");
var ColorManager_1 = require("./renderer/ColorManager");
var MouseZoneManager_1 = require("./ui/MouseZoneManager");
var AccessibilityManager_1 = require("./AccessibilityManager");
var ScreenDprMonitor_1 = require("./ui/ScreenDprMonitor");
var CharAtlasCache_1 = require("./renderer/atlas/CharAtlasCache");
var DomRenderer_1 = require("./renderer/dom/DomRenderer");
var Keyboard_1 = require("./core/input/Keyboard");
var document = (typeof window !== 'undefined') ? window.document : null;
var WRITE_BUFFER_PAUSE_THRESHOLD = 5;
var WRITE_BATCH_SIZE = 300;
var CONSTRUCTOR_ONLY_OPTIONS = ['cols', 'rows'];
var DEFAULT_OPTIONS = {
    cols: 80,
    rows: 24,
    convertEol: false,
    termName: 'xterm',
    cursorBlink: false,
    cursorStyle: 'block',
    bellSound: SoundManager_1.DEFAULT_BELL_SOUND,
    bellStyle: 'none',
    drawBoldTextInBrightColors: true,
    enableBold: true,
    experimentalCharAtlas: 'static',
    fontFamily: 'courier-new, courier, monospace',
    fontSize: 15,
    fontWeight: 'normal',
    fontWeightBold: 'bold',
    lineHeight: 1.0,
    letterSpacing: 0,
    scrollback: 1000,
    screenKeys: false,
    screenReaderMode: false,
    debug: false,
    macOptionIsMeta: false,
    macOptionClickForcesSelection: false,
    cancelEvents: false,
    disableStdin: false,
    useFlowControl: false,
    allowTransparency: false,
    tabStopWidth: 8,
    theme: null,
    rightClickSelectsWord: Browser.isMac,
    rendererType: 'canvas',
    experimentalBufferLineImpl: 'TypedArray'
};
var Terminal = (function (_super) {
    __extends(Terminal, _super);
    function Terminal(options) {
        if (options === void 0) { options = {}; }
        var _this = _super.call(this) || this;
        _this.browser = Browser;
        _this._blankLine = null;
        _this.options = Clone_1.clone(options);
        _this._setup();
        return _this;
    }
    Terminal.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._customKeyEventHandler = null;
        CharAtlasCache_1.removeTerminalFromCache(this);
        this.handler = function () { };
        this.write = function () { };
        if (this.element && this.element.parentNode) {
            this.element.parentNode.removeChild(this.element);
        }
    };
    Terminal.prototype.destroy = function () {
        this.dispose();
    };
    Terminal.prototype._setup = function () {
        var _this = this;
        Object.keys(DEFAULT_OPTIONS).forEach(function (key) {
            if (_this.options[key] === null || _this.options[key] === undefined) {
                _this.options[key] = DEFAULT_OPTIONS[key];
            }
        });
        this._parent = document ? document.body : null;
        this.cols = this.options.cols;
        this.rows = this.options.rows;
        if (this.options.handler) {
            this.on('data', this.options.handler);
        }
        this.cursorState = 0;
        this.cursorHidden = false;
        this._customKeyEventHandler = null;
        this.applicationKeypad = false;
        this.applicationCursor = false;
        this.originMode = false;
        this.insertMode = false;
        this.wraparoundMode = true;
        this.bracketedPasteMode = false;
        this.charset = null;
        this.gcharset = null;
        this.glevel = 0;
        this.charsets = [null];
        this.curAttr = Buffer_1.DEFAULT_ATTR;
        this.params = [];
        this.currentParam = 0;
        this.writeBuffer = [];
        this._writeInProgress = false;
        this._xoffSentToCatchUp = false;
        this._userScrolling = false;
        this._inputHandler = new InputHandler_1.InputHandler(this);
        this.register(this._inputHandler);
        this.renderer = this.renderer || null;
        this.selectionManager = this.selectionManager || null;
        this.linkifier = this.linkifier || new Linkifier_1.Linkifier(this);
        this._mouseZoneManager = this._mouseZoneManager || null;
        this.soundManager = this.soundManager || new SoundManager_1.SoundManager(this);
        this.buffers = new BufferSet_1.BufferSet(this);
        if (this.selectionManager) {
            this.selectionManager.clearSelection();
            this.selectionManager.initBuffersListeners();
        }
    };
    Object.defineProperty(Terminal.prototype, "buffer", {
        get: function () {
            return this.buffers.active;
        },
        enumerable: true,
        configurable: true
    });
    Terminal.prototype.eraseAttr = function () {
        return (Buffer_1.DEFAULT_ATTR & ~0x1ff) | (this.curAttr & 0x1ff);
    };
    Terminal.prototype.focus = function () {
        if (this.textarea) {
            this.textarea.focus();
        }
    };
    Object.defineProperty(Terminal.prototype, "isFocused", {
        get: function () {
            return document.activeElement === this.textarea && document.hasFocus();
        },
        enumerable: true,
        configurable: true
    });
    Terminal.prototype.getOption = function (key) {
        if (!(key in DEFAULT_OPTIONS)) {
            throw new Error('No option with key "' + key + '"');
        }
        return this.options[key];
    };
    Terminal.prototype.setOption = function (key, value) {
        if (!(key in DEFAULT_OPTIONS)) {
            throw new Error('No option with key "' + key + '"');
        }
        if (CONSTRUCTOR_ONLY_OPTIONS.indexOf(key) !== -1) {
            console.error("Option \"" + key + "\" can only be set in the constructor");
        }
        if (this.options[key] === value) {
            return;
        }
        switch (key) {
            case 'bellStyle':
                if (!value) {
                    value = 'none';
                }
                break;
            case 'cursorStyle':
                if (!value) {
                    value = 'block';
                }
                break;
            case 'fontWeight':
                if (!value) {
                    value = 'normal';
                }
                break;
            case 'fontWeightBold':
                if (!value) {
                    value = 'bold';
                }
                break;
            case 'lineHeight':
                if (value < 1) {
                    console.warn(key + " cannot be less than 1, value: " + value);
                    return;
                }
            case 'rendererType':
                if (!value) {
                    value = 'canvas';
                }
                break;
            case 'tabStopWidth':
                if (value < 1) {
                    console.warn(key + " cannot be less than 1, value: " + value);
                    return;
                }
                break;
            case 'theme':
                if (this.renderer) {
                    this._setTheme(value);
                    return;
                }
                break;
            case 'scrollback':
                value = Math.min(value, Buffer_1.MAX_BUFFER_SIZE);
                if (value < 0) {
                    console.warn(key + " cannot be less than 0, value: " + value);
                    return;
                }
                if (this.options[key] !== value) {
                    var newBufferLength = this.rows + value;
                    if (this.buffer.lines.length > newBufferLength) {
                        var amountToTrim = this.buffer.lines.length - newBufferLength;
                        var needsRefresh = (this.buffer.ydisp - amountToTrim < 0);
                        this.buffer.lines.trimStart(amountToTrim);
                        this.buffer.ybase = Math.max(this.buffer.ybase - amountToTrim, 0);
                        this.buffer.ydisp = Math.max(this.buffer.ydisp - amountToTrim, 0);
                        if (needsRefresh) {
                            this.refresh(0, this.rows - 1);
                        }
                    }
                }
                break;
        }
        this.options[key] = value;
        switch (key) {
            case 'fontFamily':
            case 'fontSize':
                if (this.renderer) {
                    this.renderer.clear();
                    this.charMeasure.measure(this.options);
                }
                break;
            case 'drawBoldTextInBrightColors':
            case 'experimentalCharAtlas':
            case 'enableBold':
            case 'letterSpacing':
            case 'lineHeight':
            case 'fontWeight':
            case 'fontWeightBold':
                if (this.renderer) {
                    this.renderer.clear();
                    this.renderer.onResize(this.cols, this.rows);
                    this.refresh(0, this.rows - 1);
                }
            case 'rendererType':
                if (this.renderer) {
                    this.unregister(this.renderer);
                    this.renderer.dispose();
                    this.renderer = null;
                }
                this._setupRenderer();
                this.renderer.onCharSizeChanged();
                if (this._theme) {
                    this.renderer.setTheme(this._theme);
                }
                this.mouseHelper.setRenderer(this.renderer);
                break;
            case 'scrollback':
                this.buffers.resize(this.cols, this.rows);
                if (this.viewport) {
                    this.viewport.syncScrollArea();
                }
                break;
            case 'screenReaderMode':
                if (value) {
                    if (!this._accessibilityManager) {
                        this._accessibilityManager = new AccessibilityManager_1.AccessibilityManager(this);
                    }
                }
                else {
                    if (this._accessibilityManager) {
                        this._accessibilityManager.dispose();
                        this._accessibilityManager = null;
                    }
                }
                break;
            case 'tabStopWidth':
                this.buffers.setupTabStops();
                break;
            case 'experimentalBufferLineImpl':
                this.buffers.normal.setBufferLineFactory(value);
                this.buffers.alt.setBufferLineFactory(value);
                this._blankLine = null;
                break;
        }
        if (this.renderer) {
            this.renderer.onOptionsChanged();
        }
    };
    Terminal.prototype._onTextAreaFocus = function (ev) {
        if (this.sendFocus) {
            this.handler(EscapeSequences_1.C0.ESC + '[I');
        }
        this.updateCursorStyle(ev);
        this.element.classList.add('focus');
        this.showCursor();
        this.emit('focus');
    };
    Terminal.prototype.blur = function () {
        return this.textarea.blur();
    };
    Terminal.prototype._onTextAreaBlur = function () {
        this.textarea.value = '';
        this.refresh(this.buffer.y, this.buffer.y);
        if (this.sendFocus) {
            this.handler(EscapeSequences_1.C0.ESC + '[O');
        }
        this.element.classList.remove('focus');
        this.emit('blur');
    };
    Terminal.prototype._initGlobal = function () {
        var _this = this;
        this._bindKeys();
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'copy', function (event) {
            if (!_this.hasSelection()) {
                return;
            }
            Clipboard_1.copyHandler(event, _this, _this.selectionManager);
        }));
        var pasteHandlerWrapper = function (event) { return Clipboard_1.pasteHandler(event, _this); };
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'paste', pasteHandlerWrapper));
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'paste', pasteHandlerWrapper));
        if (Browser.isFirefox) {
            this.register(Lifecycle_1.addDisposableDomListener(this.element, 'mousedown', function (event) {
                if (event.button === 2) {
                    Clipboard_1.rightClickHandler(event, _this.textarea, _this.selectionManager, _this.options.rightClickSelectsWord);
                }
            }));
        }
        else {
            this.register(Lifecycle_1.addDisposableDomListener(this.element, 'contextmenu', function (event) {
                Clipboard_1.rightClickHandler(event, _this.textarea, _this.selectionManager, _this.options.rightClickSelectsWord);
            }));
        }
        if (Browser.isLinux) {
            this.register(Lifecycle_1.addDisposableDomListener(this.element, 'auxclick', function (event) {
                if (event.button === 1) {
                    Clipboard_1.moveTextAreaUnderMouseCursor(event, _this.textarea);
                }
            }));
        }
    };
    Terminal.prototype._bindKeys = function () {
        var _this = this;
        var self = this;
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'keydown', function (ev) {
            if (document.activeElement !== this) {
                return;
            }
            self._keyDown(ev);
        }, true));
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'keypress', function (ev) {
            if (document.activeElement !== this) {
                return;
            }
            self._keyPress(ev);
        }, true));
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'keyup', function (ev) {
            if (!wasModifierKeyOnlyEvent(ev)) {
                _this.focus();
            }
            self._keyUp(ev);
        }, true));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'keydown', function (ev) { return _this._keyDown(ev); }, true));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'keypress', function (ev) { return _this._keyPress(ev); }, true));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'compositionstart', function () { return _this._compositionHelper.compositionstart(); }));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'compositionupdate', function (e) { return _this._compositionHelper.compositionupdate(e); }));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'compositionend', function () { return _this._compositionHelper.compositionend(); }));
        this.register(this.addDisposableListener('refresh', function () { return _this._compositionHelper.updateCompositionElements(); }));
        this.register(this.addDisposableListener('refresh', function (data) { return _this._queueLinkification(data.start, data.end); }));
    };
    Terminal.prototype.open = function (parent) {
        var _this = this;
        this._parent = parent || this._parent;
        if (!this._parent) {
            throw new Error('Terminal requires a parent element.');
        }
        this._context = this._parent.ownerDocument.defaultView;
        this._document = this._parent.ownerDocument;
        this._screenDprMonitor = new ScreenDprMonitor_1.ScreenDprMonitor();
        this._screenDprMonitor.setListener(function () { return _this.emit('dprchange', window.devicePixelRatio); });
        this.register(this._screenDprMonitor);
        this.element = this._document.createElement('div');
        this.element.dir = 'ltr';
        this.element.classList.add('terminal');
        this.element.classList.add('xterm');
        this.element.setAttribute('tabindex', '0');
        this._parent.appendChild(this.element);
        var fragment = document.createDocumentFragment();
        this._viewportElement = document.createElement('div');
        this._viewportElement.classList.add('xterm-viewport');
        fragment.appendChild(this._viewportElement);
        this._viewportScrollArea = document.createElement('div');
        this._viewportScrollArea.classList.add('xterm-scroll-area');
        this._viewportElement.appendChild(this._viewportScrollArea);
        this.screenElement = document.createElement('div');
        this.screenElement.classList.add('xterm-screen');
        this._helperContainer = document.createElement('div');
        this._helperContainer.classList.add('xterm-helpers');
        this.screenElement.appendChild(this._helperContainer);
        fragment.appendChild(this.screenElement);
        this._mouseZoneManager = new MouseZoneManager_1.MouseZoneManager(this);
        this.register(this._mouseZoneManager);
        this.register(this.addDisposableListener('scroll', function () { return _this._mouseZoneManager.clearAll(); }));
        this.linkifier.attachToDom(this._mouseZoneManager);
        this.textarea = document.createElement('textarea');
        this.textarea.classList.add('xterm-helper-textarea');
        this.textarea.setAttribute('aria-label', Strings.promptLabel);
        this.textarea.setAttribute('aria-multiline', 'false');
        this.textarea.setAttribute('autocorrect', 'off');
        this.textarea.setAttribute('autocapitalize', 'off');
        this.textarea.setAttribute('spellcheck', 'false');
        this.textarea.tabIndex = 0;
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'focus', function (ev) { return _this._onTextAreaFocus(ev); }));
        this.register(Lifecycle_1.addDisposableDomListener(this.textarea, 'blur', function () { return _this._onTextAreaBlur(); }));
        this._helperContainer.appendChild(this.textarea);
        this._compositionView = document.createElement('div');
        this._compositionView.classList.add('composition-view');
        this._compositionHelper = new CompositionHelper_1.CompositionHelper(this.textarea, this._compositionView, this);
        this._helperContainer.appendChild(this._compositionView);
        this.charMeasure = new CharMeasure_1.CharMeasure(document, this._helperContainer);
        this.element.appendChild(fragment);
        this._setupRenderer();
        this._theme = this.options.theme;
        this.options.theme = null;
        this.viewport = new Viewport_1.Viewport(this, this._viewportElement, this._viewportScrollArea, this.charMeasure);
        this.viewport.onThemeChanged(this.renderer.colorManager.colors);
        this.register(this.viewport);
        this.register(this.addDisposableListener('cursormove', function () { return _this.renderer.onCursorMove(); }));
        this.register(this.addDisposableListener('resize', function () { return _this.renderer.onResize(_this.cols, _this.rows); }));
        this.register(this.addDisposableListener('blur', function () { return _this.renderer.onBlur(); }));
        this.register(this.addDisposableListener('focus', function () { return _this.renderer.onFocus(); }));
        this.register(this.addDisposableListener('dprchange', function () { return _this.renderer.onWindowResize(window.devicePixelRatio); }));
        this.register(Lifecycle_1.addDisposableDomListener(window, 'resize', function () { return _this.renderer.onWindowResize(window.devicePixelRatio); }));
        this.register(this.charMeasure.addDisposableListener('charsizechanged', function () { return _this.renderer.onCharSizeChanged(); }));
        this.register(this.renderer.addDisposableListener('resize', function (dimensions) { return _this.viewport.syncScrollArea(); }));
        this.selectionManager = new SelectionManager_1.SelectionManager(this, this.charMeasure);
        this.register(Lifecycle_1.addDisposableDomListener(this.element, 'mousedown', function (e) { return _this.selectionManager.onMouseDown(e); }));
        this.register(this.selectionManager.addDisposableListener('refresh', function (data) { return _this.renderer.onSelectionChanged(data.start, data.end, data.columnSelectMode); }));
        this.register(this.selectionManager.addDisposableListener('newselection', function (text) {
            _this.textarea.value = text;
            _this.textarea.focus();
            _this.textarea.select();
        }));
        this.register(this.addDisposableListener('scroll', function () {
            _this.viewport.syncScrollArea();
            _this.selectionManager.refresh();
        }));
        this.register(Lifecycle_1.addDisposableDomListener(this._viewportElement, 'scroll', function () { return _this.selectionManager.refresh(); }));
        this.mouseHelper = new MouseHelper_1.MouseHelper(this.renderer);
        if (this.options.screenReaderMode) {
            this._accessibilityManager = new AccessibilityManager_1.AccessibilityManager(this);
        }
        this.charMeasure.measure(this.options);
        this.refresh(0, this.rows - 1);
        this._initGlobal();
        this.bindMouse();
    };
    Terminal.prototype._setupRenderer = function () {
        switch (this.options.rendererType) {
            case 'canvas':
                this.renderer = new Renderer_1.Renderer(this, this.options.theme);
                break;
            case 'dom':
                this.renderer = new DomRenderer_1.DomRenderer(this, this.options.theme);
                break;
            default: throw new Error("Unrecognized rendererType \"" + this.options.rendererType + "\"");
        }
        this.register(this.renderer);
    };
    Terminal.prototype._setTheme = function (theme) {
        this._theme = theme;
        var colors = this.renderer.setTheme(theme);
        if (this.viewport) {
            this.viewport.onThemeChanged(colors);
        }
    };
    Terminal.prototype.bindMouse = function () {
        var _this = this;
        var el = this.element;
        var self = this;
        var pressed = 32;
        function sendButton(ev) {
            var button;
            var pos;
            button = getButton(ev);
            pos = self.mouseHelper.getRawByteCoords(ev, self.screenElement, self.charMeasure, self.cols, self.rows);
            if (!pos)
                return;
            sendEvent(button, pos);
            switch (ev.overrideType || ev.type) {
                case 'mousedown':
                    pressed = button;
                    break;
                case 'mouseup':
                    pressed = 32;
                    break;
                case 'wheel':
                    break;
            }
        }
        function sendMove(ev) {
            var button = pressed;
            var pos = self.mouseHelper.getRawByteCoords(ev, self.screenElement, self.charMeasure, self.cols, self.rows);
            if (!pos)
                return;
            button += 32;
            sendEvent(button, pos);
        }
        function encode(data, ch) {
            if (!self.utfMouse) {
                if (ch === 255) {
                    data.push(0);
                    return;
                }
                if (ch > 127)
                    ch = 127;
                data.push(ch);
            }
            else {
                if (ch === 2047) {
                    data.push(0);
                    return;
                }
                if (ch < 127) {
                    data.push(ch);
                }
                else {
                    if (ch > 2047)
                        ch = 2047;
                    data.push(0xC0 | (ch >> 6));
                    data.push(0x80 | (ch & 0x3F));
                }
            }
        }
        function sendEvent(button, pos) {
            if (self._vt300Mouse) {
                button &= 3;
                pos.x -= 32;
                pos.y -= 32;
                var data_1 = EscapeSequences_1.C0.ESC + '[24';
                if (button === 0)
                    data_1 += '1';
                else if (button === 1)
                    data_1 += '3';
                else if (button === 2)
                    data_1 += '5';
                else if (button === 3)
                    return;
                else
                    data_1 += '0';
                data_1 += '~[' + pos.x + ',' + pos.y + ']\r';
                self.handler(data_1);
                return;
            }
            if (self._decLocator) {
                button &= 3;
                pos.x -= 32;
                pos.y -= 32;
                if (button === 0)
                    button = 2;
                else if (button === 1)
                    button = 4;
                else if (button === 2)
                    button = 6;
                else if (button === 3)
                    button = 3;
                self.handler(EscapeSequences_1.C0.ESC + '['
                    + button
                    + ';'
                    + (button === 3 ? 4 : 0)
                    + ';'
                    + pos.y
                    + ';'
                    + pos.x
                    + ';'
                    + pos.page || 0
                    + '&w');
                return;
            }
            if (self.urxvtMouse) {
                pos.x -= 32;
                pos.y -= 32;
                pos.x++;
                pos.y++;
                self.handler(EscapeSequences_1.C0.ESC + '[' + button + ';' + pos.x + ';' + pos.y + 'M');
                return;
            }
            if (self.sgrMouse) {
                pos.x -= 32;
                pos.y -= 32;
                self.handler(EscapeSequences_1.C0.ESC + '[<'
                    + (((button & 3) === 3 ? button & ~3 : button) - 32)
                    + ';'
                    + pos.x
                    + ';'
                    + pos.y
                    + ((button & 3) === 3 ? 'm' : 'M'));
                return;
            }
            var data = [];
            encode(data, button);
            encode(data, pos.x);
            encode(data, pos.y);
            self.handler(EscapeSequences_1.C0.ESC + '[M' + String.fromCharCode.apply(String, data));
        }
        function getButton(ev) {
            var button;
            var shift;
            var meta;
            var ctrl;
            var mod;
            switch (ev.overrideType || ev.type) {
                case 'mousedown':
                    button = ev.button !== null && ev.button !== undefined
                        ? +ev.button
                        : ev.which !== null && ev.which !== undefined
                            ? ev.which - 1
                            : null;
                    if (Browser.isMSIE) {
                        button = button === 1 ? 0 : button === 4 ? 1 : button;
                    }
                    break;
                case 'mouseup':
                    button = 3;
                    break;
                case 'DOMMouseScroll':
                    button = ev.detail < 0
                        ? 64
                        : 65;
                    break;
                case 'wheel':
                    button = ev.deltaY < 0
                        ? 64
                        : 65;
                    break;
            }
            shift = ev.shiftKey ? 4 : 0;
            meta = ev.metaKey ? 8 : 0;
            ctrl = ev.ctrlKey ? 16 : 0;
            mod = shift | meta | ctrl;
            if (self.vt200Mouse) {
                mod &= ctrl;
            }
            else if (!self.normalMouse) {
                mod = 0;
            }
            button = (32 + (mod << 2)) + button;
            return button;
        }
        this.register(Lifecycle_1.addDisposableDomListener(el, 'mousedown', function (ev) {
            ev.preventDefault();
            _this.focus();
            if (!_this.mouseEvents || _this.selectionManager.shouldForceSelection(ev)) {
                return;
            }
            sendButton(ev);
            if (_this.vt200Mouse) {
                ev.overrideType = 'mouseup';
                sendButton(ev);
                return _this.cancel(ev);
            }
            var moveHandler;
            if (_this.normalMouse) {
                moveHandler = function (event) {
                    if (!_this.normalMouse) {
                        return;
                    }
                    sendMove(event);
                };
                _this._document.addEventListener('mousemove', moveHandler);
            }
            var handler = function (ev) {
                if (_this.normalMouse && !_this.x10Mouse) {
                    sendButton(ev);
                }
                if (moveHandler) {
                    _this._document.removeEventListener('mousemove', moveHandler);
                    moveHandler = null;
                }
                _this._document.removeEventListener('mouseup', handler);
                return _this.cancel(ev);
            };
            _this._document.addEventListener('mouseup', handler);
            return _this.cancel(ev);
        }));
        this.register(Lifecycle_1.addDisposableDomListener(el, 'wheel', function (ev) {
            if (!_this.mouseEvents) {
                if (!_this.buffer.hasScrollback) {
                    var amount = _this.viewport.getLinesScrolled(ev);
                    if (amount === 0) {
                        return;
                    }
                    var sequence = EscapeSequences_1.C0.ESC + (_this.applicationCursor ? 'O' : '[') + (ev.deltaY < 0 ? 'A' : 'B');
                    var data = '';
                    for (var i = 0; i < Math.abs(amount); i++) {
                        data += sequence;
                    }
                    _this.handler(data);
                }
                return;
            }
            if (_this.x10Mouse || _this._vt300Mouse || _this._decLocator)
                return;
            sendButton(ev);
            ev.preventDefault();
        }));
        this.register(Lifecycle_1.addDisposableDomListener(el, 'wheel', function (ev) {
            if (_this.mouseEvents)
                return;
            _this.viewport.onWheel(ev);
            return _this.cancel(ev);
        }));
        this.register(Lifecycle_1.addDisposableDomListener(el, 'touchstart', function (ev) {
            if (_this.mouseEvents)
                return;
            _this.viewport.onTouchStart(ev);
            return _this.cancel(ev);
        }));
        this.register(Lifecycle_1.addDisposableDomListener(el, 'touchmove', function (ev) {
            if (_this.mouseEvents)
                return;
            _this.viewport.onTouchMove(ev);
            return _this.cancel(ev);
        }));
    };
    Terminal.prototype.refresh = function (start, end) {
        if (this.renderer) {
            this.renderer.refreshRows(start, end);
        }
    };
    Terminal.prototype._queueLinkification = function (start, end) {
        if (this.linkifier) {
            this.linkifier.linkifyRows(start, end);
        }
    };
    Terminal.prototype.updateCursorStyle = function (ev) {
        if (this.selectionManager && this.selectionManager.shouldColumnSelect(ev)) {
            this.element.classList.add('column-select');
        }
        else {
            this.element.classList.remove('column-select');
        }
    };
    Terminal.prototype.showCursor = function () {
        if (!this.cursorState) {
            this.cursorState = 1;
            this.refresh(this.buffer.y, this.buffer.y);
        }
    };
    Terminal.prototype.scroll = function (isWrapped) {
        if (isWrapped === void 0) { isWrapped = false; }
        var newLine;
        var useRecycling = this.options.experimentalBufferLineImpl !== 'JsArray';
        if (useRecycling) {
            newLine = this._blankLine;
            if (!newLine || newLine.length !== this.cols || newLine.get(0)[Buffer_1.CHAR_DATA_ATTR_INDEX] !== this.eraseAttr()) {
                newLine = this.buffer.getBlankLine(this.eraseAttr(), isWrapped);
                this._blankLine = newLine;
            }
            newLine.isWrapped = isWrapped;
        }
        else {
            newLine = this.buffer.getBlankLine(this.eraseAttr(), isWrapped);
        }
        var topRow = this.buffer.ybase + this.buffer.scrollTop;
        var bottomRow = this.buffer.ybase + this.buffer.scrollBottom;
        if (this.buffer.scrollTop === 0) {
            var willBufferBeTrimmed = this.buffer.lines.isFull;
            if (bottomRow === this.buffer.lines.length - 1) {
                if (useRecycling) {
                    if (willBufferBeTrimmed) {
                        this.buffer.lines.recycle().copyFrom(newLine);
                    }
                    else {
                        this.buffer.lines.push(newLine.clone());
                    }
                }
                else {
                    this.buffer.lines.push(newLine);
                }
            }
            else {
                this.buffer.lines.splice(bottomRow + 1, 0, (useRecycling) ? newLine.clone() : newLine);
            }
            if (!willBufferBeTrimmed) {
                this.buffer.ybase++;
                if (!this._userScrolling) {
                    this.buffer.ydisp++;
                }
            }
            else {
                if (this._userScrolling) {
                    this.buffer.ydisp = Math.max(this.buffer.ydisp - 1, 0);
                }
            }
        }
        else {
            var scrollRegionHeight = bottomRow - topRow + 1;
            this.buffer.lines.shiftElements(topRow + 1, scrollRegionHeight - 1, -1);
            this.buffer.lines.set(bottomRow, (useRecycling) ? newLine.clone() : newLine);
        }
        if (!this._userScrolling) {
            this.buffer.ydisp = this.buffer.ybase;
        }
        this.updateRange(this.buffer.scrollTop);
        this.updateRange(this.buffer.scrollBottom);
        this.emit('scroll', this.buffer.ydisp);
    };
    Terminal.prototype.scrollLines = function (disp, suppressScrollEvent) {
        if (disp < 0) {
            if (this.buffer.ydisp === 0) {
                return;
            }
            this._userScrolling = true;
        }
        else if (disp + this.buffer.ydisp >= this.buffer.ybase) {
            this._userScrolling = false;
        }
        var oldYdisp = this.buffer.ydisp;
        this.buffer.ydisp = Math.max(Math.min(this.buffer.ydisp + disp, this.buffer.ybase), 0);
        if (oldYdisp === this.buffer.ydisp) {
            return;
        }
        if (!suppressScrollEvent) {
            this.emit('scroll', this.buffer.ydisp);
        }
        this.refresh(0, this.rows - 1);
    };
    Terminal.prototype.scrollPages = function (pageCount) {
        this.scrollLines(pageCount * (this.rows - 1));
    };
    Terminal.prototype.scrollToTop = function () {
        this.scrollLines(-this.buffer.ydisp);
    };
    Terminal.prototype.scrollToBottom = function () {
        this.scrollLines(this.buffer.ybase - this.buffer.ydisp);
    };
    Terminal.prototype.scrollToLine = function (line) {
        var scrollAmount = line - this.buffer.ydisp;
        if (scrollAmount !== 0) {
            this.scrollLines(scrollAmount);
        }
    };
    Terminal.prototype.write = function (data) {
        var _this = this;
        if (this._isDisposed) {
            return;
        }
        if (!data) {
            return;
        }
        this.writeBuffer.push(data);
        if (this.options.useFlowControl && !this._xoffSentToCatchUp && this.writeBuffer.length >= WRITE_BUFFER_PAUSE_THRESHOLD) {
            this.handler(EscapeSequences_1.C0.DC3);
            this._xoffSentToCatchUp = true;
        }
        if (!this._writeInProgress && this.writeBuffer.length > 0) {
            this._writeInProgress = true;
            setTimeout(function () {
                _this._innerWrite();
            });
        }
    };
    Terminal.prototype._innerWrite = function () {
        var _this = this;
        if (this._isDisposed) {
            this.writeBuffer = [];
        }
        var writeBatch = this.writeBuffer.splice(0, WRITE_BATCH_SIZE);
        while (writeBatch.length > 0) {
            var data = writeBatch.shift();
            if (this._xoffSentToCatchUp && writeBatch.length === 0 && this.writeBuffer.length === 0) {
                this.handler(EscapeSequences_1.C0.DC1);
                this._xoffSentToCatchUp = false;
            }
            this._refreshStart = this.buffer.y;
            this._refreshEnd = this.buffer.y;
            this._inputHandler.parse(data);
            this.updateRange(this.buffer.y);
            this.refresh(this._refreshStart, this._refreshEnd);
        }
        if (this.writeBuffer.length > 0) {
            setTimeout(function () { return _this._innerWrite(); }, 0);
        }
        else {
            this._writeInProgress = false;
        }
    };
    Terminal.prototype.writeln = function (data) {
        this.write(data + '\r\n');
    };
    Terminal.prototype.attachCustomKeyEventHandler = function (customKeyEventHandler) {
        this._customKeyEventHandler = customKeyEventHandler;
    };
    Terminal.prototype.registerLinkMatcher = function (regex, handler, options) {
        var matcherId = this.linkifier.registerLinkMatcher(regex, handler, options);
        this.refresh(0, this.rows - 1);
        return matcherId;
    };
    Terminal.prototype.deregisterLinkMatcher = function (matcherId) {
        if (this.linkifier.deregisterLinkMatcher(matcherId)) {
            this.refresh(0, this.rows - 1);
        }
    };
    Terminal.prototype.registerCharacterJoiner = function (handler) {
        var joinerId = this.renderer.registerCharacterJoiner(handler);
        this.refresh(0, this.rows - 1);
        return joinerId;
    };
    Terminal.prototype.deregisterCharacterJoiner = function (joinerId) {
        if (this.renderer.deregisterCharacterJoiner(joinerId)) {
            this.refresh(0, this.rows - 1);
        }
    };
    Object.defineProperty(Terminal.prototype, "markers", {
        get: function () {
            return this.buffer.markers;
        },
        enumerable: true,
        configurable: true
    });
    Terminal.prototype.addMarker = function (cursorYOffset) {
        if (this.buffer !== this.buffers.normal) {
            return;
        }
        return this.buffer.addMarker(this.buffer.ybase + this.buffer.y + cursorYOffset);
    };
    Terminal.prototype.hasSelection = function () {
        return this.selectionManager ? this.selectionManager.hasSelection : false;
    };
    Terminal.prototype.getSelection = function () {
        return this.selectionManager ? this.selectionManager.selectionText : '';
    };
    Terminal.prototype.clearSelection = function () {
        if (this.selectionManager) {
            this.selectionManager.clearSelection();
        }
    };
    Terminal.prototype.selectAll = function () {
        if (this.selectionManager) {
            this.selectionManager.selectAll();
        }
    };
    Terminal.prototype.selectLines = function (start, end) {
        if (this.selectionManager) {
            this.selectionManager.selectLines(start, end);
        }
    };
    Terminal.prototype._keyDown = function (event) {
        if (this._customKeyEventHandler && this._customKeyEventHandler(event) === false) {
            return false;
        }
        if (!this._compositionHelper.keydown(event)) {
            if (this.buffer.ybase !== this.buffer.ydisp) {
                this.scrollToBottom();
            }
            return false;
        }
        var result = Keyboard_1.evaluateKeyboardEvent(event, this.applicationCursor, this.browser.isMac, this.options.macOptionIsMeta);
        this.updateCursorStyle(event);
        if (result.type === 3 || result.type === 2) {
            var scrollCount = this.rows - 1;
            this.scrollLines(result.type === 2 ? -scrollCount : scrollCount);
            return this.cancel(event, true);
        }
        if (result.type === 1) {
            this.selectAll();
        }
        if (this._isThirdLevelShift(this.browser, event)) {
            return true;
        }
        if (result.cancel) {
            this.cancel(event, true);
        }
        if (!result.key) {
            return true;
        }
        this.emit('keydown', event);
        this.emit('key', result.key, event);
        this.showCursor();
        this.handler(result.key);
        return this.cancel(event, true);
    };
    Terminal.prototype._isThirdLevelShift = function (browser, ev) {
        var thirdLevelKey = (browser.isMac && !this.options.macOptionIsMeta && ev.altKey && !ev.ctrlKey && !ev.metaKey) ||
            (browser.isMSWindows && ev.altKey && ev.ctrlKey && !ev.metaKey);
        if (ev.type === 'keypress') {
            return thirdLevelKey;
        }
        return thirdLevelKey && (!ev.keyCode || ev.keyCode > 47);
    };
    Terminal.prototype.setgLevel = function (g) {
        this.glevel = g;
        this.charset = this.charsets[g];
    };
    Terminal.prototype.setgCharset = function (g, charset) {
        this.charsets[g] = charset;
        if (this.glevel === g) {
            this.charset = charset;
        }
    };
    Terminal.prototype._keyUp = function (ev) {
        this.updateCursorStyle(ev);
    };
    Terminal.prototype._keyPress = function (ev) {
        var key;
        if (this._customKeyEventHandler && this._customKeyEventHandler(ev) === false) {
            return false;
        }
        this.cancel(ev);
        if (ev.charCode) {
            key = ev.charCode;
        }
        else if (ev.which === null || ev.which === undefined) {
            key = ev.keyCode;
        }
        else if (ev.which !== 0 && ev.charCode !== 0) {
            key = ev.which;
        }
        else {
            return false;
        }
        if (!key || ((ev.altKey || ev.ctrlKey || ev.metaKey) && !this._isThirdLevelShift(this.browser, ev))) {
            return false;
        }
        key = String.fromCharCode(key);
        this.emit('keypress', key, ev);
        this.emit('key', key, ev);
        this.showCursor();
        this.handler(key);
        return true;
    };
    Terminal.prototype.bell = function () {
        var _this = this;
        this.emit('bell');
        if (this._soundBell()) {
            this.soundManager.playBellSound();
        }
        if (this._visualBell()) {
            this.element.classList.add('visual-bell-active');
            clearTimeout(this._visualBellTimer);
            this._visualBellTimer = window.setTimeout(function () {
                _this.element.classList.remove('visual-bell-active');
            }, 200);
        }
    };
    Terminal.prototype.log = function (text, data) {
        if (!this.options.debug)
            return;
        if (!this._context.console || !this._context.console.log)
            return;
        this._context.console.log(text, data);
    };
    Terminal.prototype.error = function (text, data) {
        if (!this.options.debug)
            return;
        if (!this._context.console || !this._context.console.error)
            return;
        this._context.console.error(text, data);
    };
    Terminal.prototype.resize = function (x, y) {
        if (isNaN(x) || isNaN(y)) {
            return;
        }
        if (x === this.cols && y === this.rows) {
            if (this.charMeasure && (!this.charMeasure.width || !this.charMeasure.height)) {
                this.charMeasure.measure(this.options);
            }
            return;
        }
        if (x < 1)
            x = 1;
        if (y < 1)
            y = 1;
        this.buffers.resize(x, y);
        this.cols = x;
        this.rows = y;
        this.buffers.setupTabStops(this.cols);
        if (this.charMeasure) {
            this.charMeasure.measure(this.options);
        }
        this.refresh(0, this.rows - 1);
        this.emit('resize', { cols: x, rows: y });
    };
    Terminal.prototype.updateRange = function (y) {
        if (y < this._refreshStart)
            this._refreshStart = y;
        if (y > this._refreshEnd)
            this._refreshEnd = y;
    };
    Terminal.prototype.maxRange = function () {
        this._refreshStart = 0;
        this._refreshEnd = this.rows - 1;
    };
    Terminal.prototype.clear = function () {
        if (this.buffer.ybase === 0 && this.buffer.y === 0) {
            return;
        }
        this.buffer.lines.set(0, this.buffer.lines.get(this.buffer.ybase + this.buffer.y));
        this.buffer.lines.length = 1;
        this.buffer.ydisp = 0;
        this.buffer.ybase = 0;
        this.buffer.y = 0;
        for (var i = 1; i < this.rows; i++) {
            this.buffer.lines.push(this.buffer.getBlankLine(Buffer_1.DEFAULT_ATTR));
        }
        this.refresh(0, this.rows - 1);
        this.emit('scroll', this.buffer.ydisp);
    };
    Terminal.prototype.ch = function (cur) {
        if (cur) {
            return [this.eraseAttr(), Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE];
        }
        return [Buffer_1.DEFAULT_ATTR, Buffer_1.NULL_CELL_CHAR, Buffer_1.NULL_CELL_WIDTH, Buffer_1.NULL_CELL_CODE];
    };
    Terminal.prototype.is = function (term) {
        return (this.options.termName + '').indexOf(term) === 0;
    };
    Terminal.prototype.handler = function (data) {
        if (this.options.disableStdin) {
            return;
        }
        if (this.selectionManager && this.selectionManager.hasSelection) {
            this.selectionManager.clearSelection();
        }
        if (this.buffer.ybase !== this.buffer.ydisp) {
            this.scrollToBottom();
        }
        this.emit('data', data);
    };
    Terminal.prototype.handleTitle = function (title) {
        this.emit('title', title);
    };
    Terminal.prototype.index = function () {
        this.buffer.y++;
        if (this.buffer.y > this.buffer.scrollBottom) {
            this.buffer.y--;
            this.scroll();
        }
        if (this.buffer.x >= this.cols) {
            this.buffer.x--;
        }
    };
    Terminal.prototype.reverseIndex = function () {
        if (this.buffer.y === this.buffer.scrollTop) {
            var scrollRegionHeight = this.buffer.scrollBottom - this.buffer.scrollTop;
            this.buffer.lines.shiftElements(this.buffer.y + this.buffer.ybase, scrollRegionHeight, 1);
            this.buffer.lines.set(this.buffer.y + this.buffer.ybase, this.buffer.getBlankLine(this.eraseAttr()));
            this.updateRange(this.buffer.scrollTop);
            this.updateRange(this.buffer.scrollBottom);
        }
        else {
            this.buffer.y--;
        }
    };
    Terminal.prototype.reset = function () {
        this.options.rows = this.rows;
        this.options.cols = this.cols;
        var customKeyEventHandler = this._customKeyEventHandler;
        var inputHandler = this._inputHandler;
        var cursorState = this.cursorState;
        this._setup();
        this._customKeyEventHandler = customKeyEventHandler;
        this._inputHandler = inputHandler;
        this.cursorState = cursorState;
        this.refresh(0, this.rows - 1);
        if (this.viewport) {
            this.viewport.syncScrollArea();
        }
    };
    Terminal.prototype.tabSet = function () {
        this.buffer.tabs[this.buffer.x] = true;
    };
    Terminal.prototype.cancel = function (ev, force) {
        if (!this.options.cancelEvents && !force) {
            return;
        }
        ev.preventDefault();
        ev.stopPropagation();
        return false;
    };
    Terminal.prototype.matchColor = function (r1, g1, b1) {
        var hash = (r1 << 16) | (g1 << 8) | b1;
        if (matchColorCache[hash] !== null && matchColorCache[hash] !== undefined) {
            return matchColorCache[hash];
        }
        var ldiff = Infinity;
        var li = -1;
        var i = 0;
        var c;
        var r2;
        var g2;
        var b2;
        var diff;
        for (; i < ColorManager_1.DEFAULT_ANSI_COLORS.length; i++) {
            c = ColorManager_1.DEFAULT_ANSI_COLORS[i].rgba;
            r2 = c >>> 24;
            g2 = c >>> 16 & 0xFF;
            b2 = c >>> 8 & 0xFF;
            diff = matchColorDistance(r1, g1, b1, r2, g2, b2);
            if (diff === 0) {
                li = i;
                break;
            }
            if (diff < ldiff) {
                ldiff = diff;
                li = i;
            }
        }
        return matchColorCache[hash] = li;
    };
    Terminal.prototype._visualBell = function () {
        return false;
    };
    Terminal.prototype._soundBell = function () {
        return this.options.bellStyle === 'sound';
    };
    return Terminal;
}(EventEmitter_1.EventEmitter));
exports.Terminal = Terminal;
function wasModifierKeyOnlyEvent(ev) {
    return ev.keyCode === 16 ||
        ev.keyCode === 17 ||
        ev.keyCode === 18;
}
var matchColorCache = {};
function matchColorDistance(r1, g1, b1, r2, g2, b2) {
    return Math.pow(30 * (r1 - r2), 2)
        + Math.pow(59 * (g1 - g2), 2)
        + Math.pow(11 * (b1 - b2), 2);
}

},{"./AccessibilityManager":1,"./Buffer":2,"./BufferSet":4,"./CompositionHelper":6,"./InputHandler":8,"./Linkifier":9,"./SelectionManager":10,"./SoundManager":12,"./Strings":13,"./Viewport":15,"./common/EventEmitter":17,"./common/data/EscapeSequences":20,"./core/Platform":21,"./core/input/Keyboard":23,"./renderer/ColorManager":28,"./renderer/Renderer":32,"./renderer/atlas/CharAtlasCache":36,"./renderer/dom/DomRenderer":44,"./ui/CharMeasure":46,"./ui/Clipboard":47,"./ui/Lifecycle":48,"./ui/MouseZoneManager":49,"./ui/ScreenDprMonitor":51,"./utils/Clone":52,"./utils/MouseHelper":53}],15:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Lifecycle_1 = require("./common/Lifecycle");
var Lifecycle_2 = require("./ui/Lifecycle");
var FALLBACK_SCROLL_BAR_WIDTH = 15;
var Viewport = (function (_super) {
    __extends(Viewport, _super);
    function Viewport(_terminal, _viewportElement, _scrollArea, _charMeasure) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._viewportElement = _viewportElement;
        _this._scrollArea = _scrollArea;
        _this._charMeasure = _charMeasure;
        _this.scrollBarWidth = 0;
        _this._currentRowHeight = 0;
        _this._lastRecordedBufferLength = 0;
        _this._lastRecordedViewportHeight = 0;
        _this._lastRecordedBufferHeight = 0;
        _this._lastScrollTop = 0;
        _this._wheelPartialScroll = 0;
        _this._refreshAnimationFrame = null;
        _this._ignoreNextScrollEvent = false;
        _this.scrollBarWidth = (_this._viewportElement.offsetWidth - _this._scrollArea.offsetWidth) || FALLBACK_SCROLL_BAR_WIDTH;
        _this.register(Lifecycle_2.addDisposableDomListener(_this._viewportElement, 'scroll', _this._onScroll.bind(_this)));
        setTimeout(function () { return _this.syncScrollArea(); }, 0);
        return _this;
    }
    Viewport.prototype.onThemeChanged = function (colors) {
        this._viewportElement.style.backgroundColor = colors.background.css;
    };
    Viewport.prototype._refresh = function () {
        var _this = this;
        if (this._refreshAnimationFrame === null) {
            this._refreshAnimationFrame = requestAnimationFrame(function () { return _this._innerRefresh(); });
        }
    };
    Viewport.prototype._innerRefresh = function () {
        if (this._charMeasure.height > 0) {
            this._currentRowHeight = this._terminal.renderer.dimensions.scaledCellHeight / window.devicePixelRatio;
            this._lastRecordedViewportHeight = this._viewportElement.offsetHeight;
            var newBufferHeight = Math.round(this._currentRowHeight * this._lastRecordedBufferLength) + (this._lastRecordedViewportHeight - this._terminal.renderer.dimensions.canvasHeight);
            if (this._lastRecordedBufferHeight !== newBufferHeight) {
                this._lastRecordedBufferHeight = newBufferHeight;
                this._scrollArea.style.height = this._lastRecordedBufferHeight + 'px';
            }
        }
        var scrollTop = this._terminal.buffer.ydisp * this._currentRowHeight;
        if (this._viewportElement.scrollTop !== scrollTop) {
            this._ignoreNextScrollEvent = true;
            this._viewportElement.scrollTop = scrollTop;
        }
        this._refreshAnimationFrame = null;
    };
    Viewport.prototype.syncScrollArea = function () {
        if (this._lastRecordedBufferLength !== this._terminal.buffer.lines.length) {
            this._lastRecordedBufferLength = this._terminal.buffer.lines.length;
            this._refresh();
            return;
        }
        if (this._lastRecordedViewportHeight !== this._terminal.renderer.dimensions.canvasHeight) {
            this._refresh();
            return;
        }
        var newScrollTop = this._terminal.buffer.ydisp * this._currentRowHeight;
        if (this._lastScrollTop !== newScrollTop) {
            this._refresh();
            return;
        }
        if (this._lastScrollTop !== this._viewportElement.scrollTop) {
            this._refresh();
            return;
        }
        if (this._terminal.renderer.dimensions.scaledCellHeight / window.devicePixelRatio !== this._currentRowHeight) {
            this._refresh();
            return;
        }
    };
    Viewport.prototype._onScroll = function (ev) {
        this._lastScrollTop = this._viewportElement.scrollTop;
        if (!this._viewportElement.offsetParent) {
            return;
        }
        if (this._ignoreNextScrollEvent) {
            this._ignoreNextScrollEvent = false;
            return;
        }
        var newRow = Math.round(this._lastScrollTop / this._currentRowHeight);
        var diff = newRow - this._terminal.buffer.ydisp;
        this._terminal.scrollLines(diff, true);
    };
    Viewport.prototype.onWheel = function (ev) {
        var amount = this._getPixelsScrolled(ev);
        if (amount === 0) {
            return;
        }
        this._viewportElement.scrollTop += amount;
        ev.preventDefault();
    };
    Viewport.prototype._getPixelsScrolled = function (ev) {
        if (ev.deltaY === 0) {
            return 0;
        }
        var amount = ev.deltaY;
        if (ev.deltaMode === WheelEvent.DOM_DELTA_LINE) {
            amount *= this._currentRowHeight;
        }
        else if (ev.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
            amount *= this._currentRowHeight * this._terminal.rows;
        }
        return amount;
    };
    Viewport.prototype.getLinesScrolled = function (ev) {
        if (ev.deltaY === 0) {
            return 0;
        }
        var amount = ev.deltaY;
        if (ev.deltaMode === WheelEvent.DOM_DELTA_PIXEL) {
            amount /= this._currentRowHeight + 0.0;
            this._wheelPartialScroll += amount;
            amount = Math.floor(Math.abs(this._wheelPartialScroll)) * (this._wheelPartialScroll > 0 ? 1 : -1);
            this._wheelPartialScroll %= 1;
        }
        else if (ev.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
            amount *= this._terminal.rows;
        }
        return amount;
    };
    Viewport.prototype.onTouchStart = function (ev) {
        this._lastTouchY = ev.touches[0].pageY;
    };
    Viewport.prototype.onTouchMove = function (ev) {
        var deltaY = this._lastTouchY - ev.touches[0].pageY;
        this._lastTouchY = ev.touches[0].pageY;
        if (deltaY === 0) {
            return;
        }
        this._viewportElement.scrollTop += deltaY;
        ev.preventDefault();
    };
    return Viewport;
}(Lifecycle_1.Disposable));
exports.Viewport = Viewport;

},{"./common/Lifecycle":18,"./ui/Lifecycle":48}],16:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var EventEmitter_1 = require("./EventEmitter");
var CircularList = (function (_super) {
    __extends(CircularList, _super);
    function CircularList(_maxLength) {
        var _this = _super.call(this) || this;
        _this._maxLength = _maxLength;
        _this._array = new Array(_this._maxLength);
        _this._startIndex = 0;
        _this._length = 0;
        return _this;
    }
    Object.defineProperty(CircularList.prototype, "maxLength", {
        get: function () {
            return this._maxLength;
        },
        set: function (newMaxLength) {
            if (this._maxLength === newMaxLength) {
                return;
            }
            var newArray = new Array(newMaxLength);
            for (var i = 0; i < Math.min(newMaxLength, this.length); i++) {
                newArray[i] = this._array[this._getCyclicIndex(i)];
            }
            this._array = newArray;
            this._maxLength = newMaxLength;
            this._startIndex = 0;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CircularList.prototype, "length", {
        get: function () {
            return this._length;
        },
        set: function (newLength) {
            if (newLength > this._length) {
                for (var i = this._length; i < newLength; i++) {
                    this._array[i] = undefined;
                }
            }
            this._length = newLength;
        },
        enumerable: true,
        configurable: true
    });
    CircularList.prototype.get = function (index) {
        return this._array[this._getCyclicIndex(index)];
    };
    CircularList.prototype.set = function (index, value) {
        this._array[this._getCyclicIndex(index)] = value;
    };
    CircularList.prototype.push = function (value) {
        this._array[this._getCyclicIndex(this._length)] = value;
        if (this._length === this._maxLength) {
            this._startIndex = ++this._startIndex % this._maxLength;
            this.emit('trim', 1);
        }
        else {
            this._length++;
        }
    };
    CircularList.prototype.recycle = function () {
        if (this._length !== this._maxLength) {
            throw new Error('Can only recycle when the buffer is full');
        }
        this._startIndex = ++this._startIndex % this._maxLength;
        this.emit('trim', 1);
        return this._array[this._getCyclicIndex(this._length - 1)];
    };
    Object.defineProperty(CircularList.prototype, "isFull", {
        get: function () {
            return this._length === this._maxLength;
        },
        enumerable: true,
        configurable: true
    });
    CircularList.prototype.pop = function () {
        return this._array[this._getCyclicIndex(this._length-- - 1)];
    };
    CircularList.prototype.splice = function (start, deleteCount) {
        var items = [];
        for (var _i = 2; _i < arguments.length; _i++) {
            items[_i - 2] = arguments[_i];
        }
        if (deleteCount) {
            for (var i = start; i < this._length - deleteCount; i++) {
                this._array[this._getCyclicIndex(i)] = this._array[this._getCyclicIndex(i + deleteCount)];
            }
            this._length -= deleteCount;
        }
        if (items && items.length) {
            for (var i = this._length - 1; i >= start; i--) {
                this._array[this._getCyclicIndex(i + items.length)] = this._array[this._getCyclicIndex(i)];
            }
            for (var i = 0; i < items.length; i++) {
                this._array[this._getCyclicIndex(start + i)] = items[i];
            }
            if (this._length + items.length > this._maxLength) {
                var countToTrim = (this._length + items.length) - this._maxLength;
                this._startIndex += countToTrim;
                this._length = this._maxLength;
                this.emit('trim', countToTrim);
            }
            else {
                this._length += items.length;
            }
        }
    };
    CircularList.prototype.trimStart = function (count) {
        if (count > this._length) {
            count = this._length;
        }
        this._startIndex += count;
        this._length -= count;
        this.emit('trim', count);
    };
    CircularList.prototype.shiftElements = function (start, count, offset) {
        if (count <= 0) {
            return;
        }
        if (start < 0 || start >= this._length) {
            throw new Error('start argument out of range');
        }
        if (start + offset < 0) {
            throw new Error('Cannot shift elements in list beyond index 0');
        }
        if (offset > 0) {
            for (var i = count - 1; i >= 0; i--) {
                this.set(start + i + offset, this.get(start + i));
            }
            var expandListBy = (start + count + offset) - this._length;
            if (expandListBy > 0) {
                this._length += expandListBy;
                while (this._length > this._maxLength) {
                    this._length--;
                    this._startIndex++;
                    this.emit('trim', 1);
                }
            }
        }
        else {
            for (var i = 0; i < count; i++) {
                this.set(start + i + offset, this.get(start + i));
            }
        }
    };
    CircularList.prototype._getCyclicIndex = function (index) {
        return (this._startIndex + index) % this._maxLength;
    };
    return CircularList;
}(EventEmitter_1.EventEmitter));
exports.CircularList = CircularList;

},{"./EventEmitter":17}],17:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Lifecycle_1 = require("./Lifecycle");
var EventEmitter = (function (_super) {
    __extends(EventEmitter, _super);
    function EventEmitter() {
        var _this = _super.call(this) || this;
        _this._events = _this._events || {};
        return _this;
    }
    EventEmitter.prototype.on = function (type, listener) {
        this._events[type] = this._events[type] || [];
        this._events[type].push(listener);
    };
    EventEmitter.prototype.addDisposableListener = function (type, handler) {
        var _this = this;
        this.on(type, handler);
        var disposed = false;
        return {
            dispose: function () {
                if (disposed) {
                    return;
                }
                _this.off(type, handler);
                disposed = true;
            }
        };
    };
    EventEmitter.prototype.off = function (type, listener) {
        if (!this._events[type]) {
            return;
        }
        var obj = this._events[type];
        var i = obj.length;
        while (i--) {
            if (obj[i] === listener) {
                obj.splice(i, 1);
                return;
            }
        }
    };
    EventEmitter.prototype.removeAllListeners = function (type) {
        if (this._events[type]) {
            delete this._events[type];
        }
    };
    EventEmitter.prototype.emit = function (type) {
        var args = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            args[_i - 1] = arguments[_i];
        }
        if (!this._events[type]) {
            return;
        }
        var obj = this._events[type];
        for (var i = 0; i < obj.length; i++) {
            obj[i].apply(this, args);
        }
    };
    EventEmitter.prototype.listeners = function (type) {
        return this._events[type] || [];
    };
    EventEmitter.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._events = {};
    };
    return EventEmitter;
}(Lifecycle_1.Disposable));
exports.EventEmitter = EventEmitter;

},{"./Lifecycle":18}],18:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Disposable = (function () {
    function Disposable() {
        this._disposables = [];
        this._isDisposed = false;
    }
    Disposable.prototype.dispose = function () {
        this._isDisposed = true;
        this._disposables.forEach(function (d) { return d.dispose(); });
        this._disposables.length = 0;
    };
    Disposable.prototype.register = function (d) {
        this._disposables.push(d);
    };
    Disposable.prototype.unregister = function (d) {
        var index = this._disposables.indexOf(d);
        if (index !== -1) {
            this._disposables.splice(index, 1);
        }
    };
    return Disposable;
}());
exports.Disposable = Disposable;

},{}],19:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function fill(array, value, start, end) {
    if (array.fill) {
        return array.fill(value, start, end);
    }
    return fillFallback(array, value, start, end);
}
exports.fill = fill;
function fillFallback(array, value, start, end) {
    if (start === void 0) { start = 0; }
    if (end === void 0) { end = array.length; }
    if (start >= array.length) {
        return array;
    }
    start = (array.length + start) % array.length;
    if (end >= array.length) {
        end = array.length;
    }
    else {
        end = (array.length + end) % array.length;
    }
    for (var i = start; i < end; ++i) {
        array[i] = value;
    }
    return array;
}
exports.fillFallback = fillFallback;

},{}],20:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var C0;
(function (C0) {
    C0.NUL = '\x00';
    C0.SOH = '\x01';
    C0.STX = '\x02';
    C0.ETX = '\x03';
    C0.EOT = '\x04';
    C0.ENQ = '\x05';
    C0.ACK = '\x06';
    C0.BEL = '\x07';
    C0.BS = '\x08';
    C0.HT = '\x09';
    C0.LF = '\x0a';
    C0.VT = '\x0b';
    C0.FF = '\x0c';
    C0.CR = '\x0d';
    C0.SO = '\x0e';
    C0.SI = '\x0f';
    C0.DLE = '\x10';
    C0.DC1 = '\x11';
    C0.DC2 = '\x12';
    C0.DC3 = '\x13';
    C0.DC4 = '\x14';
    C0.NAK = '\x15';
    C0.SYN = '\x16';
    C0.ETB = '\x17';
    C0.CAN = '\x18';
    C0.EM = '\x19';
    C0.SUB = '\x1a';
    C0.ESC = '\x1b';
    C0.FS = '\x1c';
    C0.GS = '\x1d';
    C0.RS = '\x1e';
    C0.US = '\x1f';
    C0.SP = '\x20';
    C0.DEL = '\x7f';
})(C0 = exports.C0 || (exports.C0 = {}));
var C1;
(function (C1) {
    C1.PAD = '\x80';
    C1.HOP = '\x81';
    C1.BPH = '\x82';
    C1.NBH = '\x83';
    C1.IND = '\x84';
    C1.NEL = '\x85';
    C1.SSA = '\x86';
    C1.ESA = '\x87';
    C1.HTS = '\x88';
    C1.HTJ = '\x89';
    C1.VTS = '\x8a';
    C1.PLD = '\x8b';
    C1.PLU = '\x8c';
    C1.RI = '\x8d';
    C1.SS2 = '\x8e';
    C1.SS3 = '\x8f';
    C1.DCS = '\x90';
    C1.PU1 = '\x91';
    C1.PU2 = '\x92';
    C1.STS = '\x93';
    C1.CCH = '\x94';
    C1.MW = '\x95';
    C1.SPA = '\x96';
    C1.EPA = '\x97';
    C1.SOS = '\x98';
    C1.SGCI = '\x99';
    C1.SCI = '\x9a';
    C1.CSI = '\x9b';
    C1.ST = '\x9c';
    C1.OSC = '\x9d';
    C1.PM = '\x9e';
    C1.APC = '\x9f';
})(C1 = exports.C1 || (exports.C1 = {}));

},{}],21:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var isNode = (typeof navigator === 'undefined') ? true : false;
var userAgent = (isNode) ? 'node' : navigator.userAgent;
var platform = (isNode) ? 'node' : navigator.platform;
exports.isFirefox = !!~userAgent.indexOf('Firefox');
exports.isSafari = /^((?!chrome|android).)*safari/i.test(userAgent);
exports.isMSIE = !!~userAgent.indexOf('MSIE') || !!~userAgent.indexOf('Trident');
exports.isMac = contains(['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'], platform);
exports.isIpad = platform === 'iPad';
exports.isIphone = platform === 'iPhone';
exports.isMSWindows = contains(['Windows', 'Win16', 'Win32', 'WinCE'], platform);
exports.isLinux = platform.indexOf('Linux') >= 0;
function contains(arr, el) {
    return arr.indexOf(el) >= 0;
}

},{}],22:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CHARSETS = {};
exports.DEFAULT_CHARSET = exports.CHARSETS['B'];
exports.CHARSETS['0'] = {
    '`': '\u25c6',
    'a': '\u2592',
    'b': '\u0009',
    'c': '\u000c',
    'd': '\u000d',
    'e': '\u000a',
    'f': '\u00b0',
    'g': '\u00b1',
    'h': '\u2424',
    'i': '\u000b',
    'j': '\u2518',
    'k': '\u2510',
    'l': '\u250c',
    'm': '\u2514',
    'n': '\u253c',
    'o': '\u23ba',
    'p': '\u23bb',
    'q': '\u2500',
    'r': '\u23bc',
    's': '\u23bd',
    't': '\u251c',
    'u': '\u2524',
    'v': '\u2534',
    'w': '\u252c',
    'x': '\u2502',
    'y': '\u2264',
    'z': '\u2265',
    '{': '\u03c0',
    '|': '\u2260',
    '}': '\u00a3',
    '~': '\u00b7'
};
exports.CHARSETS['A'] = {
    '#': '£'
};
exports.CHARSETS['B'] = null;
exports.CHARSETS['4'] = {
    '#': '£',
    '@': '¾',
    '[': 'ij',
    '\\': '½',
    ']': '|',
    '{': '¨',
    '|': 'f',
    '}': '¼',
    '~': '´'
};
exports.CHARSETS['C'] =
    exports.CHARSETS['5'] = {
        '[': 'Ä',
        '\\': 'Ö',
        ']': 'Å',
        '^': 'Ü',
        '`': 'é',
        '{': 'ä',
        '|': 'ö',
        '}': 'å',
        '~': 'ü'
    };
exports.CHARSETS['R'] = {
    '#': '£',
    '@': 'à',
    '[': '°',
    '\\': 'ç',
    ']': '§',
    '{': 'é',
    '|': 'ù',
    '}': 'è',
    '~': '¨'
};
exports.CHARSETS['Q'] = {
    '@': 'à',
    '[': 'â',
    '\\': 'ç',
    ']': 'ê',
    '^': 'î',
    '`': 'ô',
    '{': 'é',
    '|': 'ù',
    '}': 'è',
    '~': 'û'
};
exports.CHARSETS['K'] = {
    '@': '§',
    '[': 'Ä',
    '\\': 'Ö',
    ']': 'Ü',
    '{': 'ä',
    '|': 'ö',
    '}': 'ü',
    '~': 'ß'
};
exports.CHARSETS['Y'] = {
    '#': '£',
    '@': '§',
    '[': '°',
    '\\': 'ç',
    ']': 'é',
    '`': 'ù',
    '{': 'à',
    '|': 'ò',
    '}': 'è',
    '~': 'ì'
};
exports.CHARSETS['E'] =
    exports.CHARSETS['6'] = {
        '@': 'Ä',
        '[': 'Æ',
        '\\': 'Ø',
        ']': 'Å',
        '^': 'Ü',
        '`': 'ä',
        '{': 'æ',
        '|': 'ø',
        '}': 'å',
        '~': 'ü'
    };
exports.CHARSETS['Z'] = {
    '#': '£',
    '@': '§',
    '[': '¡',
    '\\': 'Ñ',
    ']': '¿',
    '{': '°',
    '|': 'ñ',
    '}': 'ç'
};
exports.CHARSETS['H'] =
    exports.CHARSETS['7'] = {
        '@': 'É',
        '[': 'Ä',
        '\\': 'Ö',
        ']': 'Å',
        '^': 'Ü',
        '`': 'é',
        '{': 'ä',
        '|': 'ö',
        '}': 'å',
        '~': 'ü'
    };
exports.CHARSETS['='] = {
    '#': 'ù',
    '@': 'à',
    '[': 'é',
    '\\': 'ç',
    ']': 'ê',
    '^': 'î',
    '_': 'è',
    '`': 'ô',
    '{': 'ä',
    '|': 'ö',
    '}': 'ü',
    '~': 'û'
};

},{}],23:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var EscapeSequences_1 = require("../../common/data/EscapeSequences");
var KEYCODE_KEY_MAPPINGS = {
    48: ['0', ')'],
    49: ['1', '!'],
    50: ['2', '@'],
    51: ['3', '#'],
    52: ['4', '$'],
    53: ['5', '%'],
    54: ['6', '^'],
    55: ['7', '&'],
    56: ['8', '*'],
    57: ['9', '('],
    186: [';', ':'],
    187: ['=', '+'],
    188: [',', '<'],
    189: ['-', '_'],
    190: ['.', '>'],
    191: ['/', '?'],
    192: ['`', '~'],
    219: ['[', '{'],
    220: ['\\', '|'],
    221: [']', '}'],
    222: ['\'', '"']
};
function evaluateKeyboardEvent(ev, applicationCursorMode, isMac, macOptionIsMeta) {
    var result = {
        type: 0,
        cancel: false,
        key: undefined
    };
    var modifiers = (ev.shiftKey ? 1 : 0) | (ev.altKey ? 2 : 0) | (ev.ctrlKey ? 4 : 0) | (ev.metaKey ? 8 : 0);
    switch (ev.keyCode) {
        case 0:
            if (ev.key === 'UIKeyInputUpArrow') {
                if (applicationCursorMode) {
                    result.key = EscapeSequences_1.C0.ESC + 'OA';
                }
                else {
                    result.key = EscapeSequences_1.C0.ESC + '[A';
                }
            }
            else if (ev.key === 'UIKeyInputLeftArrow') {
                if (applicationCursorMode) {
                    result.key = EscapeSequences_1.C0.ESC + 'OD';
                }
                else {
                    result.key = EscapeSequences_1.C0.ESC + '[D';
                }
            }
            else if (ev.key === 'UIKeyInputRightArrow') {
                if (applicationCursorMode) {
                    result.key = EscapeSequences_1.C0.ESC + 'OC';
                }
                else {
                    result.key = EscapeSequences_1.C0.ESC + '[C';
                }
            }
            else if (ev.key === 'UIKeyInputDownArrow') {
                if (applicationCursorMode) {
                    result.key = EscapeSequences_1.C0.ESC + 'OB';
                }
                else {
                    result.key = EscapeSequences_1.C0.ESC + '[B';
                }
            }
            break;
        case 8:
            if (ev.shiftKey) {
                result.key = EscapeSequences_1.C0.BS;
                break;
            }
            else if (ev.altKey) {
                result.key = EscapeSequences_1.C0.ESC + EscapeSequences_1.C0.DEL;
                break;
            }
            result.key = EscapeSequences_1.C0.DEL;
            break;
        case 9:
            if (ev.shiftKey) {
                result.key = EscapeSequences_1.C0.ESC + '[Z';
                break;
            }
            result.key = EscapeSequences_1.C0.HT;
            result.cancel = true;
            break;
        case 13:
            result.key = EscapeSequences_1.C0.CR;
            result.cancel = true;
            break;
        case 27:
            result.key = EscapeSequences_1.C0.ESC;
            result.cancel = true;
            break;
        case 37:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'D';
                if (result.key === EscapeSequences_1.C0.ESC + '[1;3D') {
                    result.key = isMac ? EscapeSequences_1.C0.ESC + 'b' : EscapeSequences_1.C0.ESC + '[1;5D';
                }
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OD';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[D';
            }
            break;
        case 39:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'C';
                if (result.key === EscapeSequences_1.C0.ESC + '[1;3C') {
                    result.key = isMac ? EscapeSequences_1.C0.ESC + 'f' : EscapeSequences_1.C0.ESC + '[1;5C';
                }
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OC';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[C';
            }
            break;
        case 38:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'A';
                if (result.key === EscapeSequences_1.C0.ESC + '[1;3A') {
                    result.key = EscapeSequences_1.C0.ESC + '[1;5A';
                }
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OA';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[A';
            }
            break;
        case 40:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'B';
                if (result.key === EscapeSequences_1.C0.ESC + '[1;3B') {
                    result.key = EscapeSequences_1.C0.ESC + '[1;5B';
                }
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OB';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[B';
            }
            break;
        case 45:
            if (!ev.shiftKey && !ev.ctrlKey) {
                result.key = EscapeSequences_1.C0.ESC + '[2~';
            }
            break;
        case 46:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[3;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[3~';
            }
            break;
        case 36:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'H';
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OH';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[H';
            }
            break;
        case 35:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'F';
            }
            else if (applicationCursorMode) {
                result.key = EscapeSequences_1.C0.ESC + 'OF';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[F';
            }
            break;
        case 33:
            if (ev.shiftKey) {
                result.type = 2;
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[5~';
            }
            break;
        case 34:
            if (ev.shiftKey) {
                result.type = 3;
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[6~';
            }
            break;
        case 112:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'P';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + 'OP';
            }
            break;
        case 113:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'Q';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + 'OQ';
            }
            break;
        case 114:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'R';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + 'OR';
            }
            break;
        case 115:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[1;' + (modifiers + 1) + 'S';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + 'OS';
            }
            break;
        case 116:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[15;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[15~';
            }
            break;
        case 117:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[17;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[17~';
            }
            break;
        case 118:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[18;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[18~';
            }
            break;
        case 119:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[19;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[19~';
            }
            break;
        case 120:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[20;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[20~';
            }
            break;
        case 121:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[21;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[21~';
            }
            break;
        case 122:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[23;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[23~';
            }
            break;
        case 123:
            if (modifiers) {
                result.key = EscapeSequences_1.C0.ESC + '[24;' + (modifiers + 1) + '~';
            }
            else {
                result.key = EscapeSequences_1.C0.ESC + '[24~';
            }
            break;
        default:
            if (ev.ctrlKey && !ev.shiftKey && !ev.altKey && !ev.metaKey) {
                if (ev.keyCode >= 65 && ev.keyCode <= 90) {
                    result.key = String.fromCharCode(ev.keyCode - 64);
                }
                else if (ev.keyCode === 32) {
                    result.key = String.fromCharCode(0);
                }
                else if (ev.keyCode >= 51 && ev.keyCode <= 55) {
                    result.key = String.fromCharCode(ev.keyCode - 51 + 27);
                }
                else if (ev.keyCode === 56) {
                    result.key = String.fromCharCode(127);
                }
                else if (ev.keyCode === 219) {
                    result.key = String.fromCharCode(27);
                }
                else if (ev.keyCode === 220) {
                    result.key = String.fromCharCode(28);
                }
                else if (ev.keyCode === 221) {
                    result.key = String.fromCharCode(29);
                }
            }
            else if ((!isMac || macOptionIsMeta) && ev.altKey && !ev.metaKey) {
                var keyMapping = KEYCODE_KEY_MAPPINGS[ev.keyCode];
                var key = keyMapping && keyMapping[!ev.shiftKey ? 0 : 1];
                if (key) {
                    result.key = EscapeSequences_1.C0.ESC + key;
                }
                else if (ev.keyCode >= 65 && ev.keyCode <= 90) {
                    var keyCode = ev.ctrlKey ? ev.keyCode - 64 : ev.keyCode + 32;
                    result.key = EscapeSequences_1.C0.ESC + String.fromCharCode(keyCode);
                }
            }
            else if (isMac && !ev.altKey && !ev.ctrlKey && ev.metaKey) {
                if (ev.keyCode === 65) {
                    result.type = 1;
                }
            }
            break;
    }
    return result;
}
exports.evaluateKeyboardEvent = evaluateKeyboardEvent;

},{"../../common/data/EscapeSequences":20}],24:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var EscapeSequences_1 = require("../common/data/EscapeSequences");
var AltClickHandler = (function () {
    function AltClickHandler(_mouseEvent, _terminal) {
        var _a;
        this._mouseEvent = _mouseEvent;
        this._terminal = _terminal;
        this._lines = this._terminal.buffer.lines;
        this._startCol = this._terminal.buffer.x;
        this._startRow = this._terminal.buffer.y;
        var coordinates = this._terminal.mouseHelper.getCoords(this._mouseEvent, this._terminal.element, this._terminal.charMeasure, this._terminal.cols, this._terminal.rows, false);
        if (coordinates) {
            _a = coordinates.map(function (coordinate) {
                return coordinate - 1;
            }), this._endCol = _a[0], this._endRow = _a[1];
        }
    }
    AltClickHandler.prototype.move = function () {
        if (this._mouseEvent.altKey && this._endCol !== undefined && this._endRow !== undefined) {
            this._terminal.handler(this._arrowSequences());
        }
    };
    AltClickHandler.prototype._arrowSequences = function () {
        if (!this._terminal.buffer.hasScrollback) {
            return this._resetStartingRow() + this._moveToRequestedRow() + this._moveToRequestedCol();
        }
        return this._moveHorizontallyOnly();
    };
    AltClickHandler.prototype._resetStartingRow = function () {
        if (this._moveToRequestedRow().length === 0) {
            return '';
        }
        return repeat(this._bufferLine(this._startCol, this._startRow, this._startCol, this._startRow - this._wrappedRowsForRow(this._startRow), false).length, this._sequence("D"));
    };
    AltClickHandler.prototype._moveToRequestedRow = function () {
        var startRow = this._startRow - this._wrappedRowsForRow(this._startRow);
        var endRow = this._endRow - this._wrappedRowsForRow(this._endRow);
        var rowsToMove = Math.abs(startRow - endRow) - this._wrappedRowsCount();
        return repeat(rowsToMove, this._sequence(this._verticalDirection()));
    };
    AltClickHandler.prototype._moveToRequestedCol = function () {
        var startRow;
        if (this._moveToRequestedRow().length > 0) {
            startRow = this._endRow - this._wrappedRowsForRow(this._endRow);
        }
        else {
            startRow = this._startRow;
        }
        var endRow = this._endRow;
        var direction = this._horizontalDirection();
        return repeat(this._bufferLine(this._startCol, startRow, this._endCol, endRow, direction === "C").length, this._sequence(direction));
    };
    AltClickHandler.prototype._moveHorizontallyOnly = function () {
        var direction = this._horizontalDirection();
        return repeat(Math.abs(this._startCol - this._endCol), this._sequence(direction));
    };
    AltClickHandler.prototype._wrappedRowsCount = function () {
        var wrappedRows = 0;
        var startRow = this._startRow - this._wrappedRowsForRow(this._startRow);
        var endRow = this._endRow - this._wrappedRowsForRow(this._endRow);
        for (var i = 0; i < Math.abs(startRow - endRow); i++) {
            var direction = this._verticalDirection() === "A" ? -1 : 1;
            if (this._lines.get(startRow + (direction * i)).isWrapped) {
                wrappedRows++;
            }
        }
        return wrappedRows;
    };
    AltClickHandler.prototype._wrappedRowsForRow = function (currentRow) {
        var rowCount = 0;
        var lineWraps = this._lines.get(currentRow).isWrapped;
        while (lineWraps && currentRow >= 0 && currentRow < this._terminal.rows) {
            rowCount++;
            currentRow--;
            lineWraps = this._lines.get(currentRow).isWrapped;
        }
        return rowCount;
    };
    AltClickHandler.prototype._horizontalDirection = function () {
        var startRow;
        if (this._moveToRequestedRow().length > 0) {
            startRow = this._endRow - this._wrappedRowsForRow(this._endRow);
        }
        else {
            startRow = this._startRow;
        }
        if ((this._startCol < this._endCol &&
            startRow <= this._endRow) ||
            (this._startCol >= this._endCol &&
                startRow < this._endRow)) {
            return "C";
        }
        return "D";
    };
    AltClickHandler.prototype._verticalDirection = function () {
        if (this._startRow > this._endRow) {
            return "A";
        }
        return "B";
    };
    AltClickHandler.prototype._bufferLine = function (startCol, startRow, endCol, endRow, forward) {
        var currentCol = startCol;
        var currentRow = startRow;
        var bufferStr = '';
        while (currentCol !== endCol || currentRow !== endRow) {
            currentCol += forward ? 1 : -1;
            if (forward && currentCol > this._terminal.cols - 1) {
                bufferStr += this._terminal.buffer.translateBufferLineToString(currentRow, false, startCol, currentCol);
                currentCol = 0;
                startCol = 0;
                currentRow++;
            }
            else if (!forward && currentCol < 0) {
                bufferStr += this._terminal.buffer.translateBufferLineToString(currentRow, false, 0, startCol + 1);
                currentCol = this._terminal.cols - 1;
                startCol = currentCol;
                currentRow--;
            }
        }
        return bufferStr + this._terminal.buffer.translateBufferLineToString(currentRow, false, startCol, currentCol);
    };
    AltClickHandler.prototype._sequence = function (direction) {
        var mod = this._terminal.applicationCursor ? 'O' : '[';
        return EscapeSequences_1.C0.ESC + mod + direction;
    };
    return AltClickHandler;
}());
exports.AltClickHandler = AltClickHandler;
function repeat(count, str) {
    count = Math.floor(count);
    var rpt = '';
    for (var i = 0; i < count; i++) {
        rpt += str;
    }
    return rpt;
}

},{"../common/data/EscapeSequences":20}],25:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Terminal_1 = require("../Terminal");
var Strings = require("../Strings");
var Terminal = (function () {
    function Terminal(options) {
        this._core = new Terminal_1.Terminal(options);
    }
    Object.defineProperty(Terminal.prototype, "element", {
        get: function () { return this._core.element; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Terminal.prototype, "textarea", {
        get: function () { return this._core.textarea; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Terminal.prototype, "rows", {
        get: function () { return this._core.rows; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Terminal.prototype, "cols", {
        get: function () { return this._core.cols; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(Terminal.prototype, "markers", {
        get: function () { return this._core.markers; },
        enumerable: true,
        configurable: true
    });
    Terminal.prototype.blur = function () {
        this._core.blur();
    };
    Terminal.prototype.focus = function () {
        this._core.focus();
    };
    Terminal.prototype.on = function (type, listener) {
        this._core.on(type, listener);
    };
    Terminal.prototype.off = function (type, listener) {
        this._core.off(type, listener);
    };
    Terminal.prototype.emit = function (type, data) {
        this._core.emit(type, data);
    };
    Terminal.prototype.addDisposableListener = function (type, handler) {
        return this._core.addDisposableListener(type, handler);
    };
    Terminal.prototype.resize = function (columns, rows) {
        this._core.resize(columns, rows);
    };
    Terminal.prototype.writeln = function (data) {
        this._core.writeln(data);
    };
    Terminal.prototype.open = function (parent) {
        this._core.open(parent);
    };
    Terminal.prototype.attachCustomKeyEventHandler = function (customKeyEventHandler) {
        this._core.attachCustomKeyEventHandler(customKeyEventHandler);
    };
    Terminal.prototype.registerLinkMatcher = function (regex, handler, options) {
        return this._core.registerLinkMatcher(regex, handler, options);
    };
    Terminal.prototype.deregisterLinkMatcher = function (matcherId) {
        this._core.deregisterLinkMatcher(matcherId);
    };
    Terminal.prototype.registerCharacterJoiner = function (handler) {
        return this._core.registerCharacterJoiner(handler);
    };
    Terminal.prototype.deregisterCharacterJoiner = function (joinerId) {
        this._core.deregisterCharacterJoiner(joinerId);
    };
    Terminal.prototype.addMarker = function (cursorYOffset) {
        return this._core.addMarker(cursorYOffset);
    };
    Terminal.prototype.hasSelection = function () {
        return this._core.hasSelection();
    };
    Terminal.prototype.getSelection = function () {
        return this._core.getSelection();
    };
    Terminal.prototype.clearSelection = function () {
        this._core.clearSelection();
    };
    Terminal.prototype.selectAll = function () {
        this._core.selectAll();
    };
    Terminal.prototype.selectLines = function (start, end) {
        this._core.selectLines(start, end);
    };
    Terminal.prototype.dispose = function () {
        this._core.dispose();
    };
    Terminal.prototype.destroy = function () {
        this._core.destroy();
    };
    Terminal.prototype.scrollLines = function (amount) {
        this._core.scrollLines(amount);
    };
    Terminal.prototype.scrollPages = function (pageCount) {
        this._core.scrollPages(pageCount);
    };
    Terminal.prototype.scrollToTop = function () {
        this._core.scrollToTop();
    };
    Terminal.prototype.scrollToBottom = function () {
        this._core.scrollToBottom();
    };
    Terminal.prototype.scrollToLine = function (line) {
        this._core.scrollToLine(line);
    };
    Terminal.prototype.clear = function () {
        this._core.clear();
    };
    Terminal.prototype.write = function (data) {
        this._core.write(data);
    };
    Terminal.prototype.getOption = function (key) {
        return this._core.getOption(key);
    };
    Terminal.prototype.setOption = function (key, value) {
        this._core.setOption(key, value);
    };
    Terminal.prototype.refresh = function (start, end) {
        this._core.refresh(start, end);
    };
    Terminal.prototype.reset = function () {
        this._core.reset();
    };
    Terminal.applyAddon = function (addon) {
        addon.apply(Terminal);
    };
    Object.defineProperty(Terminal, "strings", {
        get: function () {
            return Strings;
        },
        enumerable: true,
        configurable: true
    });
    return Terminal;
}());
exports.Terminal = Terminal;

},{"../Strings":13,"../Terminal":14}],26:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Types_1 = require("./atlas/Types");
var CharAtlasCache_1 = require("./atlas/CharAtlasCache");
var Buffer_1 = require("../Buffer");
var CharAtlasUtils_1 = require("./atlas/CharAtlasUtils");
var BaseRenderLayer = (function () {
    function BaseRenderLayer(_container, id, zIndex, _alpha, _colors) {
        this._container = _container;
        this._alpha = _alpha;
        this._colors = _colors;
        this._scaledCharWidth = 0;
        this._scaledCharHeight = 0;
        this._scaledCellWidth = 0;
        this._scaledCellHeight = 0;
        this._scaledCharLeft = 0;
        this._scaledCharTop = 0;
        this._currentGlyphIdentifier = {
            chars: '',
            code: 0,
            bg: 0,
            fg: 0,
            bold: false,
            dim: false,
            italic: false
        };
        this._canvas = document.createElement('canvas');
        this._canvas.classList.add("xterm-" + id + "-layer");
        this._canvas.style.zIndex = zIndex.toString();
        this._initCanvas();
        this._container.appendChild(this._canvas);
    }
    BaseRenderLayer.prototype.dispose = function () {
        this._container.removeChild(this._canvas);
        if (this._charAtlas) {
            this._charAtlas.dispose();
        }
    };
    BaseRenderLayer.prototype._initCanvas = function () {
        this._ctx = this._canvas.getContext('2d', { alpha: this._alpha });
        if (!this._alpha) {
            this.clearAll();
        }
    };
    BaseRenderLayer.prototype.onOptionsChanged = function (terminal) { };
    BaseRenderLayer.prototype.onBlur = function (terminal) { };
    BaseRenderLayer.prototype.onFocus = function (terminal) { };
    BaseRenderLayer.prototype.onCursorMove = function (terminal) { };
    BaseRenderLayer.prototype.onGridChanged = function (terminal, startRow, endRow) { };
    BaseRenderLayer.prototype.onSelectionChanged = function (terminal, start, end, columnSelectMode) {
        if (columnSelectMode === void 0) { columnSelectMode = false; }
    };
    BaseRenderLayer.prototype.onThemeChanged = function (terminal, colorSet) {
        this._refreshCharAtlas(terminal, colorSet);
    };
    BaseRenderLayer.prototype.setTransparency = function (terminal, alpha) {
        if (alpha === this._alpha) {
            return;
        }
        var oldCanvas = this._canvas;
        this._alpha = alpha;
        this._canvas = this._canvas.cloneNode();
        this._initCanvas();
        this._container.replaceChild(this._canvas, oldCanvas);
        this._refreshCharAtlas(terminal, this._colors);
        this.onGridChanged(terminal, 0, terminal.rows - 1);
    };
    BaseRenderLayer.prototype._refreshCharAtlas = function (terminal, colorSet) {
        if (this._scaledCharWidth <= 0 && this._scaledCharHeight <= 0) {
            return;
        }
        this._charAtlas = CharAtlasCache_1.acquireCharAtlas(terminal, colorSet, this._scaledCharWidth, this._scaledCharHeight);
        this._charAtlas.warmUp();
    };
    BaseRenderLayer.prototype.resize = function (terminal, dim) {
        this._scaledCellWidth = dim.scaledCellWidth;
        this._scaledCellHeight = dim.scaledCellHeight;
        this._scaledCharWidth = dim.scaledCharWidth;
        this._scaledCharHeight = dim.scaledCharHeight;
        this._scaledCharLeft = dim.scaledCharLeft;
        this._scaledCharTop = dim.scaledCharTop;
        this._canvas.width = dim.scaledCanvasWidth;
        this._canvas.height = dim.scaledCanvasHeight;
        this._canvas.style.width = dim.canvasWidth + "px";
        this._canvas.style.height = dim.canvasHeight + "px";
        if (!this._alpha) {
            this.clearAll();
        }
        this._refreshCharAtlas(terminal, this._colors);
    };
    BaseRenderLayer.prototype.fillCells = function (x, y, width, height) {
        this._ctx.fillRect(x * this._scaledCellWidth, y * this._scaledCellHeight, width * this._scaledCellWidth, height * this._scaledCellHeight);
    };
    BaseRenderLayer.prototype.fillBottomLineAtCells = function (x, y, width) {
        if (width === void 0) { width = 1; }
        this._ctx.fillRect(x * this._scaledCellWidth, (y + 1) * this._scaledCellHeight - window.devicePixelRatio - 1, width * this._scaledCellWidth, window.devicePixelRatio);
    };
    BaseRenderLayer.prototype.fillLeftLineAtCell = function (x, y) {
        this._ctx.fillRect(x * this._scaledCellWidth, y * this._scaledCellHeight, window.devicePixelRatio, this._scaledCellHeight);
    };
    BaseRenderLayer.prototype.strokeRectAtCell = function (x, y, width, height) {
        this._ctx.lineWidth = window.devicePixelRatio;
        this._ctx.strokeRect(x * this._scaledCellWidth + window.devicePixelRatio / 2, y * this._scaledCellHeight + (window.devicePixelRatio / 2), width * this._scaledCellWidth - window.devicePixelRatio, (height * this._scaledCellHeight) - window.devicePixelRatio);
    };
    BaseRenderLayer.prototype.clearAll = function () {
        if (this._alpha) {
            this._ctx.clearRect(0, 0, this._canvas.width, this._canvas.height);
        }
        else {
            this._ctx.fillStyle = this._colors.background.css;
            this._ctx.fillRect(0, 0, this._canvas.width, this._canvas.height);
        }
    };
    BaseRenderLayer.prototype.clearCells = function (x, y, width, height) {
        if (this._alpha) {
            this._ctx.clearRect(x * this._scaledCellWidth, y * this._scaledCellHeight, width * this._scaledCellWidth, height * this._scaledCellHeight);
        }
        else {
            this._ctx.fillStyle = this._colors.background.css;
            this._ctx.fillRect(x * this._scaledCellWidth, y * this._scaledCellHeight, width * this._scaledCellWidth, height * this._scaledCellHeight);
        }
    };
    BaseRenderLayer.prototype.fillCharTrueColor = function (terminal, charData, x, y) {
        this._ctx.font = this._getFont(terminal, false, false);
        this._ctx.textBaseline = 'top';
        this._clipRow(terminal, y);
        this._ctx.fillText(charData[Buffer_1.CHAR_DATA_CHAR_INDEX], x * this._scaledCellWidth + this._scaledCharLeft, y * this._scaledCellHeight + this._scaledCharTop);
    };
    BaseRenderLayer.prototype.drawChars = function (terminal, chars, code, width, x, y, fg, bg, bold, dim, italic) {
        var drawInBrightColor = terminal.options.drawBoldTextInBrightColors && bold && fg < 8 && fg !== Types_1.INVERTED_DEFAULT_COLOR;
        fg += drawInBrightColor ? 8 : 0;
        this._currentGlyphIdentifier.chars = chars;
        this._currentGlyphIdentifier.code = code;
        this._currentGlyphIdentifier.bg = bg;
        this._currentGlyphIdentifier.fg = fg;
        this._currentGlyphIdentifier.bold = bold && terminal.options.enableBold;
        this._currentGlyphIdentifier.dim = dim;
        this._currentGlyphIdentifier.italic = italic;
        var atlasDidDraw = this._charAtlas && this._charAtlas.draw(this._ctx, this._currentGlyphIdentifier, x * this._scaledCellWidth + this._scaledCharLeft, y * this._scaledCellHeight + this._scaledCharTop);
        if (!atlasDidDraw) {
            this._drawUncachedChars(terminal, chars, width, fg, x, y, bold && terminal.options.enableBold, dim, italic);
        }
    };
    BaseRenderLayer.prototype._drawUncachedChars = function (terminal, chars, width, fg, x, y, bold, dim, italic) {
        this._ctx.save();
        this._ctx.font = this._getFont(terminal, bold, italic);
        this._ctx.textBaseline = 'top';
        if (fg === Types_1.INVERTED_DEFAULT_COLOR) {
            this._ctx.fillStyle = this._colors.background.css;
        }
        else if (CharAtlasUtils_1.is256Color(fg)) {
            this._ctx.fillStyle = this._colors.ansi[fg].css;
        }
        else {
            this._ctx.fillStyle = this._colors.foreground.css;
        }
        this._clipRow(terminal, y);
        if (dim) {
            this._ctx.globalAlpha = Types_1.DIM_OPACITY;
        }
        this._ctx.fillText(chars, x * this._scaledCellWidth + this._scaledCharLeft, y * this._scaledCellHeight + this._scaledCharTop);
        this._ctx.restore();
    };
    BaseRenderLayer.prototype._clipRow = function (terminal, y) {
        this._ctx.beginPath();
        this._ctx.rect(0, y * this._scaledCellHeight, terminal.cols * this._scaledCellWidth, this._scaledCellHeight);
        this._ctx.clip();
    };
    BaseRenderLayer.prototype._getFont = function (terminal, isBold, isItalic) {
        var fontWeight = isBold ? terminal.options.fontWeightBold : terminal.options.fontWeight;
        var fontStyle = isItalic ? 'italic' : '';
        return fontStyle + " " + fontWeight + " " + terminal.options.fontSize * window.devicePixelRatio + "px " + terminal.options.fontFamily;
    };
    return BaseRenderLayer;
}());
exports.BaseRenderLayer = BaseRenderLayer;

},{"../Buffer":2,"./atlas/CharAtlasCache":36,"./atlas/CharAtlasUtils":38,"./atlas/Types":43}],27:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("../Buffer");
var CharacterJoinerRegistry = (function () {
    function CharacterJoinerRegistry(_terminal) {
        this._terminal = _terminal;
        this._characterJoiners = [];
        this._nextCharacterJoinerId = 0;
    }
    CharacterJoinerRegistry.prototype.registerCharacterJoiner = function (handler) {
        var joiner = {
            id: this._nextCharacterJoinerId++,
            handler: handler
        };
        this._characterJoiners.push(joiner);
        return joiner.id;
    };
    CharacterJoinerRegistry.prototype.deregisterCharacterJoiner = function (joinerId) {
        for (var i = 0; i < this._characterJoiners.length; i++) {
            if (this._characterJoiners[i].id === joinerId) {
                this._characterJoiners.splice(i, 1);
                return true;
            }
        }
        return false;
    };
    CharacterJoinerRegistry.prototype.getJoinedCharacters = function (row) {
        if (this._characterJoiners.length === 0) {
            return [];
        }
        var line = this._terminal.buffer.lines.get(row);
        if (line.length === 0) {
            return [];
        }
        var ranges = [];
        var lineStr = this._terminal.buffer.translateBufferLineToString(row, true);
        var rangeStartColumn = 0;
        var currentStringIndex = 0;
        var rangeStartStringIndex = 0;
        var rangeAttr = line.get(0)[Buffer_1.CHAR_DATA_ATTR_INDEX] >> 9;
        for (var x = 0; x < this._terminal.cols; x++) {
            var charData = line.get(x);
            var chars = charData[Buffer_1.CHAR_DATA_CHAR_INDEX];
            var width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
            var attr = charData[Buffer_1.CHAR_DATA_ATTR_INDEX] >> 9;
            if (width === 0) {
                continue;
            }
            if (attr !== rangeAttr) {
                if (x - rangeStartColumn > 1) {
                    var joinedRanges = this._getJoinedRanges(lineStr, rangeStartStringIndex, currentStringIndex, line, rangeStartColumn);
                    for (var i = 0; i < joinedRanges.length; i++) {
                        ranges.push(joinedRanges[i]);
                    }
                }
                rangeStartColumn = x;
                rangeStartStringIndex = currentStringIndex;
                rangeAttr = attr;
            }
            currentStringIndex += chars.length;
        }
        if (this._terminal.cols - rangeStartColumn > 1) {
            var joinedRanges = this._getJoinedRanges(lineStr, rangeStartStringIndex, currentStringIndex, line, rangeStartColumn);
            for (var i = 0; i < joinedRanges.length; i++) {
                ranges.push(joinedRanges[i]);
            }
        }
        return ranges;
    };
    CharacterJoinerRegistry.prototype._getJoinedRanges = function (line, startIndex, endIndex, lineData, startCol) {
        var text = line.substring(startIndex, endIndex);
        var joinedRanges = this._characterJoiners[0].handler(text);
        for (var i = 1; i < this._characterJoiners.length; i++) {
            var joinerRanges = this._characterJoiners[i].handler(text);
            for (var j = 0; j < joinerRanges.length; j++) {
                CharacterJoinerRegistry._mergeRanges(joinedRanges, joinerRanges[j]);
            }
        }
        this._stringRangesToCellRanges(joinedRanges, lineData, startCol);
        return joinedRanges;
    };
    CharacterJoinerRegistry.prototype._stringRangesToCellRanges = function (ranges, line, startCol) {
        var currentRangeIndex = 0;
        var currentRangeStarted = false;
        var currentStringIndex = 0;
        var currentRange = ranges[currentRangeIndex];
        if (!currentRange) {
            return;
        }
        for (var x = startCol; x < this._terminal.cols; x++) {
            var charData = line.get(x);
            var width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
            var length_1 = charData[Buffer_1.CHAR_DATA_CHAR_INDEX].length;
            if (width === 0) {
                continue;
            }
            if (!currentRangeStarted && currentRange[0] <= currentStringIndex) {
                currentRange[0] = x;
                currentRangeStarted = true;
            }
            if (currentRange[1] <= currentStringIndex) {
                currentRange[1] = x;
                currentRange = ranges[++currentRangeIndex];
                if (!currentRange) {
                    break;
                }
                if (currentRange[0] <= currentStringIndex) {
                    currentRange[0] = x;
                    currentRangeStarted = true;
                }
                else {
                    currentRangeStarted = false;
                }
            }
            currentStringIndex += length_1;
        }
        if (currentRange) {
            currentRange[1] = this._terminal.cols;
        }
    };
    CharacterJoinerRegistry._mergeRanges = function (ranges, newRange) {
        var inRange = false;
        for (var i = 0; i < ranges.length; i++) {
            var range = ranges[i];
            if (!inRange) {
                if (newRange[1] <= range[0]) {
                    ranges.splice(i, 0, newRange);
                    return ranges;
                }
                if (newRange[1] <= range[1]) {
                    range[0] = Math.min(newRange[0], range[0]);
                    return ranges;
                }
                if (newRange[0] < range[1]) {
                    range[0] = Math.min(newRange[0], range[0]);
                    inRange = true;
                }
                continue;
            }
            else {
                if (newRange[1] <= range[0]) {
                    ranges[i - 1][1] = newRange[1];
                    return ranges;
                }
                if (newRange[1] <= range[1]) {
                    ranges[i - 1][1] = Math.max(newRange[1], range[1]);
                    ranges.splice(i, 1);
                    inRange = false;
                    return ranges;
                }
                ranges.splice(i, 1);
                i--;
            }
        }
        if (inRange) {
            ranges[ranges.length - 1][1] = newRange[1];
        }
        else {
            ranges.push(newRange);
        }
        return ranges;
    };
    return CharacterJoinerRegistry;
}());
exports.CharacterJoinerRegistry = CharacterJoinerRegistry;

},{"../Buffer":2}],28:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var DEFAULT_FOREGROUND = fromHex('#ffffff');
var DEFAULT_BACKGROUND = fromHex('#000000');
var DEFAULT_CURSOR = fromHex('#ffffff');
var DEFAULT_CURSOR_ACCENT = fromHex('#000000');
var DEFAULT_SELECTION = {
    css: 'rgba(255, 255, 255, 0.3)',
    rgba: 0xFFFFFF77
};
exports.DEFAULT_ANSI_COLORS = (function () {
    var colors = [
        fromHex('#2e3436'),
        fromHex('#cc0000'),
        fromHex('#4e9a06'),
        fromHex('#c4a000'),
        fromHex('#3465a4'),
        fromHex('#75507b'),
        fromHex('#06989a'),
        fromHex('#d3d7cf'),
        fromHex('#555753'),
        fromHex('#ef2929'),
        fromHex('#8ae234'),
        fromHex('#fce94f'),
        fromHex('#729fcf'),
        fromHex('#ad7fa8'),
        fromHex('#34e2e2'),
        fromHex('#eeeeec')
    ];
    var v = [0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff];
    for (var i = 0; i < 216; i++) {
        var r = v[(i / 36) % 6 | 0];
        var g = v[(i / 6) % 6 | 0];
        var b = v[i % 6];
        colors.push({
            css: "#" + toPaddedHex(r) + toPaddedHex(g) + toPaddedHex(b),
            rgba: ((r << 24) | (g << 16) | (b << 8) | 0xFF) >>> 0
        });
    }
    for (var i = 0; i < 24; i++) {
        var c = 8 + i * 10;
        var ch = toPaddedHex(c);
        colors.push({
            css: "#" + ch + ch + ch,
            rgba: ((c << 24) | (c << 16) | (c << 8) | 0xFF) >>> 0
        });
    }
    return colors;
})();
function fromHex(css) {
    return {
        css: css,
        rgba: parseInt(css.slice(1), 16) << 8 | 0xFF
    };
}
function toPaddedHex(c) {
    var s = c.toString(16);
    return s.length < 2 ? '0' + s : s;
}
var ColorManager = (function () {
    function ColorManager(document, allowTransparency) {
        this.allowTransparency = allowTransparency;
        var canvas = document.createElement('canvas');
        canvas.width = 1;
        canvas.height = 1;
        this._ctx = canvas.getContext('2d');
        this._ctx.globalCompositeOperation = 'copy';
        this._litmusColor = this._ctx.createLinearGradient(0, 0, 1, 1);
        this.colors = {
            foreground: DEFAULT_FOREGROUND,
            background: DEFAULT_BACKGROUND,
            cursor: DEFAULT_CURSOR,
            cursorAccent: DEFAULT_CURSOR_ACCENT,
            selection: DEFAULT_SELECTION,
            ansi: exports.DEFAULT_ANSI_COLORS.slice()
        };
    }
    ColorManager.prototype.setTheme = function (theme) {
        this.colors.foreground = this._parseColor(theme.foreground, DEFAULT_FOREGROUND);
        this.colors.background = this._parseColor(theme.background, DEFAULT_BACKGROUND);
        this.colors.cursor = this._parseColor(theme.cursor, DEFAULT_CURSOR, true);
        this.colors.cursorAccent = this._parseColor(theme.cursorAccent, DEFAULT_CURSOR_ACCENT, true);
        this.colors.selection = this._parseColor(theme.selection, DEFAULT_SELECTION, true);
        this.colors.ansi[0] = this._parseColor(theme.black, exports.DEFAULT_ANSI_COLORS[0]);
        this.colors.ansi[1] = this._parseColor(theme.red, exports.DEFAULT_ANSI_COLORS[1]);
        this.colors.ansi[2] = this._parseColor(theme.green, exports.DEFAULT_ANSI_COLORS[2]);
        this.colors.ansi[3] = this._parseColor(theme.yellow, exports.DEFAULT_ANSI_COLORS[3]);
        this.colors.ansi[4] = this._parseColor(theme.blue, exports.DEFAULT_ANSI_COLORS[4]);
        this.colors.ansi[5] = this._parseColor(theme.magenta, exports.DEFAULT_ANSI_COLORS[5]);
        this.colors.ansi[6] = this._parseColor(theme.cyan, exports.DEFAULT_ANSI_COLORS[6]);
        this.colors.ansi[7] = this._parseColor(theme.white, exports.DEFAULT_ANSI_COLORS[7]);
        this.colors.ansi[8] = this._parseColor(theme.brightBlack, exports.DEFAULT_ANSI_COLORS[8]);
        this.colors.ansi[9] = this._parseColor(theme.brightRed, exports.DEFAULT_ANSI_COLORS[9]);
        this.colors.ansi[10] = this._parseColor(theme.brightGreen, exports.DEFAULT_ANSI_COLORS[10]);
        this.colors.ansi[11] = this._parseColor(theme.brightYellow, exports.DEFAULT_ANSI_COLORS[11]);
        this.colors.ansi[12] = this._parseColor(theme.brightBlue, exports.DEFAULT_ANSI_COLORS[12]);
        this.colors.ansi[13] = this._parseColor(theme.brightMagenta, exports.DEFAULT_ANSI_COLORS[13]);
        this.colors.ansi[14] = this._parseColor(theme.brightCyan, exports.DEFAULT_ANSI_COLORS[14]);
        this.colors.ansi[15] = this._parseColor(theme.brightWhite, exports.DEFAULT_ANSI_COLORS[15]);
    };
    ColorManager.prototype._parseColor = function (css, fallback, allowTransparency) {
        if (allowTransparency === void 0) { allowTransparency = this.allowTransparency; }
        if (!css) {
            return fallback;
        }
        this._ctx.fillStyle = this._litmusColor;
        this._ctx.fillStyle = css;
        if (typeof this._ctx.fillStyle !== 'string') {
            console.warn("Color: " + css + " is invalid using fallback " + fallback.css);
            return fallback;
        }
        this._ctx.fillRect(0, 0, 1, 1);
        var data = this._ctx.getImageData(0, 0, 1, 1).data;
        if (!allowTransparency && data[3] !== 0xFF) {
            console.warn("Color: " + css + " is using transparency, but allowTransparency is false. " +
                ("Using fallback " + fallback.css + "."));
            return fallback;
        }
        return {
            css: css,
            rgba: (data[0] << 24 | data[1] << 16 | data[2] << 8 | data[3]) >>> 0
        };
    };
    return ColorManager;
}());
exports.ColorManager = ColorManager;

},{}],29:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("../Buffer");
var BaseRenderLayer_1 = require("./BaseRenderLayer");
var BLINK_INTERVAL = 600;
var CursorRenderLayer = (function (_super) {
    __extends(CursorRenderLayer, _super);
    function CursorRenderLayer(container, zIndex, colors) {
        var _this = _super.call(this, container, 'cursor', zIndex, true, colors) || this;
        _this._state = {
            x: null,
            y: null,
            isFocused: null,
            style: null,
            width: null
        };
        _this._cursorRenderers = {
            'bar': _this._renderBarCursor.bind(_this),
            'block': _this._renderBlockCursor.bind(_this),
            'underline': _this._renderUnderlineCursor.bind(_this)
        };
        return _this;
    }
    CursorRenderLayer.prototype.resize = function (terminal, dim) {
        _super.prototype.resize.call(this, terminal, dim);
        this._state = {
            x: null,
            y: null,
            isFocused: null,
            style: null,
            width: null
        };
    };
    CursorRenderLayer.prototype.reset = function (terminal) {
        this._clearCursor();
        if (this._cursorBlinkStateManager) {
            this._cursorBlinkStateManager.dispose();
            this._cursorBlinkStateManager = null;
            this.onOptionsChanged(terminal);
        }
    };
    CursorRenderLayer.prototype.onBlur = function (terminal) {
        if (this._cursorBlinkStateManager) {
            this._cursorBlinkStateManager.pause();
        }
        terminal.refresh(terminal.buffer.y, terminal.buffer.y);
    };
    CursorRenderLayer.prototype.onFocus = function (terminal) {
        if (this._cursorBlinkStateManager) {
            this._cursorBlinkStateManager.resume(terminal);
        }
        else {
            terminal.refresh(terminal.buffer.y, terminal.buffer.y);
        }
    };
    CursorRenderLayer.prototype.onOptionsChanged = function (terminal) {
        var _this = this;
        if (terminal.options.cursorBlink) {
            if (!this._cursorBlinkStateManager) {
                this._cursorBlinkStateManager = new CursorBlinkStateManager(terminal, function () {
                    _this._render(terminal, true);
                });
            }
        }
        else {
            if (this._cursorBlinkStateManager) {
                this._cursorBlinkStateManager.dispose();
                this._cursorBlinkStateManager = null;
            }
            terminal.refresh(terminal.buffer.y, terminal.buffer.y);
        }
    };
    CursorRenderLayer.prototype.onCursorMove = function (terminal) {
        if (this._cursorBlinkStateManager) {
            this._cursorBlinkStateManager.restartBlinkAnimation(terminal);
        }
    };
    CursorRenderLayer.prototype.onGridChanged = function (terminal, startRow, endRow) {
        if (!this._cursorBlinkStateManager || this._cursorBlinkStateManager.isPaused) {
            this._render(terminal, false);
        }
        else {
            this._cursorBlinkStateManager.restartBlinkAnimation(terminal);
        }
    };
    CursorRenderLayer.prototype._render = function (terminal, triggeredByAnimationFrame) {
        if (!terminal.cursorState || terminal.cursorHidden) {
            this._clearCursor();
            return;
        }
        var cursorY = terminal.buffer.ybase + terminal.buffer.y;
        var viewportRelativeCursorY = cursorY - terminal.buffer.ydisp;
        if (viewportRelativeCursorY < 0 || viewportRelativeCursorY >= terminal.rows) {
            this._clearCursor();
            return;
        }
        var charData = terminal.buffer.lines.get(cursorY).get(terminal.buffer.x);
        if (!charData) {
            return;
        }
        if (!terminal.isFocused) {
            this._clearCursor();
            this._ctx.save();
            this._ctx.fillStyle = this._colors.cursor.css;
            this._renderBlurCursor(terminal, terminal.buffer.x, viewportRelativeCursorY, charData);
            this._ctx.restore();
            this._state.x = terminal.buffer.x;
            this._state.y = viewportRelativeCursorY;
            this._state.isFocused = false;
            this._state.style = terminal.options.cursorStyle;
            this._state.width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
            return;
        }
        if (this._cursorBlinkStateManager && !this._cursorBlinkStateManager.isCursorVisible) {
            this._clearCursor();
            return;
        }
        if (this._state) {
            if (this._state.x === terminal.buffer.x &&
                this._state.y === viewportRelativeCursorY &&
                this._state.isFocused === terminal.isFocused &&
                this._state.style === terminal.options.cursorStyle &&
                this._state.width === charData[Buffer_1.CHAR_DATA_WIDTH_INDEX]) {
                return;
            }
            this._clearCursor();
        }
        this._ctx.save();
        this._cursorRenderers[terminal.options.cursorStyle || 'block'](terminal, terminal.buffer.x, viewportRelativeCursorY, charData);
        this._ctx.restore();
        this._state.x = terminal.buffer.x;
        this._state.y = viewportRelativeCursorY;
        this._state.isFocused = false;
        this._state.style = terminal.options.cursorStyle;
        this._state.width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
    };
    CursorRenderLayer.prototype._clearCursor = function () {
        if (this._state) {
            this.clearCells(this._state.x, this._state.y, this._state.width, 1);
            this._state = {
                x: null,
                y: null,
                isFocused: null,
                style: null,
                width: null
            };
        }
    };
    CursorRenderLayer.prototype._renderBarCursor = function (terminal, x, y, charData) {
        this._ctx.save();
        this._ctx.fillStyle = this._colors.cursor.css;
        this.fillLeftLineAtCell(x, y);
        this._ctx.restore();
    };
    CursorRenderLayer.prototype._renderBlockCursor = function (terminal, x, y, charData) {
        this._ctx.save();
        this._ctx.fillStyle = this._colors.cursor.css;
        this.fillCells(x, y, charData[Buffer_1.CHAR_DATA_WIDTH_INDEX], 1);
        this._ctx.fillStyle = this._colors.cursorAccent.css;
        this.fillCharTrueColor(terminal, charData, x, y);
        this._ctx.restore();
    };
    CursorRenderLayer.prototype._renderUnderlineCursor = function (terminal, x, y, charData) {
        this._ctx.save();
        this._ctx.fillStyle = this._colors.cursor.css;
        this.fillBottomLineAtCells(x, y);
        this._ctx.restore();
    };
    CursorRenderLayer.prototype._renderBlurCursor = function (terminal, x, y, charData) {
        this._ctx.save();
        this._ctx.strokeStyle = this._colors.cursor.css;
        this.strokeRectAtCell(x, y, charData[Buffer_1.CHAR_DATA_WIDTH_INDEX], 1);
        this._ctx.restore();
    };
    return CursorRenderLayer;
}(BaseRenderLayer_1.BaseRenderLayer));
exports.CursorRenderLayer = CursorRenderLayer;
var CursorBlinkStateManager = (function () {
    function CursorBlinkStateManager(terminal, _renderCallback) {
        this._renderCallback = _renderCallback;
        this.isCursorVisible = true;
        if (terminal.isFocused) {
            this._restartInterval();
        }
    }
    Object.defineProperty(CursorBlinkStateManager.prototype, "isPaused", {
        get: function () { return !(this._blinkStartTimeout || this._blinkInterval); },
        enumerable: true,
        configurable: true
    });
    CursorBlinkStateManager.prototype.dispose = function () {
        if (this._blinkInterval) {
            window.clearInterval(this._blinkInterval);
            this._blinkInterval = null;
        }
        if (this._blinkStartTimeout) {
            window.clearTimeout(this._blinkStartTimeout);
            this._blinkStartTimeout = null;
        }
        if (this._animationFrame) {
            window.cancelAnimationFrame(this._animationFrame);
            this._animationFrame = null;
        }
    };
    CursorBlinkStateManager.prototype.restartBlinkAnimation = function (terminal) {
        var _this = this;
        if (this.isPaused) {
            return;
        }
        this._animationTimeRestarted = Date.now();
        this.isCursorVisible = true;
        if (!this._animationFrame) {
            this._animationFrame = window.requestAnimationFrame(function () {
                _this._renderCallback();
                _this._animationFrame = null;
            });
        }
    };
    CursorBlinkStateManager.prototype._restartInterval = function (timeToStart) {
        var _this = this;
        if (timeToStart === void 0) { timeToStart = BLINK_INTERVAL; }
        if (this._blinkInterval) {
            window.clearInterval(this._blinkInterval);
        }
        this._blinkStartTimeout = setTimeout(function () {
            if (_this._animationTimeRestarted) {
                var time = BLINK_INTERVAL - (Date.now() - _this._animationTimeRestarted);
                _this._animationTimeRestarted = null;
                if (time > 0) {
                    _this._restartInterval(time);
                    return;
                }
            }
            _this.isCursorVisible = false;
            _this._animationFrame = window.requestAnimationFrame(function () {
                _this._renderCallback();
                _this._animationFrame = null;
            });
            _this._blinkInterval = setInterval(function () {
                if (_this._animationTimeRestarted) {
                    var time = BLINK_INTERVAL - (Date.now() - _this._animationTimeRestarted);
                    _this._animationTimeRestarted = null;
                    _this._restartInterval(time);
                    return;
                }
                _this.isCursorVisible = !_this.isCursorVisible;
                _this._animationFrame = window.requestAnimationFrame(function () {
                    _this._renderCallback();
                    _this._animationFrame = null;
                });
            }, BLINK_INTERVAL);
        }, timeToStart);
    };
    CursorBlinkStateManager.prototype.pause = function () {
        this.isCursorVisible = true;
        if (this._blinkInterval) {
            window.clearInterval(this._blinkInterval);
            this._blinkInterval = null;
        }
        if (this._blinkStartTimeout) {
            window.clearTimeout(this._blinkStartTimeout);
            this._blinkStartTimeout = null;
        }
        if (this._animationFrame) {
            window.cancelAnimationFrame(this._animationFrame);
            this._animationFrame = null;
        }
    };
    CursorBlinkStateManager.prototype.resume = function (terminal) {
        this._animationTimeRestarted = null;
        this._restartInterval();
        this.restartBlinkAnimation(terminal);
    };
    return CursorBlinkStateManager;
}());

},{"../Buffer":2,"./BaseRenderLayer":26}],30:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var GridCache = (function () {
    function GridCache() {
        this.cache = [];
    }
    GridCache.prototype.resize = function (width, height) {
        for (var x = 0; x < width; x++) {
            if (this.cache.length <= x) {
                this.cache.push([]);
            }
            for (var y = this.cache[x].length; y < height; y++) {
                this.cache[x].push(null);
            }
            this.cache[x].length = height;
        }
        this.cache.length = width;
    };
    GridCache.prototype.clear = function () {
        for (var x = 0; x < this.cache.length; x++) {
            for (var y = 0; y < this.cache[x].length; y++) {
                this.cache[x][y] = null;
            }
        }
    };
    return GridCache;
}());
exports.GridCache = GridCache;

},{}],31:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var BaseRenderLayer_1 = require("./BaseRenderLayer");
var Types_1 = require("./atlas/Types");
var CharAtlasUtils_1 = require("./atlas/CharAtlasUtils");
var LinkRenderLayer = (function (_super) {
    __extends(LinkRenderLayer, _super);
    function LinkRenderLayer(container, zIndex, colors, terminal) {
        var _this = _super.call(this, container, 'link', zIndex, true, colors) || this;
        _this._state = null;
        terminal.linkifier.on("linkhover", function (e) { return _this._onLinkHover(e); });
        terminal.linkifier.on("linkleave", function (e) { return _this._onLinkLeave(e); });
        return _this;
    }
    LinkRenderLayer.prototype.resize = function (terminal, dim) {
        _super.prototype.resize.call(this, terminal, dim);
        this._state = null;
    };
    LinkRenderLayer.prototype.reset = function (terminal) {
        this._clearCurrentLink();
    };
    LinkRenderLayer.prototype._clearCurrentLink = function () {
        if (this._state) {
            this.clearCells(this._state.x1, this._state.y1, this._state.cols - this._state.x1, 1);
            var middleRowCount = this._state.y2 - this._state.y1 - 1;
            if (middleRowCount > 0) {
                this.clearCells(0, this._state.y1 + 1, this._state.cols, middleRowCount);
            }
            this.clearCells(0, this._state.y2, this._state.x2, 1);
            this._state = null;
        }
    };
    LinkRenderLayer.prototype._onLinkHover = function (e) {
        if (e.fg === Types_1.INVERTED_DEFAULT_COLOR) {
            this._ctx.fillStyle = this._colors.background.css;
        }
        else if (CharAtlasUtils_1.is256Color(e.fg)) {
            this._ctx.fillStyle = this._colors.ansi[e.fg].css;
        }
        else {
            this._ctx.fillStyle = this._colors.foreground.css;
        }
        if (e.y1 === e.y2) {
            this.fillBottomLineAtCells(e.x1, e.y1, e.x2 - e.x1);
        }
        else {
            this.fillBottomLineAtCells(e.x1, e.y1, e.cols - e.x1);
            for (var y = e.y1 + 1; y < e.y2; y++) {
                this.fillBottomLineAtCells(0, y, e.cols);
            }
            this.fillBottomLineAtCells(0, e.y2, e.x2);
        }
        this._state = e;
    };
    LinkRenderLayer.prototype._onLinkLeave = function (e) {
        this._clearCurrentLink();
    };
    return LinkRenderLayer;
}(BaseRenderLayer_1.BaseRenderLayer));
exports.LinkRenderLayer = LinkRenderLayer;

},{"./BaseRenderLayer":26,"./atlas/CharAtlasUtils":38,"./atlas/Types":43}],32:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var TextRenderLayer_1 = require("./TextRenderLayer");
var SelectionRenderLayer_1 = require("./SelectionRenderLayer");
var CursorRenderLayer_1 = require("./CursorRenderLayer");
var ColorManager_1 = require("./ColorManager");
var LinkRenderLayer_1 = require("./LinkRenderLayer");
var EventEmitter_1 = require("../common/EventEmitter");
var RenderDebouncer_1 = require("../ui/RenderDebouncer");
var ScreenDprMonitor_1 = require("../ui/ScreenDprMonitor");
var CharacterJoinerRegistry_1 = require("../renderer/CharacterJoinerRegistry");
var Renderer = (function (_super) {
    __extends(Renderer, _super);
    function Renderer(_terminal, theme) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._isPaused = false;
        _this._needsFullRefresh = false;
        var allowTransparency = _this._terminal.options.allowTransparency;
        _this.colorManager = new ColorManager_1.ColorManager(document, allowTransparency);
        _this._characterJoinerRegistry = new CharacterJoinerRegistry_1.CharacterJoinerRegistry(_terminal);
        if (theme) {
            _this.colorManager.setTheme(theme);
        }
        _this._renderLayers = [
            new TextRenderLayer_1.TextRenderLayer(_this._terminal.screenElement, 0, _this.colorManager.colors, _this._characterJoinerRegistry, allowTransparency),
            new SelectionRenderLayer_1.SelectionRenderLayer(_this._terminal.screenElement, 1, _this.colorManager.colors),
            new LinkRenderLayer_1.LinkRenderLayer(_this._terminal.screenElement, 2, _this.colorManager.colors, _this._terminal),
            new CursorRenderLayer_1.CursorRenderLayer(_this._terminal.screenElement, 3, _this.colorManager.colors)
        ];
        _this.dimensions = {
            scaledCharWidth: null,
            scaledCharHeight: null,
            scaledCellWidth: null,
            scaledCellHeight: null,
            scaledCharLeft: null,
            scaledCharTop: null,
            scaledCanvasWidth: null,
            scaledCanvasHeight: null,
            canvasWidth: null,
            canvasHeight: null,
            actualCellWidth: null,
            actualCellHeight: null
        };
        _this._devicePixelRatio = window.devicePixelRatio;
        _this._updateDimensions();
        _this.onOptionsChanged();
        _this._renderDebouncer = new RenderDebouncer_1.RenderDebouncer(_this._terminal, _this._renderRows.bind(_this));
        _this._screenDprMonitor = new ScreenDprMonitor_1.ScreenDprMonitor();
        _this._screenDprMonitor.setListener(function () { return _this.onWindowResize(window.devicePixelRatio); });
        _this.register(_this._screenDprMonitor);
        if ('IntersectionObserver' in window) {
            var observer_1 = new IntersectionObserver(function (e) { return _this.onIntersectionChange(e[0]); }, { threshold: 0 });
            observer_1.observe(_this._terminal.element);
            _this.register({ dispose: function () { return observer_1.disconnect(); } });
        }
        return _this;
    }
    Renderer.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._renderLayers.forEach(function (l) { return l.dispose(); });
    };
    Renderer.prototype.onIntersectionChange = function (entry) {
        this._isPaused = entry.intersectionRatio === 0;
        if (!this._isPaused && this._needsFullRefresh) {
            this._terminal.refresh(0, this._terminal.rows - 1);
        }
    };
    Renderer.prototype.onWindowResize = function (devicePixelRatio) {
        if (this._devicePixelRatio !== devicePixelRatio) {
            this._devicePixelRatio = devicePixelRatio;
            this.onResize(this._terminal.cols, this._terminal.rows);
        }
    };
    Renderer.prototype.setTheme = function (theme) {
        var _this = this;
        this.colorManager.setTheme(theme);
        this._renderLayers.forEach(function (l) {
            l.onThemeChanged(_this._terminal, _this.colorManager.colors);
            l.reset(_this._terminal);
        });
        if (this._isPaused) {
            this._needsFullRefresh = true;
        }
        else {
            this._terminal.refresh(0, this._terminal.rows - 1);
        }
        return this.colorManager.colors;
    };
    Renderer.prototype.onResize = function (cols, rows) {
        var _this = this;
        this._updateDimensions();
        this._renderLayers.forEach(function (l) { return l.resize(_this._terminal, _this.dimensions); });
        if (this._isPaused) {
            this._needsFullRefresh = true;
        }
        else {
            this._terminal.refresh(0, this._terminal.rows - 1);
        }
        this._terminal.screenElement.style.width = this.dimensions.canvasWidth + "px";
        this._terminal.screenElement.style.height = this.dimensions.canvasHeight + "px";
        this.emit('resize', {
            width: this.dimensions.canvasWidth,
            height: this.dimensions.canvasHeight
        });
    };
    Renderer.prototype.onCharSizeChanged = function () {
        this.onResize(this._terminal.cols, this._terminal.rows);
    };
    Renderer.prototype.onBlur = function () {
        var _this = this;
        this._runOperation(function (l) { return l.onBlur(_this._terminal); });
    };
    Renderer.prototype.onFocus = function () {
        var _this = this;
        this._runOperation(function (l) { return l.onFocus(_this._terminal); });
    };
    Renderer.prototype.onSelectionChanged = function (start, end, columnSelectMode) {
        var _this = this;
        if (columnSelectMode === void 0) { columnSelectMode = false; }
        this._runOperation(function (l) { return l.onSelectionChanged(_this._terminal, start, end, columnSelectMode); });
    };
    Renderer.prototype.onCursorMove = function () {
        var _this = this;
        this._runOperation(function (l) { return l.onCursorMove(_this._terminal); });
    };
    Renderer.prototype.onOptionsChanged = function () {
        var _this = this;
        this.colorManager.allowTransparency = this._terminal.options.allowTransparency;
        this._runOperation(function (l) { return l.onOptionsChanged(_this._terminal); });
    };
    Renderer.prototype.clear = function () {
        var _this = this;
        this._runOperation(function (l) { return l.reset(_this._terminal); });
    };
    Renderer.prototype._runOperation = function (operation) {
        if (this._isPaused) {
            this._needsFullRefresh = true;
        }
        else {
            this._renderLayers.forEach(function (l) { return operation(l); });
        }
    };
    Renderer.prototype.refreshRows = function (start, end) {
        if (this._isPaused) {
            this._needsFullRefresh = true;
            return;
        }
        this._renderDebouncer.refresh(start, end);
    };
    Renderer.prototype._renderRows = function (start, end) {
        var _this = this;
        this._renderLayers.forEach(function (l) { return l.onGridChanged(_this._terminal, start, end); });
        this._terminal.emit('refresh', { start: start, end: end });
    };
    Renderer.prototype._updateDimensions = function () {
        if (!this._terminal.charMeasure.width || !this._terminal.charMeasure.height) {
            return;
        }
        this.dimensions.scaledCharWidth = Math.floor(this._terminal.charMeasure.width * window.devicePixelRatio);
        this.dimensions.scaledCharHeight = Math.ceil(this._terminal.charMeasure.height * window.devicePixelRatio);
        this.dimensions.scaledCellHeight = Math.floor(this.dimensions.scaledCharHeight * this._terminal.options.lineHeight);
        this.dimensions.scaledCharTop = this._terminal.options.lineHeight === 1 ? 0 : Math.round((this.dimensions.scaledCellHeight - this.dimensions.scaledCharHeight) / 2);
        this.dimensions.scaledCellWidth = this.dimensions.scaledCharWidth + Math.round(this._terminal.options.letterSpacing);
        this.dimensions.scaledCharLeft = Math.floor(this._terminal.options.letterSpacing / 2);
        this.dimensions.scaledCanvasHeight = this._terminal.rows * this.dimensions.scaledCellHeight;
        this.dimensions.scaledCanvasWidth = this._terminal.cols * this.dimensions.scaledCellWidth;
        this.dimensions.canvasHeight = Math.round(this.dimensions.scaledCanvasHeight / window.devicePixelRatio);
        this.dimensions.canvasWidth = Math.round(this.dimensions.scaledCanvasWidth / window.devicePixelRatio);
        this.dimensions.actualCellHeight = this.dimensions.canvasHeight / this._terminal.rows;
        this.dimensions.actualCellWidth = this.dimensions.canvasWidth / this._terminal.cols;
    };
    Renderer.prototype.registerCharacterJoiner = function (handler) {
        return this._characterJoinerRegistry.registerCharacterJoiner(handler);
    };
    Renderer.prototype.deregisterCharacterJoiner = function (joinerId) {
        return this._characterJoinerRegistry.deregisterCharacterJoiner(joinerId);
    };
    return Renderer;
}(EventEmitter_1.EventEmitter));
exports.Renderer = Renderer;

},{"../common/EventEmitter":17,"../renderer/CharacterJoinerRegistry":27,"../ui/RenderDebouncer":50,"../ui/ScreenDprMonitor":51,"./ColorManager":28,"./CursorRenderLayer":29,"./LinkRenderLayer":31,"./SelectionRenderLayer":33,"./TextRenderLayer":34}],33:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var BaseRenderLayer_1 = require("./BaseRenderLayer");
var SelectionRenderLayer = (function (_super) {
    __extends(SelectionRenderLayer, _super);
    function SelectionRenderLayer(container, zIndex, colors) {
        var _this = _super.call(this, container, 'selection', zIndex, true, colors) || this;
        _this._clearState();
        return _this;
    }
    SelectionRenderLayer.prototype._clearState = function () {
        this._state = {
            start: null,
            end: null,
            columnSelectMode: null,
            ydisp: null
        };
    };
    SelectionRenderLayer.prototype.resize = function (terminal, dim) {
        _super.prototype.resize.call(this, terminal, dim);
        this._clearState();
    };
    SelectionRenderLayer.prototype.reset = function (terminal) {
        if (this._state.start && this._state.end) {
            this._clearState();
            this.clearAll();
        }
    };
    SelectionRenderLayer.prototype.onSelectionChanged = function (terminal, start, end, columnSelectMode) {
        if (!this._didStateChange(start, end, columnSelectMode, terminal.buffer.ydisp)) {
            return;
        }
        this.clearAll();
        if (!start || !end) {
            return;
        }
        var viewportStartRow = start[1] - terminal.buffer.ydisp;
        var viewportEndRow = end[1] - terminal.buffer.ydisp;
        var viewportCappedStartRow = Math.max(viewportStartRow, 0);
        var viewportCappedEndRow = Math.min(viewportEndRow, terminal.rows - 1);
        if (viewportCappedStartRow >= terminal.rows || viewportCappedEndRow < 0) {
            return;
        }
        this._ctx.fillStyle = this._colors.selection.css;
        if (columnSelectMode) {
            var startCol = start[0];
            var width = end[0] - startCol;
            var height = viewportCappedEndRow - viewportCappedStartRow + 1;
            this.fillCells(startCol, viewportCappedStartRow, width, height);
        }
        else {
            var startCol = viewportStartRow === viewportCappedStartRow ? start[0] : 0;
            var startRowEndCol = viewportCappedStartRow === viewportCappedEndRow ? end[0] : terminal.cols;
            this.fillCells(startCol, viewportCappedStartRow, startRowEndCol - startCol, 1);
            var middleRowsCount = Math.max(viewportCappedEndRow - viewportCappedStartRow - 1, 0);
            this.fillCells(0, viewportCappedStartRow + 1, terminal.cols, middleRowsCount);
            if (viewportCappedStartRow !== viewportCappedEndRow) {
                var endCol = viewportEndRow === viewportCappedEndRow ? end[0] : terminal.cols;
                this.fillCells(0, viewportCappedEndRow, endCol, 1);
            }
        }
        this._state.start = [start[0], start[1]];
        this._state.end = [end[0], end[1]];
        this._state.columnSelectMode = columnSelectMode;
        this._state.ydisp = terminal.buffer.ydisp;
    };
    SelectionRenderLayer.prototype._didStateChange = function (start, end, columnSelectMode, ydisp) {
        return !this._areCoordinatesEqual(start, this._state.start) ||
            !this._areCoordinatesEqual(end, this._state.end) ||
            columnSelectMode !== this._state.columnSelectMode ||
            ydisp !== this._state.ydisp;
    };
    SelectionRenderLayer.prototype._areCoordinatesEqual = function (coord1, coord2) {
        if (!coord1 || !coord2) {
            return false;
        }
        return coord1[0] === coord2[0] && coord1[1] === coord2[1];
    };
    return SelectionRenderLayer;
}(BaseRenderLayer_1.BaseRenderLayer));
exports.SelectionRenderLayer = SelectionRenderLayer;

},{"./BaseRenderLayer":26}],34:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("../Buffer");
var Types_1 = require("./atlas/Types");
var GridCache_1 = require("./GridCache");
var BaseRenderLayer_1 = require("./BaseRenderLayer");
var CharAtlasUtils_1 = require("./atlas/CharAtlasUtils");
var TextRenderLayer = (function (_super) {
    __extends(TextRenderLayer, _super);
    function TextRenderLayer(container, zIndex, colors, characterJoinerRegistry, alpha) {
        var _this = _super.call(this, container, 'text', zIndex, alpha, colors) || this;
        _this._characterOverlapCache = {};
        _this._state = new GridCache_1.GridCache();
        _this._characterJoinerRegistry = characterJoinerRegistry;
        return _this;
    }
    TextRenderLayer.prototype.resize = function (terminal, dim) {
        _super.prototype.resize.call(this, terminal, dim);
        var terminalFont = this._getFont(terminal, false, false);
        if (this._characterWidth !== dim.scaledCharWidth || this._characterFont !== terminalFont) {
            this._characterWidth = dim.scaledCharWidth;
            this._characterFont = terminalFont;
            this._characterOverlapCache = {};
        }
        this._state.clear();
        this._state.resize(terminal.cols, terminal.rows);
    };
    TextRenderLayer.prototype.reset = function (terminal) {
        this._state.clear();
        this.clearAll();
    };
    TextRenderLayer.prototype._forEachCell = function (terminal, firstRow, lastRow, joinerRegistry, callback) {
        for (var y = firstRow; y <= lastRow; y++) {
            var row = y + terminal.buffer.ydisp;
            var line = terminal.buffer.lines.get(row);
            var joinedRanges = joinerRegistry ? joinerRegistry.getJoinedCharacters(row) : [];
            for (var x = 0; x < terminal.cols; x++) {
                var charData = line.get(x);
                var code = charData[Buffer_1.CHAR_DATA_CODE_INDEX];
                var chars = charData[Buffer_1.CHAR_DATA_CHAR_INDEX];
                var attr = charData[Buffer_1.CHAR_DATA_ATTR_INDEX];
                var width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
                var isJoined = false;
                var lastCharX = x;
                if (width === 0) {
                    continue;
                }
                if (joinedRanges.length > 0 && x === joinedRanges[0][0]) {
                    isJoined = true;
                    var range = joinedRanges.shift();
                    chars = terminal.buffer.translateBufferLineToString(row, true, range[0], range[1]);
                    width = range[1] - range[0];
                    code = Infinity;
                    lastCharX = range[1] - 1;
                }
                if (!isJoined && this._isOverlapping(charData)) {
                    if (lastCharX < line.length - 1 && line.get(lastCharX + 1)[Buffer_1.CHAR_DATA_CODE_INDEX] === Buffer_1.NULL_CELL_CODE) {
                        width = 2;
                    }
                }
                var flags = attr >> 18;
                var bg = attr & 0x1ff;
                var fg = (attr >> 9) & 0x1ff;
                if (flags & 8) {
                    var temp = bg;
                    bg = fg;
                    fg = temp;
                    if (fg === Types_1.DEFAULT_COLOR) {
                        fg = Types_1.INVERTED_DEFAULT_COLOR;
                    }
                    if (bg === Types_1.DEFAULT_COLOR) {
                        bg = Types_1.INVERTED_DEFAULT_COLOR;
                    }
                }
                callback(code, chars, width, x, y, fg, bg, flags);
                x = lastCharX;
            }
        }
    };
    TextRenderLayer.prototype._drawBackground = function (terminal, firstRow, lastRow) {
        var _this = this;
        var ctx = this._ctx;
        var cols = terminal.cols;
        var startX = 0;
        var startY = 0;
        var prevFillStyle = null;
        ctx.save();
        this._forEachCell(terminal, firstRow, lastRow, null, function (code, chars, width, x, y, fg, bg, flags) {
            var nextFillStyle = null;
            if (bg === Types_1.INVERTED_DEFAULT_COLOR) {
                nextFillStyle = _this._colors.foreground.css;
            }
            else if (CharAtlasUtils_1.is256Color(bg)) {
                nextFillStyle = _this._colors.ansi[bg].css;
            }
            if (prevFillStyle === null) {
                startX = x;
                startY = y;
            }
            if (y !== startY) {
                ctx.fillStyle = prevFillStyle;
                _this.fillCells(startX, startY, cols - startX, 1);
                startX = x;
                startY = y;
            }
            else if (prevFillStyle !== nextFillStyle) {
                ctx.fillStyle = prevFillStyle;
                _this.fillCells(startX, startY, x - startX, 1);
                startX = x;
                startY = y;
            }
            prevFillStyle = nextFillStyle;
        });
        if (prevFillStyle !== null) {
            ctx.fillStyle = prevFillStyle;
            this.fillCells(startX, startY, cols - startX, 1);
        }
        ctx.restore();
    };
    TextRenderLayer.prototype._drawForeground = function (terminal, firstRow, lastRow) {
        var _this = this;
        this._forEachCell(terminal, firstRow, lastRow, this._characterJoinerRegistry, function (code, chars, width, x, y, fg, bg, flags) {
            if (flags & 16) {
                return;
            }
            if (flags & 2) {
                _this._ctx.save();
                if (fg === Types_1.INVERTED_DEFAULT_COLOR) {
                    _this._ctx.fillStyle = _this._colors.background.css;
                }
                else if (CharAtlasUtils_1.is256Color(fg)) {
                    _this._ctx.fillStyle = _this._colors.ansi[fg].css;
                }
                else {
                    _this._ctx.fillStyle = _this._colors.foreground.css;
                }
                _this.fillBottomLineAtCells(x, y, width);
                _this._ctx.restore();
            }
            _this.drawChars(terminal, chars, code, width, x, y, fg, bg, !!(flags & 1), !!(flags & 32), !!(flags & 64));
        });
    };
    TextRenderLayer.prototype.onGridChanged = function (terminal, firstRow, lastRow) {
        if (this._state.cache.length === 0) {
            return;
        }
        if (this._charAtlas) {
            this._charAtlas.beginFrame();
        }
        this.clearCells(0, firstRow, terminal.cols, lastRow - firstRow + 1);
        this._drawBackground(terminal, firstRow, lastRow);
        this._drawForeground(terminal, firstRow, lastRow);
    };
    TextRenderLayer.prototype.onOptionsChanged = function (terminal) {
        this.setTransparency(terminal, terminal.options.allowTransparency);
    };
    TextRenderLayer.prototype._isOverlapping = function (charData) {
        if (charData[Buffer_1.CHAR_DATA_WIDTH_INDEX] !== 1) {
            return false;
        }
        var code = charData[Buffer_1.CHAR_DATA_CODE_INDEX];
        if (code < 256) {
            return false;
        }
        var char = charData[Buffer_1.CHAR_DATA_CHAR_INDEX];
        if (this._characterOverlapCache.hasOwnProperty(char)) {
            return this._characterOverlapCache[char];
        }
        this._ctx.save();
        this._ctx.font = this._characterFont;
        var overlaps = Math.floor(this._ctx.measureText(char).width) > this._characterWidth;
        this._ctx.restore();
        this._characterOverlapCache[char] = overlaps;
        return overlaps;
    };
    return TextRenderLayer;
}(BaseRenderLayer_1.BaseRenderLayer));
exports.TextRenderLayer = TextRenderLayer;

},{"../Buffer":2,"./BaseRenderLayer":26,"./GridCache":30,"./atlas/CharAtlasUtils":38,"./atlas/Types":43}],35:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var BaseCharAtlas = (function () {
    function BaseCharAtlas() {
        this._didWarmUp = false;
    }
    BaseCharAtlas.prototype.dispose = function () { };
    BaseCharAtlas.prototype.warmUp = function () {
        if (!this._didWarmUp) {
            this._doWarmUp();
            this._didWarmUp = true;
        }
    };
    BaseCharAtlas.prototype._doWarmUp = function () { };
    BaseCharAtlas.prototype.beginFrame = function () { };
    return BaseCharAtlas;
}());
exports.default = BaseCharAtlas;

},{}],36:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var CharAtlasUtils_1 = require("./CharAtlasUtils");
var DynamicCharAtlas_1 = require("./DynamicCharAtlas");
var NoneCharAtlas_1 = require("./NoneCharAtlas");
var StaticCharAtlas_1 = require("./StaticCharAtlas");
var charAtlasImplementations = {
    'none': NoneCharAtlas_1.default,
    'static': StaticCharAtlas_1.default,
    'dynamic': DynamicCharAtlas_1.default
};
var charAtlasCache = [];
function acquireCharAtlas(terminal, colors, scaledCharWidth, scaledCharHeight) {
    var newConfig = CharAtlasUtils_1.generateConfig(scaledCharWidth, scaledCharHeight, terminal, colors);
    for (var i = 0; i < charAtlasCache.length; i++) {
        var entry = charAtlasCache[i];
        var ownedByIndex = entry.ownedBy.indexOf(terminal);
        if (ownedByIndex >= 0) {
            if (CharAtlasUtils_1.configEquals(entry.config, newConfig)) {
                return entry.atlas;
            }
            if (entry.ownedBy.length === 1) {
                charAtlasCache.splice(i, 1);
            }
            else {
                entry.ownedBy.splice(ownedByIndex, 1);
            }
            break;
        }
    }
    for (var i = 0; i < charAtlasCache.length; i++) {
        var entry = charAtlasCache[i];
        if (CharAtlasUtils_1.configEquals(entry.config, newConfig)) {
            entry.ownedBy.push(terminal);
            return entry.atlas;
        }
    }
    var newEntry = {
        atlas: new charAtlasImplementations[terminal.options.experimentalCharAtlas](document, newConfig),
        config: newConfig,
        ownedBy: [terminal]
    };
    charAtlasCache.push(newEntry);
    return newEntry.atlas;
}
exports.acquireCharAtlas = acquireCharAtlas;
function removeTerminalFromCache(terminal) {
    for (var i = 0; i < charAtlasCache.length; i++) {
        var index = charAtlasCache[i].ownedBy.indexOf(terminal);
        if (index !== -1) {
            if (charAtlasCache[i].ownedBy.length === 1) {
                charAtlasCache.splice(i, 1);
            }
            else {
                charAtlasCache[i].ownedBy.splice(index, 1);
            }
            break;
        }
    }
}
exports.removeTerminalFromCache = removeTerminalFromCache;

},{"./CharAtlasUtils":38,"./DynamicCharAtlas":39,"./NoneCharAtlas":41,"./StaticCharAtlas":42}],37:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Platform_1 = require("../../core/Platform");
var Types_1 = require("./Types");
function generateStaticCharAtlasTexture(context, canvasFactory, config) {
    var cellWidth = config.scaledCharWidth + Types_1.CHAR_ATLAS_CELL_SPACING;
    var cellHeight = config.scaledCharHeight + Types_1.CHAR_ATLAS_CELL_SPACING;
    var canvas = canvasFactory(255 * cellWidth, (2 + 16 + 16) * cellHeight);
    var ctx = canvas.getContext('2d', { alpha: config.allowTransparency });
    ctx.fillStyle = config.colors.background.css;
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.fillStyle = config.colors.foreground.css;
    ctx.font = getFont(config.fontWeight, config);
    ctx.textBaseline = 'top';
    for (var i = 0; i < 256; i++) {
        ctx.save();
        ctx.beginPath();
        ctx.rect(i * cellWidth, 0, cellWidth, cellHeight);
        ctx.clip();
        ctx.fillText(String.fromCharCode(i), i * cellWidth, 0);
        ctx.restore();
    }
    ctx.save();
    ctx.font = getFont(config.fontWeightBold, config);
    for (var i = 0; i < 256; i++) {
        ctx.save();
        ctx.beginPath();
        ctx.rect(i * cellWidth, cellHeight, cellWidth, cellHeight);
        ctx.clip();
        ctx.fillText(String.fromCharCode(i), i * cellWidth, cellHeight);
        ctx.restore();
    }
    ctx.restore();
    ctx.font = getFont(config.fontWeight, config);
    for (var colorIndex = 0; colorIndex < 16; colorIndex++) {
        var y = (colorIndex + 2) * cellHeight;
        for (var i = 0; i < 256; i++) {
            ctx.save();
            ctx.beginPath();
            ctx.rect(i * cellWidth, y, cellWidth, cellHeight);
            ctx.clip();
            ctx.fillStyle = config.colors.ansi[colorIndex].css;
            ctx.fillText(String.fromCharCode(i), i * cellWidth, y);
            ctx.restore();
        }
    }
    ctx.font = getFont(config.fontWeightBold, config);
    for (var colorIndex = 0; colorIndex < 16; colorIndex++) {
        var y = (colorIndex + 2 + 16) * cellHeight;
        for (var i = 0; i < 256; i++) {
            ctx.save();
            ctx.beginPath();
            ctx.rect(i * cellWidth, y, cellWidth, cellHeight);
            ctx.clip();
            ctx.fillStyle = config.colors.ansi[colorIndex].css;
            ctx.fillText(String.fromCharCode(i), i * cellWidth, y);
            ctx.restore();
        }
    }
    ctx.restore();
    if (!('createImageBitmap' in context) || Platform_1.isFirefox || Platform_1.isSafari) {
        return canvas;
    }
    var charAtlasImageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    clearColor(charAtlasImageData, config.colors.background);
    return context.createImageBitmap(charAtlasImageData);
}
exports.generateStaticCharAtlasTexture = generateStaticCharAtlasTexture;
function clearColor(imageData, color) {
    var isEmpty = true;
    var r = color.rgba >>> 24;
    var g = color.rgba >>> 16 & 0xFF;
    var b = color.rgba >>> 8 & 0xFF;
    for (var offset = 0; offset < imageData.data.length; offset += 4) {
        if (imageData.data[offset] === r &&
            imageData.data[offset + 1] === g &&
            imageData.data[offset + 2] === b) {
            imageData.data[offset + 3] = 0;
        }
        else {
            isEmpty = false;
        }
    }
    return isEmpty;
}
exports.clearColor = clearColor;
function getFont(fontWeight, config) {
    return fontWeight + " " + config.fontSize * config.devicePixelRatio + "px " + config.fontFamily;
}

},{"../../core/Platform":21,"./Types":43}],38:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Types_1 = require("./Types");
function generateConfig(scaledCharWidth, scaledCharHeight, terminal, colors) {
    var clonedColors = {
        foreground: colors.foreground,
        background: colors.background,
        cursor: null,
        cursorAccent: null,
        selection: null,
        ansi: colors.ansi.slice(0, 16)
    };
    return {
        type: terminal.options.experimentalCharAtlas,
        devicePixelRatio: window.devicePixelRatio,
        scaledCharWidth: scaledCharWidth,
        scaledCharHeight: scaledCharHeight,
        fontFamily: terminal.options.fontFamily,
        fontSize: terminal.options.fontSize,
        fontWeight: terminal.options.fontWeight,
        fontWeightBold: terminal.options.fontWeightBold,
        allowTransparency: terminal.options.allowTransparency,
        colors: clonedColors
    };
}
exports.generateConfig = generateConfig;
function configEquals(a, b) {
    for (var i = 0; i < a.colors.ansi.length; i++) {
        if (a.colors.ansi[i].rgba !== b.colors.ansi[i].rgba) {
            return false;
        }
    }
    return a.type === b.type &&
        a.devicePixelRatio === b.devicePixelRatio &&
        a.fontFamily === b.fontFamily &&
        a.fontSize === b.fontSize &&
        a.fontWeight === b.fontWeight &&
        a.fontWeightBold === b.fontWeightBold &&
        a.allowTransparency === b.allowTransparency &&
        a.scaledCharWidth === b.scaledCharWidth &&
        a.scaledCharHeight === b.scaledCharHeight &&
        a.colors.foreground === b.colors.foreground &&
        a.colors.background === b.colors.background;
}
exports.configEquals = configEquals;
function is256Color(colorCode) {
    return colorCode < Types_1.DEFAULT_COLOR;
}
exports.is256Color = is256Color;

},{"./Types":43}],39:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Types_1 = require("./Types");
var BaseCharAtlas_1 = require("./BaseCharAtlas");
var ColorManager_1 = require("../ColorManager");
var CharAtlasGenerator_1 = require("./CharAtlasGenerator");
var LRUMap_1 = require("./LRUMap");
var Platform_1 = require("../../core/Platform");
var TEXTURE_WIDTH = 1024;
var TEXTURE_HEIGHT = 1024;
var TRANSPARENT_COLOR = {
    css: 'rgba(0, 0, 0, 0)',
    rgba: 0
};
var FRAME_CACHE_DRAW_LIMIT = 100;
var GLYPH_BITMAP_COMMIT_DELAY = 100;
function getGlyphCacheKey(glyph) {
    return glyph.code << 21 | glyph.bg << 12 | glyph.fg << 3 | (glyph.bold ? 0 : 4) + (glyph.dim ? 0 : 2) + (glyph.italic ? 0 : 1);
}
exports.getGlyphCacheKey = getGlyphCacheKey;
var DynamicCharAtlas = (function (_super) {
    __extends(DynamicCharAtlas, _super);
    function DynamicCharAtlas(document, _config) {
        var _this = _super.call(this) || this;
        _this._config = _config;
        _this._drawToCacheCount = 0;
        _this._glyphsWaitingOnBitmap = [];
        _this._bitmapCommitTimeout = null;
        _this._bitmap = null;
        _this._cacheCanvas = document.createElement('canvas');
        _this._cacheCanvas.width = TEXTURE_WIDTH;
        _this._cacheCanvas.height = TEXTURE_HEIGHT;
        _this._cacheCtx = _this._cacheCanvas.getContext('2d', { alpha: true });
        var tmpCanvas = document.createElement('canvas');
        tmpCanvas.width = _this._config.scaledCharWidth;
        tmpCanvas.height = _this._config.scaledCharHeight;
        _this._tmpCtx = tmpCanvas.getContext('2d', { alpha: _this._config.allowTransparency });
        _this._width = Math.floor(TEXTURE_WIDTH / _this._config.scaledCharWidth);
        _this._height = Math.floor(TEXTURE_HEIGHT / _this._config.scaledCharHeight);
        var capacity = _this._width * _this._height;
        _this._cacheMap = new LRUMap_1.default(capacity);
        _this._cacheMap.prealloc(capacity);
        return _this;
    }
    DynamicCharAtlas.prototype.dispose = function () {
        if (this._bitmapCommitTimeout !== null) {
            window.clearTimeout(this._bitmapCommitTimeout);
            this._bitmapCommitTimeout = null;
        }
    };
    DynamicCharAtlas.prototype.beginFrame = function () {
        this._drawToCacheCount = 0;
    };
    DynamicCharAtlas.prototype.draw = function (ctx, glyph, x, y) {
        if (glyph.code === 32) {
            return true;
        }
        if (!this._canCache(glyph)) {
            return false;
        }
        var glyphKey = getGlyphCacheKey(glyph);
        var cacheValue = this._cacheMap.get(glyphKey);
        if (cacheValue !== null && cacheValue !== undefined) {
            this._drawFromCache(ctx, cacheValue, x, y);
            return true;
        }
        else if (this._drawToCacheCount < FRAME_CACHE_DRAW_LIMIT) {
            var index = void 0;
            if (this._cacheMap.size < this._cacheMap.capacity) {
                index = this._cacheMap.size;
            }
            else {
                index = this._cacheMap.peek().index;
            }
            var cacheValue_1 = this._drawToCache(glyph, index);
            this._cacheMap.set(glyphKey, cacheValue_1);
            this._drawFromCache(ctx, cacheValue_1, x, y);
            return true;
        }
        return false;
    };
    DynamicCharAtlas.prototype._canCache = function (glyph) {
        return glyph.code < 256;
    };
    DynamicCharAtlas.prototype._toCoordinateX = function (index) {
        return (index % this._width) * this._config.scaledCharWidth;
    };
    DynamicCharAtlas.prototype._toCoordinateY = function (index) {
        return Math.floor(index / this._width) * this._config.scaledCharHeight;
    };
    DynamicCharAtlas.prototype._drawFromCache = function (ctx, cacheValue, x, y) {
        if (cacheValue.isEmpty) {
            return;
        }
        var cacheX = this._toCoordinateX(cacheValue.index);
        var cacheY = this._toCoordinateY(cacheValue.index);
        ctx.drawImage(cacheValue.inBitmap ? this._bitmap : this._cacheCanvas, cacheX, cacheY, this._config.scaledCharWidth, this._config.scaledCharHeight, x, y, this._config.scaledCharWidth, this._config.scaledCharHeight);
    };
    DynamicCharAtlas.prototype._getColorFromAnsiIndex = function (idx) {
        if (idx < this._config.colors.ansi.length) {
            return this._config.colors.ansi[idx];
        }
        return ColorManager_1.DEFAULT_ANSI_COLORS[idx];
    };
    DynamicCharAtlas.prototype._getBackgroundColor = function (glyph) {
        if (this._config.allowTransparency) {
            return TRANSPARENT_COLOR;
        }
        else if (glyph.bg === Types_1.INVERTED_DEFAULT_COLOR) {
            return this._config.colors.foreground;
        }
        else if (glyph.bg < 256) {
            return this._getColorFromAnsiIndex(glyph.bg);
        }
        return this._config.colors.background;
    };
    DynamicCharAtlas.prototype._getForegroundColor = function (glyph) {
        if (glyph.fg === Types_1.INVERTED_DEFAULT_COLOR) {
            return this._config.colors.background;
        }
        else if (glyph.fg < 256) {
            return this._getColorFromAnsiIndex(glyph.fg);
        }
        return this._config.colors.foreground;
    };
    DynamicCharAtlas.prototype._drawToCache = function (glyph, index) {
        this._drawToCacheCount++;
        this._tmpCtx.save();
        var backgroundColor = this._getBackgroundColor(glyph);
        this._tmpCtx.globalCompositeOperation = 'copy';
        this._tmpCtx.fillStyle = backgroundColor.css;
        this._tmpCtx.fillRect(0, 0, this._config.scaledCharWidth, this._config.scaledCharHeight);
        this._tmpCtx.globalCompositeOperation = 'source-over';
        var fontWeight = glyph.bold ? this._config.fontWeightBold : this._config.fontWeight;
        var fontStyle = glyph.italic ? 'italic' : '';
        this._tmpCtx.font =
            fontStyle + " " + fontWeight + " " + this._config.fontSize * this._config.devicePixelRatio + "px " + this._config.fontFamily;
        this._tmpCtx.textBaseline = 'top';
        this._tmpCtx.fillStyle = this._getForegroundColor(glyph).css;
        if (glyph.dim) {
            this._tmpCtx.globalAlpha = Types_1.DIM_OPACITY;
        }
        this._tmpCtx.fillText(glyph.chars, 0, 0);
        this._tmpCtx.restore();
        var imageData = this._tmpCtx.getImageData(0, 0, this._config.scaledCharWidth, this._config.scaledCharHeight);
        var isEmpty = false;
        if (!this._config.allowTransparency) {
            isEmpty = CharAtlasGenerator_1.clearColor(imageData, backgroundColor);
        }
        var x = this._toCoordinateX(index);
        var y = this._toCoordinateY(index);
        this._cacheCtx.putImageData(imageData, x, y);
        var cacheValue = {
            index: index,
            isEmpty: isEmpty,
            inBitmap: false
        };
        this._addGlyphToBitmap(cacheValue);
        return cacheValue;
    };
    DynamicCharAtlas.prototype._addGlyphToBitmap = function (cacheValue) {
        var _this = this;
        if (!('createImageBitmap' in window) || Platform_1.isFirefox || Platform_1.isSafari) {
            return;
        }
        this._glyphsWaitingOnBitmap.push(cacheValue);
        if (this._bitmapCommitTimeout !== null) {
            return;
        }
        this._bitmapCommitTimeout = window.setTimeout(function () { return _this._generateBitmap(); }, GLYPH_BITMAP_COMMIT_DELAY);
    };
    DynamicCharAtlas.prototype._generateBitmap = function () {
        var _this = this;
        var glyphsMovingToBitmap = this._glyphsWaitingOnBitmap;
        this._glyphsWaitingOnBitmap = [];
        window.createImageBitmap(this._cacheCanvas).then(function (bitmap) {
            _this._bitmap = bitmap;
            for (var i = 0; i < glyphsMovingToBitmap.length; i++) {
                var value = glyphsMovingToBitmap[i];
                value.inBitmap = true;
            }
        });
        this._bitmapCommitTimeout = null;
    };
    return DynamicCharAtlas;
}(BaseCharAtlas_1.default));
exports.default = DynamicCharAtlas;

},{"../../core/Platform":21,"../ColorManager":28,"./BaseCharAtlas":35,"./CharAtlasGenerator":37,"./LRUMap":40,"./Types":43}],40:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var LRUMap = (function () {
    function LRUMap(capacity) {
        this.capacity = capacity;
        this._map = {};
        this._head = null;
        this._tail = null;
        this._nodePool = [];
        this.size = 0;
    }
    LRUMap.prototype._unlinkNode = function (node) {
        var prev = node.prev;
        var next = node.next;
        if (node === this._head) {
            this._head = next;
        }
        if (node === this._tail) {
            this._tail = prev;
        }
        if (prev !== null) {
            prev.next = next;
        }
        if (next !== null) {
            next.prev = prev;
        }
    };
    LRUMap.prototype._appendNode = function (node) {
        var tail = this._tail;
        if (tail !== null) {
            tail.next = node;
        }
        node.prev = tail;
        node.next = null;
        this._tail = node;
        if (this._head === null) {
            this._head = node;
        }
    };
    LRUMap.prototype.prealloc = function (count) {
        var nodePool = this._nodePool;
        for (var i = 0; i < count; i++) {
            nodePool.push({
                prev: null,
                next: null,
                key: null,
                value: null
            });
        }
    };
    LRUMap.prototype.get = function (key) {
        var node = this._map[key];
        if (node !== undefined) {
            this._unlinkNode(node);
            this._appendNode(node);
            return node.value;
        }
        return null;
    };
    LRUMap.prototype.peekValue = function (key) {
        var node = this._map[key];
        if (node !== undefined) {
            return node.value;
        }
        return null;
    };
    LRUMap.prototype.peek = function () {
        var head = this._head;
        return head === null ? null : head.value;
    };
    LRUMap.prototype.set = function (key, value) {
        var node = this._map[key];
        if (node !== undefined) {
            node = this._map[key];
            this._unlinkNode(node);
            node.value = value;
        }
        else if (this.size >= this.capacity) {
            node = this._head;
            this._unlinkNode(node);
            delete this._map[node.key];
            node.key = key;
            node.value = value;
            this._map[key] = node;
        }
        else {
            var nodePool = this._nodePool;
            if (nodePool.length > 0) {
                node = nodePool.pop();
                node.key = key;
                node.value = value;
            }
            else {
                node = {
                    prev: null,
                    next: null,
                    key: key,
                    value: value
                };
            }
            this._map[key] = node;
            this.size++;
        }
        this._appendNode(node);
    };
    return LRUMap;
}());
exports.default = LRUMap;

},{}],41:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var BaseCharAtlas_1 = require("./BaseCharAtlas");
var NoneCharAtlas = (function (_super) {
    __extends(NoneCharAtlas, _super);
    function NoneCharAtlas(document, config) {
        return _super.call(this) || this;
    }
    NoneCharAtlas.prototype.draw = function (ctx, glyph, x, y) {
        return false;
    };
    return NoneCharAtlas;
}(BaseCharAtlas_1.default));
exports.default = NoneCharAtlas;

},{"./BaseCharAtlas":35}],42:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Types_1 = require("./Types");
var CharAtlasGenerator_1 = require("./CharAtlasGenerator");
var BaseCharAtlas_1 = require("./BaseCharAtlas");
var CharAtlasUtils_1 = require("./CharAtlasUtils");
var StaticCharAtlas = (function (_super) {
    __extends(StaticCharAtlas, _super);
    function StaticCharAtlas(_document, _config) {
        var _this = _super.call(this) || this;
        _this._document = _document;
        _this._config = _config;
        _this._canvasFactory = function (width, height) {
            var canvas = _this._document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            return canvas;
        };
        return _this;
    }
    StaticCharAtlas.prototype._doWarmUp = function () {
        var _this = this;
        var result = CharAtlasGenerator_1.generateStaticCharAtlasTexture(window, this._canvasFactory, this._config);
        if (result instanceof HTMLCanvasElement) {
            this._texture = result;
        }
        else {
            result.then(function (texture) {
                _this._texture = texture;
            });
        }
    };
    StaticCharAtlas.prototype._isCached = function (glyph, colorIndex) {
        var isAscii = glyph.code < 256;
        var isBasicColor = glyph.fg < 16;
        var isDefaultColor = glyph.fg === Types_1.DEFAULT_COLOR;
        var isDefaultBackground = glyph.bg === Types_1.DEFAULT_COLOR;
        return isAscii && (isBasicColor || isDefaultColor) && isDefaultBackground && !glyph.italic;
    };
    StaticCharAtlas.prototype.draw = function (ctx, glyph, x, y) {
        if (this._texture === null || this._texture === undefined) {
            return false;
        }
        var colorIndex = 0;
        if (CharAtlasUtils_1.is256Color(glyph.fg)) {
            colorIndex = 2 + glyph.fg + (glyph.bold ? 16 : 0);
        }
        else if (glyph.fg === Types_1.DEFAULT_COLOR) {
            if (glyph.bold) {
                colorIndex = 1;
            }
        }
        if (!this._isCached(glyph, colorIndex)) {
            return false;
        }
        ctx.save();
        var charAtlasCellWidth = this._config.scaledCharWidth + Types_1.CHAR_ATLAS_CELL_SPACING;
        var charAtlasCellHeight = this._config.scaledCharHeight + Types_1.CHAR_ATLAS_CELL_SPACING;
        if (glyph.dim) {
            ctx.globalAlpha = Types_1.DIM_OPACITY;
        }
        ctx.drawImage(this._texture, glyph.code * charAtlasCellWidth, colorIndex * charAtlasCellHeight, charAtlasCellWidth, this._config.scaledCharHeight, x, y, charAtlasCellWidth, this._config.scaledCharHeight);
        ctx.restore();
        return true;
    };
    return StaticCharAtlas;
}(BaseCharAtlas_1.default));
exports.default = StaticCharAtlas;

},{"./BaseCharAtlas":35,"./CharAtlasGenerator":37,"./CharAtlasUtils":38,"./Types":43}],43:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_COLOR = 256;
exports.INVERTED_DEFAULT_COLOR = 257;
exports.DIM_OPACITY = 0.5;
exports.CHAR_ATLAS_CELL_SPACING = 1;

},{}],44:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var EventEmitter_1 = require("../../common/EventEmitter");
var ColorManager_1 = require("../ColorManager");
var RenderDebouncer_1 = require("../../ui/RenderDebouncer");
var DomRendererRowFactory_1 = require("./DomRendererRowFactory");
var Types_1 = require("../atlas/Types");
var TERMINAL_CLASS_PREFIX = 'xterm-dom-renderer-owner-';
var ROW_CONTAINER_CLASS = 'xterm-rows';
var FG_CLASS_PREFIX = 'xterm-fg-';
var BG_CLASS_PREFIX = 'xterm-bg-';
var FOCUS_CLASS = 'xterm-focus';
var SELECTION_CLASS = 'xterm-selection';
var nextTerminalId = 1;
var DomRenderer = (function (_super) {
    __extends(DomRenderer, _super);
    function DomRenderer(_terminal, theme) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._terminalClass = nextTerminalId++;
        _this._rowElements = [];
        var allowTransparency = _this._terminal.options.allowTransparency;
        _this.colorManager = new ColorManager_1.ColorManager(document, allowTransparency);
        _this.setTheme(theme);
        _this._rowContainer = document.createElement('div');
        _this._rowContainer.classList.add(ROW_CONTAINER_CLASS);
        _this._rowContainer.style.lineHeight = 'normal';
        _this._rowContainer.setAttribute('aria-hidden', 'true');
        _this._refreshRowElements(_this._terminal.cols, _this._terminal.rows);
        _this._selectionContainer = document.createElement('div');
        _this._selectionContainer.classList.add(SELECTION_CLASS);
        _this._selectionContainer.setAttribute('aria-hidden', 'true');
        _this.dimensions = {
            scaledCharWidth: null,
            scaledCharHeight: null,
            scaledCellWidth: null,
            scaledCellHeight: null,
            scaledCharLeft: null,
            scaledCharTop: null,
            scaledCanvasWidth: null,
            scaledCanvasHeight: null,
            canvasWidth: null,
            canvasHeight: null,
            actualCellWidth: null,
            actualCellHeight: null
        };
        _this._updateDimensions();
        _this._renderDebouncer = new RenderDebouncer_1.RenderDebouncer(_this._terminal, _this._renderRows.bind(_this));
        _this._rowFactory = new DomRendererRowFactory_1.DomRendererRowFactory(document);
        _this._terminal.element.classList.add(TERMINAL_CLASS_PREFIX + _this._terminalClass);
        _this._terminal.screenElement.appendChild(_this._rowContainer);
        _this._terminal.screenElement.appendChild(_this._selectionContainer);
        _this._terminal.linkifier.on("linkhover", function (e) { return _this._onLinkHover(e); });
        _this._terminal.linkifier.on("linkleave", function (e) { return _this._onLinkLeave(e); });
        return _this;
    }
    DomRenderer.prototype.dispose = function () {
        this._terminal.element.classList.remove(TERMINAL_CLASS_PREFIX + this._terminalClass);
        this._terminal.screenElement.removeChild(this._rowContainer);
        this._terminal.screenElement.removeChild(this._selectionContainer);
        this._terminal.screenElement.removeChild(this._themeStyleElement);
        this._terminal.screenElement.removeChild(this._dimensionsStyleElement);
        _super.prototype.dispose.call(this);
    };
    DomRenderer.prototype._updateDimensions = function () {
        var _this = this;
        this.dimensions.scaledCharWidth = Math.floor(this._terminal.charMeasure.width * window.devicePixelRatio);
        this.dimensions.scaledCharHeight = Math.ceil(this._terminal.charMeasure.height * window.devicePixelRatio);
        this.dimensions.scaledCellWidth = this.dimensions.scaledCharWidth + Math.round(this._terminal.options.letterSpacing);
        this.dimensions.scaledCellHeight = Math.floor(this.dimensions.scaledCharHeight * this._terminal.options.lineHeight);
        this.dimensions.scaledCharLeft = 0;
        this.dimensions.scaledCharTop = 0;
        this.dimensions.scaledCanvasWidth = this.dimensions.scaledCellWidth * this._terminal.cols;
        this.dimensions.scaledCanvasHeight = this.dimensions.scaledCellHeight * this._terminal.rows;
        this.dimensions.canvasWidth = Math.round(this.dimensions.scaledCanvasWidth / window.devicePixelRatio);
        this.dimensions.canvasHeight = Math.round(this.dimensions.scaledCanvasHeight / window.devicePixelRatio);
        this.dimensions.actualCellWidth = this.dimensions.canvasWidth / this._terminal.cols;
        this.dimensions.actualCellHeight = this.dimensions.canvasHeight / this._terminal.rows;
        this._rowElements.forEach(function (element) {
            element.style.width = _this.dimensions.canvasWidth + "px";
            element.style.height = _this.dimensions.actualCellHeight + "px";
            element.style.lineHeight = _this.dimensions.actualCellHeight + "px";
            element.style.overflow = 'hidden';
        });
        if (!this._dimensionsStyleElement) {
            this._dimensionsStyleElement = document.createElement('style');
            this._terminal.screenElement.appendChild(this._dimensionsStyleElement);
        }
        var styles = this._terminalSelector + " ." + ROW_CONTAINER_CLASS + " span {" +
            " display: inline-block;" +
            " height: 100%;" +
            " vertical-align: top;" +
            (" width: " + this.dimensions.actualCellWidth + "px") +
            "}";
        this._dimensionsStyleElement.innerHTML = styles;
        this._selectionContainer.style.height = this._terminal._viewportElement.style.height;
        this._terminal.screenElement.style.width = this.dimensions.canvasWidth + "px";
        this._terminal.screenElement.style.height = this.dimensions.canvasHeight + "px";
    };
    DomRenderer.prototype.setTheme = function (theme) {
        var _this = this;
        if (theme) {
            this.colorManager.setTheme(theme);
        }
        if (!this._themeStyleElement) {
            this._themeStyleElement = document.createElement('style');
            this._terminal.screenElement.appendChild(this._themeStyleElement);
        }
        var styles = this._terminalSelector + " ." + ROW_CONTAINER_CLASS + " {" +
            (" color: " + this.colorManager.colors.foreground.css + ";") +
            (" background-color: " + this.colorManager.colors.background.css + ";") +
            (" font-family: " + this._terminal.getOption('fontFamily') + ";") +
            (" font-size: " + this._terminal.getOption('fontSize') + "px;") +
            "}";
        styles +=
            this._terminalSelector + " span:not(." + DomRendererRowFactory_1.BOLD_CLASS + ") {" +
                (" font-weight: " + this._terminal.options.fontWeight + ";") +
                "}" +
                (this._terminalSelector + " span." + DomRendererRowFactory_1.BOLD_CLASS + " {") +
                (" font-weight: " + this._terminal.options.fontWeightBold + ";") +
                "}" +
                (this._terminalSelector + " span." + DomRendererRowFactory_1.ITALIC_CLASS + " {") +
                " font-style: italic;" +
                "}";
        styles +=
            this._terminalSelector + " ." + ROW_CONTAINER_CLASS + ":not(." + FOCUS_CLASS + ") ." + DomRendererRowFactory_1.CURSOR_CLASS + " {" +
                (" outline: 1px solid " + this.colorManager.colors.cursor.css + ";") +
                " outline-offset: -1px;" +
                "}" +
                (this._terminalSelector + " ." + ROW_CONTAINER_CLASS + "." + FOCUS_CLASS + " ." + DomRendererRowFactory_1.CURSOR_CLASS + "." + DomRendererRowFactory_1.CURSOR_STYLE_BLOCK_CLASS + " {") +
                (" background-color: " + this.colorManager.colors.cursor.css + ";") +
                (" color: " + this.colorManager.colors.cursorAccent.css + ";") +
                "}" +
                (this._terminalSelector + " ." + ROW_CONTAINER_CLASS + "." + FOCUS_CLASS + " ." + DomRendererRowFactory_1.CURSOR_CLASS + "." + DomRendererRowFactory_1.CURSOR_STYLE_BAR_CLASS + " {") +
                (" box-shadow: 1px 0 0 " + this.colorManager.colors.cursor.css + " inset;") +
                "}" +
                (this._terminalSelector + " ." + ROW_CONTAINER_CLASS + "." + FOCUS_CLASS + " ." + DomRendererRowFactory_1.CURSOR_CLASS + "." + DomRendererRowFactory_1.CURSOR_STYLE_UNDERLINE_CLASS + " {") +
                (" box-shadow: 0 -1px 0 " + this.colorManager.colors.cursor.css + " inset;") +
                "}";
        styles +=
            this._terminalSelector + " ." + SELECTION_CLASS + " {" +
                " position: absolute;" +
                " top: 0;" +
                " left: 0;" +
                " z-index: 1;" +
                " pointer-events: none;" +
                "}" +
                (this._terminalSelector + " ." + SELECTION_CLASS + " div {") +
                " position: absolute;" +
                (" background-color: " + this.colorManager.colors.selection.css + ";") +
                "}";
        this.colorManager.colors.ansi.forEach(function (c, i) {
            styles +=
                _this._terminalSelector + " ." + FG_CLASS_PREFIX + i + " { color: " + c.css + "; }" +
                    (_this._terminalSelector + " ." + BG_CLASS_PREFIX + i + " { background-color: " + c.css + "; }");
        });
        styles +=
            this._terminalSelector + " ." + FG_CLASS_PREFIX + Types_1.INVERTED_DEFAULT_COLOR + " { color: " + this.colorManager.colors.background.css + "; }" +
                (this._terminalSelector + " ." + BG_CLASS_PREFIX + Types_1.INVERTED_DEFAULT_COLOR + " { background-color: " + this.colorManager.colors.foreground.css + "; }");
        this._themeStyleElement.innerHTML = styles;
        return this.colorManager.colors;
    };
    DomRenderer.prototype.onWindowResize = function (devicePixelRatio) {
        this._updateDimensions();
    };
    DomRenderer.prototype._refreshRowElements = function (cols, rows) {
        for (var i = this._rowElements.length; i <= rows; i++) {
            var row = document.createElement('div');
            this._rowContainer.appendChild(row);
            this._rowElements.push(row);
        }
        while (this._rowElements.length > rows) {
            this._rowContainer.removeChild(this._rowElements.pop());
        }
    };
    DomRenderer.prototype.onResize = function (cols, rows) {
        this._refreshRowElements(cols, rows);
        this._updateDimensions();
    };
    DomRenderer.prototype.onCharSizeChanged = function () {
        this._updateDimensions();
    };
    DomRenderer.prototype.onBlur = function () {
        this._rowContainer.classList.remove(FOCUS_CLASS);
    };
    DomRenderer.prototype.onFocus = function () {
        this._rowContainer.classList.add(FOCUS_CLASS);
    };
    DomRenderer.prototype.onSelectionChanged = function (start, end, columnSelectMode) {
        while (this._selectionContainer.children.length) {
            this._selectionContainer.removeChild(this._selectionContainer.children[0]);
        }
        if (!start || !end) {
            return;
        }
        var viewportStartRow = start[1] - this._terminal.buffer.ydisp;
        var viewportEndRow = end[1] - this._terminal.buffer.ydisp;
        var viewportCappedStartRow = Math.max(viewportStartRow, 0);
        var viewportCappedEndRow = Math.min(viewportEndRow, this._terminal.rows - 1);
        if (viewportCappedStartRow >= this._terminal.rows || viewportCappedEndRow < 0) {
            return;
        }
        var documentFragment = document.createDocumentFragment();
        if (columnSelectMode) {
            documentFragment.appendChild(this._createSelectionElement(viewportCappedStartRow, start[0], end[0], viewportCappedEndRow - viewportCappedStartRow + 1));
        }
        else {
            var startCol = viewportStartRow === viewportCappedStartRow ? start[0] : 0;
            var endCol = viewportCappedStartRow === viewportCappedEndRow ? end[0] : this._terminal.cols;
            documentFragment.appendChild(this._createSelectionElement(viewportCappedStartRow, startCol, endCol));
            var middleRowsCount = viewportCappedEndRow - viewportCappedStartRow - 1;
            documentFragment.appendChild(this._createSelectionElement(viewportCappedStartRow + 1, 0, this._terminal.cols, middleRowsCount));
            if (viewportCappedStartRow !== viewportCappedEndRow) {
                var endCol_1 = viewportEndRow === viewportCappedEndRow ? end[0] : this._terminal.cols;
                documentFragment.appendChild(this._createSelectionElement(viewportCappedEndRow, 0, endCol_1));
            }
        }
        this._selectionContainer.appendChild(documentFragment);
    };
    DomRenderer.prototype._createSelectionElement = function (row, colStart, colEnd, rowCount) {
        if (rowCount === void 0) { rowCount = 1; }
        var element = document.createElement('div');
        element.style.height = rowCount * this.dimensions.actualCellHeight + "px";
        element.style.top = row * this.dimensions.actualCellHeight + "px";
        element.style.left = colStart * this.dimensions.actualCellWidth + "px";
        element.style.width = this.dimensions.actualCellWidth * (colEnd - colStart) + "px";
        return element;
    };
    DomRenderer.prototype.onCursorMove = function () {
    };
    DomRenderer.prototype.onOptionsChanged = function () {
        this._updateDimensions();
        this.setTheme(undefined);
        this._terminal.refresh(0, this._terminal.rows - 1);
    };
    DomRenderer.prototype.clear = function () {
        this._rowElements.forEach(function (e) { return e.innerHTML = ''; });
    };
    DomRenderer.prototype.refreshRows = function (start, end) {
        this._renderDebouncer.refresh(start, end);
    };
    DomRenderer.prototype._renderRows = function (start, end) {
        var terminal = this._terminal;
        var cursorAbsoluteY = terminal.buffer.ybase + terminal.buffer.y;
        var cursorX = this._terminal.buffer.x;
        for (var y = start; y <= end; y++) {
            var rowElement = this._rowElements[y];
            rowElement.innerHTML = '';
            var row = y + terminal.buffer.ydisp;
            var lineData = terminal.buffer.lines.get(row);
            var cursorStyle = terminal.options.cursorStyle;
            rowElement.appendChild(this._rowFactory.createRow(lineData, row === cursorAbsoluteY, cursorStyle, cursorX, this.dimensions.actualCellWidth, terminal.cols));
        }
        this._terminal.emit('refresh', { start: start, end: end });
    };
    Object.defineProperty(DomRenderer.prototype, "_terminalSelector", {
        get: function () {
            return "." + TERMINAL_CLASS_PREFIX + this._terminalClass;
        },
        enumerable: true,
        configurable: true
    });
    DomRenderer.prototype.registerCharacterJoiner = function (handler) { return -1; };
    DomRenderer.prototype.deregisterCharacterJoiner = function (joinerId) { return false; };
    DomRenderer.prototype._onLinkHover = function (e) {
        this._setCellUnderline(e.x1, e.x2, e.y1, e.y2, e.cols, true);
    };
    DomRenderer.prototype._onLinkLeave = function (e) {
        this._setCellUnderline(e.x1, e.x2, e.y1, e.y2, e.cols, false);
    };
    DomRenderer.prototype._setCellUnderline = function (x, x2, y, y2, cols, enabled) {
        while (x !== x2 || y !== y2) {
            var row = this._rowElements[y];
            if (!row) {
                return;
            }
            var span = row.children[x];
            span.style.textDecoration = enabled ? 'underline' : 'none';
            x = (x + 1) % cols;
            if (x === 0) {
                y++;
            }
        }
    };
    return DomRenderer;
}(EventEmitter_1.EventEmitter));
exports.DomRenderer = DomRenderer;

},{"../../common/EventEmitter":17,"../../ui/RenderDebouncer":50,"../ColorManager":28,"../atlas/Types":43,"./DomRendererRowFactory":45}],45:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Buffer_1 = require("../../Buffer");
var Types_1 = require("../atlas/Types");
exports.BOLD_CLASS = 'xterm-bold';
exports.ITALIC_CLASS = 'xterm-italic';
exports.CURSOR_CLASS = 'xterm-cursor';
exports.CURSOR_STYLE_BLOCK_CLASS = 'xterm-cursor-block';
exports.CURSOR_STYLE_BAR_CLASS = 'xterm-cursor-bar';
exports.CURSOR_STYLE_UNDERLINE_CLASS = 'xterm-cursor-underline';
var DomRendererRowFactory = (function () {
    function DomRendererRowFactory(_document) {
        this._document = _document;
    }
    DomRendererRowFactory.prototype.createRow = function (lineData, isCursorRow, cursorStyle, cursorX, cellWidth, cols) {
        var fragment = this._document.createDocumentFragment();
        var lineLength = 0;
        for (var x = Math.min(lineData.length, cols) - 1; x >= 0; x--) {
            var charData = lineData.get(x);
            var code = charData[Buffer_1.CHAR_DATA_CODE_INDEX];
            if (code !== Buffer_1.NULL_CELL_CODE || (isCursorRow && x === cursorX)) {
                lineLength = x + 1;
                break;
            }
        }
        for (var x = 0; x < lineLength; x++) {
            var charData = lineData.get(x);
            var char = charData[Buffer_1.CHAR_DATA_CHAR_INDEX];
            var attr = charData[Buffer_1.CHAR_DATA_ATTR_INDEX];
            var width = charData[Buffer_1.CHAR_DATA_WIDTH_INDEX];
            if (width === 0) {
                continue;
            }
            var charElement = this._document.createElement('span');
            if (width > 1) {
                charElement.style.width = cellWidth * width + "px";
            }
            var flags = attr >> 18;
            var bg = attr & 0x1ff;
            var fg = (attr >> 9) & 0x1ff;
            if (isCursorRow && x === cursorX) {
                charElement.classList.add(exports.CURSOR_CLASS);
                switch (cursorStyle) {
                    case 'bar':
                        charElement.classList.add(exports.CURSOR_STYLE_BAR_CLASS);
                        break;
                    case 'underline':
                        charElement.classList.add(exports.CURSOR_STYLE_UNDERLINE_CLASS);
                        break;
                    default:
                        charElement.classList.add(exports.CURSOR_STYLE_BLOCK_CLASS);
                        break;
                }
            }
            if (flags & 8) {
                var temp = bg;
                bg = fg;
                fg = temp;
                if (fg === Types_1.DEFAULT_COLOR) {
                    fg = Types_1.INVERTED_DEFAULT_COLOR;
                }
                if (bg === Types_1.DEFAULT_COLOR) {
                    bg = Types_1.INVERTED_DEFAULT_COLOR;
                }
            }
            if (flags & 1) {
                if (fg < 8) {
                    fg += 8;
                }
                charElement.classList.add(exports.BOLD_CLASS);
            }
            if (flags & 64) {
                charElement.classList.add(exports.ITALIC_CLASS);
            }
            charElement.textContent = char;
            if (fg !== Types_1.DEFAULT_COLOR) {
                charElement.classList.add("xterm-fg-" + fg);
            }
            if (bg !== Types_1.DEFAULT_COLOR) {
                charElement.classList.add("xterm-bg-" + bg);
            }
            fragment.appendChild(charElement);
        }
        return fragment;
    };
    return DomRendererRowFactory;
}());
exports.DomRendererRowFactory = DomRendererRowFactory;

},{"../../Buffer":2,"../atlas/Types":43}],46:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var EventEmitter_1 = require("../common/EventEmitter");
var CharMeasure = (function (_super) {
    __extends(CharMeasure, _super);
    function CharMeasure(document, parentElement) {
        var _this = _super.call(this) || this;
        _this._document = document;
        _this._parentElement = parentElement;
        _this._measureElement = _this._document.createElement('span');
        _this._measureElement.classList.add('xterm-char-measure-element');
        _this._measureElement.textContent = 'W';
        _this._measureElement.setAttribute('aria-hidden', 'true');
        _this._parentElement.appendChild(_this._measureElement);
        return _this;
    }
    Object.defineProperty(CharMeasure.prototype, "width", {
        get: function () {
            return this._width;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CharMeasure.prototype, "height", {
        get: function () {
            return this._height;
        },
        enumerable: true,
        configurable: true
    });
    CharMeasure.prototype.measure = function (options) {
        this._measureElement.style.fontFamily = options.fontFamily;
        this._measureElement.style.fontSize = options.fontSize + "px";
        var geometry = this._measureElement.getBoundingClientRect();
        if (geometry.width === 0 || geometry.height === 0) {
            return;
        }
        if (this._width !== geometry.width || this._height !== geometry.height) {
            this._width = geometry.width;
            this._height = Math.ceil(geometry.height);
            this.emit('charsizechanged');
        }
    };
    return CharMeasure;
}(EventEmitter_1.EventEmitter));
exports.CharMeasure = CharMeasure;

},{"../common/EventEmitter":17}],47:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function prepareTextForTerminal(text) {
    return text.replace(/\r?\n/g, '\r');
}
exports.prepareTextForTerminal = prepareTextForTerminal;
function bracketTextForPaste(text, bracketedPasteMode) {
    if (bracketedPasteMode) {
        return '\x1b[200~' + text + '\x1b[201~';
    }
    return text;
}
exports.bracketTextForPaste = bracketTextForPaste;
function copyHandler(ev, term, selectionManager) {
    if (term.browser.isMSIE) {
        window.clipboardData.setData('Text', selectionManager.selectionText);
    }
    else {
        ev.clipboardData.setData('text/plain', selectionManager.selectionText);
    }
    ev.preventDefault();
}
exports.copyHandler = copyHandler;
function pasteHandler(ev, term) {
    ev.stopPropagation();
    var text;
    var dispatchPaste = function (text) {
        text = prepareTextForTerminal(text);
        text = bracketTextForPaste(text, term.bracketedPasteMode);
        term.handler(text);
        term.textarea.value = '';
        term.emit('paste', text);
        term.cancel(ev);
    };
    if (term.browser.isMSIE) {
        if (window.clipboardData) {
            text = window.clipboardData.getData('Text');
            dispatchPaste(text);
        }
    }
    else {
        if (ev.clipboardData) {
            text = ev.clipboardData.getData('text/plain');
            dispatchPaste(text);
        }
    }
}
exports.pasteHandler = pasteHandler;
function moveTextAreaUnderMouseCursor(ev, textarea) {
    textarea.style.position = 'fixed';
    textarea.style.width = '20px';
    textarea.style.height = '20px';
    textarea.style.left = (ev.clientX - 10) + 'px';
    textarea.style.top = (ev.clientY - 10) + 'px';
    textarea.style.zIndex = '1000';
    textarea.focus();
    setTimeout(function () {
        textarea.style.position = null;
        textarea.style.width = null;
        textarea.style.height = null;
        textarea.style.left = null;
        textarea.style.top = null;
        textarea.style.zIndex = null;
    }, 200);
}
exports.moveTextAreaUnderMouseCursor = moveTextAreaUnderMouseCursor;
function rightClickHandler(ev, textarea, selectionManager, shouldSelectWord) {
    moveTextAreaUnderMouseCursor(ev, textarea);
    if (shouldSelectWord && !selectionManager.isClickInSelection(ev)) {
        selectionManager.selectWordAtCursor(ev);
    }
    textarea.value = selectionManager.selectionText;
    textarea.select();
}
exports.rightClickHandler = rightClickHandler;

},{}],48:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function addDisposableDomListener(node, type, handler, useCapture) {
    node.addEventListener(type, handler, useCapture);
    return {
        dispose: function () {
            if (!handler) {
                return;
            }
            node.removeEventListener(type, handler, useCapture);
            node = null;
            handler = null;
        }
    };
}
exports.addDisposableDomListener = addDisposableDomListener;

},{}],49:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Lifecycle_1 = require("../common/Lifecycle");
var Lifecycle_2 = require("./Lifecycle");
var HOVER_DURATION = 500;
var MouseZoneManager = (function (_super) {
    __extends(MouseZoneManager, _super);
    function MouseZoneManager(_terminal) {
        var _this = _super.call(this) || this;
        _this._terminal = _terminal;
        _this._zones = [];
        _this._areZonesActive = false;
        _this._tooltipTimeout = null;
        _this._currentZone = null;
        _this._lastHoverCoords = [null, null];
        _this.register(Lifecycle_2.addDisposableDomListener(_this._terminal.element, 'mousedown', function (e) { return _this._onMouseDown(e); }));
        _this._mouseMoveListener = function (e) { return _this._onMouseMove(e); };
        _this._clickListener = function (e) { return _this._onClick(e); };
        return _this;
    }
    MouseZoneManager.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this._deactivate();
    };
    MouseZoneManager.prototype.add = function (zone) {
        this._zones.push(zone);
        if (this._zones.length === 1) {
            this._activate();
        }
    };
    MouseZoneManager.prototype.clearAll = function (start, end) {
        if (this._zones.length === 0) {
            return;
        }
        if (!end) {
            start = 0;
            end = this._terminal.rows - 1;
        }
        for (var i = 0; i < this._zones.length; i++) {
            var zone = this._zones[i];
            if ((zone.y1 > start && zone.y1 <= end + 1) ||
                (zone.y2 > start && zone.y2 <= end + 1) ||
                (zone.y1 < start && zone.y2 > end + 1)) {
                if (this._currentZone && this._currentZone === zone) {
                    this._currentZone.leaveCallback();
                    this._currentZone = null;
                }
                this._zones.splice(i--, 1);
            }
        }
        if (this._zones.length === 0) {
            this._deactivate();
        }
    };
    MouseZoneManager.prototype._activate = function () {
        if (!this._areZonesActive) {
            this._areZonesActive = true;
            this._terminal.element.addEventListener('mousemove', this._mouseMoveListener);
            this._terminal.element.addEventListener('click', this._clickListener);
        }
    };
    MouseZoneManager.prototype._deactivate = function () {
        if (this._areZonesActive) {
            this._areZonesActive = false;
            this._terminal.element.removeEventListener('mousemove', this._mouseMoveListener);
            this._terminal.element.removeEventListener('click', this._clickListener);
        }
    };
    MouseZoneManager.prototype._onMouseMove = function (e) {
        if (this._lastHoverCoords[0] !== e.pageX || this._lastHoverCoords[1] !== e.pageY) {
            this._onHover(e);
            this._lastHoverCoords = [e.pageX, e.pageY];
        }
    };
    MouseZoneManager.prototype._onHover = function (e) {
        var _this = this;
        var zone = this._findZoneEventAt(e);
        if (zone === this._currentZone) {
            return;
        }
        if (this._currentZone) {
            this._currentZone.leaveCallback();
            this._currentZone = null;
            if (this._tooltipTimeout) {
                clearTimeout(this._tooltipTimeout);
            }
        }
        if (!zone) {
            return;
        }
        this._currentZone = zone;
        if (zone.hoverCallback) {
            zone.hoverCallback(e);
        }
        this._tooltipTimeout = setTimeout(function () { return _this._onTooltip(e); }, HOVER_DURATION);
    };
    MouseZoneManager.prototype._onTooltip = function (e) {
        this._tooltipTimeout = null;
        var zone = this._findZoneEventAt(e);
        if (zone && zone.tooltipCallback) {
            zone.tooltipCallback(e);
        }
    };
    MouseZoneManager.prototype._onMouseDown = function (e) {
        if (!this._areZonesActive) {
            return;
        }
        var zone = this._findZoneEventAt(e);
        if (zone) {
            if (zone.willLinkActivate(e)) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
        }
    };
    MouseZoneManager.prototype._onClick = function (e) {
        var zone = this._findZoneEventAt(e);
        if (zone) {
            zone.clickCallback(e);
            e.preventDefault();
            e.stopImmediatePropagation();
        }
    };
    MouseZoneManager.prototype._findZoneEventAt = function (e) {
        var coords = this._terminal.mouseHelper.getCoords(e, this._terminal.screenElement, this._terminal.charMeasure, this._terminal.cols, this._terminal.rows);
        if (!coords) {
            return null;
        }
        var x = coords[0];
        var y = coords[1];
        for (var i = 0; i < this._zones.length; i++) {
            var zone = this._zones[i];
            if (zone.y1 === zone.y2) {
                if (y === zone.y1 && x >= zone.x1 && x < zone.x2) {
                    return zone;
                }
            }
            else {
                if ((y === zone.y1 && x >= zone.x1) ||
                    (y === zone.y2 && x < zone.x2) ||
                    (y > zone.y1 && y < zone.y2)) {
                    return zone;
                }
            }
        }
        return null;
    };
    return MouseZoneManager;
}(Lifecycle_1.Disposable));
exports.MouseZoneManager = MouseZoneManager;
var MouseZone = (function () {
    function MouseZone(x1, y1, x2, y2, clickCallback, hoverCallback, tooltipCallback, leaveCallback, willLinkActivate) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.clickCallback = clickCallback;
        this.hoverCallback = hoverCallback;
        this.tooltipCallback = tooltipCallback;
        this.leaveCallback = leaveCallback;
        this.willLinkActivate = willLinkActivate;
    }
    return MouseZone;
}());
exports.MouseZone = MouseZone;

},{"../common/Lifecycle":18,"./Lifecycle":48}],50:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var RenderDebouncer = (function () {
    function RenderDebouncer(_terminal, _callback) {
        this._terminal = _terminal;
        this._callback = _callback;
        this._animationFrame = null;
    }
    RenderDebouncer.prototype.dispose = function () {
        if (this._animationFrame) {
            window.cancelAnimationFrame(this._animationFrame);
            this._animationFrame = null;
        }
    };
    RenderDebouncer.prototype.refresh = function (rowStart, rowEnd) {
        var _this = this;
        rowStart = rowStart !== null && rowStart !== undefined ? rowStart : 0;
        rowEnd = rowEnd !== null && rowEnd !== undefined ? rowEnd : this._terminal.rows - 1;
        var isRowStartSet = this._rowStart !== undefined && this._rowStart !== null;
        var isRowEndSet = this._rowEnd !== undefined && this._rowEnd !== null;
        this._rowStart = isRowStartSet ? Math.min(this._rowStart, rowStart) : rowStart;
        this._rowEnd = isRowEndSet ? Math.max(this._rowEnd, rowEnd) : rowEnd;
        if (this._animationFrame) {
            return;
        }
        this._animationFrame = window.requestAnimationFrame(function () { return _this._innerRefresh(); });
    };
    RenderDebouncer.prototype._innerRefresh = function () {
        this._rowStart = Math.max(this._rowStart, 0);
        this._rowEnd = Math.min(this._rowEnd, this._terminal.rows - 1);
        this._callback(this._rowStart, this._rowEnd);
        this._rowStart = null;
        this._rowEnd = null;
        this._animationFrame = null;
    };
    return RenderDebouncer;
}());
exports.RenderDebouncer = RenderDebouncer;

},{}],51:[function(require,module,exports){
"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var Lifecycle_1 = require("../common/Lifecycle");
var ScreenDprMonitor = (function (_super) {
    __extends(ScreenDprMonitor, _super);
    function ScreenDprMonitor() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ScreenDprMonitor.prototype.setListener = function (listener) {
        var _this = this;
        if (this._listener) {
            this.clearListener();
        }
        this._listener = listener;
        this._outerListener = function () {
            _this._listener(window.devicePixelRatio, _this._currentDevicePixelRatio);
            _this._updateDpr();
        };
        this._updateDpr();
    };
    ScreenDprMonitor.prototype.dispose = function () {
        _super.prototype.dispose.call(this);
        this.clearListener();
    };
    ScreenDprMonitor.prototype._updateDpr = function () {
        if (this._resolutionMediaMatchList) {
            this._resolutionMediaMatchList.removeListener(this._outerListener);
        }
        this._currentDevicePixelRatio = window.devicePixelRatio;
        this._resolutionMediaMatchList = window.matchMedia("screen and (resolution: " + window.devicePixelRatio + "dppx)");
        this._resolutionMediaMatchList.addListener(this._outerListener);
    };
    ScreenDprMonitor.prototype.clearListener = function () {
        if (!this._listener) {
            return;
        }
        this._resolutionMediaMatchList.removeListener(this._outerListener);
        this._listener = null;
        this._outerListener = null;
    };
    return ScreenDprMonitor;
}(Lifecycle_1.Disposable));
exports.ScreenDprMonitor = ScreenDprMonitor;

},{"../common/Lifecycle":18}],52:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.clone = function (val, depth) {
    if (depth === void 0) { depth = 5; }
    if (typeof val !== 'object') {
        return val;
    }
    if (val === null) {
        return null;
    }
    var clonedObject = Array.isArray(val) ? [] : {};
    for (var key in val) {
        clonedObject[key] = depth <= 1 ? val[key] : exports.clone(val[key], depth - 1);
    }
    return clonedObject;
};

},{}],53:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var MouseHelper = (function () {
    function MouseHelper(_renderer) {
        this._renderer = _renderer;
    }
    MouseHelper.prototype.setRenderer = function (renderer) {
        this._renderer = renderer;
    };
    MouseHelper.getCoordsRelativeToElement = function (event, element) {
        if (event.pageX === null || event.pageX === undefined) {
            return null;
        }
        var originalElement = element;
        var x = event.pageX;
        var y = event.pageY;
        while (element) {
            x -= element.offsetLeft;
            y -= element.offsetTop;
            element = element.offsetParent;
        }
        element = originalElement;
        while (element && element !== element.ownerDocument.body) {
            x += element.scrollLeft;
            y += element.scrollTop;
            element = element.parentElement;
        }
        return [x, y];
    };
    MouseHelper.prototype.getCoords = function (event, element, charMeasure, colCount, rowCount, isSelection) {
        if (!charMeasure.width || !charMeasure.height) {
            return null;
        }
        var coords = MouseHelper.getCoordsRelativeToElement(event, element);
        if (!coords) {
            return null;
        }
        coords[0] = Math.ceil((coords[0] + (isSelection ? this._renderer.dimensions.actualCellWidth / 2 : 0)) / this._renderer.dimensions.actualCellWidth);
        coords[1] = Math.ceil(coords[1] / this._renderer.dimensions.actualCellHeight);
        coords[0] = Math.min(Math.max(coords[0], 1), colCount + (isSelection ? 1 : 0));
        coords[1] = Math.min(Math.max(coords[1], 1), rowCount);
        return coords;
    };
    MouseHelper.prototype.getRawByteCoords = function (event, element, charMeasure, colCount, rowCount) {
        var coords = this.getCoords(event, element, charMeasure, colCount, rowCount);
        var x = coords[0];
        var y = coords[1];
        x += 32;
        y += 32;
        return { x: x, y: y };
    };
    return MouseHelper;
}());
exports.MouseHelper = MouseHelper;

},{}],54:[function(require,module,exports){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Terminal_1 = require("./public/Terminal");
module.exports = Terminal_1.Terminal;

},{"./public/Terminal":25}]},{},[54])(54)
});
//# sourceMappingURL=xterm.js.map
