package com.adriens;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.modality.cv.util.BufferedImageUtils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

public class PersonCounter {
    public static String DetectedObjectsToString(DetectedObjects detection, BufferedImage img){
        String jsonString = "[";

        int count = 0;

        HashMap<String, HashMap> bounds = new HashMap<>();
        for(int i = 0; i < detection.getNumberOfObjects(); i++){
            DetectedObject detected = detection.item(i); 
            if(detected.getProbability() < 0.4){
                continue;
            }           
            count++;

            BoundingBox box = detected.getBoundingBox();
            HashMap<String, Integer> infos = new HashMap<>();
            
            String x =  Double.toString(img.getWidth() * box.getBounds().getX());
            String y =  Double.toString(img.getHeight() * box.getBounds().getY());
            String width =  Double.toString(img.getWidth() * box.getBounds().getWidth());
            String height =  Double.toString(img.getHeight() * box.getBounds().getHeight());

            infos.put("x", (int) Math.round(Double.valueOf(x)));
            infos.put("y", (int) Math.round(Double.valueOf(y)));
            infos.put("width", (int) Math.round(Double.valueOf(width)));
            infos.put("height", (int) Math.round(Double.valueOf(height)));
            bounds.put(detected.getClassName() + " " + i, infos);

            jsonString += "\t{\n";
            jsonString += "\t\"label\": \""+detected.getClassName()+"\",\n";
            jsonString += "\t\"confidence\": \""+detected.getProbability()+"\",\n";
            jsonString += "\t\"x\": \""+x+"\",\n";
            jsonString += "\t\"y\": \""+y+"\",\n";
            jsonString += "\t\"width\": \""+width+"\",\n";
            jsonString += "\t\"height\": \""+height+"\",\n";
            jsonString += "\t}";
            if(i != detection.getNumberOfObjects()-1){
                jsonString += ",";
            }
            jsonString += "\n";
        }
        jsonString += "\"count\" : \""+ count +"\"";
        jsonString += "]";

        saveBoundingBoxImage(img, bounds);
        return jsonString;
    }

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
                .optFilter("backbone", "resnet50")
                // search for an object detection model
                .optApplication(Application.CV.OBJECT_DETECTION)
                .build();

        
        try (ZooModel<BufferedImage, DetectedObjects> model = ModelZoo.loadModel(criteria)) {
            try (Predictor<BufferedImage, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);

                String jsonString = DetectedObjectsToString(detection, img);
                System.out.println(jsonString);
            }
        }
    }

    /**
     ! TEMP : Just to see the preciness of the engine
     */
    private static void saveBoundingBoxImage(BufferedImage img, HashMap<String, HashMap> bounds){
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        for(String key : bounds.keySet()){
            HashMap<String, Integer> box =  bounds.get(key);
            g2d.drawString(key, box.get("x")+10, box.get("y")+10);
            g2d.drawRect(box.get("x"), box.get("y"), box.get("width"), box.get("height"));
        }
        g2d.dispose();
        try {
            ImageIO.write(img, "png", new File("./output_image.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
