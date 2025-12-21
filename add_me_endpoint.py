import re

# Read the file
with open('src/main/java/com/abhi/authProject/controller/AuthController.java', 'r', encoding='utf-8') as f:
    content = f.read()

# The /me endpoint code
me_endpoint = '''
    // Get current user information
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            Users user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
            }
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "branch", user.getBranch() != null ? user.getBranch() : "",
                "semester", user.getSemester() != null ? user.getSemester() : 0,
                "batch", user.getBatch() != null ? user.getBatch() : "",
                "computerCode", user.getComputerCode() != null ? user.getComputerCode() : "",
                "aadharNumber", user.getAadharNumber() != null ? user.getAadharNumber() : "",
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "fatherName", user.getFatherName() != null ? user.getFatherName() : "",
                "institution", user.getInstitution() != null ? user.getInstitution() : "",
                "session", user.getSession() != null ? user.getSession() : "",
                "mobilePrimary", user.getMobilePrimary() != null ? user.getMobilePrimary() : "",
                "mobileSecondary", user.getMobileSecondary() != null ? user.getMobileSecondary() : "",
                "enrollmentNumber", user.getEnrollmentNumber() != null ? user.getEnrollmentNumber() : "",
                "startYear", user.getStartYear() != null ? user.getStartYear() : "",
                "companyName", user.getCompanyName() != null ? user.getCompanyName() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to get user info: " + e.getMessage()));
        }
    }
'''

# Find the login method end and insert after it
# Look for the pattern where login method ends
pattern = r'(}\s*}\s*\n\s*)(//.*REGISTER|@PostMapping\("/register"\))'
replacement = r'\1' + me_endpoint + r'\n\n    \2'

new_content = re.sub(pattern, replacement, content, count=1)

# Write back
with open('src/main/java/com/abhi/authProject/controller/AuthController.java', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("/me endpoint added successfully!")
