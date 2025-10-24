FROM python:3.8-slim-buster

LABEL MAINTAINER="Marina Weber <marina.weber@tum.de>"

ENV VENV=/opt/venv
RUN python3 -m venv $VENV
ENV PATH="$VENV/bin:$PATH"

ENV GROUP_ID=1000 \
    USER_ID=1000

WORKDIR /var/www/

ADD . /var/www/
ADD ./wsgi.py /var/www/wsgi.py
ADD . /var/www/app
ADD ./wsgi.py /var/www/app/wsgi.py

RUN pip install --upgrade pip
RUN pip install virtualenv
ADD ./requirements.txt /var/www/requirements.txt
RUN pip install -r requirements.txt

RUN apt update
RUN apt-get install libmetis5

# this works on the server but not i the container
# RUN apt install -y git
# RUN git clone https://github.com/networkx/networkx-metis.git
# RUN python3 networkx-metis/setup.py install

RUN addgroup -gid $GROUP_ID www
RUN adduser www -uid $USER_ID -ingroup www -shell /bin/sh

USER www

EXPOSE 5000


CMD [ "gunicorn", "-w", "1", "--bind", "0.0.0.0:5000", "wsgi"]