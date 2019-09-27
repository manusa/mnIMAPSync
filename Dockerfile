FROM azul/zulu-openjdk-alpine:11-jre

MAINTAINER Marc Nuri <marc@marcnuri.com>
LABEL MAINTAINER="Marc Nuri <marc@marcnuri.com>"


COPY ./build/libs /opt

ENTRYPOINT ["java", "-jar", "/opt/mnimapsync-all.jar"]
