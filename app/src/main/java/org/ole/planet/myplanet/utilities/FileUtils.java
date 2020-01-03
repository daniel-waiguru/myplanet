package org.ole.planet.myplanet.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.ole.planet.myplanet.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static final String SD_PATH = Environment.getExternalStorageDirectory() + "/ole";

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } else {
            return 0;
        }
    }

    public static byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte[] bytes = new byte[size];
        byte[] tmpBuff = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        ;
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private static File createFilePath(String folder, String filename) {
        File f = new File(folder);
        if (!f.exists())
            f.mkdirs();
        Utilities.log("Return file " + folder + "/" + filename);
        return new File(f, filename);
    }

    public static File getSDPathFromUrl(String url) {

        return createFilePath(SD_PATH + "/" + getIdFromUrl(url), getFileNameFromUrl(url));
    }

    public static boolean checkFileExist(String url) {
        if (url == null || url.isEmpty())
            return false;
        File f = createFilePath(SD_PATH + "/" + getIdFromUrl(url), getFileNameFromUrl(url));
        return f.exists();
    }

    public static String getFileNameFromUrl(String url) {
        try {
            return url.substring(url.lastIndexOf("/") + 1);
        } catch (Exception e) {
        }
        return "";
    }

    public static String getIdFromUrl(String url) {
        try {
            String[] sp = url.substring(url.indexOf("resources/")).split("/");
            Utilities.log("Id " + sp[1]);
            return sp[1];
        } catch (Exception e) {
        }
        return "";
    }

    public static String getFileExtension(String address) {
        if (TextUtils.isEmpty(address))
            return "";
        String filenameArray[] = address.split("\\.");
        return filenameArray[filenameArray.length - 1];
    }

    public static void installApk(AppCompatActivity activity, String file) {
        try {
            if (!file.endsWith("apk")) return;
            File toInstall = FileUtils.getSDPathFromUrl(file);
            toInstall.setReadable(true, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", toInstall);
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(apkUri);
                activity.startActivity(intent);
            } else {
                Uri apkUri = Uri.fromFile(toInstall);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static void copyAssets(Context context) {
        String[] tiles = {"dhulikhel.mbtiles", "somalia.mbtiles"};
        AssetManager assetManager = context.getAssets();
        try {
            for (String s : tiles) {
                InputStream in;
                OutputStream out;
                in = assetManager.open(s);
                Utilities.log("MAP " + s);
                File outFile = new File(Environment.getExternalStorageDirectory() + "/osmdroid", s);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                out.close();
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ggggggggg", "Failed to copy asset file: " + e);
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void openOleFolder(Fragment context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Utilities.SD_PATH);
        intent.setDataAndType(uri, "*/*");
        context.startActivityForResult(Intent.createChooser(intent, "Open folder"), 100);
    }


    public static String getImagePath(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = context.getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    public static String getMediaType(String path) {
        String ext = getFileExtension(path);
        if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("png"))
            return "image";
        else if (ext.equalsIgnoreCase("mp4"))
            return "mp4";
        else if (ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("aac"))
            return "audio";
        return "";
    }
}
