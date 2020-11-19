#!/bin/bash

while true;
do
    for file in `ls CAM02/* | sort -R`
    do
        echo `date "+[%H:%M:%S] Copied $file"`
        `cp $file ../input/BUREAU_GLIA.jpg`

        WAITING_TIME=$(( RANDOM % 60 + 40))
        echo "Sleeping time: $WAITING_TIME seconds" 
        sleep $WAITING_TIME
    done
done