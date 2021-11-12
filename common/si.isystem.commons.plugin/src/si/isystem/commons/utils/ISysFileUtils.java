package si.isystem.commons.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import si.isystem.exceptions.SIOException;


public class ISysFileUtils {

    /**
     * This method copies file from the given plug-in to location on local drive.
     *  
     * @param srcDir source directory relative to this plug-in, for example 'templates' 
     * @param destDir destination folder
     * @param fileName file to copy
     * @param copyOption if set to StandardCopyOption.REPLACE_EXISTING, then 
     *                   destination file is overwritten if it exists. May be null.
     * @param pluginId id of the plug-in to copy file from, for example Activator.PLUGIN_ID
     * 
     * @throws IOException
     * 
     * @return true, if file was copied, false if file was not copied because it
     *               already exists - call this method with StandardCopyOption.REPLACE_EXISTING
     *               to force overwrite.
     */
    public static boolean copyFileFromPlugin(String srcDir, 
                                             File destDir, 
                                             String fileName,
                                             CopyOption copyOption,
                                             String pluginID) {
        
        Bundle bundle = Platform.getBundle(pluginID);
        URL srcUri = bundle.getEntry(srcDir + '/' + fileName);
        if (srcUri == null) {
            throw new IllegalArgumentException("File not found in plug-in: '" + fileName + "'");
        }
        
        Path destPath = Paths.get(destDir.getAbsolutePath(), fileName);
        
        if (Files.exists(destPath)  &&  copyOption != StandardCopyOption.REPLACE_EXISTING) {
            return false;
        }
        
        
        try (InputStream srcStream = srcUri.openStream();) 
        {
            Files.copy(srcStream, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new SIOException("Can not copy file from plugin!").
                add("srcDir", srcDir).
                add("destDir", destDir).
                add("fileName", fileName).
                add("copyPption", copyOption);
        }
        
        return true;
        // copyFile(uri, destPath.toAbsolutePath().toString());
    }

    
    /**
     * Copies file from plug-in jar file to destPath.
     * 
     * @param srcUri uri of file in plug-in jar file, for example:
     *         Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
     *         URL uri = bundle.getEntry(pluginSrcDir + '/' + pluginFileName);
     *
     * @param destPath location on local drive
     * 
     * @throws IOException
     */
    public static void copyFileFromPlugin(URL srcUri, String destPath) throws IOException {
        try (   BufferedReader reader = new BufferedReader(new InputStreamReader(srcUri.openStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destPath)))) {
            String line = null;
            do {
                line = reader.readLine();
                if (line != null) {
                    writer.write(line);
                    writer.write('\n');
                }
            } while (line != null);
        }
    }
    
    
    /**
     * Replaces '\' with '\\', and escapes chars '[]$' in path, so that these
     * chars do not collide with reg ex special chars.
     *  
     * @param path
     * @return
     */
    static public String pathForRegEx(String path) {
        return path.replace("\\", "\\\\").replace("$", "\\$").replace("[", "\\[").replace("]", "\\]");
    }
}
