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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * This property resolver is able to aggregate a list of child property
 * resolvers, where each child resolver looks for different properties and may
 * have different ways of finding properties. The child resolvers are asked to
 * resolve the properties in the order they are added. This PropertyResolver
 * adds the property chaining feature, to allow properties within properties.
 * 
 * @author Lars K�dderitzsch
 */
public class MultiPropertyResolver implements PropertyResolver, IContextAware
{

    //
    // attributes
    //

    /** The list of PropertyResolvers. */
    private List mChildResolver = new ArrayList();

    //
    // methods
    //

    /**
     * Adds a PropertyResolver to this aggregation property resolver.
     * 
     * @param resolver the PropertyResolver to add
     */
    public void addPropertyResolver(PropertyResolver resolver)
    {
        mChildResolver.add(resolver);
    }

    /**
     * @see IContextAware#setProjectContext(org.eclipse.core.resources.IProject)
     */
    public void setProjectContext(IProject project)
    {

        // propagate context to the childs
        for (int i = 0, size = mChildResolver.size(); i < size; i++)
        {
            PropertyResolver aChildResolver = (PropertyResolver) mChildResolver.get(i);
            if (aChildResolver instanceof IContextAware)
            {
                ((IContextAware) aChildResolver).setProjectContext(project);
            }
        }
    }

    /**
     * @see com.puppycrawl.tools.checkstyle.PropertyResolver#resolve(java.lang.String)
     */
    public String resolve(String property) throws CheckstyleException
    {

        String value = null;

        for (int i = 0, size = mChildResolver.size(); i < size; i++)
        {

            PropertyResolver aChildResolver = (PropertyResolver) mChildResolver.get(i);
            value = aChildResolver.resolve(property);

            if (value != null)
            {
                break;
            }
        }

        // property chaining - might recurse internally
        while (hasUnresolvedProperties(value))
        {
            value = replaceProperties(value, this);
        }

        return value;
    }

    /**
     * Checks if the current value has unresolved properties.
     * 
     * @param value the value
     * @return the resolved property
     * @throws CheckstyleException Syntax exception in a property declaration
     */
    private boolean hasUnresolvedProperties(String value) throws CheckstyleException
    {
        if (value != null)
        {
            List props = new ArrayList();
            parsePropertyString(value, new ArrayList(), props);
            return !props.isEmpty();
        }
        else
        {
            return false;
        }
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value
     * with the string value of the corresponding data types.
     * 
     * The method is package visible to facilitate testing.
     * 
     * @param aValue The string to be scanned for property references. May be
     *            <code>null</code>, in which case this method returns
     *            immediately with no effect.
     * @param aProps Mapping (String to String) of property names to their
     *            values. Must not be <code>null</code>.
     * @param aDefaultValue default to use if one of the properties in aValue
     *            cannot be resolved from aProps.
     * 
     * @throws CheckstyleException if the string contains an opening
     *             <code>${</code> without a closing <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     * 
     * Code copied from ant -
     * http://cvs.apache.org/viewcvs/jakarta-ant/src/main/org/apache/tools/ant/ProjectHelper.java
     */
    // Package visible for testing purposes
    static String replaceProperties(String aValue, PropertyResolver aProps)
        throws CheckstyleException
    {
        if (aValue == null)
        {
            return null;
        }

        final List fragments = new ArrayList();
        final List propertyRefs = new ArrayList();
        parsePropertyString(aValue, fragments, propertyRefs);

        final StringBuffer sb = new StringBuffer();
        final Iterator i = fragments.iterator();
        final Iterator j = propertyRefs.iterator();
        while (i.hasNext())
        {
            String fragment = (String) i.next();
            if (fragment == null)
            {
                final String propertyName = (String) j.next();
                fragment = aProps.resolve(propertyName);
                if (fragment == null)
                {
                    throw new CheckstyleException("Property ${" + propertyName
                            + "} has not been set");
                }
            }
            sb.append(fragment);
        }

        return sb.toString();
    }

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection of text
     * fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property
     * reference from the second list.
     * 
     * @param aValue Text to parse. Must not be <code>null</code>.
     * @param aFragments List to add text fragments to. Must not be
     *            <code>null</code>.
     * @param aPropertyRefs List to add property names to. Must not be
     *            <code>null</code>.
     * 
     * @throws CheckstyleException if the string contains an opening
     *             <code>${</code> without a closing <code>}</code> Code
     *             copied from ant -
     *             http://cvs.apache.org/viewcvs/jakarta-ant/src/main/org/apache/tools/ant/ProjectHelper.java
     */
    private static void parsePropertyString(String aValue, List aFragments, List aPropertyRefs)
        throws CheckstyleException
    {
        int prev = 0;
        int pos;
        // search for the next instance of $ from the 'prev' position
        while ((pos = aValue.indexOf("$", prev)) >= 0)
        {

            // if there was any text before this, add it as a fragment
            // TODO, this check could be modified to go if pos>prev;
            // seems like this current version could stick empty strings
            // into the list
            if (pos > 0)
            {
                aFragments.add(aValue.substring(prev, pos));
            }
            // if we are at the end of the string, we tack on a $
            // then move past it
            if (pos == (aValue.length() - 1))
            {
                aFragments.add("$");
                prev = pos + 1;
            }
            else if (aValue.charAt(pos + 1) != '{')
            {
                // peek ahead to see if the next char is a property or not
                // not a property: insert the char as a literal
                /*
                 * fragments.addElement(value.substring(pos + 1, pos + 2)); prev =
                 * pos + 2;
                 */
                if (aValue.charAt(pos + 1) == '$')
                {
                    // backwards compatibility two $ map to one mode
                    aFragments.add("$");
                    prev = pos + 2;
                }
                else
                {
                    // new behaviour: $X maps to $X for all values of X!='$'
                    aFragments.add(aValue.substring(pos, pos + 2));
                    prev = pos + 2;
                }

            }
            else
            {
                // property found, extract its name or bail on a typo
                final int endName = aValue.indexOf('}', pos);
                if (endName < 0)
                {
                    throw new CheckstyleException("Syntax error in property: " + aValue);
                }
                final String propertyName = aValue.substring(pos + 2, endName);
                aFragments.add(null);
                aPropertyRefs.add(propertyName);
                prev = endName + 1;
            }
        }
        // no more $ signs found
        // if there is any tail to the file, append it
        if (prev < aValue.length())
        {
            aFragments.add(aValue.substring(prev));
        }
    }

}
