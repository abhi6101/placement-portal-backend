package com.abhi.authProject.repo;

import com.abhi.authProject.model.ApplicationStatus;
import com.abhi.authProject.model.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryRepository extends JpaRepository<GalleryItem, Long> {
    List<GalleryItem> findByStatus(ApplicationStatus status);

    List<GalleryItem> findByUploadedBy(String uploadedBy);
}
