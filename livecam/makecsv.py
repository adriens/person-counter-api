#!/usr/bin/python3
# -*- coding: UTF-8 -*-

import csv
import os
import sys
from datetime import datetime

def getCurrentTime():
    now = datetime.now()
    return now.strftime("%Y-%m-%d %H:%M:%S")

path = sys.argv[1]
filename = sys.argv[2]
numberPersons = sys.argv[3]

headers = ['date', 'numberPersons', 'path', 'filename']

if not os.path.exists('livecam.csv'):
    os.mknod('livecam.csv')
    
file = open("livecam.csv", "r+")
if len(file.readlines()) == 0:
    file.write("date,numberPersons,path,filename\n")
elif len(file.readlines()) > 0:
    if file.readlines()[0] != "date,numberPersons,path,filename\n" :
        file.write("date,numberPersons,path,filename\n")
file.close()

with open("livecam.csv", mode="a+", newline='') as csv_file:
    csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
    csv_reader = csv.reader(csv_file)
    csv_writer.writerow([getCurrentTime(), numberPersons, path, filename])
    csv_file.close()