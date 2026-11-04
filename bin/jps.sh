#!/bin/sh

# jps.sh version 1.0.2

# there might be multiple java processes, e.g. log-agent
JPS_CMDS=($(ps aux | grep java | grep -v 'grep java' | awk '{print $11}' | sed -n 's/java$/jps/p'))

# find the first executable jps command
JPS_CMD=""
for jps in ${JPS_CMDS[@]}; do
  if [ -x $jps ]; then
     JPS_CMD=$jps
     break
  fi
done

if [ "$JPS_CMD" == "" ]; then
    echo "No Java Process Found on this Machine."
    exit 1
else
    result=`$JPS_CMD -lmv | grep -v jps`
    if [ "$result" == "" ]; then
        ps aux | grep -E '^admin.*java.*' | grep -v grep | awk 'BEGIN{ORS=""}{print $2" ";for(j=NF;j>=12;j--){if(match($j, /^\-[a-zA-Z0-9]/)) {break;} } for(i=j+1;i<=NF;i++) {print $i" "} for(i=12;i<=j;i++) {print $i" "} print "\n" }'
    else
        echo "$result"
    fi
fi
