package com.group5.smartattendance.core;

import org.opencv.objdetect.CascadeClassifier;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class CascadeLoader {

    public static final String DEFAULT_FACE_CASCADE = "haarcascade_frontalface_alt.xml";

    private CascadeLoader() {
    }

    // load default
    public static CascadeClassifier loadDefaultFaceCascade() {
        return loadCascade(DEFAULT_FACE_CASCADE);
    }

    // return a proper cascade classifer given a resource path
    public static CascadeClassifier loadCascade(String resourcePath) {
        String filePath = copyResourceToTempFile(resourcePath);
        CascadeClassifier classifier = new CascadeClassifier(filePath);
        // move empty check from individual files to here
        if (classifier.empty()) {
            throw new IllegalStateException(
                    "Failed to load cascade: " + resourcePath + " (temp path: " + filePath + ")");
        }
        return classifier;
    }

    // opencv needs a absolute path and windows cant read classpath resource
    public static String copyResourceToTempFile(String resourcePath) {
        URL resourceUrl = Objects.requireNonNull(
                CascadeLoader.class.getClassLoader().getResource(resourcePath),
                "Cascade resource not found: " + resourcePath);

        try (InputStream in = resourceUrl.openStream()) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found on classpath: " + resourcePath);
            }
            Path tmp = Files.createTempFile("cascade-", ".xml");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to materialize cascade resource: " + resourcePath, e);
        }
    }
}
