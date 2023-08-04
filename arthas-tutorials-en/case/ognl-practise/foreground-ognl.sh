echo "Setup environment..."

mkdir -p /home/local/java
cd /home/local/java
wget https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz
tar -zxvf openjdk-11.0.1_linux-x64_bin.tar.gz
echo 'export JAVA_HOME=/home/local/java/jdk-11.0.1
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar' >> ~/.bashrc
source ~/.bashrc
mkdir -p example
cd example/
git clone https://github.com/hengyunabc/ognl-demo.git
cd ognl-demo

clear
