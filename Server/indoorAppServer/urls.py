from django.urls import path, include
from . import views
from rest_framework import routers

router = routers.DefaultRouter()
router.register('fingerprints',views.FingerprintView)
router.register('device',views.DeviceView)
router.register('wifi',views.WifiView)
router.register('bluetooth',views.BluetoothView)

urlpatterns = [
    path('',include(router.urls))
]