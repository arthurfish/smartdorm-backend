package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.ContentArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentArticleRepository extends JpaRepository<ContentArticle, UUID> {
    List<ContentArticle> findByCategory(String category);
}