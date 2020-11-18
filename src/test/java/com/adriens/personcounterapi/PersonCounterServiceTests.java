package com.adriens.personcounterapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.tika.exception.TikaException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.SAXException;

import ai.djl.ModelException;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PersonCounterServiceTests {

    @Autowired
    private PersonCounterService service;

    private Setup setup = new Setup();
    private final Logger log = LoggerFactory.getLogger(PersonCounterController.class);

    @Test
    public void detectTest() throws IOException, ModelException, TranslateException {
        log.info("Started tests on function detect");

        // TESTS ON ATM IMAGE
        Image img = ImageFactory.getInstance()
                .fromInputStream(getClass().getClassLoader().getResourceAsStream("dab1.jpg"));
        DetectedObjects objects = setup.getPredictor().predict(img);

        int numberOfDetectedPersonsAbove80Percent = 0;
        int numberOfDetectedPersonsAbove90Percent = 0;
        for (Classification object : objects.items()) {
            if (object.getProbability() >= 0.8) {
                numberOfDetectedPersonsAbove80Percent++;
            }
            if (object.getProbability() >= 0.9) {
                numberOfDetectedPersonsAbove90Percent++;
            }
        }

        assertEquals(7, numberOfDetectedPersonsAbove80Percent,
                "Wrong number of persons detected above 80% probability on the first image");
        assertEquals(6, numberOfDetectedPersonsAbove90Percent,
                "Wrong number of persons detected above 90% probability on the first image");
        log.info("Tests on function detect were successfull");
    }

    @Test
    public void getMetaDatasTest() throws IOException, SAXException, TikaException {
        FileInputStream fis = new FileInputStream(new File(getClass().getClassLoader().getResource("dab1.jpg").getFile()));
        HashMap<String, String> map = service
                .metadatas(fis);
        assert map.size() == 100;
        assert map.get("Content-Type").equals("image/jpeg");
    }
}
