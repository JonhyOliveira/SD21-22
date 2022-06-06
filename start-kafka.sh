#!/bin/bash

if [ "$#" -ne 1 ]; then 
echo "usage: start-kafka.sh localhost or start-kafka.sh kafka"
exit 1
fi

[ ! "$(docker network ls | grep sdnet )" ] && \
	docker network create --driver=bridge sdnet

docker pull smduarte/sd2122-kafka

echo "Launching Kafka Server: "  $1

docker rm -f kafka

docker run -h $1  \
           --name=kafka \
	   --network=sdnet \
	   --net-alias zookeeper \
           --rm -t -p 9092:9092 -p 2181:2181 smduarte/sd2122-kafka
