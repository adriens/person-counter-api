package com.adriens.personcounterapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.annotations.SerializedName;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.JsonUtils;

public class Setup {
    private String engineName = "Tensorflow";    
    private String modelType = "faster_rcnn";
    private String modelName = "resnet101_v1_640x640/1";
    private String modelUrl =
                "https://storage.googleapis.com/tfhub-modules/tensorflow/" + modelType + "/" + modelName + ".tar.gz";
    //private ClassLoader classLoader = getClass().getClassLoader();
    //private File ref = new File(classLoader.getResource("models/faster_rcnn_inception_resnet_v2_640x640_1.tar.gz").getFile());
    //private String modelUrl = ref.getAbsolutePath();
    private Criteria<Image, DetectedObjects> criteria;
    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;

    public Setup() {
        this.criteria = Criteria.builder()
                    .optApplication(Application.CV.OBJECT_DETECTION)
                    .setTypes(Image.class, DetectedObjects.class)
                    .optModelUrls(modelUrl)
                    // saved_model.pb file is in the subfolder of the model archive file
                    .optModelName(modelName)
                    .optFilter("inception", "resnet50")
                    .optTranslator(new MyTranslator())
                    .optProgress(new ProgressBar())
                    .build();
        try {
            this.model = ModelZoo.loadModel(criteria);
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.predictor = model.newPredictor();
    }

    public String getEngineName(){
        return engineName;
    }

    public String getModelType(){
        return modelType;
    }

    public String getModelName(){
        return modelName;
    }

    public String getModelUrl(){
        return modelUrl;
    }

    public Criteria<Image, DetectedObjects> getCriteria(){
        return criteria;
    }

    public ZooModel<Image, DetectedObjects> getModel() {
        return model;
    }

    public Predictor<Image, DetectedObjects> getPredictor(){
        return predictor;
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

    private static final class MyTranslator implements Translator<Image, DetectedObjects> {

        private Map<Integer, String> classes;
        private int maxBoxes;
        private float threshold;

        MyTranslator() {
            maxBoxes = 10;
            threshold = 0.7f;
        }

        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            // input to tf object-detection models is a list of tensors, hence NDList
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            // optionally resize the image for faster processing
            array = NDImageUtils.resize(array, 224);
            // tf object-detection models expect 8 bit unsigned integer tensor
            array = array.toType(DataType.UINT8, true);
            array = array.expandDims(0); // tf object-detection models expect a 4 dimensional input
            return new NDList(array);
        }

        @Override
        public void prepare(NDManager manager, Model model) throws IOException {
            if (classes == null) {
                classes = loadSynset();
            }
        }

        @Override
        public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
            // output of tf object-detection models is a list of tensors, hence NDList in djl
            // output NDArray order in the list are not guaranteed

            int[] classIds = null;
            float[] probabilities = null;
            NDArray boundingBoxes = null;
            for (NDArray array : list) {
                if ("detection_boxes".equals(array.getName())) {
                    boundingBoxes = array.get(0);
                } else if ("detection_scores".equals(array.getName())) {
                    probabilities = array.get(0).toFloatArray();
                } else if ("detection_classes".equals(array.getName())) {
                    // class id is between 1 - number of classes
                    classIds = array.get(0).toType(DataType.INT32, true).toIntArray();
                }
            }
            Objects.requireNonNull(classIds);
            Objects.requireNonNull(probabilities);
            Objects.requireNonNull(boundingBoxes);

            List<String> retNames = new ArrayList<>();
            List<Double> retProbs = new ArrayList<>();
            List<BoundingBox> retBB = new ArrayList<>();

            // result are already sorted
            for (int i = 0; i < Math.min(classIds.length, maxBoxes); ++i) {
                int classId = classIds[i];
                double probability = probabilities[i];
                // classId starts from 1, -1 means background
                if (classId > 0 && probability > threshold) {
                    String className = classes.getOrDefault(classId, "#" + classId);
                    float[] box = boundingBoxes.get(i).toFloatArray();
                    float yMin = box[0];
                    float xMin = box[1];
                    float yMax = box[2];
                    float xMax = box[3];
                    Rectangle rect = new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin);
                    retNames.add(className);
                    retProbs.add(probability);
                    retBB.add(rect);
                }
            }

            return new DetectedObjects(retNames, retProbs, retBB);
        }

        @Override
        public Batchifier getBatchifier() {
            return null;
        }
    }
}
