import requests
import os


def publish(username,position):
    print('Publishing new message')
    payload = {'username': username,'position':position}
    print('HERE WE ARE ')
    response = requests.post("https://indoorlocationapp.herokuapp.com/notify",
                  json={
                      'topic': 'onLocationUpdate',
                      'args': [payload]
                  })
    print(response.elapsed.total_seconds())