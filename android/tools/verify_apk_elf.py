"""Comprueba estáticamente LOAD/RELRO de bibliotecas ELF dentro de un APK.

No ejecuta ni extrae bibliotecas, no requiere NDK ni dependencias externas.
El criterio 16 KB aplica a arm64-v8a/x86_64; los ABI de 32 bits se informan aparte.
No sustituye zipalign ni la ejecución en Android con páginas de 16 KB.
Referencia: https://developer.android.com/guide/practices/page-sizes
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile

def segments(data):
    """Lee PT_LOAD/GNU_RELRO y detecta páginas protegidas que aún requieren escritura."""
    if len(data) < 64 or data[:4] != b'\x7fELF' or data[5] not in (1, 2):
        raise ValueError('Cabecera ELF no admitida')
    endian = '<' if data[5] == 1 else '>'
    bits = data[4]
    if bits == 2:
        offset = struct.unpack_from(endian+'Q', data, 32)[0]
        size, count = struct.unpack_from(endian+'HH', data, 54)
        fmt = endian+'IIQQQQQQ'
    elif bits == 1:
        offset = struct.unpack_from(endian+'I', data, 28)[0]
        size, count = struct.unpack_from(endian+'HH', data, 42)
        fmt = endian+'IIIIIIII'
    else:
        raise ValueError('Clase ELF no admitida')
    if size < struct.calcsize(fmt) or offset+size*count > len(data):
        raise ValueError('Tabla ELF fuera de límites')
    result = []
    for index in range(count):
        fields = struct.unpack_from(fmt, data, offset+size*index)
        kind = fields[0]
        if bits == 2:
            _, flags, off, virtual, _, file_size, mem_size, align = fields
        else:
            _, off, virtual, _, file_size, mem_size, flags, align = fields
        if kind in (1, 0x6474e552):
            result.append(dict(type='LOAD' if kind == 1 else 'GNU_RELRO', flags=flags, offset=off,
                virtual_address=virtual, file_size=file_size, memory_size=mem_size, alignment=align,
                load_16kb=(align >= 16384 and align & (align-1) == 0 and (virtual-off)%16384 == 0) if kind == 1 else None,
                relro_end_16kb=(virtual+mem_size)%16384 == 0 if kind != 1 else None))
    loads = [s for s in result if s['type'] == 'LOAD']
    for relro in (s for s in result if s['type'] == 'GNU_RELRO'):
        start = relro['virtual_address']; end = start + relro['memory_size']
        protected_start = start // 16384 * 16384
        protected_end = (end + 16383) // 16384 * 16384
        # La fórmula del módulo no basta: un LOAD separado, totalmente RELRO,
        # puede terminar antes de 16 KB sin proteger datos que deban ser escribibles.
        overlaps = []
        for load in loads:
            if not load['flags'] & 2: continue
            lo = load['virtual_address']; hi = lo + load['memory_size']
            if max(lo, protected_start) < min(hi, start) or max(lo, end) < min(hi, protected_end):
                overlaps.append(dict(start=lo, end=hi))
        relro['writable_outside_relro_on_protected_pages'] = overlaps
        relro['relro_16kb_safe_headers'] = not overlaps
    return result


def inspect_apk(path):
    """Devuelve cabeceras de cada ABI y fallos estáticos limitados a los ABI de 64 bits."""
    libraries = []
    with zipfile.ZipFile(path) as archive:
        for name in sorted(archive.namelist()):
            if not name.startswith('lib/') or not name.endswith('.so'):
                continue
            data = archive.read(name)
            headers = segments(data)
            loads = [header for header in headers if header['type'] == 'LOAD']
            if not loads:
                raise ValueError('Biblioteca sin LOAD: '+name)
            libraries.append(dict(path=name, sha256=hashlib.sha256(data).hexdigest(),
                segments=headers, load_16kb=all(header['load_16kb'] for header in loads),
                relro_16kb_safe_headers=all(header['relro_16kb_safe_headers']
                    for header in headers if header['type'] == 'GNU_RELRO')))
    failures = [library['path'] for library in libraries
        if library['path'].split('/')[1] in ('arm64-v8a', 'x86_64')
        and not (library['load_16kb'] and library['relro_16kb_safe_headers'])]
    return dict(apk_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        libraries=libraries, failures_64bit=failures,
        limits='Sólo cabeceras ELF; no acredita ZIP, firma ni ejecución Android 16 KB')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    args = parser.parse_args()
    try:
        result = inspect_apk(args.apk)
    except (OSError, ValueError, struct.error, zipfile.BadZipFile) as error:
        parser.exit(2, f'Comprobación detenida: {error}\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(1 if result['failures_64bit'] else 0)
