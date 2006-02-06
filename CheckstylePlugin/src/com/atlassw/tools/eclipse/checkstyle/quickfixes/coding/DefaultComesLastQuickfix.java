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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;

import com.atlassw.tools.eclipse.checkstyle.quickfixes.AbstractASTResolution;
import com.atlassw.tools.eclipse.checkstyle.util.CheckstylePluginImages;

/**
 * Quickfix implementation that moves the default case of a switch statement to
 * the last position.
 * 
 * @author Lars K�dderitzsch
 */
public class DefaultComesLastQuickfix extends AbstractASTResolution
{

    /**
     * {@inheritDoc}
     */
    protected ASTVisitor handleGetCorrectingASTVisitor(final ASTRewrite rewrite,
            final IRegion lineInfo)
    {

        return new ASTVisitor()
        {

            public boolean visit(SwitchCase node)
            {
                int pos = node.getStartPosition();
                if (pos >= lineInfo.getOffset()
                        && pos <= (lineInfo.getOffset() + lineInfo.getLength()))
                {

                    if (node.isDefault())
                    {
                        SwitchStatement switchStatement = (SwitchStatement) node.getParent();
                        ListRewrite listRewrite = rewrite.getListRewrite(switchStatement,
                                SwitchStatement.STATEMENTS_PROPERTY);

                        int defaultStatementIndex = switchStatement.statements().indexOf(node);

                        // move default statement to last position
                        listRewrite.remove(node, null);
                        listRewrite.insertLast(node, null);

                        // also move all following statements unit the next
                        // switch case to last.
                        for (int i = defaultStatementIndex + 1; i < switchStatement.statements()
                                .size(); i++)
                        {
                            ASTNode tmpNode = (ASTNode) switchStatement.statements().get(i);

                            if (!(tmpNode instanceof SwitchCase))
                            {
                                listRewrite.remove(tmpNode, null);
                                listRewrite.insertLast(tmpNode, null);
                            }
                            else
                            {
                                break;
                            }
                        }
                    }
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
        return Messages.DefaultComesLastQuickfix_description;
    }

    /**
     * {@inheritDoc}
     */
    public String getLabel()
    {
        return Messages.DefaultComesLastQuickfix_label;
    }

    /**
     * {@inheritDoc}
     */
    public Image getImage()
    {
        return CheckstylePluginImages.getImage(CheckstylePluginImages.CORRECTION_CHANGE);
    }
}
