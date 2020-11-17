#!/bin/sh


while true;
do
    WAITING_TIME=$(( ( RANDOM % 60 ) + 40 ))
    for file in `ls CAM01/* | sort -R`
    do
        echo `date "+[%H:%M:%S] Copied $file"`
        `cp $file ../input/BUREAU_GLIA.jpg`
        sleep $WAITING_TIME
    done
done