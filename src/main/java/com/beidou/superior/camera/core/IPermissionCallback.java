package com.beidou.superior.camera.core;

/**
 * Permission Callback
 */
public interface IPermissionCallback {
    boolean onRequestPermission(String[] permissions, int requestCode);
}
