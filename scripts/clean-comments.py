#!/usr/bin/env python3
"""
清理 Java 文件中的个人笔记注释（【习惯】修改指引、设计思路等）。
保留：行内注释、有意义的类/方法 Javadoc、前端注释。
用法：python3 scripts/clean-comments.py
"""

import re
import sys
from pathlib import Path

# 要清理的关键词（出现在 Javadoc 块中则清理整个块）
STRIP_KEYWORDS = ['【习惯】', '修改指引：', '设计思路：']

# 要清理的行内注释模式（单行 // 注释中包含这些）
STRIP_INLINE_PATTERNS = [
    r'^\s*//\s*【习惯】',
    r'^\s*//\s*修改指引',
]

ROOT = Path(__file__).resolve().parent.parent
BACKEND_SRC = ROOT / 'backend' / 'src'


def should_strip_javadoc_block(block: str) -> bool:
    """检查 Javadoc 块是否包含需要清理的关键词"""
    return any(kw in block for kw in STRIP_KEYWORDS)


def should_strip_inline(line: str) -> bool:
    """检查行内注释是否需要清理"""
    return any(re.match(p, line) for p in STRIP_INLINE_PATTERNS)


def clean_file(filepath: Path) -> int:
    """清理单个文件，返回删除的行数"""
    content = filepath.read_text(encoding='utf-8')
    lines = content.split('\n')
    result = []
    removed = 0
    i = 0

    while i < len(lines):
        line = lines[i]

        # 检测 Javadoc 块开始
        if line.strip().startswith('/**') and not line.strip().endswith('*/'):
            block_lines = [line]
            i += 1
            while i < len(lines):
                block_lines.append(lines[i])
                if lines[i].strip().endswith('*/'):
                    break
                i += 1
            i += 1

            block = '\n'.join(block_lines)
            if should_strip_javadoc_block(block):
                removed += len(block_lines)
                continue  # 跳过整个块
            else:
                result.extend(block_lines)
                continue

        # 检测单行 Javadoc
        if line.strip().startswith('/**') and line.strip().endswith('*/'):
            if should_strip_javadoc_block(line):
                removed += 1
                i += 1
                continue

        # 检测行内注释
        if should_strip_inline(line):
            removed += 1
            i += 1
            continue

        result.append(line)
        i += 1

    if removed > 0:
        filepath.write_text('\n'.join(result), encoding='utf-8')
    return removed


def main():
    total_removed = 0
    files_cleaned = 0

    # 只清理后端 Java 文件（前端注释基本合理）
    java_files = sorted(BACKEND_SRC.rglob('*.java'))
    print(f'扫描 {len(java_files)} 个 Java 文件...')

    for f in java_files:
        removed = clean_file(f)
        if removed > 0:
            files_cleaned += 1
            total_removed += removed
            print(f'  ✓ {f.relative_to(ROOT)}：删除 {removed} 行注释')

    print(f'\n完成：清理 {files_cleaned} 个文件，共删除 {total_removed} 行注释')


if __name__ == '__main__':
    main()
