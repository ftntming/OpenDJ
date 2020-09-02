FROM openidentityplatform/opendj

WORKDIR /opt

ADD bootstrap/ /opt/opendj/bootstrap/

ADD run.sh /opt/opendj/run.sh

# RUN chmod +x /opt/opendj/run.sh \
#      /opt/opendj/bootstrap/setup.sh \
#      /opt/opendj/bootstrap/replicate.sh

ENTRYPOINT ["/opt/opendj/run.sh"]
