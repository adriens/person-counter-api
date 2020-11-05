package com.adriens.personcounterapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.adriens.personcounterapi.exception.ImageNotFoundException;

import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import ai.djl.ModelException;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;

@RestController
public class PersonCounterController implements ErrorController {

    @Autowired
    private PersonCounterService service;

    private Setup APIsetup = new Setup();
    private final Logger log = LoggerFactory.getLogger(PersonCounterController.class);

    @GetMapping("/photos/{file}/detect")
    public ArrayList<Detection> detect(@PathVariable String file)
            throws IOException, ModelException, TranslateException {

        DetectedObjects objects = null;
        try {
            objects = service.detect(file, APIsetup);
        } catch (IIOException e) {
            log.info("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        ArrayList<Detection> json = service.detectedObjectsToJson(objects);

        return json;
    }

    @GetMapping("/photos/{file}/visualize")
    public void visualize(HttpServletResponse response, @PathVariable String file)
            throws IOException, ModelException, TranslateException {
        try {
            service.saveBoundingBoxImage(file, APIsetup);
        } catch (IIOException e) {
            log.info("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        ClassPathResource imgfile = new ClassPathResource("images/output.png");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        StreamUtils.copy(imgfile.getInputStream(), response.getOutputStream());
    }

    @GetMapping("/photos/{file}/metadata")
    public HashMap<String, String> metadata(@PathVariable String file) throws IOException, SAXException, TikaException {
        HashMap<String, String> metadatas = null;
        try{
            metadatas = service.getMetaDatas(file);
        } catch(FileNotFoundException e){
            log.info("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        };
        return metadatas;
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
        return String.format("<html>"
                        + "<body><h2>Error Page</h2><div>Status code: <b>%s</b></div>"
                        + "</body></html>",
                statusCode, exception==null? "N/A": exception.getMessage());
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}
