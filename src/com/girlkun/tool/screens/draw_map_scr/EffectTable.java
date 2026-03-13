package com.girlkun.tool.screens.draw_map_scr;

import com.girlkun.button.Button;
import com.girlkun.tool.entities.EffectTemplate;
import com.girlkun.tool.main.Manager;
import com.girlkun.tool.utils.Util;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class EffectTable extends JFrame {
   private DrawMapScr drawMapScr;
   private DefaultTableModel model1;
   private DefaultTableModel model2;
   private int indexBE = -1;
   private List<Integer> listBE = new ArrayList<>();
   private Button button1;
   private JLabel jLabel2;
   private JScrollPane jScrollPane2;
   private JScrollPane jScrollPane3;
   private JLabel lblLayer;
   private JTable tbl1;
   private JTable tbl2;
   private JTextField txtLayer;

   public EffectTable(DrawMapScr drawMapScr) {
      this.drawMapScr = drawMapScr;
      this.initComponents();
      this.setup();
      this.setDefaultCloseOperation(2);
      this.setTitle("Girlkun75 - Effect template");
   }

   private void initComponents() {
      this.jScrollPane2 = new JScrollPane();
      this.tbl1 = new JTable();
      this.lblLayer = new JLabel();
      this.txtLayer = new JTextField();
      this.jLabel2 = new JLabel();
      this.jScrollPane3 = new JScrollPane();
      this.tbl2 = new JTable();
      this.button1 = new Button();
      this.setDefaultCloseOperation(3);
      this.tbl1.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl1.setSelectionMode(0);
      this.tbl1.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            EffectTable.this.tbl1MouseClicked(evt);
         }
      });
      this.tbl1.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            EffectTable.this.tbl1KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            EffectTable.this.tbl1KeyReleased(evt);
         }
      });
      this.jScrollPane2.setViewportView(this.tbl1);
      this.lblLayer.setFont(new Font("SansSerif", 1, 12));
      this.lblLayer.setText("Layer (2)");
      this.txtLayer.setFont(new Font("SansSerif", 1, 12));
      this.txtLayer.setText("2");
      this.txtLayer.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent evt) {
            EffectTable.this.txtLayerKeyReleased(evt);
         }
      });
      this.jLabel2.setFont(new Font("SansSerif", 1, 12));
      this.jLabel2.setText("Layer: Bg(-1), 0, 1, In tilemap(2), 3");
      this.tbl2.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl2.setSelectionMode(0);
      this.tbl2.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            EffectTable.this.tbl2MouseClicked(evt);
         }
      });
      this.tbl2.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            EffectTable.this.tbl2KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            EffectTable.this.tbl2KeyReleased(evt);
         }
      });
      this.jScrollPane3.setViewportView(this.tbl2);
      this.button1.setBackground(new Color(204, 0, 204));
      this.button1.setForeground(new Color(255, 255, 255));
      this.button1.setText("Add");
      this.button1.setFont(new Font("SansSerif", 1, 14));
      this.button1.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            EffectTable.this.button1ActionPerformed(evt);
         }
      });
      GroupLayout layout = new GroupLayout(this.getContentPane());
      this.getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(
               layout.createSequentialGroup()
                  .addGroup(
                     layout.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(this.jScrollPane2, Alignment.LEADING, -2, 0, 32767)
                        .addGroup(
                           layout.createSequentialGroup()
                              .addComponent(this.lblLayer, -2, 58, -2)
                              .addPreferredGap(ComponentPlacement.RELATED)
                              .addComponent(this.txtLayer, -2, 62, -2)
                              .addPreferredGap(ComponentPlacement.RELATED)
                              .addComponent(this.jLabel2, -2, 203, -2)
                        )
                  )
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addComponent(this.jScrollPane3, -2, 331, -2)
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addComponent(this.button1, -1, 100, 32767)
            )
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(
               layout.createSequentialGroup()
                  .addGroup(
                     layout.createParallelGroup(Alignment.LEADING).addComponent(this.jScrollPane2, -2, 274, -2).addComponent(this.jScrollPane3, -2, 274, -2)
                  )
                  .addPreferredGap(ComponentPlacement.RELATED)
                  .addGroup(
                     layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(this.lblLayer, -1, -1, 32767)
                        .addComponent(this.txtLayer, -2, 33, -2)
                        .addComponent(this.jLabel2)
                  )
            )
            .addGroup(layout.createSequentialGroup().addComponent(this.button1, -2, 46, -2).addGap(0, 0, 32767))
      );
      layout.linkSize(1, this.jLabel2, this.txtLayer);
      this.pack();
   }

   private void tbl1MouseClicked(MouseEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setEffectTemplateChose(Manager.gI().getEffectTemplates().get(index));
      }
   }

   private void tbl1KeyPressed(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setEffectTemplateChose(Manager.gI().getEffectTemplates().get(index));
      }
   }

   private void tbl1KeyReleased(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setEffectTemplateChose(Manager.gI().getEffectTemplates().get(index));
      }
   }

   private void txtLayerKeyReleased(KeyEvent evt) {
      try {
         this.drawMapScr.layerEff = Integer.parseInt(this.txtLayer.getText());
         if (this.drawMapScr.layerEff < -1) {
            this.drawMapScr.layerEff = -1;
         } else if (this.drawMapScr.layerEff > 3) {
            this.txtLayer.setText("-1");
            this.drawMapScr.layerEff = 3;
            this.txtLayer.setText("3");
         }

         this.lblLayer.setText("Layer (" + this.drawMapScr.layerEff + ")");
      } catch (Exception var3) {
      }
   }

   private void tbl2MouseClicked(MouseEvent evt) {
      this.indexBE = this.tbl2.getSelectedRow();
   }

   private void tbl2KeyPressed(KeyEvent evt) {
   }

   private void tbl2KeyReleased(KeyEvent evt) {
   }

   private void button1ActionPerformed(ActionEvent evt) {
      if (this.indexBE != -1) {
         this.drawMapScr.addBEff(this.listBE.get(this.indexBE));
      }
   }

   public void load() {
      this.fillToTable();
   }

   private void fillToTable() {
      this.model1.setRowCount(0);
      this.model2.setRowCount(0); // Also clear model2 for consistency

      for (EffectTemplate eff : Manager.gI().getEffectTemplates()) {
         this.model1.addRow(new Object[]{eff.getId(), eff.getId()});
      }

      for (Integer id : this.listBE) {
         this.model2.addRow(new Object[]{id, "beff-" + id});
      }
   }

   private void setup() {
      this.setResizable(false);
      this.setLocationRelativeTo(null);
      this.model1 = new DefaultTableModel(new String[]{"ID", "Image"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.model2 = new DefaultTableModel(new String[]{"ID", "Image"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl1.setModel(this.model1);
      this.tbl2.setModel(this.model2);
      this.tbl1.setRowHeight(50);
      this.tbl2.setRowHeight(72);
      this.tbl1.getColumnModel().getColumn(1).setCellRenderer(new EffectTable.ImageRender());
      this.tbl2.getColumnModel().getColumn(1).setCellRenderer(new EffectTable.ImageRender());
      this.listBE.add(1);
      this.listBE.add(2);
      this.listBE.add(5);
      this.listBE.add(6);
      this.listBE.add(7);
      this.listBE.add(11);
      this.listBE.add(4);
      this.listBE.add(12);
      this.fillToTable();
   }

   private class ImageRender extends DefaultTableCellRenderer {
      private ImageRender() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         ImageIcon icon = null;
         String vl = value.toString();

         try {
            Image image = null;
            if (vl.startsWith("beff-")) {
               Image var12 = Util.getImageBEffectByType(Integer.parseInt(vl.replaceAll("beff-", "")));
               image = var12.getScaledInstance(20, 72, 4);
            } else {
               Image var14 = Util.getImageEffectById(Integer.parseInt(vl));
               image = var14.getScaledInstance(40, 40, 4);
            }

            icon = new ImageIcon(image);
         } catch (Exception var10) {
            return new JLabel(vl);
         }

         return new JLabel(icon);
      }
   }
}
