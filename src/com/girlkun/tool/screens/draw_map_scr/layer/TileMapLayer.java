package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.map.TilesetType;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class TileMapLayer implements Layer {
   private TilesetType tileSetType;
   private BufferedImage[] tileSet;
   private int[][] tileMap;
   private BufferedImage image;
   private DrawMapScr drawMapScr;
   public static boolean drawTileType = false;
   public static boolean drawGrid = false;
   private boolean show = true;

   @Override
   public void clear() {
   }

   public TileMapLayer(DrawMapScr drawMapScr, int w, int h) {
      this.drawMapScr = drawMapScr;
      this.image = new BufferedImage(w * 24, h * 24, 2);
      this.tileMap = new int[h][w];
      this.clearMap();
   }

   public void clearMap() {
      for (int i = 0; i < this.tileMap.length; i++) {
         for (int j = 0; j < this.tileMap[i].length; j++) {
            this.tileMap[i][j] = -1;
         }
      }
   }

   @Override
   public void draw() {
      this.clearImage();
      if (this.isShow()) {
         this.drawTileMap();
      }
   }

   @Override
   public void setSizeImage(int w, int h, int offset, int dir) {
      this.image = new BufferedImage(w * 24, h * 24, 2);
   }

   private boolean drawTileMap(Graphics2D g) {
      int ys = -this.drawMapScr.camera.camY / 24 - 1;
      if (ys < 0) {
         ys = 0;
      }

      int ye = (-this.drawMapScr.camera.camY + this.drawMapScr.camera.height) / 24 + 1;
      if (ye >= this.tileMap.length) {
         ye = this.tileMap.length - 1;
      }

      int xs = -this.drawMapScr.camera.camX / 24 - 1;
      if (xs < 0) {
         xs = 0;
      }

      int xe = (-this.drawMapScr.camera.camX + this.drawMapScr.camera.width) / 24 + 1;
      if (xe >= this.tileMap[0].length) {
         xe = this.tileMap[0].length - 1;
      }

      for (int y = ys; y <= ye; y++) {
         for (int x = xs; x <= xe; x++) {
            if (this.tileSet != null && this.tileMap[y][x] != -1) {
               try {
                  g.drawImage(this.tileSet[this.tileMap[y][x]], x * 24, y * 24, null);
               } catch (Exception var12) {
               }
            }

            if (drawTileType) {
               if (this.tileSetType == null) {
                  this.tileSetType = this.drawMapScr.tileSetType;
               }

               if (this.tileSetType != null) {
                  List<Integer> tileType = this.tileSetType.tileType.get(this.tileMap[y][x] + 1);
                  if (tileType != null) {
                     g.setColor(Color.red);
                     g.setStroke(new BasicStroke(2.0F));

                     for (int tile : tileType) {
                        if (tile == 2) {
                           g.drawLine(x * 24, y * 24 + 5, x * 24 + 24, y * 24 + 5);
                        } else if (tile == 4) {
                           g.drawLine(x * 24 + 5, y * 24, x * 24 + 5, y * 24 + 24);
                        } else if (tile == 8) {
                           g.drawLine(x * 24 + 19, y * 24, x * 24 + 19, y * 24 + 24);
                        } else if (tile == 8192) {
                           g.drawLine(x * 24, y * 24 + 19, x * 24 + 24, y * 24 + 19);
                        }
                     }
                  }
               }
            }
         }
      }

      if (drawGrid) {
         int x = -this.drawMapScr.camera.camX;
         if (x < 0) {
            x = 0;
         }

         int y = -this.drawMapScr.camera.camY;
         if (y < 0) {
            y = 0;
         }

         int w = this.drawMapScr.camera.width;
         if (w + x > this.image.getWidth()) {
            w = this.image.getWidth() - x;
         }

         int h = this.drawMapScr.camera.height;
         if (h + y > this.image.getHeight()) {
            h = this.image.getHeight() - y;
         }

         g.setStroke(new BasicStroke(1.0F));
         g.setColor(Color.white);

         for (int yy = ys; yy <= ye; yy++) {
            g.drawLine(x, yy * 24, x + w, yy * 24);

            for (int xx = xs; xx <= xe; xx++) {
               g.drawLine(xx * 24, y, xx * 24, y + h);
            }
         }
      }

      return true;
   }

   private void drawTileMap() {
      Graphics2D g = this.image.createGraphics();
      if (!this.drawTileMap(g)) {
         for (int y = 0; y < this.tileMap.length; y++) {
            if (y * 24 + this.drawMapScr.camera.camY >= 0) {
               if (y * 24 + this.drawMapScr.camera.camY > this.drawMapScr.camera.height) {
                  break;
               }

               if (drawGrid) {
                  g.drawLine(0, y * 24, this.drawMapScr.camera.width - this.drawMapScr.camera.camX, y * 24);
               }

               for (int x = 0; x < this.tileMap[y].length; x++) {
                  if (x * 24 + this.drawMapScr.camera.camX >= 0) {
                     if (x * 24 + this.drawMapScr.camera.camX > this.drawMapScr.camera.width) {
                        break;
                     }

                     if (drawGrid) {
                        g.drawLine(x * 24, 0, x * 24, this.drawMapScr.camera.height - this.drawMapScr.camera.camY);
                     }

                     if (this.tileSet != null && this.tileMap[y][x] != -1) {
                        try {
                           g.drawImage(this.tileSet[this.tileMap[y][x]], x * 24, y * 24, null);
                        } catch (Exception var7) {
                        }
                     }

                     if (drawTileType) {
                        if (this.tileSetType == null) {
                           this.tileSetType = this.drawMapScr.tileSetType;
                        }

                        if (this.tileSetType != null) {
                           List<Integer> tileType = this.tileSetType.tileType.get(this.tileMap[y][x] + 1);
                           if (tileType != null) {
                              g.setColor(Color.BLACK);
                              g.setStroke(new BasicStroke(2.0F));

                              for (int tile : tileType) {
                                 if (tile == 2) {
                                    g.drawLine(x * 24, y * 24 + 5, x * 24 + 24, y * 24 + 5);
                                 } else if (tile == 4) {
                                    g.drawLine(x * 24 + 5, y * 24, x * 24 + 5, y * 24 + 24);
                                 } else if (tile == 8) {
                                    g.drawLine(x * 24 + 19, y * 24, x * 24 + 19, y * 24 + 24);
                                 } else if (tile == 8192) {
                                    g.drawLine(x * 24, y * 24 + 19, x * 24 + 24, y * 24 + 19);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void putTileMap(BufferedImage[] tileSet, int[][] indexTile, int x, int y) {
      if (this.tileSet == null) {
         this.tileSet = tileSet;
      }

      x /= 24;
      y /= 24;

      try {
         for (int yy = 0; yy < indexTile.length; yy++) {
            for (int xx = 0; xx < indexTile[yy].length; xx++) {
               try {
                  if (indexTile.length <= 1 && indexTile[yy].length <= 1 || indexTile[yy][xx] != -1) {
                     this.tileMap[yy + y][xx + x] = indexTile[yy][xx];
                  }
               } catch (Exception var8) {
               }
            }
         }
      } catch (Exception var9) {
      }
   }

   public void setTileMap(int[][] tileMap) {
      this.tileMap = tileMap;
      if (tileMap != null && tileMap.length != 0) {
         this.image = new BufferedImage(tileMap[0].length * 24, tileMap.length * 24, 2);
      }
   }

   @Override
   public void clearImage() {
      Graphics2D g = this.image.createGraphics();
      g.setComposite(AlphaComposite.Clear);
      int x = -this.drawMapScr.camera.camX;
      if (x < 0) {
         x = 0;
      }

      int y = -this.drawMapScr.camera.camY;
      if (y < 0) {
         y = 0;
      }

      int w = this.drawMapScr.camera.width;
      if (w + x > this.image.getWidth()) {
         w = this.image.getWidth() - x;
      }

      int h = this.drawMapScr.camera.height;
      if (h + y > this.image.getHeight()) {
         h = this.image.getHeight() - y;
      }

      g.fillRect(x, y, w, h);
   }

   public void drawItemChose(BufferedImage[][] tileChose, int x, int y) {
      if (tileChose != null) {
         x -= x % 24;
         y -= y % 24;
         Graphics2D g = this.image.createGraphics();

         for (int yy = 0; yy < tileChose.length; yy++) {
            for (int xx = 0; xx < tileChose[yy].length; xx++) {
               if (tileChose[yy][xx] != null) {
                  g.drawImage(tileChose[yy][xx], x + xx * 24, y + yy * 24, null);
                  g.setColor(Color.green);
                  if (yy == 0 || tileChose[yy - 1][xx] == null) {
                     g.drawLine(x + xx * 24, y + yy * 24, x + xx * 24 + 24, y + yy * 24);
                  }

                  if (yy == tileChose.length - 1 || tileChose[yy + 1][xx] == null) {
                     g.drawLine(x + xx * 24, y + yy * 24 + 24, x + xx * 24 + 24, y + yy * 24 + 24);
                  }

                  if (xx == 0 || tileChose[yy][xx - 1] == null) {
                     g.drawLine(x + xx * 24, y + yy * 24, x + xx * 24, y + yy * 24 + 24);
                  }

                  if (xx == tileChose[yy].length - 1 || tileChose[yy][xx + 1] == null) {
                     g.drawLine(x + xx * 24 + 24, y + yy * 24, x + xx * 24 + 24, y + yy * 24 + 24);
                  }
               }
            }
         }
      }
   }

   @Override
   public BufferedImage getBufferedImage() {
      return this.image;
   }

   public void copyTile(int x1, int y1, int x2, int y2) {
      x1 -= this.drawMapScr.camera.camX;
      x2 -= this.drawMapScr.camera.camX;
      y1 -= this.drawMapScr.camera.camY;
      y2 -= this.drawMapScr.camera.camY;
      int x = x1 < x2 ? x1 : x2;
      int y = y1 < y2 ? y1 : y2;
      x -= x % 24;
      y -= y % 24;
      x /= 24;
      y /= 24;
      int w = Math.abs(x1 - x2);
      int h = Math.abs(y1 - y2);
      w += 24 - w % 24;
      h += 24 - h % 24;
      w /= 24;
      h /= 24;
      int[][] indexTilesChose = new int[h][w];

      for (int i = 0; i < indexTilesChose.length; i++) {
         for (int j = 0; j < indexTilesChose[i].length; j++) {
            try {
               indexTilesChose[i][j] = this.tileMap[i + y][j + x];
            } catch (Exception var15) {
               indexTilesChose[i][j] = -1;
            }
         }
      }

      indexTilesChose = this.borderTileChose(indexTilesChose);
      BufferedImage[][] tilesChose = new BufferedImage[indexTilesChose.length][indexTilesChose[0].length];

      for (int i = 0; i < indexTilesChose.length; i++) {
         for (int j = 0; j < indexTilesChose[i].length; j++) {
            try {
               if (indexTilesChose[i][j] != -1) {
                  tilesChose[i][j] = this.tileSet[indexTilesChose[i][j]];
               }
            } catch (Exception var14) {
               indexTilesChose[i][j] = -1;
            }
         }
      }

      this.drawMapScr.indexTilesChose = indexTilesChose;
      this.drawMapScr.tilesChose = tilesChose;
      if (this.drawMapScr.tilesChose.length == 1 && this.drawMapScr.tilesChose[0].length == 1) {
         this.drawMapScr.tileChose = this.drawMapScr.tilesChose[0][0];
      }
   }

   private int[][] borderTileChose(int[][] tileFocus) {
      if (tileFocus.length == 1 && tileFocus[0].length == 1) {
         return tileFocus;
      } else {
         boolean left = false;
         boolean right = false;
         boolean top = false;
         boolean bottom = false;

         for (int i = 0; i < tileFocus.length; i++) {
            if (tileFocus[i][0] != -1) {
               left = true;
               break;
            }
         }

         if (!left && tileFocus[0].length > 1) {
            int[][] temp = new int[tileFocus.length][tileFocus[0].length - 1];

            for (int ih = 0; ih < tileFocus.length; ih++) {
               for (int iw = 1; iw < tileFocus[ih].length; iw++) {
                  temp[ih][iw - 1] = tileFocus[ih][iw];
               }
            }

            return this.borderTileChose(temp);
         } else {
            for (int ix = 0; ix < tileFocus.length; ix++) {
               if (tileFocus[ix][tileFocus[ix].length - 1] != -1) {
                  right = true;
                  break;
               }
            }

            if (!right && tileFocus[0].length > 1) {
               int[][] temp = new int[tileFocus.length][tileFocus[0].length - 1];

               for (int ih = 0; ih < tileFocus.length; ih++) {
                  for (int iw = 0; iw < tileFocus[ih].length - 1; iw++) {
                     temp[ih][iw] = tileFocus[ih][iw];
                  }
               }

               return this.borderTileChose(temp);
            } else {
               for (int ixx = 0; ixx < tileFocus[0].length; ixx++) {
                  if (tileFocus[0][ixx] != -1) {
                     top = true;
                     break;
                  }
               }

               if (!top && tileFocus.length > 1) {
                  int[][] temp = new int[tileFocus.length - 1][tileFocus[0].length];

                  for (int ih = 1; ih < tileFocus.length; ih++) {
                     for (int iw = 0; iw < tileFocus[ih].length; iw++) {
                        temp[ih - 1][iw] = tileFocus[ih][iw];
                     }
                  }

                  return this.borderTileChose(temp);
               } else {
                  for (int ixxx = 0; ixxx < tileFocus[0].length; ixxx++) {
                     if (tileFocus[tileFocus.length - 1][ixxx] != -1) {
                        bottom = true;
                        break;
                     }
                  }

                  if (!bottom && tileFocus.length > 1) {
                     int[][] temp = new int[tileFocus.length - 1][tileFocus[0].length];

                     for (int ih = 0; ih < tileFocus.length - 1; ih++) {
                        for (int iw = 0; iw < tileFocus[ih].length; iw++) {
                           temp[ih][iw] = tileFocus[ih][iw];
                        }
                     }

                     return this.borderTileChose(temp);
                  } else {
                     return tileFocus;
                  }
               }
            }
         }
      }
   }

   @Override
   public void setShow(boolean show) {
      this.show = show;
   }

   @Override
   public boolean isShow() {
      return this.show;
   }

   public TilesetType getTileSetType() {
      return this.tileSetType;
   }

   public BufferedImage[] getTileSet() {
      return this.tileSet;
   }

   public int[][] getTileMap() {
      return this.tileMap;
   }

   public BufferedImage getImage() {
      return this.image;
   }

   public DrawMapScr getDrawMapScr() {
      return this.drawMapScr;
   }

   public void setTileSetType(TilesetType tileSetType) {
      this.tileSetType = tileSetType;
   }

   public void setTileSet(BufferedImage[] tileSet) {
      this.tileSet = tileSet;
   }

   public void setImage(BufferedImage image) {
      this.image = image;
   }

   public void setDrawMapScr(DrawMapScr drawMapScr) {
      this.drawMapScr = drawMapScr;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof TileMapLayer)) {
         return false;
      } else {
         TileMapLayer other = (TileMapLayer) o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.isShow() != other.isShow()) {
            return false;
         } else {
            Object this$tileSetType = this.getTileSetType();
            Object other$tileSetType = other.getTileSetType();
            if (this$tileSetType == null ? other$tileSetType == null : this$tileSetType.equals(other$tileSetType)) {
               if (!Arrays.deepEquals(this.getTileSet(), other.getTileSet())) {
                  return false;
               } else if (!Arrays.deepEquals(this.getTileMap(), other.getTileMap())) {
                  return false;
               } else {
                  Object this$image = this.getImage();
                  Object other$image = other.getImage();
                  if (this$image == null ? other$image == null : this$image.equals(other$image)) {
                     Object this$drawMapScr = this.getDrawMapScr();
                     Object other$drawMapScr = other.getDrawMapScr();
                     return this$drawMapScr == null ? other$drawMapScr == null
                           : this$drawMapScr.equals(other$drawMapScr);
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof TileMapLayer;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + (this.isShow() ? 79 : 97);
      Object $tileSetType = this.getTileSetType();
      result = result * 59 + ($tileSetType == null ? 43 : $tileSetType.hashCode());
      result = result * 59 + Arrays.deepHashCode(this.getTileSet());
      result = result * 59 + Arrays.deepHashCode(this.getTileMap());
      Object $image = this.getImage();
      result = result * 59 + ($image == null ? 43 : $image.hashCode());
      Object $drawMapScr = this.getDrawMapScr();
      return result * 59 + ($drawMapScr == null ? 43 : $drawMapScr.hashCode());
   }

   @Override
   public String toString() {
      return "TileMapLayer(tileSetType="
            + this.getTileSetType()
            + ", tileSet="
            + Arrays.deepToString(this.getTileSet())
            + ", tileMap="
            + Arrays.deepToString(this.getTileMap())
            + ", image="
            + this.getImage()
            + ", drawMapScr="
            + this.getDrawMapScr()
            + ", show="
            + this.isShow()
            + ")";
   }
}
