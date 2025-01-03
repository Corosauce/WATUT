package com.corosus.watut.config;

import net.minecraftforge.fml.ModContainer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JarCrawler {

    /*public static boolean findFiles(ModContainer mod, String base, Function<Path, Boolean> preprocessor, BiFunction<Path, Path, Boolean> processor, boolean defaultUnfoundRoot, boolean visitAllFiles)
    {
        FileSystem fs = null;
        try
        {
            File source = mod.getSource();

            if ("minecraft".equals(mod.getModId()))
            {
                if (!DEBUG_LOAD_MINECRAFT)
                    return true;

                try
                {
                    URI tmp = JarCrawler.class.getResource("/assets/.mcassetsroot").toURI();
                    source = new File(tmp.resolve("..").getPath());
                }
                catch (URISyntaxException e)
                {
                    FMLLog.log.error("Error finding Minecraft jar: ", e);
                    return false;
                }
            }

            Path root = null;
            if (source.isFile())
            {
                try
                {
                    fs = FileSystems.newFileSystem(source.toPath(), (ClassLoader) null);
                    root = fs.getPath("/" + base);
                }
                catch (IOException e)
                {
                    FMLLog.log.error("Error loading FileSystem from jar: ", e);
                    return false;
                }
            }
            else if (source.isDirectory())
            {
                root = source.toPath().resolve(base);
            }

            if (root == null || !Files.exists(root))
                return defaultUnfoundRoot;

            if (preprocessor != null)
            {
                Boolean cont = preprocessor.apply(root);
                if (cont == null || !cont.booleanValue())
                    return false;
            }

            boolean success = true;

            if (processor != null)
            {
                Iterator<Path> itr = null;
                try
                {
                    itr = Files.walk(root).iterator();
                }
                catch (IOException e)
                {
                    FMLLog.log.error("Error iterating filesystem for: {}", mod.getModId(), e);
                    return false;
                }

                while (itr != null && itr.hasNext())
                {
                    Boolean cont = processor.apply(root, itr.next());

                    if (visitAllFiles)
                    {
                        success &= cont != null && cont;
                    }
                    else if (cont == null || !cont)
                    {
                        return false;
                    }
                }
            }

            return success;
        }
        finally
        {
            IOUtils.closeQuietly(fs);
        }
    }*/

}
