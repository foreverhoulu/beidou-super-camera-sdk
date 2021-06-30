# Welcome to use Beidou Image Acquisition SDK
   
## 一、SDK Access preconditions
```java
1. Android SdkVersion >= 16
```

```java
2. JDK >= 1.8
```

```xml
3. Permission list：
<uses-permission android:name="android.permission.CAMERA"/>
<uses-feature android:name="android.hardware.camera"/>
<uses-feature android:name="android.hardware.camera.autofocus"/>
```

## 二、SDK Access guide

```java
// 1、SDK Entry class
import com.beidou.superior.camera.SuperiorCamera;

// 2、Define request code
private static final int sdkRequestCode = 100;

// 3、Start DK
SuperiorCamera
    .createCamera(activity) //this Activity（Required）
    .setFrontFacing()       //set camera, default is setFrontFacing(), set back camera：setBackFacing()
    .start(sdkRequestCode); //set startActivityForResult request code（Required）
    
// 4、Get callback result
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (RESULT_OK == resultCode) {
        if (requestCode == sdkRequestCode) {
            SuperiorCamera.Result result = (SuperiorCamera.Result) data.getSerializableExtra(SuperiorCamera.KEY_CAMERA_RESULT);
            //1、code, not null.
            result.getCode();
            //2、message, not null.
            result.getMessage();
            //3、image bitmap, may be null
            result.getImage();
            //4、clean
            result.clean();
        }
    }
}
```

## 三、SDK Return Code
```java
    SUCCESS(200),                   //SUCCESS
    FAILURE(400),                   //FAILURE
    FAILURE_MANUAL_EXIT(401),       //MANUAL EXIT
    FAILURE_CAMERA_NOT_SUPPORT(402),//CAMERA NOT SUPPORT
```
