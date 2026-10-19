"""Fixtures de cabeceras: detectar incompatibilidad sin rechazar RELRO separado."""
import struct
import unittest
from verify_apk_elf import segments


def elf64(headers, endian='<'):
    identity=b'\x7fELF'+bytes([2,1 if endian=='<' else 2,1])+bytes(9)
    header=struct.pack(endian+'16sHHIQQQIHHHHHH', identity,3,183,1,0,64,0,0,64,56,len(headers),0,0,0)
    return header+b''.join(struct.pack(endian+'IIQQQQQQ',*record) for record in headers)


class ElfAlignmentTest(unittest.TestCase):
    def test_32bit_header_decodes_load_alignment(self):
        identity=b'\x7fELF'+bytes([1,1,1])+bytes(9)
        header=struct.pack('<16sHHIIIIIHHHHHH',identity,3,40,1,0,52,0,0,52,32,1,0,0,0)
        data=header+struct.pack('<IIIIIIII',1,0,0,0,128,128,6,16384)
        self.assertTrue(segments(data)[0]['load_16kb'])

    def test_load_4kb_is_not_16kb(self):
        record=segments(elf64([(1,6,0,0,0,128,128,4096)]))[0]
        self.assertFalse(record['load_16kb'])

    def test_load_alignment_does_not_hide_incongruent_offset(self):
        record=segments(elf64([(1,6,0,4096,0,128,128,16384)]))[0]
        self.assertFalse(record['load_16kb'])

    def test_relro_sharing_page_with_writable_tail_is_rejected(self):
        records=segments(elf64([(1,6,0,0,0,0x6000,0x6000,16384),
            (0x6474e552,4,0,0,0,0x1000,0x1000,1)]))
        self.assertFalse(records[-1]['relro_16kb_safe_headers'])

    def test_isolated_relro_matches_android_documentation_exception(self):
        records=segments(elf64([(1,6,0x680,0x4680,0,0x1d8,0x980,16384),
            (0x6474e552,4,0x680,0x4680,0,0x1d8,0x980,1)]))
        self.assertFalse(records[-1]['relro_end_16kb'])
        self.assertTrue(records[-1]['relro_16kb_safe_headers'])

    def test_other_writable_load_on_protected_page_is_rejected(self):
        records=segments(elf64([(1,6,0,0,0,0x1000,0x1000,16384),
            (1,6,0x2000,0x2000,0,16,16,16384),
            (0x6474e552,4,0,0,0,0x1000,0x1000,1)]))
        self.assertFalse(records[-1]['relro_16kb_safe_headers'])

    def test_big_endian_header_is_decoded(self):
        self.assertTrue(segments(elf64([(1,6,0,0,0,128,128,16384)],'>'))[0]['load_16kb'])

    def test_truncated_or_out_of_bounds_headers_are_rejected(self):
        for data in (b'',b'\x7fELF',elf64([(1,6,0,0,0,128,128,16384)])[:-1]):
            with self.subTest(size=len(data)), self.assertRaises(ValueError):
                segments(data)


if __name__ == '__main__':
    unittest.main()
