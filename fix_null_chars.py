import re

# Read the file
with open('src/main/java/com/abhi/authProject/controller/AuthController.java', 'rb') as f:
    content = f.read()

# Remove null bytes
cleaned_content = content.replace(b'\x00', b'')

# Write back
with open('src/main/java/com/abhi/authProject/controller/AuthController.java', 'wb') as f:
    f.write(cleaned_content)

print("Null characters removed successfully!")
