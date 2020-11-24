#!/usr/bin/python3
# -*- coding: UTF-8 -*-

import imaplib
import base64
import os
import email
import time
import traceback
import requests
import logging
from datetime import datetime

logging.basicConfig(filename='livecam.log', level=logging.DEBUG)

confs = ["NUMBER_OF_EMAILS", "PAUSE", "DAYS_BEFORE_DELETION", "PATH_FOR_PICTURES", "API_URL", "ALIAS", "CLASS", "CONFIDENCE", "HOOK", "CSV_SCRIPT"]
infos = ["mail", "password", "server", "port"]

def getCurrentTime():
    now = datetime.now()
    return now.strftime("%d/%m/%Y %H:%M:%S")

def log(message):
    logging.info(" [" + getCurrentTime() + "] " + message)

def warn(message):
    logging.warning(" [" + getCurrentTime() + "] " + message)

def error(message):
    logging.error(" [" + getCurrentTime() + "] " + message)
    exit(1)

log("Retrieving configuration..")
if not os.path.exists('livecam.conf'):
        os.mknod('livecam.conf')
file = open("livecam.conf", "r")   

conf = ""
for lines in file.readlines():
    conf += lines
for item in confs:
    conf = conf.replace(item+":", "")
conf = conf.split('\n')

conf[0] = int(conf[0]) # NUMBER OF EMAILS
conf[1] = int(conf[1]) # PAUSE TIME
conf[2] = int(conf[2]) # DAYS BEFORE DELETION

# Retrieve configuration parameters
if(len(conf) != len(confs)):
    error("Invalid configuration")
else:
    for i in range(0, len(conf)):
        if len(str(conf[i])) == 0:
            error("Invalid configuration: " + confs[i])

log("Got configuration")

# Retrieve credentials
log("Retrieving credentials..")
if not os.path.exists('.auth'):
        os.mknod('.auth')
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

# Error handling
if(len(auth) != len(infos)):
    error("Invalid credentials")
else:
    for i in range(0, len(auth)):
        if len(auth[i]) == 0:
            error("Invalid credentials: " + infos[i])
file.close()

mails = []
while True: # Continuous execution
    try:
        mail = imaplib.IMAP4_SSL(auth[2], auth[3]) # Create a new connection with the IMAP server
        log("Got credentials, connecting to mail server..")
        mail.login(auth[0], auth[1]) # Login to the IMAP server
        log("Successfully connected to the server!")
        mail.select("Sent") # Select the 'sent' mailbox

        log("Retrieving last sent mail..")
        type, data = mail.search(None, 'ALL') # Retrieve all mails
        mail_ids = data[0]
        id_list = mail_ids.split()

        data_split = data[0].split()
        if(len(data_split) > int(conf[0])):
            data_split = data_split[-int(conf[0]):] # Get only the n last mails (n being the number of mails to retrieve in the conf file)

        count = 0
        total = len(data_split)
        for num in data_split:
            if num in mails: # Allows to skip already read mails
                continue
            else:
                mails.append(num)
            typ, data = mail.fetch(num, '(RFC822)')
            
            raw_email = data[0][1] # Converts byte literal to string, removing 'b' prefix
            raw_email_string = raw_email.decode('utf-8')
            email_message = email.message_from_string(raw_email_string)
            for part in email_message.walk():
                if part.get_content_maintype() == 'multipart':
                    continue
                if part.get('Content-Disposition') is None:
                    continue
                fileName = part.get_filename()        
                if bool(fileName):
                    filePath = os.path.join(conf[3], fileName)
                    if not os.path.isfile(filePath): # If the file doesn't exists in the path
                        if not os.path.isdir(conf[3]):
                            os.mkdir(conf[3])
                        fp = open(filePath, 'wb')
                        fp.write(part.get_payload(decode=True)) # Download the file
                        fp.close()            
                        log('Downloaded "{file}" from email.'.format(file=fileName))
            count += 1
            log('Read {count} out of {total} emails'.format(count=count, total=total))
        

        if not os.path.exists('.filetrack'):
            os.mknod('.filetrack')
        filetrack = open('.filetrack', 'r+')
        files_done = filetrack.readlines()
        files_done = list(map(lambda s: s.strip(), files_done))
        for filename in os.listdir(conf[3]):
            if filename.endswith(".jpg") or filename.endswith(".png"): # If the file is a picture
                if(time.time() - os.path.getmtime(conf[3] + filename) > conf[2] * 24 * 3600):
                    os.remove(conf[3] + filename)
                    log('Deleted old picture ' + filename)
                elif(filename not in files_done): # Allows to skip already analysed pictures
                    log('Running analysis on picture ' + filename + '...')
                    picture = open(conf[3] + filename, 'rb')
                    request = requests.post(conf[4]+'/photos/raw?class='+conf[6]+'&confidence='+conf[7]+'&alias='+conf[5], 
                                            data=picture.read()) # Call the API on the picture downloaded
                    picture.close()
                    count = len(request.json()) # Retrieve the Json of the response, its length = the number of persons
                    log("Number of persons detected: " + str(count))
                    os.system("./" + conf[9] + " " + conf[3] + " " + filename + " " + str(count)) # Call the makecsv.py script
                    if(count > 0): # If persons are detected
                        os.system("./" + conf[8] + " " + conf[3]+filename + " " + str(count)) # Call the hook
                    filetrack.write(filename+'\n') # Add the file to the list of file already analysed
                    files_done.append(filename)
        log('Finished analysing new pictures.')
        filetrack.close()
        mail.logout(); # Log out of the IMAP server
        log('Disconnected from mail server, reconnecting in ' + str(conf[1]) + ' seconds.')
    except: # If an error occured, write it to the log and prevent crash
        e = traceback.format_exc()
        warn(e)
        warn("Retrying in " + str(conf[1]) + " seconds.")

    time.sleep(conf[1])