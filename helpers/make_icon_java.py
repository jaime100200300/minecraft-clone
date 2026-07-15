import os

# Input PNG
input_path = os.path.join("helpers", "icon.png")

# Output Java file
output_path = os.path.join("src", "game", "IconData.java")

with open(input_path, "rb") as f:
    data = f.read()

with open(output_path, "w") as out:
    out.write("public class IconData {\n")
    out.write("    public static final byte[] ICON = new byte[] {\n")

    # Write bytes in hex form
    for b in data:
        out.write(f"        (byte)0x{b:02X},\n")

    out.write("    };\n")
    out.write("}\n")

print("IconData.java generated successfully.")