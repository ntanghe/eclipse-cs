//============================================================================
//
// Copyright (C) 2002-2007  David Schneider, Lars K�dderitzsch
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

package net.sf.eclipsecs.core.config.configtypes;

import java.net.URL;

import net.sf.eclipsecs.core.CheckstylePlugin;
import net.sf.eclipsecs.core.config.CheckstyleConfigurationFile;
import net.sf.eclipsecs.core.config.ICheckConfiguration;

import org.eclipse.core.runtime.Path;

import com.puppycrawl.tools.checkstyle.PropertyResolver;

/**
 * Implementation of the configuration type for a built in check configuration,
 * that is located inside the plugin.
 * 
 * @author Lars K�dderitzsch
 */
public class BuiltInConfigurationType extends ConfigurationType {

    /**
     * {@inheritDoc}
     */
    protected URL resolveLocation(ICheckConfiguration checkConfiguration) {
        return CheckstylePlugin.getDefault().find(new Path(checkConfiguration.getLocation()));
    }

    /**
     * {@inheritDoc}
     */
    protected byte[] getAdditionPropertiesBundleBytes(URL checkConfigURL) {
        // just returns null since additional property file is not needed nor
        // supported
        return null;
    }

    protected PropertyResolver getPropertyResolver(ICheckConfiguration config,
            CheckstyleConfigurationFile configFile) {
        // no properties to resolve with builtin configurations
        return null;
    }
}