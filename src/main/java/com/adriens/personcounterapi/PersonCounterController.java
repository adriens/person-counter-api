package com.adriens.personcounterapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.adriens.personcounterapi.exception.ImageNotFoundException;
import com.adriens.personcounterapi.exception.UnknownImageUrlException;

import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
     * Shows the API documention page
     * @param httpResponse Server response
     * @throws IOException
     */
    @GetMapping("/")
    public void showDoc(HttpServletResponse httpResponse) throws IOException {
        httpResponse.sendRedirect("/swagger-ui.html");
    }
    
    /**
     * Prints a list of informations about the detected objects on a picture
     * @param file URI input for file name
     * @return a list of Detection object, which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    @GetMapping("/photos/{file}/detect")
    public HashMap<String, Object> detect(@PathVariable String file, 
                            @RequestParam(name = "class", required = false) String label, 
                            @RequestParam(name = "confidence", required = false) String confidence,
                            @RequestParam(name = "alias", required = false) String alias)
            throws IOException, ModelException, TranslateException {

        DetectedObjects objects = null;
        try {
            objects = service.detect(file, APIsetup);
        } catch (IIOException e) {
            log.error("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        HashMap<String, Object> json = new HashMap<>();
        ArrayList<Detection> detections = service.detectedObjectsToJson(objects, label, confidence, alias);
        json.put("count", detections.size());
        json.put("image", detections);

        return json;
    }

    /**
     * Prints a list of informations about the detected objects on an web-hosted picture
     * @param host URI input for host name
     * @param file URI input for file name
     * @return a list of Detection object, which will be read as a json
     * @throws ModelException
     * @throws TranslateException
     * @throws IOException
     */
    @GetMapping("/photos/thirdparty/{host}/{file}/detect")
    public HashMap<String, Object> thirdPartyDetect(@PathVariable String host, 
                                                @PathVariable String file, 
                                                @RequestParam(name = "class", required = false) String label, 
                                                @RequestParam(name = "confidence", required = false) String confidence,
                                                @RequestParam(name = "alias", required = false) String alias)
            throws ModelException, TranslateException, IOException {

        DetectedObjects objects = null;
        try{
        objects = service.thirdPartyDetect(file, host, APIsetup);
        } catch(IOException e){
            log.error("Can't find file " + file + " on " + host);
            throw new UnknownImageUrlException(e.getMessage());
        }
        HashMap<String, Object> json = new HashMap<>();
        ArrayList<Detection> detections = service.detectedObjectsToJson(objects, label, confidence, alias);
        json.put("count", detections.size());
        json.put("image", detections);

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
    public HashMap<String, Object> detectFull(@PathVariable String file, 
                                        @RequestParam(name = "class", required = false) String label, 
                                        @RequestParam(name = "confidence", required = false) String confidence,
                                        @RequestParam(name = "alias", required = false) String alias)
            throws IOException, ModelException, TranslateException, SAXException, TikaException {

        HashMap<String, Object> map = new HashMap<>();
        try{
            map = service.getFullDetect(file, label, confidence, alias, APIsetup);
        } catch (IIOException e) {
            log.error("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        return map;
    }

    /**
     * Prints a list of informations about the detected objects AND the metadatas of a third party picture
     * @param host URI input for the host name
     * @param file URI input for the file name
     * @return a list of Detection object and metadatas which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     * @throws SAXException
     * @throws TikaException
     */
    @GetMapping("/photos/thirdparty/{host}/{file}/detect/full")
    public HashMap<String, Object> thirdPartyDetectFull(@PathVariable String host, 
                                                @PathVariable String file, 
                                                @RequestParam(name = "class", required = false) String label, 
                                                @RequestParam(name = "confidence", required = false) String confidence,
                                                @RequestParam(name = "alias", required = false) String alias)
            throws IOException, ModelException, TranslateException, SAXException, TikaException {

        HashMap<String, Object> map = new HashMap<>();
        try{
            map = service.thirdPartyFullDetect(host, file, label, confidence, alias, APIsetup);
        } catch(IOException e){
            log.error("Can't find file " + file + " on " + host);
            throw new UnknownImageUrlException(e.getMessage());
        }
        return map;
    }

    /**
     * Prints a list of informations about the detected objects
     * @param file Name of the raw binary file
     * @return a list of Detection object  which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    @RequestMapping(method = RequestMethod.POST, value = "/photos/raw")
    public ArrayList<Detection> rawDetect(InputStream file,
                                        @RequestParam(name = "class", required = false) String label, 
                                        @RequestParam(name = "confidence", required = false) String confidence,
                                        @RequestParam(name = "alias", required = false) String alias) throws IOException, ModelException,
            TranslateException {
        try{
            return service.detectedObjectsToJson(service.rawDetect(file, APIsetup), label, confidence, alias);
        } catch(IOException e){
            log.error("Couldn't find file entered as input");
            throw new ImageNotFoundException(e.getMessage());
        }
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
            service.visualize(file, APIsetup);
        } catch (IIOException e) {
            log.error("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        FileInputStream fis = new FileInputStream(new File("output/output.png"));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        StreamUtils.copy(fis, response.getOutputStream());
    }

    /**
     * Shows an image with boxes surrounding detected objects on a third party picture
     * @param response Server response type for error handling
     * @param host URI input for host name
     * @param file URI input for file name
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    @GetMapping("/photos/thirdparty/{host}/{file}/visualize")
    public void thirdPartyVisualize(HttpServletResponse response, 
                                @PathVariable String host, 
                                @PathVariable String file)
            throws IOException, ModelException, TranslateException {
        try{
            service.thirdPartyVisualize(host, file, APIsetup);
        } catch(IOException e){
            log.error("Can't find file " + file + " on " + host);
            throw new UnknownImageUrlException(e.getMessage());
        }
        FileInputStream fis = new FileInputStream(new File("output/output.png"));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        StreamUtils.copy(fis, response.getOutputStream());
        fis.close();
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
            metadatas = service.getMetadatas(file);
        } catch(FileNotFoundException e){
            log.error("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        };
        return metadatas;
    }

    /**
     * Prints a list of a third party picture's metadatas
     * @param host URI input for the host name
     * @param file URI input for the file name
     * @return a list of metadatas which will be read as a json
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    @GetMapping("/photos/thirdparty/{host}/{file}/metadata")
    public HashMap<String, String> metadata(@PathVariable String host, 
                                            @PathVariable String file) throws IOException, SAXException, TikaException {
        HashMap<String, String> metadatas = null;
        try{
            metadatas = service.thirdPartyMetadatas(host, file);
        } catch(IOException e){
            log.error("Can't find file " + file + " on " + host);
            throw new UnknownImageUrlException(e.getMessage());
        }
        return metadatas;
    }

    /**
     * Prints a list of informations about the image analysis process
     * @param file URI input for the file's name
     * @return a list of informations which will be read as a json
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
            log.error("Can't find image '" + file + "', is it in folder 'input'? ");
            throw new ImageNotFoundException(e.getMessage());
        }
        return analysis;
    }

    /**
     * Prints a list of information about the third party image analysis process
     * @param host URI input for host name
     * @param file URI input for file name
     * @return a list of informations which will be read as a json
     * @throws IOException
     * @throws ModelException
     * @throws TranslateException
     */
    @GetMapping("/photos/thirdparty/{host}/{file}/analysis")
    public HashMap<String, String> analysis(@PathVariable String host, @PathVariable String file)
            throws IOException, ModelException, TranslateException {
        HashMap<String, String> analysis = null;
        try{
            analysis = service.thirdPartyAnalysis(host, file, APIsetup);
        } catch(IOException e){
            log.error("Can't find file " + file + " on " + host);
            throw new UnknownImageUrlException(e.getMessage());
        }
        return analysis;
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
