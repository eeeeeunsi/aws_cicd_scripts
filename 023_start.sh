docker container run \
    -d \
    --name=guestbookapp \
    --network=host \
    -e CONTEXT_PATH=/ \
    -e MYSQL_IP=172.31.0.100 \
    -e MYSQL_PORT=3306 \
    -e MYSQL_DATABASE=guestbook \
    -e MYSQL_USER=root \
    -e MYSQL_PASSWORD=education \
    685695804727.dkr.ecr.ap-northeast-2.amazonaws.com/guestbook:[TAG]

# 685695804727.dkr.ecr.ap-northeast-2.amazonaws.com/guestbook:20230317111518_11