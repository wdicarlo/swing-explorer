/*
 *   Swing Explorer. Tool for developers exploring Java/Swing-based application internals. 
 * 	 Copyright (C) 2012, Maxim Zakharenkov
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *   
 */
package org.swingexplorer.install;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    private static final Logger log = Logger.getLogger(Installer.class.getName());
    
    
     /**
	 * Returns version-specific directory where Swing Explorer
	 * stores own files.
	 * @param autoCreate - indecates if directory should be created if it does
     * not exist
	 * @return full path
	 */
	public static String getHomeDirectory(boolean autoCreate) {

		// the exactly same implementation of this method is
		// also presented in the both in the SysUtils class
		// and in the NB plug-in's Installer class
		// making any changes here please also copy them
		// into both places


		// obtain version, if version not found - we are in development mode
		String version = Installer.class.getPackage().getImplementationVersion();
        if(version == null) {
            version = "0.0.0";// development version
        }

        String strDir =  System.getProperty("user.home") + "/.swex/" + version;

        // auto-create if necessary
		if(autoCreate) {
			File dir = new File(strDir);
			if(!dir.exists()) {
				dir.mkdirs();
			}
		}
		return strDir;
	}

    
    @Override
    public void restored() {
        String swexHomeDir = getHomeDirectory(true);
        try {
            unpackToHome(swexHomeDir, "swexpl.jar");
            unpackToHome(swexHomeDir, "swag.jar");            
        } catch(IOException ex) {
            log.log(Level.SEVERE, "Error unpacking Swing Explorer runtime file", ex);
        }
    }
    
    /**
     * Unpacks a file stored inside org-netbeans-swingexplorerplugin.jar module
     * into home directory of Swing explorer.
     */
    void unpackToHome(String swexHomeDir, String fileName) throws IOException {
        if(new File(swexHomeDir, fileName).exists()) { 
            log.info("File " + fileName + " already exists in the " + swexHomeDir);
        }        
       
        
        String pluginModule = "modules/org-swingexplorer-netbeans.jar";
        
        log.info("Unpacking " + fileName + " from " + pluginModule + " into" + swexHomeDir);
        File file = InstalledFileLocator.getDefault().locate(pluginModule, "org.swingexplorer.netbeans", false);
        JarFile jarFile = new JarFile(file);

        // find file in JAR
        JarEntry entry = jarFile.getJarEntry("org/swingexplorer/install/" + fileName);
        InputStream is = jarFile.getInputStream(entry);
        
        // Copy JAR to file
        FileOutputStream fos = new FileOutputStream(swexHomeDir + "/" + fileName);
        int count = 0;
        byte[] b = new byte[1024];
        while((count = is.read(b)) != -1) {
            fos.write(b, 0, count);
        }

        log.info("Unpacking of " + fileName + " from " + pluginModule + "  into " + swexHomeDir + " complete successfully");
        
        fos.close();
        is.close();
    }
}
