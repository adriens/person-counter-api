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

    
    /**
     * Prints a list of informations about the detected objects on a picture
     * @param file URI input for file name
     * @return a list of Detection object, which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
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

    /**
     * Prints a list of informations about the detected objects AND the metadatas of a picture
     * @param file URI input for the file's name
     * @return a list of Detection object and metadatas which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     * @throws SAXException
     * @throws TikaException
     */
    @GetMapping("/photos/{file}/detect/full")
    public HashMap<String, Object> detectFull(@PathVariable String file)
            throws IOException, ModelException, TranslateException, SAXException, TikaException {

        HashMap<String, Object> map = new HashMap<>();
        try{
            map = service.getFullDetect(file, APIsetup);
        } catch (IIOException e) {
            log.info("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        return map;
    }

    /**
     * Shows an image with boxes surrounding detected objects on a picture
     * @param response Server response type for error handling
     * @param file URI input for the file's name
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
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

    /**
     * Prints a list of a picture's metadatas
     * @param file URI input for the file's name
     * @return a list of metadatas which will be read as a json
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
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

    /**
     * Prints a list of informations about the image analysis process
     * @param file URI input for the file's name
     * @return
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    @GetMapping("/photos/{file}/analysis")
    public HashMap<String, String> analysis(@PathVariable String file)
            throws IOException, ModelException, TranslateException {
        HashMap<String, String> analysis = null;
        try{
            analysis = service.getAnalysis(file, APIsetup);
        } catch(IIOException e){
            log.info("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        return analysis;
    }

    /**
     * Adds an image to the list of available-for-analysis pictures
     * @param file URI input for the file's name
     * @throws IOException
     */
    @GetMapping("/photos/{file}/add")
    public void addImg(@PathVariable String file) throws IOException {
        service.addImg(file);
    }

    /**
     * Removes an image from the list of available-for-analysis pictures
     * @param file URI input for the file's name
     * @throws IOException
     */
    @GetMapping("/photos/{file}/remove")
    public void rmImg(@PathVariable String file) throws IOException {
        service.rmImg(file);
    }

    /**
     * Lists all available-for-analysis pictures
     * @return a list of file names
     * @throws IOException 
     */
    @GetMapping("/photos/list")
    public HashMap<String, String> list() throws IOException {
        return service.listFiles();
    }

    /**
     * Error handling function
     * @param request Server response type
     * @return an html page with corresponding error
     */
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
