//============================================================================
//
// Copyright (C) 2002-2006  David Schneider, Lars K�dderitzsch
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

package com.atlassw.tools.eclipse.checkstyle.quickfixes.coding;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;

import com.atlassw.tools.eclipse.checkstyle.quickfixes.AbstractASTResolution;
import com.atlassw.tools.eclipse.checkstyle.util.CheckstylePluginImages;

/**
 * Quickfix implementation that removes an empty statement (unneccessary
 * semicolon).
 * 
 * @author Lars K�dderitzsch
 */
public class EmptyStatementQuickfix extends AbstractASTResolution
{

    /**
     * {@inheritDoc}
     */
    protected ASTVisitor handleGetCorrectingASTVisitor(final ASTRewrite rewrite,
            final IRegion lineInfo)
    {

        return new ASTVisitor()
        {
            public boolean visit(EmptyStatement node)
            {
                int pos = node.getStartPosition();
                if (pos >= lineInfo.getOffset()
                        && pos <= (lineInfo.getOffset() + lineInfo.getLength()))
                {
                    rewrite.remove(node, null);
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription()
    {
        return Messages.EmptyStatementQuickfix_description;
    }

    /**
     * {@inheritDoc}
     */
    public String getLabel()
    {
        return Messages.EmptyStatementQuickfix_label;
    }

    /**
     * {@inheritDoc}
     */
    public Image getImage()
    {
        return CheckstylePluginImages.getImage(CheckstylePluginImages.CORRECTION_REMOVE);
    }

}
