"use strict";(self["webpackChunkgrpc_web_demo"]=self["webpackChunkgrpc_web_demo"]||[]).push([[508],{6508:function(e,t,n){n.r(t),n.d(t,{default:function(){return _}});var d=n(3396),a=n(7139);const s=e=>((0,d.dD)("data-v-b8b2da78"),e=e(),(0,d.Cn)(),e),i={style:{"text-align":"center"}},l=s((()=>(0,d._)("h3",null,"JobId",-1))),p={style:{"text-align":"center"}},r=s((()=>(0,d._)("h3",null,"working dir",-1)));function w(e,t,n,s,w,o){const u=(0,d.up)("Card"),c=(0,d.up)("Col"),g=(0,d.up)("Row");return(0,d.wg)(),(0,d.j4)(g,null,{default:(0,d.w5)((()=>[(0,d.Wm)(c,{span:"5"},{default:(0,d.w5)((()=>[(0,d.Wm)(u,{style:{width:"300px"}},{default:(0,d.w5)((()=>[(0,d._)("div",i,[l,(0,d._)("h3",null,(0,a.zw)(w.jobId),1)])])),_:1})])),_:1}),(0,d.Wm)(c,{span:"19"},{default:(0,d.w5)((()=>[(0,d.Wm)(u,{style:{width:"300px"}},{default:(0,d.w5)((()=>[(0,d._)("div",p,[r,(0,d._)("h3",null,(0,a.zw)(w.pwdResponse),1)])])),_:1})])),_:1})])),_:1})}var o=n(3378),u=n(1527),c={name:"pwd",inject:["apiHost"],data(){return{pwdClient:null,jobId:0,pwdResponse:"www"}},created(){let e=this.apiHost;this.pwdClient=new o.PwdClient(e),this.sendPwdRequest(),this.metadata={"Content-Type":"application/grpc-web-text"}},methods:{sendPwdRequest(){var e=new u.Empty;this.pwdClient.pwd(e,{},((e,t)=>{if(e)console.error(e);else{this.jobId=t.getJobid();const e=t.getType();if("pwd"==e&&t.hasStringstringmapvalue()){var n=t.getStringstringmapvalue(),d=n.getStringstringmapMap().get("workingDir");this.pwdResponse=d}}}))}}},g=n(89);const h=(0,g.Z)(c,[["render",w],["__scopeId","data-v-b8b2da78"]]);var _=h}}]);
//# sourceMappingURL=508.493ac51a.js.map