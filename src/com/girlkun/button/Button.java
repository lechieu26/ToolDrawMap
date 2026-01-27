package com.girlkun.button;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.TimingTargetAdapter;

public class Button extends JButton {
   private Animator animator;
   private int targetSize;
   private float animatSize;
   private Point pressedPoint;
   private float alpha;
   private Color effectColor = new Color(255, 255, 255);

   public Color getEffectColor() {
      return this.effectColor;
   }

   public void setEffectColor(Color effectColor) {
      this.effectColor = effectColor;
   }

   public Button() {
      this.setContentAreaFilled(false);
      this.setBorder(new EmptyBorder(5, 0, 5, 0));
      this.setBackground(Color.WHITE);
      this.setCursor(new Cursor(12));
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            Button.this.targetSize = Math.max(Button.this.getWidth(), Button.this.getHeight()) * 2;
            Button.this.animatSize = 0.0F;
            Button.this.pressedPoint = me.getPoint();
            Button.this.alpha = 0.5F;
            if (Button.this.animator.isRunning()) {
               Button.this.animator.stop();
            }

            Button.this.animator.start();
         }
      });
      TimingTarget target = new TimingTargetAdapter() {
         public void timingEvent(float fraction) {
            if (fraction > 0.5F) {
               Button.this.alpha = 1.0F - fraction;
            }

            Button.this.animatSize = fraction * (float)Button.this.targetSize;
            Button.this.repaint();
         }
      };
      this.animator = new Animator(500, target);
      this.animator.setResolution(0);
      this.animator.setAcceleration(0.5F);
      this.animator.setDeceleration(0.5F);
   }

   @Override
   protected void paintComponent(Graphics grphcs) {
      int width = this.getWidth();
      int height = this.getHeight();
      BufferedImage img = new BufferedImage(width, height, 2);
      Graphics2D g2 = img.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(this.getBackground());
      g2.fillRoundRect(0, 0, width, height, 5, 5);
      if (this.pressedPoint != null) {
         g2.setColor(this.effectColor);
         g2.setComposite(AlphaComposite.getInstance(10, this.alpha));
         g2.fillOval(
            (int)((float)this.pressedPoint.x - this.animatSize / 2.0F),
            (int)((float)this.pressedPoint.y - this.animatSize / 2.0F),
            (int)this.animatSize,
            (int)this.animatSize
         );
      }

      g2.dispose();
      grphcs.drawImage(img, 0, 0, null);
      super.paintComponent(grphcs);
   }
}
