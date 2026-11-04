// 火焰图核心实现 - Frame类
function Frame(flameGraph, raw, depth, isRoot = false) {
  this.fg = flameGraph;
  this.isRoot = isRoot;
  this.raw = raw;
  this.weight = 0;
  this.selfWeight = 0;
  this.parent = null;
  this.index = -1;
  this.hasLeftSide = false;
  this.hasRightSide = false;
  this.depth = depth;
  this.weightOfBaseline1 = 0;
  this.selfWeightOfBaseline1 = 0;
  this.weightOfBaseline2 = 0;
  this.selfWeightOfBaseline2 = 0;
  this.text = '';

  this.addWeight = function (weight) {
    this.weight += weight;
  };

  this.addWeightOfBaselines = function (weightOfBaseline1, weightOfBaseline2) {
    this.weightOfBaseline1 += weightOfBaseline1;
    this.weightOfBaseline2 += weightOfBaseline2;
  };

  this.addSelfWeightOfBaselines = function (selfWeightOfBaseline1, selfWeightOfBaseline2) {
    this.selfWeightOfBaseline1 += selfWeightOfBaseline1;
    this.selfWeightOfBaseline2 += selfWeightOfBaseline2;
  };

  this.addSelfWeight = function (weight) {
    this.selfWeight += weight;
  };

  this.setPinned = function () {
    this.pinned = true;
    if (this.parent !== this.fg.$root) {
      this.parent.setPinned();
    }
  };

  this.setSide = function (left) {
    if (left) {
      this.parent.hasLeftSide = true;
    } else {
      this.parent.hasRightSide = true;
    }
  };

  this.clearSide = function (left) {
    if (left) {
      this.parent.hasLeftSide = false;
    } else {
      this.parent.hasRightSide = false;
    }
  };

  this.clearFindSide = function () {
    if (this.fg.$pinnedFrameLeft) {
      this.fg.$pinnedFrameLeft.clearSide(true);
      this.fg.$pinnedFrameLeft = null;
    }
    if (this.fg.$pinnedFrameRight) {
      this.fg.$pinnedFrameRight.clearSide(false);
      this.fg.$pinnedFrameRight = null;
    }
  };

  this.findSide = function () {
    let n = this;
    let p = this.parent;
    while (n.index === 0) {
      if (p === this.fg.$root) {
        break;
      }
      n = p;
      p = p.parent;
    }

    if (n.index > 0) {
      let t = p.children[n.index - 1];
      this.fg.$pinnedFrameLeft = t;
      t.setSide(true);
    }

    n = this;
    p = this.parent;
    while (n.index === p.children.length - 1) {
      if (p === this.fg.$root) {
        break;
      }
      if (p.selfWeight > 0) {
        return;
      }
      n = p;
      p = p.parent;
    }

    if (n.index < p.children.length - 1) {
      let t = p.children[n.index + 1];
      this.fg.$pinnedFrameRight = t;
      t.setSide(false);
    }
  };

  this.setUnpinned = function () {
    this.pinned = false;
    if (this.parent !== this.fg.$root) {
      this.parent.setUnpinned();
    }
  };

  this.findOrAddChild = function (raw) {
    if (!this.children) {
      this.children = [];
    }

    for (let i = 0; i < this.children.length; i++) {
      const child = this.children[i];
      if (this.fg.$$frameEquator(this.fg.$dataSource, child.raw, raw)) {
        return child;
      }
    }

    return this.addChild(raw);
  };

  this.addChild = function (raw) {
    if (!this.children) {
      this.children = [];
    }

    const child = new Frame(this.fg, raw, this.depth + 1);
    child.index = this.children.length;
    child.parent = this;
    this.children.push(child);
    return child;
  };

  this.sort = function () {
    if (!this.children) {
      return;
    }

    if (this.children.length > 1) {
      this.children.sort((left, right) => right.weight - left.weight);
    }

    for (let i = 0; i < this.children.length; i++) {
      this.children[i].index = i;
      this.children[i].sort();
    }
  };

  this.diffPercent = function () {
    let cp = 0;
    if (this.fg.$totalWeightOfBaseline2 > 0) {
      cp = this.weightOfBaseline2 / this.fg.$totalWeightOfBaseline2;
    }

    let bp = 0;
    if (this.fg.$totalWeightOfBaseline1 > 0) {
      bp = this.weightOfBaseline1 / this.fg.$totalWeightOfBaseline1;
    }

    if (bp > 0) {
      let r = (cp - bp) / bp;
      if (r > 1) {
        return 1;
      }
      if (r < -1) {
        return -1;
      }
      return r;
    } else if (cp > 0) {
      return 1;
    } else {
      return 0;
    }
  };

  this.drawSelf = function () {
    if (!this.isRoot) {
      this.infomation = this.fg.$$diff
        ? {
            selfWeight: this.selfWeight,
            weight: this.weight,
            totalWeight: this.fg.$totalWeight,
            selfWeightOfBaseline1: this.selfWeightOfBaseline1,
            weightOfBaseline1: this.weightOfBaseline1,
            totalWeightOfBaseline1: this.fg.$totalWeightOfBaseline1,
            selfWeightOfBaseline2: this.selfWeightOfBaseline2,
            weightOfBaseline2: this.weightOfBaseline2,
            totalWeightOfBaseline2: this.fg.$totalWeightOfBaseline2,
            diffPercent: this.diffPercent()
          }
        : {
            selfWeight: this.selfWeight,
            weight: this.weight,
            totalWeight: this.fg.$totalWeight
          };

      this.text = this.fg.$$textGenerator(flameGraph.$dataSource, raw, this.infomation);
      this.infomation.text = this.text;
    }
    if (!this.color) {
      this.color = this.fg.$$colorSelector(this.fg.dataSource, this.raw, this.infomation);
    }

    this.fg.$context.fillStyle = this.color[0];
    this.fg.$context.fillRect(this.x, this.y, this.width, this.height);

    this.visibleText = null;
    if (this.width > this.fg.$showTextWidthThreshold && this.text.length > 0) {
      this.fg.$context.font = this.isRoot ? this.fg.$rootFont : this.fg.$font;
      this.fg.$context.fillStyle = this.color[1];
      this.fg.$context.textBaseline = 'middle';
      let w = this.fg.$context.measureText(this.text).width;
      let leftW = this.width - 2 * this.fg.$textGap;
      if (w <= leftW) {
        this.fg.$context.fillText(
          this.text,
          this.x + this.fg.$textGap,
          this.y + this.height / 2 + 1
        );
        this.visibleText = this.text;
      } else {
        let len = Math.floor(
          (this.text.length * (leftW - this.fg.$context.measureText(this.fg.$moreText).width)) / w
        );
        let text = null;
        for (let i = len; i > 0; i--) {
          text = this.text.substring(0, len) + this.fg.$moreText;
          if (this.fg.$context.measureText(text).width <= leftW) {
            break;
          }
          text = null;
        }
        if (text != null) {
          this.fg.$context.fillText(
            text,
            this.x + this.fg.$textGap,
            this.y + this.height / 2 + 1
          );
        }
        this.visibleText = text;
      }
    }
    this.fg.$stackTraceMaxDrawnDepth = Math.max(this.depth, this.fg.$stackTraceMaxDrawnDepth);
    this.fg.$sibling[this.depth].push(this);
  };

  this.resetPosition = function () {
    this.x = 0;
    this.y = 0;
    this.width = 0;
    this.height = 0;

    if (this.children) {
      this.children.forEach((c) => c.resetPosition());
    }
  };

  this.draw = function (x, y, w, h) {
    this.x = x;
    this.y = y;
    this.fg.$maxY = Math.max(y + h, this.fg.$maxY);
    this.width = w;
    this.height = h;

    this.drawSelf();

    if (this.children) {
      if (this.fg.$pinned && this === this.fg.$pinnedFrame) {
        this.fg.$drawingChildrenOfPinnedFrame = true;
      }
      let xGap = this.fg.$xGap;
      let childY = this.fg.downward ? y + h + this.fg.$yGap : y - h - this.fg.$yGap;
      if (
        !this.fg.$pinned ||
        this === this.fg.$pinnedFrame ||
        this.fg.$drawingChildrenOfPinnedFrame
      ) {
        const space = this.children.length - 1;
        let leftWidth = w;
        if ((space * xGap) / w > this.fg.$xGapThreashold) {
          xGap = 0;
        } else {
          leftWidth = leftWidth - space * xGap;
        }
        let endX = x + w;
        let nextX = x;
        for (let i = 0; i < this.children.length; i++) {
          let cw = 0;
          if (i === this.children.length - 1 && this.selfWeight === 0) {
            cw = endX - nextX;
          } else {
            cw = (leftWidth * this.children[i].weight) / this.weight;
          }
          this.children[i].draw(nextX, childY, cw, h);
          nextX += cw + xGap;
        }
      } else {
        let sideWidth = 15;
        if (this === this.fg.$pinnedFrameLeft || this === this.fg.$pinnedFrameRight) {
          this.fg.$drawingChildrenOfSideFrame = true;
          this.fg.$drawingLeftSide = this === this.fg.$pinnedFrameLeft;
        }
        if (this.fg.$drawingChildrenOfSideFrame) {
          if (!this.fg.$drawingLeftSide || this.selfWeight === 0) {
            for (let i = 0; i < this.children.length; i++) {
              if (
                (this.fg.$drawingLeftSide && i === this.children.length - 1) ||
                (!this.fg.$drawingLeftSide && i === 0)
              ) {
                this.children[i].draw(x, childY, sideWidth, h);
              } else {
                this.children[i].resetPosition();
              }
            }
          } else {
            for (let i = 0; i < this.children.length; i++) {
              this.children[i].resetPosition();
            }
          }
        } else {
          for (let i = 0; i < this.children.length; i++) {
            let xGap = this.fg.$xGap;
            if ((xGap * 2) / w > this.fg.$xGapThreashold) {
              xGap = 0;
            }
            if (this.children[i].pinned) {
              let cx = x;
              let cw = w;
              if (this.hasLeftSide) {
                cx += sideWidth + xGap;
                cw -= sideWidth + xGap;
              }
              if (this.hasRightSide) {
                cw -= sideWidth + xGap;
              } else if (this.selfWeight > 0 && this.fg.$pinnedFrame.parent === this) {
                cw -= sideWidth;
              }
              this.children[i].draw(cx, childY, cw, h);
            } else if (this.children[i] === this.fg.$pinnedFrameLeft) {
              this.children[i].draw(x, childY, sideWidth, h);
            } else if (this.children[i] === this.fg.$pinnedFrameRight) {
              this.children[i].draw(x + w - sideWidth, childY, sideWidth, h);
            } else {
              this.children[i].resetPosition();
            }
          }
        }
        if (this === this.fg.$pinnedFrameLeft || this === this.fg.$pinnedFrameRight) {
          this.fg.$drawingChildrenOfSideFrame = false;
        }
      }
      if (this.fg.$pinned && this === this.fg.$pinnedFrame) {
        this.fg.$drawingChildrenOfPinnedFrame = false;
      }
    }
  };

  this.contain = function (x, y) {
    return x > this.x && x < this.x + this.width && y > this.y && y < this.y + this.height;
  };

  this.maxDepth = function () {
    let maxDepth = this.depth;
    if (this.children) {
      for (let i = 0; i < this.children.length; i++) {
        maxDepth = Math.max(maxDepth, this.children[i].maxDepth());
      }
    }
    return maxDepth;
  };

  function hexToRGB(hex, alpha = 1) {
    let r = parseInt(hex.slice(1, 3), 16),
      g = parseInt(hex.slice(3, 5), 16),
      b = parseInt(hex.slice(5, 7), 16);

    if (hex.length === 9) {
      alpha = parseInt(hex.slice(7, 9), 16) / 255;
    }

    return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')';
  }

  this.touch = function (x, y) {
    this.fg.$frameMask.style.left = this.x + 'px';
    this.fg.$frameMask.style.top = this.y + 'px';
    this.fg.$frameMask.style.width = this.width + 'px';
    this.fg.$frameMask.style.height = this.height + 'px';
    this.fg.$frameMask.style.backgroundColor = this.color[0];
    this.fg.$frameMaskText.style.color = this.color[1];
    this.fg.$frameMaskText.style.paddingLeft = this.fg.$textGap + 'px';
    this.fg.$frameMaskText.style.lineHeight = this.fg.$frameMask.style.height;
    this.fg.$frameMaskText.style.fontSize = this === this.fg.$root ? '14px' : '12px';
    this.fg.$frameMaskText.innerText = this.visibleText;
    this.fg.$frameMask.style.cursor = 'pointer';
    this.fg.$frameMask.style.visibility = 'visible';
    this.fg.$frameMask.focus();

    let top = this.y + this.height - this.fg.$flameGraphInner.scrollTop;
    let detailsNode = this.fg.shadowRoot.getElementById('frame-postcard-content-main-details');
    if (detailsNode) {
      detailsNode.parentNode.removeChild(detailsNode);
    }

    if (this !== this.fg.$root) {
      this.fg.$framePostcardContentMain.style.backgroundColor = this.color[0];
      this.fg.$framePostcardContentMain.style.color = this.color[1];
      let hp = Math.round((this.depth / this.maxDepth()) * 100);
      let direction = this.fg.downward ? 'to bottom' : 'to top';

      this.fg.$framePostcardContentMainTitle.innerText = this.fg.$$titleGenerator(
        this.fg.$dataSource,
        this.raw,
        this.infomation
      );
      this.fg.$framePostcardContentMainLine.style.background =
        'linear-gradient(' +
        direction +
        ', ' +
        hexToRGB(this.color[1], 0.7) +
        ' 0% ' +
        hp +
        '%, ' +
        hexToRGB(this.color[1], 0.2) +
        ' ' +
        hp +
        '% 100%)';

      let details = this.fg.$$detailsGenerator(this.fg.$dataSource, this.raw, this.infomation);
      if (details) {
        let keys = Object.keys(details);
        let content = null;
        if (keys.length > 0) {
          content =
            '<div id ="frame-postcard-content-main-details" style="width: 100%; font-size: 11px; word-wrap: break-word">';
          for (let i = 0; i < keys.length; i++) {
            content += '<div style="margin-top: 5px; opacity: .7">' + keys[i] + '</div>';
            content += '<ul style="margin: 2px 0 0 -15px"><li>' + details[keys[i]] + '</li></ul>';
          }
          content += '</div>';
        }
        if (content != null) {
          let t = document.createElement('template');
          t.innerHTML = content.trim();
          this.fg.$framePostcardContentMain.appendChild(t.content.firstChild);
        }
      }

      this.fg.$framePostcardContentFoot.innerText = this.fg.$$footTextGenerator(
        this.fg.$dataSource,
        this.raw,
        this.infomation
      );
      let wp = Math.round((this.weight / this.fg.$totalWeight) * 100);

      let footColor = this.fg.$$footColorSelector(this.fg.$dataSource, this.raw, this.infomation);
      let startColor, endColor, fontColor;
      if (footColor.length > 2) {
        startColor = footColor[0];
        endColor = footColor[1];
        fontColor = footColor[2];
      } else {
        startColor = endColor = footColor[0];
        fontColor = footColor[2];
      }
      this.fg.$framePostcardContentFoot.style.background =
        'linear-gradient(to right, ' +
        hexToRGB(startColor) +
        ' 0% ' +
        wp +
        '%, ' +
        hexToRGB(endColor) +
        ' ' +
        wp +
        '% 100%)';
      this.fg.$framePostcardContentFoot.style.color = fontColor;

      this.fg.$framePostcardShadow.style.left = x + 'px';
      this.fg.$framePostcardShadow.style.top = top + 'px';
      this.fg.$framePostcard.style.visibility = 'visible';
      this.fg.decideFramePostcardLayout();

      if (this.fg.$$diff) {
        let diffPercent = this.diffPercent();
        let top;
        if (diffPercent > 0) {
          top = 0.5 * (1 - diffPercent) * 100 + '%';
        } else {
          top = (0.5 + 0.5 * -diffPercent) * 100 + '%';
        }
        this.fg.$colorArrow.style.top = top;
        this.fg.$colorArrow.style.visibility = 'visible';
      }
    }
    this.fg.$currentFrame = this;
  };

  this.leave = function () {
    this.fg.$framePostcard.style.visibility = 'hidden';
    this.fg.$frameMask.style.visibility = 'hidden';
    this.fg.$currentFrame = null;

    if (this.fg.$$diff) {
      this.fg.$colorArrow.style.visibility = 'hidden';
    }
  };

  this.clear = function () {
    this.children = null;
    this.weight = 0;
  };
}

// 导出Frame类供其他文件使用
if (typeof module !== 'undefined' && module.exports) {
  module.exports = Frame;
} else if (typeof window !== 'undefined') {
  window.Frame = Frame;
}
