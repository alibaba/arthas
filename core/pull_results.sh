#!/bin/bash  
  
sum=0
i=1  
while(( i <= 100 ))  
do  
   let "i += 1"     
   curl -XPOST http://localhost:8563/api -d @<(cat << EOF
{
"action": "pull_results",
"sessionId":"$1",
"consumerId": "$2"
}
EOF)

echo ""
 
done  
  
echo "count=$i"
