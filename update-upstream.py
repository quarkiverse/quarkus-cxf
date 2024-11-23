#!/usr/bin/env python3
from pathlib import Path
import re

from conda_build.utils import copy_into

version = "3.15.2"
directory = Path(__file__).parent / f"docs/modules/ROOT/pages"

nav = directory.parent / "nav.adoc"
# print(nav.read_text(encoding="utf-8"))

output = Path(f"cxf-converted-{version}")

replacements = {'xref:':'xref:'}

nav_match_lvl1 = re.compile( r'^[*]+ xref:(.*?)\/(.*?)\.adoc\[(.*?)\]')
nav_match_lvl2 = re.compile( r'^[*]+ xref:(.*?)\/(.*?)\/(.*?)\.adoc\[(.*?)\]')

def copy_dirs():
    dirs = [[len(d.parents) - len(directory.parents), d] for d in directory.rglob('*') if d.is_dir()]
    # print(dirs)
    for item in dirs:
        item[1].copy_into(output,  dirs_exist_ok=True, preserve_metadata=False)
    return dirs

def create_include_files(navfile):
    with open(navfile) as f:
        for line in f:
            print(line)
            if f"xref:" in line:
                m1=nav_match_lvl1.match(line)
                m2=nav_match_lvl2.match(line)
                if m1:
                    print(">", m1.groups())
                elif m2:
                    print(">", m2.groups())
                else:
                    pass
            else:
                pass

def replace_in_line(inline):
    for src, target in replacements.items():
        outline = inline.replace(src, target)
    return outline

def replace_in_file(inpath):
    outpath = output / inpath.name
    with open(inpath, 'r') as infile, open(outpath, 'w') as outfile:
        for line in infile:
            replace_in_line(line)
            outfile.write(line)

def copy_examples():
    for dir in [d for d in directory.rglob('*') if d.is_dir()]:
        for fpath in dir.glob("*.adoc"):
            rel = (len(dir.parents) - len(directory.parents)) * "../"
            # print(Path(rel).joinpath(dir.parent).name)


def copy_examples():
    for f in [d for d in (directory.parent / "examples").glob('*') if d.is_file()]:
        print(f.name, "!")

def copy_images():
    for f in [d for d in (directory.parent / "images").glob('*') if d.is_file()]:
        print(f.name, "!")



if __name__ == "__main__":
    #copy_dirs()
    print("main")
    create_include_files(nav)