import os
import glob
import re

LAYOUT_DIR = r"app\src\main\res\layout"
EXCLUDE_FILES = ["fragment_home_dashboard.xml", "fragment_alerts_list.xml"]

REPLACEMENTS = [
    # Backgrounds
    (r'(android:background)="(\?attr/android:colorBackground|@color/color_background_light|@color/white|@color/color_surface_light|#FFFFFF)"', r'\1="?android:colorBackground"'),
    
    # Surface/Card Backgrounds
    (r'(app:cardBackgroundColor)="(\?attr/colorSurfaceVariant|\?attr/colorSurface|@color/color_surface_light|@color/white|#FFFFFF)"', r'\1="@color/color_charcoal_grey"'),
    
    # Text
    (r'(android:textColor)="(\?attr/colorOnSurface|\?attr/colorOnBackground|@color/color_on_surface_light|@color/color_on_background_light|@color/black|#000000)"', r'\1="@color/color_on_background_dark"'),
    
    # Outlines / Strokes / Dividers
    (r'(app:strokeColor)="(\?attr/colorOutlineVariant|\?attr/colorOutline|@color/color_divider_light|@color/color_divider_dark)"', r'\1="@color/color_divider_dark"'),
    
    # Theme primary / tints
    (r'(app:tint|android:tint)="(\?attr/colorPrimary|@color/color_primary_light)"', r'\1="@color/color_signal_red"'),
    (r'(app:tint|android:tint)="(\?attr/colorSecondary)"', r'\1="@color/color_secondary"'),
]

def process_file(filepath):
    filename = os.path.basename(filepath)
    if filename in EXCLUDE_FILES:
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    new_content = content
    for pattern, replacement in REPLACEMENTS:
        new_content = re.sub(pattern, replacement, new_content)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filename}")

def main():
    # Process fragments
    files = glob.glob(os.path.join(LAYOUT_DIR, "fragment_*.xml"))
    for file in files:
        process_file(file)
        
    # Also process items if requested, but user said "all other fragments"
    # let's just do fragments.

if __name__ == "__main__":
    main()
