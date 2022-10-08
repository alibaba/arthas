var registerApplications = null;
var serverInfo = null;
var arthasAgentSplit = '@';
$(document).ready(function () {
    initArthasHtmlTitle();
    reloadRegisterApplications();
    initServerInfo();
});

/**
 * 获取注册的arthas客户端
 */
function reloadRegisterApplications() {
    var result = reqSync("/api/arthas/access/agents", "get");
    registerApplications = result;
    initServiceSelect("#selectServer", registerApplications, "");
    $("#selectServer").change(function (e) {
        var service = $('#selectServer option:selected').val();
        selectServiceOnchange(service)
    });
}

/**
 * 初始化arthas的连接信息
 */
function initServerInfo() {
    var result = reqSync("/api/arthas/server", "get");
    serverInfo = result;
    $('#ip').val(result.clientConnectHost);
    $('#port').val(result.port);
}


function initArthasHtmlTitle() {
    var title = reqSync("/api/arthas/html/title", "get");
    if (title) {
        document.title = title;
    }
}

/**
 * 初始化服务名下拉选择框
 */
function initServiceSelect(uiSelect, list, key) {
    $(uiSelect).html('');
    for (var i = 0; i < list.length; i++) {
        agentGroup = list[i];
        $(uiSelect).append("<option value=" + agentGroup.service + ">" + agentGroup.service + "</option>");
    }
    selectServiceOnchange(list[0].service);
}

/**
 * 服务名下拉选择框改变事件
 */
function selectServiceOnchange(service) {
    var filter = registerApplications.filter(p => p.service == service)[0];
    var list = filter.agents;
    $("#selectAgent").html('');
    for (var i = 0; i < list.length; i++) {
        var agent = list[i];
        var opt = service + arthasAgentSplit + agent.id;
        var text = agent.info.host + ':' + agent.info.port;
        $("#selectAgent").append("<option value=" + opt + ">" + text + "</option>");
    }
}


function reqSync(url, method) {
    var result = null;
    $.ajax({
        url: url,
        type: method,
        async: false, //使用同步的方式,true为异步方式
        headers: {
            'Content-Type': 'application/json;charset=utf8;',
        },
        success: function (data) {
            // console.log(data);
            result = data;
        },
        error: function (data) {
            console.log("error");
        }
    });
    return result;
}
