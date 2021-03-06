/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2009 Sun Microsystems, Inc.
 * Portions Copyright 2015-2016 ForgeRock AS.
 */

package org.opends.guitools.controlpanel.ui.renderer;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.opends.guitools.controlpanel.ui.ColorAndFontConstants;

/** Class used to render the task tables. */
public class TaskCellRenderer extends DefaultTableCellRenderer
{
  private static final long serialVersionUID = -84332267021523835L;
  /** The border of the first column. TODO: modify CustomCellRenderer to make this public. */
  private static final Border column0Border =
    BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 1, 0, 0,
          ColorAndFontConstants.gridColor),
          BorderFactory.createEmptyBorder(4, 4, 4, 4));
  /** The default border. */
  private static final Border defaultBorder = CustomCellRenderer.defaultBorder;

  /** Default constructor. */
  public TaskCellRenderer()
  {
    setFont(ColorAndFontConstants.tableFont);
    setOpaque(true);
    setBackground(ColorAndFontConstants.treeBackground);
    setForeground(ColorAndFontConstants.treeForeground);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
        row, column);

    if (hasFocus)
    {
      setBorder(
          CustomCellRenderer.getDefaultFocusBorder(table, value, isSelected,
              row, column));
    }
    else if (column == 0)
    {
      setBorder(column0Border);
    }
    else
    {
      setBorder(defaultBorder);
    }
    return this;
  }
}

