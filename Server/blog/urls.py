from django.urls import path
from . import views

urlpatterns = [
    path('', views.home,name='blog-home'), #url path
    path('about/', views.about,name='blog-about'), #url path
]