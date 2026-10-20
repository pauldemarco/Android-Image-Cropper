# Crop output failure fix

This branch starts at CanHub Android-Image-Cropper 4.7.0. It keeps crop output
writing inside the worker's exception boundary and reports unsuccessful bitmap
compression as an I/O error. Coroutine cancellation remains cancellation.

The caller must handle the crop result's error instead of assuming an output
URI exists. See [Expo issue 49802](https://github.com/expo/expo/issues/49802).

The focused regression checks belong here:

```sh
./gradlew :cropper:testDebugUnitTest \
  --tests '*BitmapCroppingWorkerJobTest' --tests '*BitmapUtilsTest'
```

The bitmap utility tests use Robolectric to exercise Android APIs directly;
they no longer mock the object under test. The worker regression injects a write
failure and checks that it reaches the completion listener exactly once.

Build with JDK 17; the published library still targets Java 11. JitPack publishes
the `android-image-cropper` module by commit under
`com.github.pauldemarco.Android-Image-Cropper`. Consumers should pin a full commit,
and return to the upstream artifact once both error paths are fixed there.
