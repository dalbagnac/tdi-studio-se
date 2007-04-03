// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.core.ui.editor.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.views.properties.tabbed.view.Tab;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.IODataComponent;
import org.talend.core.model.components.IODataComponentContainer;
import org.talend.core.model.metadata.ColumnNameChanged;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataTool;
import org.talend.core.model.process.EConnectionCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.properties.DynamicTabbedPropertySection;

/**
 * Command that will change a metadata in a node.
 * 
 * $Id$
 * 
 */
/**
 * DOC Administrator class global comment. Detailled comment <br/>
 * 
 */
public class ChangeMetadataCommand extends Command {

    private Node node, inputNode;

    private IMetadataTable currentOutputMetadata, newOutputMetadata, oldOutputMetadata;

    private boolean outputWasRepository = false, inputWasRepository = false;

    private IMetadataTable currentInputMetadata, newInputMetadata, oldInputMetadata;

    private IODataComponentContainer dataContainer;

    private IODataComponent dataComponent;

    private Boolean propagate;

    private List<ChangeMetadataCommand> propagatedChange = new ArrayList<ChangeMetadataCommand>();

    private boolean internal = false;

    private boolean repositoryMode = false;

    private static final String DEFAULT_TABLE_NAME = "MyShema";

    private String oldQuery = "";

    // Default constructor.
    public ChangeMetadataCommand() {
    }

    public ChangeMetadataCommand(Node node, Node inputNode, IMetadataTable currentInputMetadata,
            IMetadataTable newInputMetadata, IMetadataTable currentOutputMetadata, IMetadataTable newOutputMetadata) {
        this.node = node;
        this.inputNode = inputNode;
        this.currentInputMetadata = currentInputMetadata;
        if (currentInputMetadata != null) {
            oldInputMetadata = currentInputMetadata.clone();
        } else {
            oldInputMetadata = null;
        }
        this.newInputMetadata = newInputMetadata;
        this.currentOutputMetadata = currentOutputMetadata;
        if (currentOutputMetadata == null) {
            currentOutputMetadata = node.getMetadataList().get(0);
        }
        oldOutputMetadata = currentOutputMetadata.clone();
        this.newOutputMetadata = newOutputMetadata;
        initializeContainer();
        setLabel(Messages.getString("ChangeMetadataCommand.changeMetadataValues")); //$NON-NLS-1$
    }

    public ChangeMetadataCommand(Node node, IMetadataTable currentOutputMetadata, IMetadataTable newOutputMetadata) {
        this.node = node;
        this.inputNode = null;
        this.currentInputMetadata = null;
        this.newInputMetadata = null;
        oldInputMetadata = null;
        if (currentOutputMetadata == null) {
            currentOutputMetadata = node.getMetadataList().get(0);
        }
        this.currentOutputMetadata = currentOutputMetadata;
        List<IMetadataColumn> columnToSave = new ArrayList<IMetadataColumn>();
        for (IMetadataColumn column : currentOutputMetadata.getListColumns()) {
            if (column.isCustom()) {
                columnToSave.add(column);
            }
        }

        oldOutputMetadata = currentOutputMetadata.clone();
        this.newOutputMetadata = newOutputMetadata.clone();

        this.newOutputMetadata.setReadOnly(newOutputMetadata.isReadOnly());
        List<IMetadataColumn> columnToRemove = new ArrayList<IMetadataColumn>();
        for (IMetadataColumn column : newOutputMetadata.getListColumns()) {
            if (column.isCustom()) {
                columnToRemove.add(this.newOutputMetadata.getColumn(column.getLabel()));
            }
        }
        this.newOutputMetadata.getListColumns().removeAll(columnToRemove);
        this.newOutputMetadata.getListColumns().addAll(columnToSave);
        this.newOutputMetadata.sortCustomColumns();
        this.newOutputMetadata.setReadOnly(currentOutputMetadata.isReadOnly());
        initializeContainer();
        setLabel(Messages.getString("ChangeMetadataCommand.changeMetadataValues")); //$NON-NLS-1$
    }

    public void setRepositoryMode(boolean repositoryMode) {
        this.repositoryMode = repositoryMode;
    }

    private void initializeContainer() {
        dataContainer = new IODataComponentContainer();
        for (Connection connec : (List<Connection>) node.getIncomingConnections()) {
            if (connec.isActivate() && connec.getLineStyle().getCategory().equals(EConnectionCategory.MAIN)) {
                IODataComponent input = null;
                if (newInputMetadata == null) {
                    input = new IODataComponent(connec);
                } else {
                    if (connec.getMetaName().equals(newInputMetadata.getTableName())) {
                        input = new IODataComponent(connec, newInputMetadata);
                    }
                }
                if (input != null) {
                    dataContainer.getInputs().add(input);
                }

            }
        }
        for (Connection connec : (List<Connection>) node.getOutgoingConnections()) {
            if (connec.isActivate() && (connec.getLineStyle().getCategory().equals(EConnectionCategory.MAIN))) {
                if ((!connec.getSource().getConnectorFromType(connec.getLineStyle()).isBuiltIn())
                        || (connec.getMetaName().equals(newOutputMetadata.getTableName()))) {
                    IODataComponent output = new IODataComponent(connec, newOutputMetadata);
                    dataContainer.getOuputs().add(output);
                }
            }
        }
    }

    private void setInternal(boolean internal) {
        this.internal = internal;
    }

    private boolean getPropagate(Boolean returnIfNull) {
        if (propagate == null) {
            if (returnIfNull != null) {
                return returnIfNull;
            }
            propagate = MessageDialog
                    .openQuestion(
                            new Shell(),
                            Messages.getString("ChangeMetadataCommand.messageDialog.propagate"), Messages.getString("ChangeMetadataCommand.messageDialog.questionMessage")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return propagate;
    }

    private boolean getPropagate() {
        return getPropagate(null);
    }

    protected void updateColumnList(IMetadataTable oldTable, IMetadataTable newTable) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.findView("org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
        PropertySheet sheet = (PropertySheet) view;
        TabbedPropertySheetPage tabbedPropertySheetPage = (TabbedPropertySheetPage) sheet.getCurrentPage();
        Tab currentTab = tabbedPropertySheetPage.getCurrentTab();
        if (currentTab == null) {
            return;
        }
        ISection[] sections = currentTab.getSections();
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] instanceof DynamicTabbedPropertySection) {
                DynamicTabbedPropertySection currentSection = (DynamicTabbedPropertySection) sections[i];
                if (currentSection.getElement().equals(node)) {
                    currentSection.updateColumnList(MetadataTool.getColumnNameChanged(oldTable, newTable));
                    currentSection.refresh();
                }
            }
        }
    }

    public void execute(Boolean propagateP) {
        this.propagate = propagateP;
        if (currentOutputMetadata == null) {
            currentOutputMetadata = node.getMetadataList().get(0);
        }
        setInternal(true);
        execute();
    }

    private void propagateDatas(boolean isExecute) {
        // Propagate :
        if (dataContainer != null && (!dataContainer.getInputs().isEmpty() || !dataContainer.getOuputs().isEmpty())) {
            for (IODataComponent currentIO : dataContainer.getInputs()) {
                INode sourceNode = currentIO.getSource();
                if (currentIO.hasChanged()) {
                    sourceNode.metadataOutputChanged(currentIO, currentIO.getName());
                    if (isExecute) {
                        currentIO.setTable(oldInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    } else {
                        currentIO.setTable(newInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    }
                }
            }
            for (IODataComponent currentIO : dataContainer.getOuputs()) {
                if (currentIO.hasChanged()) {
                    INode targetNode = currentIO.getTarget();
                    targetNode.metadataInputChanged(currentIO, currentIO.getUniqueName());
                    if (isExecute) {
                        if (targetNode instanceof Node) {
                            if (!((Node) targetNode).isExternalNode() && getPropagate()) {
                                if (((Node) targetNode).getComponent().isSchemaAutoPropagated()) {
                                    ChangeMetadataCommand cmd = new ChangeMetadataCommand((Node) targetNode, null,
                                            newOutputMetadata);
                                    if (dataContainer.getOuputs().size() > 0) {
                                        List<ColumnNameChanged> columnNameChanged = dataContainer.getOuputs().get(0)
                                                .getColumnNameChanged();
                                        for (IODataComponent dataComp : cmd.dataContainer.getOuputs()) {
                                            dataComp.setColumnNameChanged(columnNameChanged);
                                        }
                                    }
                                    cmd.execute(true);
                                    propagatedChange.add(cmd);
                                }
                            }
                        }
                        currentIO.setTable(oldOutputMetadata);
                        currentIO.setColumnNameChanged(null);
                    } else {
                        if (targetNode instanceof Node) {
                            if (!((Node) targetNode).isExternalNode() && getPropagate()) {
                                if (((Node) targetNode).getComponent().isSchemaAutoPropagated()) {
                                    if (dataContainer.getOuputs().size() > 0) {
                                        List<ColumnNameChanged> columnNameChanged = dataContainer.getOuputs().get(0)
                                                .getColumnNameChanged();
                                        for (ChangeMetadataCommand cmd : propagatedChange) {
                                            for (IODataComponent dataComp : cmd.dataContainer.getOuputs()) {
                                                dataComp.setColumnNameChanged(columnNameChanged);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        currentIO.setTable(newOutputMetadata);
                        currentIO.setColumnNameChanged(null);
                    }
                }
            }

        } else if (dataComponent != null) {
            for (IConnection outgoingConnection : node.getOutgoingConnections()) {
                outgoingConnection.getTarget().metadataInputChanged(dataComponent, outgoingConnection.getName());
            }
        }
        // End propagate
    }

    @Override
    public void execute() {
        propagatedChange.clear();

        propagateDatas(true);

        if (currentInputMetadata != null) {
            if (!currentInputMetadata.sameMetadataAs(newInputMetadata)) {
                currentInputMetadata.setListColumns(newInputMetadata.getListColumns());
                String type = (String) inputNode.getPropertyValue(EParameterName.SCHEMA_TYPE.getName());
                if (type != null) {
                    if (type.equals(EmfComponent.REPOSITORY)) {
                        inputWasRepository = true;
                        inputNode.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
                    }
                }
            }
        }

        if (!currentOutputMetadata.sameMetadataAs(newOutputMetadata)) {
            currentOutputMetadata.setListColumns(newOutputMetadata.getListColumns());

            String type = (String) node.getPropertyValue(EParameterName.SCHEMA_TYPE.getName());
            if (type != null && type.equals(EmfComponent.REPOSITORY) && !repositoryMode) {
                outputWasRepository = true;
                node.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
            }

            String newQuery = TalendTextUtils.addQuotes(generateNewQuery(newOutputMetadata));

            for (IElementParameter param : (List<IElementParameter>) node.getElementParameters()) {
                if (param.getField() == EParameterFieldType.MEMO_SQL) {
                    oldQuery = node.getPropertyValue(param.getName()).toString();
                    node.setPropertyValue(param.getName(), newQuery);
                    param.setRepositoryValueUsed(true);
                    param.getValue();

                }
            }
            refreshPropertyView();
        }
        if (!internal) {
            updateColumnList(oldOutputMetadata, newOutputMetadata);
            ((Process) node.getProcess()).checkProcess();
        }
    }

    @Override
    public void undo() {
        propagateDatas(false);

        if (currentInputMetadata != null) {
            if (!currentInputMetadata.sameMetadataAs(oldInputMetadata)) {
                currentInputMetadata.setListColumns(oldInputMetadata.getListColumns());
                if (inputWasRepository) {
                    inputNode.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.REPOSITORY);
                }
            }
        }
        if (!currentOutputMetadata.sameMetadataAs(oldOutputMetadata)) {
            currentOutputMetadata.setListColumns(oldOutputMetadata.getListColumns());
        }
        if (outputWasRepository) {
            node.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.REPOSITORY);
        }
        for (ChangeMetadataCommand cmd : propagatedChange) {
            cmd.undo();
        }
        if (!internal) {
            updateColumnList(newOutputMetadata, oldOutputMetadata);
            ((Process) node.getProcess()).checkProcess();
        }

        for (IElementParameter param : (List<IElementParameter>) node.getElementParameters()) {
            if (param.getField() == EParameterFieldType.MEMO_SQL) {
                node.setPropertyValue(param.getName(), oldQuery);
            }
        }
    }

    public String generateNewQuery(IMetadataTable repositoryMetadata) {
        List<IMetadataColumn> metaDataColumnList = repositoryMetadata.getListColumns();
        int index = metaDataColumnList.size();
        if (index == 0) {
            return "";
        }

        StringBuffer query = new StringBuffer();
        String enter = "\n";
        String space = " ";
        query.append("SELECT").append(space);

        for (int i = 0; i < metaDataColumnList.size(); i++) {
            IMetadataColumn metaDataColumn = metaDataColumnList.get(i);
            String columnName = metaDataColumn.getLabel();
            if (i != index - 1) {
                query.append(columnName).append(",").append(space);
            } else {
                query.append(columnName).append(space);
            }
        }
        String tableName = repositoryMetadata.getTableName();
        if (tableName == null || tableName.length() < 0) {
            tableName = DEFAULT_TABLE_NAME;

        }
        query.append(enter).append("FROM").append(space).append(tableName);

        return query.toString();

    }

    /**
     * Generates new Query.
     * 
     * @param repositoryMetadata
     * @param dbType
     * @param schema
     * @return
     */
    public String generateNewQuery(IMetadataTable repositoryMetadata, String dbType, String schema) {
        List<IMetadataColumn> metaDataColumnList = repositoryMetadata.getListColumns();
        int index = metaDataColumnList.size();
        if (index == 0) {
            return "";
        }

        StringBuffer query = new StringBuffer();
        String enter = "\n";
        String space = " ";
        query.append("SELECT").append(space);
        String tableNameForColumnSuffix = repositoryMetadata.getTableName() + ".";

        for (int i = 0; i < metaDataColumnList.size(); i++) {
            IMetadataColumn metaDataColumn = metaDataColumnList.get(i);
            String columnName = getColumnName(metaDataColumn.getLabel(), dbType);
            if (i != index - 1) {
                query.append(tableNameForColumnSuffix).append(columnName).append(",").append(space);
            } else {
                query.append(tableNameForColumnSuffix).append(columnName).append(space);
            }
        }
        query.append(enter).append("FROM").append(space)
                .append(getTableName(repositoryMetadata.getTableName(), schema));

        return query.toString();
    }

    /**
     * Gets the table name.
     * 
     * @param tableName
     * @param schema
     * @return
     */
    private String getTableName(String tableName, String schema) {
        String currentTableName = tableName;
        if (schema != null && schema.length() > 0) {
            if (isJavaProject()) {
                currentTableName = "\\" + "\"" + tableName + "\\" + "\"" + "." + tableName;
            } else {
                currentTableName = "\"" + schema + "\"" + "." + tableName;
            }
            return currentTableName;
        }
        return tableName;
    }

    /**
     * Checks if database type is Postgres, add quoutes around column name.
     * 
     * @param columnName
     * @param dbType
     * @return
     */
    private String getColumnName(String columnName, String dbType) {
        String columnNameAfterChanged;
        if (!dbType.equalsIgnoreCase("PostgreSQL")) {
            columnNameAfterChanged = columnName;
        } else if (isJavaProject()) {
            columnNameAfterChanged = "\\" + "\"" + columnName + "\\" + "\"";
        } else {
            columnNameAfterChanged = "\"" + columnName + "\"";
        }
        return columnNameAfterChanged;
    }

    /**
     * Checks project type(perl or java).
     * 
     * @return
     */
    private static boolean isJavaProject() {
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        ECodeLanguage codeLanguage = repositoryContext.getProject().getLanguage();
        return (codeLanguage == ECodeLanguage.JAVA);
    }

    /**
     * Refresh property view.
     */
    public void refreshPropertyView() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.findView("org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
        PropertySheet sheet = (PropertySheet) view;
        TabbedPropertySheetPage tabbedPropertySheetPage = (TabbedPropertySheetPage) sheet.getCurrentPage();
        tabbedPropertySheetPage.refresh();
    }
}
