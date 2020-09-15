from os import environ

from twisted.internet import reactor
from twisted.internet.defer import inlineCallbacks
from autobahn.twisted.wamp import Application
from autobahn.wamp.types import SubscribeOptions
from autobahn.twisted.wamp import ApplicationSession, ApplicationRunner
import requests

# We create the WAMP client.
app = Application('monitoring')

# This is my machine's public IP since
# this client must be able to reach my server
# from the outside. You should change this value
# to the IP of the machine you put Crossbar.io
# and Django.
SERVER = '127.0.0.1'


@app.signal('onjoined')
def called_on_joined():
    """ Loop sending the state of this machine using WAMP every x seconds.
        This function is executed when the client joins the router, which
        means it's connected and authenticated, ready to send WAMP messages.
    """
    print("Connected")



# We subscribe to the "clientconfig" WAMP event.
@app.subscribe('onLocationUpdate')
def update_configuration(args):
    """ Update the client configuration when Django asks for it. """
    print("ARRIVED HERE WITH ARGS: " + str(args))


# We start our client.
if __name__ == '__main__':
    app.run(url=u"ws://%s:8080/ws" % SERVER)
    # app.run(url='ws://{}:8080/ws'.format(SERVER))