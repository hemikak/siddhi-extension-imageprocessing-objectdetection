package org.wso2.siddhi.extension.objectdetection;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.bytedeco.javacpp.opencv_core.CvMemStorage;
import static org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import static org.bytedeco.javacpp.opencv_objdetect.cvHaarDetectObjects;


/**
 * A siddhi extension which uses image processing to count the number of objects in an image. The
 * type of object is depending on a cascade file.
 */
@SuppressWarnings("UnusedDeclaration")
@SiddhiExtension(namespace = "imageprocessorobjectdetection", function = "count")
public class ObjectDetectionExtension extends FunctionExecutor {

    /**
     * The logger.
     */
    Logger log = Logger.getLogger(ObjectDetectionExtension.class);

    /**
     * The return type for the extension.
     */
    Attribute.Type returnType;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Attribute.Type[] types, SiddhiContext siddhiContext) {
        returnType = Attribute.Type.LONG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object process(Object obj) {
        long detectedObjectCount = 0;
        if (obj instanceof Object[]) {
            Object[] arguments = (Object[]) obj;
            if (arguments.length == 2) {
                if (arguments[0] instanceof String && arguments[1] instanceof String) {
                    String imageHex = (String) arguments[0];
                    String cascadePath = (String) arguments[1];
                    try {
                        detectedObjectCount = this.detectObjects(imageHex, cascadePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new IllegalArgumentException(
                            "2 String arguments of the hex string of the image and the cascade path is expected.");
                }
            } else {
                throw new IllegalArgumentException(
                        "2 String arguments of the hex string of the image and the cascade path is expected.");
            }
        } else {
            throw new IllegalArgumentException(
                    "2 String arguments of the hex string of the image and the cascade path is expected.");
        }

        return detectedObjectCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    /**
     * Detect objects using JavaCV.
     * The received images are preprocessed prior to identifying objects. The pre-processing
     * includes grayscaling and equalizing of the image histogram allowing for quick and accurate
     * object detection. The detected image count is then returned.
     *
     * @param imageHex    the image hex string
     * @param cascadePath the cascade path
     * @return the detected object count
     */
    private long detectObjects(String imageHex, String cascadePath) throws IOException {
        long objectCount = 0;
        try {
            // conversion to Mat
            byte[] imageByteArr = (byte[]) new Hex().decode(imageHex);

            // Converting received image to a buffered image of java
            InputStream in = new ByteArrayInputStream(imageByteArr);
            BufferedImage bImageFromConvert = ImageIO.read(in);

            // Converting the received image to an Ipl image of opencv
            IplImage image = IplImage.createFrom(bImageFromConvert);

            // Converting received image to grayscale (black and white)
            IplImage grayImage = IplImage.create(bImageFromConvert.getWidth(), bImageFromConvert.getHeight(), IPL_DEPTH_8U, 1);
            cvCvtColor(image, grayImage, CV_BGR2GRAY);

            // Equalizing the histogram of the grayscaled image
            IplImage equImg = IplImage.create(grayImage.width(), grayImage.height(), IPL_DEPTH_8U, 1);
            cvEqualizeHist(grayImage, equImg);

            // Creating classifier to identify objects
            CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(cascadePath));
            CvMemStorage storage = CvMemStorage.create();
            CvSeq sign = cvHaarDetectObjects(image, cascade, storage, 1.1, 3, 0);
            cvClearMemStorage(storage);

            // Returning the number of objects detected.
            return (long) sign.total();
        } catch (DecoderException e) {
            log.error("Unable to process image hex string");
        }

        return objectCount;
    }
}
