#!/usr/bin/env python3
"""
Find Chinese (CJK Han + common punctuation) characters in files under a directory.

Usage:
  python find_chinese.py <folder>         # defaults to ./src when no folder provided
  python find_chinese.py <folder> --out matches.txt
  python find_chinese.py --help
"""

import os
import sys
import re
import json
from argparse import ArgumentParser

# Regex that covers the common CJK ranges and common CJK punctuation:
CHINESE_RE = re.compile(
    r'[\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\u3000-\u303F]+'
)

# Try reading with these encodings in order
ENCODINGS_TO_TRY = ("utf-8", "gbk", "latin-1")

def is_binary_file(path, blocksize=1024):
    try:
        with open(path, 'rb') as f:
            chunk = f.read(blocksize)
            if b'\0' in chunk:
                return True
            # Heuristic: if most bytes are non-text-like, treat as binary
            # (very permissive)
            return False
    except Exception:
        return True

def read_lines_with_encoding(path):
    last_exc = None
    for enc in ENCODINGS_TO_TRY:
        try:
            with open(path, 'r', encoding=enc, errors='strict') as f:
                return f.readlines(), enc
        except Exception as e:
            last_exc = e
            # try next encoding
    # fallback: open with latin-1 replacing errors
    try:
        with open(path, 'r', encoding='latin-1', errors='replace') as f:
            return f.readlines(), 'latin-1-replace'
    except Exception as e:
        raise last_exc or e

def snippet_for_match(line, start, end, max_context=40):
    # return a short snippet around the matched text (escape newlines)
    s = line[max(0, start - max_context):min(len(line), end + max_context)]
    s = s.replace('\n', '\\n').replace('\r', '\\r')
    return s

def scan_folder(folder, include_extensions=None, exclude_dirs=None):
    results = []
    folder = os.path.abspath(folder)
    for root, dirs, files in os.walk(folder):
        # optionally exclude dirs
        if exclude_dirs:
            dirs[:] = [d for d in dirs if d not in exclude_dirs]
        for name in files:
            path = os.path.join(root, name)
            # optionally filter by extension (None = all)
            if include_extensions is not None:
                _, ext = os.path.splitext(name)
                if ext.lower() not in include_extensions:
                    continue
            # quick binary check
            if is_binary_file(path):
                continue
            try:
                lines, enc = read_lines_with_encoding(path)
            except Exception:
                # skip unreadable files
                continue
            for i, line in enumerate(lines, start=1):
                for m in CHINESE_RE.finditer(line):
                    col = m.start() + 1  # 1-based column
                    matched = m.group(0)
                    snippet = snippet_for_match(line, m.start(), m.end())
                    results.append({
                        "path": path,
                        "line": i,
                        "col": col,
                        "match": matched,
                        "snippet": snippet,
                        "encoding": enc
                    })
    return results

def print_clickable(results):
    # IntelliJ / many editors accept "path:line:column: message"
    for r in results:
        # Use a short message including the matched chars and a snippet
        msg = f"{r['match']} -- {r['snippet']}"
        # On Windows console, backslashes are fine; print absolute path
        print(f"{r['path']}:{r['line']}:{r['col']}: {msg}")

def main(argv):
    ap = ArgumentParser(description="Find Chinese characters in a source tree")
    ap.add_argument("folder", nargs="?", default="src", help="Folder to scan (default: src)")
    ap.add_argument("--ext", "-e", nargs="*", help="Limit to these file extensions (e.g. .java .yml). Default: common text files")
    ap.add_argument("--out", "-o", help="Write results to text file (clickable lines).")
    ap.add_argument("--json", "-j", help="Write JSON results to file.")
    ap.add_argument("--exclude-dir", "-x", nargs="*", help="Directory names to skip (relative names).")
    args = ap.parse_args(argv)

    if args.ext:
        include_extensions = set(e if e.startswith('.') else ('.' + e) for e in args.ext)
    else:
        # common source/resource extensions
        include_extensions = {
            '.java', '.kt', '.groovy', '.xml', '.yml', '.yaml', '.properties',
            '.txt', '.md', '.json', '.gradle', '.cfg', '.ini', '.csv', '.html', '.jsp'
        }

    results = scan_folder(args.folder, include_extensions=include_extensions, exclude_dirs=args.exclude_dir)

    if not results:
        print("No Chinese characters found.")
        return 0

    # Print clickable lines to stdout
    print_clickable(results)

    # Optionally write plain output file
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            for r in results:
                msg = f"{r['match']} -- {r['snippet']}"
                f.write(f"{r['path']}:{r['line']}:{r['col']}: {msg}\n")
        print(f"Wrote plain results to {args.out}")

    # Optionally write JSON
    if args.json:
        with open(args.json, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print(f"Wrote JSON results to {args.json}")

    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))#!/usr/bin/env python3
"""
Find Chinese (CJK Han + common punctuation) characters in files under a directory.

Usage:
  python find_chinese.py <folder>         # defaults to ./src when no folder provided
  python find_chinese.py <folder> --out matches.txt
  python find_chinese.py --help
"""

import os
import sys
import re
import json
from argparse import ArgumentParser

# Regex that covers the common CJK ranges and common CJK punctuation:
CHINESE_RE = re.compile(
    r'[\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\u3000-\u303F]+'
)

# Try reading with these encodings in order
ENCODINGS_TO_TRY = ("utf-8", "gbk", "latin-1")

def is_binary_file(path, blocksize=1024):
    try:
        with open(path, 'rb') as f:
            chunk = f.read(blocksize)
            if b'\0' in chunk:
                return True
            # Heuristic: if most bytes are non-text-like, treat as binary
            # (very permissive)
            return False
    except Exception:
        return True

def read_lines_with_encoding(path):
    last_exc = None
    for enc in ENCODINGS_TO_TRY:
        try:
            with open(path, 'r', encoding=enc, errors='strict') as f:
                return f.readlines(), enc
        except Exception as e:
            last_exc = e
            # try next encoding
    # fallback: open with latin-1 replacing errors
    try:
        with open(path, 'r', encoding='latin-1', errors='replace') as f:
            return f.readlines(), 'latin-1-replace'
    except Exception as e:
        raise last_exc or e

def snippet_for_match(line, start, end, max_context=40):
    # return a short snippet around the matched text (escape newlines)
    s = line[max(0, start - max_context):min(len(line), end + max_context)]
    s = s.replace('\n', '\\n').replace('\r', '\\r')
    return s

def scan_folder(folder, include_extensions=None, exclude_dirs=None):
    results = []
    folder = os.path.abspath(folder)
    for root, dirs, files in os.walk(folder):
        # optionally exclude dirs
        if exclude_dirs:
            dirs[:] = [d for d in dirs if d not in exclude_dirs]
        for name in files:
            path = os.path.join(root, name)
            # optionally filter by extension (None = all)
            if include_extensions is not None:
                _, ext = os.path.splitext(name)
                if ext.lower() not in include_extensions:
                    continue
            # quick binary check
            if is_binary_file(path):
                continue
            try:
                lines, enc = read_lines_with_encoding(path)
            except Exception:
                # skip unreadable files
                continue
            for i, line in enumerate(lines, start=1):
                for m in CHINESE_RE.finditer(line):
                    col = m.start() + 1  # 1-based column
                    matched = m.group(0)
                    snippet = snippet_for_match(line, m.start(), m.end())
                    results.append({
                        "path": path,
                        "line": i,
                        "col": col,
                        "match": matched,
                        "snippet": snippet,
                        "encoding": enc
                    })
    return results

def print_clickable(results):
    # IntelliJ / many editors accept "path:line:column: message"
    for r in results:
        # Use a short message including the matched chars and a snippet
        msg = f"{r['match']} -- {r['snippet']}"
        # On Windows console, backslashes are fine; print absolute path
        print(f"{r['path']}:{r['line']}:{r['col']}: {msg}")

def main(argv):
    ap = ArgumentParser(description="Find Chinese characters in a source tree")
    ap.add_argument("folder", nargs="?", default="src", help="Folder to scan (default: src)")
    ap.add_argument("--ext", "-e", nargs="*", help="Limit to these file extensions (e.g. .java .yml). Default: common text files")
    ap.add_argument("--out", "-o", help="Write results to text file (clickable lines).")
    ap.add_argument("--json", "-j", help="Write JSON results to file.")
    ap.add_argument("--exclude-dir", "-x", nargs="*", help="Directory names to skip (relative names).")
    args = ap.parse_args(argv)

    if args.ext:
        include_extensions = set(e if e.startswith('.') else ('.' + e) for e in args.ext)
    else:
        # common source/resource extensions
        include_extensions = {
            '.java', '.kt', '.groovy', '.xml', '.yml', '.yaml', '.properties',
            '.txt', '.md', '.json', '.gradle', '.cfg', '.ini', '.csv', '.html', '.jsp'
        }

    results = scan_folder(args.folder, include_extensions=include_extensions, exclude_dirs=args.exclude_dir)

    if not results:
        print("No Chinese characters found.")
        return 0

    # Print clickable lines to stdout
    print_clickable(results)

    # Optionally write plain output file
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            for r in results:
                msg = f"{r['match']} -- {r['snippet']}"
                f.write(f"{r['path']}:{r['line']}:{r['col']}: {msg}\n")
        print(f"Wrote plain results to {args.out}")

    # Optionally write JSON
    if args.json:
        with open(args.json, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print(f"Wrote JSON results to {args.json}")

    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))