//============================================================================
//
// Copyright (C) 2002-2003  David Schneider
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

package com.atlassw.tools.eclipse.checkstyle.config;

//=================================================
// Imports from java namespace
//=================================================
import java.util.HashMap;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

//=================================================
// Imports from javax namespace
//=================================================

//=================================================
// Imports from com namespace
//=================================================
import com.atlassw.tools.eclipse.checkstyle.util.CheckstyleLog;
import com.atlassw.tools.eclipse.checkstyle.util.XMLUtil;

//=================================================
// Imports from org namespace
//=================================================
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *  This class is the factory for all Checkstyle rule metadata.
 */
public class MetadataFactory implements XMLTags
{
    //=================================================
	// Public static final variables.
	//=================================================

	//=================================================
	// Static class variables.
	//=================================================
    
    /**  Metadata for the rule groups.  */
    private static List  sRuleGroupMetadata = new LinkedList();
    
    /**  Metadata for all rules, keyed by class name.  */
    private static HashMap sRuleMetadata = new HashMap();

    /**  Name of the rules metadata XML file.  */
    private static final String METADATA_FILENAME = "/CheckstyleMetadata.xml";

	//=================================================
	// Instance member variables.
	//=================================================

	//=================================================
	// Constructors & finalizer.
	//=================================================
    
    /**
     * Private constructor to prevent instantiation.
     */
    private MetadataFactory()
    {}
    
    /**
     *  Static initializer.
     */
    static
    {
        doInitialization();
    }

	//=================================================
	// Methods.
	//=================================================
    
    public static List getRuleGroupMetadata()
    {
        return sRuleGroupMetadata;
    }
    
	private static void doInitialization()
	{
        InputStream metadataStream = null;
        Document metadataDocument = null;
		try
		{
			//
			//  Get the metadata file's input stream.
			//
			metadataStream = 
                MetadataFactory.class.getResourceAsStream(METADATA_FILENAME);
			if (metadataStream == null)
			{
				CheckstyleLog.error("Failed to load audit rule metadata, input stream is null");
				return;
			}

			metadataDocument = XMLUtil.newDocument(metadataStream);
			if (metadataDocument == null)
			{
				CheckstyleLog.error("Failed to load audit rule metadata, failed to parse XML");
				return;
			}
		}
		finally
		{
			try
			{
				metadataStream.close();
			}
			catch (Exception e)
			{} //  We tried to be nice and close the stream.
		}

		//
		//  Find all the rule groups and load them.
		//
		NodeList children = metadataDocument.getDocumentElement().getChildNodes();
		int count = children.getLength();
		for (int i = 0; i < count; i++)
		{
			Node node = children.item(i);
			if (node.getNodeName().equals(RULE_GROUP_METADATA_TAG))
			{
                RuleGroupMetadata ruleGroup = null;
                try
                {
				    ruleGroup = new RuleGroupMetadata(node);
                }
                catch (Exception e)
                {
                    CheckstyleLog.warning("Failed to load audit rule group", e);
                }
                if (ruleGroup != null)
                {
				    sRuleGroupMetadata.add(ruleGroup);
                }
			}
		}
        
        //
        //  Build the hash map of rule metadata.
        //
        Iterator iter = sRuleGroupMetadata.iterator();
        for (int i = 0; iter.hasNext(); i++)
        {
            RuleGroupMetadata group = (RuleGroupMetadata)iter.next();
            Iterator iter2 = group.getRuleMetadata().iterator();
            while (iter2.hasNext())
            {
                RuleMetadata ruleMeta = (RuleMetadata)iter2.next();
                ruleMeta.setGroupIndex(i);
                sRuleMetadata.put(ruleMeta.getCheckImplClassname(), ruleMeta);
            }
        }
	}
    
    /**
     *  Get metadata for a check rule.
     * 
     *  @param classname  The rule's implementation classname.
     * 
     *  @return  The metadata.
     */
    public static RuleMetadata getRuleMetadata(String classname)
    {
        return (RuleMetadata)sRuleMetadata.get(classname);
    }
}
