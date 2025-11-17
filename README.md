<div align="center">
  <img src="https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/ic_logo.png" alt="PixelBeam Logo" width="150"/>

# PixelBeam

### Transfer images between devices using QR codes

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7.6-blue.svg)](https://developer.android.com/jetpack/compose)

<p align="center">
    <strong>No internet • No compression • 100% integrity</strong>
  </p>
</div>

---

## About

PixelBeam is an innovative Android application that enables offline image transfer between devices using QR code technology. By encoding images into a sequence of QR codes, PixelBeam allows users to transfer photos without requiring an internet connection, cloud services, or any wireless protocols. The app preserves 100% image integrity with SHA-256 verification, ensuring that the received image is exactly identical to the original.

### Proof of Concept

This project demonstrates that **files can be transferred between devices without network connections or physical hardware connections**. The core concept is simple:

- Any file can be converted into **Base64** encoded text
- This text can be split into chunks and encoded into **QR codes**
- QR codes can be displayed on one device and scanned by another
- The scanned data can be reassembled into the original file

This proves it's **technically possible** to transfer data visually through QR codes. While PixelBeam currently focuses on images, the underlying technique can theoretically be applied to any file type - documents, videos, archives, or any binary data.

> **Important**: This is purely an experimental proof of concept and portfolio project. It is **not practical or useful** for real-world file transfer scenarios. Traditional methods (WiFi, Bluetooth, USB, NFC) are far superior in every practical aspect. This project exists solely to demonstrate that visual data transfer is *possible*, not that it's a good idea.

---

## Features

- **Offline Transfer** - No internet connection required, works completely offline
- **Zero Compression** - Images transferred with 100% original quality
- **SHA-256 Verification** - File integrity verification using cryptographic checksum
- **Original Metadata** - Preserves filename, MIME type, and file size
- **Real-time Progress** - Live progress tracking during transfer
- **Configurable Speed** - Transfer speeds from 50ms to 5s per QR code
- **Auto-Advance** - Automatic QR code progression with pause/resume
- **Manual Control** - Navigate with slider and buttons for precise control
- **Dual Metadata** - Redundant metadata chunks at start and end for reliability
- **Modern UI** - Material 3 design with responsive layouts
- **Clean Architecture** - MVVM pattern with proper separation of concerns
- **Error Handling** - Comprehensive error states with user-friendly messages

---

## Download

![GitHub Release](https://img.shields.io/github/v/release/ahmmedrejowan/PixelBeam)

You can download the latest APK from here

<a href="https://github.com/ahmmedrejowan/PixelBeam/releases/download/1.0/PixelBeam_1_0.apk">
<img src="https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/get.png" width="224px" align="center"/>
</a>

Check out the [releases](https://github.com/ahmmedrejowan/PixelBeam/releases) section for more details.

---

## Screenshots

| Shots                                                                                                 | Shots                                                                                                 | Shots                                                                                                   |
|-------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| ![Screenshot 1](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_1.jpg)      | ![Screenshot 2](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_2.jpg)      | ![Screenshot 3](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_3.jpg)        |
| ![Screenshot 4](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_4.jpg)      | ![Screenshot 5](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_5.jpg)      | ![Screenshot 6](https://raw.githubusercontent.com/ahmmedrejowan/PixelBeam/main/files/shot_6.jpg)        |

---

## How It Works

PixelBeam transfers images between devices by encoding them into a series of QR codes. Here's the technical flow:

### Sender Side

1. **Image Selection** - User selects an image from gallery
2. **Metadata Extraction** - Extract filename, size, and MIME type
3. **Checksum Calculation** - Generate SHA-256 hash of original file
4. **Data Chunking** - Split image data into 650-character chunks
5. **Metadata Chunks** - Add metadata at index 0 and last index for redundancy
6. **QR Generation** - Generate QR codes for all chunks with progress tracking
7. **Sequential Display** - Show QR codes one by one at configurable speed

### QR Code Format

```
IMG|TOTAL|INDEX|CHECKSUM|DATA

Components:
- IMG: Header identifier
- TOTAL: Total number of chunks (3-digit padded)
- INDEX: Current chunk index (3-digit padded)
- CHECKSUM: MD5 hash for chunk integrity (8 characters)
- DATA: Base64 encoded payload (max 650 chars)

Special Chunks:
- Index 0: Metadata (filename, size, MIME type, SHA-256)
- Last Index: Duplicate metadata for redundancy
```

### Receiver Side

1. **Camera Scanning** - Use CameraX and ML Kit for QR detection
2. **Chunk Collection** - Gather all chunks with real-time progress
3. **Metadata Parsing** - Extract file information from metadata chunks
4. **Data Reconstruction** - Reassemble chunks in correct order
5. **Integrity Verification** - Verify SHA-256 checksum matches
6. **File Saving** - Save with original filename and metadata

---

## Architecture

PixelBeam follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI + ViewModels + Compose Screens)    │
│                                         │
│  • HomeScreen                           │
│  • ImagePickerScreen                    │
│  • ImagePreviewScreen                   │
│  • QRDisplayScreen                      │
│  • QRScannerScreen                      │
│  • ImageResultScreen                    │
│  • AboutScreen                          │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│           Domain Layer                  │
│    (Use Cases + Business Logic)         │
│                                         │
│  • QRGenerator                          │
│  • ImageReconstructor                   │
│  • ImageCompressor                      │
│  • FileManager                          │
│  • ChecksumCalculator                   │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│            Data Layer                   │
│      (Models + Repositories)            │
│                                         │
│  • QRChunk                              │
│  • TransferMetadata                     │
│  • CompressedImage                      │
└─────────────────────────────────────────┘
```

### Design Patterns

- **MVVM** - Model-View-ViewModel for UI state management
- **Repository Pattern** - Abstract data access
- **Dependency Injection** - Koin for managing dependencies
- **StateFlow** - Reactive state management
- **Clean Architecture** - Domain, Data, and Presentation layers

---

## Tech Stack

### Core

- **Kotlin 2.1.0** - Primary programming language
- **Jetpack Compose 1.7.6** - Modern declarative UI framework
- **Material 3** - Latest Material Design components
- **Coroutines & Flow** - Asynchronous programming

### Architecture Components

- **Navigation Compose** - Type-safe navigation
- **ViewModel** - UI state management
- **Lifecycle** - Lifecycle-aware components
- **Koin 4.0.0** - Dependency injection

### Image Processing

- **ZXing 3.5.3** - QR code generation
- **ML Kit Barcode Scanning 17.3.0** - QR code scanning
- **CameraX 1.4.1** - Camera operations
- **Coil 3.0.4** - Image loading and caching

### Utilities

- **Kotlinx Serialization 1.7.3** - JSON parsing and navigation
- **Accompanist Permissions 0.36.0** - Runtime permissions

### Build & Tools

- **Gradle 8.9** - Build system
- **AGP 8.7.3** - Android Gradle Plugin
- **Min SDK 24** - Android 7.0 (Nougat)
- **Target SDK 36** - Latest Android version

---

## Requirements

- **Android 7.0 (API 24)** or higher
- **Camera permission** - For QR code scanning
- **Storage permission** - For saving received images
- **8MB+ storage** - For app installation
- **Two Android devices** - One sender, one receiver

---

## Building from Source

### Prerequisites

- Android Studio Ladybug Feature Drop | 2024.2.2 or later
- JDK 17 or higher
- Android SDK with API 24+

### Steps

1. **Clone the repository**

```bash
git clone https://github.com/ahmmedrejowan/PixelBeam.git
cd PixelBeam
```

2. **Open in Android Studio**

```bash
# Open Android Studio and select "Open an Existing Project"
# Navigate to the cloned directory
```

3. **Sync Gradle**

```bash
# Android Studio will automatically sync Gradle
# Or manually: File → Sync Project with Gradle Files
```

4. **Build the project**

```bash
./gradlew assembleDebug
# Or use Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
```

5. **Run on device**

```bash
./gradlew installDebug
# Or use Android Studio: Run → Run 'app'
```

### Build Variants

- **Debug** - Development build with logging
- **Release** - Production-ready build

---

## Usage Guide

### Sending Images

1. **Launch App** - Open PixelBeam on sender device
2. **Tap Send** - Select "Send Image" from home screen
3. **Choose Image** - Pick image from gallery
4. **Preview** - Review image details and metadata
5. **Generate QR** - Tap "Generate QR Codes" to create codes
6. **Configure Speed** - Choose transfer speed (default: 1 second)
7. **Start Transfer** - Tap play to begin auto-advance
8. **Complete** - Tap "Finish Transfer" when done

### Receiving Images

1. **Launch App** - Open PixelBeam on receiver device
2. **Tap Receive** - Select "Receive Image" from home screen
3. **Grant Permission** - Allow camera access
4. **Scan Codes** - Point camera at sender's QR codes
5. **Wait** - App automatically collects all chunks
6. **Verify** - SHA-256 checksum verified automatically
7. **Save** - Tap "Save Image" to store in gallery
8. **Share** - Optionally share the received image

---

## Known Limitations

This is a **proof of concept** and **portfolio project** with the following limitations:

- Not optimized for very large images (>10MB)
- Transfer time increases with image size
- Requires good lighting for QR scanning
- Single image transfer only (no batch support)
- No encryption (visible QR codes can be intercepted)

**Not intended for production use** - This project demonstrates technical capabilities and innovative solutions to file transfer challenges.

---

## License

```
Copyright (C) 2025 K M Rejowan Ahmmed

This program is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program. If not,
see <https://www.gnu.org/licenses/>.
```

> [!WARNING]
> **This is a copyleft license.** PixelBeam is licensed under GPL v3.0, which means:
> - ✅ You can freely use, modify, and distribute this software
> - ⚠️ Any derivative works **must also be licensed under GPL v3.0**
> - ⚠️ You **must disclose your source code** if you distribute modified versions
> - ⚠️ You **cannot distribute proprietary/closed-source versions** of this software
>
> If you need different licensing terms, please contact the author.

---

## Author

**K M Rejowan Ahmmed**

- GitHub: [@ahmmedrejowan](https://github.com/ahmmedrejowan)
- Email: [ahmmadrejowan@gmail.com](mailto:ahmmadrejowan@gmail.com)

---

## Acknowledgments

- [ZXing](https://github.com/zxing/zxing) - QR code generation library
- [Google ML Kit](https://developers.google.com/ml-kit/vision/barcode-scanning) - Fast barcode scanning
- [CameraX](https://developer.android.com/jetpack/androidx/releases/camera) - Modern camera API
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Material Design 3](https://m3.material.io/) - Design system

---

