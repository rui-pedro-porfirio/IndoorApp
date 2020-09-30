import requests
import os

heroku_url = 'https://indoorlocationapp.herokuapp.com/notify'
local_url = 'http://127.0.0.1:8080/notify'

def publish(username,uuid,position):
    print('Publishing new message to topic onLocationUpdate.')
    payload = {'username': username,'UUID':uuid,'position':position}
    response = requests.post(heroku_url,
                  json={
                      'topic': 'onLocationUpdate',
                      'args': [payload]
                  })
    print('Response: '+ str(response.status_code)+' | Time: ' + str(response.elapsed.total_seconds()))