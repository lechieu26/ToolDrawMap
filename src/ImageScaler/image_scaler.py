"""
Image Scaler Tool
- Cho phép chọn nhiều ảnh PNG
- Scale ảnh theo các tỷ lệ x1, x2, x3, x4
- x4: Ảnh gốc (100%)
- x3: 75% kích thước gốc
- x2: 50% kích thước gốc
- x1: 25% kích thước gốc
"""

import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk
from PIL import Image, ImageTk
import threading


def get_app_dir():
    """
    Lấy thư mục chứa ứng dụng.
    - Nếu chạy từ file .exe (PyInstaller): trả về thư mục chứa file .exe
    - Nếu chạy từ script Python: trả về thư mục chứa file .py
    """
    if getattr(sys, 'frozen', False):
        # Chạy từ file exe được đóng gói bởi PyInstaller
        return os.path.dirname(os.path.abspath(sys.executable))
    else:
        # Chạy từ script Python
        return os.path.dirname(os.path.abspath(__file__))


class ImageScalerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Image Scaler Tool")
        self.root.geometry("900x650")
        self.root.configure(bg="#1a1a2e")
        
        # Danh sách ảnh đã chọn
        self.selected_images = []
        
        # Output directory
        self.output_dir = os.path.join(get_app_dir(), "output")
        
        self.setup_ui()
        
    def setup_ui(self):
        # Style
        style = ttk.Style()
        style.theme_use('clam')
        
        # Configure styles
        style.configure("Title.TLabel", 
                       background="#1a1a2e", 
                       foreground="#efffff",
                       font=("Segoe UI", 18, "bold"))
        
        style.configure("Info.TLabel",
                       background="#1a1a2e",
                       foreground="#a0a0a0",
                       font=("Segoe UI", 10))
        
        style.configure("Custom.TFrame", background="#1a1a2e")
        
        style.configure("Action.TButton",
                       font=("Segoe UI", 11, "bold"),
                       padding=(20, 10))
        
        style.map("Action.TButton",
                 background=[("active", "#4a69bd"), ("!active", "#3c5a99")])
        
        # Main container
        main_frame = ttk.Frame(self.root, style="Custom.TFrame")
        main_frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)
        
        # Title
        title_label = ttk.Label(main_frame, text="🖼️ Image Scaler Tool", style="Title.TLabel")
        title_label.pack(pady=(0, 5))
        
        # Subtitle
        subtitle = ttk.Label(main_frame, 
                            text="Scale ảnh theo tỷ lệ x1 (25%), x2 (50%), x3 (75%), x4 (100%)",
                            style="Info.TLabel")
        subtitle.pack(pady=(0, 20))
        
        # Button frame
        btn_frame = ttk.Frame(main_frame, style="Custom.TFrame")
        btn_frame.pack(fill=tk.X, pady=(0, 15))
        
        # Select Images button
        self.btn_select = tk.Button(btn_frame, 
                                    text="📂 Chọn Ảnh",
                                    font=("Segoe UI", 11, "bold"),
                                    bg="#4a69bd",
                                    fg="white",
                                    activebackground="#3c5a99",
                                    activeforeground="white",
                                    relief=tk.FLAT,
                                    cursor="hand2",
                                    padx=25,
                                    pady=10,
                                    command=self.select_images)
        self.btn_select.pack(side=tk.LEFT, padx=(0, 10))
        
        # Clear button
        self.btn_clear = tk.Button(btn_frame,
                                   text="🗑️ Xóa Tất Cả",
                                   font=("Segoe UI", 11, "bold"),
                                   bg="#e74c3c",
                                   fg="white",
                                   activebackground="#c0392b",
                                   activeforeground="white",
                                   relief=tk.FLAT,
                                   cursor="hand2",
                                   padx=25,
                                   pady=10,
                                   command=self.clear_images)
        self.btn_clear.pack(side=tk.LEFT, padx=(0, 10))
        
        # Rename button (xóa đuôi file)
        self.btn_rename = tk.Button(btn_frame,
                                    text="✂️ Rename",
                                    font=("Segoe UI", 11, "bold"),
                                    bg="#9b59b6",
                                    fg="white",
                                    activebackground="#8e44ad",
                                    activeforeground="white",
                                    relief=tk.FLAT,
                                    cursor="hand2",
                                    padx=25,
                                    pady=10,
                                    command=self.rename_files)
        self.btn_rename.pack(side=tk.LEFT, padx=(0, 10))
        
        # Open Output button
        self.btn_open = tk.Button(btn_frame,
                                    text="📂 Open Output",
                                    font=("Segoe UI", 11, "bold"),
                                    bg="#f39c12",
                                    fg="white",
                                    activebackground="#d35400",
                                    activeforeground="white",
                                    relief=tk.FLAT,
                                    cursor="hand2",
                                    padx=25,
                                    pady=10,
                                    command=self.open_output_folder)
        self.btn_open.pack(side=tk.LEFT, padx=(0, 10))
        
        # Generate button
        self.btn_generate = tk.Button(btn_frame,
                                      text="⚡ Generate",
                                      font=("Segoe UI", 12, "bold"),
                                      bg="#27ae60",
                                      fg="white",
                                      activebackground="#1e8449",
                                      activeforeground="white",
                                      relief=tk.FLAT,
                                      cursor="hand2",
                                      padx=30,
                                      pady=10,
                                      command=self.generate_images)
        self.btn_generate.pack(side=tk.RIGHT)
        
        # Image count label
        self.count_label = ttk.Label(main_frame, 
                                     text="Đã chọn: 0 ảnh",
                                     style="Info.TLabel")
        self.count_label.pack(anchor=tk.W, pady=(0, 10))
        
        # Listbox frame with scrollbar
        list_frame = ttk.Frame(main_frame, style="Custom.TFrame")
        list_frame.pack(fill=tk.BOTH, expand=True)
        
        # Scrollbar
        scrollbar = ttk.Scrollbar(list_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # Listbox for selected images
        self.image_listbox = tk.Listbox(list_frame,
                                        font=("Consolas", 10),
                                        bg="#16213e",
                                        fg="#efffff",
                                        selectbackground="#4a69bd",
                                        selectforeground="white",
                                        relief=tk.FLAT,
                                        highlightthickness=1,
                                        highlightcolor="#4a69bd",
                                        highlightbackground="#2a2a4a",
                                        yscrollcommand=scrollbar.set)
        self.image_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.image_listbox.yview)
        
        # Bind delete key
        self.image_listbox.bind("<Delete>", self.delete_selected)
        
        # Progress frame
        progress_frame = ttk.Frame(main_frame, style="Custom.TFrame")
        progress_frame.pack(fill=tk.X, pady=(15, 0))
        
        # Progress bar
        self.progress_var = tk.DoubleVar()
        self.progress_bar = ttk.Progressbar(progress_frame, 
                                            variable=self.progress_var,
                                            maximum=100,
                                            mode='determinate',
                                            length=400)
        self.progress_bar.pack(fill=tk.X)
        
        # Status label
        self.status_label = ttk.Label(main_frame,
                                      text="Sẵn sàng",
                                      style="Info.TLabel")
        self.status_label.pack(anchor=tk.W, pady=(10, 0))
        
        # Output path label
        output_label = ttk.Label(main_frame,
                                text=f"📁 Output: {self.output_dir}",
                                style="Info.TLabel")
        output_label.pack(anchor=tk.W, pady=(5, 0))
        
    def select_images(self):
        """Mở dialog chọn nhiều ảnh"""
        filetypes = [
            ("Image files", "*.png *.jpg *.jpeg *.bmp *.gif *.webp"),
            ("PNG files", "*.png"),
            ("JPEG files", "*.jpg *.jpeg"),
            ("All files", "*.*")
        ]
        
        files = filedialog.askopenfilenames(
            title="Chọn ảnh",
            filetypes=filetypes
        )
        
        if files:
            for f in files:
                if f not in self.selected_images:
                    self.selected_images.append(f)
                    self.image_listbox.insert(tk.END, os.path.basename(f))
            
            self.update_count()
            
    def clear_images(self):
        """Xóa tất cả ảnh đã chọn"""
        self.selected_images.clear()
        self.image_listbox.delete(0, tk.END)
        self.update_count()
        
    def delete_selected(self, event=None):
        """Xóa ảnh được chọn trong listbox"""
        selection = self.image_listbox.curselection()
        if selection:
            for index in reversed(selection):
                self.selected_images.pop(index)
                self.image_listbox.delete(index)
            self.update_count()
            
    def update_count(self):
        """Cập nhật số lượng ảnh đã chọn"""
        count = len(self.selected_images)
        self.count_label.config(text=f"Đã chọn: {count} ảnh")
        
    def generate_images(self):
        """Tạo các ảnh với các tỷ lệ khác nhau"""
        if not self.selected_images:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn ít nhất một ảnh!")
            return
        
        # Disable buttons during processing
        self._disable_buttons()
        
        # Start processing in a separate thread
        thread = threading.Thread(target=self._process_images)
        thread.start()
    
    def rename_files(self):
        """Copy file và xóa đuôi extension"""
        if not self.selected_images:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn ít nhất một ảnh!")
            return
        
        # Disable buttons during processing
        self._disable_buttons()
        
        # Start processing in a separate thread
        thread = threading.Thread(target=self._process_rename)
        thread.start()
    
    def _process_rename(self):
        """Xử lý rename trong thread riêng"""
        try:
            # Tạo thư mục output/file_rename
            rename_dir = os.path.join(self.output_dir, "file_rename")
            os.makedirs(rename_dir, exist_ok=True)
            
            total = len(self.selected_images)
            
            for idx, img_path in enumerate(self.selected_images):
                filename = os.path.basename(img_path)
                name, ext = os.path.splitext(filename)
                
                self.update_status(f"Đang rename: {filename} -> {name}")
                
                try:
                    # Copy file với tên mới (không có extension)
                    import shutil
                    output_path = os.path.join(rename_dir, name)
                    shutil.copy2(img_path, output_path)
                    
                except Exception as e:
                    print(f"Lỗi rename {img_path}: {e}")
                
                # Cập nhật progress
                progress = ((idx + 1) / total) * 100
                self.update_progress(progress)
            
            self.update_status(f"✅ Hoàn thành! Đã rename {total} file")
            messagebox.showinfo("Thành công", 
                              f"Đã rename xong {total} file!\n\nOutput: {rename_dir}")
            
        except Exception as e:
            messagebox.showerror("Lỗi", f"Lỗi rename: {str(e)}")
            self.update_status(f"❌ Lỗi: {str(e)}")
            
        finally:
            # Re-enable buttons
            self.root.after(0, self._enable_buttons)
    
    def _disable_buttons(self):
        """Disable tất cả buttons"""
        self.btn_select.config(state=tk.DISABLED)
        self.btn_clear.config(state=tk.DISABLED)
        self.btn_generate.config(state=tk.DISABLED)
        self.btn_rename.config(state=tk.DISABLED)
        self.btn_open.config(state=tk.DISABLED)
        
    def _process_images(self):
        """Xử lý ảnh trong thread riêng"""
        try:
            # Tạo thư mục output
            scale_dirs = {
                "x1": 0.25,  # 25% - x4/4
                "x2": 0.50,  # 50% - 2*x1
                "x3": 0.75,  # 75% - 3*x1
                "x4": 1.00   # 100% - Gốc
            }
            
            for scale_name in scale_dirs:
                dir_path = os.path.join(self.output_dir, scale_name)
                os.makedirs(dir_path, exist_ok=True)
            
            total = len(self.selected_images)
            
            for idx, img_path in enumerate(self.selected_images):
                self.update_status(f"Đang xử lý: {os.path.basename(img_path)}")
                
                try:
                    # Mở ảnh gốc
                    with Image.open(img_path) as img:
                        # Giữ nguyên mode RGBA nếu là PNG
                        if img.mode in ('RGBA', 'LA') or (img.mode == 'P' and 'transparency' in img.info):
                            img = img.convert('RGBA')
                        else:
                            img = img.convert('RGB')
                        
                        orig_width, orig_height = img.size
                        filename = os.path.basename(img_path)
                        name, ext = os.path.splitext(filename)
                        
                        # Đảm bảo output là PNG
                        output_filename = f"{name}.png"
                        
                        for scale_name, scale_factor in scale_dirs.items():
                            # Tính kích thước mới
                            new_width = max(1, int(orig_width * scale_factor))
                            new_height = max(1, int(orig_height * scale_factor))
                            
                            if scale_factor < 1.0:
                                # Scale xuống
                                scaled_img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
                            else:
                                # Giữ nguyên kích thước gốc cho x4
                                scaled_img = img.copy()
                            
                            # Lưu ảnh
                            output_path = os.path.join(self.output_dir, scale_name, output_filename)
                            scaled_img.save(output_path, "PNG")
                            
                except Exception as e:
                    print(f"Lỗi xử lý {img_path}: {e}")
                    
                # Cập nhật progress
                progress = ((idx + 1) / total) * 100
                self.update_progress(progress)
            
            self.update_status(f"✅ Hoàn thành! Đã xử lý {total} ảnh")
            messagebox.showinfo("Thành công", 
                              f"Đã xử lý xong {total} ảnh!\n\nOutput: {self.output_dir}")
            
        except Exception as e:
            messagebox.showerror("Lỗi", f"Lỗi xử lý: {str(e)}")
            self.update_status(f"❌ Lỗi: {str(e)}")
            
        finally:
            # Re-enable buttons
            self.root.after(0, self._enable_buttons)
            
    def _enable_buttons(self):
        """Re-enable buttons sau khi xử lý xong"""
        self.btn_select.config(state=tk.NORMAL)
        self.btn_clear.config(state=tk.NORMAL)
        self.btn_generate.config(state=tk.NORMAL)
        self.btn_rename.config(state=tk.NORMAL)
        self.btn_open.config(state=tk.NORMAL)
        
    def update_progress(self, value):
        """Cập nhật progress bar từ thread"""
        self.root.after(0, lambda: self.progress_var.set(value))
        
    def update_status(self, text):
        """Cập nhật status label từ thread"""
        self.root.after(0, lambda: self.status_label.config(text=text))
        
    def open_output_folder(self):
        """Mở thư mục output"""
        try:
            if not os.path.exists(self.output_dir):
                os.makedirs(self.output_dir, exist_ok=True)
            
            os.startfile(self.output_dir)
        except Exception as e:
            messagebox.showerror("Lỗi", f"Không thể mở thư mục: {str(e)}")


def main():
    root = tk.Tk()
    
    # Set icon nếu có
    try:
        root.iconbitmap("icon.ico")
    except:
        pass
    
    app = ImageScalerApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
