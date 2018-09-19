  function addBadge(parent, src, href) {
    var img = document.createElement('img');
    img.src = src;
    var a = document.createElement('a');
    a.href = href;
    a.appendChild(img);
    parent.appendChild(a);
    parent.appendChild(document.createElement('br'));
  }
  
  function addBadges(parent) {
    var li = document.createElement('li');
    li.className = 'toctree-l1';
    var div = document.createElement('div');
    div.className = 'project-badges';
    addBadge(div, 'https://img.shields.io/github/stars/alibaba/arthas.svg?style=flat-square',
      'https://github.com/alibaba/arthas');
    addBadge(div, 'https://img.shields.io/github/license/alibaba/arthas.svg?style=flat-square',
      'https://github.com/alibaba/arthas');
    addBadge(div, 'https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square',
      'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22');
    li.appendChild(div);
    parent.appendChild(li);
  }
  
  var menus = document.getElementsByClassName("wy-menu wy-menu-vertical");
  if (menus.length > 0) {
    var menu = menus[0];
    var lists = menu.getElementsByTagName('ul');
    if (lists.length > 0) {
      addBadges(lists[0]);
    }
  }