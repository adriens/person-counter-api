package com.adriens.personcounterapi;

import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ai.djl.ModelException;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;

@RestController
public class PersonCounterController {

    @Autowired
    private PersonCounterService service;



    private final Logger log = LoggerFactory.getLogger(PersonCounterController.class);

    @GetMapping("/photos/{file}/detect")
    public ArrayList<Detection> detect(@PathVariable String file)
            throws IOException, ModelException, TranslateException {
        
        DetectedObjects objects = null;
        try{
            objects =  service.detect(file);
        } catch(IIOException e){
            log.info("Can't find image '"+file+"', is it in folder 'input'? ");
            return null;
        }
        ArrayList<Detection> json = service.detectedObjectsToJson(objects);

        return json;
    }

    @GetMapping("/error")
    public String error(){
        return "Error! Check console";
    }
}
