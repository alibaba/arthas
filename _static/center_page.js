var head = document.head || document.getElementsByTagName('head')[0];
var style = null;

// Centers the page content dynamically.
function centerPage() {
  if (style) {
    head.removeChild(style);
    style = null;
  }

  var windowWidth = window.innerWidth;
  if (windowWidth <= 768) {
    return;
  }

  var sideWidth = 300;
  var contentWidth = 800;
  var ribbonWidth = 150;
  var leftMargin = Math.max(0, (windowWidth - sideWidth - contentWidth) / 2);
  var scrollbarWidth = document.body ? windowWidth - document.body.clientWidth : 0;
  var css = '';

  css += '.wy-nav-side { left: ' + leftMargin + 'px; }';
  css += "\n";
  css += '.wy-nav-content-wrap { margin-left: ' + (sideWidth + leftMargin) + 'px; }';
  css += "\n";
  css += '.github-fork-ribbon-wrapper.right { left: ' +
         (Math.min(windowWidth - scrollbarWidth, sideWidth + contentWidth + leftMargin) - ribbonWidth) + 'px; }';
  css += "\n";

  var newStyle = document.createElement('style');
  newStyle.type = 'text/css';
  if (newStyle.styleSheet) {
    newStyle.styleSheet.cssText = css;
  } else {
    newStyle.appendChild(document.createTextNode(css));
  }

  head.appendChild(newStyle);
  style = newStyle;
}

centerPage();
window.addEventListener('resize', centerPage);
// Adjust the position of the 'fork me at GitHub' ribbon after document.body is available,
// so that we can calculate the width of the scroll bar correctly.
window.addEventListener('DOMContentLoaded', centerPage);
