#!/usr/bin/env python3
"""Cheap offline guard: every capitalised identifier used as `Name.member` or
`new Name(` in app sources must be defined in the package, imported, or java.lang.
Catches missing project classes (the parse-only check cannot)."""
import re, sys
from pathlib import Path

SRC = Path('app/src/main/java')
files = list(SRC.rglob('*.java'))
package_classes = {f.stem for f in files}
JAVA_LANG = {'String','Math','System','Integer','Long','Float','Double','Boolean','Character',
             'Thread','Runnable','Object','Exception','IllegalStateException','IllegalArgumentException',
             'SecurityException','AssertionError','StringBuilder','Override','Void'}
bad = []
for f in files:
    text = f.read_text()
    code = re.sub(r'//.*|/\*.*?\*/|"(?:\\.|[^"\\])*"', '', text, flags=re.S)
    imported = {m.group(1) for m in re.finditer(r'^import\s+[\w.]+\.(\w+);', text, re.M)}
    nested = set(re.findall(r'\b(?:class|interface|enum)\s+(\w+)', code))
    known = package_classes | imported | JAVA_LANG | nested
    used = set(re.findall(r'(?<![\w.])([A-Z][A-Za-z0-9]*)\s*\.\s*[A-Za-z_]', code))
    used |= set(re.findall(r'\bnew\s+([A-Z][A-Za-z0-9]*)\s*[(<]', code))
    for name in sorted(used):
        if name not in known and not name.isupper():
            bad.append(f'{f.name}: unresolved class {name}')
if bad:
    print('\n'.join(bad), file=sys.stderr)
    sys.exit(1)
print(f'PASS symbol check: {len(files)} files')
