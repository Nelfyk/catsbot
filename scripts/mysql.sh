docker stop nelf_mysql
docker rm nelf_mysql
docker network create nelf-network
docker run --name nelf_mysql --network=nelf-network -p 7777:3306 \
-e MYSQL_ROOT_PASSWORD=bestuser \
-e MYSQL_USER=bestuser3 \
-e MYSQL_PASSWORD=bestuser3 \
-e MYSQL_DATABASE=cats_bot \
-d mysql:latest