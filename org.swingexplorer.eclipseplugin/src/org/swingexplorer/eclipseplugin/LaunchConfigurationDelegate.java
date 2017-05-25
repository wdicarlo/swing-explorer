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
import java.text.MessageFormat;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


public class LaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        
        
        monitor.beginTask("Launching applcation with Swing Explorer", 10);
        
        monitor.subTask("Launch");
        IVMInstall install= getVMInstall(configuration);
        IVMRunner runner = install.getVMRunner(mode);
        if (runner == null) {
            abort("Runner does not exist", null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST); 
        }
        
        // run application
        int port= SocketUtil.findFreePort();
        VMRunnerConfiguration runConfig= createRunnerConfiguration(configuration, port);
        setDefaultSourceLocator(launch, configuration);
        runner.run(runConfig, launch, monitor);
                        
        monitor.worked(3);
        
        while(!launch.isTerminated()) {
            try {
                
                monitor.subTask("Waiting for launching");
                Thread.sleep(3000);
                monitor.worked(5);
    
                Utils.logInfo("Trying to connect to Swing Explorer launch");
                JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + port + "/server");
                JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
                MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
                
                ObjectName name = new ObjectName("org.swingexplorer:name=IDESupport");
                
                // notify MBean that IDE connected it
                mbsc.invoke(name, "connect", new Object[0], new String[0]);
                
                // create action
                IJavaProject javaProject= getJavaProject(configuration);
                final ActOpenSourceLine actOpenSourceLine = new ActOpenSourceLine(javaProject, launch);
                
                // add action as listener
                mbsc.addNotificationListener(name, actOpenSourceLine, null, null);
                Utils.logInfo("The connection to Swing Explorer launch established");
                
                break; // connected break loop
            } catch (Exception e) {
                Utils.logInfo("Connect to Swing Explorer launch failed. Another attempt will be made");
            } 
        }
        
        monitor.done();
    }
    
    protected VMRunnerConfiguration createRunnerConfiguration(ILaunchConfiguration configuration, int port) throws CoreException {
        File workingDir = verifyWorkingDirectory(configuration);
        String workingDirName = null;
        if (workingDir != null) 
            workingDirName = workingDir.getAbsolutePath();
        
        String[] classPath = getClasspath(configuration);
        
        // add swexpl.jar to classpath
        String[] newClassPath = new String[classPath.length + 1];
        System.arraycopy(classPath, 0, newClassPath, 0, classPath.length);
        newClassPath[newClassPath.length - 1] = Utils.getFullPathOf("swexpl.jar");
        classPath = newClassPath;
        
        VMRunnerConfiguration runConfig= new VMRunnerConfiguration("org.swingexplorer.Launcher", classPath); //$NON-NLS-1$
        String progArgs= getProgramArguments(configuration);
        
        // adding debugged application's main type
        String mainType = getMainTypeName(configuration);
        progArgs = mainType + " " + progArgs;
        
        // adding agent and boot
        String jvmArgs = MessageFormat.format("-javaagent:\"{0}\" -Xbootclasspath/a:\"{0}\" -Dswex.mport={1} -Dcom.sun.management.jmxremote {2}", 
                        Utils.getFullPathOf("swag.jar"),
                        String.valueOf(port),
                        getVMArguments(configuration));
        
        
        // Program & VM args
        ExecutionArguments execArgs = new ExecutionArguments(jvmArgs, progArgs); //$NON-NLS-1$
        String[] args= execArgs.getProgramArgumentsArray();
        runConfig.setProgramArguments(args);
        runConfig.setVMArguments(execArgs.getVMArgumentsArray());
        runConfig.setWorkingDirectory(workingDirName);
        runConfig.setEnvironment(getEnvironment(configuration));

        Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
        runConfig.setVMSpecificAttributesMap(vmAttributesMap);

        String[] bootpath = getBootpath(configuration);
        runConfig.setBootClassPath(bootpath);
        
        return runConfig;
    }
}