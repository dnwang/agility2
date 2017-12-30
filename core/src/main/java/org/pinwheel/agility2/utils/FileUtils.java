package org.pinwheel.agility2.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class FileUtils {

    private FileUtils() {
        throw new AssertionError();
    }

    /**
     * create parent dirs
     */
    public static boolean prepareDirs(final File file) {
        boolean result = false;
        if (null != file) {
            File path = file.getParentFile();
            if (!path.exists()) {
                result = path.mkdirs();
            }
        }
        return result;
    }

    /**
     * delete file or directory
     */
    public static boolean delete(final File file) {
        boolean result = true;
        if (null == file || !file.exists()) {
            result = false;
        } else {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    result &= delete(f);
                }
            }
            result &= file.delete();
        }
        return result;
    }

    public static int copy(final File fromFile, final File toFile) {
        if (null == fromFile || !fromFile.exists()) {
            return -1;
        }
        if (null == toFile) {
            return -1;
        }
        try {
            delete(toFile);
            prepareDirs(toFile);
            return IOUtils.connect(new FileInputStream(fromFile), new FileOutputStream(toFile));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean unZip(final File fromFile, final File toPath) {
        if (null == fromFile || !fromFile.exists()) {
            return false;
        } else if (null == toPath || (toPath.exists() && toPath.isFile())) {
            return false;
        }
        boolean result = false;
        prepareDirs(toPath);
        ZipFile zip = null;
        try {
            zip = new ZipFile(fromFile);
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                final String nameEncode = entry.getName();
                final String encode = CommonTools.getEncoding(nameEncode);
                final String nameDecode = (null == encode) ? nameEncode : new String(nameEncode.getBytes(encode), "UTF-8");
                if (entry.isDirectory()) {
                    File dir = new File(toPath, nameDecode);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                } else {
                    File file = new File(toPath, nameDecode);
                    // replace
                    delete(file);
                    prepareDirs(file);
                    IOUtils.connect(zip.getInputStream(entry), new FileOutputStream(file));
                }
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != zip) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static boolean zip(final File fromPath, final File toFile, final boolean includeRootDir, final FileFilter filter) {
        if (null == fromPath || !fromPath.exists() || !fromPath.isDirectory()) {
            return false;
        } else if (null == toFile || (toFile.exists() && toFile.isDirectory())) {
            return false;
        }
        if (includeRootDir) {
            return zip(fromPath.getParentFile(), toFile, false, new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getAbsolutePath().equals(fromPath.getAbsolutePath()) || ((null != filter) && filter.accept(file));
                }
            });
        }
        boolean result = false;
        delete(toFile);
        prepareDirs(toFile);
        ZipOutputStream stream = null;
        try {
            stream = new ZipOutputStream(new FileOutputStream(toFile));
            File[] files = (null == filter) ? fromPath.listFiles() : fromPath.listFiles(filter);
            for (File tmp : files) {
                zipFile(fromPath, tmp.getName(), stream);
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(stream);
        }
        return result;
    }

    private static void zipFile(final File root, final String relativeName, final ZipOutputStream zipOutStream) throws IOException {
        File file = new File(root, relativeName);
        if (file.isFile()) {
            ZipEntry entry = new ZipEntry(relativeName);
            FileInputStream inStream = new FileInputStream(file);
            zipOutStream.putNextEntry(entry);
            int len;
            byte[] buf = new byte[1024];
            while ((len = inStream.read(buf)) >= 0) {
                zipOutStream.write(buf, 0, len);
            }
            inStream.close();
            zipOutStream.closeEntry();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files.length < 1) {
                ZipEntry entry = new ZipEntry(relativeName + File.separator);
                zipOutStream.putNextEntry(entry);
                zipOutStream.closeEntry();
            } else {
                for (File tmp : files) {
                    zipFile(root, relativeName + File.separator + tmp.getName(), zipOutStream);
                }
            }
        }
    }

}