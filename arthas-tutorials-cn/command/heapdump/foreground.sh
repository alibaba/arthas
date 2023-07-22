echo "设置环境..."
docker create -i -p80:80 -p8563:8563 -p3658:3658 --name arthas openjdk:11 bash
docker start arthas
echo docker exec -it arthas bash >> ~/.bashrc
clear
docker exec -it arthas bash
