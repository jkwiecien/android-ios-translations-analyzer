# What's this?
It's a kotlin script that compares android and ios localisation files. It might be useful while merging localisation resources of Android and iOS to shared keys and values.

# Usage

## Building yourself
```
./gradlew clean build
java -jar build/libs/android-ios-strings-merger-1.0.jar <path_to_android_file> <path_to_ios_file> <path_to_export_folder>
```

## Using a jar
Just download a released jar and execute
```
java -jar build/libs/android-ios-strings-merger-1.0.jar <path_to_android_file> <path_to_ios_file> <path_to_export_folder>
```

### Output
Export folder must exist. It will create csv files that you can easily import to any excel-like app.
