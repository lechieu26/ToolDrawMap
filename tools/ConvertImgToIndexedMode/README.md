# Sprite Indexed PNG Converter

Công cụ desktop Python/Tkinter chuyển hàng loạt PNG, JPG/JPEG, BMP, WEBP thành PNG Indexed, palette tối đa 256 màu. Chạy độc lập với project Java.

## Cài đặt và chạy (Windows PowerShell)

Yêu cầu Python 3.10 trở lên có Tkinter (bản Python Windows thông thường đã có).

```powershell
cd D:\NRO\SourceCode\ToolDrawMap\tools\ConvertImgToIndexedMode
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe main.py
```

Chọn **Browse Folder** để quét thư mục và thư mục con, hoặc **Select Images** để chọn nhiều ảnh cùng lúc (Ctrl/Shift), rồi nhấn Convert. Chọn lại sẽ thay thế lựa chọn input trước đó.

Output mặc định là thư mục `output` cạnh `main.py` (khi build EXE: cạnh file EXE), không phụ thuộc thư mục đang chạy lệnh. Thư mục được tạo khi xuất ảnh đầu tiên; có thể đổi bằng Browse Output.

Ở chế độ folder, input/output phải riêng biệt, không lồng nhau; cấu trúc thư mục được giữ nguyên. Ở chế độ chọn ảnh, các ảnh được xuất trực tiếp vào output. Phần tên được giữ nguyên và phần mở rộng đổi thành `.png`; file trùng tên đích hoặc đã tồn tại được báo lỗi, không ghi đè.


Cancel dừng sau ảnh hiện tại. Đóng cửa sổ khi đang chạy cũng chờ ảnh hiện tại hoàn tất. Open Output Folder mở thư mục kết quả.

Khi convert theo folder hoặc chọn nhiều ảnh, tool tự kiểm tra mode bằng Pillow. Ảnh đã là Indexed (`P`, kể cả palette ít hơn 256 màu) được bỏ qua, không copy sang output và không sửa file gốc. Log ghi `SKIPPED`, thống kê có mục `Skipped` riêng; các ảnh này không tính vào dung lượng chuyển đổi thành công.

## Chất lượng và bảo vệ dữ liệu

- Pixel hoàn toàn trong suốt (alpha 0) và hoàn toàn đục (alpha 255) được giữ chính xác. Không ghép nền trắng/đen. RGB của pixel hoàn toàn trong suốt được chuẩn hóa về 0, không ảnh hưởng hiển thị.
- Nếu ảnh có tối đa 256 tổ hợp RGBA (sau chuẩn hóa pixel trong suốt), màu và alpha được giữ nguyên. Với ảnh vượt giới hạn, alpha bán trong suốt được gom thành tối đa 14 mức, sai lệch tối đa 10/255, để tránh dùng hết palette chỉ cho alpha và làm mất màu/nét vẽ.
- Phần palette còn lại được phân bổ cho màu RGB bằng Pillow median-cut trong từng nhóm alpha. Không dithering để tránh nhiễu sprite. RGB có thể xấp xỉ; hãy kiểm tra ảnh đầu ra với sprite quan trọng. PNG Indexed không thể giữ mọi màu của ảnh RGBA tùy ý.
- File đầu ra luôn được mở lại và kiểm tra PNG IHDR color type 3, mode P, palette tối đa 256, kích thước, vùng trong suốt/đục và giới hạn sai lệch alpha từng pixel. ICC profile và DPI được giữ nếu có.
- Không ghi đè file có sẵn, không đổi tên để né trùng. Ví dụ `a.jpg` và `a.png` cùng thư mục đều được báo lỗi vì cùng tạo `a.png`. File lỗi không chặn các file khác. Ảnh động/multi-frame được báo lỗi để tránh mất frame. Bỏ qua symbolic link khi quét input.
- Xử lý từng ảnh trong worker thread, không tải cả bộ ảnh vào RAM. Ảnh riêng lẻ rất lớn vẫn cần nhiều RAM cho dữ liệu pixel; giới hạn bảo vệ ảnh lớn mặc định của Pillow vẫn được bật.
- Tổng dung lượng trước/sau chỉ tính các file chuyển thành công để so sánh công bằng. Reduction âm nghĩa là file Indexed lớn hơn bản gốc. Log giao diện giữ khoảng 2.500 dòng; danh sách lỗi đầy đủ được hiển thị riêng cuối batch và có thể sao chép.

## Build EXE

```powershell
.\.venv\Scripts\python.exe -m pip install pyinstaller
.\.venv\Scripts\python.exe -m PyInstaller --noconfirm --clean --onefile --windowed --name ConvertImgToIndexedMode main.py
```

File chạy: `dist\ConvertImgToIndexedMode.exe`. Build trên Windows để tạo EXE Windows. Máy sử dụng EXE không cần cài Python.

## Kiểm thử

```powershell
python -m unittest discover -s tests -v
```

`converter.py`: xử lý ảnh, kiểm tra và batch. `gui.py`: giao diện, worker. `main.py`: điểm khởi chạy.
