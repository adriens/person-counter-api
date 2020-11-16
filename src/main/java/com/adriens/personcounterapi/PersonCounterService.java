/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.adriens.personcounterapi;

import ai.djl.ModelException;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.translate.TranslateException;
import ai.djl.util.JsonUtils;

import com.adriens.personcounterapi.exception.UnsupportedHostException;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

/**
 * An example of inference of object detection using saved model from TensorFlow
 * 2 Detection Model Zoo.
 *
 * <p>
 * Tested with EfficientDet, SSD MobileNet V2, Faster RCNN Inception Resnet V2
 * downloaded from <a href=
 * "https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf2_detection_zoo.md">here</a>
 *
 * <p>
 * See this <a href=
 * "https://github.com/awslabs/djl/blob/master/examples/docs/object_detection_with_tensorflow_saved_model.md">doc</a>
 * for information about this example.
 */

@Service
public final class PersonCounterService {

    private static final Logger logger = LoggerFactory.getLogger(PersonCounterService.class);
    private static final List<String> imageHosts = Arrays.asList("imgur");

    private PersonCounterService() {
    }

    /**
     * Runs prediction on an image
     * 
     * @param img   Image to run prediction on
     * @param setup general setup
     * @return a DetectedObjects object
     * @throws TranslateException
     */
    public DetectedObjects launchPrediction(Image img, Setup setup) throws TranslateException {
        logger.info("[" + LocalTime.now() + "] Begining prediction..");
        DetectedObjects detection = setup.getPredictor().predict(img);
        logger.info("[" + LocalTime.now() + "] Prediction done!");
        return detection;
    }

    /**
     * Runs object detection on a picture
     * 
     * @param file  URI input for file name
     * @param setup general setup
     * @return a DetectedObjects object
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public DetectedObjects detect(String file, Setup setup) throws IOException, ModelException, TranslateException {
        Path imageFile = Paths.get("input/" + file);
        Image img = ImageFactory.getInstance().fromFile(imageFile);
        return launchPrediction(img, setup);
    }

    /**
     * Runs object detection on a third party picture
     * 
     * @param file  URI input for file name
     * @param host  URI input for host name
     * @param setup general setup
     * @return a DetectedObjects object
     * @throws TranslateException
     * @throws IOException
     */
    public DetectedObjects thirdPartyDetect(String file, String host, Setup setup)
            throws TranslateException, IOException {
        InputStream is = thirdPartyImage(host, file);
        Image img = ImageFactory.getInstance().fromInputStream(is);
        is.close();
        return launchPrediction(img, setup);
    }

    /**
     * Allows the conversion from DetectedObjects objects to json
     * @param detection the DetectedObjects object
     * @param label GET input for label filter
     * @param confidence GET input for confidence filter
     * @return a list of Detection objects which will be read as a json
     */
    public ArrayList<Detection> detectedObjectsToJson(DetectedObjects detection, String label, String confidence, String alias) {
        ArrayList<Detection> list = new ArrayList<>();
        
        int i = 1;
        try{
            for (Classification item : detection.items()) {
                if((label == null || label.equals(item.getClassName())) 
                && (confidence == null || item.getProbability() > Float.parseFloat(confidence)/100)){
                    Rectangle bounds = ((DetectedObject) detection.item(i-1)).getBoundingBox().getBounds();
                    list.add(new Detection(i, item.getClassName(), alias, item.getProbability(), bounds.getX(), bounds.getY(),
                            bounds.getWidth(), bounds.getHeight()));
                    i++;
                }
            }
        } catch(NumberFormatException e){
            logger.error("Invalid input for confidence parameter");
        }
        return list;
    }

    /**
     * Allows image visualization
     * 
     * @param file URI input for file name
     * @param setup general setup
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public void visualize(String file, Setup setup) throws IOException, ModelException, TranslateException {
        Path imageFile = Paths.get("input/" + file);
        Image img = ImageFactory.getInstance().fromFile(imageFile);
        saveImage(img, setup);
    }

    /**
     * Allows third party image visualization
     * 
     * @param host URI input for host name
     * @param file URI input for file name
     * @param setup general input
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public void thirdPartyVisualize(String host, String file, Setup setup)
            throws IOException, ModelException, TranslateException {
        InputStream is = thirdPartyImage(host, file);
        Image img = ImageFactory.getInstance().fromInputStream(is);
        is.close();
        saveImage(img, setup);
    }

    /**
     * Saves an image with boxes surrounding the detected objects
     * 
     * @param img the picture to process
     * @param setup general setup
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public void saveImage(Image img, Setup setup) throws IOException, TranslateException {
        DetectedObjects detection = launchPrediction(img, setup);
        Path outputDir = Paths.get("output/");
        Files.createDirectories(outputDir);

        // Make image copy with alpha channel because original image was jpg
        Image newImage = img.duplicate(Image.Type.TYPE_INT_ARGB);
        newImage.drawBoundingBoxes(detection);

        Path imagePath = outputDir.resolve("output.png");
        // OpenJDK can't save jpg with alpha channel
        newImage.save(Files.newOutputStream(imagePath), "png");
        logger.info("Image with detected objects has been saved in: {}", imagePath);
    }

    /**
     * Retrieves the metadatas of a picture
     * 
     * @param img picture to retrieve the metadatas from
     * @return a list of metadatas
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public HashMap<String, String> metadatas(InputStream img) throws IOException, SAXException, TikaException {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata(); // empty metadata object
        InputStream inputstream = img;
        ParseContext context = new ParseContext();
        parser.parse(inputstream, handler, metadata, context);

        // getting the list of all meta data elements
        String[] metadataNames = metadata.names();
        HashMap<String, String> metadatas = new HashMap<>();

        for (String name : metadataNames) {
            metadatas.put(name, metadata.get(name));
        }
        return metadatas;
    }

    /**
     * Retrieves the metadatas of a picture
     * 
     * @param file the picture to process
     * @return a list of metadatas
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public HashMap<String, String> getMetadatas(String file) throws IOException, SAXException, TikaException {
        return metadatas(new FileInputStream(new File("input/" + file)));
    }

    /**
     * Retrieves the metadatas of a third party picture
     * 
     * @param host URI input for host name
     * @param file URI inputfor file name
     * @return a list of metadatas
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public HashMap<String, String> thirdPartyMetadatas(String host, String file)
            throws IOException, SAXException, TikaException {
        return metadatas(thirdPartyImage(host, file));
    }

    /**
     * Returns the list of execution-related informations
     * @param img the picture to process
     * @param setup general setup
     * @return a list of execution-related informations
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public HashMap<String, String> analysis(Image img, Setup setup)
            throws IOException, ModelException, TranslateException {
        HashMap<String, String> analysis = new HashMap<>();

        analysis.put("engineName", setup.getEngineName());
        analysis.put("modelType", setup.getModelType());
        analysis.put("modelName", setup.getModelName());
        analysis.put("modelURL", setup.getModelUrl());

        long startTime = System.nanoTime();
        launchPrediction(img, setup);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
        analysis.put("detectionTimeMs", String.valueOf(duration));

        startTime = System.nanoTime();
        saveImage(img, setup);
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
        analysis.put("visualizationTimeMs", String.valueOf(duration));

        return analysis;
    }

    /**
     * Returns the list of execution-related informations
     * @param file URI input for file name
     * @param setup general setup
     * @return a list of execution-related informations
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public HashMap<String, String> getAnalysis(String file, Setup setup)
            throws IOException, ModelException, TranslateException {
        return analysis(ImageFactory.getInstance().fromFile(Paths.get("input/" + file)), setup);
    }

    /**
     * Returns the list of execution-related informations for a third party picture
     * @param host URI input for host name
     * @param file URI input for file name
     * @param setup general setup
     * @return a list of execution-related informations for a third party picture
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    public HashMap<String, String> thirdPartyAnalysis(String host, String file, Setup setup)
            throws IOException, ModelException, TranslateException {
        return analysis(ImageFactory.getInstance().fromInputStream(thirdPartyImage(host, file)), setup);
    }

    /**
     * Returns a list of detection objects AND metadatas
     * @param file  the picture to process
     * @param label GET input for label filter
     * @param confidence GET input for confidence filter
     * @param setup general setup
     * @return a list of detection objects AND metadatas
     * @throws IOException
     * @throws TranslateException
     * @throws SAXException
     * @throws TikaException
     */
    public HashMap<String, Object> getFullDetect(String file, String label, String confidence, String alias, Setup setup)
            throws IOException, TranslateException, SAXException, TikaException {

        HashMap<String, Object> map = new HashMap<>();

        Image img = ImageFactory.getInstance().fromInputStream(new FileInputStream("input/" + file));
        ArrayList<Detection> objects = detectedObjectsToJson(setup.getPredictor().predict(img), label, confidence, alias);
        map.put("count", objects.size());
        map.put("image", objects);

        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata(); // empty metadata object
        ParseContext context = new ParseContext();
        parser.parse(new FileInputStream("input/" + file), handler, metadata, context);

        // getting the list of all meta data elements
        String[] metadataNames = metadata.names();
        HashMap<String, String> metadatas = new HashMap<>();

        for (String name : metadataNames) {
            metadatas.put(name, metadata.get(name));
        }
        map.put("metadata", metadatas);

        return map;
    }

    /**
     * Returns a list of detection objects AND metadatas
     * @param host URI input for host name
     * @param file URI input for file name
     * @param label GET input for label filter
     * @param confidence GET input for confidence filter
     * @param setup general setup 
     * @return a list of detection objects AND metadatas
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     * @throws TranslateException
     */
    public HashMap<String, Object> thirdPartyFullDetect(String host, String file, String label, String confidence, String alias, Setup setup)
            throws FileNotFoundException, IOException, SAXException, TikaException, TranslateException {
        HashMap<String, Object> map = new HashMap<>();

        Image img = ImageFactory.getInstance().fromInputStream(thirdPartyImage(host, file));
        
        ArrayList<Detection> objects = detectedObjectsToJson(setup.getPredictor().predict(img), label, confidence, alias);
        map.put("count", objects.size());
        map.put("image", objects);

        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata(); // empty metadata object
        ParseContext context = new ParseContext();
        parser.parse(thirdPartyImage(host, file), handler, metadata, context);

        // getting the list of all meta data elements
        String[] metadataNames = metadata.names();
        HashMap<String, String> metadatas = new HashMap<>();

        for (String name : metadataNames) {
            metadatas.put(name, metadata.get(name));
        }
        map.put("metadata", metadatas);

        return map;
    }

    /**
     * Lists all pictures from the input folder
     * @return a list of file names
     * @throws IOException
     */
    public HashMap<String, String> listFiles() throws IOException {
        Path outputDir = Paths.get("input");
        Files.createDirectories(outputDir);

        File folder = new File("input/");
        File[] listOfFiles = folder.listFiles();

        HashMap<String, String> files = new HashMap<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            files.put(String.valueOf(i), listOfFiles[i].getName());
        }

        if (files.size() == 0) {
            files.put("notice", "No images found");
        }
        return files;
    }

    /**
     * Returns the InputStream of a third party image
     * @param host URI input for host name
     * @param file URI input for file name
     * @return an InputStream object
     * @throws IOException
     */
    public InputStream thirdPartyImage(String host, String file) throws IOException {
        if(imageHosts.indexOf(host) == -1){
            logger.error("Image host '" + host + "' isn't supported. Supported hosts are: " + imageHosts.toString());
            throw new UnsupportedHostException();
        }
        
        URL url = null;
        switch(host){
            case "imgur":
            default:
                url = new URL("https://i.imgur.com/" + file);
                break;    
        }

        InputStream is = null;
        is = url.openStream();
        return is;
    }

    static Map<Integer, String> loadSynset() throws IOException {
        URL synsetUrl =
                new URL(
                        "https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/mscoco_label_map.pbtxt");
        Map<Integer, String> map = new ConcurrentHashMap<>();
        int maxId = 0;
        try (InputStream is = synsetUrl.openStream();
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("item ");
            while (scanner.hasNext()) {
                String content = scanner.next();
                content = content.replaceAll("(\"|\\d)\\n\\s", "$1,");
                Item item = JsonUtils.GSON.fromJson(content, Item.class);
                map.put(item.id, item.displayName);
                if (item.id > maxId) {
                    maxId = item.id;
                }
            }
        }
        return map;
    }

    private static final class Item {
        int id;

        @SerializedName("display_name")
        String displayName;
    }
}