#!/bin/sh

while true;
do
    for file in `ls CAM01/*`
    do
        echo "Copied $file"
        `cp $file ../input/BUREAU_GLIA.jpg`
        sleep 5s
    done
done