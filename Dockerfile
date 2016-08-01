FROM smartcosmos/service
MAINTAINER SMART COSMOS Platform Core Team

ADD target/smartcosmos-*.jar  /opt/smartcosmos/smartcosmos-gateway.jar

CMD ["/opt/smartcosmos/smartcosmos-gateway.jar"]
