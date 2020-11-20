#!/usr/bin/python3
# -*- coding: UTF-8 -*-

import imaplib
import base64
import os
import email
import time
import requests
from datetime import datetime

PAUSE = 60 # Time between script execution (SECONDS)
DAYS_BEFORE_DELETION = 14 # Number of days before old photos are deleted
PATH_FOR_PICTURES = "pictures/" # Absolute path where the pictures will be stored
API_URL = "http://127.0.0.1:8080"
ALIAS = "RPI" # Alias given to object detected
CLASS = "person" # Filter for class of objects
CONFIDENCE = 80 # Minimum confidence needed to be counted as a detected object
HOOK = "hook.py" # Python script to execute in case of detected person

infos = ["mail", "password", "server", "port"]

def getCurrentTime():
    now = datetime.now()
    return now.strftime("%H:%M:%S")

def log(message):
    print("["+getCurrentTime()+'] INFO: ' + message)

def error(message):
    print("["+getCurrentTime()+'] ERROR: ' + message)
    exit(1)

log("Retrieving credentials..")
if not os.path.exists('.filetrack'):
        os.mknod('.filetrack')
file = open(".auth", "r")

auth = ""
for lines in file.readlines():
    auth += lines
for info in infos:
    auth = auth.replace(info+":", "")
auth = auth.split('\n')
# auth[0] : username
# auth[1] : password
# auth[2] : server / host
# auth[3] : port

if(len(auth) != len(infos)):
    error("Invalid credentials")
elif(len(auth[0]) == 0):
    error("Invalid e-mail")
elif(len(auth[1]) == 0):
    error("Invalid password")
file.close()
log("Got credentials, connecting to mail server..")
mail = imaplib.IMAP4_SSL(auth[2], auth[3])
mail.login(auth[0], auth[1])
log("Successfully connected to the server!")
mail.select("Sent")

while True:
    log("Retrieving last sent mail..")
    type, data = mail.search(None, 'ALL')
    mail_ids = data[0]
    id_list = mail_ids.split()

    for num in data[0].split():
        typ, data = mail.fetch(num, '(RFC822)' )
        raw_email = data[0][1]# converts byte literal to string removing 'b'
        raw_email_string = raw_email.decode('utf-8')
        email_message = email.message_from_string(raw_email_string)
        for part in email_message.walk():
            if part.get_content_maintype() == 'multipart':
                continue
            if part.get('Content-Disposition') is None:
                continue
            fileName = part.get_filename()        
            if bool(fileName):
                filePath = os.path.join(PATH_FOR_PICTURES, fileName)
                if not os.path.isfile(filePath) :
                    fp = open(filePath, 'wb')
                    fp.write(part.get_payload(decode=True))
                    fp.close()            
                    log('Downloaded "{file}" from email.'.format(file=fileName))

    if not os.path.exists('.filetrack'):
        os.mknod('.filetrack')
    filetrack = open('.filetrack', 'r+')
    files_done = filetrack.readlines()
    files_done = list(map(lambda s: s.strip(), files_done))
    for filename in os.listdir(PATH_FOR_PICTURES):
        if filename.endswith(".jpg") or filename.endswith(".png"):
            if(time.time() - os.path.getmtime(PATH_FOR_PICTURES + filename) > DAYS_BEFORE_DELETION * 24 * 3600):
                os.remove(PATH_FOR_PICTURES + filename)
                log('Deleted old picture ' + filename)
            elif(filename not in files_done):
                log('Running analysis on picture ' + filename + '...')
                picture = open(PATH_FOR_PICTURES + filename, 'rb')
                request = requests.post(API_URL+'/photos/raw?class='+CLASS+'&confidence='+str(CONFIDENCE)+'&alias='+ALIAS, 
                                        data=picture.read())
                picture.close()
                count = len(request.json())
                log("Number of persons detected: " + str(count))
                if(count > 0):
                    os.system("./" + HOOK + " " + PATH_FOR_PICTURES+filename + " " + str(count))
                filetrack.write(filename+'\n')
                files_done.append(filename)
    filetrack.close()
    time.sleep(PAUSE)