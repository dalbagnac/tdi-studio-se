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
package org.talend.repository.ui.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.talend.commons.exception.RuntimeExceptionHandler;
import org.talend.commons.utils.data.container.Container;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.metadata.builder.connection.AbstractMetadataObject;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.DelimitedFileConnection;
import org.talend.core.model.metadata.builder.connection.LdifFileConnection;
import org.talend.core.model.metadata.builder.connection.PositionalFileConnection;
import org.talend.core.model.metadata.builder.connection.QueriesConnection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.connection.RegexpFileConnection;
import org.talend.core.model.metadata.builder.connection.SubItemHelper;
import org.talend.core.model.metadata.builder.connection.TableHelper;
import org.talend.core.model.metadata.builder.connection.XmlFileConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.ui.images.ECoreImage;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.BinRepositoryNode;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.StableRepositoryNode;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode.EProperties;

/**
 * Content provider for the repository view.<br/>
 * 
 * $Id$
 * 
 */
public class RepositoryContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    /**
     * Help manage convert repository node.
     * 
     * yzhang RepositoryContentProvider class global comment. Detailled comment <br/>
     * 
     * $Id$
     * 
     */
    class RepositoryNodeManager {

        RepositoryNode node;

        RootContainer<String, IRepositoryObject> rootContainer;

        ERepositoryObjectType type;

        RepositoryNode recBinNode;

        /**
         * Constructor.
         * 
         * yzhang RepositoryContentProvider.NodeManager constructor comment.
         */
        public RepositoryNodeManager(RepositoryNode node, RootContainer<String, IRepositoryObject> rootContainer,
                ERepositoryObjectType type, RepositoryNode recBinNode) {
            this.node = node;
            this.rootContainer = rootContainer;
            this.type = type;
            this.recBinNode = recBinNode;
        }

        public void applyConvert() {
            convert(rootContainer, node, type, recBinNode);
        }

    }

    private IRepositoryView view;

    private TreeViewer viewer;

    private RepositoryNode root;

    private IProxyRepositoryFactory factory;

    private Map<Object, RepositoryNodeManager> mapNodes = new HashMap<Object, RepositoryNodeManager>();

    public RepositoryContentProvider(IRepositoryView view) {
        super();
        this.view = view;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        this.viewer = (TreeViewer) v;
    }

    public void dispose() {
    }

    public Object[] getElements(Object parent) {
        if (parent.equals(view.getViewSite())) {
            RepositoryNode systemFolders = view.getRoot();
            if (systemFolders.getChildren().isEmpty()) {
                initialize();
            }
            return systemFolders.getChildren().toArray();
        }
        return getChildren(parent);
    }

    public Object getParent(Object child) {
        return ((RepositoryNode) child).getParent();
    }

    public Object[] getChildren(Object parent) {
        RepositoryNodeManager manager = mapNodes.get(parent);
        if (manager != null) {
            manager.applyConvert();
        }
        return ((RepositoryNode) parent).getChildren().toArray();
    }

    public boolean hasChildren(Object parent) {
        return true;
    }

    // TODO SML Remove
    // public RepositoryNode getElement(IPath path, ERepositoryObjectType type) {
    // for (RepositoryNode currentNode : root.getChildren()) {
    // if (currentNode.getType() == ENodeType.STABLE_SYSTEM_FOLDER) {
    // return getElement(path, currentNode);
    // }
    // }
    // return null;
    // }
    //
    // private RepositoryNode getElement(IPath path, RepositoryNode node) {
    // String folder = path.segment(0);
    // for (RepositoryNode currentNode : node.getChildren()) {
    // if (currentNode.getType() == ENodeType.SIMPLE_FOLDER && currentNode.getLabel().equals(folder)) {
    // if (path.segmentCount() == 1) {
    // return currentNode;
    // } else {
    // getElement(path.removeFirstSegments(1), currentNode);
    // }
    // }
    // }
    // return null;
    // }

    private void initialize() {
        root = view.getRoot();
        List<RepositoryNode> nodes = root.getChildren();

        factory = ProxyRepositoryFactory.getInstance();
        try {
            // 0. Recycle bin
            RepositoryNode recBinNode = new BinRepositoryNode(root);
            nodes.add(recBinNode);

            // 1. Business process
            RepositoryNode businessProcessNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            businessProcessNode.setProperties(EProperties.LABEL, ERepositoryObjectType.BUSINESS_PROCESS);
            businessProcessNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.BUSINESS_PROCESS);
            nodes.add(businessProcessNode);
            mapNodes.put(businessProcessNode, new RepositoryNodeManager(businessProcessNode, factory
                    .getBusinessProcess(), ERepositoryObjectType.BUSINESS_PROCESS, recBinNode));
            /*
             * convert(factory.getBusinessProcess(), businessProcessNode, ERepositoryObjectType.BUSINESS_PROCESS,
             * recBinNode);
             */

            // 2. Process
            RepositoryNode processNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            processNode.setProperties(EProperties.LABEL, ERepositoryObjectType.PROCESS);
            processNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.PROCESS);
            nodes.add(processNode);
            mapNodes.put(processNode, new RepositoryNodeManager(processNode, factory.getProcess(),
                    ERepositoryObjectType.PROCESS, recBinNode));
            // convert(factory.getProcess(), processNode, ERepositoryObjectType.PROCESS, recBinNode);
            // convert(factory.getProcess2(), processNode, ERepositoryObjectType.PROCESS, recBinNode);

            // 3. Context
            RepositoryNode contextNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            contextNode.setProperties(EProperties.LABEL, ERepositoryObjectType.CONTEXT);
            contextNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.CONTEXT);
            nodes.add(contextNode);
            mapNodes.put(contextNode, new RepositoryNodeManager(contextNode, factory.getContext(),
                    ERepositoryObjectType.CONTEXT, recBinNode));
            /* convert(factory.getContext(), contextNode, ERepositoryObjectType.CONTEXT, recBinNode); */

            // 4. Code
            RepositoryNode codeNode = new StableRepositoryNode(root, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.code"), ECoreImage.CODE_ICON); //$NON-NLS-1$
            codeNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.ROUTINES);
            nodes.add(codeNode);

            // 4.1. Routines
            RepositoryNode routineNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            routineNode.setProperties(EProperties.LABEL, ERepositoryObjectType.ROUTINES);
            routineNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.ROUTINES);
            codeNode.getChildren().add(routineNode);

            // 4.2. Snippets
            RepositoryNode snippetsNode = new RepositoryNode(null, codeNode, ENodeType.STABLE_SYSTEM_FOLDER);
            snippetsNode.setProperties(EProperties.LABEL, ERepositoryObjectType.SNIPPETS);
            snippetsNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.SNIPPETS);
            codeNode.getChildren().add(snippetsNode);
            mapNodes.put(snippetsNode, new RepositoryNodeManager(routineNode, factory.getRoutine(),
                    ERepositoryObjectType.ROUTINES, recBinNode));
            mapNodes.put(routineNode, new RepositoryNodeManager(routineNode, factory.getRoutine(),
                    ERepositoryObjectType.ROUTINES, recBinNode));
            /* convert(factory.getRoutine(), routineNode, ERepositoryObjectType.ROUTINES, recBinNode); */

            // 5. Documentation
            RepositoryNode docNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            docNode.setProperties(EProperties.LABEL, ERepositoryObjectType.DOCUMENTATION);
            docNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.DOCUMENTATION);
            nodes.add(docNode);
            mapNodes.put(docNode, new RepositoryNodeManager(docNode, factory.getDocumentation(),
                    ERepositoryObjectType.DOCUMENTATION, recBinNode));
            /* convert(factory.getDocumentation(), docNode, ERepositoryObjectType.DOCUMENTATION, recBinNode); */

            // 6. Metadata
            RepositoryNode metadataNode = new RepositoryNode(null, root, ENodeType.STABLE_SYSTEM_FOLDER);
            metadataNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA);
            metadataNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA);
            nodes.add(metadataNode);

            // 6.1. Metadata connections
            RepositoryNode metadataConNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataConNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_CONNECTIONS);
            metadataConNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_CONNECTIONS);
            metadataNode.getChildren().add(metadataConNode);
            mapNodes.put(metadataConNode, new RepositoryNodeManager(metadataConNode, factory.getMetadataConnection(),
                    ERepositoryObjectType.METADATA_CONNECTIONS, recBinNode));
            /*
             * convert(factory.getMetadataConnection(), metadataConNode, ERepositoryObjectType.METADATA_CONNECTIONS,
             * recBinNode);
             */

            // 6.2. Metadata file delimited
            RepositoryNode metadataFileNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataFileNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_DELIMITED);
            metadataFileNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_DELIMITED);
            metadataNode.getChildren().add(metadataFileNode);
            mapNodes.put(metadataFileNode, new RepositoryNodeManager(metadataFileNode, factory
                    .getMetadataFileDelimited(), ERepositoryObjectType.METADATA_FILE_DELIMITED, recBinNode));
            /*
             * convert(factory.getMetadataFileDelimited(), metadataFileNode,
             * ERepositoryObjectType.METADATA_FILE_DELIMITED, recBinNode);
             */

            // 6.3. Metadata file positional
            RepositoryNode metadataFilePositionalNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataFilePositionalNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_POSITIONAL);
            metadataFilePositionalNode.setProperties(EProperties.CONTENT_TYPE,
                    ERepositoryObjectType.METADATA_FILE_POSITIONAL);
            metadataNode.getChildren().add(metadataFilePositionalNode);
            mapNodes.put(metadataFilePositionalNode, new RepositoryNodeManager(metadataFilePositionalNode, factory
                    .getMetadataFilePositional(), ERepositoryObjectType.METADATA_FILE_POSITIONAL, recBinNode));
            /*
             * convert(factory.getMetadataFilePositional(), metadataFilePositionalNode,
             * ERepositoryObjectType.METADATA_FILE_POSITIONAL, recBinNode);
             */

            // 6.4. Metadata file regexp
            RepositoryNode metadataFileRegexpNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataFileRegexpNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_REGEXP);
            metadataFileRegexpNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_REGEXP);
            metadataNode.getChildren().add(metadataFileRegexpNode);
            mapNodes.put(metadataFileRegexpNode, new RepositoryNodeManager(metadataFileRegexpNode, factory
                    .getMetadataFileRegexp(), ERepositoryObjectType.METADATA_FILE_REGEXP, recBinNode));
            /*
             * convert(factory.getMetadataFileRegexp(), metadataFileRegexpNode,
             * ERepositoryObjectType.METADATA_FILE_REGEXP, recBinNode);
             */

            // 6.5. Metadata file xml
            RepositoryNode metadataFileXmlNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataFileXmlNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_XML);
            metadataFileXmlNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_XML);
            metadataNode.getChildren().add(metadataFileXmlNode);
            mapNodes.put(metadataFileXmlNode, new RepositoryNodeManager(metadataFileXmlNode, factory
                    .getMetadataFileXml(), ERepositoryObjectType.METADATA_FILE_XML, recBinNode));
            /*
             * convert(factory.getMetadataFileXml(), metadataFileXmlNode, ERepositoryObjectType.METADATA_FILE_XML,
             * recBinNode);
             */

            // 6.6. Metadata file ldif
            // if (LanguageManager.getCurrentLanguage() == ECodeLanguage.PERL) {
            RepositoryNode metadataFileLdifNode = new RepositoryNode(null, root, ENodeType.SYSTEM_FOLDER);
            metadataFileLdifNode.setProperties(EProperties.LABEL, ERepositoryObjectType.METADATA_FILE_LDIF);
            metadataFileLdifNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_FILE_LDIF);
            metadataNode.getChildren().add(metadataFileLdifNode);
            mapNodes.put(metadataFileLdifNode, new RepositoryNodeManager(metadataFileLdifNode, factory
                    .getMetadataFileLdif(), ERepositoryObjectType.METADATA_FILE_LDIF, recBinNode));
            /*
             * convert(factory.getMetadataFileLdif(), metadataFileLdifNode, ERepositoryObjectType.METADATA_FILE_LDIF,
             * recBinNode);
             */
            // }
        } catch (Exception e) {
            RuntimeExceptionHandler.process(e);
        }
    }

    private void convert(Container fromModel, RepositoryNode parent, ERepositoryObjectType type,
            RepositoryNode recBinNode) {

        handleReferenced(parent);

        if (fromModel.isEmpty()) {
            return;
        }

        for (Object obj : fromModel.getSubContainer()) {
            Container container = (Container) obj;
            Folder oFolder = new Folder((Property) container.getProperty(), type);

            RepositoryNode folder;
            if (container.getLabel().equals(RepositoryConstants.SYSTEM_DIRECTORY)) {
                folder = new StableRepositoryNode(parent, Messages
                        .getString("RepositoryContentProvider.repositoryLabel.system"), ECoreImage.FOLDER_CLOSE_ICON); //$NON-NLS-1$
            } else {
                folder = new RepositoryNode(oFolder, parent, ENodeType.SIMPLE_FOLDER);
            }
            folder.setProperties(EProperties.LABEL, container.getLabel());
            folder.setProperties(EProperties.CONTENT_TYPE, type); // ERepositoryObjectType.FOLDER);
            parent.getChildren().add(folder);
            convert(container, folder, type, recBinNode);
        }

        for (Object obj : fromModel.getMembers()) {
            IRepositoryObject repositoryObject = (IRepositoryObject) obj;
            addNode(parent, type, recBinNode, repositoryObject);
        }
    }

    private void handleReferenced(RepositoryNode parent) {
        if (parent.getType().equals(ENodeType.SYSTEM_FOLDER)) {
            for (Iterator iter = factory.getReferencedProjects().iterator(); iter.hasNext();) {
                Project project = (Project) iter.next();

                RepositoryNode referencedProjectNode = new RepositoryNode(null, parent, ENodeType.REFERENCED_PROJECT);
                referencedProjectNode.setProperties(EProperties.LABEL, project.getLabel()); // //$NON-NLS-1$
                referencedProjectNode
                        .setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.REFERENCED_PROJECTS);
                parent.getChildren().add(referencedProjectNode);
            }
        }
    }

    private void addNode(RepositoryNode parent, ERepositoryObjectType type, RepositoryNode recBinNode,
            IRepositoryObject repositoryObject) {
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();

        RepositoryNode node = new RepositoryNode(repositoryObject, parent, ENodeType.REPOSITORY_ELEMENT);

        node.setProperties(EProperties.CONTENT_TYPE, type);
        node.setProperties(EProperties.LABEL, repositoryObject.getLabel());
        if (factory.getStatus(repositoryObject) == ERepositoryStatus.DELETED) {
            recBinNode.getChildren().add(node);
            node.setParent(recBinNode);
        } else {
            parent.getChildren().add(node);
        }

        if (type == ERepositoryObjectType.METADATA_CONNECTIONS) {
            DatabaseConnection metadataConnection = (DatabaseConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        // PTODO tgu implementation a revoir
        if (type == ERepositoryObjectType.METADATA_FILE_DELIMITED) {
            DelimitedFileConnection metadataConnection = (DelimitedFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_POSITIONAL) {
            PositionalFileConnection metadataConnection = (PositionalFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_REGEXP) {
            RegexpFileConnection metadataConnection = (RegexpFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_XML) {
            XmlFileConnection metadataConnection = (XmlFileConnection) ((ConnectionItem) repositoryObject.getProperty()
                    .getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
        if (type == ERepositoryObjectType.METADATA_FILE_LDIF) {
            LdifFileConnection metadataConnection = (LdifFileConnection) ((ConnectionItem) repositoryObject
                    .getProperty().getItem()).getConnection();
            createTables(recBinNode, node, repositoryObject, metadataConnection);
        }
    }

    /**
     * DOC tguiu Comment method "createTables".
     * 
     * @param node
     * @param repositoryObjectType TODO
     * @param iMetadataConnection
     * @param metadataConnection
     */
    private void createTables(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj,
            EList list, ERepositoryObjectType repositoryObjectType) {
        for (Object currentTable : list) {
            if (currentTable instanceof org.talend.core.model.metadata.builder.connection.MetadataTable) {
                org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable = (org.talend.core.model.metadata.builder.connection.MetadataTable) currentTable;
                RepositoryNode tableNode = createMetatableNode(node, repObj, metadataTable, repositoryObjectType);
                if (SubItemHelper.isDeleted(metadataTable)) {
                    recBinNode.getChildren().add(tableNode);
                } else {
                    node.getChildren().add(tableNode);
                }
            } else if (currentTable instanceof Query) {
                Query query = (Query) currentTable;
                RepositoryNode queryNode = createQueryNode(node, repObj, query);
                if (SubItemHelper.isDeleted(query)) {
                    recBinNode.getChildren().add(queryNode);
                } else {
                    node.getChildren().add(queryNode);
                }

            }
        }
    }

    /**
     * DOC cantoine Comment method "createTable".
     * 
     * @param node
     * @param metadataTable
     * @param repositoryObjectType TODO
     * @param iMetadataConnection
     */
    private void createTable(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj,
            org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable,
            ERepositoryObjectType repositoryObjectType) {
        RepositoryNode tableNode = createMetatableNode(node, repObj, metadataTable, repositoryObjectType);
        if (TableHelper.isDeleted(metadataTable)) {
            recBinNode.getChildren().add(tableNode);
        } else {
            node.getChildren().add(tableNode);
        }
    }

    private void createTables(RepositoryNode recBinNode, RepositoryNode node, final IRepositoryObject repObj,
            Connection metadataConnection) {
        if (metadataConnection instanceof DatabaseConnection) {
            // 1.Tables:
            RepositoryNode tablesNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.TableSchemas"), ECoreImage.FOLDER_CLOSE_ICON);
            node.getChildren().add(tablesNode);

            // 2.VIEWS:
            RepositoryNode viewsNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.ViewSchemas"), ECoreImage.FOLDER_CLOSE_ICON);
            node.getChildren().add(viewsNode);

            // 3.SYNONYMS:
            RepositoryNode synonymsNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.SynonymSchemas"),
                    ECoreImage.FOLDER_CLOSE_ICON);
            node.getChildren().add(synonymsNode);

            Iterator metadataTables = metadataConnection.getTables().iterator();
            while (metadataTables.hasNext()) {
                org.talend.core.model.metadata.builder.connection.MetadataTable metadataTable = (org.talend.core.model.metadata.builder.connection.MetadataTable) metadataTables
                        .next();

                String typeTable = null;
                if (metadataTable.getTableType() != null) {
                    typeTable = metadataTable.getTableType();
                    if (typeTable.equals("TABLE")) {
                        createTable(recBinNode, tablesNode, repObj, metadataTable,
                                ERepositoryObjectType.METADATA_CON_TABLE);

                    } else if (typeTable.equals("VIEW")) {
                        createTable(recBinNode, viewsNode, repObj, metadataTable,
                                ERepositoryObjectType.METADATA_CON_TABLE);

                    } else if (typeTable.equals("SYNONYM")) {
                        createTable(recBinNode, synonymsNode, repObj, metadataTable,
                                ERepositoryObjectType.METADATA_CON_TABLE);
                    }
                } else {
                    createTable(recBinNode, tablesNode, repObj, metadataTable, ERepositoryObjectType.METADATA_CON_TABLE);
                }
            }
            // if (!node.getChildren().contains(tablesNode)) {
            // node.getChildren().add(tablesNode);
            // }

            // createTables(recBinNode, node, repObj, metadataConnection.getTables());

            // 4.Queries:
            RepositoryNode queriesNode = new StableRepositoryNode(node, Messages
                    .getString("RepositoryContentProvider.repositoryLabel.Queries"), ECoreImage.FOLDER_CLOSE_ICON);
            node.getChildren().add(queriesNode);
            QueriesConnection queriesConnection = ((Connection) metadataConnection).getQueries();
            if (queriesConnection != null) {
                createTables(recBinNode, queriesNode, repObj, queriesConnection.getQuery(),
                        ERepositoryObjectType.METADATA_CON_TABLE);
            }
        } else {
            createTables(recBinNode, node, repObj, metadataConnection.getTables(),
                    ERepositoryObjectType.METADATA_CON_TABLE);
        }
    }

    /**
     * DOC tguiu Comment method "createMetatable".
     * 
     * @param node
     * @param table
     * @param repositoryObjectType TODO
     * @param iMetadataFileDelimited
     * @return
     */
    private RepositoryNode createMetatableNode(RepositoryNode node, IRepositoryObject repObj,
            final org.talend.core.model.metadata.builder.connection.MetadataTable table,
            ERepositoryObjectType repositoryObjectType) {
        MetadataTable modelObj = new MetadataTableRepositoryObject(repObj, table);
        modelObj.setLabel(table.getLabel());
        RepositoryNode tableNode = new RepositoryNode(modelObj, node, ENodeType.REPOSITORY_ELEMENT);
        tableNode.setProperties(EProperties.LABEL, table.getLabel());
        tableNode.setProperties(EProperties.CONTENT_TYPE, repositoryObjectType);
        return tableNode;
    }

    /**
     * DOC cantoine Comment method "createQueryNode".
     * 
     * @param node
     * @param repObj
     * @param query
     * @return
     */
    private RepositoryNode createQueryNode(RepositoryNode node, IRepositoryObject repObj, Query query) {
        QueryRepositoryObject modelObj = new QueryRepositoryObject(repObj, query);
        modelObj.setLabel(query.getLabel());
        RepositoryNode tableNode = new RepositoryNode(modelObj, node, ENodeType.REPOSITORY_ELEMENT);
        tableNode.setProperties(EProperties.LABEL, query.getLabel());
        tableNode.setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.METADATA_CON_QUERY);
        return tableNode;
    }

    /**
     */
    public interface ISubRepositoryObject {

        public AbstractMetadataObject getAbstractMetadataObject();

        public void removeFromParent();

        public Property getProperty();
    }

    /**
     */
    public static class MetadataTableRepositoryObject extends MetadataTable implements ISubRepositoryObject {

        private IRepositoryObject repObj;

        private org.talend.core.model.metadata.builder.connection.MetadataTable table;

        public MetadataTableRepositoryObject(IRepositoryObject repObj,
                org.talend.core.model.metadata.builder.connection.MetadataTable table) {
            this.repObj = repObj;
            this.table = table;
        }

        public Property getProperty() {
            return repObj.getProperty();
        }

        public void setProperty(Property property) {
            repObj.setProperty(property);
        }

        public String getVersion() {
            return repObj.getVersion();
        }

        public String getLabel() {
            return table.getLabel();
        }

        public String getId() {
            return table.getId();
        }

        public String getTableType() {
            return table.getTableType();
        }

        public org.talend.core.model.metadata.builder.connection.MetadataTable getTable() {
            return this.table;
        }

        public AbstractMetadataObject getAbstractMetadataObject() {
            return getTable();
        }

        public void removeFromParent() {
            table.getConnection().getTables().remove(table);
        }
    }

    /**
     */
    public static class QueryRepositoryObject extends org.talend.core.model.metadata.Query implements
            ISubRepositoryObject {

        private IRepositoryObject repObj;

        private Query query;

        public QueryRepositoryObject(IRepositoryObject repObj, Query table) {
            this.repObj = repObj;
            this.query = table;
        }

        public Property getProperty() {
            return repObj.getProperty();
        }

        public void setProperty(Property property) {
            repObj.setProperty(property);
        }

        public String getVersion() {
            return repObj.getVersion();
        }

        public String getLabel() {
            return query.getLabel();
        }

        public String getId() {
            return query.getId();
        }

        public Query getQuery() {
            return query;
        }

        public AbstractMetadataObject getAbstractMetadataObject() {
            return getQuery();
        }

        public void removeFromParent() {
            query.getQueries().getQuery().remove(query);
        }
    }

}

