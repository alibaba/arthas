echo "设置环境..."

mkdir -p /home/local/java/lite-jdk-linux-x86_64
cd /home/local/java/lite-jdk-linux-x86_64
wget https://github.com/hengyunabc/lite-jdk/releases/download/0.0.5/lite-jdk-11-linux-x86_64.tgz
tar -zxvf lite-jdk-11-linux-x86_64.tgz > /dev/null 2>&1
echo 'export JAVA_HOME=/home/local/java/lite-jdk-linux-x86_64
export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
cd ~

clear
