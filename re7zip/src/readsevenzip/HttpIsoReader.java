/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package readsevenzip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

/**
 *
 * @author wael
 */
public class HttpIsoReader {

    private static void usage() {
        System.out.println("Usage java - jar re7zip.jar [/x=FILE_TYPE] /s=URL /f=[FILE PATH IN ARCHIEVE] /t=[TARGET FILE PATH] ");
        System.out.println("\tSupported file types are " + Arrays.toString(ArchiveFormat.values()));
    }
    ISevenZipInArchive archive;

    public Boolean open(String URL) {
        return open(URL, null);
    }

    public Boolean open(String URL, ArchiveFormat type) {
        try {
            archive = SevenZip.openInArchive(type, new MonitorIInStream(new HttpIInStream(URL)));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Boolean open(File file) {
        return open(file, null);
    }

    public Boolean open(File file, ArchiveFormat type) {
        try {
            archive = SevenZip.openInArchive(type,
                    new MonitorIInStream(
                    new RandomAccessFileInStream(
                    new RandomAccessFile(file, "r"))));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void getFile(String sourceFilePath, String targetFilePath) throws IOException, SevenZipException {

        File f = new File(targetFilePath);
        final FileOutputStream fos = new FileOutputStream(f);
        try {
            int index = lookupIndex(sourceFilePath);
            if (index == -1) {
                throw new SevenZipException("File [" + sourceFilePath + "]is not found");
            }
            ISimpleInArchiveItem item = archive.getSimpleInterface().getArchiveItem(index);

            final int[] hash = new int[]{0};
            if (!item.isFolder()) {
                ExtractOperationResult result;
                final long[] sizeArray = new long[1];
                final Long size = item.getSize();

                result = item.extractSlow(new ISequentialOutStream() {

                    long loaded = 0;

                    public int write(byte[] data) throws SevenZipException {
                        try {
                            //                        hash[0] ^= Arrays.hashCode(data); // Consume data
                            //                        sizeArray[0] += data.length;
                            fos.write(data);
                            loaded += data.length;
                            
                            System.out.println(String.format("Percentage : %3s Reading %9s of %9s bytes",
                                    (loaded * 100) / size, loaded, size));

                            return data.length; // Return amount of consumed data
                        } catch (IOException ex) {
                            throw new SevenZipException(ex);
                        }
                    }
                });
                if (result == ExtractOperationResult.OK) {
                    fos.flush();
                    fos.close();
                } else {
                    System.err.println("Error extracting item: " + result);
                }
            }
        } catch (IOException ex) {
            throw new SevenZipException(ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                throw new SevenZipException(ex);
            }
        }
    }

    public void close() {
        try {
            archive.close();
        } catch (SevenZipException ex) {
            Logger.getLogger(HttpIsoReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int lookupIndex(String sourceFilePath) throws SevenZipException {
        int numberOfItems = archive.getNumberOfItems();
        for (int i = 0; i < numberOfItems; i++) {
            if (archive.getSimpleInterface().getArchiveItem(i).getPath().equals(sourceFilePath)) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] a) throws Exception {
        String source = null;
        ArchiveFormat type = null;
        String file = null;
        String target = null;

        source = "http://msft-dnl.digitalrivercontent.net/msvista/pub/X15-65805/X15-65805.iso";
        type = ArchiveFormat.UDF;
        file = "boot\\fonts\\chs_boot.ttf";
        target = "chs_boot.ttf";

        for (String arg : a) {
            if (arg.toLowerCase().startsWith("/x=")) {
                try {
                    type = ArchiveFormat.valueOf(arg.substring(3).toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid File Type : " + arg.substring(3).toUpperCase());
                    usage();
                    return;
                }
            }
            if (arg.toLowerCase().startsWith("/s=")) {
                source = arg.substring(3);
            }
            if (arg.toLowerCase().startsWith("/f=")) {
                file = arg.substring(3);
            }
            if (arg.toLowerCase().startsWith("/t=")) {
                target = arg.substring(3);
            }

        }

        if (file == null || source == null || target == null) {
            usage();
            return;
        }
        long start = System.currentTimeMillis();

        HttpIsoReader reader = new HttpIsoReader();
        Boolean result = false;
        if (source.toLowerCase().startsWith("http://")) {
            System.out.println("Openning HTTP Archive " + source);
            result = reader.open(source, type);
        } else {
            System.out.println("Openning FILE Archive " + source);
            try{
                result = reader.open(new File(source), type);
            }
            catch(Exception e){
                System.out.println("Invalid file "+source);
                usage();
                return;
            }
        }
        if(result){
            System.out.println("Archive is open");
        }
        else{
            System.out.println("Failed to open archive ");
            return;
        }
        
        System.out.println("Extracting file");
        reader.getFile(file, target);
        reader.close();
        long end = System.currentTimeMillis();
        long time = (end - start) / 1000;

        System.out.println("Done. Downloaded in " + (time / 60) + " minutes and " + (time % 60) + " seconds");
    }
}
