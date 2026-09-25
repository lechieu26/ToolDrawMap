import sys
import tempfile
import threading
import unittest
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from converter import convert_file, indexed_image, run_batch, validate_folders, verify


class ConverterTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.src = self.root / 'input'
        self.dst = self.root / 'output'
        self.src.mkdir()

    def test_exact_sprite_and_alpha(self):
        image = Image.new('RGBA', (16, 16))
        image.putdata([(i, 255 - i, 73, i) for i in range(256)])
        source, target = self.src / 'sprite.png', self.dst / 'sprite.png'
        image.save(source)
        convert_file(source, target)
        verify(target, image)
        with Image.open(target) as result:
            expected = list(image.getdata())
            actual = list(result.convert('RGBA').getdata())
            self.assertEqual(expected[1:], actual[1:])
            self.assertEqual(result.mode, 'P')

    def test_quantization_bounds_alpha_error(self):
        image = Image.new('RGBA', (64, 64))
        image.putdata([(i % 256, i // 16 % 256, i * 7 % 256, i // 16) for i in range(4096)])
        image.save(self.src / 'many.png')
        convert_file(self.src / 'many.png', self.dst / 'many.png')
        verify(self.dst / 'many.png', image)
        with Image.open(self.dst / 'many.png') as output:
            for before, after in zip(image.getchannel('A').getdata(), output.convert('RGBA').getchannel('A').getdata()):
                self.assertLessEqual(abs(before - after), 10)
                self.assertEqual(before == 0, after == 0)
                self.assertEqual(before == 255, after == 255)

    def test_many_alpha_levels_do_not_merge_red_and_blue(self):
        image = Image.new('RGBA', (256, 2))
        image.putdata([(255, 0, 0, a) for a in range(256)] + [(0, 0, 255, a) for a in range(256)])
        output = indexed_image(image).convert('RGBA')
        for a in range(1, 256):
            self.assertEqual(output.getpixel((a, 0))[:3], (255, 0, 0))
            self.assertEqual(output.getpixel((a, 1))[:3], (0, 0, 255))

    def test_opaque_detail_survives_sparse_alpha_ramp(self):
        image = Image.new('RGBA', (256, 100))
        pixels = [(x, x, x, 255) for _ in range(99) for x in range(256)]
        pixels += [(255, 0, 0, a) for a in range(256)]
        image.putdata(pixels)
        output = indexed_image(image).convert('RGBA')
        self.assertGreater(len({output.getpixel((x, 0))[:3] for x in range(256)}), 200)
        error = sum(abs(output.getpixel((x, 0))[0] - x) for x in range(256)) / 256
        self.assertLess(error, 2)

    def test_formats_errors_collisions_and_structure(self):
        nested = self.src / 'player'
        nested.mkdir()
        for extension in ['jpg', 'bmp', 'webp', 'png']:
            Image.new('RGB', (9, 7), 'red').save(nested / f'image_{extension}.{extension}')
        (nested / 'broken.png').write_bytes(b'not an image')
        for extension in ['jpg', 'png']:
            Image.new('RGB', (2, 2)).save(nested / f'collision.{extension}')
        result = run_batch(self.src, self.dst, lambda *args: None, threading.Event())
        self.assertEqual((result.total, result.successful, len(result.errors)), (7, 4, 3))
        self.assertEqual(len(list((self.dst / 'player').glob('*.png'))), 4)
        again = run_batch(self.src, self.dst, lambda *args: None, threading.Event())
        self.assertEqual(again.successful, 0)

    def test_existing_output_unchanged(self):
        Image.new('RGB', (2, 2)).save(self.src / 'a.png')
        self.dst.mkdir()
        target = self.dst / 'a.png'
        target.write_bytes(b'existing')
        with self.assertRaises(FileExistsError):
            convert_file(self.src / 'a.png', target)
        self.assertEqual(target.read_bytes(), b'existing')
        self.assertEqual(len(list(self.dst.iterdir())), 1)

    def test_selected_images_only_and_flat_output(self):
        nested = self.src / 'nested'
        nested.mkdir()
        first, second = self.src / 'first.jpg', nested / 'second.bmp'
        for path in [first, second, self.src / 'unselected.png']:
            Image.new('RGB', (3, 4), 'red').save(path)
        result = run_batch(None, self.dst, lambda *args: None, threading.Event(),
                           selected_files=[first, second, first])
        self.assertEqual((result.total, result.successful), (2, 2))
        self.assertEqual({p.name for p in self.dst.iterdir()}, {'first.png', 'second.png'})

    def test_selected_source_is_never_overwritten(self):
        source = self.src / 'original.png'
        Image.new('RGB', (3, 4), 'red').save(source)
        before = source.read_bytes()
        result = run_batch(None, self.src, lambda *args: None, threading.Event(),
                           selected_files=[source])
        self.assertEqual(len(result.errors), 1)
        self.assertEqual(source.read_bytes(), before)

    def test_batch_skips_indexed_images_and_reports_progress(self):
        indexed = self.src / 'indexed.png'
        image = Image.new('P', (4, 4))
        image.putpalette([255, 0, 0] + [0] * 765)
        image.save(indexed, transparency=0)
        original_bytes = indexed.read_bytes()
        rgb = self.src / 'rgb.png'
        Image.new('RGB', (4, 4), 'blue').save(rgb)
        for selection in (None, [indexed, rgb]):
            output = self.root / ('folder-output' if selection is None else 'selection-output')
            events = []
            result = run_batch(self.src, output, lambda *event: events.append(event),
                               threading.Event(), selected_files=selection)
            self.assertEqual((result.total, result.successful, result.skipped, len(result.errors)), (2, 1, 1, 0))
            self.assertFalse((output / 'indexed.png').exists())
            self.assertTrue((output / 'rgb.png').exists())
            self.assertEqual(result.original, rgb.stat().st_size)
            self.assertEqual([value for event, value in events if event == 'progress'], [1, 2])
            self.assertTrue(any(event == 'log' and 'SKIPPED:' in value for event, value in events))
        self.assertEqual(indexed.read_bytes(), original_bytes)

    def test_nested_folders_and_cancellation(self):
        for dst in [self.src, self.src / 'out', self.root]:
            with self.assertRaises(ValueError):
                validate_folders(self.src, dst)
        Image.new('RGB', (1, 1)).save(self.src / 'a.png')
        stop = threading.Event()
        stop.set()
        result = run_batch(self.src, self.dst, lambda *args: None, stop)
        self.assertTrue(result.cancelled)
        self.assertEqual(result.successful, 0)

    def test_palette_transparency_and_animation(self):
        image = Image.new('P', (2, 1))
        image.putpalette([255, 0, 0, 0, 255, 0] + [0] * 762)
        image.putdata([0, 1])
        image.save(self.src / 'palette.png', transparency=bytes([0, 120]))
        convert_file(self.src / 'palette.png', self.dst / 'palette.png')
        with Image.open(self.dst / 'palette.png') as output:
            self.assertEqual(list(output.convert('RGBA').getchannel('A').getdata()), [0, 120])
        frame = Image.new('RGBA', (2, 2), 'red')
        frame.save(self.src / 'animated.png', save_all=True,
                   append_images=[Image.new('RGBA', (2, 2), 'blue')], duration=100)
        with self.assertRaisesRegex(ValueError, 'multi-frame'):
            convert_file(self.src / 'animated.png', self.dst / 'animated.png')


if __name__ == '__main__':
    unittest.main()
