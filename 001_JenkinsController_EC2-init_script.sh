#!/bin/bash

yum install -y java-11-amazon-corretto.x86_64

yum install -y docker
systemctl enable docker
systemctl start docker
systemctl status docker
usermod -aG docker ec2-user

docker container run -d \
    --name=mysqldb \
    --restart=always \
    -e MYSQL_ROOT_PASSWORD=education \
    -e MYSQL_DATABASE=guestbook \
    -p 3306:3306 \
    yu3papa/mysql_hangul:2.0

