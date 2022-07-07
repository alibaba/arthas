module.exports = {
  '/en/doc': [
      {
          text: 'DOCS',
          children: [
              {
                  text: 'Home',
                  link: '/en/'
              },
              {
                  text: 'Introduction',
                  link: '/en/doc/README.md'
              },
              '/en/doc/quick-start.md',
              '/en/doc/install-detail.md',
              '/en/doc/download.md',
              '/en/doc/advanced-use.md',
              {
                  text: 'Other features',
                  collapsible: true,
                  children:[
                      '/en/doc/async.md',
                      '/en/doc/save-log.md',
                      '/en/doc/batch-support.md',
                      {
                          text:"How to use ognl",
                          link:"",
                          children:[
                              {
                                  text:"Basic ognl example",
                                  link:"https://github.com/alibaba/arthas/issues/11"
                              },
                              {
                                  text:"Ognl special uses",
                                  link:"https://github.com/alibaba/arthas/issues/71"
                              }
                          ]
                      }
                  ]
              },
              {
                  text: 'Commands',
                  collapsible: true,
                  link: '/en/doc/commands.md',
                  children: [
                      {
                          text: 'jvm',
                          collapsible: false,
                          children: [
                              '/en/doc/dashboard.md',
                              '/en/doc/thread.md',
                              '/en/doc/jvm.md',
                              '/en/doc/memory.md',
                              '/en/doc/sysprop.md',
                              '/en/doc/sysenv.md',
                              '/en/doc/vmoption.md',
                              '/en/doc/perfcounter.md',
                              '/en/doc/logger.md',
                              '/en/doc/mbean.md',
                              '/en/doc/getstatic.md',
                              '/en/doc/ognl.md',
                              '/en/doc/heapdump.md',
                              '/en/doc/vmtool.md',
                          ],
                      },
                      {
                          text: 'class/classloader',
                          collapsible: false,
                          children: [
                              '/en/doc/sc.md',
                              '/en/doc/sm.md',
                              '/en/doc/jad.md',
                              '/en/doc/classloader.md',
                              '/en/doc/mc.md',
                              '/en/doc/dump.md',
                              '/en/doc/retransform.md',
                              '/en/doc/redefine.md',
                          ],
                      },
                      {
                          text: 'monitor/watch/trace - related',
                          collapsible: false,
                          children: [
                              '/en/doc/monitor.md',
                              '/en/doc/watch.md',
                              '/en/doc/trace.md',
                              '/en/doc/stack.md',
                              '/en/doc/tt.md',
                          ],
                      },
                      {
                          text: 'other',
                          collapsible: false,
                          children: [
                              '/en/doc/profiler.md',
                              '/en/doc/cat.md',
                              '/en/doc/echo.md',
                              '/en/doc/grep.md',
                              '/en/doc/base64.md',
                              '/en/doc/tee.md',
                              '/en/doc/pwd.md',
                              '/en/doc/auth.md',
                              '/en/doc/options.md',
                          ],
                      },
                      {
                          text: 'Basic',
                          collapsible: false,
                          children: [
                              '/en/doc/help.md',
                              '/en/doc/cls.md',
                              '/en/doc/session.md',
                              '/en/doc/reset.md',
                              '/en/doc/history.md',
                              '/en/doc/quit.md',
                              '/en/doc/stop.md',
                              {
                                  text: 'keymap',
                                  link: '/en/doc/keymap.md'
                              },
                          ],
                      },
                  ],
              },
              '/en/doc/web-console.md',
              '/en/doc/tunnel.md',
              '/en/doc/http-api.md',
              '/en/doc/docker.md',
              '/en/doc/spring-boot-starter.md',
              '/en/doc/idea-plugin.md',
              '/en/doc/faq.md',
              {
                  text: 'User cases',
                  link: 'https://github.com/alibaba/arthas/issues?q=label%3Auser-case'
              },
              {
                  text: 'Start me at github',
                  link: 'https://github.com/alibaba/arthas'
              },
              {
                  text: 'Compile and debug/CONTRIBUTING',
                  link: 'https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md'
              },
              {
                  text: 'Release Notes',
                  link: 'https://github.com/alibaba/arthas/releases'
              },
              {
                  text: 'Contact us',
                  link: '/en/doc/contact-us.md'
              },
          ],

      },
  ]
}
