package ero.hentai.sdut.gpa.controller;

import ero.hentai.sdut.gpa.service.SDUTGPAService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author MiK
 * @version 0.0.1
 * @since JDK1.8
 */
@RestController
@RequestMapping(path = "/")
public class DefaultController {

    private Logger log = LoggerFactory.getLogger(DefaultController.class);

    @Resource(name = "sdutGPAServiceImpl", type = SDUTGPAService.class)
    private SDUTGPAService sdutGPAServiceImpl;

    @RequestMapping(path = "/index.json")
    public Object index(@RequestParam int year, @RequestParam String sessionId) {
        try {
            return sdutGPAServiceImpl.compute(year, sessionId);
//        } catch (IOException e) {
//            log.error("{}", e.getClass().getSimpleName(), e);
//            return e.getClass().getSimpleName();
////            return e;
        } catch (Throwable t) {
            log.error("{}", t.getClass().getSimpleName(), t);
            return t.getClass().getSimpleName();
//            return t;
        }
    }
}
