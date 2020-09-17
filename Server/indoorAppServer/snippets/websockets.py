from autobahn.twisted import sleep
from autobahn.twisted.wamp import ApplicationSession
import requests

from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver
from twisted.internet.defer import inlineCallbacks
from autobahn.twisted.wamp import ApplicationSession, ApplicationRunner
from os import environ
import sys
import os


def publish(username,position):
    payload = {'username': username,'position':position}
    requests.post("http://127.0.0.1:8080/notify",
                  json={
                      'topic': 'onLocationUpdate',
                      'args': [payload]
                  })