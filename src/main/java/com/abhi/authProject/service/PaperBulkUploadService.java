package com.abhi.authProject.service;

import com.abhi.authProject.model.Department;
import com.abhi.authProject.model.DepartmentBranch;
import com.abhi.authProject.model.Paper;
import com.abhi.authProject.repo.DepartmentBranchRepo;
import com.abhi.authProject.repo.DepartmentRepo;
import com.abhi.authProject.repo.PaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class PaperBulkUploadService {

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private DepartmentBranchRepo branchRepo;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Paper> processZipFile(MultipartFile file, String university, int defaultYear) throws IOException {
        Path tempDir = Files.createTempDirectory("paper-upload-");
        Path zipPath = tempDir.resolve("upload.zip");
        Files.copy(file.getInputStream(), zipPath);

        List<Paper> uploadedPapers = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".pdf")) {
                    continue;
                }

                String path = entry.getName();
                // Expected Structure: Branch/Semester X/Subject/File.pdf
                // Example: CS/Semester 1/Mathematics/Calculus.pdf
                String[] parts = path.split("/");
                if (parts.length < 4)
                    continue;

                String branchCode = parts[parts.length - 4].trim().toUpperCase();
                String semesterStr = parts[parts.length - 3].trim();
                String subjectName = parts[parts.length - 2].trim();
                String fileName = parts[parts.length - 1].trim();

                int semester = extractSemesterNumber(semesterStr);
                int year = extractYear(fileName, defaultYear);

                // 1. Ensure Department/Branch exists
                ensureBranchExists(branchCode);

                // 2. Save the file
                String savedFileName;
                try (InputStream is = zipFile.getInputStream(entry)) {
                    // Mocking a multipart file for the storage service or reusing its logic
                    savedFileName = fileStorageService.saveFileFromStream(is, fileName, "papers");
                }

                String downloadUrl = "/api/papers/download/" + savedFileName;
                String title = fileName.replace(".pdf", "").replace(".PDF", "").trim();

                Paper paper = new Paper(title, subjectName, year, semester, branchCode, null, "End-Sem",
                        university, downloadUrl);
                uploadedPapers.add(paperRepository.save(paper));
            }
        } finally {
            Files.deleteIfExists(zipPath);
            Files.deleteIfExists(tempDir);
        }

        return uploadedPapers;
    }

    private int extractYear(String fileName, int defaultYear) {
        // Look for 4 digit years starting with 20 or 19 (e.g. 2022, 2019)
        Pattern pattern = Pattern.compile("(20|19)\\d{2}");
        Matcher matcher = pattern.matcher(fileName);
        int bestYear = defaultYear;
        while (matcher.find()) {
            bestYear = Integer.parseInt(matcher.group());
        }
        return bestYear;
    }

    private int extractSemesterNumber(String semesterStr) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(semesterStr);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 1; // Default
    }

    private void ensureBranchExists(String code) {
        if (!departmentRepo.existsByCode(code)) {
            Department dept = new Department();
            dept.setName(code + " Department");
            dept.setCode(code);
            // dept.setMaxSemesters(8); // This field doesn't exist in Department
            departmentRepo.save(dept);
        }

        if (branchRepo.findByBranchCode(code).isEmpty()) {
            java.util.Optional<Department> deptOpt = departmentRepo.findByCode(code);
            if (deptOpt.isPresent()) {
                Department dept = deptOpt.get();
                DepartmentBranch branch = new DepartmentBranch();
                branch.setDepartment(dept);
                branch.setBranchName(code);
                branch.setBranchCode(code);
                branch.setMaxSemesters(8);
                branchRepo.save(branch);
            }
        }
    }
}
