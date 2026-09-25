import os
import queue
import subprocess
import sys
import threading
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
from tkinter.scrolledtext import ScrolledText
from pathlib import Path
from converter import run_batch, validate_folders, validate_selection


def size_text(value):
    return f'{value / 1024:,.1f} KiB' if abs(value) < 1048576 else f'{value / 1048576:,.2f} MiB'


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title('Sprite Indexed PNG Converter — 256 colors')
        self.geometry('1020x850')
        self.minsize(780, 650)
        style = ttk.Style(self)
        style.theme_use('clam')
        style.configure('TButton', font=('Segoe UI', 10, 'bold'), padding=(14, 10))
        for name, color, hover in [('Blue', '#2563eb', '#1d4ed8'), ('Green', '#15803d', '#166534'),
                                    ('Purple', '#7c3aed', '#6d28d9'), ('Red', '#dc2626', '#b91c1c')]:
            style.configure(f'{name}.TButton', background=color, foreground='white')
            style.map(f'{name}.TButton', background=[('disabled', '#d1d5db'), ('active', hover)],
                      foreground=[('disabled', '#6b7280')])
        style.configure('Treeview', rowheight=30, font=('Segoe UI', 10))
        style.configure('Treeview.Heading', font=('Segoe UI', 10, 'bold'))
        style.configure('Remove.TButton', font=('Segoe UI', 9, 'bold'), padding=(4, 1),
                        background='#dc2626', foreground='white')
        style.map('Remove.TButton', background=[('disabled', '#d1d5db'), ('active', '#b91c1c')],
                  foreground=[('disabled', '#6b7280')])
        style.map('Treeview', background=[('selected', '#2563eb')], foreground=[('selected', 'white')])
        self.events = queue.Queue(maxsize=512)
        self.cancel = threading.Event()
        self.busy = False
        self.closing = False
        app_dir = Path(sys.executable).resolve().parent if getattr(sys, 'frozen', False) else Path(__file__).resolve().parent
        self.input_path = tk.StringVar()
        self.output_path = tk.StringVar(value=str(app_dir / 'output'))
        self.selected_files = None
        self.status = tk.StringVar(value='Ready — 256 colors maximum, preserved transparent background, no dithering')
        self.statistics = tk.StringVar(value='No conversion yet.')
        frame = ttk.Frame(self, padding=12)
        frame.pack(fill='both', expand=True)
        frame.columnconfigure(1, weight=1)
        self.controls = []
        for row, (label, variable) in enumerate([('Input', self.input_path), ('Output Folder', self.output_path)]):
            ttk.Label(frame, text=label).grid(row=row, column=0, sticky='w', pady=5)
            entry = ttk.Entry(frame, textvariable=variable)
            entry.grid(row=row, column=1, sticky='ew', padx=10)
            if row == 0:
                self.input_entry = entry
                entry.configure(state='readonly')
            button = ttk.Button(frame, text='Browse Folder' if row == 0 else 'Browse Output', style='Blue.TButton', command=lambda v=variable: self.browse(v))
            button.grid(row=row, column=2)
            self.controls.extend([entry, button])
        actions = ttk.Frame(frame)
        actions.grid(row=2, column=0, columnspan=3, sticky='w', pady=10)
        for title, callback in [('Select Images', self.browse_images), ('Convert', self.start), ('Open Output Folder', self.open_output)]:
            button = ttk.Button(actions, text=title, command=callback,
                                style=('Green' if title == 'Convert' else 'Blue') + '.TButton')
            button.pack(side='left', padx=(0, 8))
            self.controls.append(button)
        self.stop = ttk.Button(actions, text='Cancel', style='Red.TButton', command=self.cancel.set, state='disabled')
        self.stop.pack(side='left')
        ttk.Label(frame, textvariable=self.status).grid(row=3, column=0, columnspan=3, sticky='w')
        self.progress = ttk.Progressbar(frame)
        self.progress.grid(row=4, column=0, columnspan=3, sticky='ew', pady=8)
        self.file_frame = ttk.LabelFrame(frame, text='Selected images — Ctrl/Shift to select • Delete / Backspace to remove', padding=6)
        self.file_frame.grid(row=5, column=0, columnspan=3, sticky='ew', pady=(0, 8))
        list_actions = ttk.Frame(self.file_frame)
        list_actions.pack(side='top', fill='x', pady=(0, 6))
        clear_button = ttk.Button(list_actions, text='Xóa hết', style='Red.TButton', command=self.clear_files)
        clear_button.pack(side='right')
        self.controls.append(clear_button)
        self.file_list = ttk.Treeview(self.file_frame, columns=('name', 'path', 'remove'), show='headings',
                                      selectmode='extended', height=4)
        for column, title, width in [('name', 'Image', 210), ('path', 'Folder', 440), ('remove', 'Remove', 90)]:
            self.file_list.heading(column, text=title)
            self.file_list.column(column, width=width, minwidth=60, stretch=column != 'remove',
                                  anchor='center' if column == 'remove' else 'w')
        scrollbar = ttk.Scrollbar(self.file_frame, orient='vertical', command=self.file_list.yview)
        self.remove_buttons = []
        self.remove_layout_pending = False
        def on_scroll(first, last):
            scrollbar.set(first, last)
            self.schedule_remove_buttons()
        self.file_list.configure(yscrollcommand=on_scroll)
        self.file_list.pack(side='left', fill='both', expand=True)
        scrollbar.pack(side='right', fill='y')
        self.file_list.bind('<Configure>', self.schedule_remove_buttons)
        self.file_list.bind('<ButtonRelease-1>', self.schedule_remove_buttons)
        self.file_list.bind('<Delete>', self.remove_selected)
        self.file_list.bind('<BackSpace>', self.remove_selected)
        self.file_frame.grid_remove()
        ttk.Label(frame, textvariable=self.statistics, wraplength=940).grid(row=6, column=0, columnspan=3, sticky='w', pady=10)
        self.log = ScrolledText(frame, height=12, state='disabled', wrap='word')
        self.log.grid(row=7, column=0, columnspan=3, sticky='nsew')
        frame.rowconfigure(7, weight=1)
        self.protocol('WM_DELETE_WINDOW', self.close)
        self.after(75, self.poll)

    def browse(self, variable):
        path = filedialog.askdirectory()
        if path:
            variable.set(path)
            if variable is self.input_path:
                self.selected_files = None
                self.refresh_file_list()

    def browse_images(self):
        paths = filedialog.askopenfilenames(title='Select input images', filetypes=[
            ('Images', '*.png *.jpg *.jpeg *.bmp *.webp'), ('All files', '*.*')])
        if paths:
            self.selected_files = tuple(dict.fromkeys(paths))
            self.refresh_file_list()

    def refresh_file_list(self):
        for button in self.remove_buttons:
            button.place_forget()
        rows = self.file_list.get_children()
        if rows:
            self.file_list.delete(*rows)
        if self.selected_files is None:
            self.file_frame.grid_remove()
            return
        self.file_frame.grid()
        self.input_path.set(f'{len(self.selected_files)} images selected')
        for index, path in enumerate(self.selected_files):
            self.file_list.insert('', 'end', iid=str(index), values=(Path(path).name, str(Path(path).parent), ''))
        self.schedule_remove_buttons()

    def remove_files(self, rows):
        if self.busy or self.selected_files is None or not rows:
            return
        removed = {int(row) for row in rows}
        next_index = min(removed)
        self.selected_files = tuple(path for index, path in enumerate(self.selected_files) if index not in removed)
        self.refresh_file_list()
        remaining = self.file_list.get_children()
        if remaining:
            row = remaining[min(next_index, len(remaining) - 1)]
            self.file_list.selection_set(row)
            self.file_list.focus(row)
            self.file_list.see(row)
        self.file_list.focus_set()

    def remove_selected(self, event=None):
        self.remove_files(self.file_list.selection())
        return 'break'

    def clear_files(self):
        if self.busy or self.selected_files is None:
            return
        self.selected_files = ()
        self.refresh_file_list()

    def schedule_remove_buttons(self, event=None):
        if not self.remove_layout_pending:
            self.remove_layout_pending = True
            self.after_idle(self.layout_remove_buttons)

    def layout_remove_buttons(self):
        self.remove_layout_pending = False
        # Treeview has no embedded widgets: place real buttons over visible
        # cells and reuse them when scrolling, even with thousands of images.
        visible = dict.fromkeys(self.file_list.identify_row(y)
                                for y in range(self.file_list.winfo_height()))
        cells = [(row, self.file_list.bbox(row, 'remove')) for row in visible if row]
        cells = [(row, box) for row, box in cells if box]
        while len(self.remove_buttons) < len(cells):
            self.remove_buttons.append(ttk.Button(self.file_list, text='Remove', style='Remove.TButton'))
        for button in self.remove_buttons:
            button.place_forget()
        for button, (row, (x, y, width, height)) in zip(self.remove_buttons, cells):
            button.configure(command=lambda item=row: self.remove_files((item,)),
                             state='disabled' if self.busy else 'normal')
            button.place(x=x + 3, y=y + 2, width=max(1, width - 6), height=max(1, height - 4))

    def set_busy(self, busy, cancellable=False):
        self.busy = busy
        self.schedule_remove_buttons()
        for control in self.controls:
            control.configure(state='disabled' if busy else 'normal')
        if not busy:
            self.input_entry.configure(state='readonly')
        self.stop.configure(state='normal' if busy and cancellable else 'disabled')

    def emit(self, event, data):
        self.events.put((event, data))

    def start(self):
        try:
            selection = self.selected_files
            if selection is None:
                src, dst = validate_folders(self.input_path.get(), self.output_path.get())
            else:
                selection, dst = validate_selection(selection, self.output_path.get())
                src = None
        except Exception as error:
            messagebox.showerror('Invalid folders', str(error))
            return
        self.cancel.clear()
        self.set_busy(True, True)
        self.progress['value'] = 0
        self.status.set('Scanning input folders…')
        self.statistics.set('Converting…')
        self.log.configure(state='normal')
        self.log.delete('1.0', 'end')
        self.log.configure(state='disabled')
        def worker():
            try:
                self.emit('done', run_batch(src, dst, self.emit, self.cancel, selected_files=selection))
            except Exception as error:
                self.emit('error', str(error))
        threading.Thread(target=worker, daemon=True).start()

    def append_log(self, message):
        self.log.configure(state='normal')
        self.log.insert('end', message + '\n')
        if int(self.log.index('end-1c').split('.')[0]) > 2500:
            self.log.delete('1.0', '501.0')
        self.log.see('end')
        self.log.configure(state='disabled')

    def poll(self):
        for _ in range(100):
            try:
                event, data = self.events.get_nowait()
            except queue.Empty:
                break
            if event == 'total':
                self.total = data
                self.progress['maximum'] = max(1, data)
            elif event == 'processing':
                self.current = data
            elif event == 'progress':
                self.progress['value'] = data
                self.status.set(f'Processing: {self.current} | {data} / {self.total} | {data / max(1, self.total):.0%}')
            elif event == 'log':
                self.append_log(data)
            elif event == 'error':
                self.set_busy(False)
                self.status.set('Failed: ' + data)
                messagebox.showerror('Error', data)
            elif event == 'done':
                self.set_busy(False)
                saved = data.original - data.converted
                reduction = saved / data.original * 100 if data.original else 0
                self.statistics.set(f'Total: {data.total} | Successful: {data.successful} | Skipped: {data.skipped} | Failed: {len(data.errors)} | '
                                    f'Unprocessed: {data.total - data.successful - data.skipped - len(data.errors)}\n'
                                    f'Successful files: Original {size_text(data.original)} → Converted {size_text(data.converted)} | '
                                    f'Saved: {size_text(saved)} | Reduction: {reduction:.1f}%')
                self.status.set('Cancelled.' if data.cancelled else 'Complete.')
                if data.errors:
                    window = tk.Toplevel(self)
                    window.title('Failed files — select and copy to save')
                    report = ScrolledText(window, width=100, height=25)
                    report.pack(fill='both', expand=True)
                    report.insert('end', '\n'.join(f'{path}: {error}' for path, error in data.errors))
                    report.configure(state='disabled')
        if self.closing and not self.busy:
            self.destroy()
            return
        self.after(75, self.poll)

    def open_output(self):
        path = Path(self.output_path.get())
        if not self.output_path.get() or not path.is_dir():
            messagebox.showerror('Output', 'Output folder does not exist yet.')
            return
        try:
            if sys.platform == 'win32':
                os.startfile(str(path.resolve()))
            else:
                subprocess.Popen(['open' if sys.platform == 'darwin' else 'xdg-open', str(path.resolve())])
        except OSError as error:
            messagebox.showerror('Output', str(error))

    def close(self):
        if self.busy:
            self.closing = True
            self.cancel.set()
            self.status.set('Finishing current image before closing…')
        else:
            self.destroy()
