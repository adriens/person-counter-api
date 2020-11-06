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
import ai.djl.engine.Engine;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.translate.TranslateException;
import ai.djl.util.JsonUtils;

import com.adriens.personcounterapi.exception.UnknownImageUrlException;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

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

    private PersonCounterService() {
    }

    public DetectedObjects detect(String file, Setup setup) throws IOException, ModelException, TranslateException {
        if (!"TensorFlow".equals(Engine.getInstance().getEngineName())) {
            return null;
        }

        Path imageFile = Paths.get("input/" + file);
        Image img = ImageFactory.getInstance().fromFile(imageFile);
        logger.info("[" + LocalTime.now() + "] Begining prediction..");
        DetectedObjects detection = setup.getPredictor().predict(img);
        logger.info("[" + LocalTime.now() + "] Prediction done!");
        return detection;
    }

    public ArrayList<Detection> detectedObjectsToJson(DetectedObjects detection) {
        ArrayList<Detection> list = new ArrayList<>();

        int i = 0;
        for (Classification item : detection.items()) {
            Rectangle bounds = ((DetectedObject) detection.item(i)).getBoundingBox().getBounds();
            list.add(new Detection(i, item.getClassName(), item.getProbability(), bounds.getX(), bounds.getY(),
                    bounds.getWidth(), bounds.getHeight()));
            i++;
        }
        return list;
    }

    public void saveBoundingBoxImage(String file, Setup setup) throws IOException, ModelException, TranslateException {
        DetectedObjects detection = new PersonCounterService().detect(file, setup);
        Path outputDir = Paths.get("src/main/resources/images");
        Files.createDirectories(outputDir);

        Path imageFile = Paths.get("input/" + file);
        Image img = ImageFactory.getInstance().fromFile(imageFile);

        // Make image copy with alpha channel because original image was jpg
        Image newImage = img.duplicate(Image.Type.TYPE_INT_ARGB);
        newImage.drawBoundingBoxes(detection);

        Path imagePath = outputDir.resolve("output.png");
        // OpenJDK can't save jpg with alpha channel
        newImage.save(Files.newOutputStream(imagePath), "png");
        logger.info("Detected objects image has been saved in: {}", imagePath);
    }

    public HashMap<String, String> getMetaDatas(String file) throws IOException, SAXException, TikaException {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata(); // empty metadata object
        FileInputStream inputstream = new FileInputStream("input/" + file);
        ParseContext context = new ParseContext();
        parser.parse(inputstream, handler, metadata, context);

        System.out.println(handler.toString());

        // getting the list of all meta data elements
        String[] metadataNames = metadata.names();
        HashMap<String, String> metadatas = new HashMap<>();

        for (String name : metadataNames) {
            metadatas.put(name, metadata.get(name));
        }
        return metadatas;
    }

    public HashMap<String, String> getAnalysis(String file, Setup setup)
            throws IOException, ModelException, TranslateException {
        HashMap<String, String> analysis = new HashMap<>();

        analysis.put("engineName", setup.getEngineName());
        analysis.put("modelType", setup.getModelType());
        analysis.put("modelName", setup.getModelName());
        analysis.put("modelURL", setup.getModelUrl());

        long startTime = System.nanoTime();
        new PersonCounterService().detect(file, setup);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
        analysis.put("detectionTimeMs", String.valueOf(duration));

        startTime = System.nanoTime();
        new PersonCounterService().saveBoundingBoxImage(file, setup);
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000; // divide by 1000000 to get milliseconds.
        analysis.put("visualizationTimeMs", String.valueOf(duration));

        return analysis;
    }

    public void addImg(String imageUrl) throws IOException {
        Path outputDir = Paths.get("input");
        Files.createDirectories(outputDir);

        URL url = new URL("https://i.imgur.com/" + imageUrl);
        String fileName = url.getFile();
        String destName = "input/" + fileName.substring(fileName.lastIndexOf("/"));

        String mimetype = new MimetypesFileTypeMap().getContentType(destName);
        String type = mimetype.split("/")[0];
        if(!type.equals("image")){
            logger.info(fileName + " is not an image.");
            return;
        }

        InputStream is = null;
        try{
            is = url.openStream();
        } catch(FileNotFoundException e){
            throw new UnknownImageUrlException(e.getMessage());
        }
        OutputStream os = new FileOutputStream(destName);

        byte[] b = new byte[2048];
        int length;

        while((length = is.read(b)) != -1){
            os.write(b, 0, length);
        }

        is.close();
        os.close();
        logger.info("Successfully added image " + fileName);
    }

    public void rmImg(String img){
        File file = new File("input/" + img);
        if(file.exists() && file.isFile()){
            file.delete();
            logger.info("Successfully removed image " + img);
            return;
        }
        logger.info("Couldn't remove image" + img);
    }

    public HashMap<String, String> listFiles() throws IOException {
        Path outputDir = Paths.get("input");
        Files.createDirectories(outputDir);

        File folder = new File("input/");
        File[] listOfFiles = folder.listFiles();

        HashMap<String, String> files = new HashMap<>();
        for(int i = 0; i < listOfFiles.length; i++){
            files.put(String.valueOf(i), listOfFiles[i].getName());
        }

        if(files.size() == 0){
            files.put("notice", "No images found");
        }
        return files;
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