package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.tree.TreeDataModelEvent;
import com.sun.star.awt.tree.XTreeDataModelListener;
import com.sun.star.lang.EventObject;

public abstract class AbstractTreeDataModelListener implements XTreeDataModelListener
{

  @Override
  public void disposing(EventObject arg0)
  {
    // default implementation
    
  }

  @Override
  public void treeNodesChanged(TreeDataModelEvent arg0)
  {
    // default implementation
    
  }

  @Override
  public void treeNodesInserted(TreeDataModelEvent arg0)
  {
    // default implementation
    
  }

  @Override
  public void treeNodesRemoved(TreeDataModelEvent arg0)
  {
    // default implementation
    
  }

  @Override
  public void treeStructureChanged(TreeDataModelEvent arg0)
  {
    // default implementation
    
  }

}
