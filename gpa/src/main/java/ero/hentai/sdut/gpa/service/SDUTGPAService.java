package ero.hentai.sdut.gpa.service;

import java.io.IOException;
import java.util.Map;

/**
 * @author MiK
 * @version 0.0.1
 * @since JDK1.8
 */
public interface SDUTGPAService {
    Map compute(int year, String JSESSIONID) throws IOException;
}
