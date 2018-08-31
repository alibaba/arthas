dashboard -n 1
sysprop
watch arthas.Test test "@com.alibaba.arthas.Test@n.entrySet().iterator.{? #this.key.name()=='STOP' }" -n 2
