FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp
ADD *.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENV APP_OPTS="--spring.profiles.active=prod"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /app.jar $APP_OPTS" ]
