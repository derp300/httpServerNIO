package httpserver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileReader {
    private static httpserver.FileWatcher watcher;

    private static Map<Path, byte[]> cache;
    static {
        cache = new HashMap<>();
    }

    private static Map<Path, Boolean> fileIsModify;
    static {
        fileIsModify = new HashMap<>();
    }

    private static Set<String> cachingTypes;
    static {
        cachingTypes = new HashSet<>();
    }

    private static String rootDir;

    private static Map<String, String> contentTypesMap;
    static {
        contentTypesMap = new HashMap<>();
        contentTypesMap.put("js", "application/javascript");
        contentTypesMap.put("html", "text/html");
        contentTypesMap.put("jpg", "image/jpg");
        contentTypesMap.put("jpeg", "image/jpeg");
    }
    
    public static void init(String rootDir, String cachingTypesConf) {
        FileReader.rootDir = rootDir;
        
        if(cachingTypesConf.equals("*"))
            cachingTypes.add("*");
        else {
            for(String str: cachingTypesConf.split(",")) {
                cachingTypes.add(contentTypesMap.get(str));
            }
        }
        
        try {
            watcher = new FileWatcher(Paths.get(rootDir), cache, fileIsModify);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] getFile(String filename, String contentType) throws IOException {
        Path filePath = Paths.get(rootDir + filename.substring(1));
        watcher.check();
        if(!isCaching(contentType) || fileIsModify.get(filePath) == null || fileIsModify.get(filePath)) {
            byte[] file;
            if (contentType.equals("application/javascript") || contentType.equals("text/html")) {
                file = readTextFile(filename);
            } else {
                Pattern p = Pattern.compile("^image/.*");
                Matcher m = p.matcher(contentType);

                if (m.find()) {
                    p = Pattern.compile("\\w*$");
                    m = p.matcher(contentType);
                    if (m.find()) {
                        file = readImageFile(filename, m.group());
                    } else {
                        throw new IOException();
                    }
                } else {
                    throw new IOException();
                }
            }

            fileIsModify.put(filePath, false);
            cache.put(filePath, file);
            return file;
        } else {
            System.out.println("Using cache");
            return cache.get(filePath);
        }
    }

    private static boolean isCaching(String contentType) {
        return cachingTypes.contains("*") || cachingTypes.contains(contentType);
    }

    public static String getContentType(String filename) {
        String contentType = null;

        Pattern p = Pattern.compile("\\.\\w*$");
        Matcher m = p.matcher(filename);

        if(m.find())
            contentType = contentTypesMap.get(m.group().substring(1));

        return contentType;
    }

    private static byte[] readImageFile(String pathname, String format) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            BufferedImage img = ImageIO.read(new File(rootDir + pathname.substring(1)));
            ImageIO.write(img, format, out);
            out.flush();
    
            byte[] bytes = out.toByteArray();
            return bytes;
        } catch (IOException e) {
            System.out.println(e.toString());
            return null;
        }
    }

    private static byte[] readTextFile(String pathname) throws  IOException{
        try {
            return Files.readAllBytes(Paths.get(rootDir + pathname.substring(1)));
        } catch (IOException e) {
            System.out.println(e.toString());
            return null;
        }

    }

}
