# Pull base image.
FROM openjdk:8-jre-alpine

# Define working directory.
RUN if [ ! -e "/opt/omnition/topologies" ]; then   mkdir -p "/opt/omnition/topologies"; fi;
COPY target/SyntheticLoadGenerator-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/omnition/synthetic-load-generator.jar
COPY topologies/* /opt/omnition/topologies/
COPY start.sh /opt/omnition/
RUN chmod +x /opt/omnition/start.sh
WORKDIR /opt/omnition/

# Define default command
CMD ["./start.sh"]

