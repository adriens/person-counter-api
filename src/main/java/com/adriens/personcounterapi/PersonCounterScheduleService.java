package com.adriens.personcounterapi;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PersonCounterScheduleService {
    private static final Logger log = LoggerFactory.getLogger(PersonCounterScheduleService.class);

    private final int daysLimit;

    @Autowired
    public PersonCounterScheduleService(@Value("${daysLimit}") int daysLimit){
        this.daysLimit = daysLimit;
    }

    @Scheduled(cron = "0 0 9 * * *") // Everyday at 9 o-clock (0 0 9 * * *)
    public void deleteOldPhotos(){
        log.info("Deleting old photos..");

        File folder = new File("input/");
        if (folder.exists()) {
            File[] listFiles = folder.listFiles();
            long eligibleForDeletion = System.currentTimeMillis() - (this.daysLimit * 24 * 60 * 60 * 1000);
            for (File file: listFiles) {
                if (file.isFile() && file.lastModified() < eligibleForDeletion) {
                    log.info("Deleting " + file.getName());
                    if (!file.delete()) {
                       log.info("(!) Couldn't delete "+ file.getName() +".");
                    }
                }
            }
        }
    }
}
