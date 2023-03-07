## Команды для докера:

1. docker network create nelf-network
2. docker run --name nelf_mysql --network=nelf-network -p 127.0.0.1:7777:3306 \
   -e MYSQL_ROOT_PASSWORD=bestuser3 \
   -e MYSQL_USER=bestuser3 \
   -e MYSQL_PASSWORD=bestuser3 \
   -e MYSQL_DATABASE=cats_bot \
   -d mysql:latest
3. docker run --name nelf_catsbot --network=nelf-network -p 127.0.0.1:7774:8080 nelfy/catsbot:1
