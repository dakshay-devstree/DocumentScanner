<h1 align="center">Document Scanner</h1>
<p align="center">
  <a href="https://jitpack.io/#dakshay-devstree/DocumentScanner"> <img src="https://jitpack.io/v/dakshay-devstree/DocumentScanner/month.svg" /></a>
  <a href="https://jitpack.io/#dakshay-devstree/DocumentScanner"> <img src="https://jitpack.io/v/dakshay-devstree/DocumentScanner.svg" /></a>
</p>

DocumentScanner is android library which will help you to scan image documents based on OpenCV Library.

# Installation
Step 1. Add the JitPack repository to your build file
```
allprojects {
    repositories {
	...
	maven { url 'https://jitpack.io' }
}
```
Step 2. Add the dependency
```
dependencies {
	implementation 'com.github.dakshay-devstree:DocumentScanner:Tag'
}
```

Step 3. Send local image path and get cropped image file path 
```
val intent = Intent(this, ImageCropActivity::class.java)
intent.putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, imgPath)
scanResult.launch(intent)

private var scanResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val image = result.data!!.getStringExtra(ImageCropActivity.EXTRA_OUTPUT_IMAGE)
            if (!image.isNullOrEmpty()) {
                binding.imgScanResult.setImageURI(Uri.fromFile(File(image)))
            } else Toast.makeText(this, "Document parsing failed", Toast.LENGTH_LONG).show()
        }
    }
```

**UI Customization** Use the static varibles for change default UI

```kotlin
ScannerTheme.nextButtonText = "Next"
ScannerTheme.cancelButtonText = "Cancel"
ScannerTheme.nextButtonBgColor = "#FF6200EE"
ScannerTheme.cancelButtonBgColor = "#FF000000"
```

## Thanks
* Thanks RX library to improve this project.
* Thanks OpenCV for this awesome library. - https://opencv.org/ and
* Inspiration from mayuce. Thanks him for his source codes. - https://github.com/mayuce/AndroidDocumentScanner

----
* Bug reports and pull requests are welcome.
* Make sure you use [square/java-code-styles](https://github.com/square/java-code-styles) to format your code.
