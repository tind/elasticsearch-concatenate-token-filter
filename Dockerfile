FROM maven:openjdk

WORKDIR /usr/src/elasticsearch-concatenate-token-filter
COPY ./ ./

RUN mvn clean install
