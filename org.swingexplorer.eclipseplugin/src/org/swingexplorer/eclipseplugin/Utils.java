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
package org.swingexplorer.eclipseplugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class Utils {
    
    
    static Bundle bundle;
    static ILog log;
    
    public static ILog getLog() {
        if(log == null) {
            Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
            log = Platform.getLog(bundle);
        }
        return log;
    }
    
    
    public static void logInfo(String msg) {
        Status status = new Status(Status.INFO, Activator.PLUGIN_ID, 0, msg, null);
        getLog().log(status);
    }
    
    public static void logWarning(String msg) {
        Status status = new Status(Status.WARNING, Activator.PLUGIN_ID, 0, msg, null);
        getLog().log(status);
    }
    
    public static void logError(String msg, Exception ex) {
        Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, 0, msg, ex);
        getLog().log(status);
    }
    
    /**
     * Returns full path of a resource from plugin's bundle.
     * @param fileName
     * @return
     * @throws CoreException
     */
    public static String getFullPathOf(String fileName) throws CoreException{
        // obtain URL of inside plugin
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        URL  swexplUrl = bundle.getEntry(fileName);
        
        try {
            String convertedFile = FileLocator.toFileURL(swexplUrl).getFile();
            
            // convert to OS specific
            return new File(convertedFile).getAbsolutePath();
        } catch (IOException e) {
            Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, 0, "Can not find path to swexpl.jar file", e);
            throw new CoreException(status);
        }
    }
}