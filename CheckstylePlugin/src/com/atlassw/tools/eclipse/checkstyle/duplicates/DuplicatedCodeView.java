//============================================================================
//
// Copyright (C) 2002-2004  David Schneider
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

package com.atlassw.tools.eclipse.checkstyle.duplicates;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.atlassw.tools.eclipse.checkstyle.CheckstylePlugin;
import com.atlassw.tools.eclipse.checkstyle.builder.CheckstyleMarker;
import com.atlassw.tools.eclipse.checkstyle.nature.CheckstyleNature;
import com.atlassw.tools.eclipse.checkstyle.util.CheckstyleLog;

/**
 * View that shows the report of the strict duplicated code analysis. The
 * duplications are shown per file. They are displayed in a tree and it is
 * possible to open both the source and target files involved in a code
 * duplication.
 */

public class DuplicatedCodeView extends ViewPart
{

    /**
     * Id of the view. Cf. plugin.xml
     */
    public static final String VIEW_ID = CheckstylePlugin.PLUGIN_ID + ".duplicatesView";

    /**
     * Tree viewer that displays the result of the analysis.
     */
    private TreeViewer mViewer;

    /**
     * Adapter used for adding navigation actions.
     */
    private DrillDownAdapter mDrillDownAdapter;

    /**
     * Action that opens a source file.
     */
    private Action mOpenSourceFileAction;

    /**
     * Open that opens a target file.
     */
    private Action mOpenDuplicatedCodeFileAction;

    /**
     * Content provider for the tree viewer.
     */
    class ViewContentProvider extends BaseWorkbenchContentProvider
    {

        /**
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object parentElement)
        {

            IWorkbenchAdapter adapter = getAdapter(parentElement);

            if (parentElement instanceof IProject)
            {

                final List files = new LinkedList();
                IResourceVisitor visitor = new IResourceVisitor()
                {
                    public boolean visit(IResource resource) throws CoreException
                    {
                        if (resource instanceof IFile)
                        {
                            files.add(resource);
                            return false;
                        }
                        return true;
                    }
                };

                try
                {
                    ((IProject) parentElement).accept(visitor);
                }
                catch (CoreException e)
                {
                    return new Object[0];
                }

                return files.toArray();
            }
            else if (parentElement instanceof IFile)
            {
                try
                {
                    return ((IFile) parentElement).findMarkers(CheckstyleMarker.MARKER_ID, false,
                            IResource.DEPTH_ZERO);
                }
                catch (CoreException e)
                {
                    return new Object[0];
                }
            }
            else
            {
                return super.getChildren(parentElement);
            }
        }
    }

    /**
     * Label provider for the tree viewer.
     */
    class ViewLabelProvider extends WorkbenchLabelProvider
    {

    }

    class DuplicatesFilter extends ViewerFilter
    {

        /**
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {

            try
            {
                if (element instanceof IProject)
                {

                    return ((IProject) element).hasNature(CheckstyleNature.NATURE_ID);
                }
                else if (element instanceof IFile)
                {
                    boolean result = false;
                    IMarker[] markers = ((IFile) element).findMarkers(CheckstyleMarker.MARKER_ID,
                            false, IResource.DEPTH_ZERO);
                    for (int i = 0; i < markers.length; i++)
                    {
                        if ("StrictDuplicateCode".equals(markers[i].getAttribute("ModuleName")))
                        {
                            result = true;
                            break;
                        }
                    }

                    return result;
                }
                else if (element instanceof IMarker)
                {
                    return "StrictDuplicateCode".equals(((IMarker) element)
                            .getAttribute("ModuleName"));
                }
            }
            catch (CoreException e)
            {
                return false;
            }
            return true;
        }
    }

    IResourceChangeListener resourceListener = new IResourceChangeListener()
    {
        public void resourceChanged(final IResourceChangeEvent event)
        {
            //update in UI thread
            Display.getDefault().asyncExec(new Runnable()
            {
                public void run()
                {
                    mViewer.refresh(event.getResource(), true);
                }
            });
        }
    };

    /**
     * The constructor.
     */
    public DuplicatedCodeView()
    {}

    /**
     * Cf. method below.
     * 
     * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    public void createPartControl(Composite parent)
    {
        mViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
        mDrillDownAdapter = new DrillDownAdapter(mViewer);
        mViewer.setContentProvider(new ViewContentProvider());
        mViewer.setLabelProvider(new ViewLabelProvider());
        mViewer.addFilter(new DuplicatesFilter());
        mViewer.setInput(CheckstylePlugin.getWorkspace().getRoot());
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();

        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
    }

    /**
     * Creates the context popup menu.
     */
    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                DuplicatedCodeView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(mViewer.getControl());
        mViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, mViewer);
    }

    /**
     * Adds action to the contect menu.
     * 
     * @param manager : the menu manager
     */
    private void fillContextMenu(IMenuManager manager)
    {
        manager.add(mOpenSourceFileAction);
        IStructuredSelection selection = (IStructuredSelection) mViewer.getSelection();
        if (selection.getFirstElement() instanceof IMarker)
        {
            manager.add(mOpenDuplicatedCodeFileAction);
        }
        manager.add(new Separator());
        mDrillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /**
     * Fills the tool bar with the navigation actions.
     */
    private void contributeToActionBars()
    {
        IActionBars bars = getViewSite().getActionBars();
        mDrillDownAdapter.addNavigationActions(bars.getToolBarManager());
    }

    /**
     * Creates the actions of this view.
     */
    private void makeActions()
    {
        createOpenSourceFileAction();
        createOpenDuplicatedCodeFileAction();
    }

    /**
     * Creates the action that opens a source file.
     */
    private void createOpenSourceFileAction()
    {
        mOpenSourceFileAction = new Action()
        {
            public void run()
            {
                try
                {
                    IStructuredSelection selection = (IStructuredSelection) mViewer.getSelection();
                    if (selection.getFirstElement() instanceof IFile)
                    {
                        IDE.openEditor(getSite().getPage(), (IFile) selection.getFirstElement());
                    }
                    else if (selection.getFirstElement() instanceof IMarker)
                    {
                        IMarker duplicateMarker = (IMarker) selection.getFirstElement();

                        DuplicatedCode duplicatedCode = new DuplicatedCode((IFile) duplicateMarker
                                .getResource(), ((Integer) duplicateMarker
                                .getAttribute(IMarker.LINE_NUMBER)).intValue(),
                                (String) duplicateMarker.getAttribute(IMarker.MESSAGE));

                        IEditorPart editorPart = IDE.openEditor(getSite().getPage(), duplicatedCode
                                .getSourceFile());
                        if (editorPart instanceof ITextEditor)
                        {
                            // instanceof just to be sure, but the JavaEditor is
                            // an ITextEditor
                            selectAndRevealDuplicatedLines(((ITextEditor) editorPart),
                                    duplicatedCode.getSourceFileFirstLineNumber(), duplicatedCode
                                            .getSourceFileFirstLineNumber()
                                            + duplicatedCode.getNumberOfDuplicatedLines());
                        }
                    }
                }
                catch (PartInitException e)
                {
                    CheckstyleLog.errorDialog(mViewer.getControl().getShell(),
                            "Error while opening the file editor.", e, true);
                }
                catch (CoreException e)
                {
                    CheckstyleLog.errorDialog(mViewer.getControl().getShell(),
                            "Error while opening the file editor.", e, true);
                }
            }
        };
        mOpenSourceFileAction.setText("Open source file");
        mOpenSourceFileAction
                .setToolTipText("Opens the file where duplications have been detected.");
        mOpenSourceFileAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
    }

    /**
     * Creates the action that opens a target file.
     */
    private void createOpenDuplicatedCodeFileAction()
    {
        mOpenDuplicatedCodeFileAction = new Action()
        {
            public void run()
            {
                try
                {
                    IStructuredSelection selection = (IStructuredSelection) mViewer.getSelection();
                    if (selection.getFirstElement() instanceof IMarker)
                    {
                        IMarker duplicateMarker = (IMarker) selection.getFirstElement();

                        DuplicatedCode duplicatedCode = new DuplicatedCode((IFile) duplicateMarker
                                .getResource(), ((Integer) duplicateMarker
                                .getAttribute(IMarker.LINE_NUMBER)).intValue(),
                                (String) duplicateMarker.getAttribute(IMarker.MESSAGE));
                        IFile destinationFile = duplicatedCode.getTargetFile();
                        if (destinationFile == null)
                        {
                            // nothing to do
                            return;
                        }
                        IEditorPart editorPart = IDE.openEditor(getSite().getPage(),
                                destinationFile);
                        if (editorPart instanceof ITextEditor)
                        {
                            // instanceof just to be sure, but the JavaEditor is
                            // an ITextEditor
                            selectAndRevealDuplicatedLines(((ITextEditor) editorPart),
                                    duplicatedCode.getTargetFileFirstLineNumber(), duplicatedCode
                                            .getTargetFileFirstLineNumber()
                                            + duplicatedCode.getNumberOfDuplicatedLines());
                        }
                    }
                }
                catch (PartInitException e)
                {
                    CheckstyleLog.errorDialog(mViewer.getControl().getShell(),
                            "Error while opening the file editor.", e, true);
                }
                catch (CoreException e)
                {
                    CheckstyleLog.errorDialog(mViewer.getControl().getShell(),
                            "Error while opening the file editor.", e, true);
                }
            }
        };
        mOpenDuplicatedCodeFileAction.setText("Open target file");
        mOpenDuplicatedCodeFileAction
                .setToolTipText("Opens the file where duplicate code has been found.");
        mOpenDuplicatedCodeFileAction.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
    }

    /**
     * Opens the editor and selects the area defined by the two lines.
     * 
     * @param editor the editor to open
     * @param firstLine the line to jump to and select from
     * @param lastLine the last line to select
     */
    private void selectAndRevealDuplicatedLines(ITextEditor editor, int firstLine, int lastLine)
    {
        IDocumentProvider provider = editor.getDocumentProvider();
        IDocument document = provider.getDocument(editor.getEditorInput());
        try
        {
            int start = document.getLineOffset(firstLine);
            int end = document.getLineOffset(lastLine - 1);
            editor.selectAndReveal(start, end - start);
        }
        catch (BadLocationException e)
        {

            CheckstyleLog.errorDialog(mViewer.getControl().getShell(),
                    "Error while trying to reveal and display the duplicated lines.", e, true);
        }
    }

    /**
     * Adds the double click capability.
     */
    private void hookDoubleClickAction()
    {
        mViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) mViewer.getSelection();
                mOpenSourceFileAction.run();
                if (selection.getFirstElement() instanceof DuplicatedCode)
                {
                    mOpenDuplicatedCodeFileAction.run();
                }

            }
        });
    }

    /**
     * Cf. method below.
     * 
     * @see org.eclipse.ui.IWorkbenchPart#setFocus()
     */
    public void setFocus()
    {
        mViewer.getControl().setFocus();
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        super.dispose();
    }
}