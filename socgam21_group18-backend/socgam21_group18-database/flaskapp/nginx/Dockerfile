FROM alpine:latest

LABEL MAINTAINER="Marina Weber <marina.weber@tum.de>"

RUN apk --update add nginx && \
    ln -sf /dev/stdout /var/log/nginx/access.log && \
    ln -sf /dev/stderr /var/log/nginx/error.log && \
    mkdir /etc/nginx/sites-enabled/ && \
    mkdir -p /run/nginx && \
    rm -rf /etc/nginx/conf.d/default.conf && \
    rm -rf /var/cache/apk/*

COPY conf.d/app.conf /etc/nginx/conf.d/app.conf
COPY http.d/default.conf /etc/nginx/http.d/default.conf

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]