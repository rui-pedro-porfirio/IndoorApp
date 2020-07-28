import json
import math
import numpy as np
from IPython.core.display import display
import csv
import pandas as pd

csv_columns = ['X','Y']


def load_access_points_locations():
    with open('../../access_points_location.json') as json_file:
        data = json.load(json_file)
        access_points = {}
        for k, v in data.items():
            print('KEY: ' + k)
            access_points[k] = v
            print('X: ', v['x'])
            print('Y: ', v['y'])
            print('')
        return access_points


def compute_distance_coordinate_system(x1,y1,x2,y2):
    dist = math.hypot(x2 - x1, y2 - y1)
    return dist


def compute_trilateration(x,y,access_points):
    locations = []
    distances = {}
    for k, v in access_points.items():
        locations.append((v['x'],v['y']))
    for i in range(len(locations)):
        aps = locations[i]
        print("APS")
        print(aps)
        distance_calculated = compute_distance_coordinate_system(x,y,aps[0],aps[1])
        access_list = list(access_points.items())
        distances[access_list[i][0]] = distance_calculated
        print('DISTANCE FOR POINT: x: ' + str(x) + ", y: " + str(y) + " is " + str(distance_calculated))
    print("DISTANCES DICT")
    display(distances)
    return distances


room_limit_x_min = -2.0
room_limit_x_max = 3.0
room_limit_y_min = -1.0
room_limit_y_max = 4.0
access_points = load_access_points_locations()
display(access_points)
distances_list = list()
for i in np.arange(room_limit_x_min,room_limit_x_max,1.0):
    for j in np.arange(room_limit_y_min,room_limit_y_max,1.0):
        distances = {}
        distances[(i,j)] = compute_trilateration(x=i,y=j,access_points=access_points)
        distances_list.append(distances)
print("DONE")
display(distances_list)
dataframe_list = list()
for k, v in access_points.items():
    csv_columns.append(k)
csv_columns.append('Zone')
for d in distances_list:
    print(d)
    for k, v in d.items():
        x = k[0]
        y = k[1]
        final_list = list()
        final_list.append(x)
        final_list.append(y)
        for k,v in v.items():
            final_list.append(v)
        if y >= 3.0:
            final_list.append('Personal')
        elif y >= 0.0 and y < 3.0:
            final_list.append('Social')
        else:
            final_list.append('Public')
        dataframe_list.append(final_list)
display(dataframe_list)
dataframe = pd.DataFrame(data=dataframe_list,columns=csv_columns)
display(dataframe)
dataframe.to_csv(r'.\trilateration_test_classification.csv', index = False)