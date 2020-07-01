const InputStatus= {
    /**
     * Allow input new commands
     */
    ALLOW_INPUT: 'ALLOW_INPUT',

    /**
     * Allow interrupt running job
     */
    ALLOW_INTERRUPT: 'ALLOW_INTERRUPT',

    /**
     * Disable input and interrupt
     */
    DISABLED: 'DISABLED'
};

//session data
function getDefaultSessionData() {
    return {
        sessionId: null,
        consumerId: null,
        lastPullResultTime: 0,
        commandLine: '',
        commandLineDisabled: true,
        commandResults: [],
        executingJobId: null,
        lastFinishedJobId: 0,
        inputStatus: InputStatus.DISABLED,
        startPullResults: false,
    };
}

let sessionData = getDefaultSessionData();

//session view
let SessionView = Vue.component('session-view',{
    template: '#session-view',
    data() {
        return sessionData;
    },
    methods: {
        resetData: function () {
            Object.assign(this.$data, getDefaultSessionData());
        },
        messageClass(item){
            return {
                commandMsg: item.command!=null,
                errorMsg: item.state == 'FAILED' || item.state == 'REFUSED',
            }
        },
        onSessionReady(){
            this.commandLineDisabled = false;
        },
        openNewSession(){
            window.open("/ui/", "_blank");
        },
        initPage(){
            console.log("initPage");
            let sessionId = this.$route.params.sessionId;
            if (sessionId && sessionId!='undefined'){
                //join_session, init new consumerId
                axios
                    .post('/api',{
                        "action": "join_session",
                        "sessionId": sessionId
                    })
                    .then(response => {
                        let apiResponse = response.data;
                        if (apiResponse.state == "SUCCEEDED" && sessionId == apiResponse.sessionId){
                            this.sessionId = apiResponse.sessionId;
                            this.consumerId = apiResponse.consumerId;
                            this.pullResults();
                            this.onSessionReady();
                        }else {
                            //加入会话失败，创建新会话
                            this.askForNewSession(true);
                        }
                    })
                    .catch(error => { // 请求失败处理
                        console.log(error);
                        this.$alert('Connect to server failed: '+error.message, {
                            type: 'error'
                        });
                    });
            } else {
                this.askForNewSession(false);
            }
        },

        askForNewSession(showAlert){
            if (showAlert) {
                this.$alert('The session does not exist, new session will be opened', {
                    type: 'warning',
                    callback: action => {
                        if (action == 'confirm') {
                            this.initSession();
                        }
                    }
                });
            } else {
                this.initSession();
            }
        },

        initSession(){
            console.log("initSession");
            this.resetData();
            axios
                .post('/api',{
                    "action": "init_session"
                })
                .then(response => {
                    let apiResponse = response.data;
                    if (apiResponse.state == "SUCCEEDED"){
                        this.sessionId = apiResponse.sessionId;
                        this.consumerId = apiResponse.consumerId;
                        this.$router.push("/session/"+this.sessionId);
                        this.pullResults();
                        this.onSessionReady();
                    }
                })
                .catch(error => { // 请求失败处理
                    console.log(error);
                    this.$alert('Init session failed: '+error.message, {
                        type: 'error'
                    });
                });
        },

        interruptJob(){
            console.log("interruptJob");
            axios
                .post('/api',{
                    "action": "interrupt_job",
                    "sessionId": this.sessionId,
                })
                .then(response => {
                    let apiResponse = response.data;
                    if (apiResponse.state == "SUCCEEDED"){
                        this.onSessionReady();
                    }
                })
                .catch(error =>{ // 请求失败处理
                    console.log(error);
                    this.$message.error('Interrupt current job failed: '+error.message);
                });
        },

        pullResults(){
            //保证只有一个拉取消息的timer
            if (this.startPullResults){
                return;
            }
            this.startPullResults = true;
            this.lastPullResultTime = new Date().getTime();
            //接收消息推送
            axios
                .post('/api',{
                    "action": "pull_results",
                    "sessionId": this.sessionId,
                    "consumerId": this.consumerId
                })
                .then(response => {
                    this.pullResultFailedCount = 0;
                    let apiResponse = response.data;
                    if (apiResponse.state == "SUCCEEDED"){
                        this.appendResults(apiResponse.body.results);
                        this.delayPullResults();
                    } else {
                        console.error("Pull results failed: ", apiResponse);
                        this.inputStatus = InputStatus.DISABLED;
                        this.$alert('Pull results failed: '+apiResponse.message, {
                            type: 'error'
                        });
                    }
                })
                .catch(error => { // 请求失败处理
                    console.log(error);
                    if(++this.pullResultFailedCount > 10){
                        this.inputStatus = InputStatus.DISABLED;
                        //show pull error
                        this.$alert('Pull results failed: '+error.message, {
                            type: 'error'
                        });
                    } else {
                        this.delayPullResults(2000);
                    }
                });
        },

        delayPullResults(delay) {
            this.startPullResults = false;
            if(!delay){
                delay = (new Date().getTime() - this.lastPullResultTime < 500)?500 : 50;
            }
            setTimeout(this.pullResults.bind(this), delay);
        },

        appendResults(results) {
            if (!results || !results.length || results.length == 0){
                return;
            }
            //split results
            while(results.length > 0){
                //Restrict command results
                while(this.commandResults.length > 500){
                    this.commandResults.shift();
                }
                let result = results.shift();
                console.log("result: ", result);
                if (result.type == 'input_status') {
                    this.inputStatus = result.inputStatus;
                    continue;
                }
                if (result.type == "status" && result.statusCode!=null){
                    //命令执行完毕后允许输入
                    this.setFinishedJobId(result.jobId);
                    if (result.message) {
                        this.commandResults.push(result);
                    }
                } else {
                    this.commandResults.push(result);
                }
            }

            this.scrollContentToBottom();
        },

        // appendCommand(response){
        //     if (response && response.state) {
        //         //Restrict command results
        //         while(this.commandResults.length > 500){
        //             this.commandResults.shift();
        //         }
        //         response.body.state = response.state;
        //         if (response.message) {
        //             response.body.message = response.message;
        //         }
        //         if ( response.state == 'SCHEDULED' || response.state == 'SUCCEEDED' ){
        //         } else {
        //         }
        //         this.commandResults.push(response.body)
        //     }
        // },

        executeCommand(event) {
            this.commandLine = this.commandLine.trim();
            if (this.commandLine == ''){
                return;
            }

            console.log("executing command: ", this.commandLine);
            axios
                .post('/api',{
                    "action": "async_exec",
                    "sessionId": this.sessionId,
                    "consumerId": this.consumerId,
                    "command": this.commandLine,
                })
                .then(response => {
                    let apiResponse = response.data;
                    if (apiResponse.state == "SCHEDULED" ){
                        //设置当前任务jobId，禁止输入
                        this.setExecutingJobId(apiResponse.body.jobId);
                    } else {
                        //command process error or refused
                        this.setExecutingJobId(null);
                    }
                    //this.appendCommand(apiResponse);

                })
                .catch(error =>{ // 请求失败处理
                    console.log(error);
                    //this.setExecutingJobId(null);
                    this.$message.error('Execute command failed: '+error.message);
                });


            //disable input
            this.commandLineDisabled = true;
        },

        setExecutingJobId(jobId) {
            if (jobId!=null && jobId > this.lastFinishedJobId) {
                this.executingJobId = jobId;
            } else {
                this.executingJobId = null;
                this.setFocusCommandInput();
            }
        },

        setFinishedJobId(jobId) {
            this.lastFinishedJobId = jobId;
            if (this.executingJobId && jobId >= this.executingJobId){
                this.setExecutingJobId(null);
            }
        },

        scrollContentToBottom(){
            setTimeout(function () {
                var container = this.$el.querySelector("#main-body");
                container.scrollTop = container.scrollHeight;
            }.bind(this), 50);
        },

        setFocusCommandInput() {
            this.commandLine = '';
            this.commandLineDisabled = false;
            setTimeout(function () {
                this.$refs.commandInput.focus();
            }.bind(this), 50);
        }
    },
    watch: {
        '$route'(to, from){
            console.log('route: ', to, from);
            this.initPage();
        }
    },
    created(){
        this.initPage();
    },
    updated(){

    }
});

let HistoryView = Vue.component('history-view', {
    template: '#history-view',
    data() {
        return {
        }
    }
});

//页面路由
const routes = [
    { path: '/', redirect: '/session/' },
    //{ path: '/', component: HistoryView },
    { path: '/session', component: SessionView },
    { path: '/session/:sessionId', component: SessionView },
    { path: '/history', component: HistoryView },
];

const router = new VueRouter({
    routes
});

new Vue({
    el: '#app',
    router,
    data: {
        sessionData
    },
    methods: {
    },
    watch: {
    },
    created(){

    }
});