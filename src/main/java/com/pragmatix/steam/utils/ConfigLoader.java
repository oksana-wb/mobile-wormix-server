package com.pragmatix.steam.utils;

import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Author: Vladimir
 * Date: 21.11.13 10:01
 */
public strictfp class ConfigLoader {

    public static Object loadFromString(String json) {
        try {
            return new JSONParser().parse(new StringReader(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadConfigs(String sourcePath, Pattern fileNameFilter, ConfigProcessor configProcessor) {
        try {
            String[] pathList;

            File file = getResourceFile(sourcePath);

            if (file.exists() && file.isDirectory()) {
                pathList = listPackageResources(sourcePath, fileNameFilter, true);
            } else {
                pathList = new String[]{sourcePath};
            }

            for (String path : pathList) {
                configProcessor.processResource(path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map> loadConfigs(String sourcePath) {
        String[] pathList;

        File file = getResourceFile(sourcePath);

        if (file.isDirectory()) {
            pathList = listPackageResources(sourcePath, Pattern.compile(".+\\.json$", Pattern.DOTALL), true);
        } else {
            pathList = new String[]{sourcePath};
        }

        List<Map> res = new ArrayList<>();

        for (String path : pathList) {
            try {
                Object cfg = loadFromStream(openResourceStream(path));
                if (cfg instanceof Map) {
                    res.add((Map) cfg);
                } else {
                    res.addAll((List<Map>) cfg);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error in " + path, e);
            }
        }

        return res;
    }

    private static final Pattern jsonFileFilter = Pattern.compile(".+\\.json$", Pattern.DOTALL);

    public static List<String[]> listJsonFiles(String rootUrl) {
        return listFiles(rootUrl, jsonFileFilter);
    }

    public static List<String[]> listFiles(String rootUrl, Pattern filter) {
        return listFiles(getResourceFile(rootUrl), null, filter, new ArrayList<>());
    }

    public static List<String[]> listFiles(File root, String defPath, Pattern filter, List<String[]> res) {
        if (!root.exists()) {
            throw new IllegalArgumentException("Root not exists " + root);
        }
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("Root is not a directory " + root);
        }

        String[] fileNames = root.list();

        assert fileNames != null;

        Arrays.sort(fileNames);

        for (String fileName: fileNames) {
            File file = new File(root, fileName);
            String fileDef;
            if (defPath != null && defPath.length() > 0) {
                fileDef = defPath + "/" + fileName;
            } else {
                fileDef = fileName;
            }
            if (file.isDirectory()) {
                listFiles(file, fileDef, filter, res);
            } else if (filter.matcher(fileName).matches()) {
                int i = fileDef.lastIndexOf('.');
                if (i >= 0) {
                    fileDef = fileDef.substring(0, i);
                }
                res.add(new String[] {fileDef, file.getAbsolutePath()});
            }
        }

        return res;
    }

    public static List<Map> loadConfigsFromFile(String sourcePath) {
        try {
            return (List<Map>) loadFromStream(new FileInputStream(sourcePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map loadConfig(String sourcePath) {
        try {
            return (Map) loadFromStream(openResourceStream(sourcePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object loadFromStream(InputStream stream) {
        try {
            Object res = new JSONParser().parse(new InputStreamReader(stream, "UTF-8"));
            stream.close();
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String[] listPackageResources(String packageName, Pattern filter, boolean deep) {
        try {
            packageName = packageName.replace('.', '/');

            File dir = getResourceFile(packageName);

            if (dir.isDirectory()) {
                List<String> res = findClasses(dir, packageName, filter, deep);

                return res.toArray(new String[res.size()]);
            } else {
                throw new RuntimeException("Resource not exists " + packageName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream openResourceStream(String path) {
        try {
            return getResourceUrl(path).openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getResourceFile(String path) {
        return new File(getResourceUrl(path).getFile());
    }

    private static URL getResourceUrl(String path) {
        final String classPathPx = "classpath:";
        URL resource;
        try {
            if (path.startsWith(classPathPx)) {
                path = path.substring(classPathPx.length());
            }
            URI uri = new URI(path);
            String scheme = uri.getScheme();
            if (scheme == null) {
                resource = getClassLoader().getResource(path);
            } else {
                resource = uri.toURL();
            }

            if (resource == null) {
                throw new IllegalArgumentException("Resource not exists " + path);
            }

            return resource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> findClasses(File directory, String packageName, Pattern filter, boolean deep) throws ClassNotFoundException {
        List<String> classes = new ArrayList<>();
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!deep) {
                            continue;
                        }
                        assert !file.getName().contains(".");
                        classes.addAll(findClasses(file, fullClassName(packageName, file.getName()), filter, deep));
                    } else if (filter == null || filter.matcher(file.getName()).matches()) {
                        classes.add(resourceName(packageName, file.getName()));
                    }
                }
            }
        }
        return classes;
    }

    private static String fullClassName(String packageName, String className) {
        return packageName.length() > 0 ? packageName + "." + className : className;
    }

    private static String resourceName(String packageName, String className) {
        if (packageName.length() > 0) {
            return packageName.replaceAll("\\.", "/") + "/" + className;
        } else {
            return className;
        }
    }

    private static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
