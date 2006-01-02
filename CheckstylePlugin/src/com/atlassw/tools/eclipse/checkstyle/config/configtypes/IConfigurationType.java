//============================================================================
//
// Copyright (C) 2002-2005  David Schneider, Lars K�dderitzsch
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//============================================================================

package com.atlassw.tools.eclipse.checkstyle.config.configtypes;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.swt.graphics.Image;

import com.atlassw.tools.eclipse.checkstyle.config.ICheckConfiguration;
import com.atlassw.tools.eclipse.checkstyle.util.CheckstylePluginException;
import com.puppycrawl.tools.checkstyle.PropertyResolver;

/**
 * Interface for a configuration type.
 * 
 * @author Lars K�dderitzsch
 */
public interface IConfigurationType
{

    /**
     * Initializes the configuration type.
     * 
     * @param name the displayable name of the configuration type
     * @param internalName the internal name of the configuration type
     * @param editorClass the properties editor class
     * @param image the image of the configuration type
     * @param definingPluginId the plugin id the configuration type was defined
     *            in
     * @param isCreatable <code>true</code> if a configuration of this type
     *            can be created by the user.
     * @param isEditable <code>true</code> if a configuration of this type can
     *            be edited by the user.
     * @param isConfigurable <code>true</code> if a configuration of this type
     *            can be configured by the user.
     */
    void initialize(String name, String internalName, Class editorClass, String image,
            String definingPluginId, boolean isCreatable, boolean isEditable, boolean isConfigurable);

    /**
     * The displayable name of the configuration type.
     * 
     * @return the displayable name
     */
    String getName();

    /**
     * Returns the internal name of the configuration type.
     * 
     * @return the internal name
     */
    String getInternalName();

    /**
     * Returns the class of the configuration types properties editor.
     * 
     * @return the class of the properties editor
     */
    Class getLocationEditorClass();

    /**
     * Returns an image that depicts the configuration type.
     * 
     * @return the image of the configuration type
     */
    Image getTypeImage();

    /**
     * Return if a check configuration of this type can be created by the user.
     * 
     * @return <code>true</code> if the check configuration type is creatable,
     *         otherwise <code>false</code>
     */
    boolean isCreatable();

    /**
     * Determines if the configuration properties are editable by the user.
     * 
     * @return <code>true</code>, if the configuration is editable
     */
    boolean isEditable();

    /**
     * Determines if the checkstyle configuration associates with this check
     * configuration can be configured.
     * 
     * @param checkConfiguration the actual check configuration
     * @return <code>true</code> if the checkstyle configuration can be
     *         configured.
     */
    boolean isConfigurable(ICheckConfiguration checkConfiguration);

    /**
     * Gets the property resolver for this configuration type used to expand
     * property values within the checkstyle configuration.
     * 
     * @param checkConfiguration the actual check configuration
     * @return the property resolver
     * @throws CheckstylePluginException error creating the property resolver
     */
    PropertyResolver getPropertyResolver(ICheckConfiguration checkConfiguration)
        throws CheckstylePluginException;

    /**
     * Returns the URL of the checkstyle configuration file.
     * 
     * @param checkConfiguration the actual check configuration
     * @return the URL of the checkstyle configuration file
     * @throws CheckstylePluginException error while creating the url
     */
    URL resolveLocation(ICheckConfiguration checkConfiguration) throws CheckstylePluginException;

    /**
     * Opens an input stream to the Checkstyle configuration file.
     * 
     * @param checkConfiguration the actual check configuration
     * @return input stream to the Checkstyle configuration file
     * @throws CheckstylePluginException error opening the stream
     */
    InputStream openConfigurationFileStream(ICheckConfiguration checkConfiguration)
        throws CheckstylePluginException;

    /**
     * Notifies that a check configuration has been removed.
     * 
     * @param checkConfiguration the check configuration which was removed
     * @throws CheckstylePluginException error while processing the notification
     */
    void notifyCheckConfigRemoved(ICheckConfiguration checkConfiguration)
        throws CheckstylePluginException;
}