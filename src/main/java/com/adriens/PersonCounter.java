package com.adriens;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.imageio.IIOException;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.output.DetectedObjects;

import ai.djl.modality.cv.util.BufferedImageUtils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

public class PersonCounter {
    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        Path path = FileSystems.getDefault().getPath("scan","img.jpg");
        System.out.println(path);
        //String url = "https://github.com/awslabs/djl/raw/master/examples/src/test/resources/dog_bike_car.jpg";
        BufferedImage img = null;
        try {
            img = BufferedImageUtils.fromFile(path);
        } catch(IIOException e){
            e.printStackTrace();
            System.out.println("Can't find <img.jpg> input file! Are you sure it's in the 'scan' folder?");
            System.exit(404);
        }
        // Define a criteria to search a model that matches user's need
        Criteria<BufferedImage, DetectedObjects> criteria =
        Criteria.builder()
                .setTypes(BufferedImage.class, DetectedObjects.class)

                // search for an object detection model
                .optApplication(Application.CV.OBJECT_DETECTION)
                .build();

        try (ZooModel<BufferedImage, DetectedObjects> model = ModelZoo.loadModel(criteria)) {
            try (Predictor<BufferedImage, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);
                System.out.println(detection);
            }
        }
    }
}
