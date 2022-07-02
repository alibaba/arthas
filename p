[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] arthas-all                                                         [pom]
[INFO] math-game                                                          [jar]
[INFO] arthas-common                                                      [jar]
[INFO] arthas-spy                                                         [jar]
[INFO] arthas-vmtool                                                      [jar]
[INFO] arthas-tunnel-common                                               [jar]
[INFO] arthas-tunnel-client                                               [jar]
[INFO] arthas-memorycompiler                                              [jar]
[INFO] arthas-core                                                        [jar]
[INFO] arthas-agent                                                       [jar]
[INFO] arthas-client                                                      [jar]
[INFO] arthas-boot                                                        [jar]
[INFO] arthas-agent-attach                                                [jar]
[INFO] arthas-site                                                        [jar]
[INFO] arthas-packaging                                                   [jar]
[INFO] arthas-spring-boot-starter                                         [jar]
[INFO] arthas-testcase                                                    [jar]
[INFO] web-console                                                        [jar]
[INFO] arthas-tunnel-server                                               [jar]
[INFO] 
[INFO] --------------------< com.taobao.arthas:arthas-all >--------------------
[INFO] Building arthas-all 3.6.3                                         [1/19]
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for arthas-all 3.6.3:
[INFO] 
[INFO] arthas-all ......................................... FAILURE [  0.045 s]
[INFO] math-game .......................................... SKIPPED
[INFO] arthas-common ...................................... SKIPPED
[INFO] arthas-spy ......................................... SKIPPED
[INFO] arthas-vmtool ...................................... SKIPPED
[INFO] arthas-tunnel-common ............................... SKIPPED
[INFO] arthas-tunnel-client ............................... SKIPPED
[INFO] arthas-memorycompiler .............................. SKIPPED
[INFO] arthas-core ........................................ SKIPPED
[INFO] arthas-agent ....................................... SKIPPED
[INFO] arthas-client ...................................... SKIPPED
[INFO] arthas-boot ........................................ SKIPPED
[INFO] arthas-agent-attach ................................ SKIPPED
[INFO] arthas-site ........................................ SKIPPED
[INFO] arthas-packaging ................................... SKIPPED
[INFO] arthas-spring-boot-starter ......................... SKIPPED
[INFO] arthas-testcase .................................... SKIPPED
[INFO] web-console ........................................ SKIPPED
[INFO] arthas-tunnel-server ............................... SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.064 s
[INFO] Finished at: 2022-07-03T01:20:56+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Unknown lifecycle phase "web-console". You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: validate, initialize, generate-sources, process-sources, generate-resources, process-resources, compile, process-classes, generate-test-sources, process-test-sources, generate-test-resources, process-test-resources, test-compile, process-test-classes, test, prepare-package, package, pre-integration-test, integration-test, post-integration-test, verify, install, deploy, pre-clean, clean, post-clean, pre-site, site, post-site, site-deploy. -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/LifecyclePhaseNotFoundException
