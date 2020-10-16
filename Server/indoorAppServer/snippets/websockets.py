import requests
import os

heroku_url = 'https://indoorlocationapp.herokuapp.com/notify'
local_url = 'http://127.0.0.1:8080/notify'


def publish(username, uuid, position,orientation, radio_map=None, beacon=None):
    payload = {'username': username, 'UUID': uuid, 'position': position,'orientation':orientation}
    if beacon is not None:
        payload['beacon'] = beacon
    if radio_map is not None:
        payload['radio_map'] = radio_map
    print('Publishing new message to topic onLocationUpdate. DATA: ' + str(payload))
    response = requests.post(heroku_url,
                             json={
                                 'topic': 'onLocationUpdate',
                                 'args': [payload]
                             })
    print('Response: ' + str(response.status_code) + ' | Time: ' + str(response.elapsed.total_seconds()))
