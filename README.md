# Phone Database Browser

A minimal Android app for browsing SQLite database files selected by the user on Android 10+.

## Features
- Choose a folder using Android's Storage Access Framework.
- Search that folder and accessible subfolders for `.db`, `.sqlite`, `.sqlite3`, and `.db3` files.
- List user tables and display up to 200 rows per table.
- Opens database copies read-only; no third-party libraries.

## Build
GitHub Actions builds a debug APK on pushes and pull requests. Open **Actions**, select the latest workflow run, then download the `PhoneDatabaseBrowser-debug` artifact.

Build requirements: JDK 17, Android Gradle Plugin 8.6.1, Gradle 8.7, compile SDK 34.

## Android privacy limitation
Android does not let a normal app browse other apps' private data directories. This app only scans folders the user explicitly grants access to. It does not enumerate every private/system database on the phone.
