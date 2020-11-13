#!/bin/sh

WAITING_TIME=5

while true;
do
    for file in `ls CAM01/*`
    do
        echo "Copied $file"
        `cp $file ../input/BUREAU_GLIA.jpg`
        sleep $WAITING_TIME
    done
done