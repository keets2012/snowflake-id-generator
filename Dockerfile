FROM 192.168.1.202/library/basejava
VOLUME /tmp
ADD ./target/snowflake-id-generate-1.0-SNAPSHOT.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]