# What's this?
It's a kotlin script that compares android and ios localisation files. It might be useful while merging localisation resources of Android and iOS to shared keys and values.

# Usage
`./gradlew clean build`
`java -jar build/libs/android-ios-strings-merger-1.0.jar <path_to_android_file> <path_to_ios_file> <path_to_export_folder>`

Export folder must exist. I've pointed to the Google Drive, which automatically synced. This made easy the export CSV to Google Spreadsheet later.
