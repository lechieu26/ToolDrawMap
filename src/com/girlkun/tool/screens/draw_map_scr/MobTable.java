package com.girlkun.tool.screens.draw_map_scr;

import com.girlkun.tool.entities.map.MobTemplate;
import com.girlkun.tool.main.Manager;
import com.girlkun.tool.utils.Util;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class MobTable extends JFrame {
   private DrawMapScr drawMapScr;
   private DefaultTableModel model1;
   private JScrollPane jScrollPane2;
   private JTable tbl1;

   public MobTable(DrawMapScr drawMapScr) {
      this.drawMapScr = drawMapScr;
      this.initComponents();
      this.setup();
      this.setDefaultCloseOperation(2);
      this.setTitle("Girlkun75 - Mob template");
   }

   private void initComponents() {
      this.jScrollPane2 = new JScrollPane();
      this.tbl1 = new JTable();
      this.setDefaultCloseOperation(3);
      this.tbl1.setModel(new DefaultTableModel(new Object[0][], new String[0]));
      this.tbl1.setSelectionMode(0);
      this.tbl1.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent evt) {
            MobTable.this.tbl1MouseClicked(evt);
         }
      });
      this.tbl1.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent evt) {
            MobTable.this.tbl1KeyPressed(evt);
         }

         @Override
         public void keyReleased(KeyEvent evt) {
            MobTable.this.tbl1KeyReleased(evt);
         }
      });
      this.jScrollPane2.setViewportView(this.tbl1);
      GroupLayout layout = new GroupLayout(this.getContentPane());
      this.getContentPane().setLayout(layout);
      layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.jScrollPane2, -2, -1, -2));
      layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.jScrollPane2, -2, 274, -2));
      this.pack();
   }

   private void tbl1MouseClicked(MouseEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setMobtemplateChoose(Manager.gI().getMobTemplates().get(index));
      }
   }

   private void tbl1KeyPressed(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setMobtemplateChoose(Manager.gI().getMobTemplates().get(index));
      }
   }

   private void tbl1KeyReleased(KeyEvent evt) {
      int index = this.tbl1.getSelectedRow();
      if (index != -1) {
         this.drawMapScr.setMobtemplateChoose(Manager.gI().getMobTemplates().get(index));
      }
   }

   public void load() {
      this.fillToTable();
   }

   private void fillToTable() {
      this.model1.setRowCount(0);

      for (MobTemplate mob : Manager.gI().getMobTemplates()) {
         this.model1.addRow(new Object[]{mob.getId(), mob.getName(), mob.getId()});
      }
   }

   private void setup() {
      this.setResizable(false);
      this.setLocationRelativeTo(null);
      this.model1 = new DefaultTableModel(new String[]{"ID", "Name", "Image"}, 0) {
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      this.tbl1.setModel(this.model1);
      this.tbl1.setRowHeight(50);
      this.tbl1.getColumnModel().getColumn(2).setCellRenderer(new MobTable.ImageRender());
      this.fillToTable();
   }

   public static void main(String[] args) {
      try {
         for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
               UIManager.setLookAndFeel(info.getClassName());
               break;
            }
         }
      } catch (ClassNotFoundException var5) {
         Logger.getLogger(MobTable.class.getName()).log(Level.SEVERE, null, var5);
      } catch (InstantiationException var6) {
         Logger.getLogger(MobTable.class.getName()).log(Level.SEVERE, null, var6);
      } catch (IllegalAccessException var7) {
         Logger.getLogger(MobTable.class.getName()).log(Level.SEVERE, null, var7);
      } catch (UnsupportedLookAndFeelException var8) {
         Logger.getLogger(MobTable.class.getName()).log(Level.SEVERE, null, var8);
      }

      EventQueue.invokeLater(new Runnable() {
         @Override
         public void run() {
            new MobTable(null).setVisible(true);
         }
      });
   }

   private class ImageRender extends DefaultTableCellRenderer {
      private ImageRender() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         ImageIcon icon = null;

         try {
            String id = value.toString();
            Image image = Util.getImageMobById(Integer.parseInt(id), 0);
            image = image.getScaledInstance(40, 40, 4);
            icon = new ImageIcon(image);
         } catch (Exception var10) {
         }

         return new JLabel(icon);
      }
   }
}
