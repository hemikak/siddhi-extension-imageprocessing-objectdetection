# siddhi-extension-imageprocessing-objectdetection

## Welcome,

The presented code is functional siddhi extension which counts the number of objects detected in an image. The object to be detected depends on the cascade file path given. The image processing is done using JavaCV.

Use the following maven command to build the extension so that it will support all platforms.

```
mvn clean install -Dplatform.dependencies
```

## References
* [JavaCV](https://github.com/bytedeco/javacv)
* [Functional Siddhi Extension](https://docs.wso2.com/display/CEP310/Writing+a+Custom+Function)
