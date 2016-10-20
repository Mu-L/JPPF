/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.nodeconfig.NodeConfigLink;
import org.jppf.admin.web.topology.nodethreads.NodeThreadsLink;
import org.jppf.admin.web.topology.provisioning.ProvisioningLink;
import org.jppf.admin.web.topology.serverstop.DriverStopRestartLink;
import org.jppf.admin.web.topology.systeminfo.SystemInfoLink;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.ui.utils.TopologyUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * This web page displays the topology tree.
 * @author Laurent Cohen
 */
@MountPath("topology")
public class TopologyTree extends AbstractTableTreePage implements TopologyListener {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyTree.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialize this web page.
   */
  public TopologyTree() {
    super(TreeViewType.TOPOLOGY, "topology");
  }

  @Override
  protected void createTreeTableModel() {
    TableTreeData data = getJPPFSession().getTableTreeData(viewType);
    treeModel = data.getModel();
    if (treeModel == null) {
      JPPFWebConsoleApplication app = (JPPFWebConsoleApplication) getApplication();
      treeModel = new NodeTreeTableModel(new DefaultMutableTreeNode(app.localize("tree.root.name")), getJPPFSession().getLocale());
      populateTreeTableModel();
      data.setModel(treeModel);
      app.getTopologyManager().addTopologyListener(this);
    }
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  protected synchronized void populateTreeTableModel() {
    for (TopologyDriver driver : getJPPFApplication().getTopologyManager().getDrivers()) {
      TopologyUtils.addDriver(treeModel, driver);
      for (AbstractTopologyComponent child : driver.getChildren()) {
        TopologyUtils.addNode(treeModel, driver, (TopologyNode) child);
      }
    }
  }

  @Override
  protected List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    Locale locale = getSession().getLocale();
    if (locale == null) locale = Locale.US;
    columns.add(new TopologyTreeColumn(Model.of("Tree")));
    columns.add(new TopologyColumn(NodeTreeTableModel.NODE_THREADS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NODE_STATUS));
    columns.add(new TopologyColumn(NodeTreeTableModel.EXECUTION_STATUS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NB_TASKS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NB_SLAVES));
    columns.add(new TopologyColumn(NodeTreeTableModel.PENDING_ACTION));
    return columns;
  }

  @Override
  protected void createActions() {
    ActionHandler actionHandler = getJPPFSession().getTableTreeData(viewType).getActionHandler();
    actionHandler.addActionLink(toolbar, new DriverStopRestartLink(toolbar));
    actionHandler.addActionLink(toolbar, new ServerResetStatsLink());
    actionHandler.addActionLink(toolbar, new SystemInfoLink(toolbar));
    actionHandler.addActionLink(toolbar, new NodeConfigLink(toolbar));
    actionHandler.addActionLink(toolbar, new NodeThreadsLink(toolbar));
    actionHandler.addActionLink(toolbar, new ResetTaskCounterLink());
    actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.STOP));
    actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.RESTART));
    actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.STOP_DEFERRED));
    actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.RESTART_DEFERRED));
    actionHandler.addActionLink(toolbar, new CancelPendingActionLink());
    actionHandler.addActionLink(toolbar, new SuspendNodeLink());
    actionHandler.addActionLink(toolbar, new ProvisioningLink(toolbar));
    actionHandler.addActionLink(toolbar, new ExpandAllLink());
    actionHandler.addActionLink(toolbar, new CollapseAllLink());
    actionHandler.addActionLink(toolbar, new SelectDriversLink(TreeViewType.TOPOLOGY));
    actionHandler.addActionLink(toolbar, new SelectNodesLink(TreeViewType.TOPOLOGY));
    actionHandler.addActionLink(toolbar, new SelectAllLink(TreeViewType.TOPOLOGY));
  }


  @Override
  public void driverAdded(final TopologyEvent event) {
    TopologyUtils.addDriver(treeModel, event.getDriver());
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    TopologyUtils.removeDriver(treeModel, event.getDriver());
    selectionHandler.unselect(event.getDriver().getUuid());
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    DefaultMutableTreeNode node = TopologyUtils.addNode(treeModel, event.getDriver(), event.getNodeOrPeer());
    if (node != null) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
      if (parent.getChildCount() == 1) tableTree.expand(parent);
    }
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    TopologyUtils.removeNode(treeModel, event.getDriver(), event.getNodeOrPeer());
    selectionHandler.unselect(event.getNodeOrPeer().getUuid());
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.NODE_STATE) {
      TopologyUtils.updateNode(treeModel, event.getDriver(), event.getNodeOrPeer());
    }
  }

  /**
   * This class renders cells of the first column as tree.
   */
  public class TopologyTreeColumn extends TreeColumn<DefaultMutableTreeNode, String> {
    /** Explicit serailVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this column.
     * @param displayModel the header display string.
     */
    public TopologyTreeColumn(final IModel<String> displayModel) {
      super(displayModel);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      super.populateItem(cellItem, componentId, rowModel);
      DefaultMutableTreeNode node = rowModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) node.getUserObject();
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp.isPeer()) cssClass = "peer";
      else if (comp.isNode()) {
        TopologyNode data = (TopologyNode) node.getUserObject();
        if (traceEnabled) log.trace("node status: {}", data.getStatus());
        inactive = !data.getManagementInfo().isActive();
        if (data.getStatus() == TopologyNodeStatus.UP) {
          if (inactive) cssClass = (selected) ? "tree_inactive_selected" : "tree_inactive";
          else cssClass = (selected) ? "tree_selected" : "node_up";
        }
        else cssClass = (selected) ? "tree_inactive_selected" : "node_tree_down";
      } else if (comp.isDriver()) {
        TopologyDriver driver = (TopologyDriver) node.getUserObject();
        if (driver.getConnection().getStatus().isWorkingStatus()) cssClass = (selected) ? "tree_selected" : "driver_up";
        else cssClass = (selected) ? "tree_inactive_selected" : "driver_down";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }
  }

  /**
   * This class renders cells of each columns except the first.
   */
  public class TopologyColumn extends AbstractColumn<DefaultMutableTreeNode, String> {
    /** Explicit serailVersionUID. */
    private static final long serialVersionUID = 1L;
    /**
     * The column index.
     */
    private final int index;

    /**
     * Initialize this column.
     * @param index the column index.
     */
    public TopologyColumn(final int index) {
      super(Model.of(treeModel.getColumnName(index)));
      this.index = index;
      if (debugEnabled) log.debug("adding column index {}", index);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      NodeModel<DefaultMutableTreeNode> nodeModel = (NodeModel<DefaultMutableTreeNode>) rowModel;
      DefaultMutableTreeNode treeNode = nodeModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      String value = (String) treeModel.getValueAt(treeNode, index);
      cellItem.add(new Label(componentId, value));
      if (traceEnabled) log.trace(String.format("index %d populating value=%s, treeNode=%s", index, value, treeNode));
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      if (comp.isNode()) {
        TopologyNode data = (TopologyNode) treeNode.getUserObject();
        if (data.isNode()) cssClass = ((data.getStatus() == TopologyNodeStatus.UP) ? "node_up " : "node_down ") + getCssClass();
      } else if (!selected) cssClass = "empty";
      if (selected && !comp.isPeer()) {
        if (cssClass == null) cssClass = "tree_selected";
        else cssClass += " tree_selected";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case NodeTreeTableModel.NB_SLAVES:
        case NodeTreeTableModel.NB_TASKS:
          return "number";
      }
      return "string";
    }
  }
}