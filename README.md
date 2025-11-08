# Smart Attendance System

## Requirements

- [gradle 9.0.0](https://gradle.org/install/)
- [maven 3.9.11](https://maven.apache.org/install.html)
- jdk 21

## Setup

### Installing OpenCV to local maven repository

```bash
# change /path/to/opencv-480.jar depending on where you installed
mvn install:install-file "-Dfile=/path/to/opencv-480.jar" "-DgroupId=org.opencv" "-DartifactId=opencv" "-Dversion=4.8.0" "-Dpackaging=jar"
```

## Running program

```bash
# Windows
./gradlew.bat run

# MacOS
./gradlew run
```

## Running tests

```bash
# Windows
./gradlew.bat test

# MacOS
./gradlew test
```

## Package fat JAR

```bash
# Windows
./gradlew.bat jar

# MacOS
./gradlew jar
```
