#mvn clean package
docker stop catsbot
docker rm catsbot
docker build -t catsbot .
docker run --name catsbot --network=nelf-network -p 127.0.0.1:7776:8080 catsbot