from django.urls import path, include
from . import views
from rest_framework import routers
from rest_framework.urlpatterns import format_suffix_patterns

router = routers.DefaultRouter()
router.register('fingerprints',views.FingerprintView)
router.register('device',views.DeviceView)
router.register('wifi',views.WifiView)
router.register('bluetooth',views.BluetoothView)
router.register('users',views.UserView)

urlpatterns = [
    path('',include(router.urls)),
    path('filter/',views.FilterView.as_view()),
    path('radiomap/position',views.PositioningAlgorithmsView.as_view()),
    path('proximity/position', views.ProximityAlgorithmsView.as_view()),
    path('trilateration/position', views.TrilaterationHandlerView.as_view()),
    path('proximity/distance',views.ProximityDistanceView.as_view()),
    path('scanning/',views.ScanningView.as_view())
]
