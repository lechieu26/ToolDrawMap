# Draw Map Tool - NRO

## Mô tả
Công cụ vẽ và chỉnh sửa map cho game NRO. Được tách từ GirlkunToolCBRO.

## Yêu cầu
- **Java**: JDK 17 trở lên
- **IDE**: Apache NetBeans 12+ hoặc tương đương
- **Database**: MySQL 5.7+ hoặc MySQL 8.x

## Cách mở project trong NetBeans

1. Mở NetBeans
2. Chọn **File > Open Project...**
3. Chọn thư mục `DrawMapTool`
4. Click **Open Project**

## Cấu hình Database

Chỉnh sửa file `data/config/girlkundb.properties`:

```properties
girlkun.database.amount=1
girlkun.database.log=false

girlkun.database.ds.name.0=GIRLKUN
girlkun.database.driver.0=com.mysql.cj.jdbc.Driver
girlkun.database.host.0=localhost
girlkun.database.port.0=3306
girlkun.database.name.0=nrosamurai
girlkun.database.user.0=root
girlkun.database.pass.0=your_password
girlkun.database.min.0=1
girlkun.database.max.0=2
girlkun.database.lifetime.0=120000
```

## Database Tables cần thiết

- `map_template` - Thông tin các map
- `npc_template` - Thông tin NPC
- `mob_template` - Thông tin quái vật
- `bg_item_template` - Thông tin background items
- `head_avatar` - Avatar cho NPC

## Thư mục Data

- `data/tile/` - Tileset images (24x24 pixel mỗi tile)
- `data/girlkun/map/tile_map_data/` - Tile map data
- `data/girlkun/map/item_bg_map_data/` - Background items data
- `data/girlkun/map/eff_map/` - Effect map data
- `data/girlkun/effdata/x1/` - Effect templates
- `data/bg/` - Background images

## Cách chạy

### Từ NetBeans:
1. Right-click vào project
2. Chọn **Run** hoặc nhấn **F6**

### Từ Command Line:
```bash
cd DrawMapTool
ant run
```

## Phím tắt

- **Ctrl+D**: Mở cửa sổ Draw Map mới
- **Space + Mouse Drag**: Di chuyển camera
- **Ctrl + Click**: Chọn nhiều tile
- **E**: Xóa tile

## Các chức năng chính

1. **Create Map**: Tạo map mới với kích thước tùy chỉnh
2. **Import Map**: Load map từ database
3. **Tileset**: Chọn tileset để vẽ
4. **Bg Template**: Chọn background items
5. **Npc Template**: Đặt NPC lên map
6. **Mob Template**: Đặt quái vật lên map
7. **Effect Template**: Đặt hiệu ứng
8. **Waypoint**: Đặt điểm dịch chuyển
9. **Save Map**: Lưu map vào database
10. **Expand**: Mở rộng map (Top/Bottom/Left/Right)

## Thư viện sử dụng

- `girlkundb-1.0.0.jar` - Database connection pool (HikariCP)
- `flatlaf-1.6.jar` - Look and Feel hiện đại
- `flatlaf-intellij-themes-1.6.jar` - Theme IntelliJ
- `json_simple-1.1.jar` - JSON parser
- `mysql-connector-j-8.3.0.jar` - MySQL JDBC driver
- `TimingFramework-0.55.jar` - Animation framework

## Lưu ý

- Đảm bảo MySQL đang chạy trước khi mở tool
- Kiểm tra file cấu hình database trước khi chạy
- Thư mục `data/` phải nằm cùng cấp với file JAR khi chạy

---
*Tách từ GirlkunToolCBRO - 2026*
