package com.girlkun.tool.screens.draw_map_scr;

import com.girlkun.tool.entities.map.BgItemTemplate;
import com.girlkun.tool.main.Manager;
import com.girlkun.tool.utils.Util;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class BGItemTable extends JFrame {
   private DrawMapScr drawMapScr;
   private DefaultTableModel model1;
   private DefaultTableModel model2;
   private DefaultTableModel model3;
   private DefaultTableModel model4;
   private List<BgItemTemplate> itemLayer1 = new ArrayList<>();
   private List<BgItemTemplate> itemLayer2 = new ArrayList<>();
   private List<BgItemTemplate> itemLayer3 = new ArrayList<>();
   private List<BgItemTemplate> itemLayer4 = new ArrayList<>();
   private JScrollPane jScrollPane1;
   private JScrollPane jScrollPane2;
   private JScrollPane jScrollPane5;
   private JScrollPane jScrollPane6;
   private JTable tbl1;
   private JTable tbl2;
   private JTable tbl3;
   private JTable tbl4;

   public BGItemTable(DrawMapScr drawMapScr) {
      this.drawMapScr = drawMapScr;
      this.initComponents();
      this.setup();
      this.setDefaultCloseOperation(2);
      this.setTitle("Girlkun75 - Background item template");
      this.itemLayer1.clear();
      this.itemLayer2.clear();
      this.itemLayer3.clear();
      this.itemLayer4.clear();

      for (BgItemTemplate temp : Manager.gI().getBgItemTemplates()) {
         if (temp.getLayer() == 1) {
            this.itemLayer1.add(temp);
         } else if (temp.getLayer() == 2) {
            this.itemLayer2.add(temp);
         } else if (temp.getLayer() == 3) {
            this.itemLayer3.add(temp);
         } else if (temp.getLayer() == 4) {
            this.itemLayer4.add(temp);
         }
      }

      this.fillToTable();
   }

   private void initComponents() {
      this.jScrollPane1 = new JScrollPane();
      this.tbl2 = new JTable();
      this.jScrollPane2 = new JScrollPane();
      this.tbl1 = new JTable();
      this.jScrollPane5 = new JScrollPane();
      this.tbl3 = new JTable();
      this.jScrollPane6 = new JScrollPane();
      this.tbl4 = new JTable();
      this.setDefaultCloseOperation(3);
      this.tbl2.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl2.setSelectionMode(0);
      this.tbl2.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            BGItemTable.this.tbl2MouseClicked(evt);
         }
      });
      this.tbl2.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            BGItemTable.this.tbl2KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            BGItemTable.this.tbl2KeyReleased(evt);
         }
      });
      this.jScrollPane1.setViewportView(this.tbl2);
      this.tbl1.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl1.setSelectionMode(0);
      this.tbl1.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            BGItemTable.this.tbl1MouseClicked(evt);
         }
      });
      this.tbl1.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            BGItemTable.this.tbl1KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            BGItemTable.this.tbl1KeyReleased(evt);
         }
      });
      this.jScrollPane2.setViewportView(this.tbl1);
      this.tbl3.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl3.setSelectionMode(0);
      this.tbl3.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            BGItemTable.this.tbl3MouseClicked(evt);
         }
      });
      this.tbl3.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            BGItemTable.this.tbl3KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            BGItemTable.this.tbl3KeyReleased(evt);
         }
      });
      this.jScrollPane5.setViewportView(this.tbl3);
      this.tbl4.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl4.setSelectionMode(0);
      this.tbl4.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            BGItemTable.this.tbl4MouseClicked(evt);
         }
      });
      this.tbl4.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            BGItemTable.this.tbl4KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            BGItemTable.this.tbl4KeyReleased(evt);
         }
      });
      this.jScrollPane6.setViewportView(this.tbl4);
      GroupLayout layout = new GroupLayout(this.getContentPane());
      this.getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(
               layout.createSequentialGroup()
                  .addComponent(this.jScrollPane2, -1, 259, 32767)
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addComponent(this.jScrollPane1, -1, 258, 32767)
            )
            .addGroup(
               layout.createSequentialGroup()
                  .addComponent(this.jScrollPane5, -1, 259, 32767)
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addComponent(this.jScrollPane6, -1, 258, 32767)
            )
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(
               layout.createSequentialGroup()
                  .addGroup(
                     layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(this.jScrollPane1, -1, 274, 32767)
                        .addComponent(this.jScrollPane2, -2, 0, 32767)
                  )
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addGroup(
                     layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(this.jScrollPane6, -1, 274, 32767)
                        .addComponent(this.jScrollPane5, -2, 0, 32767)
                  )
                  .addGap(0, 0, 32767)
            )
      );
      this.pack();
   }

   private void tbl1MouseClicked(MouseEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer1.get(index), 1);
      }
   }

   private void tbl1KeyPressed(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer1.get(index), 1);
      }
   }

   private void tbl1KeyReleased(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer1.get(index), 1);
      }
   }

   private void tbl2MouseClicked(MouseEvent evt) {
      int index = this.tbl2.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer2.get(index), 2);
      }
   }

   private void tbl2KeyPressed(KeyEvent evt) {
      int index = this.tbl2.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer2.get(index), 2);
      }
   }

   private void tbl2KeyReleased(KeyEvent evt) {
      int index = this.tbl2.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer2.get(index), 2);
      }
   }

   private void tbl3MouseClicked(MouseEvent evt) {
      int index = this.tbl3.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer3.get(index), 3);
      }
   }

   private void tbl3KeyPressed(KeyEvent evt) {
      int index = this.tbl3.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer3.get(index), 3);
      }
   }

   private void tbl3KeyReleased(KeyEvent evt) {
      int index = this.tbl3.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer3.get(index), 3);
      }
   }

   private void tbl4MouseClicked(MouseEvent evt) {
      int index = this.tbl4.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer4.get(index), 4);
      }
   }

   private void tbl4KeyPressed(KeyEvent evt) {
      int index = this.tbl4.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer4.get(index), 4);
      }
   }

   private void tbl4KeyReleased(KeyEvent evt) {
      int index = this.tbl4.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setBGItemTemplateChoose(this.itemLayer4.get(index), 4);
      }
   }

   private void fillToTable() {
      this.model1.setRowCount(0);
      this.model2.setRowCount(0);
      this.model3.setRowCount(0);
      this.model4.setRowCount(0);

      for (BgItemTemplate temp : this.itemLayer1) {
         this.model1.addRow(new Object[]{temp.getId(), temp.getImageId(), temp.getImageId()});
      }

      for (BgItemTemplate temp : this.itemLayer2) {
         this.model2.addRow(new Object[]{temp.getId(), temp.getImageId(), temp.getImageId()});
      }

      for (BgItemTemplate temp : this.itemLayer3) {
         this.model3.addRow(new Object[]{temp.getId(), temp.getImageId(), temp.getImageId()});
      }

      for (BgItemTemplate temp : this.itemLayer4) {
         this.model4.addRow(new Object[]{temp.getId(), temp.getImageId(), temp.getImageId()});
      }
   }

   private void setup() {
      this.setResizable(false);
      this.setLocationRelativeTo(null);
      this.model1 = new DefaultTableModel(new String[]{"ID", "Image", "Image id"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl1.setModel(this.model1);
      this.model2 = new DefaultTableModel(new String[]{"ID", "Image", "Image id"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl2.setModel(this.model2);
      this.model3 = new DefaultTableModel(new String[]{"ID", "Image", "Image id"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl3.setModel(this.model3);
      this.model4 = new DefaultTableModel(new String[]{"ID", "Image", "Image id"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl4.setModel(this.model4);
      this.tbl1.setRowHeight(50);
      this.tbl2.setRowHeight(50);
      this.tbl3.setRowHeight(50);
      this.tbl4.setRowHeight(50);
      this.tbl1.getColumnModel().getColumn(1).setCellRenderer(new BGItemTable.ImageRender());
      this.tbl2.getColumnModel().getColumn(1).setCellRenderer(new BGItemTable.ImageRender());
      this.tbl3.getColumnModel().getColumn(1).setCellRenderer(new BGItemTable.ImageRender());
      this.tbl4.getColumnModel().getColumn(1).setCellRenderer(new BGItemTable.ImageRender());
   }

   private class ImageRender extends DefaultTableCellRenderer {
      private ImageRender() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         ImageIcon icon = null;

         try {
            String id = value.toString();
            Image image = Util.getBgImageById(Integer.parseInt(id), 1);
            image = image.getScaledInstance(40, 40, 4);
            icon = new ImageIcon(image);
         } catch (Exception var10) {
         }

         return new JLabel(icon);
      }
   }
}
