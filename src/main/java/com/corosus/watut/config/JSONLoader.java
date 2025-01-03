package com.corosus.watut.config;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.JsonObjects.GuiOverrideConfigs;
import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class JSONLoader {

    String jarPathConfigGuiOverrides = "/config/watut/gui/";
    String filesystemPathConfigGuiOverrides = "." + jarPathConfigGuiOverrides;

    public File dataFolder = new File(filesystemPathConfigGuiOverrides);
    public File dataHashes = new File(filesystemPathConfigGuiOverrides + "filehashes.txt");

    Gson gson = new GsonBuilder().registerTypeAdapter(ScreenRule.class, new ScreenRuleDeserializer()).create();
    AllGuiOverrideConfigs allGuiOverrideConfigs = new AllGuiOverrideConfigs();

    public static boolean DEBUG_FORCE_CONFIG_REGEN = true;

    private static JSONLoader instance;

    public static JSONLoader getInstance() {
        if (instance == null) {
            instance = new JSONLoader();
        }
        return instance;
    }

    public void generateDataTemplates() {

        dataFolder.mkdirs();

        List<Path> listFiles = new ArrayList<>();
        List<String> listFileHashes = new ArrayList<>();

        try {
            listFiles = getFileListFromJarPath(jarPathConfigGuiOverrides);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (Path file : listFiles) {
            listFileHashes.add(copyFileFromJarPath(file.toString()));
        }

        String hashFileContents = "";
        for (String hash : listFileHashes) {
            hashFileContents += hash + "@@@";
        }

        try {
            FileUtils.writeStringToFile(dataHashes, hashFileContents, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String copyFileFromJarPath(String path) {
        String md5 = "";
        try {

            String fileContents = getContentsFromResourceLocation(path);

            if (!fileContents.equals("")) {
                File fileOut = new File("./" + path);
                CULog.dbg("copying " + path.substring(path.lastIndexOf("/")+1).toString() + " to " + fileOut.toString());
                FileUtils.writeStringToFile(fileOut, fileContents, StandardCharsets.UTF_8);
                md5 = getMD5(fileOut);
            } else {
                CULog.err("couldnt get contents of file: " + path);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return md5;
    }

    public String getContentsFromResourceLocation(String path) {
        try {
            InputStream in = JSONLoader.class.getClassLoader().getResourceAsStream(path);
            String contents = IOUtils.toString(in, StandardCharsets.UTF_8);
            return contents;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public String getMD5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return String.format("%016x", ByteBuffer.wrap(digest.digest(IOUtils.toByteArray(new FileInputStream(file)))).getLong());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public List<Path> getFileListFromJarPath(String path) throws URISyntaxException, IOException {
        List<Path> files = new ArrayList<>();
        URI uri = JSONLoader.class.getResource(path).toURI();
        Path myPath;
        if (uri.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
            myPath = fileSystem.getPath(path);
        } else {
            myPath = Paths.get(uri);
        }
        Stream<Path> walk = Files.walk(myPath, 100);
        for (Iterator<Path> it = walk.iterator(); it.hasNext();){
            Path child = it.next();
            if (child.toString().endsWith(".json")) {
                files.add(child);
            }
        }
        return files;
    }

    public void loadFiles() {

        if (!dataFolder.exists() || dataFolder.listFiles().length == 0 || DEBUG_FORCE_CONFIG_REGEN || isTemplatesUnchanged()) {
            CULog.log("Detected coroutil json data missing or unchanged from previous generation, generating from templates");
            generateDataTemplates();
        } else {
            CULog.dbg("Preserving existing configs as they have been changed since generation");
        }

        if (dataFolder.exists()) {
            processFolder(dataFolder);
        } else {
            CULog.err("CRITICAL Error generating data folder");
        }
    }


    /**
     * Note: if a file on the filesystem is missing, it still wont generate new files, because the user might not want that file there
     * we only count it true if there is a perfect match between jar folders and filesystem folders
     * @return
     */
    public boolean isTemplatesUnchanged() {

        try {
            //if hash file missing, force a regen
            if (!dataHashes.exists()) return true;
            String fileContentsHash = com.google.common.io.Files.toString(dataHashes, Charsets.UTF_8);
            String[] hashes = fileContentsHash.split("@@@");
            List<File> listFiles = getFiles(dataFolder);
            //if mismatch in file count, consider it modified
            if (hashes.length != listFiles.size()) {
                CULog.dbg("Detected file count mismatch: " + hashes.length + " vs " + listFiles.size());
                return false;
            }
            for (File child : listFiles) {
                String hash = getMD5(child);
                boolean found = false;
                for (String hashTry : hashes) {
                    if (hash.equals(hashTry)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    CULog.dbg("Detected file changed from last template generation: " + child);
                    return false;
                }
            }
            CULog.dbg("Detected no files changed in filesystem, allowing template regen");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void processFileFromFilesystem(File file) {
        try {

            FileReader reader = new FileReader(file);
            GuiOverrideConfigs guiOverrideConfigs = gson.fromJson(reader, GuiOverrideConfigs.class);
            guiOverrideConfigs.setPath(file.getPath());
            allGuiOverrideConfigs.generateLookups(guiOverrideConfigs);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void processFolder(File path) {
        for (File child : path.listFiles()) {
            if (child.isFile()) {
                try {
                    if (child.toString().endsWith(".json")) {
                        processFileFromFilesystem(child);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                processFolder(child);
            }
        }
    }

    public static List<File> getFiles(File file) {
        List<File> listFiles = new ArrayList<>();
        for (File child : file.listFiles()) {
            if (child.isFile()) {
                try {
                    if (child.toString().endsWith(".json")) {
                        listFiles.add(child);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                listFiles.addAll(getFiles(child));
            }
        }
        return listFiles;
    }

    public AllGuiOverrideConfigs getAllGuiOverrideConfigs() {
        return allGuiOverrideConfigs;
    }
}
