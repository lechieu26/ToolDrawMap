"""Indexed PNG conversion balancing RGB detail and sprite transparency."""
from dataclasses import dataclass, field
from pathlib import Path
import os
import tempfile
from PIL import Image

EXTENSIONS = {'.png', '.jpg', '.jpeg', '.bmp', '.webp'}


def load_image(path):
    with Image.open(path) as source:
        if getattr(source, 'n_frames', 1) != 1:
            raise ValueError('Animated/multi-frame images are not supported (no frames discarded).')
        source.load()
        return source.copy()


def indexed_image(source):
    rgba = source.convert('RGBA')
    # Preserve small palettes exactly. For larger images, spending one entry
    # on every alpha level can exhaust all 256 entries before RGB quantization.
    # Bound partial alpha to 14 levels (maximum error 10/255), reserving exact
    # 0 and 255 separately. The remaining budget preserves RGB detail.
    normalized = rgba.copy()
    transparent = rgba.getchannel('A').point(lambda a: 255 if a == 0 else 0)
    normalized.paste((0, 0, 0, 0), mask=transparent)
    exact = normalized.getcolors(256) is not None
    groups = {}
    for position, (r, g, b, a) in enumerate(normalized.getdata()):
        if not exact and 0 < a < 255:
            a = 1 + round(round((a - 1) * 13 / 253) * 253 / 13)
        groups.setdefault(a, []).append((position, (r, g, b) if a else (0, 0, 0)))
    counts = {a: min(256, len({rgb for _, rgb in pixels})) for a, pixels in groups.items()}
    budget = {a: 1 for a in groups}
    for _ in range(256 - len(groups)):
        candidates = [a for a in groups if budget[a] < counts[a]]
        if not candidates:
            break
        a = max(candidates, key=lambda a: len(groups[a]) ** 0.5 / budget[a])
        budget[a] += 1
    indices = bytearray(rgba.width * rgba.height)
    palette, alpha = [], []
    for a, pixels in groups.items():
        colors = list(dict.fromkeys(rgb for _, rgb in pixels))
        offset = len(alpha)
        if len(colors) <= budget[a]:
            lookup = {rgb: i for i, rgb in enumerate(colors)}
            mapped = (lookup[rgb] for _, rgb in pixels)
        else:
            strip = Image.new('RGB', (len(pixels), 1))
            strip.putdata([rgb for _, rgb in pixels])
            quantized = strip.quantize(colors=budget[a], method=Image.Quantize.MEDIANCUT,
                                       dither=Image.Dither.NONE)
            raw_palette = quantized.getpalette()
            colors = [tuple(raw_palette[i:i + 3]) for i in range(0, budget[a] * 3, 3)]
            mapped = quantized.getdata()
        for (position, _), index in zip(pixels, mapped):
            indices[position] = offset + index
        palette.extend(channel for rgb in colors for channel in rgb)
        alpha.extend([a] * len(colors))
    result = Image.frombytes('P', rgba.size, bytes(indices))
    result.putpalette(palette + [0] * (768 - len(palette)))
    if min(groups) < 255:
        result.info['transparency'] = bytes(alpha)
    return result


def verify(path, source):
    with open(path, 'rb') as stream:
        header = stream.read(26)
    if header[:8] != b'\x89PNG\r\n\x1a\n' or header[25] != 3:
        raise ValueError('Output is not an indexed PNG (IHDR color type 3).')
    with Image.open(path) as result:
        result.load()
        if result.mode != 'P' or result.size != source.size:
            raise ValueError('Output mode or dimensions do not match.')
        if not result.palette or len(result.getpalette()) // 3 > 256:
            raise ValueError('Invalid palette.')
        original_alpha = source.convert('RGBA').getchannel('A').tobytes()
        converted_alpha = result.convert('RGBA').getchannel('A').tobytes()
        for before, after in zip(original_alpha, converted_alpha):
            if ((before == 0) != (after == 0) or (before == 255) != (after == 255)
                    or abs(before - after) > 10):
                raise ValueError('Transparency changed beyond the allowed tolerance.')


def convert_file(source_path, destination):
    source = load_image(source_path)
    result = indexed_image(source)
    destination = Path(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    handle, temporary = tempfile.mkstemp(suffix='.png', dir=destination.parent)
    os.close(handle)
    try:
        metadata = {key: source.info[key] for key in ('icc_profile', 'dpi') if key in source.info}
        result.save(temporary, format='PNG', optimize=True, **metadata)
        verify(temporary, source)
        # Exclusive creation prevents overwriting existing files, including a
        # destination created by another process while conversion was running.
        with open(temporary, 'rb') as src, open(destination, 'xb') as dst:
            try:
                import shutil
                shutil.copyfileobj(src, dst)
            except Exception:
                dst.close()
                destination.unlink(missing_ok=True)
                raise
        try:
            verify(destination, source)
        except Exception:
            destination.unlink(missing_ok=True)
            raise
        return Path(source_path).stat().st_size, destination.stat().st_size
    finally:
        Path(temporary).unlink(missing_ok=True)


@dataclass
class Summary:
    total: int = 0
    successful: int = 0
    skipped: int = 0
    original: int = 0
    converted: int = 0
    cancelled: bool = False
    errors: list = field(default_factory=list)


def validate_folders(input_folder, output_folder):
    if not str(input_folder).strip() or not str(output_folder).strip():
        raise ValueError('Select input and output folders.')
    src, dst = Path(input_folder).resolve(), Path(output_folder).resolve()
    if not src.is_dir():
        raise ValueError('Input folder does not exist.')
    if src == dst or src in dst.parents or dst in src.parents:
        raise ValueError('Input and output must be separate, non-nested folders.')
    if dst.exists() and not dst.is_dir():
        raise ValueError('Output must be a folder.')
    return src, dst


def validate_selection(selected_files, output_folder):
    if not selected_files or not str(output_folder).strip():
        raise ValueError('Select images and an output folder.')
    dst = Path(output_folder).resolve()
    if dst.exists() and not dst.is_dir():
        raise ValueError('Output must be a folder.')
    return list(dict.fromkeys(Path(path).resolve() for path in selected_files)), dst


def run_batch(input_folder, output_folder, emit, cancel, selected_files=None):
    if selected_files is None:
        src, dst = validate_folders(input_folder, output_folder)
        files = []
    else:
        files, dst = validate_selection(selected_files, output_folder)
        src = None
    def scan_error(error):
        raise error
    for root, dirs, names in (os.walk(src, onerror=scan_error, followlinks=False) if src else []):
        dirs[:] = sorted(d for d in dirs if not (Path(root) / d).is_symlink())
        for name in sorted(names):
            path = Path(root) / name
            if path.suffix.lower() in EXTENSIONS and not path.is_symlink():
                files.append(path)
    summary = Summary(total=len(files))
    targets = {}
    for path in files:
        key = str((path.relative_to(src) if src else Path(path.name)).with_suffix('.png')).casefold()
        targets[key] = targets.get(key, 0) + 1
    emit('total', summary.total)
    for i, path in enumerate(files, 1):
        if cancel.is_set():
            summary.cancelled = True
            break
        relative = path.relative_to(src) if src else Path(path.name)
        target = dst / relative.with_suffix('.png')
        emit('processing', str(relative))
        try:
            if path.suffix.lower() not in EXTENSIONS:
                raise ValueError('Unsupported image format.')
            # Inspect the actual image mode, not the extension. Decode indexed
            # inputs before skipping so damaged files are still reported.
            with Image.open(path) as source:
                if source.mode == 'P':
                    source.load()
                    summary.skipped += 1
                    emit('log', f'SKIPPED: {relative}: already Indexed (P)')
                    continue
            if targets[str(relative.with_suffix('.png')).casefold()] > 1:
                raise ValueError('Output name collision (multiple inputs have the same PNG name).')
            if dst != target.resolve() and dst not in target.resolve().parents:
                raise ValueError('Output path escapes output folder.')
            original, converted = convert_file(path, target)
            summary.successful += 1
            summary.original += original
            summary.converted += converted
            emit('log', f'OK: {relative}')
        except Exception as error:
            summary.errors.append((str(relative), str(error)))
            emit('log', f'FAILED: {relative}: {error}')
        finally:
            emit('progress', i)
    return summary
