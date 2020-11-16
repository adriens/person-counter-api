#!/bin/sh

WAITING_TIME=60

while true;
do
    for file in `ls CAM01/* | sort -R`
    do
        echo `date "+[%H:%M:%S] Copied $file"`
        `cp $file ../input/BUREAU_GLIA.jpg`
        sleep $WAITING_TIME
    done
done