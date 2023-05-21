package cn.whu.cs.niu.PDR;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    public static String assetFilePath(Context context, String assetName) throws IOException {
        //如果mobile_model.ptl文件已经在“应用的内部存储空间context.getFilesDir()”中
        File file = new File(context.getFilesDir(), assetName);

        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();  //则直接返回mobile_model.ptl文件的路径
        }

        //如果不存在，则将assets目录下的mobile_model.ptl文件拷贝到“context.getFilesDir()/mobile_model.ptl”
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();  //返回拷贝后的文件
        }
    }

    public static void writeToLocalStorage(String filePath, String content) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter out = new FileWriter(file);
        out.write(content);
        out.flush();
        out.close();
    }
}
