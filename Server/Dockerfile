FROM heroku/miniconda:3

#Update Image
RUN apt-get update && apt-get -y install gcc
RUN conda update conda
RUN conda install -c anaconda python=3.7.4
RUN python -V

#Install required packages and update existing
RUN conda install python-snappy
RUN conda install setuptools
RUN conda install psycopg2
RUN pip install --upgrade pip
RUN pip install setuptools --upgrade
RUN pip install --upgrade wheel
RUN conda install psutil
RUN conda install pycparser
RUN conda install setproctitle
RUN conda install lmdb

# Grab requirements.txt.
ADD ./requirements.txt /tmp/requirements.txt

# Install dependencies
RUN pip install -r /tmp/requirements.txt --no-cache-dir

# Add code
ADD . /opt/indoorlocationapp/
WORKDIR /opt/indoorlocationapp

# Run app
CMD gunicorn --bind 0.0.0.0:$PORT Server.wsgi

#If doens't work this way, try launching with procfile and git

