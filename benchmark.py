import os
import csv
import requests
import time
from PIL import Image

INPUT = input("\nEntrez le nom de l'image originale (avec extension)\n/!\\ L'image doit se trouver dans le répertoire input\n>> ") # IMAGE ORIGINALE
INPUT_PATH = "input/"+INPUT # CHEMIN VERS IMAGE

step = 20
FIXED_QUALITY = 100
NUMBER_OF_PERSON = int(input("\nCombien il y a t-il de personnes sur la photo?\n>> ")) # NOMBRE DE PERSONNES SUR LA PHOTO

parameters = {
    "confidence" : 50,
    "class" : "person",
    "alias" : "benchmark"
}


with open("data/benchmark.csv", mode="w") as csv_file:
    csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
    csv_writer.writerow(['filename', 'filesize-bytes', 'resolution', 'confidenceThreshold', 'nbDetectedPersons', 'realNumberOfPerson', 'minConfidence', 'maxConfidence', 'processTimeMs'])
    
    img = Image.open(INPUT_PATH)
    i = 0
    while i < 100:
        QUALITY = FIXED_QUALITY - i
        if(QUALITY == 20):
            i += 9
            step = 1
        i = i + step

        OUTPUT = "benchmark_"+str(QUALITY)+".jpg" # IMAGE POST-TRAITEMENT
        OUTPUT_PATH = "input/"+OUTPUT # CHEMIN VERS IMAGE

        print("Image " + OUTPUT + " created")

        img.save(OUTPUT_PATH, quality=QUALITY, subsampling=0)

        start_time = time.time()
        detect = requests.get("http://127.0.0.1:8080/photos/"+OUTPUT+"/detect", params=parameters)
        execution_time = int((time.time()-start_time)*1000)

        detect_data = detect.json()["image"]
        detect_count = detect.json()["count"]

        bytes = os.stat(OUTPUT_PATH).st_size
        resolution = QUALITY

        row = [ 
                INPUT, 
                bytes,
                resolution,
                parameters["confidence"],
                detect_count,
                NUMBER_OF_PERSON,
            ]

        if(len(detect_data) > 0):
            minConfidence = dict(detect_data[0])["probability"]
            maxConfidence = dict(detect_data[0])["probability"]

            for j in range(0, len(detect_data)):
                jsonDict = dict(detect_data[j])
                if(jsonDict["probability"] > maxConfidence):
                    maxConfidence = jsonDict["probability"]
                
                if(jsonDict["probability"] < minConfidence):
                    minConfidence = jsonDict["probability"]
        else:
            minConfidence = 0
            maxConfidence = 0

        row.append(minConfidence)
        row.append(maxConfidence)
        row.append(execution_time)
        csv_writer.writerow(row)


    
    