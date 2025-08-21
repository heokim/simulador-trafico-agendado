package py.una.pol.simulador.eon.utils;

import java.io.InputStream;

/**
 * Clase para lectura de recursos externos
 *
 * @author Néstor E. Reinoso Wood
 */
public class ResourceReader {

    /**
     * Reads a file from a specified path and return the input stream
     *
     * @param fileName Path of the file to read
     * @return Input stream of the file
     * @throws IllegalArgumentException If the file is not found
     */
    public static InputStream getFileFromResourceAsStream(String fileName)
            throws IllegalArgumentException {
        ClassLoader classLoader = ResourceReader.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }
}
