package jp.ecuacion.splib.web.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import jp.ecuacion.lib.core.util.DateTimeApiUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides utilities for html components processing.
 */
public class SplibComponentUtil {

  /**
   * Saves uploaded file.
   * 
   * <p>The file is saved at work directory, which is specified by 
   * 
   * @param file file
   * @return file saved path, may be null when file is not uploaded.
   * @throws Exception Exception
   */
  public static String saveUploadedFile(MultipartFile file) throws Exception {
    if (file == null) {
      return null;
    }

    return saveUploadedFileCommon(file.getBytes(), file.getName());
  }

  /**
   * Saves uploaded file.
   * 
   * <p>The file is saved at work directory, which is specified by 
   * 
   * @param base64 file
   * @return file saved path, may be null when file is not uploaded.
   * @throws Exception Exception
   */
  public static String saveUploadedFile(String base64) throws Exception {

    if (StringUtils.isEmpty(base64)) {
      return null;
    }
    
    String part = ";base64,";
    
    base64 = base64.substring(base64.indexOf(part) + part.length());
    Base64.Decoder decoder = Base64.getDecoder();
    return saveUploadedFileCommon(decoder.decode(base64), "");
  }

  private static String saveUploadedFileCommon(byte[] bytes, String filename) throws IOException {

    if (bytes == null || bytes.length == 0) {
      return null;
    }

    String workDirPath = PropertyFileUtil.getApplication("jp.ecuacion.work-dir");
    new File(workDirPath).mkdirs();


    // Timestamp-threadId-filenameInMultipartFile
    String tmpFilename = DateTimeApiUtil.getTimestampStringForFilename(LocalDateTime.now()) + "-"
        + Thread.currentThread().threadId() + "-" + filename;

    String tmpFilePath = workDirPath + "/" + tmpFilename;

    try (FileOutputStream output = new FileOutputStream(tmpFilePath);) {
      output.write(bytes);
    }

    return tmpFilePath;
  }

  /**
   * Obtains picture data in BASE64 format.
   * 
   * @param path path
   * @param pictureFormat jpeg, ...
   * @return picture data in BASE64 format
   * @throws Exception Exception
   */
  public static String getPictureDataBase64(String path, String pictureFormat) throws Exception {
    Base64.Encoder encoder = Base64.getEncoder();
    return "data:image/" + pictureFormat + ";base64,"
        + encoder.encodeToString(Files.readAllBytes(Path.of(path)));
  }
}
